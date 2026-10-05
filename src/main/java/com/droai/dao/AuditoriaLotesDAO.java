package com.droai.dao;

import com.droai.config.DatabaseConfig;
import com.droai.model.DiscrepanciaLoteDTO;
import com.droai.model.KardexMovimientoDTO;
import com.droai.model.LoteDetalleDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos para la Auditoría y Control de Discrepancias entre Stock Almacén y Lotes.
 */
public class AuditoriaLotesDAO {

    private static final Logger logger = LoggerFactory.getLogger(AuditoriaLotesDAO.class);

    /**
     * Obtiene el listado de todos los artículos con discrepancia entre existencia macro y lotes.
     */
    public List<DiscrepanciaLoteDTO> listarDiscrepancias(String filtro) {
        List<DiscrepanciaLoteDTO> lista = new ArrayList<>();
        String sql = """
            WITH StockLotes AS (
                SELECT 
                    co_art,
                    co_alma,
                    SUM(stock_actual) AS stock_lotes,
                    COUNT(DISTINCT numero_lote) AS cant_lotes_con_stock
                FROM saLoteEntrada
                GROUP BY co_art, co_alma
            ),
            StockAlm AS (
                SELECT 
                    co_art,
                    co_alma,
                    stock AS stock_almacen
                FROM saStockAlmacen
                WHERE tipo = 'ACT'
            )
            SELECT 
                RTRIM(a.co_art) AS co_art,
                RTRIM(a.art_des) AS art_des,
                RTRIM(almas.co_alma) AS co_alma,
                RTRIM(ISNULL(almNom.des_alma, almas.co_alma)) AS des_alma,
                ISNULL(alm.stock_almacen, 0) AS stock_almacen,
                ISNULL(lot.stock_lotes, 0) AS stock_lotes,
                (ISNULL(alm.stock_almacen, 0) - ISNULL(lot.stock_lotes, 0)) AS diferencia,
                ISNULL(lot.cant_lotes_con_stock, 0) AS cant_lotes_activos
            FROM saArticulo a
            CROSS JOIN (SELECT DISTINCT co_alma FROM saAlmacen) almas
            LEFT JOIN saAlmacen almNom ON almas.co_alma = almNom.co_alma
            LEFT JOIN StockAlm alm ON alm.co_art = a.co_art AND alm.co_alma = almas.co_alma
            LEFT JOIN StockLotes lot ON lot.co_art = a.co_art AND lot.co_alma = almas.co_alma
            WHERE a.maneja_lote = 1
              AND (ISNULL(alm.stock_almacen, 0) <> 0 OR ISNULL(lot.stock_lotes, 0) <> 0)
              AND ABS(ISNULL(alm.stock_almacen, 0) - ISNULL(lot.stock_lotes, 0)) > 0.0001
        """;

        if (filtro != null && !filtro.isBlank()) {
            sql += " AND (a.co_art LIKE ? OR a.art_des LIKE ?) ";
        }

        sql += " ORDER BY ABS(ISNULL(alm.stock_almacen, 0) - ISNULL(lot.stock_lotes, 0)) DESC, a.co_art ASC ";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (filtro != null && !filtro.isBlank()) {
                String pattern = "%" + filtro.trim() + "%";
                ps.setString(1, pattern);
                ps.setString(2, pattern);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new DiscrepanciaLoteDTO(
                            rs.getString("co_art"),
                            rs.getString("art_des"),
                            rs.getString("co_alma"),
                            rs.getString("des_alma"),
                            rs.getDouble("stock_almacen"),
                            rs.getDouble("stock_lotes"),
                            rs.getDouble("diferencia"),
                            rs.getInt("cant_lotes_activos")
                    ));
                }
            }
        } catch (SQLException e) {
            logger.error("[AuditoriaLotesDAO] Error al listar discrepancias: {}", e.getMessage(), e);
        }
        return lista;
    }

    /**
     * Obtiene el desglose de lotes en saLoteEntrada para un artículo y almacén.
     */
    public List<LoteDetalleDTO> listarLotesArticulo(String coArt, String coAlma) {
        List<LoteDetalleDTO> lista = new ArrayList<>();
        String sql = """
            SELECT CAST(le.rowguid AS VARCHAR(36)) AS rowguid,
                   RTRIM(le.co_art) AS co_art,
                   RTRIM(le.co_alma) AS co_alma,
                   RTRIM(le.numero_lote) AS numero_lote,
                   CONVERT(VARCHAR(10), le.fecha_expiracion, 120) AS fecha_expiracion,
                   le.cantidad,
                   le.stock_actual,
                   RTRIM(le.tipo_doc) AS tipo_doc,
                   CONVERT(VARCHAR(19), le.fe_us_in, 120) AS fe_us_in
            FROM saLoteEntrada le
            WHERE RTRIM(le.co_art) = ? AND (RTRIM(le.co_alma) = ? OR ? = '')
            ORDER BY le.fecha_expiracion ASC, le.fe_us_in DESC
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, coArt.trim());
            ps.setString(2, coAlma != null ? coAlma.trim() : "");
            ps.setString(3, coAlma != null ? coAlma.trim() : "");

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new LoteDetalleDTO(
                            rs.getString("rowguid"),
                            rs.getString("co_art"),
                            rs.getString("co_alma"),
                            rs.getString("numero_lote"),
                            rs.getString("fecha_expiracion"),
                            rs.getDouble("cantidad"),
                            rs.getDouble("stock_actual"),
                            rs.getString("tipo_doc"),
                            rs.getString("fe_us_in")
                    ));
                }
            }
        } catch (SQLException e) {
            logger.error("[AuditoriaLotesDAO] Error al listar lotes para {}: {}", coArt, e.getMessage(), e);
        }
        return lista;
    }

    /**
     * Obtiene el historial de movimientos de Kardex para un artículo.
     */
    public List<KardexMovimientoDTO> listarHistorialKardex(String coArt) {
        List<KardexMovimientoDTO> lista = new ArrayList<>();
        String sql = """
            SELECT tipo_doc, RTRIM(doc_num) AS doc_num, CONVERT(VARCHAR(10), fec_emis, 120) AS fecha, reng_num, total_art, lote_asignado, RTRIM(origen) AS origen
            FROM (
                SELECT 'NREC' AS tipo_doc, r.doc_num, c.fec_emis, r.reng_num, r.total_art, r.lote_asignado, ISNULL(c.co_prov, '') AS origen
                FROM saNotaRecepcionCompraReng r JOIN saNotaRecepcionCompra c ON r.doc_num = c.doc_num
                WHERE RTRIM(r.co_art) = ? AND c.anulado = 0
                UNION ALL
                SELECT 'AJUS-' + r.co_tipo, r.ajue_num, a.fecha, r.reng_num, r.total_art, r.lote_asignado, ISNULL(a.motivo, '') AS origen
                FROM saAjusteReng r JOIN saAjuste a ON r.ajue_num = a.ajue_num
                WHERE RTRIM(r.co_art) = ? AND a.anulado = 0
                UNION ALL
                SELECT 'NENT', r.doc_num, n.fec_emis, r.reng_num, -r.total_art, r.lote_asignado, ISNULL(n.co_cli, '') AS origen
                FROM saNotaEntregaVentaReng r JOIN saNotaEntregaVenta n ON r.doc_num = n.doc_num
                WHERE RTRIM(r.co_art) = ? AND n.anulado = 0
                UNION ALL
                SELECT 'FACT', r.doc_num, f.fec_emis, r.reng_num, -r.total_art, r.lote_asignado, ISNULL(f.co_cli, '') AS origen
                FROM saFacturaVentaReng r JOIN saFacturaVenta f ON r.doc_num = f.doc_num
                WHERE RTRIM(r.co_art) = ? AND f.anulado = 0 AND (r.num_doc IS NULL OR RTRIM(r.num_doc) = '')
            ) t
            ORDER BY fecha DESC, doc_num DESC
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String art = coArt.trim();
            ps.setString(1, art);
            ps.setString(2, art);
            ps.setString(3, art);
            ps.setString(4, art);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new KardexMovimientoDTO(
                            rs.getString("tipo_doc"),
                            rs.getString("doc_num"),
                            rs.getString("fecha"),
                            rs.getInt("reng_num"),
                            rs.getDouble("total_art"),
                            rs.getBoolean("lote_asignado"),
                            rs.getString("origen")
                    ));
                }
            }
        } catch (SQLException e) {
            logger.error("[AuditoriaLotesDAO] Error al obtener historial kardex de {}: {}", coArt, e.getMessage(), e);
        }
        return lista;
    }

    /**
     * Modifica el número de lote y la fecha de vencimiento de un registro de saLoteEntrada,
     * cascadiando el cambio a saLoteSalida si ya hubo salidas de ese lote.
     */
    public boolean actualizarLoteYVencimiento(String rowguidLote, String nuevoLote, String nuevaFechaVenc, String coUsuario) {
        String sqlLe = """
            UPDATE saLoteEntrada
            SET numero_lote = ?, fecha_expiracion = ?, co_us_mo = ?, fe_us_mo = GETDATE()
            WHERE rowguid = CAST(? AS UNIQUEIDENTIFIER)
        """;

        String sqlLs = """
            UPDATE saLoteSalida
            SET numero_lote = ?, co_us_mo = ?, fe_us_mo = GETDATE()
            WHERE Rowguid_Lote = CAST(? AS UNIQUEIDENTIFIER)
        """;

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psLe = conn.prepareStatement(sqlLe);
                 PreparedStatement psLs = conn.prepareStatement(sqlLs)) {

                psLe.setString(1, nuevoLote.trim());
                psLe.setString(2, nuevaFechaVenc.trim() + " 00:00:00");
                psLe.setString(3, coUsuario != null ? coUsuario.trim() : "DROAI");
                psLe.setString(4, rowguidLote.trim());
                int rowsLe = psLe.executeUpdate();

                psLs.setString(1, nuevoLote.trim());
                psLs.setString(2, coUsuario != null ? coUsuario.trim() : "DROAI");
                psLs.setString(3, rowguidLote.trim());
                psLs.executeUpdate();

                conn.commit();
                logger.info("[AuditoriaLotesDAO] Lote {} actualizado exitosamente a nuevoLote={}, venc={}", rowguidLote, nuevoLote, nuevaFechaVenc);
                return rowsLe > 0;
            } catch (SQLException e) {
                conn.rollback();
                logger.error("[AuditoriaLotesDAO] Error al actualizar lote y vencimiento: {}", e.getMessage(), e);
                return false;
            }
        } catch (SQLException e) {
            logger.error("[AuditoriaLotesDAO] Error de conexión al actualizar lote: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Ejecuta el auto-ajuste y nivelación automática de todos los artículos con discrepancia en 1 solo clic.
     * Retorna la cantidad de artículos nivelados exitosamente.
     */
    public int autoAjustarTodo(String coUsuario) throws SQLException {
        List<DiscrepanciaLoteDTO> discrepancias = listarDiscrepancias(null);
        if (discrepancias.isEmpty()) {
            return 0;
        }

        int ajustados = 0;
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);

            try {
                for (DiscrepanciaLoteDTO d : discrepancias) {
                    double diff = d.getDiferencia();
                    String art = d.getCoArt();
                    String alma = d.getCoAlma();

                    if (diff > 0.0001) {
                        // Caso A: Stock Almacén > Stock Lotes (Faltan lotes)
                        // 1. Buscar si hay lote en saLoteEntrada para sumar la diferencia
                        String sqlBuscarLote = "SELECT TOP 1 rowguid FROM saLoteEntrada WHERE co_art = ? AND co_alma = ? ORDER BY fe_us_in DESC";
                        String targetRowguid = null;
                        try (PreparedStatement psB = conn.prepareStatement(sqlBuscarLote)) {
                            psB.setString(1, art);
                            psB.setString(2, alma);
                            try (ResultSet rsB = psB.executeQuery()) {
                                if (rsB.next()) {
                                    targetRowguid = rsB.getString("rowguid");
                                }
                            }
                        }

                        if (targetRowguid != null) {
                            String sqlUpd = "UPDATE saLoteEntrada SET cantidad = cantidad + ?, stock_actual = stock_actual + ?, co_us_mo = ?, fe_us_mo = GETDATE() WHERE rowguid = CAST(? AS UNIQUEIDENTIFIER)";
                            try (PreparedStatement psU = conn.prepareStatement(sqlUpd)) {
                                psU.setDouble(1, diff);
                                psU.setDouble(2, diff);
                                psU.setString(3, coUsuario != null ? coUsuario : "DROAI");
                                psU.setString(4, targetRowguid);
                                psU.executeUpdate();
                            }
                        } else {
                            // Insertar nuevo lote provisional
                            String sqlIns = """
                                INSERT INTO saLoteEntrada (
                                    rowguid_reng, reng_num, tipo_doc, co_art, co_alma, numero_lote,
                                    fecha_inicio, fecha_expiracion, cantidad, stock_actual, precio,
                                    co_us_in, co_sucu_in, fe_us_in, co_us_mo, co_sucu_mo, fe_us_mo,
                                    revisado, trasnfe, rowguid
                                ) VALUES (
                                    NEWID(), 1, 'AJUS', ?, ?, 'LOTE-AUTO',
                                    GETDATE(), DATEADD(year, 3, GETDATE()), ?, ?, 0,
                                    ?, '01', GETDATE(), ?, '01', GETDATE(),
                                    NULL, NULL, NEWID()
                                )
                            """;
                            try (PreparedStatement psI = conn.prepareStatement(sqlIns)) {
                                psI.setString(1, art);
                                psI.setString(2, alma);
                                psI.setDouble(3, diff);
                                psI.setDouble(4, diff);
                                psI.setString(5, coUsuario != null ? coUsuario : "DROAI");
                                psI.setString(6, coUsuario != null ? coUsuario : "DROAI");
                                psI.executeUpdate();
                            }
                        }
                        ajustados++;

                    } else if (diff < -0.0001) {
                        // Caso B: Stock Lotes > Stock Almacén (Exceso en lotes por salidas no descargadas)
                        double exceso = Math.abs(diff);

                        // Buscar los lotes con stock_actual > 0 para ir rebajando el exceso
                        String sqlLotesExceso = "SELECT CAST(rowguid AS VARCHAR(36)) AS rowguid, stock_actual FROM saLoteEntrada WHERE co_art = ? AND co_alma = ? AND stock_actual > 0 ORDER BY fecha_expiracion ASC";
                        try (PreparedStatement psLE = conn.prepareStatement(sqlLotesExceso)) {
                            psLE.setString(1, art);
                            psLE.setString(2, alma);
                            try (ResultSet rsLE = psLE.executeQuery()) {
                                while (rsLE.next() && exceso > 0.0001) {
                                    String rg = rsLE.getString("rowguid");
                                    double stkAct = rsLE.getDouble("stock_actual");
                                    double aRebajar = Math.min(stkAct, exceso);

                                    String sqlReb = "UPDATE saLoteEntrada SET stock_actual = stock_actual - ?, co_us_mo = ?, fe_us_mo = GETDATE() WHERE rowguid = CAST(? AS UNIQUEIDENTIFIER)";
                                    try (PreparedStatement psR = conn.prepareStatement(sqlReb)) {
                                        psR.setDouble(1, aRebajar);
                                        psR.setString(2, coUsuario != null ? coUsuario : "DROAI");
                                        psR.setString(3, rg);
                                        psR.executeUpdate();
                                    }
                                    exceso -= aRebajar;
                                }
                            }
                        }
                        ajustados++;
                    }
                }

                // Sincronizar marcas lote_asignado = 1 en recepciones que ya quedaron cubiertas
                String sqlSyncNrec = """
                    UPDATE r
                    SET r.lote_asignado = 1
                    FROM saNotaRecepcionCompraReng r
                    JOIN saLoteEntrada le ON r.rowguid = le.rowguid_reng
                    WHERE r.lote_asignado = 0
                """;
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate(sqlSyncNrec);
                }

                conn.commit();
                logger.info("[AuditoriaLotesDAO] Auto-ajuste completado exitosamente: {} artículos nivelados.", ajustados);
            } catch (SQLException ex) {
                conn.rollback();
                logger.error("[AuditoriaLotesDAO] Error en transacción de auto-ajuste (ROLLBACK): {}", ex.getMessage(), ex);
                throw ex;
            }
        }
        return ajustados;
    }
}

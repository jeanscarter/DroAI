package com.droai.dao;

import com.droai.config.DatabaseConfig;
import com.droai.model.AjusteProductoDTO;
import com.droai.model.ArticuloRow;
import com.droai.model.SesionUsuario;
import com.droai.model.StockAlmacenRow;
import com.droai.model.StockLoteRow;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object para el módulo de Ajustes de Inventario.
 * Permite buscar productos, consultar stock total y desgloses por almacén/lote,
 * obtener almacenes y procesar ajustes de entrada/salida.
 */
public class AjusteInventarioDAO {

    private final ArticuloDAO articuloDAO = new ArticuloDAO();

    /**
     * Busca productos reutilizando el DAO principal de Artículos (ArticuloDAO).
     */
    public List<AjusteProductoDTO> buscarProductos(String query) throws SQLException {
        List<ArticuloRow> articulos = articuloDAO.buscarProductos(query);
        List<AjusteProductoDTO> list = new ArrayList<>();
        for (ArticuloRow r : articulos) {
            AjusteProductoDTO dto = new AjusteProductoDTO();
            dto.setCodigo(r.getCodigo() != null ? r.getCodigo().trim() : "");
            dto.setDescripcion(r.getDescripcion() != null ? r.getDescripcion().trim() : "");
            dto.setMarca(r.getMarca() != null ? r.getMarca().trim() : "");
            dto.setCodigoBarra(r.getCodigoBarra() != null ? r.getCodigoBarra().trim() : "");
            dto.setUdm(r.getUdm() != null ? r.getUdm().trim() : "");
            dto.setCostoActual(r.getCostoActual());
            dto.setPrecio1(r.getPrecio1());
            dto.setIvaPct(r.getIvaPct());
            dto.setPrecioCiva(r.getPrecioCiva() > 0 ? r.getPrecioCiva() : r.getPrecio1() * (1.0 + (r.getIvaPct() / 100.0)));
            dto.setStockTotal(r.getExistencia());
            list.add(dto);
        }
        return list;
    }

    /**
     * Obtiene el desglose completo de stock por almacén y por lote para un código de producto.
     */
    public AjusteProductoDTO obtenerDetalleProducto(String codigo) throws SQLException {
        if (codigo == null || codigo.isBlank()) return null;

        List<AjusteProductoDTO> baseList = buscarProductos(codigo.trim());
        AjusteProductoDTO dto = null;
        for (AjusteProductoDTO d : baseList) {
            if (d.getCodigo().equalsIgnoreCase(codigo.trim())) {
                dto = d;
                break;
            }
        }
        if (dto == null && !baseList.isEmpty()) {
            dto = baseList.get(0);
        }
        if (dto == null) return null;

        // 1. Stock por Almacén
        List<StockAlmacenRow> stockAlmacen = new ArrayList<>();
        String sqlAlmacen = """
            SELECT sa.co_alma, RTRIM(ISNULL(a.des_alma, sa.co_alma)) AS des_alma, ISNULL(sa.stock, 0) AS stock
            FROM saStockAlmacen sa
            LEFT JOIN saAlmacen a ON sa.co_alma = a.co_alma
            WHERE sa.co_art = ?
            ORDER BY sa.co_alma
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlAlmacen)) {
            ps.setString(1, dto.getCodigo());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    stockAlmacen.add(new StockAlmacenRow(
                            rs.getString("co_alma").trim(),
                            rs.getString("des_alma").trim(),
                            rs.getDouble("stock")
                    ));
                }
            }
        }
        dto.setStockPorAlmacen(stockAlmacen);

        // Recalcular stock total acumulado de todos los almacenes
        double totalAcc = stockAlmacen.stream().mapToDouble(StockAlmacenRow::getStock).sum();
        dto.setStockTotal(totalAcc);

        // 2. Stock por Lote y Fecha de Vencimiento usando el cálculo oficial de Profit Plus
        List<StockLoteRow> stockLote = new ArrayList<>();
        String sqlLote = """
            SELECT v.co_alma, RTRIM(ISNULL(a.des_alma, v.co_alma)) AS des_alma,
                   RTRIM(ISNULL(v.numero_lote, 'S/L')) AS numero_lote,
                   v.fecha_expiracion,
                   dbo.ConsultarStockActualxAlmacenxFechaxLote(v.co_art, v.co_alma, GETDATE(), NULL, NULL, v.numero_lote) AS stock_actual
            FROM saLoteEntrada v
            LEFT JOIN saAlmacen a ON v.co_alma = a.co_alma
            WHERE RTRIM(v.co_art) = ?
            GROUP BY v.co_art, v.co_alma, a.des_alma, v.numero_lote, v.fecha_expiracion
            ORDER BY v.fecha_expiracion ASC, v.numero_lote
            """;

        DateTimeFormatter dtfOut = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlLote)) {
            ps.setString(1, dto.getCodigo());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("fecha_expiracion");
                    String fechaStr = "";
                    if (ts != null) {
                        LocalDate ld = ts.toLocalDateTime().toLocalDate();
                        if (ld.getYear() > 1900) {
                            fechaStr = ld.format(dtfOut);
                        } else {
                            fechaStr = "N/A";
                        }
                    }
                    stockLote.add(new StockLoteRow(
                            dto.getCodigo(),
                            rs.getString("co_alma").trim(),
                            rs.getString("des_alma").trim(),
                            rs.getString("numero_lote").trim(),
                            fechaStr,
                            rs.getDouble("stock_actual")
                    ));
                }
            }
        }
        dto.setStockPorLote(stockLote);

        return dto;
    }

    /**
     * Obtiene todos los almacenes registrados en el sistema.
     */
    public List<StockAlmacenRow> obtenerAlmacenes() throws SQLException {
        List<StockAlmacenRow> list = new ArrayList<>();
        String sql = "SELECT co_alma, RTRIM(ISNULL(des_alma, co_alma)) AS des_alma FROM saAlmacen ORDER BY co_alma";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new StockAlmacenRow(
                        rs.getString("co_alma").trim(),
                        rs.getString("des_alma").trim(),
                        0.0
                ));
            }
        }
        return list;
    }

    /**
     * Procesa un Ajuste de Inventario (Entrada o Salida).
     *
     * @param coArt Código del artículo
     * @param coAlma Código del almacén
     * @param tipoTrans "EA" para Entrada por Ajuste, "SA" para Salida por Ajuste
     * @param cantidad Cantidad a ajustar (positiva)
     * @param costoUnitario Costo unitario
     * @param motivo Observación / justificación del ajuste
     * @return true si se procesó correctamente
     */
    public boolean procesarAjuste(String coArt, String coAlma, String tipoTrans, double cantidad, double costoUnitario, String motivo) throws SQLException {
        return procesarAjuste(coArt, coAlma, tipoTrans, cantidad, costoUnitario, motivo, null, null);
    }

    public boolean procesarAjuste(String coArt, String coAlma, String tipoTrans, double cantidad, double costoUnitario, String motivo, String numeroLote) throws SQLException {
        return procesarAjuste(coArt, coAlma, tipoTrans, cantidad, costoUnitario, motivo, numeroLote, null);
    }

    private static final java.util.Set<String> USUARIOS_AJUSTE_PERMITIDOS = java.util.Set.of("JG", "OP", "JR", "ND");

    /**
     * Procesa un Ajuste de Inventario (Entrada o Salida) con soporte para Lote y Fecha de Vencimiento.
     */
    public boolean procesarAjuste(String coArt, String coAlma, String tipoTrans, double cantidad, double costoUnitario, String motivo, String numeroLote, java.util.Date fechaExpiracion) throws SQLException {
        if (coArt == null || coAlma == null || cantidad <= 0) {
            throw new IllegalArgumentException("Parámetros de ajuste no válidos");
        }

        String usuario = (SesionUsuario.isAutenticado() && SesionUsuario.current().getCoUsuario() != null)
                ? SesionUsuario.current().getCoUsuario().trim()
                : "";

        if (!USUARIOS_AJUSTE_PERMITIDOS.contains(usuario.toUpperCase())) {
            throw new SecurityException("No posee permisos para realizar ajustes de inventario.");
        }

        double deltaStock = tipoTrans.equalsIgnoreCase("SA") ? -cantidad : cantidad;

        boolean tieneLote = (numeroLote != null && !numeroLote.isBlank());
        String loteClean = tieneLote ? numeroLote.trim() : "";
        int loteAsignado = tieneLote ? 1 : 0;

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Obtener nuevo número correlativo para saAjuste
                String numAjuste = obtenerSiguienteNumeroAjuste(conn);

                // 2. Insertar cabecera saAjuste
                String sqlAjuste = """
                    INSERT INTO saAjuste (ajue_num, fecha, motivo, co_mone, tasa, anulado, fe_us_in, co_us_in, fe_us_mo, co_us_mo)
                    VALUES (?, GETDATE(), ?, 'BS', 1.0, 0, GETDATE(), ?, GETDATE(), ?)
                    """;
                try (PreparedStatement ps = conn.prepareStatement(sqlAjuste)) {
                    ps.setString(1, numAjuste);
                    ps.setString(2, motivo != null && !motivo.isBlank() ? motivo : (tipoTrans.equalsIgnoreCase("EA") ? "Entrada por Ajuste DroAI" : "Salida por Ajuste DroAI"));
                    ps.setString(3, usuario);
                    ps.setString(4, usuario);
                    ps.executeUpdate();
                }

                // 3. Obtener unidad de medida del artículo (co_uni)
                String coUni = "UND";
                String sqlUni = "SELECT TOP 1 co_uni FROM saArtUnidad WHERE co_art = ?";
                try (PreparedStatement psUni = conn.prepareStatement(sqlUni)) {
                    psUni.setString(1, coArt);
                    try (ResultSet rsUni = psUni.executeQuery()) {
                        if (rsUni.next() && rsUni.getString("co_uni") != null && !rsUni.getString("co_uni").isBlank()) {
                            coUni = rsUni.getString("co_uni").trim();
                        }
                    }
                }

                // 4. Insertar renglón saAjusteReng
                String rowguidReng = java.util.UUID.randomUUID().toString();
                String sqlReng = """
                    INSERT INTO saAjusteReng (
                        ajue_num, reng_num, co_tipo, co_art, co_alma, co_uni,
                        total_art, stotal_art, cost_unit, lote_asignado,
                        costo_adi1, costo_adi2, costo_adi3,
                        fe_us_in, co_us_in, fe_us_mo, co_us_mo, rowguid
                    )
                    VALUES (?, 1, ?, ?, ?, ?, ?, ?, ?, ?, 0.0, 0.0, 0.0, GETDATE(), ?, GETDATE(), ?, ?)
                    """;
                try (PreparedStatement ps = conn.prepareStatement(sqlReng)) {
                    ps.setString(1, numAjuste);
                    ps.setString(2, tipoTrans.toUpperCase());
                    ps.setString(3, coArt);
                    ps.setString(4, coAlma);
                    ps.setString(5, coUni);
                    ps.setDouble(6, cantidad);
                    ps.setDouble(7, cantidad);
                    ps.setDouble(8, costoUnitario);
                    ps.setInt(9, loteAsignado);
                    ps.setString(10, usuario);
                    ps.setString(11, usuario);
                    ps.setString(12, rowguidReng);
                    ps.executeUpdate();
                }

                // 5. Actualizar o insertar en saStockAlmacen
                // Profit puede tener múltiples filas por (co_art, co_alma) con diferentes 'tipo' (ACT, LLE, etc.)
                // Intentamos UPDATE sobre la fila principal (tipo='ACT') primero, si no existe ninguna la creamos
                String sqlStockUpd = "UPDATE saStockAlmacen SET stock = stock + ? WHERE co_art = ? AND co_alma = ? AND tipo = 'ACT'";
                int stockRowsUpdated;
                try (PreparedStatement ps = conn.prepareStatement(sqlStockUpd)) {
                    ps.setDouble(1, deltaStock);
                    ps.setString(2, coArt);
                    ps.setString(3, coAlma);
                    stockRowsUpdated = ps.executeUpdate();
                }

                if (stockRowsUpdated == 0) {
                    // No existía fila tipo ACT, intentar sin filtro de tipo
                    String sqlStockUpdAny = "UPDATE TOP(1) saStockAlmacen SET stock = stock + ? WHERE co_art = ? AND co_alma = ?";
                    try (PreparedStatement ps = conn.prepareStatement(sqlStockUpdAny)) {
                        ps.setDouble(1, deltaStock);
                        ps.setString(2, coArt);
                        ps.setString(3, coAlma);
                        stockRowsUpdated = ps.executeUpdate();
                    }
                }

                if (stockRowsUpdated == 0) {
                    // No existe ninguna fila para este artículo/almacén, insertar nueva
                    String sqlStockIns = "INSERT INTO saStockAlmacen (co_art, co_alma, stock, tipo) VALUES (?, ?, ?, 'ACT')";
                    try (PreparedStatement ps = conn.prepareStatement(sqlStockIns)) {
                        ps.setString(1, coArt);
                        ps.setString(2, coAlma);
                        ps.setDouble(3, Math.max(0, deltaStock));
                        ps.executeUpdate();
                    }
                }

                // 6. Actualizar stock por Lote en saLoteEntrada y saLoteSalida siguiendo el modelo de Profit Plus
                String usuarioLote = usuario.length() > 6 ? usuario.substring(0, 6) : usuario;

                if (tieneLote) {
                    String sqlLoteCheck = "SELECT COUNT(*) FROM saLoteEntrada WHERE RTRIM(co_art) = ? AND RTRIM(co_alma) = ? AND RTRIM(numero_lote) = ?";
                    boolean existeLote = false;
                    try (PreparedStatement ps = conn.prepareStatement(sqlLoteCheck)) {
                        ps.setString(1, coArt);
                        ps.setString(2, coAlma);
                        ps.setString(3, loteClean);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next() && rs.getInt(1) > 0) {
                                existeLote = true;
                            }
                        }
                    }

                    if (tipoTrans.equalsIgnoreCase("EA")) {
                        // En Profit Plus cada Entrada por Ajuste crea un nuevo renglón en saLoteEntrada vinculado a saAjusteReng
                        java.sql.Timestamp fVencTs = null;
                        if (fechaExpiracion != null) {
                            fVencTs = new java.sql.Timestamp(fechaExpiracion.getTime());
                        } else {
                            // Buscar fecha de vencimiento previa de este lote si ya existía
                            String sqlFecExistente = "SELECT TOP 1 fecha_expiracion FROM saLoteEntrada WHERE RTRIM(co_art) = ? AND RTRIM(co_alma) = ? AND RTRIM(numero_lote) = ? AND fecha_expiracion IS NOT NULL ORDER BY fe_us_in DESC";
                            try (PreparedStatement psFec = conn.prepareStatement(sqlFecExistente)) {
                                psFec.setString(1, coArt);
                                psFec.setString(2, coAlma);
                                psFec.setString(3, loteClean);
                                try (ResultSet rsFec = psFec.executeQuery()) {
                                    if (rsFec.next() && rsFec.getTimestamp(1) != null) {
                                        fVencTs = rsFec.getTimestamp(1);
                                    }
                                }
                            }
                            if (fVencTs == null) {
                                java.util.Calendar cal = java.util.Calendar.getInstance();
                                cal.add(java.util.Calendar.YEAR, 2);
                                fVencTs = new java.sql.Timestamp(cal.getTimeInMillis());
                            }
                        }

                        String sqlLoteIns = """
                            INSERT INTO saLoteEntrada (
                                co_art, co_alma, numero_lote, tipo_doc, reng_num,
                                cantidad, stock_actual, precio, fecha_inicio, fecha_expiracion,
                                fe_us_in, co_us_in, fe_us_mo, co_us_mo, rowguid_reng, rowguid
                            ) VALUES (
                                ?, ?, ?, 'AJUS', 1,
                                ?, ?, ?, GETDATE(), ?,
                                GETDATE(), ?, GETDATE(), ?, ?, NEWID()
                            )
                            """;
                        try (PreparedStatement ps = conn.prepareStatement(sqlLoteIns)) {
                            ps.setString(1, coArt);
                            ps.setString(2, coAlma);
                            ps.setString(3, loteClean);
                            ps.setDouble(4, cantidad);
                            ps.setDouble(5, cantidad);
                            ps.setDouble(6, costoUnitario);
                            ps.setTimestamp(7, fVencTs);
                            ps.setString(8, usuarioLote);
                            ps.setString(9, usuarioLote);
                            ps.setString(10, rowguidReng);
                            ps.executeUpdate();
                        }
                    } else if (tipoTrans.equalsIgnoreCase("SA")) {
                        // Salida de Ajuste (SA) con lote explícito
                        if (existeLote) {
                            String sqlLoteUpd = "UPDATE saLoteEntrada SET stock_actual = CASE WHEN stock_actual - ? < 0 THEN 0 ELSE stock_actual - ? END, fe_us_mo = GETDATE(), co_us_mo = ? WHERE RTRIM(co_art) = ? AND RTRIM(co_alma) = ? AND RTRIM(numero_lote) = ?";
                            try (PreparedStatement ps = conn.prepareStatement(sqlLoteUpd)) {
                                ps.setDouble(1, cantidad);
                                ps.setDouble(2, cantidad);
                                ps.setString(3, usuarioLote);
                                ps.setString(4, coArt);
                                ps.setString(5, coAlma);
                                ps.setString(6, loteClean);
                                ps.executeUpdate();
                            }
                        }
                        // Registrar en saLoteSalida para que la función de cálculo de Profit descuente las salidas
                        String sqlLoteSalidaIns = """
                            INSERT INTO saLoteSalida (
                                rowguid_reng, reng_num, tipo_doc, co_art, co_alma, numero_lote,
                                Rowguid_Lote, cantidad, precio, co_us_in, fe_us_in, co_us_mo, fe_us_mo, rowguid
                            ) VALUES (
                                ?, 1, 'AJUS', ?, ?, ?,
                                ISNULL((SELECT TOP 1 rowguid FROM saLoteEntrada WHERE RTRIM(co_art) = ? AND RTRIM(co_alma) = ? AND RTRIM(numero_lote) = ?), NEWID()),
                                ?, ?, ?, GETDATE(), ?, GETDATE(), NEWID()
                            )
                            """;
                        try (PreparedStatement psSal = conn.prepareStatement(sqlLoteSalidaIns)) {
                            psSal.setString(1, rowguidReng);
                            psSal.setString(2, coArt);
                            psSal.setString(3, coAlma);
                            psSal.setString(4, loteClean);
                            psSal.setString(5, coArt);
                            psSal.setString(6, coAlma);
                            psSal.setString(7, loteClean);
                            psSal.setDouble(8, cantidad);
                            psSal.setDouble(9, costoUnitario);
                            psSal.setString(10, usuarioLote);
                            psSal.setString(11, usuarioLote);
                            psSal.executeUpdate();
                        }
                    }
                } else if (tipoTrans.equalsIgnoreCase("SA")) {
                    // Si es una Salida (SA) sin lote explícito, aplicar algoritmo FEFO para descontar de saLoteEntrada y registrar saLoteSalida
                    String sqlFefo = """
                        SELECT numero_lote, stock_actual, CAST(rowguid AS VARCHAR(36)) AS rowguid_str
                        FROM saLoteEntrada
                        WHERE RTRIM(co_art) = ? AND RTRIM(co_alma) = ? AND stock_actual > 0
                        ORDER BY fecha_expiracion ASC, numero_lote ASC
                        """;
                    List<Object[]> lotesFefo = new ArrayList<>();
                    try (PreparedStatement psFefo = conn.prepareStatement(sqlFefo)) {
                        psFefo.setString(1, coArt);
                        psFefo.setString(2, coAlma);
                        try (ResultSet rsFefo = psFefo.executeQuery()) {
                            while (rsFefo.next()) {
                                lotesFefo.add(new Object[]{ rsFefo.getString("numero_lote").trim(), rsFefo.getDouble("stock_actual"), rsFefo.getString("rowguid_str") });
                            }
                        }
                    }

                    if (!lotesFefo.isEmpty()) {
                        double pendienteDescuento = cantidad;
                        String sqlUpdFefo = "UPDATE saLoteEntrada SET stock_actual = stock_actual - ?, fe_us_mo = GETDATE(), co_us_mo = ? WHERE RTRIM(co_art) = ? AND RTRIM(co_alma) = ? AND RTRIM(numero_lote) = ?";
                        String sqlLoteSalidaFefo = """
                            INSERT INTO saLoteSalida (
                                rowguid_reng, reng_num, tipo_doc, co_art, co_alma, numero_lote,
                                Rowguid_Lote, cantidad, precio, co_us_in, fe_us_in, co_us_mo, fe_us_mo, rowguid
                            ) VALUES (
                                ?, 1, 'AJUS', ?, ?, ?,
                                ?, ?, ?, ?, GETDATE(), ?, GETDATE(), NEWID()
                            )
                            """;
                        try (PreparedStatement psUpdFefo = conn.prepareStatement(sqlUpdFefo);
                             PreparedStatement psSalFefo = conn.prepareStatement(sqlLoteSalidaFefo)) {
                            for (Object[] loteInfo : lotesFefo) {
                                if (pendienteDescuento <= 0) break;
                                String lNum = (String) loteInfo[0];
                                double stLote = (Double) loteInfo[1];
                                String rowguidLote = (String) loteInfo[2];
                                double aDescontar = Math.min(pendienteDescuento, stLote);

                                psUpdFefo.setDouble(1, aDescontar);
                                psUpdFefo.setString(2, usuarioLote);
                                psUpdFefo.setString(3, coArt);
                                psUpdFefo.setString(4, coAlma);
                                psUpdFefo.setString(5, lNum);
                                psUpdFefo.executeUpdate();

                                psSalFefo.setString(1, rowguidReng);
                                psSalFefo.setString(2, coArt);
                                psSalFefo.setString(3, coAlma);
                                psSalFefo.setString(4, lNum);
                                psSalFefo.setString(5, rowguidLote);
                                psSalFefo.setDouble(6, aDescontar);
                                psSalFefo.setDouble(7, costoUnitario);
                                psSalFefo.setString(8, usuarioLote);
                                psSalFefo.setString(9, usuarioLote);
                                psSalFefo.executeUpdate();

                                pendienteDescuento -= aDescontar;
                            }
                        }
                    }
                }

                conn.commit();
                return true;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private String obtenerSiguienteNumeroAjuste(Connection conn) throws SQLException {
        String sql = "SELECT ISNULL(MAX(CAST(RTRIM(ajue_num) AS INT)), 0) + 1 FROM saAjuste WHERE ISNUMERIC(ajue_num) = 1";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                int nextVal = rs.getInt(1);
                return String.format("%010d", nextVal);
            }
        }
        return "0000000001";
    }
}

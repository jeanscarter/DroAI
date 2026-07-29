package com.droai.dao;

import com.droai.config.DatabaseConfig;
import com.droai.model.CargaMasivaCostosPreciosRow;
import com.droai.model.SesionUsuario;

import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CargaMasivaCostosPreciosDAO {

    /**
     * Completa las filas leídas del Excel con los datos actuales de la BD (descripción, costo actual, precio 1 actual)
     * y marca si el artículo existe en saArticulo.
     */
    public void enriquecerConDatosBd(List<CargaMasivaCostosPreciosRow> filas) throws SQLException {
        if (filas == null || filas.isEmpty()) {
            return;
        }

        String sqlArticulo = """
                SELECT a.co_art, ISNULL(a.art_des, '') AS descripcion, a.rowguid,
                       ISNULL(ce.costo, 0) AS costoActual,
                       ISNULL(p1.monto, 0) AS precio1Actual
                FROM saArticulo a
                LEFT JOIN (
                    SELECT cod_articulo_rowguid, costo
                    FROM (
                        SELECT cod_articulo_rowguid, costo,
                               ROW_NUMBER() OVER (
                                   PARTITION BY cod_articulo_rowguid
                                   ORDER BY CASE WHEN tipo_doc = 'PROV' THEN 1 ELSE 2 END, fecha_emision DESC
                               ) AS rn
                        FROM saCostoHistoricoEntrada
                        WHERE costo > 0
                    ) ranked
                    WHERE rn = 1
                ) ce ON a.rowguid = ce.cod_articulo_rowguid
                LEFT JOIN (
                    SELECT co_art, MAX(monto) AS monto
                    FROM saArtPrecio
                    WHERE co_precio = '01'
                    GROUP BY co_art
                ) p1 ON a.co_art = p1.co_art
                WHERE a.co_art = ?
                """;

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlArticulo)) {

            for (CargaMasivaCostosPreciosRow row : filas) {
                if (row.getCoArt() == null || row.getCoArt().isBlank()) {
                    row.setValido(false);
                    row.setExisteEnBd(false);
                    row.setEstado("❌ Código vacío");
                    continue;
                }

                ps.setString(1, row.getCoArt().trim());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        row.setExisteEnBd(true);
                        row.setDescripcion(rs.getString("descripcion"));
                        row.setCostoActual(rs.getDouble("costoActual"));
                        row.setPrecio1Actual(rs.getDouble("precio1Actual"));

                        if (row.tieneCambios()) {
                            row.setValido(true);
                            row.setEstado("✔ Listo para actualizar");
                        } else {
                            row.setValido(true);
                            row.setEstado("⚠️ Sin cambios detectados");
                        }
                    } else {
                        row.setExisteEnBd(false);
                        row.setValido(false);
                        row.setDescripcion("--- No Encontrado ---");
                        row.setEstado("❌ Código no existe en BD");
                    }
                }
            }
        }
    }

    /**
     * Aplica la carga masiva en lote dentro de una sola transacción SQL.
     * Actualiza costos en saCostoHistoricoEntrada y precios en saArtPrecio.
     *
     * @return Número de registros procesados con éxito.
     */
    public int ejecutarCargaMasiva(List<CargaMasivaCostosPreciosRow> filas) throws SQLException {
        if (filas == null || filas.isEmpty()) {
            return 0;
        }

        String usuario = (SesionUsuario.current() != null && SesionUsuario.current().getCoUsuario() != null)
                ? SesionUsuario.current().getCoUsuario().trim()
                : "ADMIN";

        String sqlGetRowguid = "SELECT rowguid FROM saArticulo WHERE co_art = ?";

        String sqlInsertCosto = """
                INSERT INTO saCostoHistoricoEntrada (
                    cod_costo_historico_entrada, cod_articulo_rowguid, cod_almacen,
                    tipo_doc, doc_orig, cantidad, cantidad_usada, costo, costo_pro,
                    fecha_emision, fecha_registro, rengNum
                ) VALUES (
                    NEWID(), ?, '01', 'PROV', NEWID(), 1, 0, ?, ?, GETDATE(), GETDATE(), 1
                )
                """;

        String sqlUpdatePrecio = """
                UPDATE saArtPrecio
                SET monto = ?, fe_us_mo = GETDATE(), co_us_mo = ?
                WHERE co_art = ? AND co_precio = '01'
                """;

        String sqlInsertPrecio = """
                INSERT INTO saArtPrecio (
                    co_art, co_precio, co_alma_calculado, desde, monto,
                    precioOm, co_us_in, fe_us_in, co_us_mo, fe_us_mo, Inactivo, rowguid
                ) VALUES (
                    ?, '01', ' ', GETDATE(), ?,
                    0, ?, GETDATE(), ?, GETDATE(), 0, NEWID()
                )
                """;

        int totalActualizados = 0;

        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement psRowguid = conn.prepareStatement(sqlGetRowguid);
                 PreparedStatement psCosto = conn.prepareStatement(sqlInsertCosto);
                 PreparedStatement psUpdPrecio = conn.prepareStatement(sqlUpdatePrecio);
                 PreparedStatement psInsPrecio = conn.prepareStatement(sqlInsertPrecio)) {

                for (CargaMasivaCostosPreciosRow row : filas) {
                    if (!row.isValido() || !row.isExisteEnBd()) {
                        continue;
                    }

                    boolean huboActualizacion = false;
                    String coArt = row.getCoArt().trim();

                    // 1. Actualizar Costo si cambió
                    if (row.getCostoNuevo() > 0 && Math.abs(row.getCostoNuevo() - row.getCostoActual()) > 0.0001) {
                        psRowguid.setString(1, coArt);
                        try (ResultSet rs = psRowguid.executeQuery()) {
                            if (rs.next()) {
                                Object rowguidObj = rs.getObject("rowguid");
                                psCosto.setObject(1, rowguidObj);
                                psCosto.setDouble(2, row.getCostoNuevo());
                                psCosto.setDouble(3, row.getCostoNuevo());
                                psCosto.executeUpdate();
                                huboActualizacion = true;
                            }
                        }
                    }

                    // 2. Actualizar / Insertar Precio 1 si cambió
                    if (row.getPrecio1Nuevo() > 0 && Math.abs(row.getPrecio1Nuevo() - row.getPrecio1Actual()) > 0.0001) {
                        psUpdPrecio.setDouble(1, row.getPrecio1Nuevo());
                        psUpdPrecio.setString(2, usuario);
                        psUpdPrecio.setString(3, coArt);
                        int rowsUpd = psUpdPrecio.executeUpdate();

                        if (rowsUpd == 0) {
                            psInsPrecio.setString(1, coArt);
                            psInsPrecio.setDouble(2, row.getPrecio1Nuevo());
                            psInsPrecio.setString(3, usuario);
                            psInsPrecio.setString(4, usuario);
                            psInsPrecio.executeUpdate();
                        }
                        huboActualizacion = true;
                    }

                    if (huboActualizacion) {
                        totalActualizados++;
                        row.setEstado("✅ Actualizado con éxito");
                    }
                }

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw new SQLException("Error durante la transacción de carga masiva: " + e.getMessage(), e);
            }
        }

        return totalActualizados;
    }
}

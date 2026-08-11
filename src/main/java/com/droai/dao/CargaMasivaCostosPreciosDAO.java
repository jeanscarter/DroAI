package com.droai.dao;

import com.droai.config.DatabaseConfig;
import com.droai.model.CargaMasivaCostosPreciosRow;
import com.droai.model.SesionUsuario;

import java.sql.*;
import java.util.List;

public class CargaMasivaCostosPreciosDAO {

    /**
     * Consulta la tasa de cambio vigente para USD desde Profit (tabla saTasa o fallback a saMoneda).
     */
    public double obtenerTasaUSD() throws SQLException {
        String sqlTasa = """
                SELECT TOP 1 ISNULL(tasa_v, tasa_c) AS tasa
                FROM saTasa
                WHERE co_mone IN ('USD', 'US$')
                ORDER BY fecha DESC, fe_us_mo DESC
                """;

        String sqlMonedaFallback = """
                SELECT TOP 1 ISNULL(cambio, 1) AS tasa
                FROM saMoneda
                WHERE co_mone IN ('USD', 'US$') OR mone_des LIKE '%DOLAR%'
                ORDER BY CASE WHEN co_mone = 'USD' THEN 1 ELSE 2 END
                """;

        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(sqlTasa);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double t = rs.getDouble("tasa");
                    if (t > 0) return t;
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(sqlMonedaFallback);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double t = rs.getDouble("tasa");
                    if (t > 0) return t;
                }
            }
        }
        return 1.0;
    }

    /**
     * Completa las filas leídas del Excel con los datos actuales de la BD (descripción, costo actual, precio 1 actual),
     * calcula los equivalentes en Bs y $ con la tasa de Profit y marca si el artículo existe.
     */
    public void enriquecerConDatosBd(List<CargaMasivaCostosPreciosRow> filas) throws SQLException {
        if (filas == null || filas.isEmpty()) {
            return;
        }

        double tasaUsd = obtenerTasaUSD();

        String sqlArticulo = """
                SELECT a.co_art, ISNULL(a.art_des, '') AS descripcion, a.rowguid,
                       ISNULL(ce.costo, 0) AS costoActual,
                       ISNULL(p1.monto, 0) AS precio1MontoActual,
                       ISNULL(p1.precioOm, 0) AS precio1OmActual
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
                    SELECT co_art, monto, CAST(precioOm AS int) AS precioOm
                    FROM (
                        SELECT co_art, monto, ISNULL(precioOm, 0) AS precioOm,
                               ROW_NUMBER() OVER (PARTITION BY co_art ORDER BY desde DESC) AS rn
                        FROM saArtPrecio
                        WHERE co_precio = '01'
                    ) ranked
                    WHERE rn = 1
                ) p1 ON a.co_art = p1.co_art
                WHERE LTRIM(RTRIM(a.co_art)) = ?
                   OR (LEN(?) <= 6 AND ISNUMERIC(?) = 1 AND LTRIM(RTRIM(a.co_art)) = RIGHT('000000' + ?, 6))
                """;

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlArticulo)) {

            for (CargaMasivaCostosPreciosRow row : filas) {
                row.setTasaUsd(tasaUsd);
                row.setCostoNuevoBs(row.getCostoNuevoUsd() * tasaUsd);
                row.setPrecio1NuevoBs(row.getPrecio1NuevoUsd() * tasaUsd);

                if (row.getCoArt() == null || row.getCoArt().isBlank()) {
                    row.setValido(false);
                    row.setExisteEnBd(false);
                    row.setEstado("❌ Código vacío");
                    continue;
                }

                String codeClean = row.getCoArt().trim();
                ps.setString(1, codeClean);
                ps.setString(2, codeClean);
                ps.setString(3, codeClean);
                ps.setString(4, codeClean);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        row.setCoArt(rs.getString("co_art").trim());
                        row.setExisteEnBd(true);
                        row.setDescripcion(rs.getString("descripcion").trim());

                        double cUsd = rs.getDouble("costoActual");
                        double pMonto = rs.getDouble("precio1MontoActual");
                        int pOm = rs.getInt("precio1OmActual");

                        double pUsd;
                        if (pOm == 1 && pMonto > 0 && tasaUsd > 0) {
                            pUsd = pMonto / tasaUsd;
                        } else if (pMonto > 0 && tasaUsd > 0) {
                            pUsd = pMonto > 500 ? (pMonto / tasaUsd) : pMonto;
                        } else {
                            pUsd = 0.0;
                        }

                        row.setCostoActualUsd(cUsd);
                        row.setCostoActualBs(cUsd * tasaUsd);
                        row.setCostoRawBd(cUsd);

                        row.setPrecio1ActualUsd(pUsd);
                        row.setPrecio1ActualBs(pUsd * tasaUsd);
                        row.setPrecio1MontoRawBd(pMonto);
                        row.setPrecioOmActual(pOm);

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
    public int ejecutarCargaMasiva(List<CargaMasivaCostosPreciosRow> filas, boolean forzar) throws SQLException {
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

        String sqlUpdateCostoArticulo = """
                UPDATE saArticulo
                SET prec_om = ?, co_us_mo = ?, fe_us_mo = GETDATE()
                WHERE co_art = ?
                """;

        String sqlUpdatePrecio = """
                UPDATE saArtPrecio
                SET monto = ?, precioOm = 1, fe_us_mo = GETDATE(), co_us_mo = ?
                WHERE co_art = ? AND co_precio = '01'
                """;

        String sqlInsertPrecio = """
                INSERT INTO saArtPrecio (
                    co_art, co_precio, desde, monto,
                    precioOm, co_us_in, fe_us_in, co_us_mo, fe_us_mo, Inactivo, rowguid
                ) VALUES (
                    ?, '01', GETDATE(), ?,
                    1, ?, GETDATE(), ?, GETDATE(), 0, NEWID()
                )
                """;

        int totalActualizados = 0;

        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement psRowguid = conn.prepareStatement(sqlGetRowguid);
                 PreparedStatement psCosto = conn.prepareStatement(sqlInsertCosto);
                 PreparedStatement psUpdCostoArt = conn.prepareStatement(sqlUpdateCostoArticulo);
                 PreparedStatement psUpdPrecio = conn.prepareStatement(sqlUpdatePrecio);
                 PreparedStatement psInsPrecio = conn.prepareStatement(sqlInsertPrecio)) {

                for (CargaMasivaCostosPreciosRow row : filas) {
                    if (!row.isValido() || !row.isExisteEnBd()) {
                        continue;
                    }

                    boolean huboActualizacion = false;
                    String coArt = row.getCoArt().trim();

                    // 1. Actualizar Costo (en USD $) si cambió o si se fuerza
                    // Comparar contra el costo RAW de BD
                    if (row.getCostoNuevoUsd() > 0 && (forzar || Math.abs(row.getCostoNuevoUsd() - row.getCostoRawBd()) > 0.0001)) {
                        psRowguid.setString(1, coArt);
                        try (ResultSet rs = psRowguid.executeQuery()) {
                            if (rs.next()) {
                                Object rowguidObj = rs.getObject("rowguid");
                                psCosto.setObject(1, rowguidObj);
                                psCosto.setDouble(2, row.getCostoNuevoUsd());
                                psCosto.setDouble(3, row.getCostoNuevoUsd());
                                psCosto.executeUpdate();
                            }
                        }

                        psUpdCostoArt.setDouble(1, row.getCostoNuevoUsd());
                        psUpdCostoArt.setString(2, usuario);
                        psUpdCostoArt.setString(3, coArt);
                        psUpdCostoArt.executeUpdate();

                        huboActualizacion = true;
                    }

                    // 2. Actualizar / Insertar Precio 1 (monto en Bs, precioOm = 1) si cambió o si se fuerza
                    // Comparar contra el monto RAW de BD para detectar cambios de formato (precioOm 0→1)
                    if (row.getPrecio1NuevoUsd() > 0 && (forzar || Math.abs(row.getPrecio1NuevoBs() - row.getPrecio1MontoRawBd()) > 0.01)) {
                        // UPDATE: monto (Bs), co_us_mo, co_art
                        psUpdPrecio.setDouble(1, row.getPrecio1NuevoBs());
                        psUpdPrecio.setString(2, usuario);
                        psUpdPrecio.setString(3, coArt);
                        int rowsUpd = psUpdPrecio.executeUpdate();

                        if (rowsUpd == 0) {
                            // INSERT: co_art, monto (Bs), co_us_in, co_us_mo
                            psInsPrecio.setString(1, coArt);
                            psInsPrecio.setDouble(2, row.getPrecio1NuevoBs());
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

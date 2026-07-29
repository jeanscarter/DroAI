package com.droai.dao;

import com.droai.config.DatabaseConfig;
import com.droai.model.DescuentoProductoRow;
import com.droai.model.SesionUsuario;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DescuentoProductoDAO {

    private static final String SQL_GET_ALL = """
            SELECT
                a.co_art                        AS codigo,
                ISNULL(a.ref, '')               AS codigoBarra,
                ISNULL(a.art_des, '')           AS descripcion,
                ISNULL(sl.subl_des, '')         AS principioActivo,
                ISNULL(p.prov_des, '')          AS marca,
                ISNULL(a.prec_om, 0)            AS costoFabrica,
                ISNULL(a.porc_arancel, 0)       AS arancelPct,
                ISNULL(ce.costo, 0)             AS costoActual,
                ISNULL(p1.monto, 0)             AS precio1,
                CASE
                    WHEN ISNULL(p1.monto, 0) > 0
                    THEN ((ISNULL(p1.monto, 0) - ISNULL(ce.costo, 0)) / ISNULL(p1.monto, 0)) * 100
                    ELSE 0
                END                             AS utilidadPct,
                ISNULL(da.porc1, 0)             AS dctoPct,
                ISNULL(a.volumen, 0)            AS dctoPct2,
                CASE
                    WHEN ISNULL(a.volumen, 0) > 0
                    THEN ISNULL(p1.monto, 0) * (1 - (ISNULL(a.volumen, 0) / 100.0))
                    ELSE ISNULL(p1.monto, 0)
                END                             AS precioDcto,
                ISNULL(CONVERT(varchar, da.fecha_ini, 23), '') AS fechaDesde,
                ISNULL(CONVERT(varchar, da.fecha_fin, 23), '') AS fechaHasta,
                ISNULL(a.co_lin, '')            AS codLinea,
                ISNULL(l.lin_des, '')           AS linea,
                ISNULL(ap.co_prov, '')          AS codProveedor,
                ISNULL(p.prov_des, '')          AS nombreProveedor
            FROM saArticulo a
            LEFT JOIN (
                SELECT co_art, co_prov
                FROM (
                    SELECT co_art, co_prov,
                           ROW_NUMBER() OVER (PARTITION BY co_art ORDER BY reng_num) AS rn
                    FROM saArtProveedorReng
                ) ranked
                WHERE rn = 1
            ) ap ON a.co_art = ap.co_art
            LEFT JOIN saProveedor p ON ap.co_prov = p.co_prov
            LEFT JOIN saLineaArticulo l ON a.co_lin = l.co_lin
            LEFT JOIN saSubLinea sl ON a.co_subl = sl.co_subl AND a.co_lin = sl.co_lin
            LEFT JOIN (
                SELECT co_art, MAX(monto) AS monto
                FROM saArtPrecio
                WHERE co_precio = '01'
                GROUP BY co_art
            ) p1 ON a.co_art = p1.co_art
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
            LEFT JOIN saDescArticulo da ON a.co_art = da.co_art AND da.tip_cli = '000001'
            ORDER BY a.co_art
            """;

    public List<DescuentoProductoRow> fetchDescuentosProducto() throws SQLException {
        List<DescuentoProductoRow> rows = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_GET_ALL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                DescuentoProductoRow row = new DescuentoProductoRow();
                row.setCodigo(rs.getString("codigo").trim());
                row.setCodigoBarra(rs.getString("codigoBarra").trim());
                row.setDescripcion(rs.getString("descripcion").trim());
                row.setPrincipioActivo(rs.getString("principioActivo").trim());
                row.setMarca(rs.getString("marca").trim());
                row.setCostoFabrica(rs.getDouble("costoFabrica"));
                row.setArancelPct(rs.getDouble("arancelPct"));
                row.setCostoActual(rs.getDouble("costoActual"));
                row.setPrecio1(rs.getDouble("precio1"));
                row.setUtilidadPct(rs.getDouble("utilidadPct"));
                row.setDctoPct(rs.getDouble("dctoPct"));
                row.setDctoPct2(rs.getDouble("dctoPct2"));
                row.setPrecioDcto(rs.getDouble("precioDcto"));
                row.setFechaDesde(rs.getString("fechaDesde").trim());
                row.setFechaHasta(rs.getString("fechaHasta").trim());
                row.setCodLinea(rs.getString("codLinea").trim());
                row.setLinea(rs.getString("linea").trim());
                row.setCodProveedor(rs.getString("codProveedor").trim());
                row.setNombreProveedor(rs.getString("nombreProveedor").trim());
                rows.add(row);
            }
        }
        return rows;
    }

    public void updateDescuentosProducto(List<String> codigosArticulos, double dctoDA, double dctoDV) throws SQLException {
        if (codigosArticulos == null || codigosArticulos.isEmpty()) {
            return;
        }

        String coUsuario = SesionUsuario.isAutenticado()
                ? SesionUsuario.current().getCoUsuario() : "SYSTEM";

        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Actualizar el DA en saArticulo (columna volumen)
                String sqlUpdateArt = "UPDATE saArticulo SET volumen = ?, co_us_mo = ?, fe_us_mo = GETDATE() WHERE co_art = ?";
                try (PreparedStatement psUpdateArt = conn.prepareStatement(sqlUpdateArt)) {
                    for (String coArt : codigosArticulos) {
                        psUpdateArt.setDouble(1, dctoDA);
                        psUpdateArt.setString(2, coUsuario);
                        psUpdateArt.setString(3, coArt.trim());
                        psUpdateArt.addBatch();
                    }
                    psUpdateArt.executeBatch();
                }

                // 2. Obtener el consecutivo para saDescArticulo si hay inserciones
                int nextCoDesc = 1;
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT MAX(co_desc) FROM saDescArticulo")) {
                    if (rs.next()) {
                        String maxVal = rs.getString(1);
                        if (maxVal != null && !maxVal.trim().isEmpty()) {
                            try {
                                nextCoDesc = Integer.parseInt(maxVal.trim()) + 1;
                            } catch (NumberFormatException e) {
                                nextCoDesc = 30000;
                            }
                        }
                    }
                }

                // 3. Upsert del DV en saDescArticulo (columna porc1 para tip_cli 000001 a 000007)
                String sqlCheck = "SELECT COUNT(*) FROM saDescArticulo WHERE co_art = ? AND tip_cli = ?";
                String sqlUpdate = """
                        UPDATE saDescArticulo
                        SET porc1 = ?, co_us_mo = ?, fe_us_mo = GETDATE()
                        WHERE co_art = ? AND tip_cli = ?
                        """;
                String sqlInsert = """
                        INSERT INTO saDescArticulo
                            (co_desc, des_des, co_art, tip_cli,
                             hasta1, hasta2, hasta3, hasta4, hasta5,
                             porc1, porc2, porc3, porc4, porc5, porc6,
                             co_us_in, fe_us_in, co_us_mo, fe_us_mo, rowguid)
                        VALUES
                            (?, 'Descuento por Producto', ?, ?,
                             99999999.99, 99999999.99, 99999999.99, 0.0, 0.0,
                             ?, 0.0, 0.0, 0.0, 0.0, 0.0,
                             ?, GETDATE(), ?, GETDATE(), ?)
                        """;

                try (PreparedStatement psCheck = conn.prepareStatement(sqlCheck);
                     PreparedStatement psUpdate = conn.prepareStatement(sqlUpdate);
                     PreparedStatement psInsert = conn.prepareStatement(sqlInsert)) {

                    for (String coArt : codigosArticulos) {
                        String cleanCoArt = coArt.trim();

                        for (int i = 1; i <= 7; i++) {
                            String tipCli = String.format("%06d", i);

                            psCheck.setString(1, cleanCoArt);
                            psCheck.setString(2, tipCli);
                            boolean exists = false;
                            try (ResultSet rs = psCheck.executeQuery()) {
                                if (rs.next() && rs.getInt(1) > 0) {
                                    exists = true;
                                }
                            }

                            if (exists) {
                                psUpdate.setDouble(1, dctoDV);
                                psUpdate.setString(2, coUsuario);
                                psUpdate.setString(3, cleanCoArt);
                                psUpdate.setString(4, tipCli);
                                psUpdate.addBatch();
                            } else {
                                String coDesc = String.format("%06d", nextCoDesc++);
                                UUID guid = UUID.randomUUID();
                                psInsert.setString(1, coDesc);
                                psInsert.setString(2, cleanCoArt);
                                psInsert.setString(3, tipCli);
                                psInsert.setDouble(4, dctoDV);
                                psInsert.setString(5, coUsuario);
                                psInsert.setString(6, coUsuario);
                                psInsert.setString(7, guid.toString());
                                psInsert.addBatch();
                            }
                        }
                    }

                    psUpdate.executeBatch();
                    psInsert.executeBatch();
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public void updateDescuentosProductoDA(List<String> codigosArticulos, double dctoDA) throws SQLException {
        if (codigosArticulos == null || codigosArticulos.isEmpty()) {
            return;
        }

        String coUsuario = SesionUsuario.isAutenticado()
                ? SesionUsuario.current().getCoUsuario() : "SYSTEM";

        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            conn.setAutoCommit(false);
            try {
                String sqlUpdateArt = "UPDATE saArticulo SET volumen = ?, co_us_mo = ?, fe_us_mo = GETDATE() WHERE co_art = ?";
                try (PreparedStatement psUpdateArt = conn.prepareStatement(sqlUpdateArt)) {
                    for (String coArt : codigosArticulos) {
                        psUpdateArt.setDouble(1, dctoDA);
                        psUpdateArt.setString(2, coUsuario);
                        psUpdateArt.setString(3, coArt.trim());
                        psUpdateArt.addBatch();
                    }
                    psUpdateArt.executeBatch();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public void updateDescuentosProductoDAMap(java.util.Map<String, Double> dctosMap) throws SQLException {
        if (dctosMap == null || dctosMap.isEmpty()) {
            return;
        }

        String coUsuario = SesionUsuario.isAutenticado()
                ? SesionUsuario.current().getCoUsuario() : "SYSTEM";

        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            conn.setAutoCommit(false);
            try {
                String sqlUpdateArt = "UPDATE saArticulo SET volumen = ?, co_us_mo = ?, fe_us_mo = GETDATE() WHERE co_art = ?";
                try (PreparedStatement psUpdateArt = conn.prepareStatement(sqlUpdateArt)) {
                    for (java.util.Map.Entry<String, Double> entry : dctosMap.entrySet()) {
                        String coArt = entry.getKey().trim();
                        double nuevoPorcentaje = entry.getValue();
                        psUpdateArt.setDouble(1, nuevoPorcentaje);
                        psUpdateArt.setString(2, coUsuario);
                        psUpdateArt.setString(3, coArt);
                        psUpdateArt.addBatch();
                    }
                    psUpdateArt.executeBatch();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }
}

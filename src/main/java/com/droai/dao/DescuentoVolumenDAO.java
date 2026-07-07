package com.droai.dao;

import com.droai.config.DatabaseConfig;
import com.droai.model.DescuentoVolumenRow;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DescuentoVolumenDAO {

    private static final String SQL_GET_ALL = """
            SELECT 
                a.co_art AS codigo,
                ISNULL(a.art_des, '') AS descripcion,
                ISNULL(p.prov_des, '') AS marca,
                ISNULL(a.ref, '') AS codigoBarra,
                ISNULL(pr.monto, 0) AS precio1,
                ISNULL(da.porc1, 0) AS descuentoDV,
                ISNULL(ap.co_prov, '') AS codProveedor,
                ISNULL(p.prov_des, '') AS nombreProveedor,
                ISNULL(a.co_lin, '') AS codLinea,
                ISNULL(l.lin_des, '') AS linea
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
            LEFT JOIN (
                SELECT co_art, MAX(monto) AS monto 
                FROM saArtPrecio 
                WHERE co_precio = '01' 
                GROUP BY co_art
            ) pr ON a.co_art = pr.co_art
            LEFT JOIN saDescArticulo da ON a.co_art = da.co_art AND da.tip_cli = '000001' AND da.fecha_ini IS NULL
            ORDER BY a.co_art
            """;

    public List<DescuentoVolumenRow> fetchDescuentosVolumen() throws SQLException {
        List<DescuentoVolumenRow> rows = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_GET_ALL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                DescuentoVolumenRow row = new DescuentoVolumenRow();
                row.setCodigo(rs.getString("codigo").trim());
                row.setDescripcion(rs.getString("descripcion").trim());
                row.setMarca(rs.getString("marca").trim());
                row.setCodigoBarra(rs.getString("codigoBarra").trim());
                row.setPrecio1(rs.getDouble("precio1"));
                row.setDescuentoDV(rs.getDouble("descuentoDV"));
                row.setCodProveedor(rs.getString("codProveedor").trim());
                row.setNombreProveedor(rs.getString("nombreProveedor").trim());
                row.setCodLinea(rs.getString("codLinea").trim());
                row.setLinea(rs.getString("linea").trim());
                rows.add(row);
            }
        }
        return rows;
    }

    public void updateDescuentosVolumen(List<String> codigosArticulos, double nuevoPorcentaje) throws SQLException {
        if (codigosArticulos == null || codigosArticulos.isEmpty()) {
            return;
        }

        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Obtener el máximo co_desc actual para posibles inserciones
                int nextCoDesc = 1;
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT MAX(co_desc) FROM saDescArticulo")) {
                    if (rs.next()) {
                        String maxVal = rs.getString(1);
                        if (maxVal != null && !maxVal.trim().isEmpty()) {
                            try {
                                nextCoDesc = Integer.parseInt(maxVal.trim()) + 1;
                            } catch (NumberFormatException e) {
                                // En caso de valor no numérico, buscar por conteo o valor seguro
                                nextCoDesc = 30000;
                            }
                        }
                    }
                }

                // 2. Preparar sentencias
                String sqlCheck = "SELECT COUNT(*) FROM saDescArticulo WHERE co_art = ? AND fecha_ini IS NULL";
                String sqlUpdate = "UPDATE saDescArticulo SET porc1 = ?, co_us_mo = 'ADMIN', fe_us_mo = GETDATE() WHERE co_art = ? AND tip_cli = ? AND fecha_ini IS NULL";
                String sqlInsert = "INSERT INTO saDescArticulo (co_desc, des_des, co_art, tip_cli, hasta1, hasta2, hasta3, hasta4, hasta5, porc1, porc2, porc3, porc4, porc5, porc6, co_us_in, fe_us_in, co_us_mo, fe_us_mo, rowguid) " +
                                   "VALUES (?, 'Descuento por Volumen', ?, ?, 99999999.99, 99999999.99, 99999999.99, 0.0, 0.0, ?, 0.0, 0.0, 0.0, 0.0, 0.0, 'ADMIN', GETDATE(), 'ADMIN', GETDATE(), ?)";

                try (PreparedStatement psCheck = conn.prepareStatement(sqlCheck);
                     PreparedStatement psUpdate = conn.prepareStatement(sqlUpdate);
                     PreparedStatement psInsert = conn.prepareStatement(sqlInsert)) {

                    for (String coArt : codigosArticulos) {
                        // Limitar longitud al formato de Profit Plus
                        String cleanCoArt = coArt.trim();
                        
                        // Verificar si ya tiene descuento
                        psCheck.setString(1, cleanCoArt);
                        boolean exists = false;
                        try (ResultSet rs = psCheck.executeQuery()) {
                            if (rs.next() && rs.getInt(1) > 0) {
                                exists = true;
                            }
                        }

                        if (exists) {
                            // Actualizar para los 7 tipos de clientes (000001 a 000007)
                            for (int i = 1; i <= 7; i++) {
                                String tipCli = String.format("%06d", i);
                                psUpdate.setDouble(1, nuevoPorcentaje);
                                psUpdate.setString(2, cleanCoArt);
                                psUpdate.setString(3, tipCli);
                                psUpdate.addBatch();
                            }
                            psUpdate.executeBatch();
                        } else {
                            // Insertar registros para los 7 tipos de clientes
                            for (int i = 1; i <= 7; i++) {
                                String tipCli = String.format("%06d", i);
                                String coDesc = String.format("%06d", nextCoDesc++);
                                UUID guid = UUID.randomUUID();

                                psInsert.setString(1, coDesc);
                                psInsert.setString(2, cleanCoArt);
                                psInsert.setString(3, tipCli);
                                psInsert.setDouble(4, nuevoPorcentaje);
                                psInsert.setString(5, guid.toString());
                                psInsert.addBatch();
                            }
                            psInsert.executeBatch();
                        }
                    }
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }
}

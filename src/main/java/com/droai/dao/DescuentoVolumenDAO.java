package com.droai.dao;

import com.droai.config.DatabaseConfig;
import com.droai.model.DescuentoVolumenRow;
import com.droai.model.SesionUsuario;

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
                ISNULL(a.modelo, '') AS modelo,
                ISNULL(a.ref, '') AS codigoBarra,
                ISNULL(pr.monto, 0) AS precio1,
                ISNULL(da.porc1, 0) AS descuentoDV,
                ISNULL(CONVERT(varchar, da.fecha_ini, 23), '') AS fechaIni,
                ISNULL(CONVERT(varchar, da.fecha_fin, 23), '') AS fechaFin,
                ISNULL(da.co_us_in, '') AS coUsIn,
                ISNULL(CONVERT(varchar, da.fe_us_in, 120), '') AS feUsIn,
                ISNULL(da.co_us_mo, '') AS coUsMo,
                ISNULL(CONVERT(varchar, da.fe_us_mo, 120), '') AS feUsMo,
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
            LEFT JOIN saDescArticulo da ON a.co_art = da.co_art AND da.tip_cli = '000001'
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
                row.setModelo(rs.getString("modelo") != null ? rs.getString("modelo").trim() : "");
                row.setCodigoBarra(rs.getString("codigoBarra").trim());
                row.setPrecio1(rs.getDouble("precio1"));
                row.setDescuentoDV(rs.getDouble("descuentoDV"));
                row.setFechaIni(rs.getString("fechaIni") != null ? rs.getString("fechaIni").trim() : "");
                row.setFechaFin(rs.getString("fechaFin") != null ? rs.getString("fechaFin").trim() : "");
                row.setCoUsIn(rs.getString("coUsIn") != null ? rs.getString("coUsIn").trim() : "");
                row.setFeUsIn(rs.getString("feUsIn") != null ? rs.getString("feUsIn").trim() : "");
                row.setCoUsMo(rs.getString("coUsMo") != null ? rs.getString("coUsMo").trim() : "");
                row.setFeUsMo(rs.getString("feUsMo") != null ? rs.getString("feUsMo").trim() : "");
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
        updateDescuentosVolumen(codigosArticulos, nuevoPorcentaje, null, null);
    }

    public void updateDescuentosVolumen(List<String> codigosArticulos, double nuevoPorcentaje, Date fechaIni, Date fechaFin) throws SQLException {
        if (codigosArticulos == null || codigosArticulos.isEmpty()) {
            return;
        }

        String coUsuario = SesionUsuario.isAutenticado()
                ? SesionUsuario.current().getCoUsuario() : "SYSTEM";

        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Obtener el máximo co_desc actual para inserciones
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

                // 2. Usar DELETE + INSERT para evitar violación de UK (tip_cli, co_art, fecha_ini)
                String sqlDelete = "DELETE FROM saDescArticulo WHERE co_art = ? AND tip_cli = ?";
                String sqlInsert = "INSERT INTO saDescArticulo (co_desc, des_des, co_art, tip_cli, hasta1, hasta2, hasta3, hasta4, hasta5, porc1, porc2, porc3, porc4, porc5, porc6, fecha_ini, fecha_fin, co_us_in, fe_us_in, co_us_mo, fe_us_mo, rowguid) " +
                                   "VALUES (?, 'Descuento por Volumen', ?, ?, 99999999.99, 99999999.99, 99999999.99, 0.0, 0.0, ?, 0.0, 0.0, 0.0, 0.0, 0.0, ?, ?, ?, GETDATE(), ?, GETDATE(), ?)";

                try (PreparedStatement psDelete = conn.prepareStatement(sqlDelete);
                     PreparedStatement psInsert = conn.prepareStatement(sqlInsert)) {

                    for (String coArt : codigosArticulos) {
                        String cleanCoArt = coArt.trim();

                        for (int i = 1; i <= 7; i++) {
                            String tipCli = String.format("%06d", i);

                            // Eliminar filas existentes para este par (co_art, tip_cli)
                            psDelete.setString(1, cleanCoArt);
                            psDelete.setString(2, tipCli);
                            psDelete.executeUpdate();

                            // Insertar fila nueva
                            String coDesc = String.format("%06d", nextCoDesc++);
                            UUID guid = UUID.randomUUID();

                            psInsert.setString(1, coDesc);
                            psInsert.setString(2, cleanCoArt);
                            psInsert.setString(3, tipCli);
                            psInsert.setDouble(4, nuevoPorcentaje);
                            if (fechaIni != null) psInsert.setDate(5, fechaIni); else psInsert.setNull(5, Types.DATE);
                            if (fechaFin != null) psInsert.setDate(6, fechaFin); else psInsert.setNull(6, Types.DATE);
                            psInsert.setString(7, coUsuario);
                            psInsert.setString(8, coUsuario);
                            psInsert.setString(9, guid.toString());
                            psInsert.executeUpdate();
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

    public void updateDescuentosVolumenMap(java.util.Map<String, Double> dctosMap) throws SQLException {
        updateDescuentosVolumenMap(dctosMap, null, null);
    }

    public void updateDescuentosVolumenMap(java.util.Map<String, Double> dctosMap, Date fechaIni, Date fechaFin) throws SQLException {
        if (dctosMap == null || dctosMap.isEmpty()) {
            return;
        }

        String coUsuario = SesionUsuario.isAutenticado()
                ? SesionUsuario.current().getCoUsuario() : "SYSTEM";

        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            conn.setAutoCommit(false);
            try {
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

                String sqlCheckArticulo = "SELECT co_art FROM saArticulo WHERE co_art = ?";
                String sqlDelete = "DELETE FROM saDescArticulo WHERE co_art = ? AND tip_cli = ?";
                String sqlInsert = "INSERT INTO saDescArticulo (co_desc, des_des, co_art, tip_cli, hasta1, hasta2, hasta3, hasta4, hasta5, porc1, porc2, porc3, porc4, porc5, porc6, fecha_ini, fecha_fin, co_us_in, fe_us_in, co_us_mo, fe_us_mo, rowguid) " +
                                   "VALUES (?, 'Descuento por Volumen', ?, ?, 99999999.99, 99999999.99, 99999999.99, 0.0, 0.0, ?, 0.0, 0.0, 0.0, 0.0, 0.0, ?, ?, ?, GETDATE(), ?, GETDATE(), ?)";

                try (PreparedStatement psCheckArticulo = conn.prepareStatement(sqlCheckArticulo);
                     PreparedStatement psDelete = conn.prepareStatement(sqlDelete);
                     PreparedStatement psInsert = conn.prepareStatement(sqlInsert)) {

                    for (java.util.Map.Entry<String, Double> entry : dctosMap.entrySet()) {
                        String coArt = entry.getKey().trim();
                        double nuevoPorcentaje = entry.getValue();

                        String matchedCoArt = null;
                        psCheckArticulo.setString(1, coArt);
                        try (ResultSet rs = psCheckArticulo.executeQuery()) {
                            if (rs.next()) {
                                matchedCoArt = rs.getString("co_art").trim();
                            }
                        }

                        if (matchedCoArt == null && coArt.matches("\\d+")) {
                            try {
                                String padded = String.format("%06d", Integer.parseInt(coArt));
                                psCheckArticulo.setString(1, padded);
                                try (ResultSet rs = psCheckArticulo.executeQuery()) {
                                    if (rs.next()) {
                                        matchedCoArt = rs.getString("co_art").trim();
                                    }
                                }
                            } catch (NumberFormatException ignored) {
                            }
                        }

                        if (matchedCoArt == null) {
                            System.out.println("Advertencia: Se omitió el artículo porque no existe en la tabla saArticulo: " + coArt);
                            continue;
                        }

                        for (int i = 1; i <= 7; i++) {
                            String tipCli = String.format("%06d", i);

                            // Eliminar filas existentes para este par (co_art, tip_cli)
                            psDelete.setString(1, matchedCoArt);
                            psDelete.setString(2, tipCli);
                            psDelete.executeUpdate();

                            // Insertar fila nueva
                            String coDesc = String.format("%06d", nextCoDesc++);
                            UUID guid = UUID.randomUUID();

                            psInsert.setString(1, coDesc);
                            psInsert.setString(2, matchedCoArt);
                            psInsert.setString(3, tipCli);
                            psInsert.setDouble(4, nuevoPorcentaje);
                            if (fechaIni != null) psInsert.setDate(5, fechaIni); else psInsert.setNull(5, Types.DATE);
                            if (fechaFin != null) psInsert.setDate(6, fechaFin); else psInsert.setNull(6, Types.DATE);
                            psInsert.setString(7, coUsuario);
                            psInsert.setString(8, coUsuario);
                            psInsert.setString(9, guid.toString());
                            psInsert.executeUpdate();
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

    public void updateDescuentosVolumenItems(List<com.droai.service.ImportadorService.DescuentoDVImportItem> items, Date fallbackIni, Date fallbackFin) throws SQLException {
        if (items == null || items.isEmpty()) {
            return;
        }

        String coUsuario = SesionUsuario.isAutenticado()
                ? SesionUsuario.current().getCoUsuario() : "SYSTEM";

        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            conn.setAutoCommit(false);
            try {
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

                String sqlCheckArticulo = "SELECT co_art FROM saArticulo WHERE co_art = ?";
                String sqlDelete = "DELETE FROM saDescArticulo WHERE co_art = ? AND tip_cli = ?";
                String sqlInsert = "INSERT INTO saDescArticulo (co_desc, des_des, co_art, tip_cli, hasta1, hasta2, hasta3, hasta4, hasta5, porc1, porc2, porc3, porc4, porc5, porc6, fecha_ini, fecha_fin, co_us_in, fe_us_in, co_us_mo, fe_us_mo, rowguid) " +
                                   "VALUES (?, 'Descuento por Volumen', ?, ?, 99999999.99, 99999999.99, 99999999.99, 0.0, 0.0, ?, 0.0, 0.0, 0.0, 0.0, 0.0, ?, ?, ?, GETDATE(), ?, GETDATE(), ?)";

                try (PreparedStatement psCheckArticulo = conn.prepareStatement(sqlCheckArticulo);
                     PreparedStatement psDelete = conn.prepareStatement(sqlDelete);
                     PreparedStatement psInsert = conn.prepareStatement(sqlInsert)) {

                    for (com.droai.service.ImportadorService.DescuentoDVImportItem item : items) {
                        String coArt = item.getCodigoArticulo().trim();
                        double nuevoPorcentaje = item.getPorcentaje();
                        Date itemIni = (item.getFechaIni() != null) ? item.getFechaIni() : fallbackIni;
                        Date itemFin = (item.getFechaFin() != null) ? item.getFechaFin() : fallbackFin;

                        String matchedCoArt = null;
                        psCheckArticulo.setString(1, coArt);
                        try (ResultSet rs = psCheckArticulo.executeQuery()) {
                            if (rs.next()) {
                                matchedCoArt = rs.getString("co_art").trim();
                            }
                        }

                        if (matchedCoArt == null && coArt.matches("\\d+")) {
                            try {
                                String padded = String.format("%06d", Integer.parseInt(coArt));
                                psCheckArticulo.setString(1, padded);
                                try (ResultSet rs = psCheckArticulo.executeQuery()) {
                                    if (rs.next()) {
                                        matchedCoArt = rs.getString("co_art").trim();
                                    }
                                }
                            } catch (NumberFormatException ignored) {
                            }
                        }

                        if (matchedCoArt == null) {
                            System.out.println("Advertencia: Se omitió el artículo porque no existe en la tabla saArticulo: " + coArt);
                            continue;
                        }

                        for (int i = 1; i <= 7; i++) {
                            String tipCli = String.format("%06d", i);

                            // Eliminar filas existentes para este par (co_art, tip_cli)
                            psDelete.setString(1, matchedCoArt);
                            psDelete.setString(2, tipCli);
                            psDelete.executeUpdate();

                            // Insertar fila nueva
                            String coDesc = String.format("%06d", nextCoDesc++);
                            UUID guid = UUID.randomUUID();

                            psInsert.setString(1, coDesc);
                            psInsert.setString(2, matchedCoArt);
                            psInsert.setString(3, tipCli);
                            psInsert.setDouble(4, nuevoPorcentaje);
                            if (itemIni != null) psInsert.setDate(5, itemIni); else psInsert.setNull(5, Types.DATE);
                            if (itemFin != null) psInsert.setDate(6, itemFin); else psInsert.setNull(6, Types.DATE);
                            psInsert.setString(7, coUsuario);
                            psInsert.setString(8, coUsuario);
                            psInsert.setString(9, guid.toString());
                            psInsert.executeUpdate();
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

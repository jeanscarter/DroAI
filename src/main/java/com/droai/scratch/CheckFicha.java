package com.droai.scratch;

import java.sql.*;

public class CheckFicha {
    public static void main(String[] args) {
        String url = "jdbc:sqlserver://srvdb0101:1433;databaseName=DROA_A_DEV;trustServerCertificate=true;encrypt=false";
        String user = "profit";
        String pass = "profit";
        
        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            // Check saArtUnidad columns
            DatabaseMetaData meta = conn.getMetaData();
            System.out.println("--- COLUMNS IN saArtUnidad ---");
            try (ResultSet rs = meta.getColumns(null, null, "saArtUnidad", null)) {
                while (rs.next()) {
                    System.out.println(rs.getString("COLUMN_NAME"));
                }
            }

            System.out.println("\n--- saArtUnidad FOR 004551 ---");
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM saArtUnidad WHERE co_art = '004551'")) {
                ResultSetMetaData rsMeta = rs.getMetaData();
                int cols = rsMeta.getColumnCount();
                for (int i = 1; i <= cols; i++) System.out.print(rsMeta.getColumnLabel(i) + " | ");
                System.out.println();
                while (rs.next()) {
                    for (int i = 1; i <= cols; i++) System.out.print(rs.getString(i) + " | ");
                    System.out.println();
                }
            }

            // Query full product data for 004551
            System.out.println("\n--- PRODUCT 004551 ---");
            String sql = """
                SELECT TOP 1
                    a.co_art, a.art_des, a.ref, a.tipo_imp, a.co_lin, a.co_subl,
                    a.co_ubicacion, a.campo1, a.campo2, a.campo3, a.campo4, a.campo5, a.campo6,
                    a.prec_om, a.porc_arancel, a.tipo_cos, a.anulado, a.destaca,
                    a.modelo, a.cod_proc, a.peso, a.volumen, a.margen_min, a.margen_max,
                    ISNULL(stk.totalStock, 0) AS existencia,
                    ISNULL(p.prov_des, '') AS marca,
                    ISNULL(p1.monto, 0) AS precio1,
                    ISNULL(p2.monto, 0) AS precio2,
                    ISNULL(p3.monto, 0) AS precio3,
                    ISNULL(p4.monto, 0) AS precio4,
                    ISNULL(i.porc, 0) AS ivaPct,
                    ISNULL(l.lin_des, '') AS linea,
                    ISNULL(sl.subl_des, '') AS subLinea
                FROM saArticulo a
                LEFT JOIN (SELECT co_art, co_prov FROM saArtProveedorReng WHERE reng_num = 1) ap ON a.co_art = ap.co_art
                LEFT JOIN saProveedor p ON ap.co_prov = p.co_prov
                LEFT JOIN saLineaArticulo l ON a.co_lin = l.co_lin
                LEFT JOIN saSubLinea sl ON a.co_subl = sl.co_subl
                LEFT JOIN (SELECT tipo_imp, MAX(porc_tasa) AS porc FROM saImpuestoSobreVentaReng GROUP BY tipo_imp) i ON a.tipo_imp = i.tipo_imp
                LEFT JOIN (SELECT co_art, SUM(stock) AS totalStock FROM saStockAlmacen GROUP BY co_art) stk ON a.co_art = stk.co_art
                LEFT JOIN (SELECT co_art, MAX(monto) AS monto FROM saArtPrecio WHERE co_precio = '01' GROUP BY co_art) p1 ON a.co_art = p1.co_art
                LEFT JOIN (SELECT co_art, MAX(monto) AS monto FROM saArtPrecio WHERE co_precio = '02' GROUP BY co_art) p2 ON a.co_art = p2.co_art
                LEFT JOIN (SELECT co_art, MAX(monto) AS monto FROM saArtPrecio WHERE co_precio = '03' GROUP BY co_art) p3 ON a.co_art = p3.co_art
                LEFT JOIN (SELECT co_art, MAX(monto) AS monto FROM saArtPrecio WHERE co_precio = '04' GROUP BY co_art) p4 ON a.co_art = p4.co_art
                WHERE a.co_art = '004551'
                """;
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                ResultSetMetaData rsMeta = rs.getMetaData();
                int cols = rsMeta.getColumnCount();
                if (rs.next()) {
                    for (int i = 1; i <= cols; i++) {
                        System.out.println(rsMeta.getColumnLabel(i) + " = " + rs.getString(i));
                    }
                }
            }

            // Check how many price levels exist
            System.out.println("\n--- PRICE LEVELS (co_precio) ---");
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT DISTINCT co_precio FROM saArtPrecio ORDER BY co_precio")) {
                while (rs.next()) {
                    System.out.print(rs.getString(1) + " | ");
                }
                System.out.println();
            }

            // saArtPrecio for 004551
            System.out.println("\n--- saArtPrecio (004551) ---");
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM saArtPrecio WHERE co_art = '004551'")) {
                ResultSetMetaData rsMeta = rs.getMetaData();
                int cols = rsMeta.getColumnCount();
                for (int i = 1; i <= cols; i++) System.out.print(rsMeta.getColumnLabel(i) + " | ");
                System.out.println();
                while (rs.next()) {
                    for (int i = 1; i <= cols; i++) System.out.print(rs.getString(i) + " | ");
                    System.out.println();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

package com.droai.scratch;

import java.sql.*;

public class FastSchemaCheck {
    public static void main(String[] args) {
        String url = "jdbc:sqlserver://srvdb0101:1433;databaseName=DROA_A_DEV;trustServerCertificate=true;encrypt=false";
        String user = "ProfitPlus"; // Asuming from context or previous logs if available
        String pass = "p+123456";    // Asuming from context or previous logs if available
        
        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            DatabaseMetaData meta = conn.getMetaData();
            System.out.println("--- Columns in saArticulo ---");
            try (ResultSet rs = meta.getColumns(null, null, "saArticulo", null)) {
                while (rs.next()) {
                    System.out.println("saArticulo: " + rs.getString("COLUMN_NAME"));
                }
            }
            System.out.println("--- Columns in saFacturaVentaReng ---");
            try (ResultSet rs = meta.getColumns(null, null, "saFacturaVentaReng", null)) {
                while (rs.next()) {
                    System.out.println("saFacturaVentaReng: " + rs.getString("COLUMN_NAME"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

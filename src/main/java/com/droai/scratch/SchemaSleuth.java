package com.droai.scratch;

import java.sql.*;

public class SchemaSleuth {
    public static void main(String[] args) {
        String url = "jdbc:sqlserver://srvdb0101:1433;databaseName=DROA_A_DEV;trustServerCertificate=true;encrypt=false";
        String user = "profit";
        String pass = "profit";
        
        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            DatabaseMetaData meta = conn.getMetaData();
            
            System.out.println("--- COLUMNS IN saArticulo ---");
            try (ResultSet rs = meta.getColumns(null, null, "saArticulo", null)) {
                while (rs.next()) {
                    String name = rs.getString("COLUMN_NAME");
                    if (name.toLowerCase().contains("prov")) {
                        System.out.println("FOUND: " + name);
                    }
                }
            }

            System.out.println("--- CHECKING saArtPro ---");
            try (ResultSet rs = meta.getTables(null, null, "saArtPro", null)) {
                if (rs.next()) {
                    System.out.println("Table saArtPro exists.");
                    try (ResultSet cols = meta.getColumns(null, null, "saArtPro", null)) {
                        while (cols.next()) {
                            System.out.println("saArtPro: " + cols.getString("COLUMN_NAME"));
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

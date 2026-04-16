package com.droai.scratch;

import java.sql.*;

public class SchemaDeepDive {
    public static void main(String[] args) {
        String url = "jdbc:sqlserver://srvdb0101:1433;databaseName=DROA_A_DEV;trustServerCertificate=true;encrypt=false";
        String user = "profit";
        String pass = "profit";

        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            DatabaseMetaData meta = conn.getMetaData();

            System.out.println("--- COLUMNS IN saArticulo ---");
            try (ResultSet rs = meta.getColumns(null, "dbo", "saArticulo", null)) {
                while (rs.next()) {
                    System.out.println(rs.getString("COLUMN_NAME"));
                }
            }

            System.out.println("--- TABLES RELATED TO PROVEEDOR ---");
            try (ResultSet rs = meta.getTables(null, "dbo", "%PROV%", null)) {
                while (rs.next()) {
                    System.out.println(rs.getString("TABLE_NAME"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

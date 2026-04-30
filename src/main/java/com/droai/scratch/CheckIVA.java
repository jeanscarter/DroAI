package com.droai.scratch;

import java.sql.*;

public class CheckIVA {
    public static void main(String[] args) {
        String url = "jdbc:sqlserver://srvdb0101:1433;databaseName=DROA_A_DEV;trustServerCertificate=true;encrypt=false";
        String user = "profit";
        String pass = "profit";
        
        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            DatabaseMetaData meta = conn.getMetaData();
            
            System.out.println("--- COLUMNS IN saImpuesto ---");
            try (ResultSet rs = meta.getColumns(null, null, "saImpuesto", null)) {
                while (rs.next()) {
                    System.out.println(rs.getString("COLUMN_NAME") + " - " + rs.getString("TYPE_NAME"));
                }
            }
            System.out.println("--- COLUMNS IN saImpuestoReng ---");
            try (ResultSet rs = meta.getColumns(null, null, "saImpuestoReng", null)) {
                while (rs.next()) {
                    System.out.println(rs.getString("COLUMN_NAME") + " - " + rs.getString("TYPE_NAME"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

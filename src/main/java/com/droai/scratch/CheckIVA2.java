package com.droai.scratch;

import java.sql.*;

public class CheckIVA2 {
    public static void main(String[] args) {
        String url = "jdbc:sqlserver://srvdb0101:1433;databaseName=DROA_A_DEV;trustServerCertificate=true;encrypt=false";
        String user = "profit";
        String pass = "profit";
        
        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            DatabaseMetaData meta = conn.getMetaData();
            System.out.println("--- COLUMNS IN saFacturaVentaReng WITH imp ---");
            try (ResultSet rs = meta.getColumns(null, null, "saFacturaVentaReng", "%imp%")) {
                while (rs.next()) {
                    System.out.println(rs.getString("COLUMN_NAME"));
                }
            }
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT TOP 5 cod_impuesto, des_impuesto, campo1 FROM saImpuesto")) {
                while (rs.next()) {
                    System.out.println(rs.getString(1) + " | " + rs.getString(2) + " | " + rs.getString(3));
                }
            } catch (Exception ex) {
                System.out.println("Error saImpuesto: " + ex.getMessage());
            }
            
            System.out.println("--- TOP 5 rows FROM saArticulo ---");
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT TOP 5 tipo_imp FROM saArticulo WHERE tipo_imp IS NOT NULL AND tipo_imp <> ''")) {
                while (rs.next()) {
                    System.out.println(rs.getString(1));
                }
            } catch (Exception ex) {
                System.out.println("Error saArticulo: " + ex.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

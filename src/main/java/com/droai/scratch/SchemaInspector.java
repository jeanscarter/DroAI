package com.droai.scratch;

import com.droai.config.DatabaseConfig;
import java.sql.*;

public class SchemaInspector {
    public static void main(String[] args) {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            
            System.out.println("--- Column Info for saArticulo ---");
            try (ResultSet rs = metaData.getColumns(null, null, "saArticulo", null)) {
                while (rs.next()) {
                    String colName = rs.getString("COLUMN_NAME");
                    String typeName = rs.getString("TYPE_NAME");
                    System.out.println("saArticulo: " + colName + " (" + typeName + ")");
                }
            }

            System.out.println("\n--- Column Info for saFacturaVentaReng ---");
            try (ResultSet rs = metaData.getColumns(null, null, "saFacturaVentaReng", null)) {
                while (rs.next()) {
                    String colName = rs.getString("COLUMN_NAME");
                    String typeName = rs.getString("TYPE_NAME");
                    System.out.println("saFacturaVentaReng: " + colName + " (" + typeName + ")");
                }
            }
            
            System.out.println("\n--- Sample Data for porc_desc ---");
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT TOP 10 porc_desc, doc_num FROM saFacturaVentaReng")) {
                while (rs.next()) {
                    System.out.println("doc_num: " + rs.getString("doc_num") + ", porc_desc: [" + rs.getObject("porc_desc") + "]");
                }
            }

            System.out.println("\n--- Searching for '2+0+0+6' in all columns of saFacturaVentaReng (TOP 100) ---");
            // This is hard to do generically without knowing columns, but we can check doc_num and others
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT TOP 100 * FROM saFacturaVentaReng")) {
                ResultSetMetaData rsmd = rs.getMetaData();
                int columnCount = rsmd.getColumnCount();
                while (rs.next()) {
                    for (int i = 1; i <= columnCount; i++) {
                        Object val = rs.getObject(i);
                        if (val != null && val.toString().contains("2+0+0+6")) {
                            System.out.println("FOUND '2+0+0+6' in column: " + rsmd.getColumnName(i) + " for doc_num: " + rs.getString("doc_num"));
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

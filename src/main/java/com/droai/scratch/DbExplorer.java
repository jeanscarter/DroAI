package com.droai.scratch;

import com.droai.config.DatabaseConfig;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class DbExplorer {
    public static void main(String[] args) {
        System.out.println("DEBUG: DbExplorer main started");
        try {
            System.out.println("DEBUG: Testing connection...");
            DatabaseConfig.testConnection();
            
            try (Connection conn = DatabaseConfig.getDataSource().getConnection();
                 Statement st = conn.createStatement()) {
                
                System.out.println("DEBUG: Connection obtained");
                System.out.println("--- DB STATISTICS ---");
                
                // Total invoices
                try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM saFacturaVenta")) {
                    if (rs.next()) System.out.println("Total invoices (saFacturaVenta): " + rs.getInt(1));
                }
                
                // Date range
                try (ResultSet rs = st.executeQuery("SELECT MIN(fec_emis), MAX(fec_emis) FROM saFacturaVenta")) {
                    if (rs.next()) {
                        System.out.println("First invoice date: " + rs.getDate(1));
                        System.out.println("Last invoice date:  " + rs.getDate(2));
                    }
                }
                
                // Lines check
                try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM saFacturaVentaReng")) {
                    if (rs.next()) System.out.println("Total invoice lines (saFacturaVentaReng): " + rs.getInt(1));
                }

                // List columns of saArticulo to find the provider link
                System.out.println("--- Columns in saArticulo ---");
                try (ResultSet rs = conn.getMetaData().getColumns(null, null, "saArticulo", null)) {
                    while (rs.next()) {
                        System.out.println(rs.getString("COLUMN_NAME"));
                    }
                }

            }
        } catch (Exception e) {
            System.err.println("SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("DEBUG: DbExplorer main finished");
        System.exit(0);
    }
}

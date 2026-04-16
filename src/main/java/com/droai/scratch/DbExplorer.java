package com.droai.scratch;

import com.droai.config.DatabaseConfig;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class DbExplorer {
    public static void main(String[] args) {
        String testQuery = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'saFacturaVentaReng'";

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(testQuery)) {
            System.out.println("Columns in saFacturaVentaReng:");
            while (rs.next()) {
                System.out.println(rs.getString("COLUMN_NAME"));
            }
        } catch (Exception e) {
            System.err.println("SQL ERROR: " + e.getMessage());
        }
        System.exit(0);
    }
}

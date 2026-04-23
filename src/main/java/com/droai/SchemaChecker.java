package com.droai;

import com.droai.config.DatabaseConfig;
import java.sql.*;

public class SchemaChecker {
    public static void main(String[] args) {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();

            System.out.println("--- Table: saImpuesto ---");
            ResultSet rsImp = metaData.getColumns(null, null, "saImpuesto", null);
            while (rsImp.next()) {
                System.out.println("- " + rsImp.getString("COLUMN_NAME"));
            }

            System.out.println("\n--- Table: saImpuestoReng ---");
            ResultSet rsImpR = metaData.getColumns(null, null, "saImpuestoReng", null);
            while (rsImpR.next()) {
                System.out.println("- " + rsImpR.getString("COLUMN_NAME"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

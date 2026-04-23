package com.droai;

import com.droai.config.DatabaseConfig;
import java.sql.*;

public class SchemaChecker {
    public static void main(String[] args) {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();

            ResultSet rsCosto = metaData.getColumns(null, null, "saArtCosto", null);
            System.out.println("Columns in saArtCosto:");
            while (rsCosto.next()) {
                System.out.println("- " + rsCosto.getString("COLUMN_NAME"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DatabaseConfig.shutdown();
        }
    }
}

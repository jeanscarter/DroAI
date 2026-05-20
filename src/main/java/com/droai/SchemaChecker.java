package com.droai;

import com.droai.config.DatabaseConfig;
import java.sql.*;

public class SchemaChecker {
    public static void main(String[] args) {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();

            System.out.println("--- Table: saArticulo Columns Nullability ---");
            try (ResultSet rs = metaData.getColumns(null, null, "saArticulo", null)) {
                while (rs.next()) {
                    String colName = rs.getString("COLUMN_NAME");
                    String typeName = rs.getString("TYPE_NAME");
                    int colSize = rs.getInt("COLUMN_SIZE");
                    String isNullable = rs.getString("IS_NULLABLE");
                    System.out.printf("%s: %s (%d) | Nullable: %s%n", colName, typeName, colSize, isNullable);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.exit(0);
        }
    }
}

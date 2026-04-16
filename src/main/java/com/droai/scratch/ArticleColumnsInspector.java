package com.droai.scratch;

import com.droai.config.DatabaseConfig;
import java.sql.*;

public class ArticleColumnsInspector {
    public static void main(String[] args) {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            System.out.println("--- saArticulo Columns ---");
            try (ResultSet rs = metaData.getColumns(null, null, "saArticulo", null)) {
                while (rs.next()) {
                    System.out.println(rs.getString("COLUMN_NAME"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

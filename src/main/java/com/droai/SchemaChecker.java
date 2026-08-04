package com.droai;

import com.droai.config.DatabaseConfig;
import java.sql.*;

public class SchemaChecker {
    public static void main(String[] args) {
        System.out.println("=== Querying saStockAlmacen for Negative Stock ===");
        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {

            // 1. Individual negative stock records
            String sqlInd = """
                SELECT s.co_art, a.art_des, s.co_alma, s.tipo, s.stock, a.co_lin
                FROM saStockAlmacen s
                LEFT JOIN saArticulo a ON s.co_art = a.co_art
                WHERE s.stock < 0
                ORDER BY s.stock ASC
                """;

            int countInd = 0;
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(sqlInd)) {
                System.out.println("\n--- Negative Stock Records in saStockAlmacen (per warehouse/type) ---");
                while (rs.next()) {
                    countInd++;
                    String desc = rs.getString("art_des") != null ? rs.getString("art_des").trim() : "N/A";
                    System.out.printf("[%d] Code: %s | Alma: %s | Tipo: %s | Stock: %.2f | Desc: %s%n",
                            countInd, rs.getString("co_art").trim(), rs.getString("co_alma").trim(),
                            rs.getString("tipo").trim(), rs.getDouble("stock"), desc);
                }
            }
            System.out.println("\nTotal negative records in saStockAlmacen: " + countInd);

            // 2. Aggregate stock per product across all warehouses and types < 0
            String sqlAgg = """
                SELECT s.co_art, a.art_des, SUM(s.stock) as stock_total, a.co_lin
                FROM saStockAlmacen s
                LEFT JOIN saArticulo a ON s.co_art = a.co_art
                GROUP BY s.co_art, a.art_des, a.co_lin
                HAVING SUM(s.stock) < 0
                ORDER BY stock_total ASC
                """;

            int countAgg = 0;
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(sqlAgg)) {
                System.out.println("\n--- Products with Total Stock Across All Warehouses < 0 ---");
                while (rs.next()) {
                    countAgg++;
                    String desc = rs.getString("art_des") != null ? rs.getString("art_des").trim() : "N/A";
                    System.out.printf("[%d] Code: %s | Total Stock: %.2f | Line: %s | Desc: %s%n",
                            countAgg, rs.getString("co_art").trim(), rs.getDouble("stock_total"),
                            rs.getString("co_lin") != null ? rs.getString("co_lin").trim() : "N/A", desc);
                }
            }
            System.out.println("\nTotal products with aggregate negative stock: " + countAgg);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}








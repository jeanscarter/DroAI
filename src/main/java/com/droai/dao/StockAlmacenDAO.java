package com.droai.dao;

import com.droai.config.DatabaseConfig;
import com.droai.model.StockAlmacenRow;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO para consultar el desglose de stock por almacén de un artículo.
 *
 * <p>Consulta {@code saStockAlmacen} con JOIN a {@code saAlmacen}
 * para obtener la descripción del almacén.
 *
 * <p>Agrupa las entradas de {@code saLoteEntrada} por (artículo, almacén, numero_lote)
 * de modo que recepciones múltiples del mismo lote se sumen en un único registro.
 */
public class StockAlmacenDAO {

    private static final String SQL_STOCK_DETALLE = """
            SELECT
                sa.co_art                   AS codigoArticulo,
                sa.co_alma                  AS codigoAlmacen,
                ISNULL(al.des_alma, '')     AS nombreAlmacen,
                ISNULL(sa.stock, 0)         AS stock,
                0                           AS stockComprometido
            FROM saStockAlmacen sa
            LEFT JOIN saAlmacen al
                ON sa.co_alma = al.co_alma
            WHERE sa.co_art = ?
            ORDER BY sa.co_alma
            """;

    private static final String SQL_STOCK_LOTES = """
            SELECT
                le.co_art                   AS codigoArticulo,
                le.co_alma                  AS codigoAlmacen,
                ISNULL(al.des_alma, '')     AS nombreAlmacen,
                ISNULL(le.numero_lote, '')  AS lote,
                SUM(le.stock_actual)        AS stock,
                0                           AS stockComprometido
            FROM saLoteEntrada le
            LEFT JOIN saAlmacen al
                ON le.co_alma = al.co_alma
            WHERE le.co_art = ?
              AND le.stock_actual > 0
            GROUP BY le.co_art, le.co_alma, al.des_alma, le.numero_lote
            HAVING SUM(le.stock_actual) > 0
            ORDER BY le.co_alma, le.numero_lote
            """;

    private static final String SQL_STOCK_LOTES_ALL = """
            SELECT
                le.co_art                   AS codigoArticulo,
                le.co_alma                  AS codigoAlmacen,
                ISNULL(al.des_alma, '')     AS nombreAlmacen,
                ISNULL(le.numero_lote, '')  AS lote,
                SUM(le.stock_actual)        AS stock,
                0                           AS stockComprometido
            FROM saLoteEntrada le
            LEFT JOIN saAlmacen al
                ON le.co_alma = al.co_alma
            WHERE le.stock_actual > 0
            GROUP BY le.co_art, le.co_alma, al.des_alma, le.numero_lote
            HAVING SUM(le.stock_actual) > 0
            ORDER BY le.co_art, le.co_alma, le.numero_lote
            """;

    private static final String SQL_STOCK_ALL = """
            SELECT
                sa.co_art                   AS codigoArticulo,
                sa.co_alma                  AS codigoAlmacen,
                ISNULL(al.des_alma, '')     AS nombreAlmacen,
                ''                          AS lote,
                ISNULL(sa.stock, 0)         AS stock,
                0                           AS stockComprometido
            FROM saStockAlmacen sa
            LEFT JOIN saAlmacen al
                ON sa.co_alma = al.co_alma
            ORDER BY sa.co_art, sa.co_alma
            """;

    public List<StockAlmacenRow> fetchStockDetalle(String codigoArticulo) throws SQLException {
        List<StockAlmacenRow> rows = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            List<StockAlmacenRow> lotes = tryFetchLotes(conn, codigoArticulo);
            if (!lotes.isEmpty()) {
                return lotes;
            }

            try (PreparedStatement ps = conn.prepareStatement(SQL_STOCK_DETALLE)) {
                ps.setString(1, codigoArticulo);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        StockAlmacenRow row = new StockAlmacenRow();
                        row.setCodigoArticulo(rs.getString("codigoArticulo"));
                        row.setCodigoAlmacen(rs.getString("codigoAlmacen"));
                        row.setNombreAlmacen(rs.getString("nombreAlmacen"));
                        row.setLote("");
                        row.setStock(getSafeDouble(rs, "stock"));
                        row.setStockComprometido(getSafeDouble(rs, "stockComprometido"));
                        rows.add(row);
                    }
                }
            }
        }
        return rows;
    }

    public Map<String, List<StockAlmacenRow>> fetchStockDetalleAll() throws SQLException {
        Map<String, List<StockAlmacenRow>> map = new HashMap<>();

        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            Map<String, List<StockAlmacenRow>> lotesMap = tryFetchLotesAll(conn);
            map.putAll(lotesMap);

            try (PreparedStatement ps = conn.prepareStatement(SQL_STOCK_ALL);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String coArt = rs.getString("codigoArticulo");
                    String key = coArt != null ? coArt.trim() : "";
                    
                    if (!lotesMap.containsKey(key)) {
                        StockAlmacenRow row = new StockAlmacenRow();
                        row.setCodigoArticulo(coArt);
                        row.setCodigoAlmacen(rs.getString("codigoAlmacen"));
                        row.setNombreAlmacen(rs.getString("nombreAlmacen"));
                        row.setLote(rs.getString("lote"));
                        row.setStock(getSafeDouble(rs, "stock"));
                        row.setStockComprometido(getSafeDouble(rs, "stockComprometido"));
                        map.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
                    }
                }
            }
        }
        return map;
    }

    private List<StockAlmacenRow> tryFetchLotes(Connection conn, String codigoArticulo) {
        List<StockAlmacenRow> rows = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SQL_STOCK_LOTES)) {
            ps.setString(1, codigoArticulo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    StockAlmacenRow row = new StockAlmacenRow();
                    row.setCodigoArticulo(rs.getString("codigoArticulo"));
                    row.setCodigoAlmacen(rs.getString("codigoAlmacen"));
                    row.setNombreAlmacen(rs.getString("nombreAlmacen"));
                    row.setLote(rs.getString("lote"));
                    row.setStock(getSafeDouble(rs, "stock"));
                    row.setStockComprometido(getSafeDouble(rs, "stockComprometido"));
                    rows.add(row);
                }
            }
        } catch (SQLException e) {
            System.err.println("[INFO] saLoteEntrada no disponible, usando solo saStockAlmacen: "
                    + e.getMessage());
        }
        return rows;
    }

    private Map<String, List<StockAlmacenRow>> tryFetchLotesAll(Connection conn) {
        Map<String, List<StockAlmacenRow>> map = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(SQL_STOCK_LOTES_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                StockAlmacenRow row = new StockAlmacenRow();
                String coArt = rs.getString("codigoArticulo");
                row.setCodigoArticulo(coArt);
                row.setCodigoAlmacen(rs.getString("codigoAlmacen"));
                row.setNombreAlmacen(rs.getString("nombreAlmacen"));
                row.setLote(rs.getString("lote"));
                row.setStock(getSafeDouble(rs, "stock"));
                row.setStockComprometido(getSafeDouble(rs, "stockComprometido"));
                map.computeIfAbsent(coArt != null ? coArt.trim() : "", k -> new ArrayList<>()).add(row);
            }
        } catch (SQLException e) {
            System.err.println("[INFO] saLoteEntrada no disponible para carga masiva: "
                    + e.getMessage());
        }
        return map;
    }

    private double getSafeDouble(ResultSet rs, String column) {
        try {
            String raw = rs.getString(column);
            if (raw == null || raw.isBlank()) return 0.0;
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException | SQLException e) {
            return 0.0;
        }
    }
}

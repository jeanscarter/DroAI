package com.droai.dao;

import com.droai.config.DatabaseConfig;
import com.droai.model.ProductoReporteRow;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para el Reporte de Productos.
 * Consulta todos los artículos de saArticulo (con y sin inventario)
 * incluyendo: Línea, Principio Activo (Sublínea), Categoría y Proveedor.
 *
 * <p>Utiliza la misma conexión HikariCP ({@link DatabaseConfig}) que el
 * resto de la aplicación.
 *
 * <p><b>Joins:</b>
 * <ul>
 *   <li>saLineaArticulo → Línea (lin_des)</li>
 *   <li>saSubLinea → Principio Activo (subl_des)</li>
 *   <li>saCatArticulo → Categoría (cat_des)</li>
 *   <li>saArtProveedorReng + saProveedor → Proveedor (prov_des)</li>
 *   <li>saStockAlmacen → Existencia total (SUM stock)</li>
 * </ul>
 */
public class ReporteProductoDAO {

    private static final String SQL_REPORTE = """
            SELECT
                a.co_art                    AS codigo,
                ISNULL(a.art_des, '')       AS descripcion,
                ISNULL(a.co_lin, '')        AS codLinea,
                ISNULL(l.lin_des, '')       AS linea,
                ISNULL(a.co_subl, '')       AS codSubLinea,
                ISNULL(sl.subl_des, '')     AS principioActivo,
                ISNULL(a.co_cat, '')        AS codCategoria,
                ISNULL(cat.cat_des, '')     AS categoria,
                ISNULL(ap.co_prov, '')      AS codProveedor,
                ISNULL(p.prov_des, '')      AS proveedor,
                ISNULL(stk.totalStock, 0)   AS existencia
            FROM saArticulo a
            LEFT JOIN saLineaArticulo l
                ON a.co_lin = l.co_lin
            LEFT JOIN saSubLinea sl
                ON a.co_subl = sl.co_subl AND a.co_lin = sl.co_lin
            LEFT JOIN saCatArticulo cat
                ON a.co_cat = cat.co_cat
            LEFT JOIN (
                SELECT co_art, co_prov
                FROM saArtProveedorReng
                WHERE reng_num = 1
            ) ap ON a.co_art = ap.co_art
            LEFT JOIN saProveedor p
                ON ap.co_prov = p.co_prov
            LEFT JOIN (
                SELECT co_art, SUM(stock) AS totalStock
                FROM saStockAlmacen
                GROUP BY co_art
            ) stk ON a.co_art = stk.co_art
            ORDER BY a.co_art
            """;

    /**
     * Obtiene el reporte completo de todos los productos en saArticulo.
     * Incluye productos con existencia 0.
     *
     * @return lista de {@link ProductoReporteRow} con los datos del reporte.
     * @throws SQLException si ocurre un error de conexión o consulta.
     */
    public List<ProductoReporteRow> fetchReporteProductos() throws SQLException {
        List<ProductoReporteRow> rows = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_REPORTE);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                ProductoReporteRow row = new ProductoReporteRow();
                row.setCodigo(rs.getString("codigo"));
                row.setDescripcion(rs.getString("descripcion"));
                row.setCodLinea(rs.getString("codLinea"));
                row.setLinea(rs.getString("linea"));
                row.setCodSubLinea(rs.getString("codSubLinea"));
                row.setPrincipioActivo(rs.getString("principioActivo"));
                row.setCodCategoria(rs.getString("codCategoria"));
                row.setCategoria(rs.getString("categoria"));
                row.setCodProveedor(rs.getString("codProveedor"));
                row.setProveedor(rs.getString("proveedor"));
                row.setExistencia(getSafeDouble(rs, "existencia"));
                rows.add(row);
            }
        }
        return rows;
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

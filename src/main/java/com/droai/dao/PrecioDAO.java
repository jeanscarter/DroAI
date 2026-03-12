package com.droai.dao;

import com.droai.config.DatabaseConfig;
import com.droai.model.FacturaRow;
import com.droai.model.ResumenRow;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos: consultas y actualizaciones sobre factura/reng_fac/art
 * en la base Profit Plus (DROA_A_DEV).
 */
public class PrecioDAO {

    // ---------- Listado principal (Tab 1) ----------

    private static final String SQL_LISTADO = """
        SELECT
            a.co_art       AS codigo,
            a.art_des      AS descripcion,
            r.co_alma      AS referencia,
            r.total_art    AS existencia,
            a.uni_venta    AS udm,
            r.cost_unit_om AS costo_fabrica,
            r.otros1       AS arancel_pct,
            r.cost_unit_om AS costo_om,
            r.prec_vta     AS util_pct,
            r.prec_vta     AS precio1,
            r.porc_imp     AS iva_pct,
            r.prec_vta * (1 + r.porc_imp / 100.0) AS precio_civa
        FROM reng_fac r
        INNER JOIN factura f ON f.fact_num = r.fact_num
        INNER JOIN art     a ON a.co_art   = r.co_art
        WHERE f.fec_emis BETWEEN ? AND ?
        ORDER BY a.co_art
        """;

    public List<FacturaRow> fetchListado(LocalDate from, LocalDate to) throws SQLException {
        List<FacturaRow> rows = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_LISTADO)) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new FacturaRow(
                        rs.getString("codigo"),
                        rs.getString("descripcion"),
                        rs.getString("referencia"),
                        rs.getDouble("existencia"),
                        rs.getString("udm"),
                        rs.getDouble("costo_fabrica"),
                        rs.getDouble("arancel_pct"),
                        rs.getDouble("costo_om"),
                        rs.getDouble("util_pct"),
                        rs.getDouble("precio1"),
                        rs.getDouble("iva_pct"),
                        rs.getDouble("precio_civa")
                    ));
                }
            }
        }
        return rows;
    }

    // ---------- Descuentos x Volumen (Tab 3) ----------

    private static final String SQL_DV = """
        SELECT
            a.co_art       AS clave,
            a.art_des      AS descripcion,
            SUM(r.prec_vta * r.total_art) AS total,
            SUM(r.prec_vta * r.total_art * r.porc_desc / 100.0) AS descuento,
            SUM(r.prec_vta * r.total_art * (1 - r.porc_desc / 100.0)) AS neto,
            AVG(r.porc_desc) AS porcentaje
        FROM reng_fac r
        INNER JOIN factura f ON f.fact_num = r.fact_num
        INNER JOIN art     a ON a.co_art   = r.co_art
        WHERE f.fec_emis BETWEEN ? AND ?
        GROUP BY a.co_art, a.art_des
        ORDER BY a.co_art
        """;

    public List<ResumenRow> fetchDescuentosVolumen(LocalDate from, LocalDate to) throws SQLException {
        return executeResumen(SQL_DV, from, to);
    }

    // ---------- Descuento x Producto (Tab 4) ----------

    private static final String SQL_DP = """
        SELECT
            a.co_art       AS clave,
            a.art_des      AS descripcion,
            SUM(r.prec_vta * r.total_art) AS total,
            SUM(r.prec_vta * r.total_art * r.porc_desc2 / 100.0) AS descuento,
            SUM(r.prec_vta * r.total_art * (1 - r.porc_desc2 / 100.0)) AS neto,
            AVG(r.porc_desc2) AS porcentaje
        FROM reng_fac r
        INNER JOIN factura f ON f.fact_num = r.fact_num
        INNER JOIN art     a ON a.co_art   = r.co_art
        WHERE f.fec_emis BETWEEN ? AND ?
        GROUP BY a.co_art, a.art_des
        ORDER BY a.co_art
        """;

    public List<ResumenRow> fetchDescuentosProducto(LocalDate from, LocalDate to) throws SQLException {
        return executeResumen(SQL_DP, from, to);
    }

    // ---------- UPDATE descuentos ----------

    private static final String SQL_UPDATE = """
        UPDATE reng_fac
        SET cost_unit_om = ?, otros1 = ?, prec_vta = ?, porc_imp = ?
        WHERE co_art = ?
        """;

    public void updatePrecios(List<FacturaRow> modified) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {
            conn.setAutoCommit(false);
            for (FacturaRow row : modified) {
                if (!row.isModified()) continue;
                ps.setDouble(1, row.getCostoFabrica());
                ps.setDouble(2, row.getArancelPct());
                ps.setDouble(3, row.getPrecio1());
                ps.setDouble(4, row.getIvaPct());
                ps.setString(5, row.getCodigo());
                ps.addBatch();
            }
            ps.executeBatch();
            conn.commit();
            // Limpiar flags
            modified.forEach(FacturaRow::clearModified);
        }
    }

    // ---------- Helper ----------

    private List<ResumenRow> executeResumen(String sql, LocalDate from, LocalDate to) throws SQLException {
        List<ResumenRow> rows = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new ResumenRow(
                        rs.getString("clave"),
                        rs.getString("descripcion"),
                        rs.getDouble("total"),
                        rs.getDouble("descuento"),
                        rs.getDouble("neto"),
                        rs.getDouble("porcentaje")
                    ));
                }
            }
        }
        return rows;
    }
}

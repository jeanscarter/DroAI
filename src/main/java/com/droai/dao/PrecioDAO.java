package com.droai.dao;

import com.droai.config.DatabaseConfig;
import com.droai.model.FacturaRow;
import com.droai.model.ResumenRow;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PrecioDAO {

    // ---------- Matriz de Ventas (Tab 1) ----------
    private static final String SQL_LISTADO = """
        SELECT TOP 1000
            f.doc_num      AS numero,
            CONVERT(varchar, f.fec_emis, 23) AS fecha,
            c.co_cli       AS ciRif,
            c.cli_des      AS nombreRazonSocial,
            v.co_ven       AS coVen,
            v.ven_des      AS nombreVendedor,
            f.tasa         AS tasa,
            a.co_art       AS codigoArt,
            a.art_des      AS descripcionArt,
            r.total_art    AS cantidad,
            r.prec_vta     AS precio,
            r.porc_desc    AS dp,
            0              AS dct,
            0              AS da,
            0              AS dv,
            r.porc_desc    AS descPct,
            (r.prec_vta * r.total_art) AS totalRenglon,
            0              AS descPctGlobal,
            (r.prec_vta * r.total_art) AS renglonDg,
            r.monto_imp    AS montoIva,
            (r.prec_vta * r.total_art) + r.monto_imp AS totRenglonIva,
            r.cost_vta     AS costoVenta,
            (r.cost_vta * r.total_art) AS totalCostoVenta,
            0              AS totCvDp,
            ((r.prec_vta * r.total_art) - (r.cost_vta * r.total_art)) AS montoUtilidad,
            0              AS utilPct,
            0              AS costoActual,
            0              AS stockActual,
            l.co_lin       AS codLinea,
            l.lin_des      AS linea
        FROM saFacturaVenta f
        JOIN saFacturaVentaReng r ON f.doc_num = r.doc_num
        JOIN saArticulo     a ON a.co_art  = r.co_art
        LEFT JOIN saCliente c ON f.co_cli = c.co_cli
        LEFT JOIN saVendedor v ON f.co_ven = v.co_ven
        LEFT JOIN saLineaArticulo l ON a.co_lin = l.co_lin
        WHERE f.fec_emis BETWEEN ? AND ?
        ORDER BY f.fec_emis DESC, f.doc_num DESC
        """;

    public List<FacturaRow> fetchListado(LocalDate from, LocalDate to) throws SQLException {
        List<FacturaRow> rows = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_LISTADO)) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FacturaRow row = new FacturaRow();
                    row.setNumero(rs.getString("numero"));
                    row.setFecha(rs.getString("fecha"));
                    row.setCiRif(rs.getString("ciRif"));
                    row.setNombreRazonSocial(rs.getString("nombreRazonSocial"));
                    row.setCoVen(rs.getString("coVen"));
                    row.setNombreVendedor(rs.getString("nombreVendedor"));
                    row.setTasa(rs.getDouble("tasa"));
                    row.setCodigoArt(rs.getString("codigoArt"));
                    row.setDescripcionArt(rs.getString("descripcionArt"));
                    row.setCantidad(rs.getDouble("cantidad"));
                    row.setPrecio(rs.getDouble("precio"));
                    row.setDp(rs.getDouble("dp"));
                    row.setDct(rs.getDouble("dct"));
                    row.setDa(rs.getDouble("da"));
                    row.setDv(rs.getDouble("dv"));
                    row.setDescPct(rs.getDouble("descPct"));
                    row.setTotalRenglon(rs.getDouble("totalRenglon"));
                    row.setDescPctGlobal(rs.getDouble("descPctGlobal"));
                    row.setRenglonDg(rs.getDouble("renglonDg"));
                    row.setMontoIva(rs.getDouble("montoIva"));
                    row.setTotRenglonIva(rs.getDouble("totRenglonIva"));
                    row.setCostoVenta(rs.getDouble("costoVenta"));
                    row.setTotalCostoVenta(rs.getDouble("totalCostoVenta"));
                    row.setTotCvDp(rs.getDouble("totCvDp"));
                    row.setMontoUtilidad(rs.getDouble("montoUtilidad"));
                    row.setUtilPct(rs.getDouble("utilPct"));
                    row.setCostoActual(rs.getDouble("costoActual"));
                    row.setStockActual(rs.getDouble("stockActual"));
                    row.setCodLinea(rs.getString("codLinea"));
                    row.setLinea(rs.getString("linea"));
                    rows.add(row);
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
        FROM saFacturaVentaReng r
        INNER JOIN saFacturaVenta f ON f.doc_num = r.doc_num
        INNER JOIN saArticulo     a ON a.co_art  = r.co_art
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
            SUM(r.prec_vta * r.total_art * r.porc_desc / 100.0) AS descuento,
            SUM(r.prec_vta * r.total_art * (1 - r.porc_desc / 100.0)) AS neto,
            AVG(r.porc_desc) AS porcentaje
        FROM saFacturaVentaReng r
        INNER JOIN saFacturaVenta f ON f.doc_num = r.doc_num
        INNER JOIN saArticulo     a ON a.co_art  = r.co_art
        WHERE f.fec_emis BETWEEN ? AND ?
        GROUP BY a.co_art, a.art_des
        ORDER BY a.co_art
        """;

    public List<ResumenRow> fetchDescuentosProducto(LocalDate from, LocalDate to) throws SQLException {
        return executeResumen(SQL_DP, from, to);
    }

    public void updatePrecios(List<FacturaRow> modified) throws SQLException {
        // No-op for read-only Matrix
    }

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

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
            r.porc_imp     AS ivaPct,
            (r.prec_vta * r.total_art) + r.monto_imp AS totRenglonIva,
            ISNULL(ce.costo, 0) AS costoVenta,
            (ISNULL(ce.costo, 0) * r.total_art) AS totalCostoVenta,
            0              AS totCvDp,
            ((r.prec_vta * r.total_art) - (ISNULL(ce.costo, 0) * r.total_art)) AS montoUtilidad,
            0              AS utilPct,
            ISNULL(ce.costo, 0) AS costoActual,
            0              AS stockActual,
            l.co_lin       AS codLinea,
            l.lin_des      AS linea
        FROM saFacturaVenta f
        JOIN saFacturaVentaReng r ON f.doc_num = r.doc_num
        JOIN saArticulo     a ON a.co_art  = r.co_art
        LEFT JOIN saCliente c ON f.co_cli = c.co_cli
        LEFT JOIN saVendedor v ON f.co_ven = v.co_ven
        LEFT JOIN saLineaArticulo l ON a.co_lin = l.co_lin
        LEFT JOIN (
            SELECT cod_articulo_rowguid, costo
            FROM (
                SELECT cod_articulo_rowguid, costo,
                       ROW_NUMBER() OVER (
                           PARTITION BY cod_articulo_rowguid
                           ORDER BY CASE WHEN tipo_doc = 'PROV' THEN 1 ELSE 2 END, fecha_emision DESC
                       ) AS rn
                FROM saCostoHistoricoEntrada
                WHERE costo > 0
            ) ranked
            WHERE rn = 1
        ) ce ON a.rowguid = ce.cod_articulo_rowguid
        WHERE f.fec_emis BETWEEN ? AND ?
        ORDER BY f.fec_emis DESC, f.doc_num DESC
        """;

    public List<FacturaRow> fetchListado(LocalDate from, LocalDate to) throws SQLException {
        List<FacturaRow> rows = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_LISTADO)) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            try (ResultSet resultSet = ps.executeQuery()) {
                while (resultSet.next()) {
                    FacturaRow row = new FacturaRow();
                    row.setNumero(resultSet.getString("numero"));
                    row.setFecha(resultSet.getString("fecha"));
                    row.setCiRif(resultSet.getString("ciRif"));
                    row.setNombreRazonSocial(resultSet.getString("nombreRazonSocial"));
                    row.setCoVen(resultSet.getString("coVen"));
                    row.setNombreVendedor(resultSet.getString("nombreVendedor"));
                    row.setTasa(resultSet.getDouble("tasa"));
                    row.setCodigoArt(resultSet.getString("codigoArt"));
                    row.setDescripcionArt(resultSet.getString("descripcionArt"));
                    row.setCantidad(resultSet.getDouble("cantidad"));
                    row.setPrecio(resultSet.getDouble("precio"));
                    row.setDp(resultSet.getDouble("dp"));
                    row.setDct(resultSet.getDouble("dct"));
                    row.setDa(resultSet.getDouble("da"));
                    row.setDv(resultSet.getDouble("dv"));
                    row.setDescPct(resultSet.getDouble("descPct"));
                    row.setTotalRenglon(resultSet.getDouble("totalRenglon"));
                    row.setDescPctGlobal(resultSet.getDouble("descPctGlobal"));
                    row.setRenglonDg(resultSet.getDouble("renglonDg"));
                    row.setMontoIva(resultSet.getDouble("montoIva"));
                    row.setIvaPct(resultSet.getDouble("ivaPct"));
                    row.setTotRenglonIva(resultSet.getDouble("totRenglonIva"));
                    row.setCostoVenta(resultSet.getDouble("costoVenta"));
                    row.setTotalCostoVenta(resultSet.getDouble("totalCostoVenta"));
                    row.setTotCvDp(resultSet.getDouble("totCvDp"));
                    row.setMontoUtilidad(resultSet.getDouble("montoUtilidad"));
                    row.setUtilPct(resultSet.getDouble("utilPct"));
                    row.setCostoActual(resultSet.getDouble("costoActual"));
                    row.setStockActual(resultSet.getDouble("stockActual"));
                    row.setCodLinea(resultSet.getString("codLinea"));
                    row.setLinea(resultSet.getString("linea"));
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
            AVG(r.porc_desc) AS porcentaje,
            SUM(ISNULL(ce.costo, 0) * r.total_art) AS costoOm,
            CASE WHEN SUM(r.prec_vta * r.total_art * (1 - r.porc_desc / 100.0)) = 0 THEN 0
                 ELSE ((SUM(r.prec_vta * r.total_art * (1 - r.porc_desc / 100.0)) - SUM(ISNULL(ce.costo, 0) * r.total_art)) / SUM(r.prec_vta * r.total_art * (1 - r.porc_desc / 100.0))) * 100.0 END AS utilPct
        FROM saFacturaVentaReng r
        INNER JOIN saFacturaVenta f ON f.doc_num = r.doc_num
        INNER JOIN saArticulo     a ON a.co_art  = r.co_art
        LEFT JOIN (
            SELECT cod_articulo_rowguid, costo
            FROM (
                SELECT cod_articulo_rowguid, costo,
                       ROW_NUMBER() OVER (
                           PARTITION BY cod_articulo_rowguid
                           ORDER BY CASE WHEN tipo_doc = 'PROV' THEN 1 ELSE 2 END, fecha_emision DESC
                       ) AS rn
                FROM saCostoHistoricoEntrada
                WHERE costo > 0
            ) ranked
            WHERE rn = 1
        ) ce ON a.rowguid = ce.cod_articulo_rowguid
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
            AVG(r.porc_desc) AS porcentaje,
            SUM(ISNULL(ce.costo, 0) * r.total_art) AS costoOm,
            CASE WHEN SUM(r.prec_vta * r.total_art * (1 - r.porc_desc / 100.0)) = 0 THEN 0
                 ELSE ((SUM(r.prec_vta * r.total_art * (1 - r.porc_desc / 100.0)) - SUM(ISNULL(ce.costo, 0) * r.total_art)) / SUM(r.prec_vta * r.total_art * (1 - r.porc_desc / 100.0))) * 100.0 END AS utilPct
        FROM saFacturaVentaReng r
        INNER JOIN saFacturaVenta f ON f.doc_num = r.doc_num
        INNER JOIN saArticulo     a ON a.co_art  = r.co_art
        LEFT JOIN (
            SELECT cod_articulo_rowguid, costo
            FROM (
                SELECT cod_articulo_rowguid, costo,
                       ROW_NUMBER() OVER (
                           PARTITION BY cod_articulo_rowguid
                           ORDER BY CASE WHEN tipo_doc = 'PROV' THEN 1 ELSE 2 END, fecha_emision DESC
                       ) AS rn
                FROM saCostoHistoricoEntrada
                WHERE costo > 0
            ) ranked
            WHERE rn = 1
        ) ce ON a.rowguid = ce.cod_articulo_rowguid
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
            try (ResultSet resultSet = ps.executeQuery()) {
                while (resultSet.next()) {
                    rows.add(new ResumenRow(
                        resultSet.getString("clave"),
                        resultSet.getString("descripcion"),
                        resultSet.getDouble("total"),
                        resultSet.getDouble("descuento"),
                        resultSet.getDouble("neto"),
                        resultSet.getDouble("porcentaje"),
                        resultSet.getDouble("costoOm"),
                        resultSet.getDouble("utilPct")
                    ));
                }
            }
        }
        return rows;
    }
}

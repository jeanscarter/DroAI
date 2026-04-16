package com.droai.dao;

import com.droai.config.DatabaseConfig;
import com.droai.model.MatrizVentasRow;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MatrizVentasDAO {

    private static final String SQL_MATRIZ_VENTAS = """
            SELECT
                f.doc_num AS numero,
                CONVERT(varchar, f.fec_emis, 23) AS fecha,
                c.rif AS ciRif,
                c.cli_des AS nombreRazonSocial,
                v.co_ven AS coVen,
                v.ven_des AS nombreVendedor,
                f.tasa AS tasa,
                r.co_art AS codigoArt,
                a.art_des AS descripcion,
                r.total_art AS cantidad,
                r.prec_vta AS precio,
                r.porc_desc AS dp,
                0 AS dct,
                0 AS da,
                0 AS dv,
                r.porc_desc AS descPct,
                (r.prec_vta * r.total_art) AS totalRenglon,
                ISNULL(f.porc_desc_glob, 0) AS descPctGlobal,
                ((r.prec_vta * r.total_art) * (1 - (ISNULL(f.porc_desc_glob, 0) / 100.0))) AS renglonDg,
                r.monto_imp AS montoIva,
                ((r.prec_vta * r.total_art) + r.monto_imp) AS totRenglonIva,
                0 AS costoVenta,
                0 AS totalCostoVenta,
                0 AS totCvDp,
                ((r.prec_vta * r.total_art) - (0)) AS montoUtilidad,
                100 AS utilPct,
                0 AS costoActual,
                ISNULL(sa.stock, 0) AS stockActual,
                a.co_lin AS codLinea,
                l.lin_des AS linea,
                a.co_subl AS codSub,
                sl.subl_des AS subLinea,
                '' AS codProveedor,
                '' AS nombreProveedor,
                c.co_zon AS zona,
                r.co_alma AS almacen,
                f.campo1 AS pedidoWeb,
                f.campo2 AS origen,
                f.campo3 AS usuarioWeb
            FROM saFacturaVenta f
            JOIN saFacturaVentaReng r ON f.doc_num = r.doc_num
            LEFT JOIN saCliente c ON f.co_cli = c.co_cli
            LEFT JOIN saVendedor v ON f.co_ven = v.co_ven
            LEFT JOIN saArticulo a ON r.co_art = a.co_art
            LEFT JOIN saLineaArticulo l ON a.co_lin = l.co_lin
            LEFT JOIN saSubLinea sl ON a.co_subl = sl.co_subl
            LEFT JOIN saStockAlmacen sa ON a.co_art = sa.co_art AND r.co_alma = sa.co_alma
            WHERE f.fec_emis BETWEEN ? AND ?
            ORDER BY f.fec_emis DESC, f.doc_num DESC
            """;

    public List<MatrizVentasRow> fetchMatrizVentas(LocalDate from, LocalDate to) throws SQLException {
        List<MatrizVentasRow> rows = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
                PreparedStatement ps = conn.prepareStatement(SQL_MATRIZ_VENTAS)) {

            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MatrizVentasRow row = new MatrizVentasRow();
                    row.setNumero(rs.getString("numero"));
                    row.setFecha(rs.getString("fecha"));
                    row.setCiRif(rs.getString("ciRif"));
                    row.setNombreRazonSocial(rs.getString("nombreRazonSocial"));
                    row.setCoVen(rs.getString("coVen"));
                    row.setNombreVendedor(rs.getString("nombreVendedor"));
                    row.setTasa(rs.getDouble("tasa"));
                    row.setCodigoArt(rs.getString("codigoArt"));
                    row.setDescripcion(rs.getString("descripcion"));
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
                    row.setCodSub(rs.getString("codSub"));
                    row.setSubLinea(rs.getString("subLinea"));
                    row.setCodProveedor(rs.getString("codProveedor"));
                    row.setNombreProveedor(rs.getString("nombreProveedor"));
                    row.setZona(rs.getString("zona"));
                    row.setAlmacen(rs.getString("almacen"));
                    row.setPedidoWeb(rs.getString("pedidoWeb"));
                    row.setOrigen(rs.getString("origen"));
                    row.setUsuarioWeb(rs.getString("usuarioWeb"));

                    rows.add(row);
                }
            }
        }
        return rows;
    }
}
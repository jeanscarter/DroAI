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
                r.porc_desc AS rawDiscounts,
                (r.prec_vta * r.total_art) AS totalRenglon,
                ISNULL(f.porc_desc_glob, 0) AS descPctGlobal,
                r.reng_neto AS renglonDg,
                r.monto_imp AS montoIva,
                r.porc_imp AS ivaPct,
                (r.reng_neto + r.monto_imp) AS totRenglonIva,
                ISNULL(ce.costo, 0) AS costoVenta,
                (ISNULL(ce.costo, 0) * r.total_art) AS totalCostoVenta,
                0 AS totCvDp,
                (r.reng_neto - (ISNULL(ce.costo, 0) * r.total_art)) AS montoUtilidad,
                0 AS utilPct,
                ISNULL(ce.costo, 0) AS costoActual,
                ISNULL(sa.stock, 0) AS stockActual,
                a.co_lin AS codLinea,
                l.lin_des AS linea,
                a.co_subl AS codSub,
                sl.subl_des AS subLinea,
                p.co_prov AS codProveedor,
                p.prov_des AS nombreProveedor,
                ISNULL(z.zon_des, c.co_zon) AS zona,
                ISNULL(c.ciudad, '') AS ciudad,
                ISNULL(a.campo1, '') AS codProv,
                r.co_alma AS almacen,
                f.campo1 AS pedidoWeb,
                f.campo2 AS origen,
                f.campo3 AS usuarioWeb
            FROM saFacturaVenta f
            JOIN saFacturaVentaReng r ON f.doc_num = r.doc_num
            LEFT JOIN saCliente c ON f.co_cli = c.co_cli
            LEFT JOIN saZona z ON c.co_zon = z.co_zon
            LEFT JOIN saVendedor v ON f.co_ven = v.co_ven
            LEFT JOIN saArticulo a ON r.co_art = a.co_art
            LEFT JOIN (SELECT co_art, MIN(co_prov) AS co_prov FROM saArtProveedorReng GROUP BY co_art) ap ON a.co_art = ap.co_art
            LEFT JOIN saProveedor p ON ap.co_prov = p.co_prov
            LEFT JOIN saLineaArticulo l ON a.co_lin = l.co_lin
            LEFT JOIN saSubLinea sl ON a.co_lin = sl.co_lin AND a.co_subl = sl.co_subl
            LEFT JOIN (
                SELECT co_art, co_alma, SUM(stock) AS stock
                FROM saStockAlmacen
                GROUP BY co_art, co_alma
            ) sa ON a.co_art = sa.co_art AND r.co_alma = sa.co_alma
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
            WHERE CONVERT(date, f.fec_emis) BETWEEN ? AND ? AND ISNULL(f.anulado, 0) = 0
            ORDER BY f.fec_emis DESC, f.doc_num DESC
            """;

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MatrizVentasDAO.class);
    private static final LocalDate CUTOFF_DATE = LocalDate.of(2026, 7, 27);
    private static final String HISTORICAL_DB = "DROA2_A";

    public List<MatrizVentasRow> fetchMatrizVentas(LocalDate from, LocalDate to) throws SQLException {
        if (to.isBefore(CUTOFF_DATE)) {
            // Rango completamente previo al 27/07/2026 -> DROA2_A
            return fetchMatrizVentasForDb(from, to, HISTORICAL_DB);
        } else if (!from.isBefore(CUTOFF_DATE)) {
            // Rango a partir del 27/07/2026 -> BD actual por defecto
            return fetchMatrizVentasForDb(from, to, null);
        } else {
            // Rango que abarca antes y después del 27/07/2026 -> Combinar consultas
            List<MatrizVentasRow> combined = new ArrayList<>();
            LocalDate endHistorical = CUTOFF_DATE.minusDays(1);
            combined.addAll(fetchMatrizVentasForDb(from, endHistorical, HISTORICAL_DB));
            combined.addAll(fetchMatrizVentasForDb(CUTOFF_DATE, to, null));
            return combined;
        }
    }

    public List<MatrizVentasRow> fetchMatrizVentasForDb(LocalDate from, LocalDate to, String targetDb) throws SQLException {
        List<MatrizVentasRow> rows = new ArrayList<>();
        logger.info("Consultando Matriz de Ventas en BD [{}] del {} al {}",
                targetDb != null ? targetDb : "DEFAULT", from, to);

        try (Connection conn = DatabaseConfig.getConnection(targetDb);
                PreparedStatement ps = conn.prepareStatement(SQL_MATRIZ_VENTAS)) {

            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MatrizVentasRow row = new MatrizVentasRow();
                    String docNum = rs.getString("numero");
                    row.setNumero(docNum);
                    row.setFecha(rs.getString("fecha"));
                    row.setCiRif(rs.getString("ciRif"));
                    row.setNombreRazonSocial(rs.getString("nombreRazonSocial"));
                    row.setCoVen(rs.getString("coVen"));
                    row.setNombreVendedor(rs.getString("nombreVendedor"));
                    row.setTasa(getSafeDouble(rs, "tasa", docNum));
                    row.setCodigoArt(rs.getString("codigoArt"));
                    row.setDescripcion(rs.getString("descripcion"));
                    row.setCantidad(getSafeDouble(rs, "cantidad", docNum));
                    row.setPrecio(getSafeDouble(rs, "precio", docNum));
                    parseCompositeDiscounts(rs.getString("rawDiscounts"), row);
                    row.setTotalRenglon(getSafeDouble(rs, "totalRenglon", docNum));
                    row.setDescPctGlobal(getSafeDouble(rs, "descPctGlobal", docNum));
                    row.setRenglonDg(getSafeDouble(rs, "renglonDg", docNum));
                    row.setMontoIva(getSafeDouble(rs, "montoIva", docNum));
                    row.setIvaPct(getSafeDouble(rs, "ivaPct", docNum));
                    row.setTotRenglonIva(getSafeDouble(rs, "totRenglonIva", docNum));
                    row.setCostoVenta(getSafeDouble(rs, "costoVenta", docNum));
                    row.setTotalCostoVenta(getSafeDouble(rs, "totalCostoVenta", docNum));
                    row.setTotCvDp(getSafeDouble(rs, "totCvDp", docNum));
                    row.setMontoUtilidad(getSafeDouble(rs, "montoUtilidad", docNum));
                    row.setUtilPct(getSafeDouble(rs, "utilPct", docNum));
                    row.setCostoActual(getSafeDouble(rs, "costoActual", docNum));
                    row.setStockActual(getSafeDouble(rs, "stockActual", docNum));
                    row.setCodLinea(rs.getString("codLinea"));
                    row.setLinea(rs.getString("linea"));
                    row.setCodSub(rs.getString("codSub"));
                    row.setSubLinea(rs.getString("subLinea"));
                    row.setCodProveedor(rs.getString("codProveedor"));
                    row.setNombreProveedor(rs.getString("nombreProveedor"));
                    row.setZona(rs.getString("zona"));
                    row.setCiudad(rs.getString("ciudad"));
                    row.setCodProv(rs.getString("codProv"));
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

    /**
    /**
     * Parses a composite discount string like "3+0+4+10+6" into its components:
     * When 5 discounts are present:
     *   Index 0: DP (Descuento Producto)
     *   Index 1: DA (Descuento Adicional)
     *   Index 2: DCT (Descuento CT / Web)
     *   Index 3: DC (Descuento Cliente)
     *   Index 4: DV (Descuento Volumen)
     *
     * When 4 discounts (legacy) are present:
     *   Index 0: DP, Index 1: DA, Index 2: DCT, Index 3: DV.
     */
    private void parseCompositeDiscounts(String raw, MatrizVentasRow row) {
        if (raw == null || raw.isBlank()) {
            row.setDp(0);
            row.setDa(0);
            row.setDct(0);
            row.setDc(0);
            row.setDv(0);
            row.setDescPct(0);
            return;
        }

        String[] parts = raw.split("\\+");
        if (parts.length >= 5) {
            row.setDp(parsePart(parts, 0));
            row.setDa(parsePart(parts, 1));
            row.setDct(parsePart(parts, 2));
            row.setDc(parsePart(parts, 3));
            row.setDv(parsePart(parts, 4));
        } else {
            row.setDp(parsePart(parts, 0));
            row.setDa(parsePart(parts, 1));
            row.setDct(parsePart(parts, 2));
            row.setDc(0);
            row.setDv(parsePart(parts, 3));
        }

        row.setDescPct(row.getDp());
    }

    private double parsePart(String[] parts, int index) {
        if (index >= parts.length) return 0.0;
        try {
            return Double.parseDouble(parts[index].trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * Reads a column as String and safely parses it to double.
     * Returns 0.0 if the value is null, empty, or not a valid number.
     * Logs a warning for non-numeric values to help identify corrupt data.
     */
    private double getSafeDouble(ResultSet rs, String column, String docNum) {
        try {
            String raw = rs.getString(column);
            if (raw == null || raw.isBlank()) {
                return 0.0;
            }
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            System.err.println("[WARN] doc_num=" + docNum + " columna '" + column
                    + "' valor no numérico: \"" + e.getMessage() + "\" → usando 0.0");
            return 0.0;
        } catch (SQLException e) {
            System.err.println("[WARN] doc_num=" + docNum + " error leyendo columna '" + column
                    + "': " + e.getMessage() + " → usando 0.0");
            return 0.0;
        }
    }
}
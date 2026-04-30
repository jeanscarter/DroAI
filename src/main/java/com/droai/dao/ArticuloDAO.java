package com.droai.dao;

import com.droai.config.DatabaseConfig;
import com.droai.model.ArticuloRow;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para obtener el catálogo maestro de productos desde saArticulo (Profit
 * Plus).
 * "Marca" se mapea al proveedor principal (prov_des).
 */
public class ArticuloDAO {

    private static final String SQL_CATALOGO = """
            SELECT
                a.co_art        AS codigo,
                ISNULL(a.art_des, '')   AS descripcion,
                ISNULL(p.prov_des, '')  AS marca,
                ISNULL(stk.totalStock, 0) AS existencia,
                ISNULL(u.co_uni, '')    AS udm,
                0                       AS costoFabrica,
                ISNULL(a.porc_arancel, 0) AS arancelPct,
                ISNULL(a.prec_om, 0)    AS costoOm,
                ISNULL(p1.monto, 0)     AS precio1,
                ISNULL(p2.monto, 0)     AS precio2,
                ISNULL(p3.monto, 0)     AS precio3,
                ISNULL(p4.monto, 0)     AS precio4,
                ISNULL(i.porc, 0)       AS ivaPct,
                ISNULL(a.co_lin, '')    AS codLinea,
                ISNULL(l.lin_des, '')   AS linea,
                ISNULL(a.co_subl, '')   AS codSub,
                ISNULL(sl.subl_des, '') AS subLinea,
                ISNULL(ap.co_prov, '')  AS codProveedor,
                ISNULL(p.prov_des, '')  AS nombreProveedor,
                ISNULL(a.ref, '')       AS referencia,
                ISNULL(a.modelo, '')    AS modelo,
                ISNULL(a.cod_proc, '')  AS procedencia,
                ISNULL(a.peso, 0)       AS peso,
                ISNULL(a.volumen, 0)    AS volumen,
                ISNULL(a.ref, '')       AS codigoBarra,
                ISNULL(a.co_ubicacion, '') AS ubicacion,
                ISNULL(a.campo1, '')    AS campo1,
                ISNULL(a.campo2, '')    AS campo2,
                ISNULL(a.campo3, '')    AS campo3,
                ISNULL(a.campo4, '')    AS campo4,
                ISNULL(a.campo5, '')    AS campo5,
                ISNULL(a.campo6, '')    AS campo6,
                ISNULL(a.destaca, 0)    AS destacado,
                ISNULL(a.anulado, 0)    AS anulado,
                ISNULL(a.margen_min, 0) AS margenMin,
                ISNULL(a.margen_max, 0) AS margenMax
            FROM saArticulo a
            LEFT JOIN (SELECT co_art, co_prov FROM saArtProveedorReng WHERE reng_num = 1) ap
                ON a.co_art = ap.co_art
            LEFT JOIN saProveedor p
                ON ap.co_prov = p.co_prov
            LEFT JOIN saLineaArticulo l
                ON a.co_lin = l.co_lin
            LEFT JOIN saSubLinea sl
                ON a.co_subl = sl.co_subl
            LEFT JOIN (
                SELECT tipo_imp, MAX(porc_tasa) AS porc
                FROM saImpuestoSobreVentaReng
                GROUP BY tipo_imp
            ) i ON a.tipo_imp = i.tipo_imp
            LEFT JOIN (
                SELECT co_art, SUM(stock) AS totalStock
                FROM saStockAlmacen
                GROUP BY co_art
            ) stk ON a.co_art = stk.co_art
            LEFT JOIN (SELECT co_art, MAX(co_uni) AS co_uni FROM saArtUnidad GROUP BY co_art) u
                ON a.co_art = u.co_art
            LEFT JOIN (SELECT co_art, MAX(monto) AS monto FROM saArtPrecio WHERE co_precio = '01' GROUP BY co_art) p1
                ON a.co_art = p1.co_art
            LEFT JOIN (SELECT co_art, MAX(monto) AS monto FROM saArtPrecio WHERE co_precio = '02' GROUP BY co_art) p2
                ON a.co_art = p2.co_art
            LEFT JOIN (SELECT co_art, MAX(monto) AS monto FROM saArtPrecio WHERE co_precio = '03' GROUP BY co_art) p3
                ON a.co_art = p3.co_art
            LEFT JOIN (SELECT co_art, MAX(monto) AS monto FROM saArtPrecio WHERE co_precio = '05' GROUP BY co_art) p4
                ON a.co_art = p4.co_art
            ORDER BY a.co_art
            """;

    public List<ArticuloRow> fetchCatalogo() throws SQLException {
        List<ArticuloRow> rows = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
                PreparedStatement ps = conn.prepareStatement(SQL_CATALOGO);
                ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                ArticuloRow row = new ArticuloRow();
                String codigo = rs.getString("codigo");
                row.setCodigo(codigo);
                row.setDescripcion(rs.getString("descripcion"));
                row.setMarca(rs.getString("marca"));
                row.setExistencia(getSafeDouble(rs, "existencia", codigo));
                row.setUdm(rs.getString("udm"));
                row.setCostoFabrica(getSafeDouble(rs, "costoFabrica", codigo));
                row.setArancelPct(getSafeDouble(rs, "arancelPct", codigo));
                row.setCostoOm(getSafeDouble(rs, "costoOm", codigo));
                row.setPrecio1(getSafeDouble(rs, "precio1", codigo));
                row.setPrecio2(getSafeDouble(rs, "precio2", codigo));
                row.setPrecio3(getSafeDouble(rs, "precio3", codigo));
                row.setPrecio4(getSafeDouble(rs, "precio4", codigo));
                row.setIvaPct(getSafeDouble(rs, "ivaPct", codigo));
                row.setCodLinea(rs.getString("codLinea"));
                row.setLinea(rs.getString("linea"));
                row.setCodSub(rs.getString("codSub"));
                row.setSubLinea(rs.getString("subLinea"));
                row.setCodProveedor(rs.getString("codProveedor"));
                row.setNombreProveedor(rs.getString("nombreProveedor"));
                row.setReferencia(rs.getString("referencia"));
                row.setModelo(rs.getString("modelo"));
                row.setProcedencia(rs.getString("procedencia"));
                row.setPeso(getSafeDouble(rs, "peso", codigo));
                row.setVolumen(getSafeDouble(rs, "volumen", codigo));
                row.setCodigoBarra(rs.getString("codigoBarra"));
                row.setUbicacion(rs.getString("ubicacion"));
                row.setCampo1(rs.getString("campo1"));
                row.setCampo2(rs.getString("campo2"));
                row.setCampo3(rs.getString("campo3"));
                row.setCampo4(rs.getString("campo4"));
                row.setCampo5(rs.getString("campo5"));
                row.setCampo6(rs.getString("campo6"));
                row.setDestacado(rs.getInt("destacado") == 1);
                row.setAnulado(rs.getInt("anulado") == 1);
                row.setMargenMin(getSafeDouble(rs, "margenMin", codigo));
                row.setMargenMax(getSafeDouble(rs, "margenMax", codigo));
                rows.add(row);
            }
        }
        return rows;
    }

    private double getSafeDouble(ResultSet rs, String column, String codigo) {
        try {
            String raw = rs.getString(column);
            if (raw == null || raw.isBlank())
                return 0.0;
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            System.err.println("[WARN] co_art=" + codigo + " columna '" + column
                    + "' valor no numérico → usando 0.0");
            return 0.0;
        } catch (SQLException e) {
            System.err.println("[WARN] co_art=" + codigo + " error leyendo columna '" + column
                    + "': " + e.getMessage() + " → usando 0.0");
            return 0.0;
        }
    }
}

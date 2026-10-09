package com.droai.dao;

import com.droai.config.DatabaseConfig;
import com.droai.model.DescuentoAdicionalRow;
import com.droai.model.SesionUsuario;
import com.droai.service.ImportadorService;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DAO para la gestión de Descuento Adicional (DA).
 * Consulta y actualiza directamente la tabla saArticulo:
 * lic_grado_al (Descuento Adicional), fdesdesc (Fecha Desde), fhasdesc (Fecha Hasta), fvendesc (Fecha Vence).
 */
public class DescuentoAdicionalDAO {

    private static final String SQL_GET_ALL = """
            SELECT 
                a.co_art AS codigo,
                ISNULL(a.art_des, '') AS descripcion,
                ISNULL(p.prov_des, '') AS marca,
                ISNULL(a.modelo, '') AS modelo,
                ISNULL(a.ref, '') AS codigoBarra,
                ISNULL(pr.monto, 0) AS precio1,
                ISNULL(a.lic_grado_al, 0) AS descuentoDA,
                ISNULL(a.volumen, 0) AS volumen,
                ISNULL(CONVERT(varchar, a.fdesdesc, 23), '') AS fechaDesde,
                ISNULL(CONVERT(varchar, a.fhasdesc, 23), '') AS fechaHasta,
                ISNULL(CONVERT(varchar, a.fvendesc, 23), '') AS fechaVence,
                ISNULL(a.co_us_in, '') AS coUsIn,
                ISNULL(CONVERT(varchar, a.fe_us_in, 120), '') AS feUsIn,
                ISNULL(a.co_us_mo, '') AS coUsMo,
                ISNULL(CONVERT(varchar, a.fe_us_mo, 120), '') AS feUsMo,
                ISNULL(ap.co_prov, '') AS codProveedor,
                ISNULL(p.prov_des, '') AS nombreProveedor,
                ISNULL(a.co_lin, '') AS codLinea,
                ISNULL(l.lin_des, '') AS linea
            FROM saArticulo a
            LEFT JOIN (
                SELECT co_art, co_prov
                FROM (
                    SELECT co_art, co_prov,
                           ROW_NUMBER() OVER (PARTITION BY co_art ORDER BY reng_num) AS rn
                    FROM saArtProveedorReng
                ) ranked
                WHERE rn = 1
            ) ap ON a.co_art = ap.co_art
            LEFT JOIN saProveedor p ON ap.co_prov = p.co_prov
            LEFT JOIN saLineaArticulo l ON a.co_lin = l.co_lin
            LEFT JOIN (
                SELECT co_art, MAX(monto) AS monto 
                FROM saArtPrecio 
                WHERE co_precio = '01' 
                GROUP BY co_art
            ) pr ON a.co_art = pr.co_art
            ORDER BY a.co_art
            """;

    public List<DescuentoAdicionalRow> fetchDescuentosAdicionales() throws SQLException {
        List<DescuentoAdicionalRow> rows = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_GET_ALL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                DescuentoAdicionalRow row = new DescuentoAdicionalRow();
                row.setCodigo(rs.getString("codigo").trim());
                row.setDescripcion(rs.getString("descripcion").trim());
                row.setMarca(rs.getString("marca").trim());
                row.setModelo(rs.getString("modelo") != null ? rs.getString("modelo").trim() : "");
                row.setCodigoBarra(rs.getString("codigoBarra").trim());
                row.setPrecio1(rs.getDouble("precio1"));
                row.setDescuentoDA(rs.getDouble("descuentoDA"));
                row.setVolumen(rs.getDouble("volumen"));
                row.setFechaDesde(rs.getString("fechaDesde") != null ? rs.getString("fechaDesde").trim() : "");
                row.setFechaHasta(rs.getString("fechaHasta") != null ? rs.getString("fechaHasta").trim() : "");
                row.setFechaVence(rs.getString("fechaVence") != null ? rs.getString("fechaVence").trim() : "");
                row.setCoUsIn(rs.getString("coUsIn") != null ? rs.getString("coUsIn").trim() : "");
                row.setFeUsIn(rs.getString("feUsIn") != null ? rs.getString("feUsIn").trim() : "");
                row.setCoUsMo(rs.getString("coUsMo") != null ? rs.getString("coUsMo").trim() : "");
                row.setFeUsMo(rs.getString("feUsMo") != null ? rs.getString("feUsMo").trim() : "");
                row.setCodProveedor(rs.getString("codProveedor").trim());
                row.setNombreProveedor(rs.getString("nombreProveedor").trim());
                row.setCodLinea(rs.getString("codLinea").trim());
                row.setLinea(rs.getString("linea").trim());
                rows.add(row);
            }
        }
        return rows;
    }

    public void updateDescuentosAdicionales(List<String> codigosArticulos, double nuevoPorcentaje) throws SQLException {
        updateDescuentosAdicionales(codigosArticulos, nuevoPorcentaje, null, null);
    }

    public void updateDescuentosAdicionales(List<String> codigosArticulos, double nuevoPorcentaje, Date fechaDesde, Date fechaHasta) throws SQLException {
        if (codigosArticulos == null || codigosArticulos.isEmpty()) {
            return;
        }

        String coUsuario = SesionUsuario.isAutenticado()
                ? SesionUsuario.current().getCoUsuario() : "SYSTEM";

        String sql = """
                UPDATE saArticulo
                SET lic_grado_al = ?,
                    fdesdesc = ?,
                    fhasdesc = ?,
                    co_us_mo = ?,
                    fe_us_mo = GETDATE()
                WHERE co_art = ?
                """;

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (String coArt : codigosArticulos) {
                ps.setDouble(1, nuevoPorcentaje);
                if (fechaDesde != null) {
                    ps.setTimestamp(2, new Timestamp(fechaDesde.getTime()));
                } else {
                    ps.setNull(2, Types.TIMESTAMP);
                }
                if (fechaHasta != null) {
                    ps.setTimestamp(3, new Timestamp(fechaHasta.getTime()));
                } else {
                    ps.setNull(3, Types.TIMESTAMP);
                }
                ps.setString(4, coUsuario);
                ps.setString(5, coArt.trim());
                ps.addBatch();
            }

            ps.executeBatch();
        }
    }

    public void updateDescuentoAdicionalIndividual(String codigo, double nuevoPorcentaje) throws SQLException {
        updateDescuentoAdicionalIndividual(codigo, nuevoPorcentaje, null, null);
    }

    public void updateDescuentoAdicionalIndividual(String codigo, double nuevoPorcentaje, Date fechaDesde, Date fechaHasta) throws SQLException {
        String coUsuario = SesionUsuario.isAutenticado()
                ? SesionUsuario.current().getCoUsuario() : "SYSTEM";

        String sql = """
                UPDATE saArticulo
                SET lic_grado_al = ?,
                    fdesdesc = ?,
                    fhasdesc = ?,
                    co_us_mo = ?,
                    fe_us_mo = GETDATE()
                WHERE co_art = ?
                """;

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, nuevoPorcentaje);
            if (fechaDesde != null) {
                ps.setTimestamp(2, new Timestamp(fechaDesde.getTime()));
            } else {
                ps.setNull(2, Types.TIMESTAMP);
            }
            if (fechaHasta != null) {
                ps.setTimestamp(3, new Timestamp(fechaHasta.getTime()));
            } else {
                ps.setNull(3, Types.TIMESTAMP);
            }
            ps.setString(4, coUsuario);
            ps.setString(5, codigo.trim());
            ps.executeUpdate();
        }
    }

    public void updateFechasDescuentoAdicionalIndividual(String codigo, Date fechaDesde, Date fechaHasta) throws SQLException {
        String coUsuario = SesionUsuario.isAutenticado()
                ? SesionUsuario.current().getCoUsuario() : "SYSTEM";

        String sql = """
                UPDATE saArticulo
                SET fdesdesc = ?,
                    fhasdesc = ?,
                    co_us_mo = ?,
                    fe_us_mo = GETDATE()
                WHERE co_art = ?
                """;

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (fechaDesde != null) {
                ps.setTimestamp(1, new Timestamp(fechaDesde.getTime()));
            } else {
                ps.setNull(1, Types.TIMESTAMP);
            }
            if (fechaHasta != null) {
                ps.setTimestamp(2, new Timestamp(fechaHasta.getTime()));
            } else {
                ps.setNull(2, Types.TIMESTAMP);
            }
            ps.setString(3, coUsuario);
            ps.setString(4, codigo.trim());
            ps.executeUpdate();
        }
    }

    public void updateDescuentosAdicionalesMap(Map<String, Double> dctosMap, Date fechaDesde, Date fechaHasta) throws SQLException {
        if (dctosMap == null || dctosMap.isEmpty()) {
            return;
        }

        String coUsuario = SesionUsuario.isAutenticado()
                ? SesionUsuario.current().getCoUsuario() : "SYSTEM";

        String sqlCheckArticulo = """
                SELECT co_art FROM saArticulo
                WHERE LTRIM(RTRIM(co_art)) = ?
                   OR (LEN(?) <= 6 AND ISNUMERIC(?) = 1 AND LTRIM(RTRIM(co_art)) = RIGHT('000000' + ?, 6))
                   OR (LEN(?) <= 8 AND ISNUMERIC(?) = 1 AND LTRIM(RTRIM(co_art)) = RIGHT('00000000' + ?, 8))
                   OR LTRIM(RTRIM(ref)) = ?
                """;

        String sqlUpdate = """
                UPDATE saArticulo
                SET lic_grado_al = ?,
                    fdesdesc = ?,
                    fhasdesc = ?,
                    co_us_mo = ?,
                    fe_us_mo = GETDATE()
                WHERE co_art = ?
                """;

        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psCheck = conn.prepareStatement(sqlCheckArticulo);
                 PreparedStatement psUpdate = conn.prepareStatement(sqlUpdate)) {

                for (Map.Entry<String, Double> entry : dctosMap.entrySet()) {
                    String rawCode = entry.getKey() != null ? entry.getKey().trim() : "";
                    if (rawCode.isEmpty()) continue;
                    if (rawCode.endsWith(".0")) {
                        rawCode = rawCode.substring(0, rawCode.length() - 2).trim();
                    }

                    String matchedCoArt = null;
                    psCheck.setString(1, rawCode);
                    psCheck.setString(2, rawCode);
                    psCheck.setString(3, rawCode);
                    psCheck.setString(4, rawCode);
                    psCheck.setString(5, rawCode);
                    psCheck.setString(6, rawCode);
                    psCheck.setString(7, rawCode);
                    psCheck.setString(8, rawCode);

                    try (ResultSet rs = psCheck.executeQuery()) {
                        if (rs.next()) {
                            matchedCoArt = rs.getString("co_art").trim();
                        }
                    }

                    if (matchedCoArt == null) {
                        System.out.println("Advertencia DA: Se omitió el artículo porque no existe en saArticulo: " + rawCode);
                        continue;
                    }

                    double nuevoPorcentaje = entry.getValue();
                    Date fDesde = (nuevoPorcentaje > 0.0001) ? fechaDesde : null;
                    Date fHasta = (nuevoPorcentaje > 0.0001) ? fechaHasta : null;

                    psUpdate.setDouble(1, nuevoPorcentaje);
                    if (fDesde != null) {
                        psUpdate.setTimestamp(2, new Timestamp(fDesde.getTime()));
                    } else {
                        psUpdate.setNull(2, Types.TIMESTAMP);
                    }
                    if (fHasta != null) {
                        psUpdate.setTimestamp(3, new Timestamp(fHasta.getTime()));
                    } else {
                        psUpdate.setNull(3, Types.TIMESTAMP);
                    }
                    psUpdate.setString(4, coUsuario);
                    psUpdate.setString(5, matchedCoArt);
                    psUpdate.addBatch();
                }

                psUpdate.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public int updateDescuentosAdicionalesItems(List<ImportadorService.DescuentoDAImportItem> items, Date fallbackFechaDesde, Date fallbackFechaHasta) throws SQLException {
        if (items == null || items.isEmpty()) {
            return 0;
        }

        String coUsuario = SesionUsuario.isAutenticado()
                ? SesionUsuario.current().getCoUsuario() : "SYSTEM";

        String sqlCheckArticulo = """
                SELECT co_art FROM saArticulo
                WHERE LTRIM(RTRIM(co_art)) = ?
                   OR (LEN(?) <= 6 AND ISNUMERIC(?) = 1 AND LTRIM(RTRIM(co_art)) = RIGHT('000000' + ?, 6))
                   OR (LEN(?) <= 8 AND ISNUMERIC(?) = 1 AND LTRIM(RTRIM(co_art)) = RIGHT('00000000' + ?, 8))
                   OR LTRIM(RTRIM(ref)) = ?
                """;

        String sqlUpdate = """
                UPDATE saArticulo
                SET lic_grado_al = ?,
                    fdesdesc = ?,
                    fhasdesc = ?,
                    co_us_mo = ?,
                    fe_us_mo = GETDATE()
                WHERE co_art = ?
                """;

        int totalActualizados = 0;
        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psCheck = conn.prepareStatement(sqlCheckArticulo);
                 PreparedStatement psUpdate = conn.prepareStatement(sqlUpdate)) {

                for (ImportadorService.DescuentoDAImportItem item : items) {
                    String rawCode = item.getCodigoArticulo() != null ? item.getCodigoArticulo().trim() : "";
                    if (rawCode.isEmpty()) continue;
                    if (rawCode.endsWith(".0")) {
                        rawCode = rawCode.substring(0, rawCode.length() - 2).trim();
                    }

                    String matchedCoArt = null;
                    psCheck.setString(1, rawCode);
                    psCheck.setString(2, rawCode);
                    psCheck.setString(3, rawCode);
                    psCheck.setString(4, rawCode);
                    psCheck.setString(5, rawCode);
                    psCheck.setString(6, rawCode);
                    psCheck.setString(7, rawCode);
                    psCheck.setString(8, rawCode);

                    try (ResultSet rs = psCheck.executeQuery()) {
                        if (rs.next()) {
                            matchedCoArt = rs.getString("co_art").trim();
                        }
                    }

                    if (matchedCoArt == null) {
                        System.out.println("Advertencia DA: Se omitió el artículo porque no existe en saArticulo: " + rawCode);
                        continue;
                    }

                    double nuevoPorcentaje = item.getPorcentaje();
                    Date fDesde = (nuevoPorcentaje > 0.0001)
                            ? (item.getFechaDesde() != null ? item.getFechaDesde() : fallbackFechaDesde)
                            : null;
                    Date fHasta = (nuevoPorcentaje > 0.0001)
                            ? (item.getFechaHasta() != null ? item.getFechaHasta() : fallbackFechaHasta)
                            : null;

                    psUpdate.setDouble(1, nuevoPorcentaje);
                    if (fDesde != null) {
                        psUpdate.setTimestamp(2, new Timestamp(fDesde.getTime()));
                    } else {
                        psUpdate.setNull(2, Types.TIMESTAMP);
                    }
                    if (fHasta != null) {
                        psUpdate.setTimestamp(3, new Timestamp(fHasta.getTime()));
                    } else {
                        psUpdate.setNull(3, Types.TIMESTAMP);
                    }
                    psUpdate.setString(4, coUsuario);
                    psUpdate.setString(5, matchedCoArt);
                    psUpdate.addBatch();
                    totalActualizados++;
                }

                psUpdate.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
        return totalActualizados;
    }
}

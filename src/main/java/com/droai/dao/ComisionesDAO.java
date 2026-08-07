package com.droai.dao;

import com.droai.config.DatabaseConfig;
import com.droai.model.ComisionRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para la extracción de cobros y cálculo de comisiones.
 * Selecciona dinámicamente la base de datos según el rango de fechas:
 *   - Fechas ≤ 27/07/2026 → DROA2_A (base histórica/migrada)
 *   - Fechas > 27/07/2026 → DROA_A (base de producción actual)
 *   - Rango cruzado → consulta ambas y combina resultados
 */
public class ComisionesDAO {

    private static final Logger logger = LoggerFactory.getLogger(ComisionesDAO.class);

    /** Fecha de corte: hasta esta fecha inclusive se usa DROA2_A; después, DROA_A */
    private static final LocalDate FECHA_CORTE = LocalDate.of(2026, 7, 27);

    private static final String DB_HISTORICA  = "DROA2_A";
    private static final String DB_PRODUCCION = "DROA_A";

    public static class VendedorOption {
        private final String codigo;
        private final String nombre;

        public VendedorOption(String codigo, String nombre) {
            this.codigo = codigo;
            this.nombre = nombre;
        }

        public String getCodigo() {
            return codigo;
        }

        public String getNombre() {
            return nombre;
        }

        @Override
        public String toString() {
            return "[" + codigo + "] " + nombre;
        }
    }

    /**
     * Determina la(s) base(s) de datos a usar según el rango de fechas.
     * @return lista con 1 o 2 elementos [db, desde, hasta] agrupados.
     */
    public static class RangoDb {
        public final String database;
        public final LocalDate desde;
        public final LocalDate hasta;

        public RangoDb(String database, LocalDate desde, LocalDate hasta) {
            this.database = database;
            this.desde = desde;
            this.hasta = hasta;
        }
    }

    /**
     * Resuelve los rangos de BD según las fechas proporcionadas.
     */
    public static List<RangoDb> resolverRangos(LocalDate desde, LocalDate hasta) {
        List<RangoDb> rangos = new ArrayList<>();

        if (!hasta.isAfter(FECHA_CORTE)) {
            // Todo el rango cae en la base histórica
            rangos.add(new RangoDb(DB_HISTORICA, desde, hasta));
        } else if (desde.isAfter(FECHA_CORTE)) {
            // Todo el rango cae en la base de producción
            rangos.add(new RangoDb(DB_PRODUCCION, desde, hasta));
        } else {
            // Rango cruzado: parte en histórica, parte en producción
            rangos.add(new RangoDb(DB_HISTORICA, desde, FECHA_CORTE));
            rangos.add(new RangoDb(DB_PRODUCCION, FECHA_CORTE.plusDays(1), hasta));
        }

        return rangos;
    }

    /**
     * Obtiene la lista de vendedores activos registrados en saVendedor.
     * Usa la BD de producción (DROA_A) por defecto para vendedores.
     */
    public List<VendedorOption> fetchVendedores() {
        return fetchVendedores(DB_PRODUCCION);
    }

    /**
     * Obtiene la lista de vendedores activos desde la BD especificada.
     */
    public List<VendedorOption> fetchVendedores(String targetDb) {
        List<VendedorOption> list = new ArrayList<>();
        String sql = "SELECT RTRIM(co_ven) AS co_ven, RTRIM(ven_des) AS ven_des FROM saVendedor ORDER BY co_ven";

        try (Connection conn = DatabaseConfig.getConnection(targetDb);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                String coVen = rs.getString("co_ven");
                String venDes = rs.getString("ven_des");
                if (coVen != null && !coVen.isBlank()) {
                    list.add(new VendedorOption(coVen, venDes));
                }
            }
        } catch (SQLException e) {
            logger.error("Error al obtener vendedores de {}: {}", targetDb, e.getMessage(), e);
        }
        return list;
    }

    /**
     * Extrae y calcula los registros de comisiones para un rango de fechas y vendedor opcional.
     * Selecciona dinámicamente la BD según las fechas y filtra solo facturas cerradas (status = '1').
     * Si el rango cruza la fecha de corte (27/07/2026), consulta ambas BDs y combina resultados.
     */
    public List<ComisionRow> fetchComisiones(LocalDate desde, LocalDate hasta, String coVenFiltro) {
        List<RangoDb> rangos = resolverRangos(desde, hasta);
        List<ComisionRow> allResults = new ArrayList<>();
        int secuenciaGlobal = 1;

        for (RangoDb rango : rangos) {
            logger.info("Consultando comisiones en {} desde {} hasta {}", rango.database, rango.desde, rango.hasta);
            List<ComisionRow> parcial = fetchComisionesDesde(rango.database, rango.desde, rango.hasta, coVenFiltro, secuenciaGlobal);
            secuenciaGlobal += parcial.size();
            allResults.addAll(parcial);
        }

        return allResults;
    }

    /**
     * Retorna la(s) base(s) de datos que se usarán para un rango dado (para mostrar en UI).
     */
    public static String getDbLabel(LocalDate desde, LocalDate hasta) {
        List<RangoDb> rangos = resolverRangos(desde, hasta);
        if (rangos.size() == 1) {
            return rangos.get(0).database;
        } else {
            return DB_HISTORICA + " + " + DB_PRODUCCION;
        }
    }

    /**
     * Ejecuta la consulta de comisiones contra una BD específica.
     */
    private List<ComisionRow> fetchComisionesDesde(String targetDb, LocalDate desde, LocalDate hasta,
                                                    String coVenFiltro, int secInicial) {
        List<ComisionRow> result = new ArrayList<>();

        String sql = """
            SELECT 
                CASE 
                    WHEN RTRIM(r.co_tipo_doc) = 'AJNA' THEN 'DPP'
                    ELSE RTRIM(r.co_tipo_doc)
                END AS tipo_doc,
                CAST(CAST(RTRIM(r.nro_doc) AS int) AS varchar) AS numero_doc,
                ISNULL(RTRIM(d.n_control), '01') AS clase,
                d.fec_emis, 
                d.fec_venc, 
                ISNULL(t.fecha_che, c.fecha) AS fec_cobro,
                CAST(CAST(RTRIM(c.cob_num) AS int) AS varchar) AS num_cobro,
                DATEDIFF(day, d.fec_venc, ISNULL(t.fecha_che, c.fecha)) AS dias_calle,
                REPLACE(REPLACE(REPLACE(REPLACE(ISNULL(RTRIM(cli.rif), ''), 'J-', ''), 'V-', ''), 'G-', ''), 'E-', '') AS cod_cli,
                ISNULL(RTRIM(cli.cli_des), '') AS nom_cli,
                ISNULL(d.total_neto, r.mont_cob) AS monto_doc,
                ISNULL(r.dpcobro_porc_desc, 0) AS porc_desc,
                r.mont_cob AS monto_cobrado,
                ISNULL(d.total_bruto, r.mont_cob) AS base_comision,
                ISNULL(RTRIM(v.ven_des), '') AS vendedor,
                RTRIM(c.co_ven) AS co_ven,
                ISNULL(d.saldo, -1) AS saldo_factura,
                ISNULL((
                    SELECT TOP 1 CAST(CAST(RTRIM(r_psico.nro_doc) AS int) AS varchar)
                    FROM saCobroDocReng r_psico
                    JOIN saDocumentoVentaReng dr ON dr.nro_doc = r_psico.nro_doc AND dr.co_tipo_doc = r_psico.co_tipo_doc
                    JOIN saArticulo a ON dr.co_art = a.co_art
                    WHERE r_psico.cob_num = c.cob_num
                      AND RTRIM(r_psico.co_tipo_doc) = 'FACT'
                      AND RTRIM(a.co_lin) BETWEEN '000007' AND '000010'
                ), '#N/D') AS psico
            FROM saCobro c
            JOIN saCobroDocReng r ON c.cob_num = r.cob_num
            LEFT JOIN saCobroTPReng t ON c.cob_num = t.cob_num AND t.reng_num = 1
            LEFT JOIN saDocumentoVenta d ON RTRIM(r.nro_doc) = RTRIM(d.nro_doc) AND RTRIM(r.co_tipo_doc) = RTRIM(d.co_tipo_doc)
            LEFT JOIN saCliente cli ON c.co_cli = cli.co_cli
            LEFT JOIN saVendedor v ON c.co_ven = v.co_ven
            WHERE c.fecha BETWEEN ? AND ?
              AND ISNULL(c.anulado, 0) = 0
              AND RTRIM(r.co_tipo_doc) IN ('FACT', 'N/CR', 'DPP', 'AJNA')
              AND RTRIM(ISNULL(v.ven_des, '')) NOT IN ('EMPLEADOS', 'OFICINA')
              AND RTRIM(c.co_ven) NOT IN ('EMP', 'OFI')
              AND ISNULL(d.saldo, 0) = 0
              AND ABS(r.mont_cob) > 0.01
            """;

        if (coVenFiltro != null && !coVenFiltro.isBlank() && !"TODOS".equalsIgnoreCase(coVenFiltro)) {
            sql += " AND RTRIM(c.co_ven) = ? ";
        }
        sql += " ORDER BY c.fecha, c.cob_num, r.nro_doc";

        try (Connection conn = DatabaseConfig.getConnection(targetDb);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, java.sql.Date.valueOf(desde));
            ps.setDate(2, java.sql.Date.valueOf(hasta));

            if (coVenFiltro != null && !coVenFiltro.isBlank() && !"TODOS".equalsIgnoreCase(coVenFiltro)) {
                ps.setString(3, coVenFiltro.trim());
            }

            try (ResultSet rs = ps.executeQuery()) {
                int sec = secInicial;
                while (rs.next()) {
                    ComisionRow row = new ComisionRow();
                    row.setNumero(sec++);
                    row.setTipoDoc(rs.getString("tipo_doc"));
                    row.setNumeroDocumento(rs.getString("numero_doc"));
                    row.setClase(rs.getString("clase"));

                    Date fecEmis = rs.getDate("fec_emis");
                    row.setFechaEmision(fecEmis != null ? fecEmis.toLocalDate() : null);

                    Date fecVenc = rs.getDate("fec_venc");
                    row.setFechaVencimiento(fecVenc != null ? fecVenc.toLocalDate() : null);

                    Date fecCob = rs.getDate("fec_cobro");
                    row.setFechaCobro(fecCob != null ? fecCob.toLocalDate() : null);

                    row.setNumeroCobro(rs.getString("num_cobro"));

                    int dias = rs.getInt("dias_calle");
                    row.setDiasCalle(dias);

                    row.setCodigoCliente(rs.getString("cod_cli"));
                    row.setNombreCliente(rs.getString("nom_cli"));

                    double montoDoc = rs.getDouble("monto_doc");
                    double porcDesc = rs.getDouble("porc_desc");
                    double montCob = rs.getDouble("monto_cobrado");
                    double baseCom = rs.getDouble("base_comision");

                    // Excluir montos de 0.01 (+0.01 o -0.01) por ser ajustes de redondeo/sistema
                    if (Math.abs(montCob) <= 0.01) {
                        continue;
                    }

                    String tipoDoc = row.getTipoDoc();
                    String codVen = rs.getString("co_ven");

                    // Factura cerrada = saldo igual a 0 (completamente cobrada)
                    // saldo = -1 indica que no se encontró la factura (LEFT JOIN sin match)
                    double saldoFactura = rs.getDouble("saldo_factura");
                    row.setFacturaCerrada(saldoFactura == 0);

                    // Ajustes de signo si es N/CR o DPP
                    if ("N/CR".equalsIgnoreCase(tipoDoc) || "DPP".equalsIgnoreCase(tipoDoc)) {
                        if (montCob > 0) montCob = -montCob;
                        if (baseCom > 0) baseCom = -baseCom;
                        if (montoDoc > 0) montoDoc = -montoDoc;
                    }

                    row.setMontoDocumento(montoDoc);
                    row.setPorcDesc(porcDesc);
                    row.setMontoCobrado(montCob);
                    row.setBaseComision(baseCom);
                    row.setCodigoVendedor(codVen);
                    row.setNombreVendedor(rs.getString("vendedor"));

                    // Cálculo de % Comisión según regla de Días Calle
                    double pctComision;
                    if ("KG".equalsIgnoreCase(codVen) || "KARINA GARCIA".equalsIgnoreCase(row.getNombreVendedor())) {
                        pctComision = 1.00;
                    } else if ("N/CR".equalsIgnoreCase(tipoDoc)) {
                        pctComision = (dias > 12) ? 0.0 : 1.50;
                    } else {
                        if (dias <= 2) {
                            pctComision = 1.50;
                        } else if (dias <= 7) {
                            pctComision = 0.75;
                        } else if (dias <= 12) {
                            pctComision = 0.30;
                        } else {
                            pctComision = 0.00;
                        }
                    }

                    String psicoVal = rs.getString("psico");
                    row.setPorcComision(pctComision);
                    row.setMontoComision(baseCom * (pctComision / 100.0));
                    row.setPsico(psicoVal);

                    result.add(row);

                    // Si es psicotrópico y el vendedor original no es Karina García, duplicar para Karina García con 1%
                    if (psicoVal != null && !"#N/D".equalsIgnoreCase(psicoVal.trim()) 
                            && !"KG".equalsIgnoreCase(codVen) 
                            && !"KARINA GARCIA".equalsIgnoreCase(row.getNombreVendedor())) {

                        ComisionRow kgRow = new ComisionRow();
                        kgRow.setNumero(sec++);
                        kgRow.setTipoDoc(row.getTipoDoc());
                        kgRow.setNumeroDocumento(row.getNumeroDocumento());
                        kgRow.setClase(row.getClase());
                        kgRow.setFechaEmision(row.getFechaEmision());
                        kgRow.setFechaVencimiento(row.getFechaVencimiento());
                        kgRow.setFechaCobro(row.getFechaCobro());
                        kgRow.setNumeroCobro(row.getNumeroCobro());
                        kgRow.setDiasCalle(row.getDiasCalle());
                        kgRow.setCodigoCliente(row.getCodigoCliente());
                        kgRow.setNombreCliente(row.getNombreCliente());
                        kgRow.setMontoDocumento(row.getMontoDocumento());
                        kgRow.setPorcDesc(row.getPorcDesc());
                        kgRow.setMontoCobrado(row.getMontoCobrado());
                        kgRow.setBaseComision(row.getBaseComision());
                        kgRow.setFacturaCerrada(row.isFacturaCerrada());
                        kgRow.setPsico(psicoVal);

                        kgRow.setCodigoVendedor("KG");
                        kgRow.setNombreVendedor("KARINA GARCIA");
                        kgRow.setPorcComision(1.00);
                        kgRow.setMontoComision(baseCom * (1.00 / 100.0));

                        result.add(kgRow);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error al consultar comisiones en {}: {}", targetDb, e.getMessage(), e);
        }

        logger.info("Obtenidos {} registros de comisiones desde {}", result.size(), targetDb);
        return result;
    }
}

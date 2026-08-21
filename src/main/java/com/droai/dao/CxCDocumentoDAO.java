package com.droai.dao;

import com.droai.config.DatabaseConfig;
import com.droai.model.CxCDocumentoRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para la consulta de Estado de Cuentas por Cobrar (CxC) Multimoneda (USD).
 * Consulta saDocumentoVenta con filtros por rango de fechas de emisión, condición Sin Cancelar (saldo != 0)
 * y moneda USD en la base de datos de producción (DROA_A).
 */
public class CxCDocumentoDAO {

    private static final Logger logger = LoggerFactory.getLogger(CxCDocumentoDAO.class);
    private static final String DB_PRODUCCION = "DROA_A";

    public static String getDbLabel(LocalDate desde, LocalDate hasta) {
        return DB_PRODUCCION;
    }

    /**
     * Consulta los documentos de cuentas por cobrar en el rango de fechas especificado.
     *
     * @param desde Fecha inicial de emisión
     * @param hasta Fecha final de emisión
     * @param fechaReferenciaVenc Fecha de corte para cálculo de días de vencimiento (generalmente hoy o fecha hasta)
     * @return Lista de filas CxCDocumentoRow
     */
    public List<CxCDocumentoRow> fetchDocumentosCxC(LocalDate desde, LocalDate hasta, LocalDate fechaReferenciaVenc) {
        logger.info("Consultando CxC en {} desde {} hasta {}", DB_PRODUCCION, desde, hasta);
        return fetchDocumentosDesde(DB_PRODUCCION, desde, hasta, fechaReferenciaVenc);
    }

    private List<CxCDocumentoRow> fetchDocumentosDesde(String targetDb, LocalDate desde, LocalDate hasta, LocalDate fechaRefVenc) {
        List<CxCDocumentoRow> result = new ArrayList<>();

        // En Profit Plus:
        // saDocumentoVenta contiene los documentos de venta / CxC.
        // d.total_bruto representa el Monto Neto (subtotal antes de IVA / base imponible).
        // d.total_neto y d.saldo representan los montos totales con IVA.
        // co_mone suele ser '0002' o 'USD' para la moneda extranjera.
        // Si co_mone es US$ o 0002 o dolares, filtramos los documentos USD sin cancelar (saldo <> 0).
        String sql = """
            SELECT
                ISNULL(RTRIM(cli.co_cli), RTRIM(d.co_cli)) AS cod_cliente,
                ISNULL(RTRIM(cli.rif), '') AS rif_cliente,
                ISNULL(RTRIM(sg.seg_des), ISNULL(RTRIM(cli.co_seg), '')) AS grupo_cliente,
                ISNULL(RTRIM(cli.cli_des), '') AS nom_cliente,
                CAST(CAST(RTRIM(d.nro_doc) AS int) AS varchar) AS num_factura,
                RTRIM(d.co_tipo_doc) AS tipo_doc,
                ISNULL(RTRIM(t.tipo_mov), 'DE') AS tipo_mov,
                d.fec_emis,
                d.fec_venc,
                d.total_bruto AS neto,
                d.monto_imp AS iva,
                d.saldo,
                d.tasa,
                ISNULL(RTRIM(cli.co_ven), RTRIM(d.co_ven)) AS cod_vendedor,
                ISNULL(RTRIM(vcli.ven_des), ISNULL(RTRIM(vdoc.ven_des), ISNULL(RTRIM(cli.co_ven), RTRIM(d.co_ven)))) AS nom_vendedor,
                ISNULL(RTRIM(d.observa), '') AS observa,
                ISNULL(RTRIM(d.campo1), '') AS campo1,
                ISNULL(RTRIM(d.campo2), '') AS campo2,
                ISNULL(RTRIM(d.co_mone), '') AS co_mone
            FROM saDocumentoVenta d
            LEFT JOIN saCliente cli ON d.co_cli = cli.co_cli
            LEFT JOIN saSegmento sg ON cli.co_seg = sg.co_seg
            LEFT JOIN saVendedor vcli ON cli.co_ven = vcli.co_ven
            LEFT JOIN saVendedor vdoc ON d.co_ven = vdoc.co_ven
            LEFT JOIN saTipoDocumento t ON d.co_tipo_doc = t.co_tipo_doc
            WHERE CAST(d.fec_emis AS date) BETWEEN ? AND ?
              AND ISNULL(d.anulado, 0) = 0
              AND ABS(d.saldo) > 0.001
              AND RTRIM(ISNULL(d.co_mone, '')) IN ('0002', 'USD', 'US$', 'DOLAR', 'DOLARES')
            ORDER BY d.fec_venc ASC, d.nro_doc ASC
        """;

        try (Connection conn = DatabaseConfig.getConnection(targetDb);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, java.sql.Date.valueOf(desde));
            ps.setDate(2, java.sql.Date.valueOf(hasta));

            try (ResultSet rs = ps.executeQuery()) {
                LocalDate refDate = (fechaRefVenc != null) ? fechaRefVenc : LocalDate.now();

                while (rs.next()) {
                    CxCDocumentoRow row = new CxCDocumentoRow();

                    row.setCodigoCliente(rs.getString("cod_cliente"));
                    row.setRifCliente(rs.getString("rif_cliente"));
                    row.setGrupoCliente(rs.getString("grupo_cliente"));
                    row.setCliente(rs.getString("nom_cliente"));
                    row.setFactura(rs.getString("num_factura"));
                    row.setTipoDoc(rs.getString("tipo_doc"));

                    String tipoMov = rs.getString("tipo_mov");
                    int sign = "CR".equalsIgnoreCase(tipoMov) ? -1 : 1;

                    Date fecEmis = rs.getDate("fec_emis");
                    row.setFechaEmision(fecEmis != null ? fecEmis.toLocalDate() : null);

                    Date fecVenc = rs.getDate("fec_venc");
                    LocalDate ldtVenc = (fecVenc != null) ? fecVenc.toLocalDate() : (row.getFechaEmision() != null ? row.getFechaEmision() : refDate);
                    row.setFechaVencimiento(ldtVenc);

                    // Cálculo de días de vencimiento respecto a la fecha de corte
                    // En Excel: TODAY() - FEC_VENC (positivo = ya venció, negativo = por vencer)
                    int diasVenc = (int) java.time.temporal.ChronoUnit.DAYS.between(ldtVenc, refDate);
                    row.setDiasVencimiento(diasVenc);

                    double netoRaw = rs.getDouble("neto");
                    double ivaRaw = rs.getDouble("iva");
                    double saldoRaw = rs.getDouble("saldo");
                    double tasa = rs.getDouble("tasa");
                    if (tasa <= 0) tasa = 1.0;

                    // En Profit Plus, total_bruto (neto sin IVA), monto_imp (IVA) y saldo se almacenan en Bolívares (moneda base).
                    // Para obtener el monto USD, dividimos entre la tasa de cambio y aplicamos signo por tipo_mov.
                    double netoUSD = (netoRaw / tasa) * sign;
                    double ivaUSD = (ivaRaw / tasa) * sign;
                    double saldoUSD = (saldoRaw / tasa) * sign;

                    row.setNeto(netoUSD);
                    row.setIva(ivaUSD);
                    row.setSaldo(saldoUSD);
                    row.setTasa(tasa);
                    row.setTotalBs(saldoRaw * sign); // saldoRaw ya está en Bs (con signo)

                    // Clasificación en bandas de vencimiento (en USD)
                    if (diasVenc <= 0) {
                        row.setPorVencer(saldoUSD);
                        row.setVencido1a30(0);
                        row.setVencido31a60(0);
                        row.setVencido61a90(0);
                        row.setVencidoMas91(0);
                    } else if (diasVenc <= 30) {
                        row.setPorVencer(0);
                        row.setVencido1a30(saldoUSD);
                        row.setVencido31a60(0);
                        row.setVencido61a90(0);
                        row.setVencidoMas91(0);
                    } else if (diasVenc <= 60) {
                        row.setPorVencer(0);
                        row.setVencido1a30(0);
                        row.setVencido31a60(saldoUSD);
                        row.setVencido61a90(0);
                        row.setVencidoMas91(0);
                    } else if (diasVenc <= 90) {
                        row.setPorVencer(0);
                        row.setVencido1a30(0);
                        row.setVencido31a60(0);
                        row.setVencido61a90(saldoUSD);
                        row.setVencidoMas91(0);
                    } else {
                        row.setPorVencer(0);
                        row.setVencido1a30(0);
                        row.setVencido31a60(0);
                        row.setVencido61a90(0);
                        row.setVencidoMas91(saldoUSD);
                    }

                    String codVen = rs.getString("cod_vendedor");
                    String nomVen = rs.getString("nom_vendedor");
                    row.setCodVendedor(codVen != null ? codVen.trim() : "");
                    row.setNombreVendedor(nomVen != null ? nomVen.trim() : "");

                    // Analista asignado (D-H: Denisse Hernandez, F-E: Francisco Echeto, J-S: Jovana Sanchez)
                    row.setAnalista(determinarAnalista(row.getCodVendedor(), row.getNombreVendedor()));

                    // Observaciones / Pedido
                    String obs = rs.getString("observa");
                    String c1 = rs.getString("campo1");
                    String c2 = rs.getString("campo2");
                    String pedidoText = !obs.isBlank() ? obs : (!c1.isBlank() ? c1 : c2);
                    row.setPedido(pedidoText);

                    // Marca de Factura Impaga (F-I)
                    boolean tieneMarcaFI = pedidoText.toUpperCase().contains("*FI*") 
                                        || pedidoText.toUpperCase().contains("F-I")
                                        || "F-I".equalsIgnoreCase(c1)
                                        || "F-I".equalsIgnoreCase(c2);
                    row.setFacturaImpaga(tieneMarcaFI ? "F-I" : "");

                    result.add(row);
                }
            }
        } catch (SQLException e) {
            logger.error("Error al consultar documentos de CxC en {}: {}", targetDb, e.getMessage(), e);
        }

        // Orden por defecto: 1° Vencimiento (antiguo a nuevo), 2° Factura (A-Z)
        result.sort(java.util.Comparator
                .comparing(CxCDocumentoRow::getFechaVencimiento, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()))
                .thenComparing(r -> r.getFactura() != null ? r.getFactura() : "", String.CASE_INSENSITIVE_ORDER)
        );

        logger.info("Obtenidos {} documentos de CxC desde {}", result.size(), targetDb);
        return result;
    }

    /**
     * Mapeo de analistas de cobranza según el vendedor:
     * - D-H: Denisse Hernandez
     * - F-E: Francisco Echeto
     * - J-S: Jovana Sánchez
     */
    public static String determinarAnalista(String codVen, String nomVen) {
        String cod = (codVen != null) ? codVen.trim().toUpperCase() : "";
        String nom = (nomVen != null) ? nomVen.trim().toUpperCase() : "";

        // J-S: Jovana Sánchez
        if (nom.contains("ANSONY") || nom.contains("EDWARD") || nom.contains("JOVANA") || nom.contains("SANCHEZ")
                || "AB".equals(cod) || "LB".equals(cod) || "ES".equals(cod) || "DM".equals(cod) && nom.contains("EDWARD")) {
            return "J-S";
        }

        // D-H: Denisse Hernandez
        if (nom.contains("CINTHIA") || nom.contains("LUIS GONZALEZ") || nom.contains("OMAR ZEA")
                || nom.contains("ALEJANDRA") || nom.contains("DENISSE") || nom.contains("HERNANDEZ")
                || "CC".equals(cod) || "LG".equals(cod) || "OZ".equals(cod) || "ABB".equals(cod)) {
            return "D-H";
        }

        // F-E: Francisco Echeto
        if (nom.contains("EULER") || nom.contains("ALYELICK") || nom.contains("JESUS CARRASQUERO")
                || nom.contains("OFICINA") || nom.contains("FRANCISCO") || nom.contains("ECHETO")
                || "EC".equals(cod) || "GF".equals(cod) || "AR".equals(cod) || "MP".equals(cod)
                || "OFI".equals(cod) || "01".equals(cod) || "JG".equals(cod) || "KC".equals(cod)
                || "VI".equals(cod) || "JH".equals(cod)) {
            return "F-E";
        }

        return "";
    }
}

package com.droai.dao;

import com.droai.config.DatabaseConfig;
import com.droai.model.NotaCreditoModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para consulta, navegación y gestión de Notas de Crédito / Documentos de Venta (saDocumentoVenta).
 */
public class NotaCreditoDAO {

    private static final Logger logger = LoggerFactory.getLogger(NotaCreditoDAO.class);
    private static final String DB_PRODUCCION = "DROA_A";

    private static final String SQL_SELECT_DOC = """
        SELECT
            RTRIM(d.nro_doc) AS nro_doc,
            RTRIM(d.co_tipo_doc) AS co_tipo_doc,
            ISNULL(RTRIM(t.descrip), RTRIM(d.co_tipo_doc)) AS tipo_doc_desc,
            ISNULL(RTRIM(t.tipo_mov), 'CR') AS tipo_mov,
            d.fec_emis,
            d.fec_venc,
            d.fec_reg,
            d.feccom AS fec_cheque,
            ISNULL(RTRIM(d.n_control), '') AS n_control,
            ISNULL(RTRIM(d.observa), '') AS descripcion,
            ISNULL(d.impresa, 0) AS impresa,
            ISNULL(d.anulado, 0) AS anulado,
            
            -- Cliente
            ISNULL(RTRIM(cli.co_cli), RTRIM(d.co_cli)) AS co_cli,
            ISNULL(RTRIM(cli.cli_des), '') AS cli_des,
            ISNULL(RTRIM(cli.rif), '') AS rif,
            ISNULL(RTRIM(cli.direc1), '') AS direccion,
            ISNULL(RTRIM(cli.telefonos), '') AS telefonos,
            ISNULL(RTRIM(cli.email), '') AS email,
            
            -- Vendedor
            ISNULL(RTRIM(ven.co_ven), RTRIM(d.co_ven)) AS co_ven,
            ISNULL(RTRIM(ven.ven_des), RTRIM(d.co_ven)) AS ven_des,
            
            -- Moneda
            ISNULL(RTRIM(d.co_mone), 'USD') AS co_mone,
            ISNULL(RTRIM(m.mone_des), 'DOLAR AMERICANO') AS mone_des,
            ISNULL(d.tasa, 1.0) AS tasa,
            ISNULL(RTRIM(d.tipo_imp), '7') AS tipo_imp,
            
            -- Documento origen / afectado
            ISNULL(RTRIM(d.doc_orig), '') AS doc_orig,
            ISNULL(RTRIM(d.nro_orig), '') AS nro_orig,
            orig.fec_emis AS fec_emis_orig,
            ISNULL(RTRIM(orig.n_control), '') AS n_control_orig,
            ISNULL(orig.total_bruto, 0.0) AS subtotal_orig_bs,
            ISNULL(orig.monto_imp, 0.0) AS iva_orig_bs,
            ISNULL(orig.total_neto, 0.0) AS total_orig_bs,
            
            -- Factura de respaldo si doc_orig='FACT' o saFacturaVenta
            fac.fec_emis AS fac_fec_emis,
            ISNULL(RTRIM(fac.n_control), '') AS fac_n_control,
            ISNULL(fac.total_bruto, 0.0) AS fac_subtotal_bs,
            ISNULL(fac.monto_imp, 0.0) AS fac_iva_bs,
            ISNULL(fac.total_neto, 0.0) AS fac_total_bs,
            
            -- Montos
            ISNULL(d.total_bruto, 0.0) AS total_bruto,
            ISNULL(d.monto_desc_glob, 0.0) AS monto_desc_glob,
            ISNULL(RTRIM(d.porc_desc_glob), '') AS porc_desc_glob,
            ISNULL(d.monto_reca, 0.0) AS monto_reca,
            ISNULL(RTRIM(d.porc_reca), '') AS porc_reca,
            ISNULL(d.total_neto, 0.0) AS total_neto,
            ISNULL(d.monto_imp, 0.0) AS monto_imp,
            ISNULL(d.monto_imp2, 0.0) AS monto_imp2,
            ISNULL(d.monto_imp3, 0.0) AS monto_imp3,
            ISNULL(d.otros1, 0.0) AS otros1,
            ISNULL(d.otros2, 0.0) AS otros2,
            ISNULL(d.otros3, 0.0) AS otros3,
            ISNULL(d.adicional, 0.0) AS adicional,
            ISNULL(d.saldo, 0.0) AS saldo,
            
            -- Banco y Cheque
            ISNULL(RTRIM(d.nro_che), '') AS nro_che,
            ISNULL(RTRIM(d.mov_ban), '') AS mov_ban,
            ISNULL(RTRIM(d.num_comprobante), '') AS num_comprobante,
            
            -- Campos Adicionales y Auditoría
            ISNULL(RTRIM(d.campo1), '') AS campo1,
            ISNULL(RTRIM(d.campo2), '') AS campo2,
            ISNULL(RTRIM(d.campo3), '') AS campo3,
            ISNULL(RTRIM(d.campo4), '') AS campo4,
            ISNULL(RTRIM(d.campo5), '') AS campo5,
            ISNULL(RTRIM(d.campo6), '') AS campo6,
            ISNULL(RTRIM(d.campo7), '') AS campo7,
            ISNULL(RTRIM(d.campo8), '') AS campo8,
            ISNULL(RTRIM(d.co_us_in), '') AS co_us_in,
            d.fe_us_in,
            ISNULL(RTRIM(d.co_us_mo), '') AS co_us_mo,
            d.fe_us_mo,
            ISNULL(RTRIM(d.co_sucu_in), '') AS co_sucu_in,
            ISNULL(RTRIM(d.co_sucu_mo), '') AS co_sucu_mo
        FROM saDocumentoVenta d
        LEFT JOIN saTipoDocumento t ON d.co_tipo_doc = t.co_tipo_doc
        LEFT JOIN saCliente cli ON d.co_cli = cli.co_cli
        LEFT JOIN saVendedor ven ON d.co_ven = ven.co_ven
        LEFT JOIN saMoneda m ON d.co_mone = m.co_mone
        LEFT JOIN saDocumentoVenta orig ON RTRIM(d.nro_orig) = RTRIM(orig.nro_doc) AND RTRIM(d.doc_orig) = RTRIM(orig.co_tipo_doc)
        LEFT JOIN saFacturaVenta fac ON RTRIM(d.nro_orig) = RTRIM(fac.doc_num)
        WHERE RTRIM(d.nro_doc) = ? AND RTRIM(d.co_tipo_doc) = ?
    """;

    /**
     * Consulta una Nota de Crédito / Documento de Venta específico.
     */
    public NotaCreditoModel consultarDocumento(String nroDoc, String coTipoDoc) {
        if (nroDoc == null || nroDoc.isBlank()) return null;
        String tipo = (coTipoDoc != null && !coTipoDoc.isBlank()) ? coTipoDoc.trim() : "N/CR";
        String nroLimpio = nroDoc.trim();

        try (Connection conn = DatabaseConfig.getConnection(DB_PRODUCCION);
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_DOC)) {

            ps.setString(1, nroLimpio);
            ps.setString(2, tipo);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToModel(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error al consultar documento {} tipo {}: {}", nroDoc, coTipoDoc, e.getMessage(), e);
        }
        return null;
    }

    /**
     * Busca por término clave (número de documento, control, campo5 o cliente).
     */
    public List<NotaCreditoModel> buscarNotasCredito(String filtro, int limit) {
        List<NotaCreditoModel> list = new ArrayList<>();
        String sql = """
            SELECT TOP (?)
                RTRIM(d.nro_doc) AS nro_doc,
                RTRIM(d.co_tipo_doc) AS co_tipo_doc,
                ISNULL(RTRIM(t.descrip), RTRIM(d.co_tipo_doc)) AS tipo_doc_desc,
                ISNULL(RTRIM(t.tipo_mov), 'CR') AS tipo_mov,
                d.fec_emis,
                d.fec_venc,
                d.fec_reg,
                d.feccom AS fec_cheque,
                ISNULL(RTRIM(d.n_control), '') AS n_control,
                ISNULL(RTRIM(d.observa), '') AS descripcion,
                ISNULL(d.impresa, 0) AS impresa,
                ISNULL(d.anulado, 0) AS anulado,
                ISNULL(RTRIM(cli.co_cli), RTRIM(d.co_cli)) AS co_cli,
                ISNULL(RTRIM(cli.cli_des), '') AS cli_des,
                ISNULL(RTRIM(cli.rif), '') AS rif,
                ISNULL(RTRIM(cli.direc1), '') AS direccion,
                ISNULL(RTRIM(cli.telefonos), '') AS telefonos,
                ISNULL(RTRIM(cli.email), '') AS email,
                ISNULL(RTRIM(ven.co_ven), RTRIM(d.co_ven)) AS co_ven,
                ISNULL(RTRIM(ven.ven_des), RTRIM(d.co_ven)) AS ven_des,
                ISNULL(RTRIM(d.co_mone), 'USD') AS co_mone,
                ISNULL(RTRIM(m.mone_des), 'DOLAR AMERICANO') AS mone_des,
                ISNULL(d.tasa, 1.0) AS tasa,
                ISNULL(RTRIM(d.tipo_imp), '7') AS tipo_imp,
                ISNULL(RTRIM(d.doc_orig), '') AS doc_orig,
                ISNULL(RTRIM(d.nro_orig), '') AS nro_orig,
                orig.fec_emis AS fec_emis_orig,
                ISNULL(RTRIM(orig.n_control), '') AS n_control_orig,
                ISNULL(orig.total_bruto, 0.0) AS subtotal_orig_bs,
                ISNULL(orig.monto_imp, 0.0) AS iva_orig_bs,
                ISNULL(orig.total_neto, 0.0) AS total_orig_bs,
                fac.fec_emis AS fac_fec_emis,
                ISNULL(RTRIM(fac.n_control), '') AS fac_n_control,
                ISNULL(fac.total_bruto, 0.0) AS fac_subtotal_bs,
                ISNULL(fac.monto_imp, 0.0) AS fac_iva_bs,
                ISNULL(fac.total_neto, 0.0) AS fac_total_bs,
                ISNULL(d.total_bruto, 0.0) AS total_bruto,
                ISNULL(d.monto_desc_glob, 0.0) AS monto_desc_glob,
                ISNULL(RTRIM(d.porc_desc_glob), '') AS porc_desc_glob,
                ISNULL(d.monto_reca, 0.0) AS monto_reca,
                ISNULL(RTRIM(d.porc_reca), '') AS porc_reca,
                ISNULL(d.total_neto, 0.0) AS total_neto,
                ISNULL(d.monto_imp, 0.0) AS monto_imp,
                ISNULL(d.monto_imp2, 0.0) AS monto_imp2,
                ISNULL(d.monto_imp3, 0.0) AS monto_imp3,
                ISNULL(d.otros1, 0.0) AS otros1,
                ISNULL(d.otros2, 0.0) AS otros2,
                ISNULL(d.otros3, 0.0) AS otros3,
                ISNULL(d.adicional, 0.0) AS adicional,
                ISNULL(d.saldo, 0.0) AS saldo,
                ISNULL(RTRIM(d.nro_che), '') AS nro_che,
                ISNULL(RTRIM(d.mov_ban), '') AS mov_ban,
                ISNULL(RTRIM(d.num_comprobante), '') AS num_comprobante,
                ISNULL(RTRIM(d.campo1), '') AS campo1,
                ISNULL(RTRIM(d.campo2), '') AS campo2,
                ISNULL(RTRIM(d.campo3), '') AS campo3,
                ISNULL(RTRIM(d.campo4), '') AS campo4,
                ISNULL(RTRIM(d.campo5), '') AS campo5,
                ISNULL(RTRIM(d.campo6), '') AS campo6,
                ISNULL(RTRIM(d.campo7), '') AS campo7,
                ISNULL(RTRIM(d.campo8), '') AS campo8,
                ISNULL(RTRIM(d.co_us_in), '') AS co_us_in,
                d.fe_us_in,
                ISNULL(RTRIM(d.co_us_mo), '') AS co_us_mo,
                d.fe_us_mo,
                ISNULL(RTRIM(d.co_sucu_in), '') AS co_sucu_in,
                ISNULL(RTRIM(d.co_sucu_mo), '') AS co_sucu_mo
            FROM saDocumentoVenta d
            LEFT JOIN saTipoDocumento t ON d.co_tipo_doc = t.co_tipo_doc
            LEFT JOIN saCliente cli ON d.co_cli = cli.co_cli
            LEFT JOIN saVendedor ven ON d.co_ven = ven.co_ven
            LEFT JOIN saMoneda m ON d.co_mone = m.co_mone
            LEFT JOIN saDocumentoVenta orig ON RTRIM(d.nro_orig) = RTRIM(orig.nro_doc) AND RTRIM(d.doc_orig) = RTRIM(orig.co_tipo_doc)
            LEFT JOIN saFacturaVenta fac ON RTRIM(d.nro_orig) = RTRIM(fac.doc_num)
            WHERE d.co_tipo_doc = 'N/CR'
              AND (? = '' OR d.nro_doc LIKE ? OR d.n_control LIKE ? OR d.campo5 LIKE ? OR cli.cli_des LIKE ? OR d.observa LIKE ? OR d.nro_orig LIKE ?)
            ORDER BY d.fec_emis DESC, d.nro_doc DESC
        """;

        try (Connection conn = DatabaseConfig.getConnection(DB_PRODUCCION);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String pattern = "%" + (filtro != null ? filtro.trim() : "") + "%";
            ps.setInt(1, limit > 0 ? limit : 50);
            ps.setString(2, filtro != null ? filtro.trim() : "");
            ps.setString(3, pattern);
            ps.setString(4, pattern);
            ps.setString(5, pattern);
            ps.setString(6, pattern);
            ps.setString(7, pattern);
            ps.setString(8, pattern);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToModel(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error al buscar notas de crédito: {}", e.getMessage(), e);
        }
        return list;
    }

    // ── Métodos de navegación de documentos ──

    public String obtenerPrimerNumero(String coTipoDoc) {
        String sql = "SELECT TOP 1 RTRIM(nro_doc) FROM saDocumentoVenta WHERE co_tipo_doc = ? ORDER BY nro_doc ASC";
        return querySingleDoc(sql, coTipoDoc);
    }

    public String obtenerUltimoNumero(String coTipoDoc) {
        String sql = "SELECT TOP 1 RTRIM(nro_doc) FROM saDocumentoVenta WHERE co_tipo_doc = ? ORDER BY nro_doc DESC";
        return querySingleDoc(sql, coTipoDoc);
    }

    public String obtenerAnteriorNumero(String currentDoc, String coTipoDoc) {
        String sql = "SELECT TOP 1 RTRIM(nro_doc) FROM saDocumentoVenta WHERE co_tipo_doc = ? AND nro_doc < ? ORDER BY nro_doc DESC";
        try (Connection conn = DatabaseConfig.getConnection(DB_PRODUCCION);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, coTipoDoc);
            ps.setString(2, currentDoc);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (SQLException e) {
            logger.error("Error al obtener doc anterior: {}", e.getMessage(), e);
        }
        return null;
    }

    public String obtenerSiguienteNumero(String currentDoc, String coTipoDoc) {
        String sql = "SELECT TOP 1 RTRIM(nro_doc) FROM saDocumentoVenta WHERE co_tipo_doc = ? AND nro_doc > ? ORDER BY nro_doc ASC";
        try (Connection conn = DatabaseConfig.getConnection(DB_PRODUCCION);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, coTipoDoc);
            ps.setString(2, currentDoc);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (SQLException e) {
            logger.error("Error al obtener doc siguiente: {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * Marca el documento como impreso en la base de datos de Profit Plus.
     */
    public boolean marcarComoImpresa(String nroDoc, String coTipoDoc, String usuario) {
        String sql = """
            UPDATE saDocumentoVenta
            SET impresa = 1,
                co_us_mo = ISNULL(?, co_us_mo),
                fe_us_mo = GETDATE()
            WHERE RTRIM(nro_doc) = ? AND RTRIM(co_tipo_doc) = ?
        """;
        try (Connection conn = DatabaseConfig.getConnection(DB_PRODUCCION);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, usuario);
            ps.setString(2, nroDoc.trim());
            ps.setString(3, coTipoDoc.trim());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error al marcar documento como impreso: {}", e.getMessage(), e);
            return false;
        }
    }

    private String querySingleDoc(String sql, String coTipoDoc) {
        try (Connection conn = DatabaseConfig.getConnection(DB_PRODUCCION);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, coTipoDoc);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (SQLException e) {
            logger.error("Error al consultar doc individual: {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * Mapea un ResultSet de SQL Server al objeto NotaCreditoModel con todos sus cálculos.
     */
    private NotaCreditoModel mapResultSetToModel(ResultSet rs) throws SQLException {
        NotaCreditoModel m = new NotaCreditoModel();

        m.setNroDoc(rs.getString("nro_doc"));
        m.setCoTipoDoc(rs.getString("co_tipo_doc"));
        m.setTipoDocDesc(rs.getString("tipo_doc_desc"));
        m.setTipoMov(rs.getString("tipo_mov"));

        Timestamp tsEmis = rs.getTimestamp("fec_emis");
        m.setFecEmis(tsEmis != null ? tsEmis.toLocalDateTime() : null);

        Date dtVenc = rs.getDate("fec_venc");
        m.setFecVenc(dtVenc != null ? dtVenc.toLocalDate() : null);

        Date dtReg = rs.getDate("fec_reg");
        m.setFecReg(dtReg != null ? dtReg.toLocalDate() : null);

        Date dtCheque = rs.getDate("fec_cheque");
        m.setFecCheque(dtCheque != null ? dtCheque.toLocalDate() : null);

        m.setNControl(rs.getString("n_control"));
        m.setDescripcion(rs.getString("descripcion"));
        m.setImpresa(rs.getBoolean("impresa"));
        m.setAnulado(rs.getBoolean("anulado"));

        // Cliente
        m.setCoCli(rs.getString("co_cli"));
        m.setCliDes(rs.getString("cli_des"));
        m.setRif(rs.getString("rif"));
        m.setDireccion(rs.getString("direccion"));
        m.setTelefonos(rs.getString("telefonos"));
        m.setEmail(rs.getString("email"));

        // Vendedor
        m.setCoVen(rs.getString("co_ven"));
        m.setVenDes(rs.getString("ven_des"));

        // Moneda y Tasa
        m.setCoMone(rs.getString("co_mone"));
        m.setMoneDes(rs.getString("mone_des"));
        double tasa = rs.getDouble("tasa");
        if (tasa <= 0) tasa = 1.0;
        m.setTasa(tasa);

        // Impuesto
        String tipoImp = rs.getString("tipo_imp");
        m.setTipoImp(tipoImp);
        m.setTipoImpDesc("7".equals(tipoImp) ? "Exentos" : ("1".equals(tipoImp) ? "General" : "Alícuota " + tipoImp));

        // Documento Origen / Afectado
        m.setDocOrig(rs.getString("doc_orig"));
        m.setNroOrig(rs.getString("nro_orig"));

        Timestamp tsOrig = rs.getTimestamp("fec_emis_orig");
        if (tsOrig == null) tsOrig = rs.getTimestamp("fac_fec_emis");
        m.setFecEmisOrig(tsOrig != null ? tsOrig.toLocalDateTime() : null);

        String nCtrlOrig = rs.getString("n_control_orig");
        if (nCtrlOrig == null || nCtrlOrig.isBlank()) nCtrlOrig = rs.getString("fac_n_control");
        m.setNControlOrig(nCtrlOrig);

        double subOrig = rs.getDouble("subtotal_orig_bs");
        if (subOrig <= 0) subOrig = rs.getDouble("fac_subtotal_bs");
        m.setSubtotalOrigBs(subOrig);

        double ivaOrig = rs.getDouble("iva_orig_bs");
        if (ivaOrig <= 0) ivaOrig = rs.getDouble("fac_iva_bs");
        m.setIvaOrigBs(ivaOrig);

        double totOrig = rs.getDouble("total_orig_bs");
        if (totOrig <= 0) totOrig = rs.getDouble("fac_total_bs");
        m.setTotalOrigBs(totOrig);

        // Montos en Bolívares (Base)
        double totalBrutoBs = rs.getDouble("total_bruto");
        double montoDescBs = rs.getDouble("monto_desc_glob");
        double montoRecaBs = rs.getDouble("monto_reca");
        double ivaBs = rs.getDouble("monto_imp") + rs.getDouble("monto_imp2") + rs.getDouble("monto_imp3");
        double otrosBs = rs.getDouble("otros1") + rs.getDouble("otros2") + rs.getDouble("otros3") + rs.getDouble("adicional");
        double totalNetoBs = rs.getDouble("total_neto");
        double saldoBs = rs.getDouble("saldo");

        m.setMontoBrutoBs(totalBrutoBs);
        m.setMontoDescBs(montoDescBs);
        m.setMontoRecaBs(montoRecaBs);
        m.setTotalSinImpuestoBs(totalBrutoBs - montoDescBs + montoRecaBs);
        m.setIvaBs(ivaBs);
        m.setOtrosBs(otrosBs);
        m.setMontoNetoBs(totalNetoBs);
        m.setSaldoBs(saldoBs);

        // Determinar Base Imponible vs Monto Exento
        if ("7".equals(tipoImp) || ivaBs == 0.0) {
            m.setMontoExentoBs(totalBrutoBs - montoDescBs + montoRecaBs);
            m.setBaseImponibleBs(0.0);
        } else {
            m.setMontoExentoBs(0.0);
            m.setBaseImponibleBs(totalBrutoBs - montoDescBs + montoRecaBs);
        }

        // Montos en USD (Dividiendo entre tasa)
        m.setMontoBrutoUsd(totalBrutoBs / tasa);
        m.setMontoDescUsd(montoDescBs / tasa);
        m.setMontoRecaUsd(montoRecaBs / tasa);
        m.setTotalSinImpuestoUsd((totalBrutoBs - montoDescBs + montoRecaBs) / tasa);
        m.setIvaUsd(ivaBs / tasa);
        m.setOtrosUsd(otrosBs / tasa);
        m.setMontoNetoUsd(totalNetoBs / tasa);
        m.setSaldoUsd(saldoBs / tasa);
        m.setBaseImponibleUsd(m.getBaseImponibleBs() / tasa);
        m.setMontoExentoUsd(m.getMontoExentoBs() / tasa);

        // Estatus
        if (m.isAnulado()) {
            m.setEstatus("Anulado");
        } else if (Math.abs(saldoBs) < 0.001) {
            m.setEstatus("Cancelado");
        } else if (Math.abs(saldoBs - totalNetoBs) < 0.01) {
            m.setEstatus("Pendiente");
        } else {
            m.setEstatus("Parcial");
        }

        // Banco / Cheque
        m.setNroCheque(rs.getString("nro_che"));
        m.setMovBanco(rs.getString("mov_ban"));
        m.setComprobIva(rs.getString("num_comprobante"));

        // Campos libres y auditoría
        m.setCampo1(rs.getString("campo1"));
        m.setCampo2(rs.getString("campo2"));
        m.setCampo3(rs.getString("campo3"));
        m.setCampo4(rs.getString("campo4"));
        m.setCampo5(rs.getString("campo5"));
        m.setCampo6(rs.getString("campo6"));
        m.setCampo7(rs.getString("campo7"));
        m.setCampo8(rs.getString("campo8"));

        m.setCoUsIn(rs.getString("co_us_in"));
        Timestamp tsUsIn = rs.getTimestamp("fe_us_in");
        m.setFeUsIn(tsUsIn != null ? tsUsIn.toLocalDateTime() : null);

        m.setCoUsMo(rs.getString("co_us_mo"));
        Timestamp tsUsMo = rs.getTimestamp("fe_us_mo");
        m.setFeUsMo(tsUsMo != null ? tsUsMo.toLocalDateTime() : null);

        m.setCoSucuIn(rs.getString("co_sucu_in"));
        m.setCoSucuMo(rs.getString("co_sucu_mo"));

        return m;
    }
}

package com.droai.dao;

import com.droai.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * DAO para operaciones de Tesorería, Cuentas Bancarias, Conceptos de Gasto y validación de referencias contra Profit Plus.
 */
public class TesoreriaDAO {

    public record CuentaBancariaInfo(String codCta, String codBanco, String descBanco, String numCta) {
        @Override
        public String toString() {
            return codCta + " - " + descBanco + " (" + (numCta.length() >= 4 ? numCta.substring(numCta.length() - 4) : numCta) + ")";
        }
    }

    public record ConceptoGastoInfo(String codConcepto, String descripcion) {
        @Override
        public String toString() {
            return codConcepto + " - " + descripcion;
        }
    }

    /**
     * Consulta las cuentas bancarias configuradas en saCuentaBancaria unidas a saBanco.
     */
    public List<CuentaBancariaInfo> listarCuentasBancarias() {
        List<CuentaBancariaInfo> lista = new ArrayList<>();
        String sql = """
            SELECT RTRIM(c.cod_cta) AS cod_cta,
                   RTRIM(c.co_ban) AS co_ban,
                   ISNULL(RTRIM(b.des_ban), RTRIM(c.co_ban)) AS des_ban,
                   RTRIM(c.num_cta) AS num_cta
            FROM saCuentaBancaria c
            LEFT JOIN saBanco b ON c.co_ban = b.co_ban
            WHERE c.inactivo = 0
            ORDER BY c.cod_cta
        """;

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(new CuentaBancariaInfo(
                    rs.getString("cod_cta"),
                    rs.getString("co_ban"),
                    rs.getString("des_ban"),
                    rs.getString("num_cta")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[TesoreriaDAO] Error al consultar cuentas bancarias: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Consulta el catálogo de conceptos de ingreso y egreso (saCuentaIngEgr).
     */
    public List<ConceptoGastoInfo> listarConceptosGasto() {
        List<ConceptoGastoInfo> lista = new ArrayList<>();
        String sql = """
            SELECT RTRIM(co_cta_ingr_egr) AS cod,
                   RTRIM(descrip) AS descrip
            FROM saCuentaIngEgr
            ORDER BY co_cta_ingr_egr
        """;

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(new ConceptoGastoInfo(
                    rs.getString("cod"),
                    rs.getString("descrip")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[TesoreriaDAO] Error al consultar conceptos de gasto: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Verifica qué referencias bancarias ya existen registradas en saMovimientoBanco o en saOrdenPago.
     * Retorna un mapa: Referencia -> "Existe en Movimiento de Banco (mov_num)" u "Existe en Orden de Pago (ord_num)"
     */
    public Map<String, String> verificarReferenciasExistentes(Set<String> referencias) {
        Map<String, String> encontrados = new HashMap<>();
        if (referencias == null || referencias.isEmpty()) {
            return encontrados;
        }

        List<String> listaRef = new ArrayList<>(referencias);
        // Procesar en lotes de 100 referencias para no sobrecargar el IN
        int batchSize = 100;
        for (int i = 0; i < listaRef.size(); i += batchSize) {
            List<String> subList = listaRef.subList(i, Math.min(i + batchSize, listaRef.size()));
            String inPlaceholders = String.join(",", Collections.nCopies(subList.size(), "?"));

            // 1. Chequear saMovimientoBanco
            String sqlMov = "SELECT RTRIM(doc_num) AS doc, RTRIM(mov_num) AS mov FROM saMovimientoBanco WHERE doc_num IN (" + inPlaceholders + ") AND anulado = 0";
            try (Connection conn = DatabaseConfig.getDataSource().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sqlMov)) {
                for (int j = 0; j < subList.size(); j++) {
                    ps.setString(j + 1, subList.get(j));
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        encontrados.put(rs.getString("doc"), "Movimiento de Banco: " + rs.getString("mov"));
                    }
                }
            } catch (SQLException e) {
                System.err.println("[TesoreriaDAO] Error verificando referencias en saMovimientoBanco: " + e.getMessage());
            }

            // 2. Chequear saOrdenPago
            String sqlOrd = "SELECT RTRIM(doc_num) AS doc, RTRIM(ord_num) AS ord FROM saOrdenPago WHERE doc_num IN (" + inPlaceholders + ") AND anulado = 0";
            try (Connection conn = DatabaseConfig.getDataSource().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sqlOrd)) {
                for (int j = 0; j < subList.size(); j++) {
                    ps.setString(j + 1, subList.get(j));
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String doc = rs.getString("doc");
                        if (!encontrados.containsKey(doc)) {
                            encontrados.put(doc, "Orden de Pago: " + rs.getString("ord"));
                        }
                    }
                }
            } catch (SQLException e) {
                System.err.println("[TesoreriaDAO] Error verificando referencias en saOrdenPago: " + e.getMessage());
            }
        }

        return encontrados;
    }

    public record TransaccionEjecutivaInfo(
            String idDoc,
            String tipoOrigen,      // "Movimiento de Banco" o "Orden de Pago"
            String codCta,
            String bancoDesc,
            java.time.LocalDate fecha,
            String referencia,
            String beneficiario,
            String codConcepto,
            String conceptoDesc,
            double montoBs,
            double tasa,
            double montoUSD
    ) {}

    /**
     * Consulta la tasa de cambio vigente USD en Profit (saTasa o fallback saMoneda).
     */
    public double obtenerTasaUSD() {
        String sqlTasa = """
                SELECT TOP 1 ISNULL(tasa_v, tasa_c) AS tasa
                FROM saTasa
                WHERE co_mone IN ('USD', 'US$')
                ORDER BY fecha DESC, fe_us_mo DESC
                """;
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlTasa);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                double t = rs.getDouble("tasa");
                if (t > 0) return t;
            }
        } catch (SQLException e) {
            System.err.println("[TesoreriaDAO] Error consultando saTasa: " + e.getMessage());
        }

        String sqlMoneda = """
                SELECT TOP 1 ISNULL(cambio, 1) AS tasa
                FROM saMoneda
                WHERE co_mone IN ('USD', 'US$') OR mone_des LIKE '%DOLAR%'
                ORDER BY CASE WHEN co_mone = 'USD' THEN 1 ELSE 2 END
                """;
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlMoneda);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                double t = rs.getDouble("tasa");
                if (t > 0) return t;
            }
        } catch (SQLException e) {
            System.err.println("[TesoreriaDAO] Error consultando saMoneda: " + e.getMessage());
        }
        return 1.0;
    }

    /**
     * Verifica en tiempo real si una referencia ya existe en saMovimientoBanco o en saOrdenPago.
     */
    public Optional<String> verificarReferenciaIndividual(String codCta, String referencia) {
        if (referencia == null || referencia.trim().isEmpty()) return Optional.empty();
        String ref = referencia.trim();

        // 1. Chequear saMovimientoBanco
        String sqlMov = "SELECT TOP 1 RTRIM(mov_num) AS mov, fecha, monto_h FROM saMovimientoBanco WHERE doc_num = ? AND anulado = 0";
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlMov)) {
            ps.setString(1, ref);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of("Movimiento de Banco N° " + rs.getString("mov") + " (Monto: Bs " + String.format("%,.2f", rs.getDouble("monto_h")) + ", Fecha: " + rs.getDate("fecha") + ")");
                }
            }
        } catch (SQLException e) {
            System.err.println("[TesoreriaDAO] Error verificando referencia en saMovimientoBanco: " + e.getMessage());
        }

        // 2. Chequear saOrdenPago
        String sqlOrd = "SELECT TOP 1 RTRIM(ord_num) AS ord, fecha FROM saOrdenPago WHERE doc_num = ? AND anulado = 0";
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlOrd)) {
            ps.setString(1, ref);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of("Orden de Pago N° " + rs.getString("ord") + " (Fecha: " + rs.getDate("fecha") + ")");
                }
            }
        } catch (SQLException e) {
            System.err.println("[TesoreriaDAO] Error verificando referencia en saOrdenPago: " + e.getMessage());
        }

        return Optional.empty();
    }

    /**
     * Consulta consolidada de egresos y pagos desde Profit Plus para el Reporte Ejecutivo y Analítica.
     */
    public List<TransaccionEjecutivaInfo> consultarReporteEjecutivo(java.time.LocalDate desde, java.time.LocalDate hasta, String codCtaFiltro, String tipoFiltro) {
        List<TransaccionEjecutivaInfo> transacciones = new ArrayList<>();
        java.sql.Date dIni = java.sql.Date.valueOf(desde);
        java.sql.Date dFin = java.sql.Date.valueOf(hasta);

        boolean incluirMB = tipoFiltro == null || tipoFiltro.equalsIgnoreCase("TODOS") || tipoFiltro.toUpperCase().contains("MOVIMIENTO");
        boolean incluirOP = tipoFiltro == null || tipoFiltro.equalsIgnoreCase("TODOS") || tipoFiltro.toUpperCase().contains("ORDEN");
        boolean filtrarCta = codCtaFiltro != null && !codCtaFiltro.isBlank() && !codCtaFiltro.equalsIgnoreCase("TODAS");

        // 1. Movimientos de Banco (Egresos: monto_h > 0)
        if (incluirMB) {
            StringBuilder sbMov = new StringBuilder("""
                SELECT RTRIM(m.mov_num) AS id_doc,
                       RTRIM(m.cod_cta) AS cod_cta,
                       ISNULL(RTRIM(b.des_ban), RTRIM(m.cod_cta)) AS des_ban,
                       m.fecha,
                       ISNULL(RTRIM(m.doc_num), '') AS referencia,
                       ISNULL(RTRIM(m.descrip), '') AS beneficiario,
                       ISNULL(RTRIM(m.co_cta_ingr_egr), '000018') AS cod_concepto,
                       ISNULL(RTRIM(c.descrip), 'GASTOS VARIOS') AS desc_concepto,
                       m.monto_h AS monto_bs,
                       ISNULL(m.tasa, 1.0) AS tasa
                FROM saMovimientoBanco m
                LEFT JOIN saCuentaBancaria cb ON m.cod_cta = cb.cod_cta
                LEFT JOIN saBanco b ON cb.co_ban = b.co_ban
                LEFT JOIN saCuentaIngEgr c ON m.co_cta_ingr_egr = c.co_cta_ingr_egr
                WHERE m.anulado = 0 AND m.monto_h > 0
                  AND m.fecha >= ? AND m.fecha <= ?
            """);
            if (filtrarCta) {
                sbMov.append(" AND m.cod_cta = ?");
            }
            sbMov.append(" ORDER BY m.fecha DESC, m.mov_num DESC");

            try (Connection conn = DatabaseConfig.getDataSource().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sbMov.toString())) {
                ps.setDate(1, dIni);
                ps.setDate(2, dFin);
                if (filtrarCta) {
                    ps.setString(3, codCtaFiltro.trim());
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        double bs = rs.getDouble("monto_bs");
                        double t = rs.getDouble("tasa");
                        if (t <= 0) t = 1.0;
                        double usd = bs / t;
                        transacciones.add(new TransaccionEjecutivaInfo(
                                rs.getString("id_doc"),
                                "Movimiento de Banco",
                                rs.getString("cod_cta"),
                                rs.getString("des_ban"),
                                rs.getDate("fecha").toLocalDate(),
                                rs.getString("referencia"),
                                rs.getString("beneficiario"),
                                rs.getString("cod_concepto"),
                                rs.getString("desc_concepto"),
                                bs,
                                t,
                                usd
                        ));
                    }
                }
            } catch (SQLException e) {
                System.err.println("[TesoreriaDAO] Error consultando saMovimientoBanco para reporte: " + e.getMessage());
            }
        }

        // 2. Órdenes de Pago (Egresos a Proveedores)
        if (incluirOP) {
            StringBuilder sbOrd = new StringBuilder("""
                SELECT RTRIM(o.ord_num) AS id_doc,
                       ISNULL(RTRIM(o.cod_cta), '') AS cod_cta,
                       ISNULL(RTRIM(b.des_ban), 'PAGO A PROVEEDOR') AS des_ban,
                       o.fecha,
                       ISNULL(RTRIM(o.doc_num), '') AS referencia,
                       ISNULL(RTRIM(o.descrip), '') AS beneficiario,
                       ISNULL(RTRIM(r.co_cta_ingr_egr), '000018') AS cod_concepto,
                       ISNULL(RTRIM(c.descrip), 'ORDEN DE PAGO') AS desc_concepto,
                       ISNULL(r.tot_monto, 0.0) AS monto_bs,
                       ISNULL(o.tasa, 1.0) AS tasa
                FROM saOrdenPago o
                LEFT JOIN saCuentaBancaria cb ON o.cod_cta = cb.cod_cta
                LEFT JOIN saBanco b ON cb.co_ban = b.co_ban
                LEFT JOIN (
                    SELECT ord_num, MIN(co_cta_ingr_egr) AS co_cta_ingr_egr, SUM(monto_h) AS tot_monto
                    FROM saOrdenPagoReng
                    GROUP BY ord_num
                ) r ON o.ord_num = r.ord_num
                LEFT JOIN saCuentaIngEgr c ON r.co_cta_ingr_egr = c.co_cta_ingr_egr
                WHERE o.anulado = 0
                  AND o.fecha >= ? AND o.fecha <= ?
            """);
            if (filtrarCta) {
                sbOrd.append(" AND o.cod_cta = ?");
            }
            sbOrd.append(" ORDER BY o.fecha DESC, o.ord_num DESC");

            try (Connection conn = DatabaseConfig.getDataSource().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sbOrd.toString())) {
                ps.setDate(1, dIni);
                ps.setDate(2, dFin);
                if (filtrarCta) {
                    ps.setString(3, codCtaFiltro.trim());
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        double bs = rs.getDouble("monto_bs");
                        double t = rs.getDouble("tasa");
                        if (t <= 0) t = 1.0;
                        double usd = bs / t;
                        transacciones.add(new TransaccionEjecutivaInfo(
                                rs.getString("id_doc"),
                                "Orden de Pago",
                                rs.getString("cod_cta"),
                                rs.getString("des_ban"),
                                rs.getDate("fecha").toLocalDate(),
                                rs.getString("referencia"),
                                rs.getString("beneficiario"),
                                rs.getString("cod_concepto"),
                                rs.getString("desc_concepto"),
                                bs,
                                t,
                                usd
                        ));
                    }
                }
            } catch (SQLException e) {
                System.err.println("[TesoreriaDAO] Error consultando saOrdenPago para reporte: " + e.getMessage());
            }
        }

        // Ordenar globalmente por fecha descendente
        transacciones.sort((a, b) -> b.fecha().compareTo(a.fecha()));
        return transacciones;
    }
}

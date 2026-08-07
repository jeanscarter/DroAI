package com.droai;

import java.sql.*;

public class SchemaChecker {
    public static void main(String[] args) {
        String url = "jdbc:sqlserver://srvdb0101:1433;databaseName=DROA2_A;encrypt=false;trustServerCertificate=true";
        String user = "profit";
        String pass = "profit";
        
        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            System.out.println("Connected to DROA2_A");
            
            // 1. Check: are saCobro.cob_num and saCobroDocReng.cob_num same length?
            System.out.println("\n--- Check key lengths ---");
            String sqlLen = """
                SELECT TOP 3
                    c.cob_num AS c_cob, LEN(c.cob_num) AS c_len,
                    r.cob_num AS r_cob, LEN(r.cob_num) AS r_len,
                    r.nro_doc AS r_doc, LEN(r.nro_doc) AS r_doc_len
                FROM saCobro c
                JOIN saCobroDocReng r ON c.cob_num = r.cob_num
                WHERE c.fecha BETWEEN '2026-06-29' AND '2026-07-01'
                """;
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlLen)) {
                while (rs.next()) {
                    System.out.printf("  c.cob='%s'(%d) r.cob='%s'(%d) r.doc='%s'(%d)%n",
                        rs.getString("c_cob"), rs.getInt("c_len"),
                        rs.getString("r_cob"), rs.getInt("r_len"),
                        rs.getString("r_doc"), rs.getInt("r_doc_len"));
                }
            }
            
            // 2. Check: do nro_doc in saCobroDocReng match nro_doc in saDocumentoVenta?
            System.out.println("\n--- Check doc key match ---");
            String sqlDocMatch = """
                SELECT TOP 3
                    r.nro_doc AS r_doc, LEN(r.nro_doc) AS r_len, r.co_tipo_doc AS r_tipo,
                    d.nro_doc AS d_doc, LEN(d.nro_doc) AS d_len, d.co_tipo_doc AS d_tipo
                FROM saCobroDocReng r
                JOIN saDocumentoVenta d ON RTRIM(r.nro_doc) = RTRIM(d.nro_doc) AND RTRIM(r.co_tipo_doc) = RTRIM(d.co_tipo_doc)
                WHERE RTRIM(r.cob_num) = '0000024372'
                """;
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlDocMatch)) {
                while (rs.next()) {
                    System.out.printf("  r.doc='%s'(%d) r.tipo='%s' -> d.doc='%s'(%d) d.tipo='%s'%n",
                        rs.getString("r_doc"), rs.getInt("r_len"), rs.getString("r_tipo").trim(),
                        rs.getString("d_doc"), rs.getInt("d_len"), rs.getString("d_tipo").trim());
                }
            }
            
            // 3. Full working query for cobro 24372 with RTRIM joins
            System.out.println("\n--- FULL QUERY: cobro 24372 with RTRIM joins ---");
            String sqlFull = """
                SELECT 
                    RTRIM(r.co_tipo_doc) AS tipo_doc,
                    CAST(CAST(RTRIM(r.nro_doc) AS int) AS varchar) AS numero_doc,
                    RTRIM(d.n_control) AS clase,
                    CONVERT(varchar, d.fec_emis, 103) AS fec_emis,
                    CONVERT(varchar, d.fec_venc, 103) AS fec_venc,
                    CONVERT(varchar, c.fecha, 103) AS fec_cobro,
                    CAST(CAST(RTRIM(c.cob_num) AS int) AS varchar) AS num_cobro,
                    DATEDIFF(day, d.fec_venc, c.fecha) AS dias_calle,
                    RTRIM(cli.rif) AS cod_cli,
                    RTRIM(cli.cli_des) AS nom_cli,
                    d.total_bruto AS monto_doc,
                    ISNULL(r.dpcobro_porc_desc, 0) AS porc_desc,
                    r.mont_cob AS monto_cobrado,
                    d.total_neto AS base_comision,
                    RTRIM(v.ven_des) AS vendedor,
                    RTRIM(v.co_ven) AS co_ven
                FROM saCobro c
                JOIN saCobroDocReng r ON c.cob_num = r.cob_num
                LEFT JOIN saDocumentoVenta d ON RTRIM(r.nro_doc) = RTRIM(d.nro_doc) AND RTRIM(r.co_tipo_doc) = RTRIM(d.co_tipo_doc)
                LEFT JOIN saCliente cli ON c.co_cli = cli.co_cli
                LEFT JOIN saVendedor v ON c.co_ven = v.co_ven
                WHERE RTRIM(c.cob_num) = '0000024372'
                ORDER BY r.nro_doc
                """;
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlFull)) {
                while (rs.next()) {
                    System.out.printf("  %s | %s | %s | %s | %s | %s | %s | %d | %s | %s | %.2f | %.2f | %.2f | %.2f | [%s] %s%n",
                        rs.getString("tipo_doc"), rs.getString("numero_doc"), 
                        rs.getString("clase"),
                        rs.getString("fec_emis"), rs.getString("fec_venc"),
                        rs.getString("fec_cobro"), rs.getString("num_cobro"),
                        rs.getInt("dias_calle"),
                        rs.getString("cod_cli"), rs.getString("nom_cli"),
                        rs.getDouble("monto_doc"), rs.getDouble("porc_desc"), rs.getDouble("monto_cobrado"),
                        rs.getDouble("base_comision"),
                        rs.getString("co_ven"), rs.getString("vendedor"));
                }
            }

            // 4. Broader range query matching Excel - first 10 rows
            System.out.println("\n--- FULL RANGE: 29/06/2026 to 15/07/2026 vendedor AB (first 10) ---");
            String sqlRange = """
                SELECT TOP 10
                    RTRIM(r.co_tipo_doc) AS tipo_doc,
                    CAST(CAST(RTRIM(r.nro_doc) AS int) AS varchar) AS numero_doc,
                    CONVERT(varchar, d.fec_emis, 103) AS fec_emis,
                    CONVERT(varchar, d.fec_venc, 103) AS fec_venc,
                    CONVERT(varchar, c.fecha, 103) AS fec_cobro,
                    CAST(CAST(RTRIM(c.cob_num) AS int) AS varchar) AS num_cobro,
                    DATEDIFF(day, d.fec_venc, c.fecha) AS dias_calle,
                    RTRIM(cli.rif) AS cod_cli,
                    RTRIM(cli.cli_des) AS nom_cli,
                    d.total_bruto AS monto_doc,
                    r.mont_cob AS monto_cobrado,
                    RTRIM(v.ven_des) AS vendedor
                FROM saCobro c
                JOIN saCobroDocReng r ON c.cob_num = r.cob_num
                LEFT JOIN saDocumentoVenta d ON RTRIM(r.nro_doc) = RTRIM(d.nro_doc) AND RTRIM(r.co_tipo_doc) = RTRIM(d.co_tipo_doc)
                LEFT JOIN saCliente cli ON c.co_cli = cli.co_cli
                LEFT JOIN saVendedor v ON c.co_ven = v.co_ven
                WHERE c.fecha BETWEEN '2026-06-29' AND '2026-07-15'
                  AND ISNULL(c.anulado, 0) = 0
                  AND RTRIM(c.co_ven) = 'AB'
                ORDER BY c.fecha, c.cob_num, r.nro_doc
                """;
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlRange)) {
                while (rs.next()) {
                    System.out.printf("  %s | %s | %s | %s | %s | %s | %d | %s | %s | %.2f | %.2f | %s%n",
                        rs.getString("tipo_doc"), rs.getString("numero_doc"), 
                        rs.getString("fec_emis"), rs.getString("fec_venc"),
                        rs.getString("fec_cobro"), rs.getString("num_cobro"),
                        rs.getInt("dias_calle"),
                        rs.getString("cod_cli"), rs.getString("nom_cli"),
                        rs.getDouble("monto_doc"), rs.getDouble("monto_cobrado"),
                        rs.getString("vendedor"));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

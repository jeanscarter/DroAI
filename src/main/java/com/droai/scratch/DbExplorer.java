package com.droai.scratch;

import com.droai.config.DatabaseConfig;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class DbExplorer {
    public static void main(String[] args) {
        String testQuery = "SELECT TOP 1 " +
                           "f.doc_num, f.fec_emis, f.tasa, " +
                           "c.co_cli, c.cli_des, " +
                           "v.co_ven, v.ven_des, " +
                           "a.co_art, a.art_des, " +
                           "r.total_art, r.prec_vta, r.porc_desc, r.monto_imp, r.cost_vta, " +
                           "l.co_lin, l.lin_des " +
                           "FROM saFacturaVenta f " +
                           "JOIN saFacturaVentaReng r ON f.doc_num = r.doc_num " +
                           "JOIN saArticulo a ON r.co_art = a.co_art " +
                           "LEFT JOIN saCliente c ON f.co_cli = c.co_cli " +
                           "LEFT JOIN saVendedor v ON f.co_ven = v.co_ven " +
                           "LEFT JOIN saLineaArticulo l ON a.co_lin = l.co_lin";

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(testQuery)) {
            System.out.println("SQL Syntax OK! Result exists: " + rs.next());
        } catch (Exception e) {
            System.err.println("SQL ERROR: " + e.getMessage());
        }
        System.exit(0);
    }
}

package com.droai.dao;

import com.droai.config.DatabaseConfig;
import com.droai.model.ClienteMaestroRow;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos para el Maestro General de Clientes en Profit Plus.
 * Consulta en tiempo real las tablas saCliente, saTipoCliente, saZona,
 * saSegmento, saVendedor y saCondicionPago.
 */
public class ClienteDAO {

    private static final String SQL_MAESTRO_CLIENTES = """
            SELECT
                c.co_cli AS codigo,
                c.rif AS rif,
                c.cli_des AS nombre,
                ISNULL(c.nit, '') AS nit,
                CONVERT(varchar, c.fecha_reg, 23) AS fechaRegistro,
                CASE WHEN c.contrib = 1 THEN 'SI' ELSE 'NO' END AS contribuyente,
                ISNULL(tc.des_tipo, ISNULL(c.tip_cli, 'SIN TIPO')) AS tipoCliente,
                ISNULL(c.co_pais, 'Venezuela') AS pais,
                ISNULL(z.zon_des, ISNULL(c.co_zon, 'SIN ZONA')) AS zona,
                ISNULL(c.ciudad, '') AS ciudad,
                ISNULL(sg.seg_des, ISNULL(c.co_seg, 'SIN GRUPO')) AS segmento,
                CASE WHEN c.inactivo = 1 THEN 'SI' ELSE 'NO' END AS inactivo,
                ISNULL(v.ven_des, ISNULL(c.co_ven, 'OFICINA')) AS vendedor,
                ISNULL(c.zip, '') AS codPostal,
                ISNULL(cp.cond_des, ISNULL(c.cond_pag, 'CONTADO')) AS condPago,
                ISNULL(c.email, '') AS email,
                CASE WHEN c.sincredito = 1 THEN 'NO' ELSE 'SI' END AS credito,
                ISNULL(c.telefonos, '') AS telefono,
                ISNULL(c.mont_cre, 0) AS limiteCredito,
                ISNULL(c.campo1, '') AS ruta,
                CASE
                    WHEN c.tipo_per = '1' OR c.tipo_per = 'N' THEN 'Natural Residente'
                    WHEN c.tipo_per = '2' OR c.tipo_per = 'J' THEN 'Juridica Domiciliada'
                    ELSE ISNULL(c.tipo_per, '')
                END AS tipoPersona,
                ISNULL(c.respons, '') AS contacto,
                LTRIM(RTRIM(ISNULL(c.direc1, '') + ' ' + ISNULL(c.direc2, ''))) AS direccion
            FROM saCliente c
            LEFT JOIN saTipoCliente tc ON c.tip_cli = tc.tip_cli
            LEFT JOIN saZona z ON c.co_zon = z.co_zon
            LEFT JOIN saSegmento sg ON c.co_seg = sg.co_seg
            LEFT JOIN saVendedor v ON c.co_ven = v.co_ven
            LEFT JOIN saCondicionPago cp ON c.cond_pag = cp.co_cond
            ORDER BY c.cli_des
            """;

    /**
     * Obtiene el listado completo y actualizado de todos los clientes en Profit Plus.
     */
    public List<ClienteMaestroRow> obtenerMaestroClientes() throws SQLException {
        List<ClienteMaestroRow> list = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SQL_MAESTRO_CLIENTES)) {

            while (rs.next()) {
                ClienteMaestroRow row = new ClienteMaestroRow();
                row.setCodigo(rs.getString("codigo"));
                row.setRif(rs.getString("rif"));
                row.setNombre(rs.getString("nombre"));
                row.setNit(rs.getString("nit"));
                row.setFechaRegistro(rs.getString("fechaRegistro"));
                row.setContribuyente(rs.getString("contribuyente"));
                row.setTipoCliente(rs.getString("tipoCliente"));
                row.setPais(rs.getString("pais"));
                row.setZona(rs.getString("zona"));
                row.setCiudad(rs.getString("ciudad"));
                row.setSegmento(rs.getString("segmento"));
                row.setInactivo(rs.getString("inactivo"));
                row.setVendedor(rs.getString("vendedor"));
                row.setCodPostal(rs.getString("codPostal"));
                row.setCondPago(rs.getString("condPago"));
                row.setEmail(rs.getString("email"));
                row.setCredito(rs.getString("credito"));
                row.setTelefono(rs.getString("telefono"));
                row.setLimiteCredito(rs.getDouble("limiteCredito"));
                row.setRuta(rs.getString("ruta"));
                row.setTipoPersona(rs.getString("tipoPersona"));
                row.setContacto(rs.getString("contacto"));
                row.setDireccion(rs.getString("direccion"));
                list.add(row);
            }
        }
        return list;
    }
}

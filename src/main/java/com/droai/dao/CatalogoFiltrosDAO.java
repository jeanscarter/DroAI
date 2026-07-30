package com.droai.dao;

import com.droai.config.DatabaseConfig;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para consultar catálogos auxiliares de filtro (Líneas, SubLíneas, Proveedores, Almacenes).
 */
public class CatalogoFiltrosDAO {

    public record OptionItem(String code, String name) {
        @Override
        public String toString() {
            if (code.isEmpty()) return name;
            if (name.isEmpty() || name.equals(code)) return code;
            return code + " - " + name;
        }
    }

    /**
     * Obtiene la lista de líneas (Grupos) desde saLineaArticulo.
     */
    public List<OptionItem> obtenerLineas() {
        List<OptionItem> list = new ArrayList<>();
        String sql = "SELECT RTRIM(co_lin) AS co_lin, RTRIM(lin_des) AS lin_des FROM saLineaArticulo ORDER BY co_lin";
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new OptionItem(rs.getString("co_lin"), rs.getString("lin_des")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Obtiene la lista de sublíneas (SubGrupos) desde saSubLinea, opcionalmente filtradas por línea.
     */
    public List<OptionItem> obtenerSubLineas(String coLin) {
        List<OptionItem> list = new ArrayList<>();
        boolean filterByLine = coLin != null && !coLin.isBlank();
        String sql = "SELECT RTRIM(co_subl) AS co_subl, RTRIM(subl_des) AS subl_des FROM saSubLinea "
                   + (filterByLine ? "WHERE co_lin = ? " : "")
                   + "ORDER BY co_subl";
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (filterByLine) {
                ps.setString(1, coLin.trim());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new OptionItem(rs.getString("co_subl"), rs.getString("subl_des")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Obtiene la lista de proveedores desde saProveedor.
     */
    public List<OptionItem> obtenerProveedores() {
        List<OptionItem> list = new ArrayList<>();
        String sql = "SELECT RTRIM(co_prov) AS co_prov, RTRIM(prov_des) AS prov_des FROM saProveedor ORDER BY co_prov";
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new OptionItem(rs.getString("co_prov"), rs.getString("prov_des")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Obtiene la lista de almacenes desde saAlmacen.
     */
    public List<OptionItem> obtenerAlmacenes() {
        List<OptionItem> list = new ArrayList<>();
        String sql = "SELECT RTRIM(co_alma) AS co_alma, RTRIM(des_alma) AS des_alma FROM saAlmacen ORDER BY co_alma";
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new OptionItem(rs.getString("co_alma"), rs.getString("des_alma")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}

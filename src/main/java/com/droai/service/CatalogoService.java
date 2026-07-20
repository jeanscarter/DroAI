package com.droai.service;

import com.droai.dao.ArticuloDAO;
import com.droai.dao.DescuentoProductoDAO;
import com.droai.dao.DescuentoVolumenDAO;
import com.droai.model.ArticuloRow;
import com.droai.model.DescuentoProductoRow;
import com.droai.model.DescuentoVolumenRow;

import java.sql.SQLException;
import java.util.List;

/**
 * Servicio para el Catálogo de Productos.
 * Obtiene los artículos del DAO y calcula campos derivados (utilPct, precioCiva).
 */
public class CatalogoService {

    private final ArticuloDAO articuloDAO;
    private final DescuentoVolumenDAO descuentoVolumenDAO;
    private final DescuentoProductoDAO descuentoProductoDAO;

    public CatalogoService() {
        this.articuloDAO = new ArticuloDAO();
        this.descuentoVolumenDAO = new DescuentoVolumenDAO();
        this.descuentoProductoDAO = new DescuentoProductoDAO();
    }

    public CatalogoService(ArticuloDAO articuloDAO, DescuentoVolumenDAO descuentoVolumenDAO, DescuentoProductoDAO descuentoProductoDAO) {
        this.articuloDAO = articuloDAO;
        this.descuentoVolumenDAO = descuentoVolumenDAO;
        this.descuentoProductoDAO = descuentoProductoDAO;
    }

    public List<DescuentoVolumenRow> obtenerDescuentosVolumen() throws SQLException {
        return descuentoVolumenDAO.fetchDescuentosVolumen();
    }

    public void actualizarDescuentosVolumen(List<String> codigos, double nuevoPorcentaje) throws SQLException {
        descuentoVolumenDAO.updateDescuentosVolumen(codigos, nuevoPorcentaje, null, null);
    }

    public void actualizarDescuentosVolumen(List<String> codigos, double nuevoPorcentaje, java.sql.Date fechaIni, java.sql.Date fechaFin) throws SQLException {
        descuentoVolumenDAO.updateDescuentosVolumen(codigos, nuevoPorcentaje, fechaIni, fechaFin);
    }

    public void actualizarDescuentosVolumenMap(java.util.Map<String, Double> dctosMap) throws SQLException {
        descuentoVolumenDAO.updateDescuentosVolumenMap(dctosMap, null, null);
    }

    public void actualizarDescuentosVolumenMap(java.util.Map<String, Double> dctosMap, java.sql.Date fechaIni, java.sql.Date fechaFin) throws SQLException {
        descuentoVolumenDAO.updateDescuentosVolumenMap(dctosMap, fechaIni, fechaFin);
    }

    public void actualizarDescuentosVolumenItems(List<ImportadorService.DescuentoDVImportItem> items, java.sql.Date fechaIni, java.sql.Date fechaFin) throws SQLException {
        descuentoVolumenDAO.updateDescuentosVolumenItems(items, fechaIni, fechaFin);
    }

    public List<DescuentoProductoRow> obtenerDescuentosProducto() throws SQLException {
        return descuentoProductoDAO.fetchDescuentosProducto();
    }

    public void actualizarDescuentosProducto(List<String> codigos, double dctoDA, double dctoDV) throws SQLException {
        descuentoProductoDAO.updateDescuentosProducto(codigos, dctoDA, dctoDV);
    }

    public void actualizarDescuentosProductoDA(List<String> codigos, double dctoDA) throws SQLException {
        descuentoProductoDAO.updateDescuentosProductoDA(codigos, dctoDA);
    }

    public void actualizarDescuentosProductoDAMap(java.util.Map<String, Double> dctosMap) throws SQLException {
        descuentoProductoDAO.updateDescuentosProductoDAMap(dctosMap);
    }

    public List<ArticuloRow> obtenerCatalogo() throws SQLException {
        List<ArticuloRow> catalogo = articuloDAO.fetchCatalogo();
        calcularCamposDerivados(catalogo);
        return catalogo;
    }

    private void calcularCamposDerivados(List<ArticuloRow> filas) {
        for (ArticuloRow a : filas) {
            // Base de costo: usar costoActual si está disponible, sino costoOm
            double costo = (a.getCostoActual() > 0) ? a.getCostoActual() : a.getCostoOm();

            // Utilidad Financiera (margen sobre precio de venta):
            // ((PrecioS/IVA - Costo) / PrecioS/IVA) * 100
            if (a.getPrecio1() > 0) {
                a.setUtilPct(((a.getPrecio1() - costo) / a.getPrecio1()) * 100.0);
            } else {
                a.setUtilPct(0);
            }

            // Precio C/IVA = precio1 * (1 + ivaPct / 100)
            a.setPrecioCiva(a.getPrecio1() * (1.0 + a.getIvaPct() / 100.0));
        }
    }
}

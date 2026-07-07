package com.droai.service;

import com.droai.dao.ArticuloDAO;
import com.droai.dao.DescuentoVolumenDAO;
import com.droai.model.ArticuloRow;
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

    public CatalogoService() {
        this.articuloDAO = new ArticuloDAO();
        this.descuentoVolumenDAO = new DescuentoVolumenDAO();
    }

    public CatalogoService(ArticuloDAO articuloDAO, DescuentoVolumenDAO descuentoVolumenDAO) {
        this.articuloDAO = articuloDAO;
        this.descuentoVolumenDAO = descuentoVolumenDAO;
    }

    public List<DescuentoVolumenRow> obtenerDescuentosVolumen() throws SQLException {
        return descuentoVolumenDAO.fetchDescuentosVolumen();
    }

    public void actualizarDescuentosVolumen(List<String> codigos, double nuevoPorcentaje) throws SQLException {
        descuentoVolumenDAO.updateDescuentosVolumen(codigos, nuevoPorcentaje);
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

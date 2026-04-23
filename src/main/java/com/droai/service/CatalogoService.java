package com.droai.service;

import com.droai.dao.ArticuloDAO;
import com.droai.model.ArticuloRow;

import java.sql.SQLException;
import java.util.List;

/**
 * Servicio para el Catálogo de Productos.
 * Obtiene los artículos del DAO y calcula campos derivados (utilPct, precioCiva).
 */
public class CatalogoService {

    private final ArticuloDAO articuloDAO;

    public CatalogoService() {
        this.articuloDAO = new ArticuloDAO();
    }

    public CatalogoService(ArticuloDAO articuloDAO) {
        this.articuloDAO = articuloDAO;
    }

    public List<ArticuloRow> obtenerCatalogo() throws SQLException {
        List<ArticuloRow> catalogo = articuloDAO.fetchCatalogo();
        calcularCamposDerivados(catalogo);
        return catalogo;
    }

    private void calcularCamposDerivados(List<ArticuloRow> filas) {
        for (ArticuloRow a : filas) {
            // Util % = ((precio1 - costoOm) / costoOm) * 100  (si costoOm > 0)
            if (a.getCostoOm() > 0) {
                a.setUtilPct(((a.getPrecio1() - a.getCostoOm()) / a.getCostoOm()) * 100.0);
            } else {
                a.setUtilPct(0);
            }

            // Precio C/IVA = precio1 * (1 + ivaPct / 100)
            a.setPrecioCiva(a.getPrecio1() * (1.0 + a.getIvaPct() / 100.0));
        }
    }
}

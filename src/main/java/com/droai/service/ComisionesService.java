package com.droai.service;

import com.droai.dao.ComisionesDAO;
import com.droai.model.ComisionRow;

import java.time.LocalDate;
import java.util.List;

/**
 * Servicio para la lógica de negocio de Cálculo de Comisiones.
 */
public class ComisionesService {

    private final ComisionesDAO dao;

    public ComisionesService() {
        this.dao = new ComisionesDAO();
    }

    public List<ComisionesDAO.VendedorOption> obtenerVendedores() {
        return dao.fetchVendedores();
    }

    public List<ComisionRow> consultarComisiones(LocalDate desde, LocalDate hasta, String coVen) {
        return dao.fetchComisiones(desde, hasta, coVen);
    }

    /**
     * Retorna la etiqueta de BD(s) que se usarán para un rango de fechas.
     * Ej: "DROA2_A", "DROA_A", o "DROA2_A + DROA_A"
     */
    public String getDbLabel(LocalDate desde, LocalDate hasta) {
        return ComisionesDAO.getDbLabel(desde, hasta);
    }

    /**
     * Cuenta cuántas facturas NO están cerradas en los resultados.
     */
    public long contarFacturasAbiertas(List<ComisionRow> rows) {
        if (rows == null) return 0;
        return rows.stream().filter(r -> !r.isFacturaCerrada()).count();
    }

    public static class TotalesComisiones {
        private final double totalMontoDoc;
        private final double totalMontoCobrado;
        private final double totalBaseComision;
        private final double totalMontoComision;
        private final int totalRegistros;

        public TotalesComisiones(double totalMontoDoc, double totalMontoCobrado, double totalBaseComision, double totalMontoComision, int totalRegistros) {
            this.totalMontoDoc = totalMontoDoc;
            this.totalMontoCobrado = totalMontoCobrado;
            this.totalBaseComision = totalBaseComision;
            this.totalMontoComision = totalMontoComision;
            this.totalRegistros = totalRegistros;
        }

        public double getTotalMontoDoc() {
            return totalMontoDoc;
        }

        public double getTotalMontoCobrado() {
            return totalMontoCobrado;
        }

        public double getTotalBaseComision() {
            return totalBaseComision;
        }

        public double getTotalMontoComision() {
            return totalMontoComision;
        }

        public int getTotalRegistros() {
            return totalRegistros;
        }
    }

    public TotalesComisiones calcularTotales(List<ComisionRow> rows) {
        double totDoc = 0;
        double totCob = 0;
        double totBase = 0;
        double totCom = 0;

        if (rows != null) {
            for (ComisionRow r : rows) {
                totDoc += r.getMontoDocumento();
                totCob += r.getMontoCobrado();
                totBase += r.getBaseComision();
                totCom += r.getMontoComision();
            }
        }

        return new TotalesComisiones(totDoc, totCob, totBase, totCom, rows != null ? rows.size() : 0);
    }
}

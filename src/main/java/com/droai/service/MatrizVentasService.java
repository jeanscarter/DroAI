package com.droai.service;

import com.droai.dao.MatrizVentasDAO;
import com.droai.model.MatrizVentasRow;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class MatrizVentasService {

    private final MatrizVentasDAO matrizVentasDAO;

    public MatrizVentasService() {
        this.matrizVentasDAO = new MatrizVentasDAO();
    }

    public MatrizVentasService(MatrizVentasDAO matrizVentasDAO) {
        this.matrizVentasDAO = matrizVentasDAO;
    }

    public List<MatrizVentasRow> obtenerMatrizVentas(LocalDate desde, LocalDate hasta) throws SQLException {
        if (desde == null || hasta == null) {
            throw new IllegalArgumentException("Las fechas 'desde' y 'hasta' no pueden ser nulas.");
        }

        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha 'desde' no puede ser posterior a la fecha 'hasta'.");
        }

        List<MatrizVentasRow> matriz = matrizVentasDAO.fetchMatrizVentas(desde, hasta);

        recalcularValoresNegocio(matriz);

        return matriz;
    }

    private void recalcularValoresNegocio(List<MatrizVentasRow> filas) {
        for (MatrizVentasRow fila : filas) {
            double totalRenglon = fila.getPrecio() * fila.getCantidad();
            fila.setTotalRenglon(totalRenglon);

            double renglonDg = totalRenglon * (1 - (fila.getDescPctGlobal() / 100.0));
            fila.setRenglonDg(renglonDg);

            double totRenglonIva = totalRenglon + fila.getMontoIva();
            fila.setTotRenglonIva(totRenglonIva);

            double totalCostoVenta = fila.getCostoVenta() * fila.getCantidad();
            fila.setTotalCostoVenta(totalCostoVenta);

            double montoUtilidad = totalRenglon - totalCostoVenta;
            fila.setMontoUtilidad(montoUtilidad);

            if (totalRenglon != 0) {
                fila.setUtilPct((montoUtilidad / totalRenglon) * 100);
            } else {
                fila.setUtilPct(0);
            }
        }
    }
}
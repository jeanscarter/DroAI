package com.droai.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Resumen consolidado del costo de ventas y margen del período.
 */
public class CostoVentasPeriodoResult {

    private List<CostoVentaRow> items = new ArrayList<>();
    private double totalVentaUSD = 0.0;
    private double totalVentaBS = 0.0;
    private double totalCostoUSD = 0.0;
    private double totalCostoBS = 0.0;
    private double totalCostoFirmeUSD = 0.0;
    private double totalCostoFirmeBS = 0.0;
    private double totalCostoConsigUSD = 0.0;
    private double totalCostoConsigBS = 0.0;
    private double utilidadUSD = 0.0;
    private double utilidadBS = 0.0;
    private double margenPct = 0.0;
    private double totalUnidades = 0.0;

    public CostoVentasPeriodoResult() {}

    public List<CostoVentaRow> getItems() { return items; }
    public void setItems(List<CostoVentaRow> items) { this.items = items; }

    public double getTotalVentaUSD() { return totalVentaUSD; }
    public void setTotalVentaUSD(double totalVentaUSD) { this.totalVentaUSD = totalVentaUSD; }

    public double getTotalVentaBS() { return totalVentaBS; }
    public void setTotalVentaBS(double totalVentaBS) { this.totalVentaBS = totalVentaBS; }

    public double getTotalCostoUSD() { return totalCostoUSD; }
    public void setTotalCostoUSD(double totalCostoUSD) { this.totalCostoUSD = totalCostoUSD; }

    public double getTotalCostoBS() { return totalCostoBS; }
    public void setTotalCostoBS(double totalCostoBS) { this.totalCostoBS = totalCostoBS; }

    public double getTotalCostoFirmeUSD() { return totalCostoFirmeUSD; }
    public void setTotalCostoFirmeUSD(double totalCostoFirmeUSD) { this.totalCostoFirmeUSD = totalCostoFirmeUSD; }

    public double getTotalCostoFirmeBS() { return totalCostoFirmeBS; }
    public void setTotalCostoFirmeBS(double totalCostoFirmeBS) { this.totalCostoFirmeBS = totalCostoFirmeBS; }

    public double getTotalCostoConsigUSD() { return totalCostoConsigUSD; }
    public void setTotalCostoConsigUSD(double totalCostoConsigUSD) { this.totalCostoConsigUSD = totalCostoConsigUSD; }

    public double getTotalCostoConsigBS() { return totalCostoConsigBS; }
    public void setTotalCostoConsigBS(double totalCostoConsigBS) { this.totalCostoConsigBS = totalCostoConsigBS; }

    public double getUtilidadUSD() { return utilidadUSD; }
    public void setUtilidadUSD(double utilidadUSD) { this.utilidadUSD = utilidadUSD; }

    public double getUtilidadBS() { return utilidadBS; }
    public void setUtilidadBS(double utilidadBS) { this.utilidadBS = utilidadBS; }

    public double getMargenPct() { return margenPct; }
    public void setMargenPct(double margenPct) { this.margenPct = margenPct; }

    public double getTotalUnidades() { return totalUnidades; }
    public void setTotalUnidades(double totalUnidades) { this.totalUnidades = totalUnidades; }
}

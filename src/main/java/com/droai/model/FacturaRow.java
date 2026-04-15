package com.droai.model;

/**
 * Modelo de una fila de la Matriz de Ventas.
 * Mapea las 30 columnas extraídas de Profit Plus simulando el archivo Excel original.
 */
public class FacturaRow {

    private String numero;
    private String fecha;
    private String ciRif;
    private String nombreRazonSocial;
    private String coVen;
    private String nombreVendedor;
    private double tasa;
    private String codigoArt;
    private String descripcionArt;
    private double cantidad;
    private double precio;
    private double dp;
    private double dct;
    private double da;
    private double dv;
    private double descPct;
    private double totalRenglon;
    private double descPctGlobal;
    private double renglonDg;
    private double montoIva;
    private double totRenglonIva;
    private double costoVenta;
    private double totalCostoVenta;
    private double totCvDp;
    private double montoUtilidad;
    private double utilPct;
    private double costoActual;
    private double stockActual;
    private String codLinea;
    private String linea;

    public FacturaRow() {}

    // Getters
    public String getNumero() { return numero; }
    public String getFecha() { return fecha; }
    public String getCiRif() { return ciRif; }
    public String getNombreRazonSocial() { return nombreRazonSocial; }
    public String getCoVen() { return coVen; }
    public String getNombreVendedor() { return nombreVendedor; }
    public double getTasa() { return tasa; }
    public String getCodigoArt() { return codigoArt; }
    public String getDescripcionArt() { return descripcionArt; }
    public double getCantidad() { return cantidad; }
    public double getPrecio() { return precio; }
    public double getDp() { return dp; }
    public double getDct() { return dct; }
    public double getDa() { return da; }
    public double getDv() { return dv; }
    public double getDescPct() { return descPct; }
    public double getTotalRenglon() { return totalRenglon; }
    public double getDescPctGlobal() { return descPctGlobal; }
    public double getRenglonDg() { return renglonDg; }
    public double getMontoIva() { return montoIva; }
    public double getTotRenglonIva() { return totRenglonIva; }
    public double getCostoVenta() { return costoVenta; }
    public double getTotalCostoVenta() { return totalCostoVenta; }
    public double getTotCvDp() { return totCvDp; }
    public double getMontoUtilidad() { return montoUtilidad; }
    public double getUtilPct() { return utilPct; }
    public double getCostoActual() { return costoActual; }
    public double getStockActual() { return stockActual; }
    public String getCodLinea() { return codLinea; }
    public String getLinea() { return linea; }

    // Setters
    public void setNumero(String numero) { this.numero = numero; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    public void setCiRif(String ciRif) { this.ciRif = ciRif; }
    public void setNombreRazonSocial(String nombreRazonSocial) { this.nombreRazonSocial = nombreRazonSocial; }
    public void setCoVen(String coVen) { this.coVen = coVen; }
    public void setNombreVendedor(String nombreVendedor) { this.nombreVendedor = nombreVendedor; }
    public void setTasa(double tasa) { this.tasa = tasa; }
    public void setCodigoArt(String codigoArt) { this.codigoArt = codigoArt; }
    public void setDescripcionArt(String descripcionArt) { this.descripcionArt = descripcionArt; }
    public void setCantidad(double cantidad) { this.cantidad = cantidad; }
    public void setPrecio(double precio) { this.precio = precio; }
    public void setDp(double dp) { this.dp = dp; }
    public void setDct(double dct) { this.dct = dct; }
    public void setDa(double da) { this.da = da; }
    public void setDv(double dv) { this.dv = dv; }
    public void setDescPct(double descPct) { this.descPct = descPct; }
    public void setTotalRenglon(double totalRenglon) { this.totalRenglon = totalRenglon; }
    public void setDescPctGlobal(double descPctGlobal) { this.descPctGlobal = descPctGlobal; }
    public void setRenglonDg(double renglonDg) { this.renglonDg = renglonDg; }
    public void setMontoIva(double montoIva) { this.montoIva = montoIva; }
    public void setTotRenglonIva(double totRenglonIva) { this.totRenglonIva = totRenglonIva; }
    public void setCostoVenta(double costoVenta) { this.costoVenta = costoVenta; }
    public void setTotalCostoVenta(double totalCostoVenta) { this.totalCostoVenta = totalCostoVenta; }
    public void setTotCvDp(double totCvDp) { this.totCvDp = totCvDp; }
    public void setMontoUtilidad(double montoUtilidad) { this.montoUtilidad = montoUtilidad; }
    public void setUtilPct(double utilPct) { this.utilPct = utilPct; }
    public void setCostoActual(double costoActual) { this.costoActual = costoActual; }
    public void setStockActual(double stockActual) { this.stockActual = stockActual; }
    public void setCodLinea(String codLinea) { this.codLinea = codLinea; }
    public void setLinea(String linea) { this.linea = linea; }
}

package com.droai.model;

/**
 * Representa la existencia de un artículo por Lote y Fecha de Vencimiento.
 */
public class StockLoteRow {
    private String coArt;
    private String coAlma;
    private String desAlma;
    private String numeroLote;
    private String fechaExpiracion;
    private double stockActual;

    public StockLoteRow() {}

    public StockLoteRow(String coArt, String coAlma, String desAlma, String numeroLote, String fechaExpiracion, double stockActual) {
        this.coArt = coArt;
        this.coAlma = coAlma;
        this.desAlma = desAlma;
        this.numeroLote = numeroLote;
        this.fechaExpiracion = fechaExpiracion;
        this.stockActual = stockActual;
    }

    public String getCoArt() {
        return coArt;
    }

    public void setCoArt(String coArt) {
        this.coArt = coArt;
    }

    public String getCoAlma() {
        return coAlma;
    }

    public void setCoAlma(String coAlma) {
        this.coAlma = coAlma;
    }

    public String getDesAlma() {
        return desAlma;
    }

    public void setDesAlma(String desAlma) {
        this.desAlma = desAlma;
    }

    public String getNumeroLote() {
        return numeroLote;
    }

    public void setNumeroLote(String numeroLote) {
        this.numeroLote = numeroLote;
    }

    public String getFechaExpiracion() {
        return fechaExpiracion;
    }

    public void setFechaExpiracion(String fechaExpiracion) {
        this.fechaExpiracion = fechaExpiracion;
    }

    public double getStockActual() {
        return stockActual;
    }

    public void setStockActual(double stockActual) {
        this.stockActual = stockActual;
    }
}

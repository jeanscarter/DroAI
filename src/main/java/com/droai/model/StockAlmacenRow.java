package com.droai.model;

/**
 * Representa la existencia de un artículo en un almacén específico.
 */
public class StockAlmacenRow {
    private String coAlma;
    private String desAlma;
    private double stock;

    public StockAlmacenRow() {}

    public StockAlmacenRow(String coAlma, String desAlma, double stock) {
        this.coAlma = coAlma;
        this.desAlma = desAlma;
        this.stock = stock;
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

    public double getStock() {
        return stock;
    }

    public void setStock(double stock) {
        this.stock = stock;
    }
}

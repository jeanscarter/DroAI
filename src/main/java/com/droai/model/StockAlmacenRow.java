package com.droai.model;

/**
 * Representa la existencia de un artículo en un almacén específico.
 */
public class StockAlmacenRow {
    private String codigoArticulo;
    private String coAlma;
    private String desAlma;
    private String lote;
    private double stock;
    private double stockComprometido;

    public StockAlmacenRow() {}

    public StockAlmacenRow(String coAlma, String desAlma, double stock) {
        this.coAlma = coAlma;
        this.desAlma = desAlma;
        this.stock = stock;
    }

    public String getCodigoArticulo() {
        return codigoArticulo;
    }

    public void setCodigoArticulo(String codigoArticulo) {
        this.codigoArticulo = codigoArticulo;
    }

    public String getCoArt() {
        return codigoArticulo;
    }

    public void setCoArt(String coArt) {
        this.codigoArticulo = coArt;
    }

    public String getCoAlma() {
        return coAlma;
    }

    public void setCoAlma(String coAlma) {
        this.coAlma = coAlma;
    }

    public String getCodigoAlmacen() {
        return coAlma;
    }

    public void setCodigoAlmacen(String codigoAlmacen) {
        this.coAlma = codigoAlmacen;
    }

    public String getDesAlma() {
        return desAlma;
    }

    public void setDesAlma(String desAlma) {
        this.desAlma = desAlma;
    }

    public String getNombreAlmacen() {
        return desAlma;
    }

    public void setNombreAlmacen(String nombreAlmacen) {
        this.desAlma = nombreAlmacen;
    }

    public String getLote() {
        return lote;
    }

    public void setLote(String lote) {
        this.lote = lote;
    }

    public double getStock() {
        return stock;
    }

    public void setStock(double stock) {
        this.stock = stock;
    }

    public double getStockComprometido() {
        return stockComprometido;
    }

    public void setStockComprometido(double stockComprometido) {
        this.stockComprometido = stockComprometido;
    }
}

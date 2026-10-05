package com.droai.model;

/**
 * Representa una discrepancia entre la existencia física en almacén (saStockAlmacen ACT)
 * y la suma de existencia disponible en lotes (saLoteEntrada stock_actual).
 */
public class DiscrepanciaLoteDTO {
    private String coArt;
    private String artDes;
    private String coAlma;
    private String desAlma;
    private double stockAlmacen;
    private double stockLotes;
    private double diferencia; // stockAlmacen - stockLotes
    private int cantLotesActivos;
    private String estado;

    public DiscrepanciaLoteDTO() {}

    public DiscrepanciaLoteDTO(String coArt, String artDes, String coAlma, String desAlma,
                               double stockAlmacen, double stockLotes, double diferencia,
                               int cantLotesActivos) {
        this.coArt = coArt;
        this.artDes = artDes;
        this.coAlma = coAlma;
        this.desAlma = desAlma;
        this.stockAlmacen = stockAlmacen;
        this.stockLotes = stockLotes;
        this.diferencia = diferencia;
        this.cantLotesActivos = cantLotesActivos;
        determinarEstado();
    }

    private void determinarEstado() {
        if (stockAlmacen > 0 && stockLotes <= 0) {
            this.estado = "Sin Lote (Crítico)";
        } else if (stockAlmacen > stockLotes) {
            this.estado = "Lote Insuficiente";
        } else if (stockLotes > stockAlmacen) {
            this.estado = "Lote en Exceso";
        } else {
            this.estado = "Nivelado";
        }
    }

    public String getCoArt() { return coArt; }
    public void setCoArt(String coArt) { this.coArt = coArt; }

    public String getArtDes() { return artDes; }
    public void setArtDes(String artDes) { this.artDes = artDes; }

    public String getCoAlma() { return coAlma; }
    public void setCoAlma(String coAlma) { this.coAlma = coAlma; }

    public String getDesAlma() { return desAlma; }
    public void setDesAlma(String desAlma) { this.desAlma = desAlma; }

    public double getStockAlmacen() { return stockAlmacen; }
    public void setStockAlmacen(double stockAlmacen) { 
        this.stockAlmacen = stockAlmacen;
        determinarEstado();
    }

    public double getStockLotes() { return stockLotes; }
    public void setStockLotes(double stockLotes) { 
        this.stockLotes = stockLotes;
        determinarEstado();
    }

    public double getDiferencia() { return diferencia; }
    public void setDiferencia(double diferencia) { 
        this.diferencia = diferencia;
        determinarEstado();
    }

    public int getCantLotesActivos() { return cantLotesActivos; }
    public void setCantLotesActivos(int cantLotesActivos) { this.cantLotesActivos = cantLotesActivos; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}

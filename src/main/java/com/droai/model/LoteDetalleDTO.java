package com.droai.model;

/**
 * Detalle de un registro de saLoteEntrada para consulta y edición.
 */
public class LoteDetalleDTO {
    private String rowguid;
    private String coArt;
    private String coAlma;
    private String numeroLote;
    private String fechaExpiracion;
    private double cantidadInicial;
    private double stockActual;
    private String tipoDoc;
    private String fechaIngreso;

    public LoteDetalleDTO() {}

    public LoteDetalleDTO(String rowguid, String coArt, String coAlma, String numeroLote,
                          String fechaExpiracion, double cantidadInicial, double stockActual,
                          String tipoDoc, String fechaIngreso) {
        this.rowguid = rowguid;
        this.coArt = coArt;
        this.coAlma = coAlma;
        this.numeroLote = numeroLote;
        this.fechaExpiracion = fechaExpiracion;
        this.cantidadInicial = cantidadInicial;
        this.stockActual = stockActual;
        this.tipoDoc = tipoDoc;
        this.fechaIngreso = fechaIngreso;
    }

    public String getRowguid() { return rowguid; }
    public void setRowguid(String rowguid) { this.rowguid = rowguid; }

    public String getCoArt() { return coArt; }
    public void setCoArt(String coArt) { this.coArt = coArt; }

    public String getCoAlma() { return coAlma; }
    public void setCoAlma(String coAlma) { this.coAlma = coAlma; }

    public String getNumeroLote() { return numeroLote; }
    public void setNumeroLote(String numeroLote) { this.numeroLote = numeroLote; }

    public String getFechaExpiracion() { return fechaExpiracion; }
    public void setFechaExpiracion(String fechaExpiracion) { this.fechaExpiracion = fechaExpiracion; }

    public double getCantidadInicial() { return cantidadInicial; }
    public void setCantidadInicial(double cantidadInicial) { this.cantidadInicial = cantidadInicial; }

    public double getStockActual() { return stockActual; }
    public void setStockActual(double stockActual) { this.stockActual = stockActual; }

    public String getTipoDoc() { return tipoDoc; }
    public void setTipoDoc(String tipoDoc) { this.tipoDoc = tipoDoc; }

    public String getFechaIngreso() { return fechaIngreso; }
    public void setFechaIngreso(String fechaIngreso) { this.fechaIngreso = fechaIngreso; }
}

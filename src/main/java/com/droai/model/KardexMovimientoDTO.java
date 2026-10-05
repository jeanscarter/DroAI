package com.droai.model;

/**
 * Movimiento de kardex histórico (recepciones, ajustes, notas de entrega, facturas).
 */
public class KardexMovimientoDTO {
    private String tipoDoc;
    private String docNum;
    private String fecha;
    private int rengNum;
    private double cantidad;
    private boolean loteAsignado;
    private String origenODestino;

    public KardexMovimientoDTO() {}

    public KardexMovimientoDTO(String tipoDoc, String docNum, String fecha, int rengNum,
                               double cantidad, boolean loteAsignado, String origenODestino) {
        this.tipoDoc = tipoDoc;
        this.docNum = docNum;
        this.fecha = fecha;
        this.rengNum = rengNum;
        this.cantidad = cantidad;
        this.loteAsignado = loteAsignado;
        this.origenODestino = origenODestino;
    }

    public String getTipoDoc() { return tipoDoc; }
    public void setTipoDoc(String tipoDoc) { this.tipoDoc = tipoDoc; }

    public String getDocNum() { return docNum; }
    public void setDocNum(String docNum) { this.docNum = docNum; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public int getRengNum() { return rengNum; }
    public void setRengNum(int rengNum) { this.rengNum = rengNum; }

    public double getCantidad() { return cantidad; }
    public void setCantidad(double cantidad) { this.cantidad = cantidad; }

    public boolean isLoteAsignado() { return loteAsignado; }
    public void setLoteAsignado(boolean loteAsignado) { this.loteAsignado = loteAsignado; }

    public String getOrigenODestino() { return origenODestino; }
    public void setOrigenODestino(String origenODestino) { this.origenODestino = origenODestino; }
}

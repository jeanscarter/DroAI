package com.droai.model;

/**
 * Representa una fila en el reporte de Movimiento de Producto (Kárdex Multimoneda).
 */
public class KardexMovimientoValoradoRow {

    private String fecha;
    private String tipoDoc;
    private String docNum;
    private String almacen;
    private String origenDestino;
    private String lote;
    private double cantidadEntrada;
    private double cantidadSalida;
    private double saldo;
    private double costoUnitUSD;
    private double costoUnitBS;
    private double totalUSD;
    private double totalBS;

    public KardexMovimientoValoradoRow() {}

    public KardexMovimientoValoradoRow(String fecha, String tipoDoc, String docNum, String almacen,
                                       String origenDestino, String lote, double cantidadEntrada,
                                       double cantidadSalida, double saldo, double costoUnitUSD,
                                       double costoUnitBS, double totalUSD, double totalBS) {
        this.fecha = fecha;
        this.tipoDoc = tipoDoc;
        this.docNum = docNum;
        this.almacen = almacen;
        this.origenDestino = origenDestino;
        this.lote = lote;
        this.cantidadEntrada = cantidadEntrada;
        this.cantidadSalida = cantidadSalida;
        this.saldo = saldo;
        this.costoUnitUSD = costoUnitUSD;
        this.costoUnitBS = costoUnitBS;
        this.totalUSD = totalUSD;
        this.totalBS = totalBS;
    }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getTipoDoc() { return tipoDoc; }
    public void setTipoDoc(String tipoDoc) { this.tipoDoc = tipoDoc; }

    public String getDocNum() { return docNum; }
    public void setDocNum(String docNum) { this.docNum = docNum; }

    public String getAlmacen() { return almacen; }
    public void setAlmacen(String almacen) { this.almacen = almacen; }

    public String getOrigenDestino() { return origenDestino; }
    public void setOrigenDestino(String origenDestino) { this.origenDestino = origenDestino; }

    public String getLote() { return lote; }
    public void setLote(String lote) { this.lote = lote; }

    public double getCantidadEntrada() { return cantidadEntrada; }
    public void setCantidadEntrada(double cantidadEntrada) { this.cantidadEntrada = cantidadEntrada; }

    public double getCantidadSalida() { return cantidadSalida; }
    public void setCantidadSalida(double cantidadSalida) { this.cantidadSalida = cantidadSalida; }

    public double getSaldo() { return saldo; }
    public void setSaldo(double saldo) { this.saldo = saldo; }

    public double getCostoUnitUSD() { return costoUnitUSD; }
    public void setCostoUnitUSD(double costoUnitUSD) { this.costoUnitUSD = costoUnitUSD; }

    public double getCostoUnitBS() { return costoUnitBS; }
    public void setCostoUnitBS(double costoUnitBS) { this.costoUnitBS = costoUnitBS; }

    public double getTotalUSD() { return totalUSD; }
    public void setTotalUSD(double totalUSD) { this.totalUSD = totalUSD; }

    public double getTotalBS() { return totalBS; }
    public void setTotalBS(double totalBS) { this.totalBS = totalBS; }
}

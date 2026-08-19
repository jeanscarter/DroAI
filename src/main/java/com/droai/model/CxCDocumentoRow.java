package com.droai.model;

import java.time.LocalDate;

/**
 * Modelo para las filas del reporte de Estado de Cuentas por Cobrar (CxC).
 * Reproduce las columnas de la pestaña '14-08-26' del Excel Maestro de Cobranzas.
 */
public class CxCDocumentoRow {

    private String codigoCliente;
    private String grupoCliente;
    private String cliente;
    private String factura;
    private String tipoDoc;
    private String facturaImpaga; // "F-I" o vacío
    private LocalDate fechaEmision;
    private LocalDate fechaVencimiento;
    private int diasVencimiento;
    private double neto;
    private double iva;
    private double saldo;
    private double tasa;
    private double totalBs;
    private double porVencer;
    private double vencido1a30;
    private double vencido31a60;
    private double vencido61a90;
    private double vencidoMas91;
    private String codVendedor;
    private String nombreVendedor;
    private String analista;
    private String pedido;

    public CxCDocumentoRow() {}

    // Getters y Setters
    public String getCodigoCliente() {
        return codigoCliente;
    }

    public void setCodigoCliente(String codigoCliente) {
        this.codigoCliente = codigoCliente;
    }

    public String getGrupoCliente() {
        return grupoCliente;
    }

    public void setGrupoCliente(String grupoCliente) {
        this.grupoCliente = grupoCliente;
    }

    public String getCliente() {
        return cliente;
    }

    public void setCliente(String cliente) {
        this.cliente = cliente;
    }

    public String getFactura() {
        return factura;
    }

    public void setFactura(String factura) {
        this.factura = factura;
    }

    public String getTipoDoc() {
        return tipoDoc;
    }

    public void setTipoDoc(String tipoDoc) {
        this.tipoDoc = tipoDoc;
    }

    public String getFacturaImpaga() {
        return facturaImpaga;
    }

    public void setFacturaImpaga(String facturaImpaga) {
        this.facturaImpaga = facturaImpaga;
    }

    public LocalDate getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(LocalDate fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(LocalDate fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }

    public int getDiasVencimiento() {
        return diasVencimiento;
    }

    public void setDiasVencimiento(int diasVencimiento) {
        this.diasVencimiento = diasVencimiento;
    }

    public double getNeto() {
        return neto;
    }

    public void setNeto(double neto) {
        this.neto = neto;
    }

    public double getIva() {
        return iva;
    }

    public void setIva(double iva) {
        this.iva = iva;
    }

    public double getSaldo() {
        return saldo;
    }

    public void setSaldo(double saldo) {
        this.saldo = saldo;
    }

    public double getTasa() {
        return tasa;
    }

    public void setTasa(double tasa) {
        this.tasa = tasa;
    }

    public double getTotalBs() {
        return totalBs;
    }

    public void setTotalBs(double totalBs) {
        this.totalBs = totalBs;
    }

    public double getPorVencer() {
        return porVencer;
    }

    public void setPorVencer(double porVencer) {
        this.porVencer = porVencer;
    }

    public double getVencido1a30() {
        return vencido1a30;
    }

    public void setVencido1a30(double vencido1a30) {
        this.vencido1a30 = vencido1a30;
    }

    public double getVencido31a60() {
        return vencido31a60;
    }

    public void setVencido31a60(double vencido31a60) {
        this.vencido31a60 = vencido31a60;
    }

    public double getVencido61a90() {
        return vencido61a90;
    }

    public void setVencido61a90(double vencido61a90) {
        this.vencido61a90 = vencido61a90;
    }

    public double getVencidoMas91() {
        return vencidoMas91;
    }

    public void setVencidoMas91(double vencidoMas91) {
        this.vencidoMas91 = vencidoMas91;
    }

    public String getCodVendedor() {
        return codVendedor;
    }

    public void setCodVendedor(String codVendedor) {
        this.codVendedor = codVendedor;
    }

    public String getNombreVendedor() {
        return nombreVendedor;
    }

    public void setNombreVendedor(String nombreVendedor) {
        this.nombreVendedor = nombreVendedor;
    }

    public String getAnalista() {
        return analista;
    }

    public void setAnalista(String analista) {
        this.analista = analista;
    }

    public String getPedido() {
        return pedido;
    }

    public void setPedido(String pedido) {
        this.pedido = pedido;
    }
}

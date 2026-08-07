package com.droai.model;

import java.time.LocalDate;

/**
 * Modelo de datos para representar una fila de la relación de comisiones (Hoja GENERAL (2)).
 */
public class ComisionRow {

    private int numero;
    private String tipoDoc;
    private String numeroDocumento;
    private String clase;
    private LocalDate fechaEmision;
    private LocalDate fechaVencimiento;
    private LocalDate fechaCobro;
    private String numeroCobro;
    private int diasCalle;
    private String codigoCliente;
    private String nombreCliente;
    private double montoDocumento;
    private double porcDesc;
    private double montoCobrado;
    private double baseComision;
    private double porcComision;
    private double montoComision;
    private String codigoVendedor;
    private String nombreVendedor;
    private String psico;
    private boolean facturaCerrada;

    public ComisionRow() {
    }

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public String getTipoDoc() {
        return tipoDoc;
    }

    public void setTipoDoc(String tipoDoc) {
        this.tipoDoc = tipoDoc;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public void setNumeroDocumento(String numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
    }

    public String getClase() {
        return clase;
    }

    public void setClase(String clase) {
        this.clase = clase;
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

    public LocalDate getFechaCobro() {
        return fechaCobro;
    }

    public void setFechaCobro(LocalDate fechaCobro) {
        this.fechaCobro = fechaCobro;
    }

    public String getNumeroCobro() {
        return numeroCobro;
    }

    public void setNumeroCobro(String numeroCobro) {
        this.numeroCobro = numeroCobro;
    }

    public int getDiasCalle() {
        return diasCalle;
    }

    public void setDiasCalle(int diasCalle) {
        this.diasCalle = diasCalle;
    }

    public String getCodigoCliente() {
        return codigoCliente;
    }

    public void setCodigoCliente(String codigoCliente) {
        this.codigoCliente = codigoCliente;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }

    public double getMontoDocumento() {
        return montoDocumento;
    }

    public void setMontoDocumento(double montoDocumento) {
        this.montoDocumento = montoDocumento;
    }

    public double getPorcDesc() {
        return porcDesc;
    }

    public void setPorcDesc(double porcDesc) {
        this.porcDesc = porcDesc;
    }

    public double getMontoCobrado() {
        return montoCobrado;
    }

    public void setMontoCobrado(double montoCobrado) {
        this.montoCobrado = montoCobrado;
    }

    public double getBaseComision() {
        return baseComision;
    }

    public void setBaseComision(double baseComision) {
        this.baseComision = baseComision;
    }

    public double getPorcComision() {
        return porcComision;
    }

    public void setPorcComision(double porcComision) {
        this.porcComision = porcComision;
    }

    public double getMontoComision() {
        return montoComision;
    }

    public void setMontoComision(double montoComision) {
        this.montoComision = montoComision;
    }

    public String getCodigoVendedor() {
        return codigoVendedor;
    }

    public void setCodigoVendedor(String codigoVendedor) {
        this.codigoVendedor = codigoVendedor;
    }

    public String getNombreVendedor() {
        return nombreVendedor;
    }

    public void setNombreVendedor(String nombreVendedor) {
        this.nombreVendedor = nombreVendedor;
    }

    public String getPsico() {
        return psico;
    }

    public void setPsico(String psico) {
        this.psico = psico;
    }

    public boolean isFacturaCerrada() {
        return facturaCerrada;
    }

    public void setFacturaCerrada(boolean facturaCerrada) {
        this.facturaCerrada = facturaCerrada;
    }
}

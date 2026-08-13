package com.droai.model;

/**
 * DTO que representa una fila del proceso de Carga Masiva de Costos y Precios desde Excel.
 */
public class CargaMasivaCostosPreciosRow {

    private String coArt;
    private String descripcion;

    private double costoActualBs;
    private double costoActualUsd;
    private double costoNuevoUsd;
    private double costoNuevoBs;

    private double precio1ActualBs;
    private double precio1ActualUsd;
    private double precio1NuevoUsd;
    private double precio1NuevoBs;

    private double precio1MontoRawBd;
    private double costoRawBd;
    private int precioOmActual;
    private boolean precioEnBsDetectado;

    private double tasaUsd = 1.0;

    private boolean existeEnBd;
    private boolean valido;
    private String estado;

    public CargaMasivaCostosPreciosRow() {
        this.valido = true;
        this.estado = "Pendiente";
    }

    public String getCoArt() {
        return coArt;
    }

    public void setCoArt(String coArt) {
        this.coArt = coArt;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    // --- Métodos de compatibilidad y alias ---
    public double getCostoActual() {
        return costoActualUsd;
    }

    public void setCostoActual(double costoActual) {
        this.costoActualUsd = costoActual;
    }

    public double getCostoNuevo() {
        return costoNuevoUsd;
    }

    public void setCostoNuevo(double costoNuevo) {
        this.costoNuevoUsd = costoNuevo;
    }

    public double getPrecio1Actual() {
        return precio1ActualUsd;
    }

    public void setPrecio1Actual(double precio1Actual) {
        this.precio1ActualUsd = precio1Actual;
    }

    public double getPrecio1Nuevo() {
        return precio1NuevoUsd;
    }

    public void setPrecio1Nuevo(double precio1Nuevo) {
        this.precio1NuevoUsd = precio1Nuevo;
    }

    // --- Métodos explicitos USD / Bs / Tasa ---
    public double getCostoActualBs() {
        return costoActualBs;
    }

    public void setCostoActualBs(double costoActualBs) {
        this.costoActualBs = costoActualBs;
    }

    public double getCostoActualUsd() {
        return costoActualUsd;
    }

    public void setCostoActualUsd(double costoActualUsd) {
        this.costoActualUsd = costoActualUsd;
    }

    public double getCostoNuevoUsd() {
        return costoNuevoUsd;
    }

    public void setCostoNuevoUsd(double costoNuevoUsd) {
        this.costoNuevoUsd = costoNuevoUsd;
    }

    public double getCostoNuevoBs() {
        return costoNuevoBs;
    }

    public void setCostoNuevoBs(double costoNuevoBs) {
        this.costoNuevoBs = costoNuevoBs;
    }

    public double getPrecio1ActualBs() {
        return precio1ActualBs;
    }

    public void setPrecio1ActualBs(double precio1ActualBs) {
        this.precio1ActualBs = precio1ActualBs;
    }

    public double getPrecio1ActualUsd() {
        return precio1ActualUsd;
    }

    public void setPrecio1ActualUsd(double precio1ActualUsd) {
        this.precio1ActualUsd = precio1ActualUsd;
    }

    public double getPrecio1NuevoUsd() {
        return precio1NuevoUsd;
    }

    public void setPrecio1NuevoUsd(double precio1NuevoUsd) {
        this.precio1NuevoUsd = precio1NuevoUsd;
    }

    public double getPrecio1NuevoBs() {
        return precio1NuevoBs;
    }

    public void setPrecio1NuevoBs(double precio1NuevoBs) {
        this.precio1NuevoBs = precio1NuevoBs;
    }

    public double getPrecio1MontoRawBd() {
        return precio1MontoRawBd;
    }

    public void setPrecio1MontoRawBd(double precio1MontoRawBd) {
        this.precio1MontoRawBd = precio1MontoRawBd;
    }

    public double getCostoRawBd() {
        return costoRawBd;
    }

    public void setCostoRawBd(double costoRawBd) {
        this.costoRawBd = costoRawBd;
    }

    public int getPrecioOmActual() {
        return precioOmActual;
    }

    public void setPrecioOmActual(int precioOmActual) {
        this.precioOmActual = precioOmActual;
    }

    public boolean isPrecioEnBsDetectado() {
        return precioEnBsDetectado;
    }

    public void setPrecioEnBsDetectado(boolean precioEnBsDetectado) {
        this.precioEnBsDetectado = precioEnBsDetectado;
    }

    public double getTasaUsd() {
        return tasaUsd;
    }

    public void setTasaUsd(double tasaUsd) {
        this.tasaUsd = tasaUsd;
    }

    public boolean isExisteEnBd() {
        return existeEnBd;
    }

    public void setExisteEnBd(boolean existeEnBd) {
        this.existeEnBd = existeEnBd;
    }

    public boolean isValido() {
        return valido;
    }

    public void setValido(boolean valido) {
        this.valido = valido;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public boolean tieneCambios() {
        // Comparar costo nuevo USD contra el raw de BD (costo siempre está en USD en Profit)
        boolean cambioCosto = costoNuevoUsd > 0 && Math.abs(costoNuevoUsd - costoRawBd) > 0.0001;
        // Comparar precio nuevo USD vs precio actual USD (ambos correctamente convertidos)
        // También detectar si precioOm necesita corrección de 0 (Bs) → 1 (USD)
        // También comparar el Bs nuevo vs el monto raw de BD para detectar cuando
        // el monto almacenado está en USD pero debería estar en Bs
        boolean cambioPrecio = precio1NuevoUsd > 0 && (
                precioEnBsDetectado
                || Math.abs(precio1NuevoUsd - precio1ActualUsd) > 0.0001
                || Math.abs(precio1NuevoBs - precio1MontoRawBd) > 0.01
        );
        return cambioCosto || cambioPrecio;
    }
}

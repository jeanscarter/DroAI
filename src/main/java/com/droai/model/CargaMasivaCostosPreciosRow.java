package com.droai.model;

/**
 * DTO que representa una fila del proceso de Carga Masiva de Costos y Precios desde Excel.
 */
public class CargaMasivaCostosPreciosRow {

    private String coArt;
    private String descripcion;
    private double costoActual;
    private double costoNuevo;
    private double precio1Actual;
    private double precio1Nuevo;
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

    public double getCostoActual() {
        return costoActual;
    }

    public void setCostoActual(double costoActual) {
        this.costoActual = costoActual;
    }

    public double getCostoNuevo() {
        return costoNuevo;
    }

    public void setCostoNuevo(double costoNuevo) {
        this.costoNuevo = costoNuevo;
    }

    public double getPrecio1Actual() {
        return precio1Actual;
    }

    public void setPrecio1Actual(double precio1Actual) {
        this.precio1Actual = precio1Actual;
    }

    public double getPrecio1Nuevo() {
        return precio1Nuevo;
    }

    public void setPrecio1Nuevo(double precio1Nuevo) {
        this.precio1Nuevo = precio1Nuevo;
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
        boolean cambioCosto = Math.abs(costoNuevo - costoActual) > 0.0001 && costoNuevo > 0;
        boolean cambioPrecio = Math.abs(precio1Nuevo - precio1Actual) > 0.0001 && precio1Nuevo > 0;
        return cambioCosto || cambioPrecio;
    }
}

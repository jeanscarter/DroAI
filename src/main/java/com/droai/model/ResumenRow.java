package com.droai.model;

/**
 * Fila genérica para las pestañas de resumen (Descuentos x Volumen, Descuento x Producto).
 */
public class ResumenRow {

    private String clave;
    private String descripcion;
    private double total;
    private double descuento;
    private double neto;
    private double porcentaje;

    public ResumenRow() {}

    public ResumenRow(String clave, String descripcion, double total,
                      double descuento, double neto, double porcentaje) {
        this.clave       = clave;
        this.descripcion = descripcion;
        this.total       = total;
        this.descuento   = descuento;
        this.neto        = neto;
        this.porcentaje  = porcentaje;
    }

    public String getClave()          { return clave; }
    public void setClave(String v)    { this.clave = v; }

    public String getDescripcion()         { return descripcion; }
    public void setDescripcion(String v)   { this.descripcion = v; }

    public double getTotal()          { return total; }
    public void setTotal(double v)    { this.total = v; }

    public double getDescuento()          { return descuento; }
    public void setDescuento(double v)    { this.descuento = v; }

    public double getNeto()          { return neto; }
    public void setNeto(double v)    { this.neto = v; }

    public double getPorcentaje()          { return porcentaje; }
    public void setPorcentaje(double v)    { this.porcentaje = v; }
}

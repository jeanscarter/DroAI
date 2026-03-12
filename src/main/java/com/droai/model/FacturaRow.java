package com.droai.model;

/**
 * Modelo de una fila del Listado de Precios.
 * Mapea columnas: Codigo, Descripcion, Referencia, Existencia, UdM,
 * Costo Fabrica, Arancel%, Costo OM, Util%, Precio1, %IVA, Precio C/IVA.
 */
public class FacturaRow {

    private String  codigo;
    private String  descripcion;
    private String  referencia;
    private double  existencia;
    private String  udm;
    private double  costoFabrica;
    private double  arancelPct;
    private double  costoOM;
    private double  utilPct;
    private double  precio1;
    private double  ivaPct;
    private double  precioCIVA;

    // --- Metadata para tracking de edición ---
    private boolean modified;

    public FacturaRow() {}

    public FacturaRow(String codigo, String descripcion, String referencia,
                      double existencia, String udm, double costoFabrica,
                      double arancelPct, double costoOM, double utilPct,
                      double precio1, double ivaPct, double precioCIVA) {
        this.codigo       = codigo;
        this.descripcion  = descripcion;
        this.referencia   = referencia;
        this.existencia   = existencia;
        this.udm          = udm;
        this.costoFabrica = costoFabrica;
        this.arancelPct   = arancelPct;
        this.costoOM      = costoOM;
        this.utilPct      = utilPct;
        this.precio1      = precio1;
        this.ivaPct       = ivaPct;
        this.precioCIVA   = precioCIVA;
    }

    // ---------- Getters / Setters ----------

    public String getCodigo()        { return codigo; }
    public void setCodigo(String v)  { this.codigo = v; }

    public String getDescripcion()        { return descripcion; }
    public void setDescripcion(String v)  { this.descripcion = v; }

    public String getReferencia()        { return referencia; }
    public void setReferencia(String v)  { this.referencia = v; }

    public double getExistencia()        { return existencia; }
    public void setExistencia(double v)  { this.existencia = v; }

    public String getUdm()        { return udm; }
    public void setUdm(String v)  { this.udm = v; }

    public double getCostoFabrica()        { return costoFabrica; }
    public void setCostoFabrica(double v)  { this.costoFabrica = v; markModified(); }

    public double getArancelPct()        { return arancelPct; }
    public void setArancelPct(double v)  { this.arancelPct = v; markModified(); }

    public double getCostoOM()        { return costoOM; }
    public void setCostoOM(double v)  { this.costoOM = v; }

    public double getUtilPct()        { return utilPct; }
    public void setUtilPct(double v)  { this.utilPct = v; markModified(); }

    public double getPrecio1()        { return precio1; }
    public void setPrecio1(double v)  { this.precio1 = v; markModified(); }

    public double getIvaPct()        { return ivaPct; }
    public void setIvaPct(double v)  { this.ivaPct = v; markModified(); }

    public double getPrecioCIVA()        { return precioCIVA; }
    public void setPrecioCIVA(double v)  { this.precioCIVA = v; }

    public boolean isModified()   { return modified; }
    public void markModified()    { this.modified = true; }
    public void clearModified()   { this.modified = false; }
}

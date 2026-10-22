package com.droai.model;

/**
 * Fila representativa de un renglón de venta con su costo de ventas determinado
 * según las reglas contables:
 * - Venta Firme: Costo promedio a la fecha de la venta.
 * - Consignación: Último costo reportado por el proveedor a la fecha de la venta.
 */
public class CostoVentaRow {

    private String tipoDoc;
    private String docNum;
    private String fecha;
    private String coCli;
    private String cliDes;
    private String coArt;
    private String artDes;
    private String coAlma;
    private String tipoMercancia; // "VENTA FIRME" o "CONSIGNACION"
    private double cantidad;
    private double precioVentaUSD;
    private double precioVentaBS;
    private double totalVentaUSD;
    private double totalVentaBS;
    private double costoUnitUSD;
    private double costoUnitBS;
    private double totalCostoUSD;
    private double totalCostoBS;
    private double utilidadUSD;
    private double utilidadBS;
    private double margenPct;
    private double tasaDoc;

    public CostoVentaRow() {}

    public String getTipoDoc() { return tipoDoc; }
    public void setTipoDoc(String tipoDoc) { this.tipoDoc = tipoDoc; }

    public String getDocNum() { return docNum; }
    public void setDocNum(String docNum) { this.docNum = docNum; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getCoCli() { return coCli; }
    public void setCoCli(String coCli) { this.coCli = coCli; }

    public String getCliDes() { return cliDes; }
    public void setCliDes(String cliDes) { this.cliDes = cliDes; }

    public String getCoArt() { return coArt; }
    public void setCoArt(String coArt) { this.coArt = coArt; }

    public String getArtDes() { return artDes; }
    public void setArtDes(String artDes) { this.artDes = artDes; }

    public String getCoAlma() { return coAlma; }
    public void setCoAlma(String coAlma) { this.coAlma = coAlma; }

    public String getTipoMercancia() { return tipoMercancia; }
    public void setTipoMercancia(String tipoMercancia) { this.tipoMercancia = tipoMercancia; }

    public double getCantidad() { return cantidad; }
    public void setCantidad(double cantidad) { this.cantidad = cantidad; }

    public double getPrecioVentaUSD() { return precioVentaUSD; }
    public void setPrecioVentaUSD(double precioVentaUSD) { this.precioVentaUSD = precioVentaUSD; }

    public double getPrecioVentaBS() { return precioVentaBS; }
    public void setPrecioVentaBS(double precioVentaBS) { this.precioVentaBS = precioVentaBS; }

    public double getTotalVentaUSD() { return totalVentaUSD; }
    public void setTotalVentaUSD(double totalVentaUSD) { this.totalVentaUSD = totalVentaUSD; }

    public double getTotalVentaBS() { return totalVentaBS; }
    public void setTotalVentaBS(double totalVentaBS) { this.totalVentaBS = totalVentaBS; }

    public double getCostoUnitUSD() { return costoUnitUSD; }
    public void setCostoUnitUSD(double costoUnitUSD) { this.costoUnitUSD = costoUnitUSD; }

    public double getCostoUnitBS() { return costoUnitBS; }
    public void setCostoUnitBS(double costoUnitBS) { this.costoUnitBS = costoUnitBS; }

    public double getTotalCostoUSD() { return totalCostoUSD; }
    public void setTotalCostoUSD(double totalCostoUSD) { this.totalCostoUSD = totalCostoUSD; }

    public double getTotalCostoBS() { return totalCostoBS; }
    public void setTotalCostoBS(double totalCostoBS) { this.totalCostoBS = totalCostoBS; }

    public double getUtilidadUSD() { return utilidadUSD; }
    public void setUtilidadUSD(double utilidadUSD) { this.utilidadUSD = utilidadUSD; }

    public double getUtilidadBS() { return utilidadBS; }
    public void setUtilidadBS(double utilidadBS) { this.utilidadBS = utilidadBS; }

    public double getMargenPct() { return margenPct; }
    public void setMargenPct(double margenPct) { this.margenPct = margenPct; }

    public double getTasaDoc() { return tasaDoc; }
    public void setTasaDoc(double tasaDoc) { this.tasaDoc = tasaDoc; }
}

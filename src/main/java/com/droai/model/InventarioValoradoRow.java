package com.droai.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Fila de datos para el reporte de Inventario Valorado a fecha determinada.
 */
public class InventarioValoradoRow {

    private String coArt;
    private String artDes;
    private String linea;
    private String sublinea;
    private String categoria;
    private String coProv;
    private String nomProv;
    private String coAlma;
    private String desAlma;
    private String tipoMercancia; // "VENTA FIRME" o "CONSIGNACION"
    private double existencia;
    private double costoPromedioUSD;
    private double costoPromedioBS;
    private double ultimoCostoUSD;
    private double ultimoCostoBS;
    private double costoProveedorUSD;
    private double costoProveedorBS;
    private double totalValorizadoUSD;
    private double totalValorizadoBS;
    private List<CapaCostoDTO> capas = new ArrayList<>();

    public InventarioValoradoRow() {}

    public String getCoArt() { return coArt; }
    public void setCoArt(String coArt) { this.coArt = coArt; }

    public String getArtDes() { return artDes; }
    public void setArtDes(String artDes) { this.artDes = artDes; }

    public String getLinea() { return linea; }
    public void setLinea(String linea) { this.linea = linea; }

    public String getSublinea() { return sublinea; }
    public void setSublinea(String sublinea) { this.sublinea = sublinea; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getCoProv() { return coProv; }
    public void setCoProv(String coProv) { this.coProv = coProv; }

    public String getNomProv() { return nomProv; }
    public void setNomProv(String nomProv) { this.nomProv = nomProv; }

    public String getCoAlma() { return coAlma; }
    public void setCoAlma(String coAlma) { this.coAlma = coAlma; }

    public String getDesAlma() { return desAlma; }
    public void setDesAlma(String desAlma) { this.desAlma = desAlma; }

    public String getTipoMercancia() { return tipoMercancia; }
    public void setTipoMercancia(String tipoMercancia) { this.tipoMercancia = tipoMercancia; }

    public double getExistencia() { return existencia; }
    public void setExistencia(double existencia) { this.existencia = existencia; }

    public double getCostoPromedioUSD() { return costoPromedioUSD; }
    public void setCostoPromedioUSD(double costoPromedioUSD) { this.costoPromedioUSD = costoPromedioUSD; }

    public double getCostoPromedioBS() { return costoPromedioBS; }
    public void setCostoPromedioBS(double costoPromedioBS) { this.costoPromedioBS = costoPromedioBS; }

    public double getUltimoCostoUSD() { return ultimoCostoUSD; }
    public void setUltimoCostoUSD(double ultimoCostoUSD) { this.ultimoCostoUSD = ultimoCostoUSD; }

    public double getUltimoCostoBS() { return ultimoCostoBS; }
    public void setUltimoCostoBS(double ultimoCostoBS) { this.ultimoCostoBS = ultimoCostoBS; }

    public double getCostoProveedorUSD() { return costoProveedorUSD; }
    public void setCostoProveedorUSD(double costoProveedorUSD) { this.costoProveedorUSD = costoProveedorUSD; }

    public double getCostoProveedorBS() { return costoProveedorBS; }
    public void setCostoProveedorBS(double costoProveedorBS) { this.costoProveedorBS = costoProveedorBS; }

    public double getTotalValorizadoUSD() { return totalValorizadoUSD; }
    public void setTotalValorizadoUSD(double totalValorizadoUSD) { this.totalValorizadoUSD = totalValorizadoUSD; }

    public double getTotalValorizadoBS() { return totalValorizadoBS; }
    public void setTotalValorizadoBS(double totalValorizadoBS) { this.totalValorizadoBS = totalValorizadoBS; }

    public List<CapaCostoDTO> getCapas() { return capas; }
    public void setCapas(List<CapaCostoDTO> capas) {
        this.capas = (capas != null) ? capas : new ArrayList<>();
    }

    public int getCantCapas() {
        return (capas != null) ? capas.size() : 0;
    }
}

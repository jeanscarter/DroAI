package com.droai.model;

/**
 * Representa una capa individual de compra (FIFO/PEPS) que compone la existencia
 * de un producto a una fecha de corte determinada.
 */
public class CapaCostoDTO {

    private String tipoDoc;            // NREC, FC, AJUS, etc.
    private String docNum;             // Número de documento
    private String fecha;              // Fecha de emisión (YYYY-MM-DD)
    private double cantidadTotal;      // Cantidad total comprada en el documento
    private double cantidadEnCapa;     // Cantidad de esta compra que aún vive en la existencia actual
    private double costUnitUSD;        // Costo unitario en USD
    private double costUnitBS;         // Costo unitario en Bs
    private double tasaCambio;         // Tasa de cambio de la compra
    private String coProv;             // Código de proveedor
    private String nomProv;            // Nombre de proveedor

    public CapaCostoDTO() {}

    public CapaCostoDTO(String tipoDoc, String docNum, String fecha, double cantidadTotal,
                        double cantidadEnCapa, double costUnitUSD, double costUnitBS,
                        double tasaCambio, String coProv, String nomProv) {
        this.tipoDoc = tipoDoc;
        this.docNum = docNum;
        this.fecha = fecha;
        this.cantidadTotal = cantidadTotal;
        this.cantidadEnCapa = cantidadEnCapa;
        this.costUnitUSD = costUnitUSD;
        this.costUnitBS = costUnitBS;
        this.tasaCambio = tasaCambio;
        this.coProv = coProv;
        this.nomProv = nomProv;
    }

    public double getSubtotalUSD() {
        return cantidadEnCapa * costUnitUSD;
    }

    public double getSubtotalBS() {
        return cantidadEnCapa * costUnitBS;
    }

    public String getTipoDoc() { return tipoDoc; }
    public void setTipoDoc(String tipoDoc) { this.tipoDoc = tipoDoc; }

    public String getDocNum() { return docNum; }
    public void setDocNum(String docNum) { this.docNum = docNum; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public double getCantidadTotal() { return cantidadTotal; }
    public void setCantidadTotal(double cantidadTotal) { this.cantidadTotal = cantidadTotal; }

    public double getCantidadEnCapa() { return cantidadEnCapa; }
    public void setCantidadEnCapa(double cantidadEnCapa) { this.cantidadEnCapa = cantidadEnCapa; }

    public double getCostUnitUSD() { return costUnitUSD; }
    public void setCostUnitUSD(double costUnitUSD) { this.costUnitUSD = costUnitUSD; }

    public double getCostUnitBS() { return costUnitBS; }
    public void setCostUnitBS(double costUnitBS) { this.costUnitBS = costUnitBS; }

    public double getTasaCambio() { return tasaCambio; }
    public void setTasaCambio(double tasaCambio) { this.tasaCambio = tasaCambio; }

    public String getCoProv() { return coProv; }
    public void setCoProv(String coProv) { this.coProv = coProv; }

    public String getNomProv() { return nomProv; }
    public void setNomProv(String nomProv) { this.nomProv = nomProv; }
}

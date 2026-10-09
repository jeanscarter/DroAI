package com.droai.model;

/**
 * Representa el detalle a nivel de renglón de un pedido para la auditoría de facturación.
 */
public class AuditoriaPedidoRenglonRow {

    private int rengNum;
    private String coArt;
    private String artDes;
    private double cantWeb;
    private double cantNota;
    private double cantFact;
    private double cantFaltante;
    private double precVta;
    private String porcDesc;
    private double rengNeto;
    private String nroLote;
    private String facturaDoc;
    private String estado; // FACTURADO, PENDIENTE_FACTURA, NO_VIAJO_A_NOTA, PARCIAL

    public AuditoriaPedidoRenglonRow() {
    }

    public AuditoriaPedidoRenglonRow(int rengNum, String coArt, String artDes, double cantWeb,
                                     double cantNota, double cantFact, double cantFaltante,
                                     double precVta, String porcDesc, double rengNeto,
                                     String nroLote, String facturaDoc, String estado) {
        this.rengNum = rengNum;
        this.coArt = coArt;
        this.artDes = artDes;
        this.cantWeb = cantWeb;
        this.cantNota = cantNota;
        this.cantFact = cantFact;
        this.cantFaltante = cantFaltante;
        this.precVta = precVta;
        this.porcDesc = porcDesc;
        this.rengNeto = rengNeto;
        this.nroLote = nroLote;
        this.facturaDoc = facturaDoc;
        this.estado = estado;
    }

    public int getRengNum() { return rengNum; }
    public void setRengNum(int rengNum) { this.rengNum = rengNum; }

    public String getCoArt() { return coArt; }
    public void setCoArt(String coArt) { this.coArt = coArt; }

    public String getArtDes() { return artDes; }
    public void setArtDes(String artDes) { this.artDes = artDes; }

    public double getCantWeb() { return cantWeb; }
    public void setCantWeb(double cantWeb) { this.cantWeb = cantWeb; }

    public double getCantNota() { return cantNota; }
    public void setCantNota(double cantNota) { this.cantNota = cantNota; }

    public double getCantFact() { return cantFact; }
    public void setCantFact(double cantFact) { this.cantFact = cantFact; }

    public double getCantFaltante() { return cantFaltante; }
    public void setCantFaltante(double cantFaltante) { this.cantFaltante = cantFaltante; }

    public double getPrecVta() { return precVta; }
    public void setPrecVta(double precVta) { this.precVta = precVta; }

    public String getPorcDesc() { return porcDesc; }
    public void setPorcDesc(String porcDesc) { this.porcDesc = porcDesc; }

    public double getRengNeto() { return rengNeto; }
    public void setRengNeto(double rengNeto) { this.rengNeto = rengNeto; }

    public String getNroLote() { return nroLote; }
    public void setNroLote(String nroLote) { this.nroLote = nroLote; }

    public String getFacturaDoc() { return facturaDoc; }
    public void setFacturaDoc(String facturaDoc) { this.facturaDoc = facturaDoc; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}

package com.droai.model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa la cabecera consolidada de un pedido auditado entre Pedido Web (TXT SIAD),
 * Nota de Entrega (Profit Plus) y Factura de Venta.
 */
public class AuditoriaPedidoRow {

    private String pedidoWeb;
    private String notaEntrega;
    private String docNumOriginal;
    private String facturas;
    private Timestamp fecEmis;
    private String coCli;
    private String cliDes;
    private String coVen;
    private String venDes;

    private int totalRengWeb;
    private int totalRengNota;
    private int totalRengFact;
    private int totalRengFaltantes;

    private double totalUndWeb;
    private double totalUndNota;
    private double totalUndFact;
    private double totalUndFaltantes;

    private double totalMontoNota;
    private double totalMontoFact;
    private double totalMontoPend;

    private boolean anulado;
    private String estado; // CRITICO_FALTAN_RENGLONES, PARCIALMENTE_FACTURADO, PENDIENTE_FACTURAR, FACTURADO_TOTAL, ANULADO
    private String diagnostico;

    private List<AuditoriaPedidoRenglonRow> renglones = new ArrayList<>();

    public AuditoriaPedidoRow() {
    }

    public String getPedidoWeb() { return pedidoWeb; }
    public void setPedidoWeb(String pedidoWeb) { this.pedidoWeb = pedidoWeb; }

    public String getNotaEntrega() { return notaEntrega; }
    public void setNotaEntrega(String notaEntrega) { this.notaEntrega = notaEntrega; }

    public String getDocNumOriginal() { return docNumOriginal; }
    public void setDocNumOriginal(String docNumOriginal) { this.docNumOriginal = docNumOriginal; }

    public String getFacturas() { return facturas; }
    public void setFacturas(String facturas) { this.facturas = facturas; }

    public Timestamp getFecEmis() { return fecEmis; }
    public void setFecEmis(Timestamp fecEmis) { this.fecEmis = fecEmis; }

    public String getCoCli() { return coCli; }
    public void setCoCli(String coCli) { this.coCli = coCli; }

    public String getCliDes() { return cliDes; }
    public void setCliDes(String cliDes) { this.cliDes = cliDes; }

    public String getCoVen() { return coVen; }
    public void setCoVen(String coVen) { this.coVen = coVen; }

    public String getVenDes() { return venDes; }
    public void setVenDes(String venDes) { this.venDes = venDes; }

    public int getTotalRengWeb() { return totalRengWeb; }
    public void setTotalRengWeb(int totalRengWeb) { this.totalRengWeb = totalRengWeb; }

    public int getTotalRengNota() { return totalRengNota; }
    public void setTotalRengNota(int totalRengNota) { this.totalRengNota = totalRengNota; }

    public int getTotalRengFact() { return totalRengFact; }
    public void setTotalRengFact(int totalRengFact) { this.totalRengFact = totalRengFact; }

    public int getTotalRengFaltantes() { return totalRengFaltantes; }
    public void setTotalRengFaltantes(int totalRengFaltantes) { this.totalRengFaltantes = totalRengFaltantes; }

    public double getTotalUndWeb() { return totalUndWeb; }
    public void setTotalUndWeb(double totalUndWeb) { this.totalUndWeb = totalUndWeb; }

    public double getTotalUndNota() { return totalUndNota; }
    public void setTotalUndNota(double totalUndNota) { this.totalUndNota = totalUndNota; }

    public double getTotalUndFact() { return totalUndFact; }
    public void setTotalUndFact(double totalUndFact) { this.totalUndFact = totalUndFact; }

    public double getTotalUndFaltantes() { return totalUndFaltantes; }
    public void setTotalUndFaltantes(double totalUndFaltantes) { this.totalUndFaltantes = totalUndFaltantes; }

    public double getTotalMontoNota() { return totalMontoNota; }
    public void setTotalMontoNota(double totalMontoNota) { this.totalMontoNota = totalMontoNota; }

    public double getTotalMontoFact() { return totalMontoFact; }
    public void setTotalMontoFact(double totalMontoFact) { this.totalMontoFact = totalMontoFact; }

    public double getTotalMontoPend() { return totalMontoPend; }
    public void setTotalMontoPend(double totalMontoPend) { this.totalMontoPend = totalMontoPend; }

    public boolean isAnulado() { return anulado; }
    public void setAnulado(boolean anulado) { this.anulado = anulado; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getDiagnostico() { return diagnostico; }
    public void setDiagnostico(String diagnostico) { this.diagnostico = diagnostico; }

    public List<AuditoriaPedidoRenglonRow> getRenglones() { return renglones; }
    public void setRenglones(List<AuditoriaPedidoRenglonRow> renglones) { this.renglones = renglones; }
}

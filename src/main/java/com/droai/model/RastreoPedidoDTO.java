package com.droai.model;

import java.io.File;
import java.sql.Timestamp;

/**
 * DTO que encapsula el rastreo integral de un pedido web:
 * 1. Estado en el sistema de archivos SIAD (Error, Cola activa, Procesados).
 * 2. Detalle y diagnóstico de renglones y lotes en saStockAlmacen / saLoteEntrada.
 * 3. Documentos generados en Profit Plus (Nota de Entrega, Factura, Pedido de Venta).
 */
public class RastreoPedidoDTO {

    private String pedidoId;
    private String ubicacionArchivo; // "ERROR", "COLA", "PROCESADO", "NO_ENCONTRADO"
    private File archivoTxt;
    private File archivoError;
    private String mensajeErrorSiad;

    // Datos extraídos del archivo TXT de SIAD
    private String coCli;
    private String cliDes;
    private String coVen;
    private String venDes;
    private String fechaEmisionTxt;
    private double montoNetoTxt;
    private int totalRenglonesTxt;
    private double totalUnidadesTxt;

    // Diagnóstico detallado renglón a renglón
    private SiadDiagnosticoDTO diagnostico;

    // Datos en la base de datos de Profit Plus
    private boolean existeEnProfit;
    private String docNumNota;
    private Timestamp fecEmisNota;
    private double totalNetoNota;
    private String statusNota;
    private boolean anuladaNota;
    private String facturasProfit;
    private String docNumPedidoProfit;

    // Estado general sintetizado y diagnóstico legible
    private String estadoGeneral; // "REBOTADO_SIAD", "EN_COLA_SIAD", "PROCESADO_PROFIT", "SOLO_EN_PROFIT", "NO_ENCONTRADO"
    private String resumenDiagnostico;

    public RastreoPedidoDTO() {}

    public String getPedidoId() { return pedidoId; }
    public void setPedidoId(String pedidoId) { this.pedidoId = pedidoId; }

    public String getUbicacionArchivo() { return ubicacionArchivo; }
    public void setUbicacionArchivo(String ubicacionArchivo) { this.ubicacionArchivo = ubicacionArchivo; }

    public File getArchivoTxt() { return archivoTxt; }
    public void setArchivoTxt(File archivoTxt) { this.archivoTxt = archivoTxt; }

    public File getArchivoError() { return archivoError; }
    public void setArchivoError(File archivoError) { this.archivoError = archivoError; }

    public String getMensajeErrorSiad() { return mensajeErrorSiad; }
    public void setMensajeErrorSiad(String mensajeErrorSiad) { this.mensajeErrorSiad = mensajeErrorSiad; }

    public String getCoCli() { return coCli; }
    public void setCoCli(String coCli) { this.coCli = coCli; }

    public String getCliDes() { return cliDes; }
    public void setCliDes(String cliDes) { this.cliDes = cliDes; }

    public String getCoVen() { return coVen; }
    public void setCoVen(String coVen) { this.coVen = coVen; }

    public String getVenDes() { return venDes; }
    public void setVenDes(String venDes) { this.venDes = venDes; }

    public String getFechaEmisionTxt() { return fechaEmisionTxt; }
    public void setFechaEmisionTxt(String fechaEmisionTxt) { this.fechaEmisionTxt = fechaEmisionTxt; }

    public double getMontoNetoTxt() { return montoNetoTxt; }
    public void setMontoNetoTxt(double montoNetoTxt) { this.montoNetoTxt = montoNetoTxt; }

    public int getTotalRenglonesTxt() { return totalRenglonesTxt; }
    public void setTotalRenglonesTxt(int totalRenglonesTxt) { this.totalRenglonesTxt = totalRenglonesTxt; }

    public double getTotalUnidadesTxt() { return totalUnidadesTxt; }
    public void setTotalUnidadesTxt(double totalUnidadesTxt) { this.totalUnidadesTxt = totalUnidadesTxt; }

    public SiadDiagnosticoDTO getDiagnostico() { return diagnostico; }
    public void setDiagnostico(SiadDiagnosticoDTO diagnostico) { this.diagnostico = diagnostico; }

    public boolean isExisteEnProfit() { return existeEnProfit; }
    public void setExisteEnProfit(boolean existeEnProfit) { this.existeEnProfit = existeEnProfit; }

    public String getDocNumNota() { return docNumNota; }
    public void setDocNumNota(String docNumNota) { this.docNumNota = docNumNota; }

    public Timestamp getFecEmisNota() { return fecEmisNota; }
    public void setFecEmisNota(Timestamp fecEmisNota) { this.fecEmisNota = fecEmisNota; }

    public double getTotalNetoNota() { return totalNetoNota; }
    public void setTotalNetoNota(double totalNetoNota) { this.totalNetoNota = totalNetoNota; }

    public String getStatusNota() { return statusNota; }
    public void setStatusNota(String statusNota) { this.statusNota = statusNota; }

    public boolean isAnuladaNota() { return anuladaNota; }
    public void setAnuladaNota(boolean anuladaNota) { this.anuladaNota = anuladaNota; }

    public String getFacturasProfit() { return facturasProfit; }
    public void setFacturasProfit(String facturasProfit) { this.facturasProfit = facturasProfit; }

    public String getDocNumPedidoProfit() { return docNumPedidoProfit; }
    public void setDocNumPedidoProfit(String docNumPedidoProfit) { this.docNumPedidoProfit = docNumPedidoProfit; }

    public String getEstadoGeneral() { return estadoGeneral; }
    public void setEstadoGeneral(String estadoGeneral) { this.estadoGeneral = estadoGeneral; }

    public String getResumenDiagnostico() { return resumenDiagnostico; }
    public void setResumenDiagnostico(String resumenDiagnostico) { this.resumenDiagnostico = resumenDiagnostico; }
}

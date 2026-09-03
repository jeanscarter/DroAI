package com.droai.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Modelo de datos representativo de un Documento de Venta / Nota de Crédito (saDocumentoVenta) en Profit Plus.
 */
public class NotaCreditoModel {

    // ── Encabezado del documento ──
    private String nroDoc;
    private String coTipoDoc; // "N/CR", "FACT", "N/DB", etc.
    private String tipoDocDesc;
    private String tipoMov; // "CR" o "DE"
    private String estatus; // "Pendiente", "Cancelado", "Parcial", "Anulado"
    private LocalDateTime fecEmis;
    private LocalDate fecVenc;
    private LocalDate fecReg;
    private LocalDate fecCheque;
    private String nControl;
    private String descripcion; // observa
    private boolean impresa;
    private boolean anulado;

    // ── Datos del cliente ──
    private String coCli;
    private String cliDes;
    private String rif;
    private String direccion;
    private String telefonos;
    private String email;

    // ── Datos del vendedor ──
    private String coVen;
    private String venDes;

    // ── Moneda e impuestos ──
    private String coMone; // "USD", "BS", etc.
    private String moneDes;
    private double tasa;
    private String tipoImp; // "7" = Exentos, "1" = General, etc.
    private String tipoImpDesc;

    // ── Documento origen / afectado ──
    private String docOrig; // "FACT", "DEVO", etc. (Resuelto a la factura afectada para impresión oficial)
    private String nroOrig;
    private String docOrigRaw; // Documento original directo tal como está en saDocumentoVenta (ej: "DEVO")
    private String nroOrigRaw; // Número original directo (ej: número de devolución "0000001055")
    private LocalDateTime fecEmisOrig;
    private String nControlOrig;
    private double subtotalOrigBs;
    private double ivaOrigBs;
    private double totalOrigBs;

    // ── Montos en Moneda Base (Bolívares - Bs.) ──
    private double montoBrutoBs;
    private double porcDescGlob;
    private double montoDescBs;
    private double porcReca;
    private double montoRecaBs;
    private double totalSinImpuestoBs;
    private double baseImponibleBs;
    private double montoExentoBs;
    private double ivaBs;
    private double otrosBs;
    private double montoNetoBs;
    private double saldoBs;

    // ── Montos en Moneda Extranjera (USD) ──
    private double montoBrutoUsd;
    private double montoDescUsd;
    private double montoRecaUsd;
    private double totalSinImpuestoUsd;
    private double baseImponibleUsd;
    private double montoExentoUsd;
    private double ivaUsd;
    private double otrosUsd;
    private double montoNetoUsd;
    private double saldoUsd;

    // ── Banco / Cheque ──
    private String banco;
    private String nroCheque;
    private String movBanco;
    private String comprobIva;

    // ── Campos Adicionales y Auditoría ──
    private String campo1;
    private String campo2;
    private String campo3;
    private String campo4;
    private String campo5; // Habitualmente Nro de Nota de Crédito o correlativo fiscal
    private String campo6;
    private String campo7;
    private String campo8;
    private String coUsIn;
    private LocalDateTime feUsIn;
    private String coUsMo;
    private LocalDateTime feUsMo;
    private String coSucuIn;
    private String coSucuMo;

    // ── Constructor ──
    public NotaCreditoModel() {
        this.coTipoDoc = "N/CR";
        this.coMone = "USD";
        this.tasa = 1.0;
    }

    // ── Getters y Setters ──

    public String getNroDoc() { return nroDoc != null ? nroDoc.trim() : ""; }
    public void setNroDoc(String nroDoc) { this.nroDoc = nroDoc; }

    public String getCoTipoDoc() { return coTipoDoc != null ? coTipoDoc.trim() : ""; }
    public void setCoTipoDoc(String coTipoDoc) { this.coTipoDoc = coTipoDoc; }

    public String getTipoDocDesc() { return tipoDocDesc != null ? tipoDocDesc.trim() : ""; }
    public void setTipoDocDesc(String tipoDocDesc) { this.tipoDocDesc = tipoDocDesc; }

    public String getTipoMov() { return tipoMov != null ? tipoMov.trim() : "CR"; }
    public void setTipoMov(String tipoMov) { this.tipoMov = tipoMov; }

    public String getEstatus() { return estatus != null ? estatus.trim() : "Pendiente"; }
    public void setEstatus(String estatus) { this.estatus = estatus; }

    public LocalDateTime getFecEmis() { return fecEmis; }
    public void setFecEmis(LocalDateTime fecEmis) { this.fecEmis = fecEmis; }

    public LocalDate getFecVenc() { return fecVenc; }
    public void setFecVenc(LocalDate fecVenc) { this.fecVenc = fecVenc; }

    public LocalDate getFecReg() { return fecReg; }
    public void setFecReg(LocalDate fecReg) { this.fecReg = fecReg; }

    public LocalDate getFecCheque() { return fecCheque; }
    public void setFecCheque(LocalDate fecCheque) { this.fecCheque = fecCheque; }

    public String getNControl() { return nControl != null ? nControl.trim() : ""; }
    public void setNControl(String nControl) { this.nControl = nControl; }

    public String getDescripcion() { return descripcion != null ? descripcion.trim() : ""; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public boolean isImpresa() { return impresa; }
    public void setImpresa(boolean impresa) { this.impresa = impresa; }

    public boolean isAnulado() { return anulado; }
    public void setAnulado(boolean anulado) { this.anulado = anulado; }

    public String getCoCli() { return coCli != null ? coCli.trim() : ""; }
    public void setCoCli(String coCli) { this.coCli = coCli; }

    public String getCliDes() { return cliDes != null ? cliDes.trim() : ""; }
    public void setCliDes(String cliDes) { this.cliDes = cliDes; }

    public String getRif() { return rif != null ? rif.trim() : ""; }
    public void setRif(String rif) { this.rif = rif; }

    public String getDireccion() { return direccion != null ? direccion.trim() : ""; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getTelefonos() { return telefonos != null ? telefonos.trim() : ""; }
    public void setTelefonos(String telefonos) { this.telefonos = telefonos; }

    public String getEmail() { return email != null ? email.trim() : ""; }
    public void setEmail(String email) { this.email = email; }

    public String getCoVen() { return coVen != null ? coVen.trim() : ""; }
    public void setCoVen(String coVen) { this.coVen = coVen; }

    public String getVenDes() { return venDes != null ? venDes.trim() : ""; }
    public void setVenDes(String venDes) { this.venDes = venDes; }

    public String getCoMone() { return coMone != null ? coMone.trim() : "USD"; }
    public void setCoMone(String coMone) { this.coMone = coMone; }

    public String getMoneDes() { return moneDes != null ? moneDes.trim() : ""; }
    public void setMoneDes(String moneDes) { this.moneDes = moneDes; }

    public double getTasa() { return tasa > 0 ? tasa : 1.0; }
    public void setTasa(double tasa) { this.tasa = tasa; }

    public String getTipoImp() { return tipoImp != null ? tipoImp.trim() : "7"; }
    public void setTipoImp(String tipoImp) { this.tipoImp = tipoImp; }

    public String getTipoImpDesc() { return tipoImpDesc != null ? tipoImpDesc.trim() : "Exentos"; }
    public void setTipoImpDesc(String tipoImpDesc) { this.tipoImpDesc = tipoImpDesc; }

    public String getDocOrig() { return docOrig != null ? docOrig.trim() : ""; }
    public void setDocOrig(String docOrig) { this.docOrig = docOrig; }

    public String getNroOrig() { return nroOrig != null ? nroOrig.trim() : ""; }
    public void setNroOrig(String nroOrig) { this.nroOrig = nroOrig; }

    public String getDocOrigRaw() { return docOrigRaw != null ? docOrigRaw.trim() : ""; }
    public void setDocOrigRaw(String docOrigRaw) { this.docOrigRaw = docOrigRaw; }

    public String getNroOrigRaw() { return nroOrigRaw != null ? nroOrigRaw.trim() : ""; }
    public void setNroOrigRaw(String nroOrigRaw) { this.nroOrigRaw = nroOrigRaw; }

    public boolean esOrigenDevolucion() { return "DEVO".equalsIgnoreCase(docOrigRaw); }

    public LocalDateTime getFecEmisOrig() { return fecEmisOrig; }
    public void setFecEmisOrig(LocalDateTime fecEmisOrig) { this.fecEmisOrig = fecEmisOrig; }

    public String getNControlOrig() { return nControlOrig != null ? nControlOrig.trim() : ""; }
    public void setNControlOrig(String nControlOrig) { this.nControlOrig = nControlOrig; }

    public double getSubtotalOrigBs() { return subtotalOrigBs; }
    public void setSubtotalOrigBs(double subtotalOrigBs) { this.subtotalOrigBs = subtotalOrigBs; }

    public double getIvaOrigBs() { return ivaOrigBs; }
    public void setIvaOrigBs(double ivaOrigBs) { this.ivaOrigBs = ivaOrigBs; }

    public double getTotalOrigBs() { return totalOrigBs; }
    public void setTotalOrigBs(double totalOrigBs) { this.totalOrigBs = totalOrigBs; }

    public double getMontoBrutoBs() { return montoBrutoBs; }
    public void setMontoBrutoBs(double montoBrutoBs) { this.montoBrutoBs = montoBrutoBs; }

    public double getPorcDescGlob() { return porcDescGlob; }
    public void setPorcDescGlob(double porcDescGlob) { this.porcDescGlob = porcDescGlob; }

    public double getMontoDescBs() { return montoDescBs; }
    public void setMontoDescBs(double montoDescBs) { this.montoDescBs = montoDescBs; }

    public double getPorcReca() { return porcReca; }
    public void setPorcReca(double porcReca) { this.porcReca = porcReca; }

    public double getMontoRecaBs() { return montoRecaBs; }
    public void setMontoRecaBs(double montoRecaBs) { this.montoRecaBs = montoRecaBs; }

    public double getTotalSinImpuestoBs() { return totalSinImpuestoBs; }
    public void setTotalSinImpuestoBs(double totalSinImpuestoBs) { this.totalSinImpuestoBs = totalSinImpuestoBs; }

    public double getBaseImponibleBs() { return baseImponibleBs; }
    public void setBaseImponibleBs(double baseImponibleBs) { this.baseImponibleBs = baseImponibleBs; }

    public double getMontoExentoBs() { return montoExentoBs; }
    public void setMontoExentoBs(double montoExentoBs) { this.montoExentoBs = montoExentoBs; }

    public double getIvaBs() { return ivaBs; }
    public void setIvaBs(double ivaBs) { this.ivaBs = ivaBs; }

    public double getOtrosBs() { return otrosBs; }
    public void setOtrosBs(double otrosBs) { this.otrosBs = otrosBs; }

    public double getMontoNetoBs() { return montoNetoBs; }
    public void setMontoNetoBs(double montoNetoBs) { this.montoNetoBs = montoNetoBs; }

    public double getSaldoBs() { return saldoBs; }
    public void setSaldoBs(double saldoBs) { this.saldoBs = saldoBs; }

    public double getMontoBrutoUsd() { return montoBrutoUsd; }
    public void setMontoBrutoUsd(double montoBrutoUsd) { this.montoBrutoUsd = montoBrutoUsd; }

    public double getMontoDescUsd() { return montoDescUsd; }
    public void setMontoDescUsd(double montoDescUsd) { this.montoDescUsd = montoDescUsd; }

    public double getMontoRecaUsd() { return montoRecaUsd; }
    public void setMontoRecaUsd(double montoRecaUsd) { this.montoRecaUsd = montoRecaUsd; }

    public double getTotalSinImpuestoUsd() { return totalSinImpuestoUsd; }
    public void setTotalSinImpuestoUsd(double totalSinImpuestoUsd) { this.totalSinImpuestoUsd = totalSinImpuestoUsd; }

    public double getBaseImponibleUsd() { return baseImponibleUsd; }
    public void setBaseImponibleUsd(double baseImponibleUsd) { this.baseImponibleUsd = baseImponibleUsd; }

    public double getMontoExentoUsd() { return montoExentoUsd; }
    public void setMontoExentoUsd(double montoExentoUsd) { this.montoExentoUsd = montoExentoUsd; }

    public double getIvaUsd() { return ivaUsd; }
    public void setIvaUsd(double ivaUsd) { this.ivaUsd = ivaUsd; }

    public double getOtrosUsd() { return otrosUsd; }
    public void setOtrosUsd(double otrosUsd) { this.otrosUsd = otrosUsd; }

    public double getMontoNetoUsd() { return montoNetoUsd; }
    public void setMontoNetoUsd(double montoNetoUsd) { this.montoNetoUsd = montoNetoUsd; }

    public double getSaldoUsd() { return saldoUsd; }
    public void setSaldoUsd(double saldoUsd) { this.saldoUsd = saldoUsd; }

    public String getBanco() { return banco != null ? banco.trim() : ""; }
    public void setBanco(String banco) { this.banco = banco; }

    public String getNroCheque() { return nroCheque != null ? nroCheque.trim() : ""; }
    public void setNroCheque(String nroCheque) { this.nroCheque = nroCheque; }

    public String getMovBanco() { return movBanco != null ? movBanco.trim() : ""; }
    public void setMovBanco(String movBanco) { this.movBanco = movBanco; }

    public String getComprobIva() { return comprobIva != null ? comprobIva.trim() : ""; }
    public void setComprobIva(String comprobIva) { this.comprobIva = comprobIva; }

    public String getCampo1() { return campo1 != null ? campo1.trim() : ""; }
    public void setCampo1(String campo1) { this.campo1 = campo1; }

    public String getCampo2() { return campo2 != null ? campo2.trim() : ""; }
    public void setCampo2(String campo2) { this.campo2 = campo2; }

    public String getCampo3() { return campo3 != null ? campo3.trim() : ""; }
    public void setCampo3(String campo3) { this.campo3 = campo3; }

    public String getCampo4() { return campo4 != null ? campo4.trim() : ""; }
    public void setCampo4(String campo4) { this.campo4 = campo4; }

    public String getCampo5() { return campo5 != null ? campo5.trim() : ""; }
    public void setCampo5(String campo5) { this.campo5 = campo5; }

    public String getCampo6() { return campo6 != null ? campo6.trim() : ""; }
    public void setCampo6(String campo6) { this.campo6 = campo6; }

    public String getCampo7() { return campo7 != null ? campo7.trim() : ""; }
    public void setCampo7(String campo7) { this.campo7 = campo7; }

    public String getCampo8() { return campo8 != null ? campo8.trim() : ""; }
    public void setCampo8(String campo8) { this.campo8 = campo8; }

    public String getCoUsIn() { return coUsIn != null ? coUsIn.trim() : ""; }
    public void setCoUsIn(String coUsIn) { this.coUsIn = coUsIn; }

    public LocalDateTime getFeUsIn() { return feUsIn; }
    public void setFeUsIn(LocalDateTime feUsIn) { this.feUsIn = feUsIn; }

    public String getCoUsMo() { return coUsMo != null ? coUsMo.trim() : ""; }
    public void setCoUsMo(String coUsMo) { this.coUsMo = coUsMo; }

    public LocalDateTime getFeUsMo() { return feUsMo; }
    public void setFeUsMo(LocalDateTime feUsMo) { this.feUsMo = feUsMo; }

    public String getCoSucuIn() { return coSucuIn != null ? coSucuIn.trim() : ""; }
    public void setCoSucuIn(String coSucuIn) { this.coSucuIn = coSucuIn; }

    public String getCoSucuMo() { return coSucuMo != null ? coSucuMo.trim() : ""; }
    public void setCoSucuMo(String coSucuMo) { this.coSucuMo = coSucuMo; }

    /**
     * Retorna el número que debe mostrarse como correlativo de la Nota de Crédito en la impresión.
     * Si campo5 tiene un valor (ej: "00001354"), se usa este. De lo contrario se usa nroDoc.
     */
    public String getNumeroImpresion() {
        if (campo5 != null && !campo5.isBlank()) {
            return campo5.trim();
        }
        return getNroDoc();
    }
}

package com.droai.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO para almacenar el diagnóstico integral de un pedido rebotado en SIAD.
 */
public class SiadDiagnosticoDTO {

    private String pedidoId;
    private String coCli;
    private String cliDes;
    private String fechaEmision;
    private String errorOriginal;
    private File archivoTxt;
    private File archivoError;
    private List<RenglonDiagnosticoDTO> renglones = new ArrayList<>();

    public SiadDiagnosticoDTO() {}

    public String getPedidoId() { return pedidoId; }
    public void setPedidoId(String pedidoId) { this.pedidoId = pedidoId; }

    public String getCoCli() { return coCli; }
    public void setCoCli(String coCli) { this.coCli = coCli; }

    public String getCliDes() { return cliDes; }
    public void setCliDes(String cliDes) { this.cliDes = cliDes; }

    public String getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(String fechaEmision) { this.fechaEmision = fechaEmision; }

    public String getErrorOriginal() { return errorOriginal; }
    public void setErrorOriginal(String errorOriginal) { this.errorOriginal = errorOriginal; }

    public File getArchivoTxt() { return archivoTxt; }
    public void setArchivoTxt(File archivoTxt) { this.archivoTxt = archivoTxt; }

    public File getArchivoError() { return archivoError; }
    public void setArchivoError(File archivoError) { this.archivoError = archivoError; }

    public List<RenglonDiagnosticoDTO> getRenglones() { return renglones; }
    public void setRenglones(List<RenglonDiagnosticoDTO> renglones) { this.renglones = renglones; }

    public boolean tieneErrores() {
        if (renglones == null) return false;
        for (RenglonDiagnosticoDTO r : renglones) {
            if (r.isTieneError()) return true;
        }
        return false;
    }

    public int getCantidadErrores() {
        if (renglones == null) return 0;
        int count = 0;
        for (RenglonDiagnosticoDTO r : renglones) {
            if (r.isTieneError()) count++;
        }
        return count;
    }

    /**
     * Detalle a nivel de renglón con análisis contra Profit Plus.
     */
    public static class RenglonDiagnosticoDTO {
        private int rengNum;
        private String coArt;
        private String artDes;
        private double cantSolicitada;
        private String loteSolicitado;
        private double stockAlmacen;
        private double stockLoteSolicitado;
        private boolean tieneError;
        private String tipoError; // "STOCK_INSUFICIENTE", "LOTE_AGOTADO_HAY_FEFO", "OK"
        private String mensajeError;
        private List<LoteFEFODTO> lotesDisponibles = new ArrayList<>();

        public RenglonDiagnosticoDTO() {}

        public int getRengNum() { return rengNum; }
        public void setRengNum(int rengNum) { this.rengNum = rengNum; }

        public String getCoArt() { return coArt; }
        public void setCoArt(String coArt) { this.coArt = coArt; }

        public String getArtDes() { return artDes; }
        public void setArtDes(String artDes) { this.artDes = artDes; }

        public double getCantSolicitada() { return cantSolicitada; }
        public void setCantSolicitada(double cantSolicitada) { this.cantSolicitada = cantSolicitada; }

        public String getLoteSolicitado() { return loteSolicitado; }
        public void setLoteSolicitado(String loteSolicitado) { this.loteSolicitado = loteSolicitado; }

        public double getStockAlmacen() { return stockAlmacen; }
        public void setStockAlmacen(double stockAlmacen) { this.stockAlmacen = stockAlmacen; }

        public double getStockLoteSolicitado() { return stockLoteSolicitado; }
        public void setStockLoteSolicitado(double stockLoteSolicitado) { this.stockLoteSolicitado = stockLoteSolicitado; }

        public boolean isTieneError() { return tieneError; }
        public void setTieneError(boolean tieneError) { this.tieneError = tieneError; }

        public String getTipoError() { return tipoError; }
        public void setTipoError(String tipoError) { this.tipoError = tipoError; }

        public String getMensajeError() { return mensajeError; }
        public void setMensajeError(String mensajeError) { this.mensajeError = mensajeError; }

        public List<LoteFEFODTO> getLotesDisponibles() { return lotesDisponibles; }
        public void setLotesDisponibles(List<LoteFEFODTO> lotesDisponibles) { this.lotesDisponibles = lotesDisponibles; }

        public LoteFEFODTO getMejorLoteFEFO() {
            if (lotesDisponibles == null || lotesDisponibles.isEmpty()) return null;
            for (LoteFEFODTO l : lotesDisponibles) {
                if (l.getCantDisponible() >= cantSolicitada) {
                    return l;
                }
            }
            return lotesDisponibles.get(0);
        }
    }

    /**
     * Información de lote alternativo con vencimiento.
     */
    public static class LoteFEFODTO {
        private String numeroLote;
        private double cantDisponible;
        private String fechaVencimiento;

        public LoteFEFODTO(String numeroLote, double cantDisponible, String fechaVencimiento) {
            this.numeroLote = numeroLote;
            this.cantDisponible = cantDisponible;
            this.fechaVencimiento = fechaVencimiento;
        }

        public String getNumeroLote() { return numeroLote; }
        public double getCantDisponible() { return cantDisponible; }
        public String getFechaVencimiento() { return fechaVencimiento; }

        @Override
        public String toString() {
            return "Lote " + numeroLote + " (" + cantDisponible + " uds, vence " + (fechaVencimiento != null ? fechaVencimiento : "S/F") + ")";
        }
    }
}

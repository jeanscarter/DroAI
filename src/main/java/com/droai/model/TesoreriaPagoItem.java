package com.droai.model;

import java.time.LocalDate;

/**
 * Representa un renglón de pago extraído del arqueo de tesorería (Excel de Bancos y Pagos)
 * con su clasificación y destino para Profit Plus.
 */
public class TesoreriaPagoItem {

    public enum DestinoProfit {
        MOVIMIENTO_BANCO("Movimiento de Banco (Gasto Directo)"),
        ORDEN_PAGO("Orden de Pago (Proveedor / Factura)"),
        TRASPASO("Traspaso entre Cuentas Propias"),
        OMITIR("Omitir / No Procesar");

        private final String label;
        DestinoProfit(String label) { this.label = label; }
        public String getLabel() { return label; }
        @Override public String toString() { return label; }
    }

    public enum EstadoValidacion {
        PENDIENTE("Pendiente"),
        NUEVO("Listo (Nuevo)"),
        YA_EXISTE("Ya existe en Profit"),
        PROCESADO("Procesado OK"),
        ERROR("Error");

        private final String label;
        EstadoValidacion(String label) { this.label = label; }
        public String getLabel() { return label; }
        @Override public String toString() { return label; }
    }

    private boolean seleccionado = true;
    private int filaExcel;
    private LocalDate fecha;
    private String bancoNombreOriginal = "";
    private String codCta = "0134"; // Por defecto Banesco
    private String referencia = "";
    private String descripcion = "";
    private double montoBs;
    private double montoUSD;
    private String tipoPagoOriginal = "TRANSFERENCIA";
    private DestinoProfit destinoProfit = DestinoProfit.MOVIMIENTO_BANCO;
    private String codConcepto = "000018"; // Gastos varios por defecto
    private String descConcepto = "GASTOS VARIOS";
    private String codBeneficiario = "";
    private String descBeneficiario = "";
    private EstadoValidacion estadoValidacion = EstadoValidacion.PENDIENTE;
    private String mensajeValidacion = "";

    public TesoreriaPagoItem() {}

    // Getters y Setters
    public boolean isSeleccionado() { return seleccionado; }
    public void setSeleccionado(boolean seleccionado) { this.seleccionado = seleccionado; }

    public int getFilaExcel() { return filaExcel; }
    public void setFilaExcel(int filaExcel) { this.filaExcel = filaExcel; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public String getBancoNombreOriginal() { return bancoNombreOriginal; }
    public void setBancoNombreOriginal(String bancoNombreOriginal) { this.bancoNombreOriginal = bancoNombreOriginal; }

    public String getCodCta() { return codCta; }
    public void setCodCta(String codCta) { this.codCta = codCta; }

    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public double getMontoBs() { return montoBs; }
    public void setMontoBs(double montoBs) { this.montoBs = montoBs; }

    public double getMontoUSD() { return montoUSD; }
    public void setMontoUSD(double montoUSD) { this.montoUSD = montoUSD; }

    public String getTipoPagoOriginal() { return tipoPagoOriginal; }
    public void setTipoPagoOriginal(String tipoPagoOriginal) { this.tipoPagoOriginal = tipoPagoOriginal; }

    public DestinoProfit getDestinoProfit() { return destinoProfit; }
    public void setDestinoProfit(DestinoProfit destinoProfit) { this.destinoProfit = destinoProfit; }

    public String getCodConcepto() { return codConcepto; }
    public void setCodConcepto(String codConcepto) { this.codConcepto = codConcepto; }

    public String getDescConcepto() { return descConcepto; }
    public void setDescConcepto(String descConcepto) { this.descConcepto = descConcepto; }

    public String getCodBeneficiario() { return codBeneficiario; }
    public void setCodBeneficiario(String codBeneficiario) { this.codBeneficiario = codBeneficiario; }

    public String getDescBeneficiario() { return descBeneficiario; }
    public void setDescBeneficiario(String descBeneficiario) { this.descBeneficiario = descBeneficiario; }

    public EstadoValidacion getEstadoValidacion() { return estadoValidacion; }
    public void setEstadoValidacion(EstadoValidacion estadoValidacion) { this.estadoValidacion = estadoValidacion; }

    public String getMensajeValidacion() { return mensajeValidacion; }
    public void setMensajeValidacion(String mensajeValidacion) { this.mensajeValidacion = mensajeValidacion; }
}

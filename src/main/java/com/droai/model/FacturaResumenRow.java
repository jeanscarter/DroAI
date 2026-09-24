package com.droai.model;

/**
 * Modelo de resumen de factura (una fila por documento).
 *
 * <p>Agrupa los renglones de {@link MatrizVentasRow} por {@code doc_num},
 * sumando montos, IVA y totales para mostrar en la pestaña "Facturas"
 * del diálogo de detalle del Monitor Situacional.
 */
public class FacturaResumenRow {

    private String numero;
    private String fecha;
    private String codCliente;
    private String nombreCliente;
    private double montoBruto;
    private double descPct;
    private double descuento;
    private double montoNeto;
    private double iva;
    private double totalGeneral;
    private String codVen;
    private String nombreVendedor;
    private double tasa;

    public FacturaResumenRow() {}

    // ── Getters ──

    public String getNumero()          { return numero; }
    public String getFecha()           { return fecha; }
    public String getCodCliente()      { return codCliente; }
    public String getNombreCliente()   { return nombreCliente; }
    public double getMontoBruto()      { return montoBruto; }
    public double getDescPct()         { return descPct; }
    public double getDescuento()       { return descuento; }
    public double getMontoNeto()       { return montoNeto; }
    public double getIva()             { return iva; }
    public double getTotalGeneral()    { return totalGeneral; }
    public String getCodVen()          { return codVen; }
    public String getNombreVendedor()  { return nombreVendedor; }
    public double getTasa()            { return tasa; }

    // ── Setters ──

    public void setNumero(String numero)                { this.numero = numero; }
    public void setFecha(String fecha)                  { this.fecha = fecha; }
    public void setCodCliente(String codCliente)        { this.codCliente = codCliente; }
    public void setNombreCliente(String nombreCliente)  { this.nombreCliente = nombreCliente; }
    public void setMontoBruto(double montoBruto)        { this.montoBruto = montoBruto; }
    public void setDescPct(double descPct)              { this.descPct = descPct; }
    public void setDescuento(double descuento)          { this.descuento = descuento; }
    public void setMontoNeto(double montoNeto)          { this.montoNeto = montoNeto; }
    public void setIva(double iva)                      { this.iva = iva; }
    public void setTotalGeneral(double totalGeneral)    { this.totalGeneral = totalGeneral; }
    public void setCodVen(String codVen)                { this.codVen = codVen; }
    public void setNombreVendedor(String nombreVendedor){ this.nombreVendedor = nombreVendedor; }
    public void setTasa(double tasa)                    { this.tasa = tasa; }
}

package com.droai.model;

/**
 * Modelo de datos para la pestaña "Descuentos Adicional".
 * Mapea los campos de saArticulo relacionados al Descuento Adicional (DA):
 * lic_grado_al (porcentaje DA), volumen, fdesdesc, fhasdesc, fvendesc.
 */
public class DescuentoAdicionalRow {

    private String codigo;
    private String descripcion;
    private String marca;           // prov_des del proveedor principal
    private String modelo;
    private String codigoBarra;     // ref de saArticulo
    private double precio1;         // precio nivel 1
    private double descuentoDA;     // lic_grado_al de saArticulo (Descuento Adicional)
    private double volumen;         // volumen de saArticulo (Descuento por Volumen base)

    // Campos de vigencia por fechas y auditoría de usuario en saArticulo
    private String fechaDesde;      // fdesdesc
    private String fechaHasta;      // fhasdesc
    private String fechaVence;      // fvendesc
    private String coUsIn;
    private String feUsIn;
    private String coUsMo;
    private String feUsMo;

    // Campos auxiliares para filtrado (no visibles en la tabla)
    private String codProveedor;
    private String nombreProveedor;
    private String codLinea;
    private String linea;

    public DescuentoAdicionalRow() {}

    // --- Getters y Setters ---

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public String getCodigoBarra() { return codigoBarra; }
    public void setCodigoBarra(String codigoBarra) { this.codigoBarra = codigoBarra; }

    public double getPrecio1() { return precio1; }
    public void setPrecio1(double precio1) { this.precio1 = precio1; }

    public double getDescuentoDA() { return descuentoDA; }
    public void setDescuentoDA(double descuentoDA) { this.descuentoDA = descuentoDA; }

    public double getVolumen() { return volumen; }
    public void setVolumen(double volumen) { this.volumen = volumen; }

    public String getFechaDesde() { return fechaDesde; }
    public void setFechaDesde(String fechaDesde) { this.fechaDesde = fechaDesde; }

    public String getFechaHasta() { return fechaHasta; }
    public void setFechaHasta(String fechaHasta) { this.fechaHasta = fechaHasta; }

    public String getFechaVence() { return fechaVence; }
    public void setFechaVence(String fechaVence) { this.fechaVence = fechaVence; }

    public String getCoUsIn() { return coUsIn; }
    public void setCoUsIn(String coUsIn) { this.coUsIn = coUsIn; }

    public String getFeUsIn() { return feUsIn; }
    public void setFeUsIn(String feUsIn) { this.feUsIn = feUsIn; }

    public String getCoUsMo() { return coUsMo; }
    public void setCoUsMo(String coUsMo) { this.coUsMo = coUsMo; }

    public String getFeUsMo() { return feUsMo; }
    public void setFeUsMo(String feUsMo) { this.feUsMo = feUsMo; }

    public String getCodProveedor() { return codProveedor; }
    public void setCodProveedor(String codProveedor) { this.codProveedor = codProveedor; }

    public String getNombreProveedor() { return nombreProveedor; }
    public void setNombreProveedor(String nombreProveedor) { this.nombreProveedor = nombreProveedor; }

    public String getCodLinea() { return codLinea; }
    public void setCodLinea(String codLinea) { this.codLinea = codLinea; }

    public String getLinea() { return linea; }
    public void setLinea(String linea) { this.linea = linea; }
}

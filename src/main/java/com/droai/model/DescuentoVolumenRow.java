package com.droai.model;

/**
 * Modelo de datos para la pestaña "Descuentos x Volumen".
 * Cada fila representa un producto con su descuento DV (primer rango de saDescArticulo).
 */
public class DescuentoVolumenRow {

    private String codigo;
    private String descripcion;
    private String marca;           // prov_des del proveedor principal
    private String codigoBarra;     // ref de saArticulo
    private double precio1;         // precio nivel 1
    private double descuentoDV;     // porc1 de saDescArticulo (primer rango)

    // Campos auxiliares para filtrado (no visibles en la tabla)
    private String codProveedor;
    private String nombreProveedor;
    private String codLinea;
    private String linea;

    public DescuentoVolumenRow() {}

    // --- Getters y Setters ---

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public String getCodigoBarra() { return codigoBarra; }
    public void setCodigoBarra(String codigoBarra) { this.codigoBarra = codigoBarra; }

    public double getPrecio1() { return precio1; }
    public void setPrecio1(double precio1) { this.precio1 = precio1; }

    public double getDescuentoDV() { return descuentoDV; }
    public void setDescuentoDV(double descuentoDV) { this.descuentoDV = descuentoDV; }

    public String getCodProveedor() { return codProveedor; }
    public void setCodProveedor(String codProveedor) { this.codProveedor = codProveedor; }

    public String getNombreProveedor() { return nombreProveedor; }
    public void setNombreProveedor(String nombreProveedor) { this.nombreProveedor = nombreProveedor; }

    public String getCodLinea() { return codLinea; }
    public void setCodLinea(String codLinea) { this.codLinea = codLinea; }

    public String getLinea() { return linea; }
    public void setLinea(String linea) { this.linea = linea; }
}

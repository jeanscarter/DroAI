package com.droai.model;

/**
 * Modelo de datos para la pestaña "Descuento x Producto".
 * Cada fila representa un producto con sus descuentos (porc1/porc2 de saDescArticulo).
 */
public class DescuentoProductoRow {

    private String codigo;
    private String codigoBarra;
    private String descripcion;
    private String principioActivo;
    private String marca;
    private double costoFabrica;
    private double arancelPct;
    private double costoActual;
    private double precio1;
    private double utilidadPct;
    private double dctoPct;
    private double dctoPct2;
    private double precioDcto;
    private String fechaDesde;
    private String fechaHasta;

    // Campos auxiliares para filtrado (no visibles en la tabla)
    private String codLinea;
    private String linea;
    private String codProveedor;
    private String nombreProveedor;

    public DescuentoProductoRow() {}

    // --- Getters y Setters ---

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getCodigoBarra() { return codigoBarra; }
    public void setCodigoBarra(String codigoBarra) { this.codigoBarra = codigoBarra; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getPrincipioActivo() { return principioActivo; }
    public void setPrincipioActivo(String principioActivo) { this.principioActivo = principioActivo; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public double getCostoFabrica() { return costoFabrica; }
    public void setCostoFabrica(double costoFabrica) { this.costoFabrica = costoFabrica; }

    public double getArancelPct() { return arancelPct; }
    public void setArancelPct(double arancelPct) { this.arancelPct = arancelPct; }

    public double getCostoActual() { return costoActual; }
    public void setCostoActual(double costoActual) { this.costoActual = costoActual; }

    public double getPrecio1() { return precio1; }
    public void setPrecio1(double precio1) { this.precio1 = precio1; }

    public double getUtilidadPct() { return utilidadPct; }
    public void setUtilidadPct(double utilidadPct) { this.utilidadPct = utilidadPct; }

    public double getDctoPct() { return dctoPct; }
    public void setDctoPct(double dctoPct) { this.dctoPct = dctoPct; }

    public double getDctoPct2() { return dctoPct2; }
    public void setDctoPct2(double dctoPct2) { this.dctoPct2 = dctoPct2; }

    public double getPrecioDcto() { return precioDcto; }
    public void setPrecioDcto(double precioDcto) { this.precioDcto = precioDcto; }

    public String getFechaDesde() { return fechaDesde; }
    public void setFechaDesde(String fechaDesde) { this.fechaDesde = fechaDesde; }

    public String getFechaHasta() { return fechaHasta; }
    public void setFechaHasta(String fechaHasta) { this.fechaHasta = fechaHasta; }

    public String getCodLinea() { return codLinea; }
    public void setCodLinea(String codLinea) { this.codLinea = codLinea; }

    public String getLinea() { return linea; }
    public void setLinea(String linea) { this.linea = linea; }

    public String getCodProveedor() { return codProveedor; }
    public void setCodProveedor(String codProveedor) { this.codProveedor = codProveedor; }

    public String getNombreProveedor() { return nombreProveedor; }
    public void setNombreProveedor(String nombreProveedor) { this.nombreProveedor = nombreProveedor; }
}

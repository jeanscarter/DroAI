package com.droai.model;

/**
 * Modelo de datos para el Catálogo de Productos (basado en saArticulo).
 * Contiene los campos visibles en la UI (12 columnas) y campos adicionales para exportación Excel.
 */
public class ArticuloRow {

    // === Campos visibles en la UI (12 columnas) ===
    private String codigo;
    private String descripcion;
    private String marca;          // mapeado desde prov_des (Proveedor)
    private double existencia;     // stock total
    private String udm;            // unidad de medida
    private double costoFabrica;
    private double arancelPct;
    private double costoOm;
    private double utilPct;        // calculado
    private double precio1;
    private double ivaPct;
    private double precioCiva;     // calculado: precio1 * (1 + ivaPct/100)

    // === Campos adicionales para exportación Excel ===
    private double precio2;
    private double precio3;
    private double precio4;
    private String codLinea;
    private String linea;
    private String codSub;
    private String subLinea;
    private String codProveedor;
    private String nombreProveedor;
    private String referencia;
    private String modelo;
    private String procedencia;
    private double peso;
    private double volumen;
    private String codigoBarra;
    private String ubicacion;
    private String campo1;
    private String campo2;
    private String campo3;
    private String campo4;
    private String campo5;
    private String campo6;

    // === Campos para Ficha Producto ===
    private double costoActual;
    private double costoPromedio;
    private boolean destacado;
    private boolean anulado;
    private double margenMin;
    private double margenMax;

    public ArticuloRow() {}

    // --- Getters y Setters ---

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public double getExistencia() { return existencia; }
    public void setExistencia(double existencia) { this.existencia = existencia; }

    public String getUdm() { return udm; }
    public void setUdm(String udm) { this.udm = udm; }

    public double getCostoFabrica() { return costoFabrica; }
    public void setCostoFabrica(double costoFabrica) { this.costoFabrica = costoFabrica; }

    public double getArancelPct() { return arancelPct; }
    public void setArancelPct(double arancelPct) { this.arancelPct = arancelPct; }

    public double getCostoOm() { return costoOm; }
    public void setCostoOm(double costoOm) { this.costoOm = costoOm; }

    public double getUtilPct() { return utilPct; }
    public void setUtilPct(double utilPct) { this.utilPct = utilPct; }

    public double getPrecio1() { return precio1; }
    public void setPrecio1(double precio1) { this.precio1 = precio1; }

    public double getIvaPct() { return ivaPct; }
    public void setIvaPct(double ivaPct) { this.ivaPct = ivaPct; }

    public double getPrecioCiva() { return precioCiva; }
    public void setPrecioCiva(double precioCiva) { this.precioCiva = precioCiva; }

    public double getPrecio2() { return precio2; }
    public void setPrecio2(double precio2) { this.precio2 = precio2; }

    public double getPrecio3() { return precio3; }
    public void setPrecio3(double precio3) { this.precio3 = precio3; }

    public String getCodLinea() { return codLinea; }
    public void setCodLinea(String codLinea) { this.codLinea = codLinea; }

    public String getLinea() { return linea; }
    public void setLinea(String linea) { this.linea = linea; }

    public String getCodSub() { return codSub; }
    public void setCodSub(String codSub) { this.codSub = codSub; }

    public String getSubLinea() { return subLinea; }
    public void setSubLinea(String subLinea) { this.subLinea = subLinea; }

    public String getCodProveedor() { return codProveedor; }
    public void setCodProveedor(String codProveedor) { this.codProveedor = codProveedor; }

    public String getNombreProveedor() { return nombreProveedor; }
    public void setNombreProveedor(String nombreProveedor) { this.nombreProveedor = nombreProveedor; }

    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public String getProcedencia() { return procedencia; }
    public void setProcedencia(String procedencia) { this.procedencia = procedencia; }

    public double getPeso() { return peso; }
    public void setPeso(double peso) { this.peso = peso; }

    public double getVolumen() { return volumen; }
    public void setVolumen(double volumen) { this.volumen = volumen; }

    public String getCodigoBarra() { return codigoBarra; }
    public void setCodigoBarra(String codigoBarra) { this.codigoBarra = codigoBarra; }

    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }

    public String getCampo1() { return campo1; }
    public void setCampo1(String campo1) { this.campo1 = campo1; }

    public String getCampo2() { return campo2; }
    public void setCampo2(String campo2) { this.campo2 = campo2; }

    public double getPrecio4() { return precio4; }
    public void setPrecio4(double precio4) { this.precio4 = precio4; }

    public String getCampo3() { return campo3; }
    public void setCampo3(String campo3) { this.campo3 = campo3; }

    public String getCampo4() { return campo4; }
    public void setCampo4(String campo4) { this.campo4 = campo4; }

    public String getCampo5() { return campo5; }
    public void setCampo5(String campo5) { this.campo5 = campo5; }

    public String getCampo6() { return campo6; }
    public void setCampo6(String campo6) { this.campo6 = campo6; }

    public double getCostoActual() { return costoActual; }
    public void setCostoActual(double costoActual) { this.costoActual = costoActual; }

    public double getCostoPromedio() { return costoPromedio; }
    public void setCostoPromedio(double costoPromedio) { this.costoPromedio = costoPromedio; }

    public boolean isDestacado() { return destacado; }
    public void setDestacado(boolean destacado) { this.destacado = destacado; }

    public boolean isAnulado() { return anulado; }
    public void setAnulado(boolean anulado) { this.anulado = anulado; }

    public double getMargenMin() { return margenMin; }
    public void setMargenMin(double margenMin) { this.margenMin = margenMin; }

    public double getMargenMax() { return margenMax; }
    public void setMargenMax(double margenMax) { this.margenMax = margenMax; }
}

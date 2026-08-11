package com.droai.model;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO con la información consolidada de un producto para Ajustes de Inventario:
 * Datos del artículo, Stock Total, Precio, Costo y desgloses por Almacén y Lote.
 */
public class AjusteProductoDTO {
    private String codigo;
    private String descripcion;
    private String marca;
    private String codigoBarra;
    private String udm;
    private double costoActual;
    private double precio1;
    private double ivaPct;
    private double precioCiva;
    private double stockTotal;

    private List<StockAlmacenRow> stockPorAlmacen = new ArrayList<>();
    private List<StockLoteRow> stockPorLote = new ArrayList<>();

    public AjusteProductoDTO() {}

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getCodigoBarra() {
        return codigoBarra;
    }

    public void setCodigoBarra(String codigoBarra) {
        this.codigoBarra = codigoBarra;
    }

    public String getUdm() {
        return udm;
    }

    public void setUdm(String udm) {
        this.udm = udm;
    }

    public double getCostoActual() {
        return costoActual;
    }

    public void setCostoActual(double costoActual) {
        this.costoActual = costoActual;
    }

    public double getPrecio1() {
        return precio1;
    }

    public void setPrecio1(double precio1) {
        this.precio1 = precio1;
    }

    public double getIvaPct() {
        return ivaPct;
    }

    public void setIvaPct(double ivaPct) {
        this.ivaPct = ivaPct;
    }

    public double getPrecioCiva() {
        return precioCiva;
    }

    public void setPrecioCiva(double precioCiva) {
        this.precioCiva = precioCiva;
    }

    public double getStockTotal() {
        return stockTotal;
    }

    public void setStockTotal(double stockTotal) {
        this.stockTotal = stockTotal;
    }

    public List<StockAlmacenRow> getStockPorAlmacen() {
        return stockPorAlmacen;
    }

    public void setStockPorAlmacen(List<StockAlmacenRow> stockPorAlmacen) {
        this.stockPorAlmacen = stockPorAlmacen;
    }

    public List<StockLoteRow> getStockPorLote() {
        return stockPorLote;
    }

    public void setStockPorLote(List<StockLoteRow> stockPorLote) {
        this.stockPorLote = stockPorLote;
    }
}

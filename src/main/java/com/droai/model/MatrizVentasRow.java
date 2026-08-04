package com.droai.model;

public class MatrizVentasRow {

    private String numero;
    private String fecha;
    private String ciRif;
    private String nombreRazonSocial;
    private String coVen;
    private String nombreVendedor;
    private double tasa;
    private String codigoArt;
    private String descripcion;
    private double cantidad;
    private double precio;
    private double dp;
    private double da;
    private double dct;
    private double dc;
    private double dv;
    private double descPct;
    private double totalRenglon;
    private double descPctGlobal;
    private double renglonDg;
    private double montoIva;
    private double totRenglonIva;
    private double costoVenta;
    private double totalCostoVenta;
    private double totCvDp;
    private double montoUtilidad;
    private double utilPct;
    private double costoActual;
    private double stockActual;
    private String codLinea;
    private String linea;
    private String codSub;
    private String subLinea;
    private String codProveedor;
    private String nombreProveedor;
    private String zona;
    private String almacen;
    private String pedidoWeb;
    private String origen;
    private String usuarioWeb;
    private String marca;
    private String udm;
    private double costoFabrica;
    private double arancelPct;
    private double costoOm;
    private double ivaPct;
    private double precioCiva;
    private String ciudad;
    private String codProv;

    public MatrizVentasRow() {
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getCiRif() {
        if (ciRif == null) return "";
        String trimmed = ciRif.trim();
        if (trimmed.matches("(?i)^[JVEGP]\\d+.*")) {
            return trimmed.substring(0, 1).toUpperCase() + "-" + trimmed.substring(1);
        }
        return trimmed;
    }

    public void setCiRif(String ciRif) {
        this.ciRif = ciRif;
    }

    public String getNombreRazonSocial() {
        return nombreRazonSocial;
    }

    public void setNombreRazonSocial(String nombreRazonSocial) {
        this.nombreRazonSocial = nombreRazonSocial;
    }

    public String getCoVen() {
        return coVen;
    }

    public void setCoVen(String coVen) {
        this.coVen = coVen;
    }

    public String getNombreVendedor() {
        return nombreVendedor;
    }

    public void setNombreVendedor(String nombreVendedor) {
        this.nombreVendedor = nombreVendedor;
    }

    public double getTasa() {
        return tasa;
    }

    public void setTasa(double tasa) {
        this.tasa = tasa;
    }

    public String getCodigoArt() {
        return codigoArt;
    }

    public void setCodigoArt(String codigoArt) {
        this.codigoArt = codigoArt;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public double getCantidad() {
        return cantidad;
    }

    public void setCantidad(double cantidad) {
        this.cantidad = cantidad;
    }

    public double getPrecio() {
        return precio;
    }

    public void setPrecio(double precio) {
        this.precio = precio;
    }

    public double getDp() {
        return dp;
    }

    public void setDp(double dp) {
        this.dp = dp;
    }

    public double getDct() {
        return dct;
    }

    public void setDct(double dct) {
        this.dct = dct;
    }

    public double getDa() {
        return da;
    }

    public void setDa(double da) {
        this.da = da;
    }

    public double getDc() {
        return dc;
    }

    public void setDc(double dc) {
        this.dc = dc;
    }

    public double getDv() {
        return dv;
    }

    public void setDv(double dv) {
        this.dv = dv;
    }

    public double getDescPct() {
        return descPct;
    }

    public void setDescPct(double descPct) {
        this.descPct = descPct;
    }

    public double getTotalRenglon() {
        return totalRenglon;
    }

    public void setTotalRenglon(double totalRenglon) {
        this.totalRenglon = totalRenglon;
    }

    public double getDescPctGlobal() {
        return descPctGlobal;
    }

    public void setDescPctGlobal(double descPctGlobal) {
        this.descPctGlobal = descPctGlobal;
    }

    public double getRenglonDg() {
        return renglonDg;
    }

    public void setRenglonDg(double renglonDg) {
        this.renglonDg = renglonDg;
    }

    public double getMontoIva() {
        return montoIva;
    }

    public void setMontoIva(double montoIva) {
        this.montoIva = montoIva;
    }

    public double getTotRenglonIva() {
        return totRenglonIva;
    }

    public void setTotRenglonIva(double totRenglonIva) {
        this.totRenglonIva = totRenglonIva;
    }

    public double getCostoVenta() {
        return costoVenta;
    }

    public void setCostoVenta(double costoVenta) {
        this.costoVenta = costoVenta;
    }

    public double getTotalCostoVenta() {
        return totalCostoVenta;
    }

    public void setTotalCostoVenta(double totalCostoVenta) {
        this.totalCostoVenta = totalCostoVenta;
    }

    public double getTotCvDp() {
        return totCvDp;
    }

    public void setTotCvDp(double totCvDp) {
        this.totCvDp = totCvDp;
    }

    public double getMontoUtilidad() {
        return montoUtilidad;
    }

    public void setMontoUtilidad(double montoUtilidad) {
        this.montoUtilidad = montoUtilidad;
    }

    public double getUtilPct() {
        return utilPct;
    }

    public void setUtilPct(double utilPct) {
        this.utilPct = utilPct;
    }

    public double getCostoActual() {
        return costoActual;
    }

    public void setCostoActual(double costoActual) {
        this.costoActual = costoActual;
    }

    public double getStockActual() {
        return stockActual;
    }

    public void setStockActual(double stockActual) {
        this.stockActual = stockActual;
    }

    public String getCodLinea() {
        return codLinea;
    }

    public void setCodLinea(String codLinea) {
        this.codLinea = codLinea;
    }

    public String getLinea() {
        return linea;
    }

    public void setLinea(String linea) {
        this.linea = linea;
    }

    public String getCodSub() {
        return codSub;
    }

    public void setCodSub(String codSub) {
        this.codSub = codSub;
    }

    public String getSubLinea() {
        return subLinea;
    }

    public void setSubLinea(String subLinea) {
        this.subLinea = subLinea;
    }

    public String getCodProveedor() {
        return codProveedor;
    }

    public void setCodProveedor(String codProveedor) {
        this.codProveedor = codProveedor;
    }

    public String getNombreProveedor() {
        return nombreProveedor;
    }

    public void setNombreProveedor(String nombreProveedor) {
        this.nombreProveedor = nombreProveedor;
    }

    public String getZona() {
        return zona;
    }

    public void setZona(String zona) {
        this.zona = zona;
    }

    public String getAlmacen() {
        return almacen;
    }

    public void setAlmacen(String almacen) {
        this.almacen = almacen;
    }

    public String getPedidoWeb() {
        return pedidoWeb;
    }

    public void setPedidoWeb(String pedidoWeb) {
        this.pedidoWeb = pedidoWeb;
    }

    public String getOrigen() {
        return origen;
    }

    public void setOrigen(String origen) {
        this.origen = origen;
    }

    public String getUsuarioWeb() {
        return usuarioWeb;
    }

    public void setUsuarioWeb(String usuarioWeb) {
        this.usuarioWeb = usuarioWeb;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getUdm() {
        return udm;
    }

    public void setUdm(String udm) {
        this.udm = udm;
    }

    public double getCostoFabrica() {
        return costoFabrica;
    }

    public void setCostoFabrica(double costoFabrica) {
        this.costoFabrica = costoFabrica;
    }

    public double getArancelPct() {
        return arancelPct;
    }

    public void setArancelPct(double arancelPct) {
        this.arancelPct = arancelPct;
    }

    public double getCostoOm() {
        return costoOm;
    }

    public void setCostoOm(double costoOm) {
        this.costoOm = costoOm;
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

    public String getCiudad() {
        return ciudad != null ? ciudad : "";
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public String getCodProv() {
        return codProv != null ? codProv : "";
    }

    public void setCodProv(String codProv) {
        this.codProv = codProv;
    }

    public String getMes() {
        if (fecha == null || fecha.isBlank()) return "";
        try {
            String[] months = {"ENERO", "FEBRERO", "MARZO", "ABRIL", "MAYO", "JUNIO",
                               "JULIO", "AGOSTO", "SEPTIEMBRE", "OCTUBRE", "NOVIEMBRE", "DICIEMBRE"};
            int m = -1;
            int y = -1;
            if (fecha.contains("-")) {
                String[] parts = fecha.split("-");
                if (parts.length >= 2) {
                    y = Integer.parseInt(parts[0].trim());
                    m = Integer.parseInt(parts[1].trim());
                }
            } else if (fecha.contains("/")) {
                String[] parts = fecha.split("/");
                if (parts.length >= 3) {
                    m = Integer.parseInt(parts[1].trim());
                    y = Integer.parseInt(parts[2].trim());
                }
            }
            if (m >= 1 && m <= 12) {
                return y > 0 ? months[m - 1] + " " + y : months[m - 1];
            }
        } catch (Exception ignored) {}
        return fecha;
    }
}
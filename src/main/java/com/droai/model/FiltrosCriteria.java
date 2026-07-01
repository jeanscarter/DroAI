package com.droai.model;

/**
 * POJO que encapsula todos los criterios de filtro del diálogo de Filtros.
 * Utilizado por el CatalogoTableModel para filtrar la tabla de productos.
 */
public class FiltrosCriteria {

    // ── Enums ──

    public enum FiltroCosto {
        TODOS, SIN_COSTO, CON_COSTO
    }

    public enum FiltroPrecio {
        TODOS, SIN_PRECIO, CON_PRECIO
    }

    public enum FiltroStock {
        TODOS, CON_STOCK, SIN_STOCK
    }

    // ── Campos de texto ──

    private String codigo = "";
    private String descripcion = "";
    private String referencia = "";
    private String codigoBarra = "";
    private String marca = "";
    private String modelo = "";
    private String ubicacion = "";
    private String campo1 = "";
    private String campo2 = "";

    // ── Combos / selección ──

    private String moneda = "";
    private String grupo = "";
    private String subGrupo = "";
    private String proveedor = "";
    private String almacen = "";

    // ── Checkboxes ──

    private boolean mostrarInactivos = false;
    private boolean cualquierPosicion = true;
    private boolean diferenteUbicacion = false;
    private boolean soloPrecioMenorCosto = false;

    // ── Radio buttons ──

    private FiltroCosto filtroCosto = FiltroCosto.TODOS;
    private FiltroPrecio filtroPrecio = FiltroPrecio.TODOS;
    private FiltroStock filtroStock = FiltroStock.TODOS;

    // ── Método utilitario ──

    /**
     * Indica si hay filtros activos (al menos un campo no vacío o un radio distinto de TODOS).
     */
    public boolean isEmpty() {
        return codigo.isEmpty()
                && descripcion.isEmpty()
                && referencia.isEmpty()
                && codigoBarra.isEmpty()
                && marca.isEmpty()
                && modelo.isEmpty()
                && ubicacion.isEmpty()
                && campo1.isEmpty()
                && campo2.isEmpty()
                && moneda.isEmpty()
                && grupo.isEmpty()
                && subGrupo.isEmpty()
                && proveedor.isEmpty()
                && almacen.isEmpty()
                && !mostrarInactivos
                && !diferenteUbicacion
                && !soloPrecioMenorCosto
                && filtroCosto == FiltroCosto.TODOS
                && filtroPrecio == FiltroPrecio.TODOS
                && filtroStock == FiltroStock.TODOS;
    }

    // ── Getters y Setters ──

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo != null ? codigo.trim() : ""; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion != null ? descripcion.trim() : ""; }

    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia != null ? referencia.trim() : ""; }

    public String getCodigoBarra() { return codigoBarra; }
    public void setCodigoBarra(String codigoBarra) { this.codigoBarra = codigoBarra != null ? codigoBarra.trim() : ""; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca != null ? marca.trim() : ""; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo != null ? modelo.trim() : ""; }

    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion != null ? ubicacion.trim() : ""; }

    public String getCampo1() { return campo1; }
    public void setCampo1(String campo1) { this.campo1 = campo1 != null ? campo1.trim() : ""; }

    public String getCampo2() { return campo2; }
    public void setCampo2(String campo2) { this.campo2 = campo2 != null ? campo2.trim() : ""; }

    public String getMoneda() { return moneda; }
    public void setMoneda(String moneda) { this.moneda = moneda != null ? moneda.trim() : ""; }

    public String getGrupo() { return grupo; }
    public void setGrupo(String grupo) { this.grupo = grupo != null ? grupo.trim() : ""; }

    public String getSubGrupo() { return subGrupo; }
    public void setSubGrupo(String subGrupo) { this.subGrupo = subGrupo != null ? subGrupo.trim() : ""; }

    public String getProveedor() { return proveedor; }
    public void setProveedor(String proveedor) { this.proveedor = proveedor != null ? proveedor.trim() : ""; }

    public String getAlmacen() { return almacen; }
    public void setAlmacen(String almacen) { this.almacen = almacen != null ? almacen.trim() : ""; }

    public boolean isMostrarInactivos() { return mostrarInactivos; }
    public void setMostrarInactivos(boolean mostrarInactivos) { this.mostrarInactivos = mostrarInactivos; }

    public boolean isCualquierPosicion() { return cualquierPosicion; }
    public void setCualquierPosicion(boolean cualquierPosicion) { this.cualquierPosicion = cualquierPosicion; }

    public boolean isDiferenteUbicacion() { return diferenteUbicacion; }
    public void setDiferenteUbicacion(boolean diferenteUbicacion) { this.diferenteUbicacion = diferenteUbicacion; }

    public boolean isSoloPrecioMenorCosto() { return soloPrecioMenorCosto; }
    public void setSoloPrecioMenorCosto(boolean soloPrecioMenorCosto) { this.soloPrecioMenorCosto = soloPrecioMenorCosto; }

    public FiltroCosto getFiltroCosto() { return filtroCosto; }
    public void setFiltroCosto(FiltroCosto filtroCosto) { this.filtroCosto = filtroCosto; }

    public FiltroPrecio getFiltroPrecio() { return filtroPrecio; }
    public void setFiltroPrecio(FiltroPrecio filtroPrecio) { this.filtroPrecio = filtroPrecio; }

    public FiltroStock getFiltroStock() { return filtroStock; }
    public void setFiltroStock(FiltroStock filtroStock) { this.filtroStock = filtroStock; }
}

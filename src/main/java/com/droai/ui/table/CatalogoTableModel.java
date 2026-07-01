package com.droai.ui.table;

import com.droai.model.ArticuloRow;
import com.droai.model.FiltrosCriteria;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * TableModel para el Catálogo de Productos.
 * Muestra columnas dinámicas en la UI con soporte para mostrar/ocultar
 * la columna "Existencia" mediante el checkbox "Ver existencia".
 * El modelo subyacente almacena todos los datos para la exportación.
 *
 * <p>Soporta filtrado avanzado vía {@link FiltrosCriteria} (diálogo Filtros)
 * combinado con el filtro de texto libre de la barra de búsqueda.
 */
public class CatalogoTableModel extends AbstractTableModel {

    /** Definición de todas las columnas posibles con su clave interna. */
    private static final String[][] ALL_COLUMNS = {
            {"Codigo", "codigo"},
            {"Descripcion", "descripcion"},
            {"Marca", "marca"},               // columna dinámica (idx 2)
            {"Existencia", "existencia"},      // togglable (idx 3)
            {"UdM", "udm"},
            {"Costo Fabrica", "costoFabrica"},
            {"Arancel%", "arancelPct"},
            {"Costo OM", "costoOm"},
            {"Util %", "utilPct"},
            {"Precio1", "precio1"},
            {"%IVA", "ivaPct"},
            {"Precio C/IVA", "precioCiva"}
    };

    private List<ArticuloRow> allData = new ArrayList<>();
    private List<ArticuloRow> filteredData = new ArrayList<>();
    private String filterText = "";
    private String columnaDinamicaActual = "Marca";
    private boolean showExistencia = true;
    private FiltrosCriteria filtrosCriteria = null;

    /** Índices de columnas activas basados en la visibilidad. */
    private List<Integer> visibleColumns = new ArrayList<>();

    public CatalogoTableModel() {
        rebuildVisibleColumns();
    }

    private void rebuildVisibleColumns() {
        visibleColumns.clear();
        for (int i = 0; i < ALL_COLUMNS.length; i++) {
            if (i == 3 && !showExistencia) continue; // "Existencia"
            visibleColumns.add(i);
        }
    }

    @Override
    public int getRowCount() {
        return filteredData.size();
    }

    @Override
    public int getColumnCount() {
        return visibleColumns.size();
    }

    @Override
    public String getColumnName(int viewCol) {
        int realCol = visibleColumns.get(viewCol);
        if (realCol == 2) return columnaDinamicaActual; // columna dinámica
        return ALL_COLUMNS[realCol][0];
    }

    @Override
    public Class<?> getColumnClass(int viewCol) {
        int realCol = visibleColumns.get(viewCol);
        return switch (realCol) {
            case 0, 1, 2, 4 -> String.class; // Codigo, Descripcion, Dinamica, UdM
            default -> Double.class;
        };
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return false;
    }

    @Override
    public Object getValueAt(int row, int viewCol) {
        ArticuloRow r = filteredData.get(row);
        int realCol = visibleColumns.get(viewCol);

        if (realCol == 2) {
            return switch (columnaDinamicaActual) {
                case "Referencia" -> r.getReferencia();
                case "Codigo de Barra" -> r.getCodigoBarra();
                case "Marca" -> r.getMarca();
                case "Modelo" -> r.getModelo();
                case "Ubicacion" -> r.getUbicacion();
                case "Campo 1" -> r.getCampo1();
                case "Campo 2" -> r.getCampo2();
                default -> r.getMarca();
            };
        }

        return switch (realCol) {
            case 0  -> r.getCodigo();
            case 1  -> r.getDescripcion();
            case 3  -> r.getExistencia();
            case 4  -> r.getUdm();
            case 5  -> r.getCostoFabrica();
            case 6  -> r.getArancelPct();
            case 7  -> r.getCostoOm();
            case 8  -> r.getUtilPct();
            case 9  -> r.getPrecio1();
            case 10 -> r.getIvaPct();
            case 11 -> r.getPrecioCiva();
            default -> null;
        };
    }

    // ========== Visibilidad de columnas ==========

    public void setShowExistencia(boolean show) {
        if (this.showExistencia != show) {
            this.showExistencia = show;
            rebuildVisibleColumns();
            fireTableStructureChanged();
        }
    }

    // ========== Data management ==========

    public void setData(List<ArticuloRow> data) {
        this.allData = new ArrayList<>(data);
        applyFilter();
    }

    public void setColumnaDinamica(String columna) {
        this.columnaDinamicaActual = columna;
        fireTableStructureChanged();
    }

    public void ordenarPor(String criterio) {
        java.util.Comparator<ArticuloRow> comparator;
        switch (criterio) {
            case "Codigo":
                comparator = java.util.Comparator.comparing(r -> r.getCodigo() != null ? r.getCodigo() : "");
                break;
            case "Descripcion":
                comparator = java.util.Comparator.comparing(r -> r.getDescripcion() != null ? r.getDescripcion() : "");
                break;
            case "Referencia":
                comparator = java.util.Comparator.comparing(r -> r.getReferencia() != null ? r.getReferencia() : "");
                break;
            case "Codigo de Barra":
                comparator = java.util.Comparator.comparing(r -> r.getCodigoBarra() != null ? r.getCodigoBarra() : "");
                break;
            case "Marca":
                comparator = java.util.Comparator.comparing(r -> r.getMarca() != null ? r.getMarca() : "");
                break;
            case "Modelo":
                comparator = java.util.Comparator.comparing(r -> r.getModelo() != null ? r.getModelo() : "");
                break;
            case "Ubicacion":
                comparator = java.util.Comparator.comparing(r -> r.getUbicacion() != null ? r.getUbicacion() : "");
                break;
            case "Campo 1":
                comparator = java.util.Comparator.comparing(r -> r.getCampo1() != null ? r.getCampo1() : "");
                break;
            case "Campo 2":
                comparator = java.util.Comparator.comparing(r -> r.getCampo2() != null ? r.getCampo2() : "");
                break;
            default:
                comparator = java.util.Comparator.comparing(r -> r.getCodigo() != null ? r.getCodigo() : "");
                break;
        }
        allData.sort(comparator);
        filteredData.sort(comparator);
        fireTableDataChanged();
    }

    public void setFilter(String text) {
        this.filterText = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        applyFilter();
    }

    // ========== Filtros avanzados (diálogo Filtros) ==========

    /**
     * Establece los criterios de filtrado avanzado y re-aplica el filtro.
     */
    public void setFiltrosCriteria(FiltrosCriteria criteria) {
        this.filtrosCriteria = criteria;
        applyFilter();
    }

    /**
     * Retorna los criterios de filtrado actuales (puede ser null).
     */
    public FiltrosCriteria getFiltrosCriteria() {
        return filtrosCriteria;
    }

    private void applyFilter() {
        // Empezar con todos los datos
        java.util.stream.Stream<ArticuloRow> stream = allData.stream();

        // 1. Aplicar filtros avanzados (FiltrosCriteria)
        if (filtrosCriteria != null && !filtrosCriteria.isEmpty()) {
            stream = stream.filter(this::matchesCriteria);
        }

        // 2. Aplicar filtro de texto libre (barra de búsqueda)
        if (!filterText.isEmpty()) {
            stream = stream.filter(r ->
                    matches(r.getCodigo())
                    || matches(r.getDescripcion())
                    || matches(r.getMarca())
                    || matches(r.getCodigoBarra())
                    || matches(r.getCodLinea()));
        }

        // 3. Filtrar inactivos (anulados) por defecto, a menos que el filtro lo permita
        if (filtrosCriteria == null || !filtrosCriteria.isMostrarInactivos()) {
            stream = stream.filter(r -> !r.isAnulado());
        }

        filteredData = stream.collect(Collectors.toList());
        fireTableDataChanged();
    }

    /**
     * Verifica si una fila cumple con todos los criterios avanzados de FiltrosCriteria.
     */
    private boolean matchesCriteria(ArticuloRow r) {
        FiltrosCriteria c = this.filtrosCriteria;

        // Campos de texto: búsqueda parcial (contains) o exacta según "cualquierPosicion"
        boolean anyPos = c.isCualquierPosicion();

        if (!c.getCodigo().isEmpty() && !matchField(r.getCodigo(), c.getCodigo(), anyPos)) return false;
        if (!c.getDescripcion().isEmpty() && !matchField(r.getDescripcion(), c.getDescripcion(), anyPos)) return false;
        if (!c.getReferencia().isEmpty() && !matchField(r.getReferencia(), c.getReferencia(), anyPos)) return false;
        if (!c.getCodigoBarra().isEmpty() && !matchField(r.getCodigoBarra(), c.getCodigoBarra(), anyPos)) return false;
        if (!c.getMarca().isEmpty() && !matchField(r.getMarca(), c.getMarca(), anyPos)) return false;
        if (!c.getModelo().isEmpty() && !matchField(r.getModelo(), c.getModelo(), anyPos)) return false;
        if (!c.getUbicacion().isEmpty()) {
            boolean matchUbic = matchField(r.getUbicacion(), c.getUbicacion(), anyPos);
            if (c.isDiferenteUbicacion()) {
                if (matchUbic) return false; // "Diferente" = excluir los que coinciden
            } else {
                if (!matchUbic) return false;
            }
        }
        if (!c.getCampo1().isEmpty() && !matchField(r.getCampo1(), c.getCampo1(), anyPos)) return false;
        if (!c.getCampo2().isEmpty() && !matchField(r.getCampo2(), c.getCampo2(), anyPos)) return false;

        // Proveedor
        if (!c.getProveedor().isEmpty()
                && !matchField(r.getNombreProveedor(), c.getProveedor(), anyPos)
                && !matchField(r.getCodProveedor(), c.getProveedor(), anyPos)) return false;

        // Grupo (Línea)
        if (!c.getGrupo().isEmpty()
                && !matchField(r.getLinea(), c.getGrupo(), anyPos)
                && !matchField(r.getCodLinea(), c.getGrupo(), anyPos)) return false;

        // SubGrupo (SubLínea)
        if (!c.getSubGrupo().isEmpty()
                && !matchField(r.getSubLinea(), c.getSubGrupo(), anyPos)
                && !matchField(r.getCodSub(), c.getSubGrupo(), anyPos)) return false;

        // Costo
        double costo = (r.getCostoActual() > 0) ? r.getCostoActual() : r.getCostoOm();
        switch (c.getFiltroCosto()) {
            case SIN_COSTO -> { if (costo > 0) return false; }
            case CON_COSTO -> { if (costo <= 0) return false; }
            default -> {} // TODOS: no filtrar
        }

        // Precio
        switch (c.getFiltroPrecio()) {
            case SIN_PRECIO -> { if (r.getPrecio1() > 0) return false; }
            case CON_PRECIO -> { if (r.getPrecio1() <= 0) return false; }
            default -> {}
        }

        // Solo Precio <= Costo
        if (c.isSoloPrecioMenorCosto()) {
            if (r.getPrecio1() > costo) return false;
        }

        // Stock
        switch (c.getFiltroStock()) {
            case CON_STOCK -> { if (r.getExistencia() <= 0) return false; }
            case SIN_STOCK -> { if (r.getExistencia() > 0) return false; }
            default -> {}
        }

        return true;
    }

    /**
     * Coincidencia de campo: si anyPos=true usa contains, si no usa startsWith.
     */
    private boolean matchField(String value, String filter, boolean anyPosition) {
        if (value == null) return false;
        String v = value.toLowerCase(Locale.ROOT);
        String f = filter.toLowerCase(Locale.ROOT);
        return anyPosition ? v.contains(f) : v.startsWith(f);
    }

    private boolean matches(String value) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(filterText);
    }

    public List<ArticuloRow> getFilteredData() {
        return filteredData;
    }

    public List<ArticuloRow> getAllData() {
        return allData;
    }

    public int getTotalCount() {
        return allData.size();
    }
}

package com.droai.ui.table;

import com.droai.model.DescuentoProductoRow;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class DescuentoProductoTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {
            "Seleccionar", "Código", "Código de Barra", "Descripción", "Principio Activo",
            "Marca", "Costo Fábrica", "Arancel %", "Costo Actual", "Utilidad %",
            "Precio 1", "% Dcto (DP)", "Precio Dcto", "Desde", "Hasta"
    };

    private List<DescuentoProductoRow> allData = new ArrayList<>();
    private List<DescuentoProductoRow> filteredData = new ArrayList<>();
    private final Map<String, Boolean> selectionMap = new HashMap<>();

    private String filterText = "";
    private String filterMarca = "";
    private com.droai.model.FiltrosCriteria filtrosCriteria = null;

    public void setFiltrosCriteria(com.droai.model.FiltrosCriteria criteria) {
        this.filtrosCriteria = criteria;
        applyFilter();
    }

    public com.droai.model.FiltrosCriteria getFiltrosCriteria() {
        return filtrosCriteria;
    }

    @Override
    public int getRowCount() {
        return filteredData.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int col) {
        return COLUMNS[col];
    }

    @Override
    public Class<?> getColumnClass(int col) {
        return switch (col) {
            case 0 -> Boolean.class;
            case 1, 2, 3, 4, 5, 13, 14 -> String.class;
            case 6, 7, 8, 9, 10, 11, 12 -> Double.class;
            default -> Object.class;
        };
    }

    private java.util.function.BiConsumer<String, Double> onCellDiscountEdited;

    public void setOnCellDiscountEdited(java.util.function.BiConsumer<String, Double> cb) {
        this.onCellDiscountEdited = cb;
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return col == 0 || col == 11;
    }

    @Override
    public Object getValueAt(int row, int col) {
        DescuentoProductoRow r = filteredData.get(row);
        return switch (col) {
            case 0  -> selectionMap.getOrDefault(r.getCodigo(), false);
            case 1  -> r.getCodigo();
            case 2  -> r.getCodigoBarra();
            case 3  -> r.getDescripcion();
            case 4  -> r.getPrincipioActivo();
            case 5  -> r.getMarca();
            case 6  -> (r.getCostoActual() > 0) ? r.getCostoActual() : r.getCostoFabrica();
            case 7  -> r.getArancelPct();
            case 8  -> (r.getCostoActual() > 0) ? r.getCostoActual() : r.getCostoFabrica();
            case 9  -> r.getUtilidadPct();
            case 10 -> r.getPrecio1();
            case 11 -> r.getDctoPct2();
            case 12 -> r.getPrecioDcto();
            case 13 -> r.getFechaDesde();
            case 14 -> r.getFechaHasta();
            default -> null;
        };
    }

    @Override
    public void setValueAt(Object value, int row, int col) {
        if (row < 0 || row >= filteredData.size()) return;
        DescuentoProductoRow r = filteredData.get(row);

        if (col == 0 && value instanceof Boolean selected) {
            selectionMap.put(r.getCodigo(), selected);
            fireTableCellUpdated(row, col);
        } else if (col == 11) {
            double nuevoDcto = 0.0;
            if (value instanceof Number n) {
                nuevoDcto = n.doubleValue();
            } else if (value != null) {
                try {
                    String str = value.toString().replace(",", ".").trim();
                    if (!str.isEmpty()) {
                        nuevoDcto = Double.parseDouble(str);
                    }
                } catch (NumberFormatException ignored) {}
            }
            if (nuevoDcto < 0) nuevoDcto = 0.0;
            if (nuevoDcto > 100) nuevoDcto = 100.0;

            r.setDctoPct2(nuevoDcto);
            double precio1 = r.getPrecio1();
            r.setPrecioDcto(precio1 * (1.0 - (nuevoDcto / 100.0)));
            selectionMap.put(r.getCodigo(), true);

            fireTableCellUpdated(row, 0);
            fireTableCellUpdated(row, 11);
            fireTableCellUpdated(row, 12);

            if (onCellDiscountEdited != null) {
                onCellDiscountEdited.accept(r.getCodigo(), nuevoDcto);
            }
        }
    }

    public void setData(List<DescuentoProductoRow> data) {
        this.allData = new ArrayList<>(data);
        selectionMap.clear();
        applyFilter();
    }

    public List<DescuentoProductoRow> getAllData() {
        return allData;
    }

    public List<DescuentoProductoRow> getFilteredData() {
        return filteredData;
    }

    public void setFilterText(String text) {
        this.filterText = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        applyFilter();
    }

    public void setFilterMarca(String marca) {
        this.filterMarca = marca == null ? "" : marca.trim().toLowerCase(Locale.ROOT);
        applyFilter();
    }

    private void applyFilter() {
        java.util.stream.Stream<DescuentoProductoRow> stream = allData.stream();

        if (filtrosCriteria != null && !filtrosCriteria.isEmpty()) {
            stream = stream.filter(this::matchesCriteria);
        }

        if (!filterMarca.isEmpty()) {
            stream = stream.filter(r -> r.getMarca() != null && r.getMarca().toLowerCase(Locale.ROOT).contains(filterMarca));
        }

        if (!filterText.isEmpty()) {
            stream = stream.filter(r ->
                    matches(r.getCodigo())
                    || matches(r.getCodigoBarra())
                    || matches(r.getDescripcion())
                    || matches(r.getPrincipioActivo())
                    || matches(r.getMarca())
                    || matches(r.getLinea()));
        }

        filteredData = stream.collect(Collectors.toList());
        fireTableDataChanged();
    }

    private boolean matchesCriteria(DescuentoProductoRow r) {
        com.droai.model.FiltrosCriteria c = this.filtrosCriteria;
        boolean anyPos = c.isCualquierPosicion();

        if (!c.getCodigo().isEmpty() && !matchField(r.getCodigo(), c.getCodigo(), anyPos)) return false;
        if (!c.getDescripcion().isEmpty() && !matchField(r.getDescripcion(), c.getDescripcion(), anyPos)) return false;
        if (!c.getCodigoBarra().isEmpty() && !matchField(r.getCodigoBarra(), c.getCodigoBarra(), anyPos)) return false;
        if (!c.getMarca().isEmpty() && !matchField(r.getMarca(), c.getMarca(), anyPos)) return false;

        if (!c.getProveedor().isEmpty()
                && !matchField(r.getNombreProveedor(), c.getProveedor(), anyPos)
                && !matchField(r.getCodProveedor(), c.getProveedor(), anyPos)) return false;

        if (!c.getGrupo().isEmpty()
                && !matchField(r.getLinea(), c.getGrupo(), anyPos)
                && !matchField(r.getCodLinea(), c.getGrupo(), anyPos)) return false;

        double costo = (r.getCostoActual() > 0) ? r.getCostoActual() : r.getCostoFabrica();
        switch (c.getFiltroCosto()) {
            case SIN_COSTO -> { if (costo > 0) return false; }
            case CON_COSTO -> { if (costo <= 0) return false; }
            default -> {}
        }

        switch (c.getFiltroPrecio()) {
            case SIN_PRECIO -> { if (r.getPrecio1() > 0) return false; }
            case CON_PRECIO -> { if (r.getPrecio1() <= 0) return false; }
            default -> {}
        }

        if (c.isSoloPrecioMenorCosto()) {
            if (r.getPrecio1() > costo) return false;
        }

        return true;
    }

    private boolean matchField(String value, String filter, boolean anyPosition) {
        if (value == null) return false;
        String v = value.toLowerCase(Locale.ROOT);
        String f = filter.toLowerCase(Locale.ROOT);
        return anyPosition ? v.contains(f) : v.startsWith(f);
    }

    private boolean matches(String val) {
        return val != null && val.toLowerCase(Locale.ROOT).contains(filterText);
    }

    // --- API de Selección ---

    public List<String> getSelectedCodigos() {
        List<String> selected = new ArrayList<>();
        for (DescuentoProductoRow r : filteredData) {
            if (selectionMap.getOrDefault(r.getCodigo(), false)) {
                selected.add(r.getCodigo());
            }
        }
        return selected;
    }

    public void selectAll(boolean select) {
        for (DescuentoProductoRow r : filteredData) {
            selectionMap.put(r.getCodigo(), select);
        }
        fireTableDataChanged();
    }
}

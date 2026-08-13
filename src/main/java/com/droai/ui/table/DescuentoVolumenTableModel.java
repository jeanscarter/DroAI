package com.droai.ui.table;

import com.droai.model.DescuentoVolumenRow;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class DescuentoVolumenTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {
            "Seleccionar", "Codigo", "Descripcion", "Marca", "Codigo de Barra", "Precio", "Descuento DV",
            "Fecha Inicio", "Fecha Fin"
    };

    private List<DescuentoVolumenRow> allData = new ArrayList<>();
    private List<DescuentoVolumenRow> filteredData = new ArrayList<>();
    private final Map<String, Boolean> selectionMap = new HashMap<>();

    private String filterText = "";
    private String filterMarca = "";
    private com.droai.model.FiltrosCriteria filtrosCriteria = null;

    private java.util.function.BiConsumer<String, Double> onCellDiscountEdited;

    public void setOnCellDiscountEdited(java.util.function.BiConsumer<String, Double> cb) {
        this.onCellDiscountEdited = cb;
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
            case 1, 2, 3, 4, 7, 8 -> String.class;
            case 5, 6 -> Double.class;
            default -> Object.class;
        };
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return col == 0 || col == 6; // Selección y Descuento DV son editables
    }

    @Override
    public Object getValueAt(int row, int col) {
        DescuentoVolumenRow r = filteredData.get(row);
        return switch (col) {
            case 0 -> selectionMap.getOrDefault(r.getCodigo(), false);
            case 1 -> r.getCodigo();
            case 2 -> r.getDescripcion();
            case 3 -> r.getMarca();
            case 4 -> r.getCodigoBarra();
            case 5 -> r.getPrecio1();
            case 6 -> r.getDescuentoDV();
            case 7 -> r.getFechaIni();
            case 8 -> r.getFechaFin();
            default -> null;
        };
    }

    @Override
    public void setValueAt(Object value, int row, int col) {
        if (row < 0 || row >= filteredData.size()) return;
        DescuentoVolumenRow r = filteredData.get(row);

        if (col == 0 && value instanceof Boolean selected) {
            selectionMap.put(r.getCodigo(), selected);
            fireTableCellUpdated(row, col);
        } else if (col == 6) {
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

            r.setDescuentoDV(nuevoDcto);
            selectionMap.put(r.getCodigo(), true);

            fireTableCellUpdated(row, 0);
            fireTableCellUpdated(row, 6);

            if (onCellDiscountEdited != null) {
                onCellDiscountEdited.accept(r.getCodigo(), nuevoDcto);
            }
        }
    }

    public void setData(List<DescuentoVolumenRow> data) {
        this.allData = new ArrayList<>(data);
        selectionMap.clear();
        applyFilter();
    }

    public List<DescuentoVolumenRow> getAllData() {
        return allData;
    }

    public List<DescuentoVolumenRow> getFilteredData() {
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

    public void setFiltrosCriteria(com.droai.model.FiltrosCriteria criteria) {
        this.filtrosCriteria = criteria;
        applyFilter();
    }

    public com.droai.model.FiltrosCriteria getFiltrosCriteria() {
        return filtrosCriteria;
    }

    private void applyFilter() {
        java.util.stream.Stream<DescuentoVolumenRow> stream = allData.stream();

        if (filtrosCriteria != null && !filtrosCriteria.isEmpty()) {
            stream = stream.filter(this::matchesCriteria);
        }

        if (!filterMarca.isEmpty()) {
            stream = stream.filter(r -> r.getMarca() != null && r.getMarca().toLowerCase(Locale.ROOT).contains(filterMarca));
        }

        if (!filterText.isEmpty()) {
            stream = stream.filter(r ->
                    matches(r.getCodigo())
                    || matches(r.getDescripcion())
                    || matches(r.getMarca())
                    || matches(r.getCodigoBarra())
                    || matches(r.getLinea()));
        }

        filteredData = stream.collect(Collectors.toList());
        fireTableDataChanged();
    }

    private boolean matchesCriteria(DescuentoVolumenRow r) {
        com.droai.model.FiltrosCriteria c = this.filtrosCriteria;
        boolean anyPos = c.isCualquierPosicion();

        if (!c.getCodigo().isEmpty() && !matchField(r.getCodigo(), c.getCodigo(), anyPos)) return false;
        if (!c.getDescripcion().isEmpty() && !matchField(r.getDescripcion(), c.getDescripcion(), anyPos)) return false;
        if (!c.getCodigoBarra().isEmpty() && !matchField(r.getCodigoBarra(), c.getCodigoBarra(), anyPos)) return false;
        if (!c.getMarca().isEmpty() && !matchField(r.getMarca(), c.getMarca(), anyPos) && !matchField(r.getModelo(), c.getMarca(), anyPos)) return false;

        if (!c.getProveedor().isEmpty()
                && !matchField(r.getNombreProveedor(), c.getProveedor(), anyPos)
                && !matchField(r.getCodProveedor(), c.getProveedor(), anyPos)) return false;

        if (!c.getGrupo().isEmpty()
                && !matchField(r.getLinea(), c.getGrupo(), anyPos)
                && !matchField(r.getCodLinea(), c.getGrupo(), anyPos)) return false;

        switch (c.getFiltroPrecio()) {
            case SIN_PRECIO -> { if (r.getPrecio1() > 0) return false; }
            case CON_PRECIO -> { if (r.getPrecio1() <= 0) return false; }
            default -> {}
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
        for (DescuentoVolumenRow r : filteredData) {
            if (selectionMap.getOrDefault(r.getCodigo(), false)) {
                selected.add(r.getCodigo());
            }
        }
        return selected;
    }

    public void selectAll(boolean select) {
        for (DescuentoVolumenRow r : filteredData) {
            selectionMap.put(r.getCodigo(), select);
        }
        fireTableDataChanged();
    }
}

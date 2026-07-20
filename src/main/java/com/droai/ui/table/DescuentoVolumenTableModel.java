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
        return col == 0; // Solo la columna de selección es editable
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
        if (col == 0 && value instanceof Boolean selected) {
            DescuentoVolumenRow r = filteredData.get(row);
            selectionMap.put(r.getCodigo(), selected);
            fireTableCellUpdated(row, col);
        }
    }

    public void setData(List<DescuentoVolumenRow> data) {
        this.allData = new ArrayList<>(data);
        // Limpiar selección previa al recargar
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

    private void applyFilter() {
        java.util.stream.Stream<DescuentoVolumenRow> stream = allData.stream();

        // 1. Filtrar por Marca
        if (!filterMarca.isEmpty()) {
            stream = stream.filter(r -> r.getMarca() != null && r.getMarca().toLowerCase(Locale.ROOT).contains(filterMarca));
        }

        // 2. Búsqueda de texto libre
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

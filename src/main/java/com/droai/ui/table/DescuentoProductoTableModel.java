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

    @Override
    public boolean isCellEditable(int row, int col) {
        return col == 0;
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
            case 6  -> r.getCostoFabrica();
            case 7  -> r.getArancelPct();
            case 8  -> r.getCostoActual();
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
        if (col == 0 && value instanceof Boolean selected) {
            DescuentoProductoRow r = filteredData.get(row);
            selectionMap.put(r.getCodigo(), selected);
            fireTableCellUpdated(row, col);
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

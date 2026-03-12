package com.droai.ui.table;

import com.droai.model.FacturaRow;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Modelo editable para la pestaña "Listado de Productos".
 * Columnas exactas: Codigo, Descripcion, Referencia, Existencia, UdM,
 * Costo Fabrica, Arancel%, Costo OM, Util%, Precio1, %IVA, Precio C/IVA.
 *
 * Editable: Costo Fabrica(5), Arancel%(6), Util%(8), Precio1(9), %IVA(10).
 */
public class ListadoTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {
        "Codigo", "Descripcion", "Referencia", "Existencia", "UdM",
        "Costo Fabrica", "Arancel%", "Costo OM", "Util %",
        "Precio1", "%IVA", "Precio C/IVA"
    };

    // Columnas editables por índice
    private static final int[] EDITABLE = {5, 6, 8, 9, 10};

    private List<FacturaRow> allData = new ArrayList<>();
    private List<FacturaRow> filteredData = new ArrayList<>();
    private String filterText = "";

    @Override public int getRowCount()    { return filteredData.size(); }
    @Override public int getColumnCount() { return COLUMNS.length; }
    @Override public String getColumnName(int col) { return COLUMNS[col]; }

    @Override
    public Class<?> getColumnClass(int col) {
        return switch (col) {
            case 0, 1, 2, 4 -> String.class;
            default -> Double.class;
        };
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        for (int e : EDITABLE) if (e == col) return true;
        return false;
    }

    @Override
    public Object getValueAt(int row, int col) {
        FacturaRow r = filteredData.get(row);
        return switch (col) {
            case 0  -> r.getCodigo();
            case 1  -> r.getDescripcion();
            case 2  -> r.getReferencia();
            case 3  -> r.getExistencia();
            case 4  -> r.getUdm();
            case 5  -> r.getCostoFabrica();
            case 6  -> r.getArancelPct();
            case 7  -> r.getCostoOM();
            case 8  -> r.getUtilPct();
            case 9  -> r.getPrecio1();
            case 10 -> r.getIvaPct();
            case 11 -> r.getPrecioCIVA();
            default -> null;
        };
    }

    @Override
    public void setValueAt(Object value, int row, int col) {
        FacturaRow r = filteredData.get(row);
        double v = (value instanceof Number n) ? n.doubleValue() : 0.0;
        switch (col) {
            case 5  -> r.setCostoFabrica(v);
            case 6  -> r.setArancelPct(v);
            case 8  -> r.setUtilPct(v);
            case 9  -> r.setPrecio1(v);
            case 10 -> r.setIvaPct(v);
        }
        // Recalcular Precio C/IVA
        r.setPrecioCIVA(r.getPrecio1() * (1 + r.getIvaPct() / 100.0));
        fireTableRowsUpdated(row, row);
    }

    /** Carga nuevos datos desde DAO. */
    public void setData(List<FacturaRow> data) {
        this.allData = new ArrayList<>(data);
        applyFilter();
    }

    /** Filtro de búsqueda in-memory por texto (codigo o descripcion). */
    public void setFilter(String text) {
        this.filterText = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        applyFilter();
    }

    private void applyFilter() {
        if (filterText.isEmpty()) {
            filteredData = new ArrayList<>(allData);
        } else {
            filteredData = allData.stream()
                .filter(r -> r.getCodigo().toLowerCase(Locale.ROOT).contains(filterText)
                          || r.getDescripcion().toLowerCase(Locale.ROOT).contains(filterText)
                          || r.getReferencia().toLowerCase(Locale.ROOT).contains(filterText))
                .collect(java.util.stream.Collectors.toList());
        }
        fireTableDataChanged();
    }

    /** Obtiene solo filas modificadas para guardar en BD. */
    public List<FacturaRow> getModifiedRows() {
        return allData.stream().filter(FacturaRow::isModified)
                .collect(java.util.stream.Collectors.toList());
    }

    /** Todos los datos filtrados (para exportar). */
    public List<FacturaRow> getFilteredData() { return filteredData; }

    /** Todos los datos sin filtrar. */
    public List<FacturaRow> getAllData() { return allData; }

    public int getTotalCount() { return allData.size(); }
}

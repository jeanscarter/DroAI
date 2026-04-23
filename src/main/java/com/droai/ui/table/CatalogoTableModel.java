package com.droai.ui.table;

import com.droai.model.ArticuloRow;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * TableModel para el Catálogo de Productos.
 * Muestra exactamente 12 columnas en la UI.
 * El modelo subyacente almacena todos los datos para la exportación.
 */
public class CatalogoTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {
            "Codigo", "Descripcion", "Marca", "Existencia", "UdM", "Costo Fabrica",
            "Arancel%", "Costo OM", "Util %", "Precio1", "%IVA", "Precio C/IVA"
    };

    private List<ArticuloRow> allData = new ArrayList<>();
    private List<ArticuloRow> filteredData = new ArrayList<>();
    private String filterText = "";

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
            case 0, 1, 2, 4 -> String.class;
            default -> Double.class;
        };
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return false;
    }

    @Override
    public Object getValueAt(int row, int col) {
        ArticuloRow r = filteredData.get(row);
        return switch (col) {
            case 0  -> r.getCodigo();
            case 1  -> r.getDescripcion();
            case 2  -> r.getMarca();
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

    public void setData(List<ArticuloRow> data) {
        this.allData = new ArrayList<>(data);
        applyFilter();
    }

    public void setFilter(String text) {
        this.filterText = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        applyFilter();
    }

    private void applyFilter() {
        if (filterText.isEmpty()) {
            filteredData = new ArrayList<>(allData);
        } else {
            filteredData = allData.stream()
                    .filter(r -> matches(r.getCodigo())
                            || matches(r.getDescripcion())
                            || matches(r.getMarca())
                            || matches(r.getCodLinea()))
                    .collect(Collectors.toList());
        }
        fireTableDataChanged();
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

package com.droai.ui.table;

import com.droai.model.MatrizVentasRow;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class MatrizVentasTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {
            "Codigo", "Descripcion", "Marca", "Existencia", "UdM", "Costo Fabrica",
            "Arancel%", "Costo OM", "Util %", "Precio1", "%IVA", "Precio C/IVA"
    };

    private List<MatrizVentasRow> allData = new ArrayList<>();
    private List<MatrizVentasRow> filteredData = new ArrayList<>();
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
        MatrizVentasRow r = filteredData.get(row);
        return switch (col) {
            case 0 -> r.getCodigoArt();
            case 1 -> r.getDescripcion();
            case 2 -> r.getMarca();
            case 3 -> r.getStockActual();
            case 4 -> r.getUdm();
            case 5 -> r.getCostoFabrica();
            case 6 -> r.getArancelPct();
            case 7 -> r.getCostoOm();
            case 8 -> r.getUtilPct();
            case 9 -> r.getPrecio();
            case 10 -> r.getIvaPct();
            case 11 -> r.getPrecioCiva();
            default -> null;
        };
    }

    public void setData(List<MatrizVentasRow> data) {
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
                    .filter(r -> (r.getCodigoArt() != null
                            && r.getCodigoArt().toLowerCase(Locale.ROOT).contains(filterText))
                            || (r.getDescripcion() != null
                                    && r.getDescripcion().toLowerCase(Locale.ROOT).contains(filterText))
                            || (r.getNombreRazonSocial() != null
                                    && r.getNombreRazonSocial().toLowerCase(Locale.ROOT).contains(filterText))
                            || (r.getNumero() != null && r.getNumero().toLowerCase(Locale.ROOT).contains(filterText)))
                    .collect(Collectors.toList());
        }
        fireTableDataChanged();
    }

    public List<MatrizVentasRow> getFilteredData() {
        return filteredData;
    }

    public List<MatrizVentasRow> getAllData() {
        return allData;
    }

    public int getTotalCount() {
        return allData.size();
    }
}
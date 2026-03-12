package com.droai.ui.table;

import com.droai.model.ResumenRow;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Modelo solo-lectura para tabs de resumen (Descuentos x Volumen, Descuento x Producto).
 */
public class ResumenTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {
        "Codigo", "Descripcion", "Total", "Descuento", "Neto", "% Descuento"
    };

    private List<ResumenRow> data = new ArrayList<>();

    @Override public int getRowCount()    { return data.size(); }
    @Override public int getColumnCount() { return COLUMNS.length; }
    @Override public String getColumnName(int col) { return COLUMNS[col]; }

    @Override
    public Class<?> getColumnClass(int col) {
        return switch (col) {
            case 0, 1 -> String.class;
            default -> Double.class;
        };
    }

    @Override public boolean isCellEditable(int row, int col) { return false; }

    @Override
    public Object getValueAt(int row, int col) {
        ResumenRow r = data.get(row);
        return switch (col) {
            case 0 -> r.getClave();
            case 1 -> r.getDescripcion();
            case 2 -> r.getTotal();
            case 3 -> r.getDescuento();
            case 4 -> r.getNeto();
            case 5 -> r.getPorcentaje();
            default -> null;
        };
    }

    public void setData(List<ResumenRow> data) {
        this.data = new ArrayList<>(data);
        fireTableDataChanged();
    }

    public List<ResumenRow> getData() { return data; }
}

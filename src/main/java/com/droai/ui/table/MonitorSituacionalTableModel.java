package com.droai.ui.table;

import com.droai.service.MonitorService.AgrupacionImpuesto;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Modelo de tabla para el Monitor Situacional.
 *
 * <p>Muestra agrupaciones de ventas por tipo de impuesto (alícuota)
 * con columnas: Grupo, Unidades, Descripción, Monto y Porcentaje.
 */
public class MonitorSituacionalTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {
            "Grupo", "Unidades", "Descripción", "Monto (Bs)", "% del Total"
    };

    private List<AgrupacionImpuesto> data = new ArrayList<>();

    @Override
    public int getRowCount() {
        return data.size();
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
            case 0, 2 -> String.class;
            case 1, 3, 4 -> Double.class;
            default -> Object.class;
        };
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return false;
    }

    @Override
    public Object getValueAt(int row, int col) {
        AgrupacionImpuesto r = data.get(row);
        return switch (col) {
            case 0 -> r.grupo();
            case 1 -> r.unidades();
            case 2 -> r.descripcion();
            case 3 -> r.monto();
            case 4 -> r.porcentaje();
            default -> null;
        };
    }

    /**
     * Establece los datos de la tabla.
     *
     * @param agrupaciones lista de agrupaciones por alícuota.
     */
    public void setData(List<AgrupacionImpuesto> agrupaciones) {
        this.data = agrupaciones != null ? new ArrayList<>(agrupaciones) : new ArrayList<>();
        fireTableDataChanged();
    }

    /**
     * Limpia los datos de la tabla.
     */
    public void clear() {
        this.data.clear();
        fireTableDataChanged();
    }

    /**
     * @return los datos actuales.
     */
    public List<AgrupacionImpuesto> getData() {
        return data;
    }
}

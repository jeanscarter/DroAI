package com.droai.ui.table;

import com.droai.service.MonitorService.AgrupacionLinea;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Modelo de tabla para la sección principal del Monitor Situacional.
 *
 * <p>Muestra la agrupación de unidades vendidas por Línea de Producto
 * con columnas: Grupo (código + nombre) y Unidades.
 * Replica la "Tabla de Datos" del sistema anterior.
 */
public class LineasProductoTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = { "Grupo", "Unidades" };

    private List<AgrupacionLinea> data = new ArrayList<>();

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
            case 0 -> String.class;
            case 1 -> Double.class;
            default -> Object.class;
        };
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return false;
    }

    @Override
    public Object getValueAt(int row, int col) {
        AgrupacionLinea r = data.get(row);
        return switch (col) {
            case 0 -> r.codLinea() + " " + r.linea();
            case 1 -> r.unidades();
            default -> null;
        };
    }

    /**
     * Establece los datos de la tabla.
     *
     * @param agrupaciones lista de agrupaciones por línea de producto.
     */
    public void setData(List<AgrupacionLinea> agrupaciones) {
        this.data = agrupaciones != null ? new ArrayList<>(agrupaciones) : new ArrayList<>();
        fireTableDataChanged();
    }

    /**
     * @return los datos actuales.
     */
    public List<AgrupacionLinea> getData() {
        return data;
    }
}

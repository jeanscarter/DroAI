package com.droai.ui.table;

import com.droai.model.ProductoReporteRow;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * TableModel para el visor de reporte de productos.
 */
public class ReporteTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {
        "Código", "Código de Barra", "Descripción", "Marca", "Línea",
        "Principio Activo", "Categoría", "Proveedor", "Existencia", "Impuesto"
    };

    private List<ProductoReporteRow> data = new ArrayList<>();

    public ReporteTableModel() {}

    public void setData(List<ProductoReporteRow> data) {
        this.data = data != null ? new ArrayList<>(data) : new ArrayList<>();
        fireTableDataChanged();
    }

    public List<ProductoReporteRow> getData() {
        return data;
    }

    public ProductoReporteRow getRow(int index) {
        if (index >= 0 && index < data.size()) {
            return data.get(index);
        }
        return null;
    }

    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 8 || columnIndex == 9) {
            return Double.class;
        }
        return String.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        ProductoReporteRow row = data.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> row.getCodigo();
            case 1 -> row.getCodigoBarra();
            case 2 -> row.getDescripcion();
            case 3 -> row.getMarca();
            case 4 -> row.getLinea();
            case 5 -> row.getPrincipioActivo();
            case 6 -> row.getCategoria();
            case 7 -> row.getProveedor();
            case 8 -> row.getExistencia();
            case 9 -> row.getImpuesto();
            default -> null;
        };
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }
}

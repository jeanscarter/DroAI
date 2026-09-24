package com.droai.ui.table;

import com.droai.model.FacturaResumenRow;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Modelo de tabla para la pestaña "Facturas" del diálogo de detalle.
 *
 * <p>Muestra un resumen por factura (una fila por doc_num) con columnas:
 * Número, Fecha, Cod.Cliente, Nombre Cliente, Monto Bruto, Desc.%,
 * Descuento, Monto Neto, I.V.A, Total General, Cod.Ven, Nombre Vendedor.
 */
public class FacturasTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {
            "Numero", "Fecha", "Cod.Cliente", "Nombre Cliente",
            "Monto Bruto", "Desc.%", "Descuento", "Monto Neto",
            "I.V.A", "Total General", "Cod.Ven", "Nombre Vendedor"
    };

    private List<FacturaResumenRow> data = new ArrayList<>();
    private boolean esDolar = false;

    public void setEsDolar(boolean esDolar) {
        this.esDolar = esDolar;
        fireTableDataChanged();
    }

    public boolean isEsDolar() {
        return esDolar;
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
    public String getColumnName(int col) {
        return COLUMNS[col];
    }

    @Override
    public Class<?> getColumnClass(int col) {
        return switch (col) {
            case 0, 1, 2, 3, 10, 11 -> String.class;
            case 4, 5, 6, 7, 8, 9   -> Double.class;
            default -> Object.class;
        };
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return false;
    }

    private double formatMonetary(double val, double tasa) {
        if (esDolar && tasa > 0) {
            return val / tasa;
        }
        return val;
    }

    @Override
    public Object getValueAt(int row, int col) {
        FacturaResumenRow r = data.get(row);
        double t = r.getTasa();
        return switch (col) {
            case 0  -> r.getNumero();
            case 1  -> r.getFecha();
            case 2  -> r.getCodCliente();
            case 3  -> r.getNombreCliente();
            case 4  -> formatMonetary(r.getMontoBruto(), t);
            case 5  -> r.getDescPct();
            case 6  -> formatMonetary(r.getDescuento(), t);
            case 7  -> formatMonetary(r.getMontoNeto(), t);
            case 8  -> formatMonetary(r.getIva(), t);
            case 9  -> formatMonetary(r.getTotalGeneral(), t);
            case 10 -> r.getCodVen();
            case 11 -> r.getNombreVendedor();
            default -> null;
        };
    }

    /**
     * Establece los datos de la tabla.
     *
     * @param facturas lista de facturas resumidas.
     */
    public void setData(List<FacturaResumenRow> facturas) {
        this.data = facturas != null ? new ArrayList<>(facturas) : new ArrayList<>();
        fireTableDataChanged();
    }

    /**
     * @return los datos actuales.
     */
    public List<FacturaResumenRow> getData() {
        return data;
    }

    public double getTotalMontoBruto() {
        return data.stream().mapToDouble(r -> formatMonetary(r.getMontoBruto(), r.getTasa())).sum();
    }

    public double getTotalDescuento() {
        return data.stream().mapToDouble(r -> formatMonetary(r.getDescuento(), r.getTasa())).sum();
    }

    public double getTotalIva() {
        return data.stream().mapToDouble(r -> formatMonetary(r.getIva(), r.getTasa())).sum();
    }

    public double getTotalGeneral() {
        return data.stream().mapToDouble(r -> formatMonetary(r.getTotalGeneral(), r.getTasa())).sum();
    }
}

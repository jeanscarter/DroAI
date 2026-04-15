package com.droai.ui.table;

import com.droai.model.FacturaRow;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Modelo para visualizar la "Matriz de Ventas".
 * Columnas: 30 columnas extraídas de Profit Plus y cálculos adicionales.
 */
public class ListadoTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {
        "Numero", "Fecha", "CI/Rif", "Nombre o Razon Social", "Vendedor", "Nombre Vendedor",
        "Tasa", "codigo art", "Descripcion", "Cantidad", "Precio", "DP", "DCT", "DA", "DV",
        "Desc.%", "Total Renglon", "Desc.%Global", "Renglon-DG", "Monto IVA", "Tot.Renglon+IVA",
        "Costo de Venta", "Total Costo Venta", "Tot.CV-DP", "Monto Utilidad", "% Utilidad",
        "Costo Actual", "Stock Actual", "Cod.Linea", "Linea"
    };

    private List<FacturaRow> allData = new ArrayList<>();
    private List<FacturaRow> filteredData = new ArrayList<>();
    private String filterText = "";

    @Override public int getRowCount()    { return filteredData.size(); }
    @Override public int getColumnCount() { return COLUMNS.length; }
    @Override public String getColumnName(int col) { return COLUMNS[col]; }

    @Override
    public Class<?> getColumnClass(int col) {
        return switch (col) {
            case 0, 1, 2, 3, 4, 5, 7, 8, 28, 29 -> String.class;
            default -> Double.class;
        };
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        // La matriz general histórica normalmente es solo lectura.
        return false;
    }

    @Override
    public Object getValueAt(int row, int col) {
        FacturaRow r = filteredData.get(row);
        return switch (col) {
            case 0 -> r.getNumero();
            case 1 -> r.getFecha();
            case 2 -> r.getCiRif();
            case 3 -> r.getNombreRazonSocial();
            case 4 -> r.getCoVen();
            case 5 -> r.getNombreVendedor();
            case 6 -> r.getTasa();
            case 7 -> r.getCodigoArt();
            case 8 -> r.getDescripcionArt();
            case 9 -> r.getCantidad();
            case 10 -> r.getPrecio();
            case 11 -> r.getDp();
            case 12 -> r.getDct();
            case 13 -> r.getDa();
            case 14 -> r.getDv();
            case 15 -> r.getDescPct();
            case 16 -> r.getTotalRenglon();
            case 17 -> r.getDescPctGlobal();
            case 18 -> r.getRenglonDg();
            case 19 -> r.getMontoIva();
            case 20 -> r.getTotRenglonIva();
            case 21 -> r.getCostoVenta();
            case 22 -> r.getTotalCostoVenta();
            case 23 -> r.getTotCvDp();
            case 24 -> r.getMontoUtilidad();
            case 25 -> r.getUtilPct();
            case 26 -> r.getCostoActual();
            case 27 -> r.getStockActual();
            case 28 -> r.getCodLinea();
            case 29 -> r.getLinea();
            default -> null;
        };
    }

    public void setData(List<FacturaRow> data) {
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
                .filter(r -> r.getCodigoArt().toLowerCase(Locale.ROOT).contains(filterText)
                          || (r.getDescripcionArt() != null && r.getDescripcionArt().toLowerCase(Locale.ROOT).contains(filterText))
                          || (r.getNombreRazonSocial() != null && r.getNombreRazonSocial().toLowerCase(Locale.ROOT).contains(filterText))
                          || (r.getNumero() != null && r.getNumero().toLowerCase(Locale.ROOT).contains(filterText)))
                .collect(java.util.stream.Collectors.toList());
        }
        fireTableDataChanged();
    }

    public List<FacturaRow> getFilteredData() { return filteredData; }
    public List<FacturaRow> getAllData() { return allData; }
    public int getTotalCount() { return allData.size(); }
}

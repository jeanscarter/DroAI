package com.droai.ui.table;

import com.droai.model.MatrizVentasRow;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class MatrizVentasTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {
            "Numero", "Fecha", "CI/Rif", "Nombre o Razon Social", "Vendedor", "Nombre Vendedor",
            "Tasa", "codigo art", "Descripcion", "Cantidad", "Precio", "DP", "DCT", "DA", "DV",
            "Desc.%", "Total Renglon", "Desc.%Global", "Renglon-DG", "Monto IVA", "Tot.Renglon+IVA",
            "Costo de Venta", "Total Costo Venta", "Tot.CV-DP", "Monto Utilidad", "% Utilidad",
            "Costo Actual", "Stock Actual", "Cod.Linea", "Linea", "Cod.Sub.", "SubLinea",
            "Cod.Proveedor", "Nombre Proveedor", "Zona", "almacen", "Pedido Web", "Origen", "Usuario Web"
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
            case 0, 1, 2, 3, 4, 5, 7, 8, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38 -> String.class;
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
            case 0 -> r.getNumero();
            case 1 -> r.getFecha();
            case 2 -> r.getCiRif();
            case 3 -> r.getNombreRazonSocial();
            case 4 -> r.getCoVen();
            case 5 -> r.getNombreVendedor();
            case 6 -> r.getTasa();
            case 7 -> r.getCodigoArt();
            case 8 -> r.getDescripcion();
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
            case 30 -> r.getCodSub();
            case 31 -> r.getSubLinea();
            case 32 -> r.getCodProveedor();
            case 33 -> r.getNombreProveedor();
            case 34 -> r.getZona();
            case 35 -> r.getAlmacen();
            case 36 -> r.getPedidoWeb();
            case 37 -> r.getOrigen();
            case 38 -> r.getUsuarioWeb();
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
package com.droai.ui.table;

import com.droai.model.MatrizVentasRow;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Modelo de tabla para la pestaña "Productos Facturados" del diálogo de detalle.
 *
 * <p>Muestra exactamente las 40 columnas exportadas al archivo de Excel.
 * Soporta conversión dinámica de moneda (Bs vs USD).
 */
public class ProductosFacturadosTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {
            "Numero", "Fecha", "CI / RIF", "Nombre o Razon Social", "Cod.Ven.",
            "Nombre Vendedor", "Tasa", "Codigo Art.", "Descripcion", "Cantidad",
            "Precio", "DP", "DA", "DCT", "DC", "DV",
            "Total Renglon", "% Desc. Glob.", "Renglon DG", "Monto I.V.A.",
            "% I.V.A.", "Tot. Renglon IVA", "Costo Venta", "Total Costo Venta", "Tot. CV DP",
            "Monto Utilidad", "% Util.", "Costo Actual", "Stock Actual", "Cod. Linea",
            "Linea", "Cod. Sub.", "Sub-Linea", "Cod. Prov.", "Nombre Proveedor",
            "Zona", "Almacen", "Pedido Web", "Origen", "Usuario Web"
    };

    private List<MatrizVentasRow> data = new ArrayList<>();
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
            case 0, 1, 2, 3, 4, 5, 7, 8, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39 -> String.class;
            default -> Double.class;
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
        MatrizVentasRow r = data.get(row);
        double t = r.getTasa();
        return switch (col) {
            case 0  -> r.getNumero();
            case 1  -> r.getFecha();
            case 2  -> r.getCiRif();
            case 3  -> r.getNombreRazonSocial();
            case 4  -> r.getCoVen();
            case 5  -> r.getNombreVendedor();
            case 6  -> r.getTasa();
            case 7  -> r.getCodigoArt();
            case 8  -> r.getDescripcion();
            case 9  -> r.getCantidad();
            case 10 -> formatMonetary(r.getPrecio(), t);
            case 11 -> r.getDp();
            case 12 -> r.getDa();
            case 13 -> r.getDct();
            case 14 -> r.getDc();
            case 15 -> r.getDv();
            case 16 -> formatMonetary(r.getTotalRenglon(), t);
            case 17 -> r.getDescPctGlobal();
            case 18 -> formatMonetary(r.getRenglonDg(), t);
            case 19 -> formatMonetary(r.getMontoIva(), t);
            case 20 -> r.getIvaPct();
            case 21 -> formatMonetary(r.getTotRenglonIva(), t);
            case 22 -> formatMonetary(r.getCostoVenta(), t);
            case 23 -> formatMonetary(r.getTotalCostoVenta(), t);
            case 24 -> formatMonetary(r.getTotCvDp(), t);
            case 25 -> formatMonetary(r.getMontoUtilidad(), t);
            case 26 -> r.getUtilPct();
            case 27 -> formatMonetary(r.getCostoActual(), t);
            case 28 -> r.getStockActual();
            case 29 -> r.getCodLinea();
            case 30 -> r.getLinea();
            case 31 -> r.getCodSub();
            case 32 -> r.getSubLinea();
            case 33 -> r.getCodProveedor();
            case 34 -> r.getNombreProveedor();
            case 35 -> r.getZona();
            case 36 -> r.getAlmacen();
            case 37 -> r.getPedidoWeb();
            case 38 -> r.getOrigen();
            case 39 -> r.getUsuarioWeb();
            default -> null;
        };
    }

    public void setData(List<MatrizVentasRow> rows) {
        this.data = rows != null ? new ArrayList<>(rows) : new ArrayList<>();
        fireTableDataChanged();
    }

    public List<MatrizVentasRow> getData() {
        return data;
    }

    public double getTotalMonto() {
        return data.stream().mapToDouble(r -> formatMonetary(r.getPrecio() * r.getCantidad(), r.getTasa())).sum();
    }

    public double getTotalCantidad() {
        return data.stream().mapToDouble(MatrizVentasRow::getCantidad).sum();
    }

    public double getTotalCosto() {
        return data.stream().mapToDouble(r -> formatMonetary(r.getCostoVenta(), r.getTasa())).sum();
    }
}

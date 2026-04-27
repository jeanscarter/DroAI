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
    private String columnaDinamicaActual = "Marca";

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
        if (col == 2) return columnaDinamicaActual;
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
        if (col == 2) {
            return switch (columnaDinamicaActual) {
                case "Referencia" -> r.getReferencia();
                case "Codigo de Barra" -> r.getCodigoBarra();
                case "Marca" -> r.getMarca();
                case "Modelo" -> r.getModelo();
                case "Ubicacion" -> r.getUbicacion();
                case "Campo 1" -> r.getCampo1();
                case "Campo 2" -> r.getCampo2();
                default -> r.getMarca();
            };
        }
        return switch (col) {
            case 0  -> r.getCodigo();
            case 1  -> r.getDescripcion();
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

    public void setColumnaDinamica(String columna) {
        this.columnaDinamicaActual = columna;
        fireTableStructureChanged();
    }

    public void ordenarPor(String criterio) {
        java.util.Comparator<ArticuloRow> comparator;
        switch (criterio) {
            case "Codigo":
                comparator = java.util.Comparator.comparing(r -> r.getCodigo() != null ? r.getCodigo() : "");
                break;
            case "Descripcion":
                comparator = java.util.Comparator.comparing(r -> r.getDescripcion() != null ? r.getDescripcion() : "");
                break;
            case "Referencia":
                comparator = java.util.Comparator.comparing(r -> r.getReferencia() != null ? r.getReferencia() : "");
                break;
            case "Codigo de Barra":
                comparator = java.util.Comparator.comparing(r -> r.getCodigoBarra() != null ? r.getCodigoBarra() : "");
                break;
            case "Marca":
                comparator = java.util.Comparator.comparing(r -> r.getMarca() != null ? r.getMarca() : "");
                break;
            case "Modelo":
                comparator = java.util.Comparator.comparing(r -> r.getModelo() != null ? r.getModelo() : "");
                break;
            case "Ubicacion":
                comparator = java.util.Comparator.comparing(r -> r.getUbicacion() != null ? r.getUbicacion() : "");
                break;
            case "Campo 1":
                comparator = java.util.Comparator.comparing(r -> r.getCampo1() != null ? r.getCampo1() : "");
                break;
            case "Campo 2":
                comparator = java.util.Comparator.comparing(r -> r.getCampo2() != null ? r.getCampo2() : "");
                break;
            default:
                comparator = java.util.Comparator.comparing(r -> r.getCodigo() != null ? r.getCodigo() : "");
                break;
        }
        allData.sort(comparator);
        filteredData.sort(comparator);
        fireTableDataChanged();
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

package com.droai.ui.table;

import com.droai.model.DescuentoAdicionalRow;
import com.droai.model.FiltrosCriteria;

import javax.swing.table.AbstractTableModel;
import java.util.*;
import java.util.stream.Collectors;

public class DescuentoAdicionalTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {
            "Seleccionar", "Codigo", "Descripcion", "Marca", "Codigo de Barra", "Precio", "Descuento DA",
            "Fecha Desde", "Fecha Hasta"
    };

    private List<DescuentoAdicionalRow> allData = new ArrayList<>();
    private List<DescuentoAdicionalRow> filteredData = new ArrayList<>();
    private final Map<String, Boolean> selectionMap = new HashMap<>();

    private String filterText = "";
    private String filterMarca = "";
    private FiltrosCriteria filtrosCriteria = null;

    @FunctionalInterface
    public interface CellDiscountEditedListener {
        void onEdited(String codigo, double nuevoPorcentaje, String fechaDesde, String fechaHasta);
    }

    @FunctionalInterface
    public interface CellDateEditedListener {
        void onDateEdited(String codigo, String fechaDesde, String fechaHasta);
    }

    private CellDiscountEditedListener onCellDiscountEdited;
    private CellDateEditedListener onCellDateEdited;

    public void setOnCellDiscountEdited(CellDiscountEditedListener cb) {
        this.onCellDiscountEdited = cb;
    }

    public void setOnCellDateEdited(CellDateEditedListener cb) {
        this.onCellDateEdited = cb;
    }

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
            case 0 -> Boolean.class;
            case 1, 2, 3, 4, 7, 8 -> String.class;
            case 5, 6 -> Double.class;
            default -> Object.class;
        };
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return col == 0 || col == 6 || col == 7 || col == 8; // Selección, Descuento DA, Fecha Desde y Fecha Hasta son editables
    }

    @Override
    public Object getValueAt(int row, int col) {
        if (row < 0 || row >= filteredData.size()) return null;
        DescuentoAdicionalRow r = filteredData.get(row);
        return switch (col) {
            case 0 -> selectionMap.getOrDefault(r.getCodigo(), false);
            case 1 -> r.getCodigo();
            case 2 -> r.getDescripcion();
            case 3 -> r.getMarca();
            case 4 -> r.getCodigoBarra();
            case 5 -> r.getPrecio1();
            case 6 -> r.getDescuentoDA();
            case 7 -> r.getFechaDesde();
            case 8 -> r.getFechaHasta();
            default -> null;
        };
    }

    @Override
    public void setValueAt(Object value, int row, int col) {
        if (row < 0 || row >= filteredData.size()) return;
        DescuentoAdicionalRow r = filteredData.get(row);

        if (col == 0 && value instanceof Boolean selected) {
            selectionMap.put(r.getCodigo(), selected);
            fireTableCellUpdated(row, col);
        } else if (col == 6) {
            double nuevoDcto = 0.0;
            if (value instanceof Number n) {
                nuevoDcto = n.doubleValue();
            } else if (value != null) {
                try {
                    String str = value.toString().replace(",", ".").trim();
                    if (!str.isEmpty()) {
                        nuevoDcto = Double.parseDouble(str);
                    }
                } catch (NumberFormatException ignored) {}
            }
            if (nuevoDcto < 0) nuevoDcto = 0.0;
            if (nuevoDcto > 100) nuevoDcto = 100.0;

            r.setDescuentoDA(nuevoDcto);
            selectionMap.put(r.getCodigo(), true);

            // Si el descuento es > 0 y la vigencia está vacía o vencida, asignar vigencia activa
            if (nuevoDcto > 0) {
                boolean vigenciaVencidaOVacia = false;
                String fh = r.getFechaHasta();
                if (fh == null || fh.trim().isEmpty()) {
                    vigenciaVencidaOVacia = true;
                } else {
                    try {
                        java.time.LocalDate dFin = java.time.LocalDate.parse(fh.trim());
                        if (dFin.isBefore(java.time.LocalDate.now())) {
                            vigenciaVencidaOVacia = true;
                        }
                    } catch (Exception ex) {
                        vigenciaVencidaOVacia = true;
                    }
                }

                if (vigenciaVencidaOVacia) {
                    java.time.LocalDate hoy = java.time.LocalDate.now();
                    java.time.LocalDate finMes = hoy.withDayOfMonth(hoy.lengthOfMonth());
                    r.setFechaDesde(hoy.toString());
                    r.setFechaHasta(finMes.toString());
                    r.setFechaVence(finMes.toString());
                    fireTableCellUpdated(row, 7);
                    fireTableCellUpdated(row, 8);
                }
            }

            fireTableCellUpdated(row, 0);
            fireTableCellUpdated(row, 6);

            if (onCellDiscountEdited != null) {
                onCellDiscountEdited.onEdited(r.getCodigo(), nuevoDcto, r.getFechaDesde(), r.getFechaHasta());
            }
        } else if (col == 7) {
            String str = value != null ? value.toString().trim() : "";
            String norm = normalizeDate(str);
            r.setFechaDesde(norm);
            selectionMap.put(r.getCodigo(), true);
            fireTableCellUpdated(row, 0);
            fireTableCellUpdated(row, 7);
            if (onCellDateEdited != null) {
                onCellDateEdited.onDateEdited(r.getCodigo(), r.getFechaDesde(), r.getFechaHasta());
            }
        } else if (col == 8) {
            String str = value != null ? value.toString().trim() : "";
            String norm = normalizeDate(str);
            r.setFechaHasta(norm);
            r.setFechaVence(norm);
            selectionMap.put(r.getCodigo(), true);
            fireTableCellUpdated(row, 0);
            fireTableCellUpdated(row, 8);
            if (onCellDateEdited != null) {
                onCellDateEdited.onDateEdited(r.getCodigo(), r.getFechaDesde(), r.getFechaHasta());
            }
        }
    }

    private String normalizeDate(String input) {
        if (input == null || input.trim().isEmpty()) return "";
        String s = input.trim();
        if (s.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            return s;
        }
        if (s.matches("^\\d{1,2}[/-]\\d{1,2}[/-]\\d{4}$")) {
            String[] parts = s.split("[/-]");
            int day = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int year = Integer.parseInt(parts[2]);
            return String.format("%04d-%02d-%02d", year, month, day);
        }
        if (s.matches("^\\d{4}/\\d{1,2}/\\d{1,2}$")) {
            String[] parts = s.split("/");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int day = Integer.parseInt(parts[2]);
            return String.format("%04d-%02d-%02d", year, month, day);
        }
        return s;
    }

    public void setData(List<DescuentoAdicionalRow> data) {
        this.allData = new ArrayList<>(data);
        selectionMap.clear();
        applyFilter();
    }

    public List<DescuentoAdicionalRow> getFilteredData() {
        return filteredData;
    }

    public List<String> getSelectedCodigos() {
        return filteredData.stream()
                .filter(r -> selectionMap.getOrDefault(r.getCodigo(), false))
                .map(DescuentoAdicionalRow::getCodigo)
                .collect(Collectors.toList());
    }

    public void selectAll(boolean select) {
        for (DescuentoAdicionalRow r : filteredData) {
            selectionMap.put(r.getCodigo(), select);
        }
        fireTableDataChanged();
    }

    public void setFilterText(String text) {
        this.filterText = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        applyFilter();
    }

    public void setFilterMarca(String marca) {
        this.filterMarca = marca == null ? "" : marca.trim().toLowerCase(Locale.ROOT);
        applyFilter();
    }

    public void setFiltrosCriteria(FiltrosCriteria criteria) {
        this.filtrosCriteria = criteria;
        applyFilter();
    }

    public FiltrosCriteria getFiltrosCriteria() {
        return filtrosCriteria;
    }

    private void applyFilter() {
        java.util.stream.Stream<DescuentoAdicionalRow> stream = allData.stream();

        if (filtrosCriteria != null && !filtrosCriteria.isEmpty()) {
            stream = stream.filter(this::matchesCriteria);
        }

        if (!filterMarca.isEmpty()) {
            stream = stream.filter(r -> r.getMarca() != null && r.getMarca().toLowerCase(Locale.ROOT).contains(filterMarca));
        }

        if (!filterText.isEmpty()) {
            stream = stream.filter(r ->
                    matches(r.getCodigo())
                    || matches(r.getDescripcion())
                    || matches(r.getMarca())
                    || matches(r.getCodigoBarra())
                    || matches(r.getLinea()));
        }

        filteredData = stream.collect(Collectors.toList());
        fireTableDataChanged();
    }

    private boolean matchesCriteria(DescuentoAdicionalRow r) {
        FiltrosCriteria c = this.filtrosCriteria;
        boolean anyPos = c.isCualquierPosicion();

        if (!c.getCodigo().isEmpty() && !matchField(r.getCodigo(), c.getCodigo(), anyPos)) return false;
        if (!c.getDescripcion().isEmpty() && !matchField(r.getDescripcion(), c.getDescripcion(), anyPos)) return false;
        if (!c.getCodigoBarra().isEmpty() && !matchField(r.getCodigoBarra(), c.getCodigoBarra(), anyPos)) return false;
        if (!c.getMarca().isEmpty() && !matchField(r.getMarca(), c.getMarca(), anyPos) && !matchField(r.getModelo(), c.getMarca(), anyPos)) return false;

        if (!c.getProveedor().isEmpty()
                && !matchField(r.getNombreProveedor(), c.getProveedor(), anyPos)
                && !matchField(r.getCodProveedor(), c.getProveedor(), anyPos)) return false;

        if (!c.getGrupo().isEmpty()
                && !matchField(r.getLinea(), c.getGrupo(), anyPos)
                && !matchField(r.getCodLinea(), c.getGrupo(), anyPos)) return false;

        switch (c.getFiltroPrecio()) {
            case SIN_PRECIO -> { if (r.getPrecio1() > 0) return false; }
            case CON_PRECIO -> { if (r.getPrecio1() <= 0) return false; }
            default -> {}
        }

        return true;
    }

    private boolean matchField(String value, String filter, boolean anyPosition) {
        if (value == null) return false;
        String v = value.toLowerCase(Locale.ROOT);
        String f = filter.toLowerCase(Locale.ROOT);
        return anyPosition ? v.contains(f) : v.startsWith(f);
    }

    private boolean matches(String val) {
        return val != null && val.toLowerCase(Locale.ROOT).contains(filterText);
    }
}

package com.droai.ui;

import com.droai.model.CargaMasivaCostosPreciosRow;
import com.droai.service.CargaMasivaCostosPreciosService;
import com.droai.ui.components.Toast;
import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class CargaMasivaCostosPreciosPanel extends JPanel {

    private final CargaMasivaCostosPreciosService service;
    private final JTable tablePreview;
    private final CargaMasivaTableModel tableModel;

    private final JButton btnCargarExcel;
    private final JButton btnAplicar;
    private final JTextField txtFiltro;
    private final JLabel lblArchivoInfo;
    private final JLabel lblTasaInfo;
    private final JLabel lblEstadisticas;

    private List<CargaMasivaCostosPreciosRow> allRows = new ArrayList<>();
    private List<CargaMasivaCostosPreciosRow> displayedRows = new ArrayList<>();
    private File selectedFile;

    private Runnable onCargaExitosa;

    public List<CargaMasivaCostosPreciosRow> getDisplayedRows() {
        return displayedRows;
    }

    public CargaMasivaCostosPreciosPanel() {
        this.service = new CargaMasivaCostosPreciosService();
        setLayout(new MigLayout("insets 16, fill, wrap", "[grow]", "[]12[grow]12[]"));

        // ── Header Panel ──
        JPanel pnlHeader = new JPanel(new MigLayout("insets 10 16 10 16, fillx", "[]12[]12[]push[]", "[]"));
        pnlHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Component.borderColor")));

        btnCargarExcel = new JButton("Seleccionar Archivo Excel (.xlsx)");
        btnCargarExcel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnCargarExcel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCargarExcel.addActionListener(e -> seleccionarYProcesarExcel());
        pnlHeader.add(btnCargarExcel);

        lblArchivoInfo = new JLabel("No se ha seleccionado ningún archivo Excel");
        lblArchivoInfo.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblArchivoInfo.setForeground(UIManager.getColor("Label.disabledForeground"));
        pnlHeader.add(lblArchivoInfo);

        lblTasaInfo = new JLabel("Tasa Profit (saTasa): ---");
        lblTasaInfo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTasaInfo.setForeground(new Color(30, 136, 229));
        pnlHeader.add(lblTasaInfo);

        txtFiltro = new JTextField();
        txtFiltro.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Filtrar por código o descripción...");
        txtFiltro.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtFiltro.addActionListener(e -> filtrarTabla());
        txtFiltro.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                filtrarTabla();
            }
        });
        pnlHeader.add(txtFiltro, "w 280!");

        add(pnlHeader, "growx");

        // ── Tabla Preview ──
        tableModel = new CargaMasivaTableModel();
        tablePreview = new JTable(tableModel);
        tablePreview.setRowHeight(28);
        tablePreview.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tablePreview.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tablePreview.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Formato numérico para costos y precios
        DecimalFormat df = new DecimalFormat("#,##0.00");
        DefaultTableCellRenderer numericRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value instanceof Number) {
                    setText(df.format(((Number) value).doubleValue()));
                }
                setHorizontalAlignment(SwingConstants.RIGHT);
                return c;
            }
        };

        for (int c = 2; c <= 9; c++) {
            tablePreview.getColumnModel().getColumn(c).setCellRenderer(numericRenderer);
        }

        // Renderer de Estado con colores
        tablePreview.getColumnModel().getColumn(10).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setFont(new Font("Segoe UI", Font.BOLD, 11));

                String estado = value != null ? value.toString() : "";
                if (estado.contains("Listo") || estado.contains("éxito") || estado.contains("exito")) {
                    label.setForeground(new Color(46, 125, 50));
                } else if (estado.contains("no existe") || estado.contains("Error") || estado.contains("vacío")) {
                    label.setForeground(new Color(198, 40, 40));
                } else if (estado.contains("Sin cambios")) {
                    label.setForeground(new Color(230, 81, 0));
                } else {
                    label.setForeground(UIManager.getColor("Label.foreground"));
                }
                return label;
            }
        });

        JScrollPane scrollTable = new JScrollPane(tablePreview);
        scrollTable.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, UIManager.getColor("Component.borderColor")));
        add(scrollTable, "grow");

        // ── Footer Panel ──
        JPanel pnlFooter = new JPanel(new MigLayout("insets 10 16 10 16, fillx", "[]push[]", "[]"));
        pnlFooter.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("Component.borderColor")));

        lblEstadisticas = new JLabel("Cargue un archivo Excel para ver la vista previa y estadísticas.");
        lblEstadisticas.setFont(new Font("Segoe UI", Font.BOLD, 12));
        pnlFooter.add(lblEstadisticas);

        btnAplicar = new JButton("Aplicar Carga Masiva");
        btnAplicar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnAplicar.setBackground(UIManager.getColor("Component.accentColor"));
        btnAplicar.setForeground(Color.WHITE);
        btnAplicar.setFocusPainted(false);
        btnAplicar.setEnabled(false);
        btnAplicar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnAplicar.addActionListener(e -> aplicarCargaMasiva());
        pnlFooter.add(btnAplicar);

        add(pnlFooter, "growx");
    }

    public void setOnCargaExitosa(Runnable listener) {
        this.onCargaExitosa = listener;
    }

    private void seleccionarYProcesarExcel() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Seleccionar Archivo Excel de Costos y Precios");
        fc.setFileFilter(new FileNameExtensionFilter("Archivos Excel (*.xlsx)", "xlsx"));

        int res = fc.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            selectedFile = fc.getSelectedFile();
            lblArchivoInfo.setText("Archivo: " + selectedFile.getName());

            btnCargarExcel.setEnabled(false);
            lblEstadisticas.setText("Leyendo archivo Excel y consultando base de datos...");

            SwingWorker<List<CargaMasivaCostosPreciosRow>, Void> worker = new SwingWorker<>() {
                @Override
                protected List<CargaMasivaCostosPreciosRow> doInBackground() throws Exception {
                    return service.cargarDesdeExcel(selectedFile);
                }

                @Override
                protected void done() {
                    btnCargarExcel.setEnabled(true);
                    try {
                        allRows = get();
                        if (!allRows.isEmpty()) {
                            double tasaUsd = allRows.get(0).getTasaUsd();
                            lblTasaInfo.setText(String.format("Tasa Profit (saTasa): %.4f Bs/$", tasaUsd));
                        }
                        filtrarTabla();
                        actualizarEstadisticas();
                        Toast.show("Vista previa cargada correctamente: " + allRows.size() + " registros.", Toast.Type.SUCCESS);
                    } catch (Exception ex) {
                        lblEstadisticas.setText("Error al procesar Excel.");
                        JOptionPane.showMessageDialog(CargaMasivaCostosPreciosPanel.this,
                                "Error al cargar el archivo Excel:\n" + ex.getMessage(),
                                "Error de Carga", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        }
    }

    private void filtrarTabla() {
        String query = txtFiltro.getText() != null ? txtFiltro.getText().trim().toLowerCase() : "";
        displayedRows.clear();

        for (CargaMasivaCostosPreciosRow row : allRows) {
            if (query.isEmpty()
                    || (row.getCoArt() != null && row.getCoArt().toLowerCase().contains(query))
                    || (row.getDescripcion() != null && row.getDescripcion().toLowerCase().contains(query))) {
                displayedRows.add(row);
            }
        }
        tableModel.fireTableDataChanged();
    }

    private void actualizarEstadisticas() {
        int total = allRows.size();
        int validos = 0;
        int conCambios = 0;
        int errores = 0;

        for (CargaMasivaCostosPreciosRow row : allRows) {
            if (!row.isExisteEnBd() || !row.isValido()) {
                errores++;
            } else {
                validos++;
                if (row.tieneCambios()) {
                    conCambios++;
                }
            }
        }

        lblEstadisticas.setText(String.format("Total Filas: %d  |  Válidos: %d  |  Con Cambios a Aplicar: %d  |  Errores/No Encontrados: %d",
                total, validos, conCambios, errores));

        btnAplicar.setEnabled(conCambios > 0);
    }

    private void aplicarCargaMasiva() {
        List<CargaMasivaCostosPreciosRow> porActualizar = new ArrayList<>();
        for (CargaMasivaCostosPreciosRow r : allRows) {
            if (r.isValido() && r.isExisteEnBd() && r.tieneCambios()) {
                porActualizar.add(r);
            }
        }

        if (porActualizar.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay filas válidas con cambios pendientes por aplicar.", "Sin Cambios", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Se aplicará la carga masiva para " + porActualizar.size() + " productos en la base de datos (DROA_A).\n¿Desea continuar con la actualización?",
                "Confirmar Carga Masiva", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        btnAplicar.setEnabled(false);
        btnCargarExcel.setEnabled(false);
        lblEstadisticas.setText("⏳ Aplicando cambios en lote en la base de datos...");

        SwingWorker<Integer, Void> worker = new SwingWorker<>() {
            @Override
            protected Integer doInBackground() throws Exception {
                return service.ejecutarCargaMasiva(porActualizar);
            }

            @Override
            protected void done() {
                btnCargarExcel.setEnabled(true);
                try {
                    int procesados = get();
                    actualizarEstadisticas();
                    tableModel.fireTableDataChanged();

                    JOptionPane.showMessageDialog(CargaMasivaCostosPreciosPanel.this,
                            "¡Carga masiva completada con éxito!\n\nSe actualizaron " + procesados + " productos en DROA_A.",
                            "Carga Éxito", JOptionPane.INFORMATION_MESSAGE);

                    Toast.show("✔ Carga masiva ejecutada: " + procesados + " productos actualizados.", Toast.Type.SUCCESS);

                    if (onCargaExitosa != null) {
                        onCargaExitosa.run();
                    }
                } catch (Exception ex) {
                    actualizarEstadisticas();
                    JOptionPane.showMessageDialog(CargaMasivaCostosPreciosPanel.this,
                            "Error al ejecutar la carga masiva en la base de datos:\n" + ex.getMessage(),
                            "Error BD", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private class CargaMasivaTableModel extends AbstractTableModel {
        private final String[] COLUMNS = {
                "Código", "Descripción del Producto",
                "Costo Act. ($)", "Costo Act. (Bs)", "Nuevo Costo ($)", "Nuevo Costo (Bs)",
                "Precio 1 Act. ($)", "Precio 1 Act. (Bs)", "Nuevo Precio 1 ($)", "Nuevo Precio 1 (Bs)",
                "Estado"
        };

        @Override
        public int getRowCount() {
            return displayedRows.size();
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
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex < 0 || rowIndex >= displayedRows.size()) return null;
            CargaMasivaCostosPreciosRow row = displayedRows.get(rowIndex);

            return switch (columnIndex) {
                case 0 -> row.getCoArt();
                case 1 -> row.getDescripcion();
                case 2 -> row.getCostoActualUsd();
                case 3 -> row.getCostoActualBs();
                case 4 -> row.getCostoNuevoUsd();
                case 5 -> row.getCostoNuevoBs();
                case 6 -> row.getPrecio1ActualUsd();
                case 7 -> row.getPrecio1ActualBs();
                case 8 -> row.getPrecio1NuevoUsd();
                case 9 -> row.getPrecio1NuevoBs();
                case 10 -> row.getEstado();
                default -> null;
            };
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex >= 2 && columnIndex <= 9) {
                return Double.class;
            }
            return String.class;
        }
    }
}

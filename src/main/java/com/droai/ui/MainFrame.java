package com.droai.ui;

import com.droai.config.DatabaseConfig;
import com.droai.model.MatrizVentasRow;
import com.droai.service.MatrizVentasService;
import com.droai.export.ExcelExporter;
import com.droai.ui.components.Toast;
import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.util.List;

/**
 * Ventana principal de DroAI — Módulo Matriz de Ventas.
 * Actualizado para utilizar MatrizVentasService y MatrizVentasTableModel.
 */
public class MainFrame extends JFrame {

    private static final Color BG = new Color(26, 29, 36);

    private final HeaderPanel header;
    private final DataTabbedPane dataTabs;
    private final FooterPanel footer;
    private final MatrizVentasService service;

    public MainFrame() {
        setTitle("DroAI — Matriz de Ventas");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1540, 920);
        setMinimumSize(new Dimension(1200, 750));
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);

        try {
            ImageIcon icon = new ImageIcon(getClass().getResource("/images/logo.png"));
            setIconImage(icon.getImage());
        } catch (Exception ignored) {
        }

        Toast.setParentFrame(this);
        service = new MatrizVentasService();

        JPanel root = new JPanel(new MigLayout("insets 0, fill, wrap", "[grow]", "[]0[grow]0[]"));
        root.setBackground(BG);

        dataTabs = new DataTabbedPane();
        footer = new FooterPanel();
        footer.setOnExportar(this::exportExcel);

        header = new HeaderPanel();
        header.setOnSearch(query -> {
            dataTabs.getMatrizModel().setFilter(query);
            footer.setRegistroCount(dataTabs.getMatrizModel().getRowCount());
        });
        header.setOnFiltrar(this::loadData);
        header.setOnGuardar(this::saveData);
        header.setOnDeshacer(this::loadData);
        root.add(header, "growx");

        root.add(dataTabs, "grow");
        root.add(footer, "growx");

        setContentPane(root);

        getRootPane().putClientProperty(FlatClientProperties.TITLE_BAR_BACKGROUND, BG);
        getRootPane().putClientProperty(FlatClientProperties.TITLE_BAR_FOREGROUND, Color.WHITE);

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return DatabaseConfig.testConnection();
            }

            @Override
            protected void done() {
                try {
                    boolean ok = get();
                    if (ok) {
                        Toast.show("✔ Conexión a la base de datos exitosa", Toast.Type.SUCCESS);
                        loadData();
                    } else {
                        Toast.show("✘ La conexión a la base de datos no es válida", Toast.Type.ERROR);
                    }
                } catch (Exception ex) {
                    Toast.show("✘ Error de conexión: " + ex.getMessage(), Toast.Type.ERROR);
                }
            }
        }.execute();
    }

    private void loadData() {
        Toast.show("Consultando Matriz de Ventas...", Toast.Type.INFO);
        new SwingWorker<List<MatrizVentasRow>, Void>() {
            @Override
            protected List<MatrizVentasRow> doInBackground() throws Exception {
                // Rango por defecto: último mes
                LocalDate from = LocalDate.now().minusMonths(1);
                LocalDate to = LocalDate.now();
                return service.obtenerMatrizVentas(from, to);
            }

            @Override
            protected void done() {
                try {
                    List<MatrizVentasRow> rows = get();
                    dataTabs.getMatrizModel().setData(rows);
                    footer.setRegistroCount(rows.size());
                    Toast.show("Matriz cargada: " + rows.size() + " registros", Toast.Type.SUCCESS);
                    loadResumenTabs();
                } catch (Exception ex) {
                    Toast.show("Error al cargar matriz: " + ex.getMessage(), Toast.Type.ERROR);
                }
            }
        }.execute();
    }

    private void loadResumenTabs() {
        // Implementación pendiente de DAOs de resumen actualizados si se requiere
    }

    private void saveData() {
        Toast.show("La Matriz de Ventas es de solo lectura", Toast.Type.INFO);
    }

    private void exportExcel() {
        Toast.show("Generando reporte Excel...", Toast.Type.INFO);
        new SwingWorker<File, Void>() {
            @Override
            protected File doInBackground() throws Exception {
                ExcelExporter exporter = new ExcelExporter();
                // Nota: ExcelExporter debe actualizarse para aceptar List<MatrizVentasRow>
                return exporter.exportMatriz(
                        dataTabs.getMatrizModel().getAllData(),
                        footer.getTasa());
            }

            @Override
            protected void done() {
                try {
                    File file = get();
                    Toast.show("Excel exportado correctamente", Toast.Type.SUCCESS);
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().open(file);
                    }
                } catch (Exception ex) {
                    Toast.show("Error en exportación: " + ex.getMessage(), Toast.Type.ERROR);
                }
            }
        }.execute();
    }
}
package com.droai.ui;

import com.droai.config.DatabaseConfig;
import com.droai.model.ArticuloRow;
import com.droai.service.CatalogoService;
import com.droai.export.ExcelExporter;
import com.droai.ui.components.Toast;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;

public class MainFrame extends JFrame {

    private final HeaderPanel header;
    private final DataTabbedPane dataTabs;
    private final FooterPanel footer;
    private final CatalogoService service;
    private boolean isDarkTheme = true;

    public MainFrame() {
        setTitle("DroAI — Catálogo de Productos");
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
        service = new CatalogoService();

        JPanel root = new JPanel(new MigLayout("insets 0, fill, wrap", "[grow]", "[]0[grow]0[]"));

        dataTabs = new DataTabbedPane();
        footer = new FooterPanel();
        footer.setOnExportar(this::exportExcel);

        header = new HeaderPanel();
        header.setOnSearch(query -> {
            dataTabs.getCatalogoModel().setFilter(query);
            footer.setRegistroCount(dataTabs.getCatalogoModel().getRowCount());
        });
        header.setOnFiltrar(this::loadData);
        header.setOnGuardar(this::saveData);
        header.setOnDeshacer(this::loadData);
        header.setOnCambiarTema(this::toggleTheme);
        root.add(header, "growx");

        root.add(dataTabs, "grow");
        root.add(footer, "growx");

        setContentPane(root);

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
                    ex.printStackTrace();
                    Toast.show("✘ Error de conexión. Revisa la consola.", Toast.Type.ERROR);
                }
            }
        }.execute();
    }

    private void loadData() {
        Toast.show("Consultando Catálogo de Productos...", Toast.Type.INFO);
        new SwingWorker<List<ArticuloRow>, Void>() {
            @Override
            protected List<ArticuloRow> doInBackground() throws Exception {
                return service.obtenerCatalogo();
            }

            @Override
            protected void done() {
                try {
                    List<ArticuloRow> rows = get();
                    dataTabs.getCatalogoModel().setData(rows);
                    footer.setRegistroCount(rows.size());
                    Toast.show("Catálogo cargado: " + rows.size() + " artículos", Toast.Type.SUCCESS);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Toast.show("Error al cargar catálogo. Revisa la consola.", Toast.Type.ERROR);
                }
            }
        }.execute();
    }

    private void saveData() {
        Toast.show("El catálogo es de solo lectura", Toast.Type.INFO);
    }

    private void exportExcel() {
        Toast.show("Generando reporte Excel...", Toast.Type.INFO);
        new SwingWorker<File, Void>() {
            @Override
            protected File doInBackground() throws Exception {
                ExcelExporter exporter = new ExcelExporter();
                return exporter.exportCatalogo(
                        dataTabs.getCatalogoModel().getAllData(),
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
                    ex.printStackTrace();
                    Toast.show("Error en exportación. Revisa la consola.", Toast.Type.ERROR);
                }
            }
        }.execute();
    }

    private void toggleTheme() {
        isDarkTheme = !isDarkTheme;
        try {
            if (isDarkTheme) {
                FlatDarkLaf.setup();
            } else {
                FlatLightLaf.setup();
            }
            FlatLaf.updateUI();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
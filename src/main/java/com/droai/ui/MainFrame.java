package com.droai.ui;

import com.droai.config.DatabaseConfig;
import com.droai.model.ArticuloRow;
import com.droai.model.FiltrosCriteria;
import com.droai.service.CatalogoService;
import com.droai.export.ExcelExporter;
import com.droai.ui.components.Toast;
import com.droai.ui.dialog.FichaProductoDialog;
import com.droai.ui.dialog.FiltrosDialog;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        footer.setOnFichaProducto(this::openFichaProducto);

        header = new HeaderPanel();
        header.setOnSearch(query -> {
            dataTabs.getCatalogoModel().setFilter(query);
            footer.setRegistroCount(dataTabs.getCatalogoModel().getRowCount());
        });
        header.setOnFiltrar(this::abrirFiltros);
        header.setOnGuardar(this::saveData);
        header.setOnDeshacer(this::loadData);
        header.setOnCambiarTema(this::toggleTheme);
        header.setOnSortChanged(criterio -> {
            dataTabs.getCatalogoModel().ordenarPor(criterio);
        });
        root.add(header, "growx");

        footer.setOnColumna3Changed(columna -> {
            header.setTercerRadioText(columna);
            dataTabs.getCatalogoModel().setColumnaDinamica(columna);
            // Opcional: ordenar si es necesario al cambiar la columna
            dataTabs.getCatalogoModel().ordenarPor(columna);
        });

        footer.setOnVerExistenciaChanged(show -> {
            dataTabs.getCatalogoModel().setShowExistencia(show);
        });

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

    private void abrirFiltros() {
        FiltrosCriteria anterior = dataTabs.getCatalogoModel().getFiltrosCriteria();
        FiltrosDialog dialog = new FiltrosDialog(this, anterior);
        dialog.setVisible(true);

        FiltrosCriteria result = dialog.getResultado();
        if (result != null) {
            dataTabs.getCatalogoModel().setFiltrosCriteria(result);
            footer.setRegistroCount(dataTabs.getCatalogoModel().getRowCount());

            if (result.isEmpty()) {
                Toast.show("Filtros eliminados", Toast.Type.INFO);
            } else {
                Toast.show("Filtros aplicados: " + dataTabs.getCatalogoModel().getRowCount() + " resultados",
                        Toast.Type.SUCCESS);
            }
        }
    }

    private void saveData() {
        Toast.show("El catálogo es de solo lectura", Toast.Type.INFO);
    }

    private void openFichaProducto() {
        javax.swing.JTable table = dataTabs.getCatalogoTable();
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            Toast.show("Selecciona un producto de la tabla", Toast.Type.WARNING);
            return;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        ArticuloRow selected = dataTabs.getCatalogoModel().getFilteredData().get(modelRow);
        FichaProductoDialog dialog = new FichaProductoDialog(this, selected);
        dialog.setVisible(true);
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
            Map<String, String> palette = new HashMap<>();
            if (isDarkTheme) {
                palette.put("@background", "#11151C");
                palette.put("@control", "#1E232E");
                palette.put("@accentColor", "#2A6BFF");
                palette.put("Button.default.background", "#00D29E");
                palette.put("@foreground", "#F8FAFC");
                FlatLaf.setGlobalExtraDefaults(palette);
                FlatDarkLaf.setup();
            } else {
                palette.put("@background", "#F1F5F9");
                palette.put("@control", "#FFFFFF");
                palette.put("@accentColor", "#1D4ED8");
                palette.put("Button.default.background", "#059669");
                palette.put("@foreground", "#0F172A");
                FlatLaf.setGlobalExtraDefaults(palette);
                FlatLightLaf.setup();
            }
            FlatLaf.updateUI();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
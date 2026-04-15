package com.droai.ui;

import com.droai.config.DatabaseConfig;
import com.droai.dao.PrecioDAO;
import com.droai.model.FacturaRow;
import com.droai.model.ResumenRow;
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
 * Ventana principal de DroAI — Módulo Listado de Precios.
 * Layout: North(Header) → Center(DataTabbedPane) → South(Footer).
 */
public class MainFrame extends JFrame {

    private static final Color BG = new Color(26, 29, 36);

    private final HeaderPanel header;
    private final DataTabbedPane dataTabs;
    private final FooterPanel footer;
    private final PrecioDAO dao;

    public MainFrame() {
        setTitle("DroAI — Listado de Precios");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1540, 920);
        setMinimumSize(new Dimension(1200, 750));
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);

        // Window + taskbar icon
        try {
            ImageIcon icon = new ImageIcon(getClass().getResource("/images/logo.png"));
            setIconImage(icon.getImage());
        } catch (Exception ignored) {}

        Toast.setParentFrame(this);
        dao = new PrecioDAO();

        // ------ Root panel (no gaps between zones) ------
        JPanel root = new JPanel(new MigLayout("insets 0, fill, wrap", "[grow]", "[]0[grow]0[]"));
        root.setBackground(BG);

        // ------ Create components first (needed for cross-references) ------
        dataTabs = new DataTabbedPane();
        footer = new FooterPanel();
        footer.setOnExportar(this::exportExcel);

        // ------ Header ------
        header = new HeaderPanel();
        header.setOnSearch(query -> {
            dataTabs.getListadoModel().setFilter(query);
            footer.setRegistroCount(dataTabs.getListadoModel().getRowCount());
        });
        header.setOnFiltrar(this::loadData);
        header.setOnGuardar(this::saveData);
        header.setOnDeshacer(this::loadData);
        root.add(header, "growx");

        // ------ Data Tabs (directly after header, no separator gap) ------
        root.add(dataTabs, "grow");

        // ------ Footer ------
        root.add(footer, "growx");

        setContentPane(root);

        // Title bar styling
        getRootPane().putClientProperty(FlatClientProperties.TITLE_BAR_BACKGROUND, BG);
        getRootPane().putClientProperty(FlatClientProperties.TITLE_BAR_FOREGROUND, Color.WHITE);

        // Verify DB connection in background and show Toast result
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
                        loadDemoData();
                    }
                } catch (Exception ex) {
                    String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                    Toast.show("✘ Sin conexión a BD: " + msg, Toast.Type.ERROR);
                    loadDemoData();
                }
            }
        }.execute();
    }

    // ========== DATA OPERATIONS ==========

    /** Carga datos de la BD de forma asíncrona. */
    private void loadData() {
        Toast.show("Consultando datos...", Toast.Type.INFO);
        new SwingWorker<List<FacturaRow>, Void>() {
            @Override
            protected List<FacturaRow> doInBackground() throws Exception {
                LocalDate from = LocalDate.now().minusMonths(1);
                LocalDate to   = LocalDate.now();
                return dao.fetchListado(from, to);
            }

            @Override
            protected void done() {
                try {
                    List<FacturaRow> rows = get();
                    dataTabs.getListadoModel().setData(rows);
                    footer.setRegistroCount(rows.size());
                    Toast.show("Cargados " + rows.size() + " registros", Toast.Type.SUCCESS);

                    // Also load summary tabs
                    loadResumenTabs();
                } catch (Exception ex) {
                    Toast.show("Error al consultar: " + ex.getMessage(), Toast.Type.ERROR);
                }
            }
        }.execute();
    }

    private void loadResumenTabs() {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                LocalDate from = LocalDate.now().minusMonths(1);
                LocalDate to   = LocalDate.now();
                List<ResumenRow> dv  = dao.fetchDescuentosVolumen(from, to);
                List<ResumenRow> dp  = dao.fetchDescuentosProducto(from, to);
                SwingUtilities.invokeLater(() -> {
                    dataTabs.getDctoVolumenModel().setData(dv);
                    dataTabs.getDctoProductoModel().setData(dp);
                });
                return null;
            }
        }.execute();
    }

    /** Guarda filas modificadas a la BD. */
    private void saveData() {
        Toast.show("No se puede editar directamente la Matriz de Ventas histórica", Toast.Type.INFO);
    }

    /** Exporta a Excel en background. */
    private void exportExcel() {
        Toast.show("Generando Excel...", Toast.Type.INFO);
        new SwingWorker<File, Void>() {
            @Override
            protected File doInBackground() throws Exception {
                ExcelExporter exporter = new ExcelExporter();
                return exporter.export(
                    dataTabs.getListadoModel().getAllData(),
                    dataTabs.getDctoVolumenModel().getData(),
                    dataTabs.getDctoProductoModel().getData(),
                    footer.getTasa()
                );
            }

            @Override
            protected void done() {
                try {
                    File file = get();
                    Toast.show("Excel exportado: " + file.getName(), Toast.Type.SUCCESS);
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().open(file);
                    }
                } catch (Exception ex) {
                    Toast.show("Error exportando: " + ex.getMessage(), Toast.Type.ERROR);
                }
            }
        }.execute();
    }

    // ========== DEMO DATA (sin BD) ==========

    private void loadDemoData() {
        // Enforce using live database connection for proper testing limit!
        dataTabs.getListadoModel().setData(new java.util.ArrayList<>());
        footer.setRegistroCount(0);
        Toast.show("Sin conexión. Conecta a la Base de Datos para ver la Matriz de Ventas.", Toast.Type.ERROR);
    }
}

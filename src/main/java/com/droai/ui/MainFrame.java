package com.droai.ui;

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

        // Load sample data on startup (demo)
        SwingUtilities.invokeLater(this::loadDemoData);
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
        List<FacturaRow> modified = dataTabs.getListadoModel().getModifiedRows();
        if (modified.isEmpty()) {
            Toast.show("No hay cambios para guardar", Toast.Type.INFO);
            return;
        }
        Toast.show("Guardando " + modified.size() + " registros...", Toast.Type.INFO);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                dao.updatePrecios(modified);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    Toast.show("Guardado exitoso", Toast.Type.SUCCESS);
                } catch (Exception ex) {
                    Toast.show("Error al guardar: " + ex.getMessage(), Toast.Type.ERROR);
                }
            }
        }.execute();
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
        List<FacturaRow> demo = new java.util.ArrayList<>();
        String[][] items = {
            {"004544", "ATORVASTATINA TAB 40MG X 1", "890800703"},
            {"004545", "CEFADROXILO CAPS 500MG X 1", "890612099"},
            {"004546", "LORATADINA/BETAMETASON/A", "890800703"},
            {"004547", "LEVONORGESTREL TAB 1.5MG", "890612099"},
            {"004548", "AMOXICILINA/ACIDO CLAVULA", "890612099"},
            {"004549", "LOPERAMIDA CAPS 2MG X 10", "890800703"},
            {"004550", "BETAHISTINA TAB 16MG X 10", "890800703"},
            {"004551", "CREMA ANTISEPTICA SANALO", "759113807"},
            {"004552", "CREMA ANTISEPTICA SANALO", "759113807"},
            {"004553", "JAB ANTIBACTERIAL SANALO",  "759113807"},
            {"004554", "ALCOHOL SANOL X 1000ML VAL", "759113807"},
            {"004555", "ALCOHOL SANOL X 500ML VAR", "759113807"},
            {"004556", "ALCOHOL SANOL X 240ML VAR", "759113807"},
            {"004557", "ALCOHOL SANOL X 120ML VAR", "759113807"},
        };
        double[][] nums = {
            {0.00, 0, 0, 2.24, 23.02, 2.91, 0.00, 2.91},
            {149.00, 0, 0, 4.65, 22.97, 6.04, 0.00, 6.04},
            {139.00, 0, 0, 2.77, 22.94, 3.60, 0.00, 3.60},
            {20.00, 0, 0, 2.35, 23.11, 3.05, 0.00, 3.05},
            {43.00, 0, 0, 12.45, 22.98, 16.16, 0.00, 16.16},
            {105.00, 0, 0, 2.70, 23.03, 3.51, 0.00, 3.51},
            {72.00, 0, 0, 2.70, 22.98, 3.51, 0.00, 3.51},
            {96.00, 0, 0, 4.50, 18.03, 5.49, 16.00, 6.36},
            {144.00, 0, 0, 3.00, 18.03, 3.66, 16.00, 4.24},
            {72.00, 0, 0, 2.30, 17.86, 2.80, 16.00, 3.24},
            {0.00, 0, 0, 3.28, 18.00, 4.00, 0.00, 4.00},
            {0.00, 0, 0, 2.20, 17.91, 2.68, 0.00, 2.68},
            {0.00, 0, 0, 1.06, 17.83, 1.29, 0.00, 1.29},
            {0.00, 0, 0, 0.60, 17.81, 0.73, 0.00, 0.73},
        };
        for (int i = 0; i < items.length; i++) {
            demo.add(new FacturaRow(
                items[i][0], items[i][1], items[i][2],
                nums[i][0], "UND",
                nums[i][3], nums[i][2], nums[i][3],
                nums[i][4], nums[i][5], nums[i][6], nums[i][7]
            ));
        }
        dataTabs.getListadoModel().setData(demo);
        footer.setRegistroCount(demo.size());
    }
}

package com.droai.ui;

import com.droai.config.DatabaseConfig;
import com.droai.model.ArticuloRow;
import com.droai.model.DescuentoProductoRow;
import com.droai.model.DescuentoVolumenRow;
import com.droai.model.FiltrosCriteria;
import com.droai.model.ProductoReporteRow;
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
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.droai.service.ImportadorService;

public class MainFrame extends JFrame {

    private final HeaderPanel header;
    private final DataTabbedPane dataTabs;
    private final FooterPanel footer;
    private final CatalogoService service;
    private boolean isDarkTheme = true;

    public MainFrame() {
        setTitle("DroAI — Catálogo de Productos");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
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
            dataTabs.getDctoVolumenModel().setFilterText(query);
            dataTabs.getDctoProductoModel().setFilterText(query);
            actualizarFooterConPestanaActiva();
        });
        header.setOnFiltrar(this::abrirFiltros);
        header.setOnGuardar(this::saveData);
        header.setOnDeshacer(this::loadData);
        header.setOnCambiarTema(this::toggleTheme);
        header.setOnSortChanged(criterio -> {
            dataTabs.getCatalogoModel().ordenarPor(criterio);
        });
        root.add(header, "growx");

        // Escuchar cambios de pestaña para actualizar el footer de registros
        dataTabs.addChangeListener(e -> actualizarFooterConPestanaActiva());

        // Escuchar los botones para aplicar descuento masivo
        dataTabs.getBtnDVAplicar().addActionListener(e -> aplicarDescuentoDV());
        dataTabs.getBtnDVImportExcel().addActionListener(e -> cargarDescuentoDVDesdeExcel());
        dataTabs.getBtnDPAplicar().addActionListener(e -> aplicarDescuentoDP());
        dataTabs.getBtnDPImportExcel().addActionListener(e -> cargarDescuentoDPDesdeExcel());

        dataTabs.getCargaMasivaPanel().setOnCargaExitosa(this::loadData);

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
        Toast.show("Consultando Catálogo de Productos y Descuentos...", Toast.Type.INFO);
        new SwingWorker<Boolean, Void>() {
            private List<ArticuloRow> catalogoRows;
            private List<DescuentoVolumenRow> dvRows;
            private List<DescuentoProductoRow> dpRows;

            @Override
            protected Boolean doInBackground() throws Exception {
                catalogoRows = service.obtenerCatalogo();
                dvRows = service.obtenerDescuentosVolumen();
                dpRows = service.obtenerDescuentosProducto();
                return true;
            }

            @Override
            protected void done() {
                try {
                    get();
                    dataTabs.getCatalogoModel().setData(catalogoRows);
                    dataTabs.getDctoVolumenModel().setData(dvRows);
                    dataTabs.getDctoProductoModel().setData(dpRows);
                    actualizarFooterConPestanaActiva();
                    Toast.show("Catálogo y descuentos cargados con éxito", Toast.Type.SUCCESS);
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

        if (dialog.isFiltrosEliminados()) {
            dataTabs.getCatalogoModel().setFiltrosCriteria(null);
            dataTabs.getDctoVolumenModel().setFilterMarca(null);
            dataTabs.getDctoProductoModel().setFilterMarca(null);
            actualizarFooterConPestanaActiva();
            Toast.show("Filtros eliminados", Toast.Type.INFO);
        } else {
            FiltrosCriteria result = dialog.getResultado();
            if (result != null) {
                dataTabs.getCatalogoModel().setFiltrosCriteria(result);
                dataTabs.getDctoVolumenModel().setFilterMarca(result.getMarca());
                dataTabs.getDctoProductoModel().setFilterMarca(result.getMarca());
                actualizarFooterConPestanaActiva();
                Toast.show("Filtros aplicados", Toast.Type.SUCCESS);
            }
        }
    }

    private void actualizarFooterConPestanaActiva() {
        int index = dataTabs.getSelectedIndex();
        int count = switch (index) {
            case 0 -> dataTabs.getCatalogoModel().getRowCount();
            case 2 -> dataTabs.getDctoVolumenModel().getRowCount();
            case 3 -> dataTabs.getDctoProductoModel().getRowCount();
            default -> 0;
        };
        footer.setRegistroCount(count);
    }

    private void aplicarDescuentoDV() {
        List<String> codigos = dataTabs.getDctoVolumenModel().getSelectedCodigos();
        if (codigos.isEmpty()) {
            Toast.show("Selecciona al menos un producto de la lista", Toast.Type.WARNING);
            return;
        }

        String pctStr = dataTabs.getTxtDVPorcentaje().getText().trim();
        double porcentaje;
        try {
            porcentaje = Double.parseDouble(pctStr);
            if (porcentaje < 0 || porcentaje > 100) {
                Toast.show("El porcentaje debe estar entre 0 y 100", Toast.Type.WARNING);
                return;
            }
        } catch (NumberFormatException e) {
            Toast.show("Porcentaje de descuento inválido", Toast.Type.WARNING);
            return;
        }

        java.time.LocalDate localIni = dataTabs.getDateDVFechaIni().getDate();
        java.time.LocalDate localFin = dataTabs.getDateDVFechaFin().getDate();

        java.sql.Date fechaIni = (localIni != null) ? java.sql.Date.valueOf(localIni) : null;
        java.sql.Date fechaFin = (localFin != null) ? java.sql.Date.valueOf(localFin) : null;

        if (fechaIni != null && fechaFin != null && fechaFin.before(fechaIni)) {
            Toast.show("La fecha 'Hasta' no puede ser anterior a la fecha 'Desde'", Toast.Type.WARNING);
            return;
        }

        final java.sql.Date fIni = fechaIni;
        final java.sql.Date fFin = fechaFin;

        String msgConfirm = "¿Deseas aplicar un descuento del " + String.format("%.2f", porcentaje) + "% a los " + codigos.size() + " productos seleccionados?";
        if (fIni != null || fFin != null) {
            msgConfirm += "\nVigencia: [" + (fIni != null ? fIni : "Sin inicio") + "] a [" + (fFin != null ? fFin : "Sin fin") + "]";
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                msgConfirm,
                "Confirmar Descuento Masivo",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        Toast.show("Actualizando descuentos por volumen...", Toast.Type.INFO);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                service.actualizarDescuentosVolumen(codigos, porcentaje, fIni, fFin);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    Toast.show(codigos.size() + " descuentos actualizados correctamente", Toast.Type.SUCCESS);
                    loadData();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Toast.show("Error al actualizar descuentos", Toast.Type.ERROR);
                }
            }
        }.execute();
    }

    private void cargarDescuentoDVDesdeExcel() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Seleccionar Archivo de Descuentos DV");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Archivos Excel (*.xlsx, *.xls)", "xlsx", "xls"));
        
        int userSelection = fileChooser.showOpenDialog(this);
        if (userSelection != JFileChooser.APPROVE_OPTION) {
            return;
        }

        java.time.LocalDate localIni = dataTabs.getDateDVFechaIni().getDate();
        java.time.LocalDate localFin = dataTabs.getDateDVFechaFin().getDate();

        java.sql.Date fechaIni = (localIni != null) ? java.sql.Date.valueOf(localIni) : null;
        java.sql.Date fechaFin = (localFin != null) ? java.sql.Date.valueOf(localFin) : null;

        if (fechaIni != null && fechaFin != null && fechaFin.before(fechaIni)) {
            Toast.show("La fecha 'Hasta' no puede ser anterior a la fecha 'Desde'", Toast.Type.WARNING);
            return;
        }

        final java.sql.Date fIni = fechaIni;
        final java.sql.Date fFin = fechaFin;
        
        File selectedFile = fileChooser.getSelectedFile();
        Toast.show("Leyendo archivo Excel de descuentos...", Toast.Type.INFO);
        
        new SwingWorker<List<ImportadorService.DescuentoDVImportItem>, Void>() {
            private final ImportadorService importadorService = new ImportadorService();

            @Override
            protected List<ImportadorService.DescuentoDVImportItem> doInBackground() throws Exception {
                return importadorService.leerExcelDescuentoDVItems(selectedFile);
            }
            
            @Override
            protected void done() {
                try {
                    List<ImportadorService.DescuentoDVImportItem> items = get();
                    if (items == null || items.isEmpty()) {
                        Toast.show("No se encontraron descuentos válidos en el archivo.", Toast.Type.WARNING);
                        return;
                    }

                    long conFechasExcel = items.stream().filter(i -> i.getFechaIni() != null || i.getFechaFin() != null).count();

                    String msgConfirm = "Se leyeron " + items.size() + " descuentos del archivo Excel.\n";
                    if (conFechasExcel > 0) {
                        msgConfirm += conFechasExcel + " registros contienen su propio rango de fechas en el Excel.\n";
                    }
                    if (fIni != null || fFin != null) {
                        msgConfirm += "Vigencia por defecto UI: [" + (fIni != null ? fIni : "Sin inicio") + "] a [" + (fFin != null ? fFin : "Sin fin") + "]\n";
                    }
                    msgConfirm += "¿Deseas aplicarlos en la base de datos?";
                    
                    int confirm = JOptionPane.showConfirmDialog(MainFrame.this,
                            msgConfirm,
                            "Confirmar Carga de Descuentos",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE);
                            
                    if (confirm != JOptionPane.YES_OPTION) {
                        return;
                    }
                    
                    Toast.show("Actualizando descuentos por volumen...", Toast.Type.INFO);
                    
                    new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() throws Exception {
                            service.actualizarDescuentosVolumenItems(items, fIni, fFin);
                            return null;
                        }
                        
                        @Override
                        protected void done() {
                            try {
                                get();
                                Toast.show("Se actualizaron " + items.size() + " descuentos correctamente.", Toast.Type.SUCCESS);
                                loadData();
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                Toast.show("Error al actualizar descuentos en la base de datos.", Toast.Type.ERROR);
                            }
                        }
                    }.execute();
                    
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Toast.show("Error al leer el archivo Excel: " + ex.getMessage(), Toast.Type.ERROR);
                }
            }
        }.execute();
    }

    private void cargarDescuentoDPDesdeExcel() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Seleccionar Archivo de Descuentos DP");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Archivos Excel (*.xlsx, *.xls)", "xlsx", "xls"));
        
        int userSelection = fileChooser.showOpenDialog(this);
        if (userSelection != JFileChooser.APPROVE_OPTION) {
            return;
        }
        
        File selectedFile = fileChooser.getSelectedFile();
        Toast.show("Leyendo archivo Excel de descuentos...", Toast.Type.INFO);
        
        new SwingWorker<java.util.Map<String, Double>, Void>() {
            private final ImportadorService importadorService = new ImportadorService();

            @Override
            protected java.util.Map<String, Double> doInBackground() throws Exception {
                return importadorService.leerExcelDescuentoDA(selectedFile);
            }
            
            @Override
            protected void done() {
                try {
                    java.util.Map<String, Double> dctosMap = get();
                    if (dctosMap == null || dctosMap.isEmpty()) {
                        Toast.show("No se encontraron descuentos válidos en el archivo.", Toast.Type.WARNING);
                        return;
                    }
                    
                    int confirm = JOptionPane.showConfirmDialog(MainFrame.this,
                            "Se leyeron " + dctosMap.size() + " descuentos DP del archivo Excel.\n¿Deseas aplicarlos en la base de datos?",
                            "Confirmar Carga de Descuentos",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE);
                            
                    if (confirm != JOptionPane.YES_OPTION) {
                        return;
                    }
                    
                    Toast.show("Actualizando descuentos por producto (DP)...", Toast.Type.INFO);
                    
                    new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() throws Exception {
                            service.actualizarDescuentosProductoDAMap(dctosMap);
                            return null;
                        }
                        
                        @Override
                        protected void done() {
                            try {
                                get();
                                Toast.show("Se actualizaron " + dctosMap.size() + " descuentos correctamente.", Toast.Type.SUCCESS);
                                loadData();
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                Toast.show("Error al actualizar descuentos en la base de datos.", Toast.Type.ERROR);
                            }
                        }
                    }.execute();
                    
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Toast.show("Error al leer el archivo Excel: " + ex.getMessage(), Toast.Type.ERROR);
                }
            }
        }.execute();
    }

    private void aplicarDescuentoDP() {
        List<String> codigos = dataTabs.getDctoProductoModel().getSelectedCodigos();
        if (codigos.isEmpty()) {
            Toast.show("Selecciona al menos un producto de la lista", Toast.Type.WARNING);
            return;
        }

        String dctoStr = dataTabs.getTxtDPDcto().getText().trim();
        double dctoDA;
        try {
            dctoDA = Double.parseDouble(dctoStr);
            if (dctoDA < 0 || dctoDA > 100) {
                Toast.show("El porcentaje debe estar entre 0 y 100", Toast.Type.WARNING);
                return;
            }
        } catch (NumberFormatException e) {
            Toast.show("Porcentaje de descuento inválido", Toast.Type.WARNING);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "¿Deseas aplicar un descuento DP del " + String.format("%.2f", dctoDA) + "% a los " + codigos.size() + " productos seleccionados?",
                "Confirmar Descuento por Producto",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        Toast.show("Actualizando descuentos por producto (DP)...", Toast.Type.INFO);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                service.actualizarDescuentosProductoDA(codigos, dctoDA);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    Toast.show(codigos.size() + " descuentos por producto (DP) actualizados correctamente", Toast.Type.SUCCESS);
                    loadData();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Toast.show("Error al actualizar descuentos por producto", Toast.Type.ERROR);
                }
            }
        }.execute();
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
        int mainSelectedIndex = dataTabs.getSelectedIndex();
        boolean isReporte = false;
        boolean isDV = false;
        java.util.List<ProductoReporteRow> reporteData = null;
        java.util.List<DescuentoVolumenRow> dvData = null;
        java.util.List<ArticuloRow> catalogoData = null;

        if (mainSelectedIndex == 4) { // Index of "Importar Datos"
            ImportarPanel panel = dataTabs.getImportarPanel();
            if (panel.isShowingReporteTab()) {
                isReporte = true;
                reporteData = panel.getReporteRowsVisibles();
            }
        } else if (mainSelectedIndex == 2) { // Descuentos x Volumen
            isDV = true;
            dvData = dataTabs.getDctoVolumenRowsVisibles();
        } else if (mainSelectedIndex == 0) { // Catálogo
            catalogoData = dataTabs.getCatalogoRowsVisibles();
        }

        final boolean exportReporte = isReporte;
        final boolean exportDV = isDV;
        final java.util.List<ProductoReporteRow> finalReporteData = reporteData;
        final java.util.List<DescuentoVolumenRow> finalDVData = dvData;
        final java.util.List<ArticuloRow> finalCatalogoData = catalogoData;

        if (exportReporte && (finalReporteData == null || finalReporteData.isEmpty())) {
            Toast.show("No hay datos cargados en el reporte de productos para exportar.", Toast.Type.WARNING);
            return;
        }
        if (exportDV && (finalDVData == null || finalDVData.isEmpty())) {
            Toast.show("No hay datos de descuentos por volumen para exportar.", Toast.Type.WARNING);
            return;
        }
        if (!exportReporte && !exportDV && (finalCatalogoData == null || finalCatalogoData.isEmpty())) {
            Toast.show("No hay datos en el catálogo de productos para exportar.", Toast.Type.WARNING);
            return;
        }

        Toast.show("Generando reporte Excel...", Toast.Type.INFO);
        new SwingWorker<File, Void>() {
            @Override
            protected File doInBackground() throws Exception {
                ExcelExporter exporter = new ExcelExporter();
                if (exportReporte) {
                    return exporter.exportReporteProductos(finalReporteData);
                } else if (exportDV) {
                    return exporter.exportDescuentosVolumen(finalDVData);
                } else {
                    return exporter.exportCatalogo(finalCatalogoData, footer.getTasa());
                }
            }

            @Override
            protected void done() {
                try {
                    File file = get();
                    if (file != null) {
                        Toast.show("Excel exportado correctamente", Toast.Type.SUCCESS);
                        if (Desktop.isDesktopSupported()) {
                            Desktop.getDesktop().open(file);
                        }
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
            dataTabs.updateDatePickerThemes(isDarkTheme);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
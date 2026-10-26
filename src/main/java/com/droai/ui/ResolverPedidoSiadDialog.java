package com.droai.ui;

import com.droai.dao.SiadPedidosDAO;
import com.droai.model.SiadDiagnosticoDTO;
import com.droai.model.SiadDiagnosticoDTO.LoteFEFODTO;
import com.droai.model.SiadDiagnosticoDTO.RenglonDiagnosticoDTO;
import com.droai.ui.components.RoundedPanel;
import com.droai.ui.components.Toast;
import com.droai.ui.util.IconHelper;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.io.File;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Asistente interactivo para diagnosticar, corregir por FEFO, nivelar stock y
 * reprocesar en 1-Clic pedidos que fueron rebotados por SIAD.
 */
public class ResolverPedidoSiadDialog extends JDialog {

    private static final DecimalFormat DF = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.of("es", "VE")));

    private final ThemeManager tm = ThemeManager.get();
    private final SiadPedidosDAO siadDAO = new SiadPedidosDAO();
    private final File archivoTxt;
    private final Runnable onReprocesadoCallback;

    private SiadDiagnosticoDTO diagnostico;

    // UI
    private JLabel lblTituloPedido;
    private JLabel lblErrorSiad;
    private JLabel lblEstadoGeneral;
    private DefaultTableModel modelTable;
    private JTable tblRenglones;

    private JButton btnReasignarFefo;
    private JButton btnNivelarStock;
    private JButton btnAutoFefoAll;
    private JButton btnRevalidar;
    private JButton btnReprocesar;

    public ResolverPedidoSiadDialog(Window parent, File archivoTxt, Runnable onReprocesadoCallback) {
        super(parent, "DroAI — Asistente de Solución de Pedidos en SIAD", ModalityType.APPLICATION_MODAL);
        this.archivoTxt = archivoTxt;
        this.onReprocesadoCallback = onReprocesadoCallback;

        setSize(1180, 720);
        setMinimumSize(new Dimension(950, 600));
        setLocationRelativeTo(parent);

        initUI();
        cargarDiagnostico();
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(tm.background());

        // ═══════════════════════════════════════════════════════════
        // HEADER + BANNER DE ERROR
        // ═══════════════════════════════════════════════════════════
        JPanel topPanel = new JPanel(new MigLayout("insets 16 20 10 20, fillx, wrap", "[grow]", "[]8[]"));
        topPanel.setOpaque(false);

        // Título
        JPanel titleBox = new JPanel(new MigLayout("insets 0, fillx", "[]12[]push[]", "[]"));
        titleBox.setOpaque(false);

        JLabel lblIcon = new JLabel("🔧");
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
        titleBox.add(lblIcon);

        JPanel titleTexts = new JPanel(new MigLayout("insets 0, wrap, gap 0", "[]", "[]2[]"));
        titleTexts.setOpaque(false);
        lblTituloPedido = new JLabel("Diagnóstico y Solución de Pedido Rebotado en SIAD");
        lblTituloPedido.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblTituloPedido.setForeground(tm.textPrimary());
        titleTexts.add(lblTituloPedido);

        JLabel lblSub = new JLabel("Analiza inconsistencias de stock/lotes contra Profit Plus y permite reasignar FEFO o nivelar en 1-Clic");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(tm.textSecondary());
        titleTexts.add(lblSub);
        titleBox.add(titleTexts);

        topPanel.add(titleBox, "growx");

        // Banner del error original de SIAD
        RoundedPanel bannerError = new RoundedPanel(10, false);
        bannerError.setBackground(tm.isDark() ? new Color(69, 10, 10) : new Color(254, 242, 242));
        bannerError.setLayout(new MigLayout("insets 10 14 10 14, fillx", "[]10[grow]", "[]"));
        bannerError.setBorder(BorderFactory.createLineBorder(new Color(239, 68, 68), 1));

        JLabel lblErrIcon = new JLabel("🚨");
        lblErrIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        bannerError.add(lblErrIcon);

        lblErrorSiad = new JLabel("Cargando error...");
        lblErrorSiad.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblErrorSiad.setForeground(new Color(220, 38, 38));
        bannerError.add(lblErrorSiad, "growx");

        topPanel.add(bannerError, "growx");
        root.add(topPanel, BorderLayout.NORTH);

        // ═══════════════════════════════════════════════════════════
        // TABLA DE RENGLONES Y DIAGNÓSTICO
        // ═══════════════════════════════════════════════════════════
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);
        centerPanel.setBorder(new EmptyBorder(0, 20, 10, 20));

        String[] cols = {
                "Estado", "Reng#", "Código", "Descripción", "Cant. Solicitada",
                "Lote Pedido", "Stock Lote Profit", "Stock Almacén Profit", "Lotes Alternativos Disponibles (FEFO)"
        };

        modelTable = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        tblRenglones = new JTable(modelTable);
        tblRenglones.setRowHeight(28);
        tblRenglones.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tblRenglones.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblRenglones.setShowGrid(true);
        tblRenglones.setGridColor(tm.border());

        JTableHeader th = tblRenglones.getTableHeader();
        th.setFont(new Font("Segoe UI", Font.BOLD, 11));
        th.setBackground(tm.cardBg());
        th.setForeground(tm.textPrimary());

        tblRenglones.getColumnModel().getColumn(0).setCellRenderer(new EstadoRenglonRenderer());
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        tblRenglones.getColumnModel().getColumn(1).setCellRenderer(rightRenderer);
        tblRenglones.getColumnModel().getColumn(4).setCellRenderer(rightRenderer);
        tblRenglones.getColumnModel().getColumn(6).setCellRenderer(rightRenderer);
        tblRenglones.getColumnModel().getColumn(7).setCellRenderer(rightRenderer);

        tblRenglones.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                actualizarEstadoBotonesAccion();
            }
        });

        JScrollPane sp = new JScrollPane(tblRenglones);
        sp.setBorder(BorderFactory.createLineBorder(tm.border()));
        sp.getViewport().setBackground(tm.cardBg());
        centerPanel.add(sp, BorderLayout.CENTER);

        // Barra de acciones por renglón
        JPanel actionsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        actionsRow.setOpaque(false);

        btnReasignarFefo = new JButton("🔄 Reasignar a Mejor Lote FEFO");
        btnReasignarFefo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnReasignarFefo.setBackground(tm.accent());
        btnReasignarFefo.setForeground(tm.btnForegroundFor(tm.accent()));
        btnReasignarFefo.setEnabled(false);
        btnReasignarFefo.addActionListener(e -> reasignarLoteSeleccionado());
        actionsRow.add(btnReasignarFefo);

        btnNivelarStock = new JButton("📦 Nivelar Existencia en Profit");
        btnNivelarStock.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnNivelarStock.setBackground(tm.tealAccent());
        btnNivelarStock.setForeground(tm.btnForegroundFor(tm.tealAccent()));
        btnNivelarStock.setEnabled(false);
        btnNivelarStock.addActionListener(e -> nivelarStockSeleccionado());
        actionsRow.add(btnNivelarStock);

        btnAutoFefoAll = new JButton("⚡ Auto-Corregir Todos con FEFO");
        btnAutoFefoAll.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnAutoFefoAll.setToolTipText("Reasigna automáticamente todos los artículos con lote agotado hacia el lote disponible más próximo a vencer");
        btnAutoFefoAll.addActionListener(e -> autoCorregirTodosFefo());
        actionsRow.add(btnAutoFefoAll);

        centerPanel.add(actionsRow, BorderLayout.SOUTH);
        root.add(centerPanel, BorderLayout.CENTER);

        // ═══════════════════════════════════════════════════════════
        // FOOTER / ACCIONES PRINCIPALES
        // ═══════════════════════════════════════════════════════════
        JPanel footer = new JPanel(new MigLayout("insets 12 20 14 20, fillx", "[]push[]8[]8[]", "[]"));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, tm.border()));

        lblEstadoGeneral = new JLabel("Verificando consistencia...");
        lblEstadoGeneral.setFont(new Font("Segoe UI", Font.BOLD, 13));
        footer.add(lblEstadoGeneral);

        btnRevalidar = new JButton("🔍 Re-validar contra Profit");
        btnRevalidar.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnRevalidar.addActionListener(e -> cargarDiagnostico());
        footer.add(btnRevalidar);

        btnReprocesar = new JButton("🚀 Reprocesar Pedido Ahora (Enviar a SIAD)");
        btnReprocesar.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnReprocesar.setBackground(tm.greenAccent());
        btnReprocesar.setForeground(tm.btnForegroundFor(tm.greenAccent()));
        btnReprocesar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnReprocesar.addActionListener(e -> ejecutarReprocesamiento());
        footer.add(btnReprocesar);

        JButton btnCerrar = new JButton("Cerrar");
        btnCerrar.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnCerrar.addActionListener(e -> dispose());
        footer.add(btnCerrar);

        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);
    }

    /**
     * Carga y evalúa el diagnóstico completo del archivo contra Profit Plus.
     */
    private void cargarDiagnostico() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        SwingWorker<SiadDiagnosticoDTO, Void> worker = new SwingWorker<>() {
            @Override
            protected SiadDiagnosticoDTO doInBackground() {
                return siadDAO.diagnosticarPedido(archivoTxt);
            }

            @Override
            protected void done() {
                try {
                    diagnostico = get();
                    actualizarVista();
                } catch (Exception ex) {
                    Toast.show("Error diagnosticando pedido: " + ex.getMessage(), Toast.Type.ERROR);
                } finally {
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        };
        worker.execute();
    }

    private void actualizarVista() {
        if (diagnostico == null) return;

        lblTituloPedido.setText("Pedido Web #" + diagnostico.getPedidoId() + " — " + diagnostico.getCliDes() + " (" + diagnostico.getCoCli() + ")");
        lblErrorSiad.setText("<html><b>Motivo de Rechazo en SIAD:</b> " + diagnostico.getErrorOriginal() + "</html>");

        modelTable.setRowCount(0);
        for (RenglonDiagnosticoDTO r : diagnostico.getRenglones()) {
            StringBuilder fefoDesc = new StringBuilder();
            if (r.getLotesDisponibles() != null && !r.getLotesDisponibles().isEmpty()) {
                for (int i = 0; i < Math.min(3, r.getLotesDisponibles().size()); i++) {
                    if (i > 0) fefoDesc.append(" | ");
                    LoteFEFODTO l = r.getLotesDisponibles().get(i);
                    fefoDesc.append(l.getNumeroLote()).append(" (").append((int) l.getCantDisponible()).append(" uds, ").append(l.getFechaVencimiento()).append(")");
                }
            } else {
                fefoDesc.append("Sin otros lotes disponibles");
            }

            modelTable.addRow(new Object[]{
                    r.getTipoError(),
                    r.getRengNum(),
                    r.getCoArt(),
                    r.getArtDes(),
                    DF.format(r.getCantSolicitada()),
                    r.getLoteSolicitado(),
                    DF.format(r.getStockLoteSolicitado()),
                    DF.format(r.getStockAlmacen()),
                    fefoDesc.toString()
            });
        }

        int errCount = diagnostico.getCantidadErrores();
        if (errCount > 0) {
            lblEstadoGeneral.setText("🔴 Quedan " + errCount + " renglón(es) con discrepancia de existencia o lote en Profit");
            lblEstadoGeneral.setForeground(new Color(239, 68, 68));
            btnReprocesar.setEnabled(false);
            btnReprocesar.setToolTipText("Debe resolver las discrepancias antes de enviar a SIAD");
        } else {
            lblEstadoGeneral.setText("🟢 ¡100% de renglones y lotes validados contra Profit Plus! Listo para importar.");
            lblEstadoGeneral.setForeground(new Color(34, 197, 94));
            btnReprocesar.setEnabled(true);
            btnReprocesar.setToolTipText(null);
        }

        actualizarEstadoBotonesAccion();
    }

    private void actualizarEstadoBotonesAccion() {
        int row = tblRenglones.getSelectedRow();
        if (row >= 0 && diagnostico != null && row < diagnostico.getRenglones().size()) {
            RenglonDiagnosticoDTO r = diagnostico.getRenglones().get(row);
            btnReasignarFefo.setEnabled(r.getMejorLoteFEFO() != null && !r.getLoteSolicitado().trim().equalsIgnoreCase(r.getMejorLoteFEFO().getNumeroLote().trim()));
            btnNivelarStock.setEnabled(r.isTieneError());
        } else {
            btnReasignarFefo.setEnabled(false);
            btnNivelarStock.setEnabled(false);
        }
    }

    private void reasignarLoteSeleccionado() {
        int row = tblRenglones.getSelectedRow();
        if (row < 0 || diagnostico == null || row >= diagnostico.getRenglones().size()) return;

        RenglonDiagnosticoDTO r = diagnostico.getRenglones().get(row);
        LoteFEFODTO mejor = r.getMejorLoteFEFO();
        if (mejor == null) {
            Toast.show("No hay lotes alternativos con stock para este artículo", Toast.Type.WARNING);
            return;
        }

        boolean ok = siadDAO.reasignarLoteEnPedidoTxt(archivoTxt, r.getCoArt(), mejor.getNumeroLote());
        if (ok) {
            Toast.show("Lote de " + r.getCoArt() + " reasignado a " + mejor.getNumeroLote(), Toast.Type.SUCCESS);
            cargarDiagnostico();
        } else {
            Toast.show("Error al modificar archivo de pedido", Toast.Type.ERROR);
        }
    }

    private void nivelarStockSeleccionado() {
        int row = tblRenglones.getSelectedRow();
        if (row < 0 || diagnostico == null || row >= diagnostico.getRenglones().size()) return;

        RenglonDiagnosticoDTO r = diagnostico.getRenglones().get(row);

        double cantFaltante = Math.max(r.getCantSolicitada() - r.getStockAlmacen(), r.getCantSolicitada() - r.getStockLoteSolicitado());
        if (cantFaltante <= 0) cantFaltante = r.getCantSolicitada();

        String msg = String.format("<html>¿Confirmas que físicamente en almacén existen las <b>%.0f unidades</b> del producto <b>%s</b> en el lote <b>%s</b>?<br><br>" +
                        "Se registrará la nivelación directamente en <b>saStockAlmacen</b> y <b>saLoteEntrada</b> de Profit Plus.</html>",
                cantFaltante, r.getCoArt(), r.getLoteSolicitado());

        int opt = JOptionPane.showConfirmDialog(this, msg, "Nivelar Existencia Física en Profit", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (opt == JOptionPane.YES_OPTION) {
            try {
                boolean ok = siadDAO.reponerStockLoteProfit(r.getCoArt(), "01", r.getLoteSolicitado(), null, cantFaltante, "DROAI");
                if (ok) {
                    Toast.show("Stock repuesto exitosamente en Profit Plus", Toast.Type.SUCCESS);
                    cargarDiagnostico();
                }
            } catch (Exception ex) {
                Toast.show("Error al actualizar stock: " + ex.getMessage(), Toast.Type.ERROR);
            }
        }
    }

    private void autoCorregirTodosFefo() {
        if (diagnostico == null) return;
        int count = 0;
        for (RenglonDiagnosticoDTO r : diagnostico.getRenglones()) {
            if (r.isTieneError() && r.getMejorLoteFEFO() != null) {
                boolean ok = siadDAO.reasignarLoteEnPedidoTxt(archivoTxt, r.getCoArt(), r.getMejorLoteFEFO().getNumeroLote());
                if (ok) count++;
            }
        }
        if (count > 0) {
            Toast.show("Se reasignaron " + count + " renglón(es) al lote FEFO disponible más próximo", Toast.Type.SUCCESS);
            cargarDiagnostico();
        } else {
            Toast.show("No se encontraron lotes FEFO alternativos aplicables", Toast.Type.INFO);
        }
    }

    private void ejecutarReprocesamiento() {
        if (diagnostico != null && diagnostico.tieneErrores()) {
            int opt = JOptionPane.showConfirmDialog(this,
                    "Aún existen discrepancias en Profit Plus para este pedido.\n¿Desea enviarlo a la cola de todas formas?",
                    "Advertencia", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (opt != JOptionPane.YES_OPTION) return;
        }

        boolean ok = siadDAO.reprocesarPedidoSiad(archivoTxt);
        if (ok) {
            JOptionPane.showMessageDialog(this,
                    "¡Pedido #" + (diagnostico != null ? diagnostico.getPedidoId() : "") + " movido exitosamente a la cola activa de SIAD!\n\n" +
                            "El servicio importará el pedido a Profit Plus en unos instantes.",
                    "Reprocesamiento en Curso", JOptionPane.INFORMATION_MESSAGE);
            if (onReprocesadoCallback != null) {
                onReprocesadoCallback.run();
            }
            dispose();
        } else {
            Toast.show("No se pudo mover el archivo a la cola de SIAD", Toast.Type.ERROR);
        }
    }

    // ── Renderer ──
    private static class EstadoRenglonRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String st = String.valueOf(value);
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));

            if ("OK".equals(st)) {
                lbl.setText("🟢 DISPONIBLE");
                lbl.setForeground(new Color(34, 197, 94));
            } else if ("LOTE_AGOTADO_HAY_FEFO".equals(st)) {
                lbl.setText("🟠 LOTE AGOTADO (HAY FEFO)");
                lbl.setForeground(new Color(249, 115, 22));
            } else if ("STOCK_INSUFICIENTE".equals(st) || "LOTE_INSUFICIENTE".equals(st)) {
                lbl.setText("🔴 INSUFICIENTE");
                lbl.setForeground(new Color(239, 68, 68));
            } else {
                lbl.setText(st);
            }
            return lbl;
        }
    }
}

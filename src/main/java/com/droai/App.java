package com.droai;

import com.droai.model.SesionUsuario;
import com.droai.ui.AdminDashboardFrame;
import com.droai.ui.ThemeManager;
import com.droai.ui.dialog.LoginAuditoriaDialog;

import javax.swing.*;
import java.awt.*;

/**
 * Punto de entrada principal de la aplicación DroAI.
 */
public class App {
    public static void main(String[] args) {
        // ── Inicializar tema DroAI (Dark por defecto) ──
        ThemeManager.get().initialize();

        UIManager.put("Component.arc", 12);
        UIManager.put("Button.arc", 14);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
        UIManager.put("ScrollPane.smoothScrolling", true);

        // ----- Table structural settings -----
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.intercellSpacing", new Dimension(0, 1));
        UIManager.put("Table.rowHeight", 30);

        SwingUtilities.invokeLater(() -> {
            // ═══════════════════════════════════════════════════════
            //  PASO 1: Mostrar Login ANTES de la ventana principal
            // ═══════════════════════════════════════════════════════
            LoginAuditoriaDialog loginDialog = new LoginAuditoriaDialog(null);
            loginDialog.setVisible(true); // Modal — bloquea hasta cerrar

            // Si no se autenticó (cerró el diálogo sin login), salir de la app
            if (!SesionUsuario.isAutenticado()) {
                System.out.println("[DroAI] Login cancelado. Cerrando aplicación.");
                System.exit(0);
                return;
            }

            // ═══════════════════════════════════════════════════════
            //  PASO 2: Login exitoso → Abrir Dashboard Administrativo
            // ═══════════════════════════════════════════════════════
            System.out.println("[DroAI] ✔ Acceso autorizado: "
                    + SesionUsuario.current().getCoUsuario()
                    + " (" + SesionUsuario.current().getNombreUsuario() + ")"
                    + " — Máquina: " + SesionUsuario.current().getMaquina());

            AdminDashboardFrame dashboard = new AdminDashboardFrame();
            dashboard.setVisible(true);
        });
    }
}

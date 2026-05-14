package com.droai;

import com.droai.model.SesionUsuario;
import com.droai.ui.dialog.LoginAuditoriaDialog;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class App {
    public static void main(String[] args) {
        // ── Paleta DroAI Dark ──
        Map<String, String> darkPalette = new HashMap<>();
        darkPalette.put("@background", "#11151C");
        darkPalette.put("@control", "#1E232E");
        darkPalette.put("@accentColor", "#2A6BFF");
        darkPalette.put("Button.default.background", "#00D29E");
        darkPalette.put("@foreground", "#F8FAFC");
        FlatLaf.setGlobalExtraDefaults(darkPalette);
        FlatDarkLaf.setup();

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
            //  PASO 2: Login exitoso → Abrir ventana principal
            // ═══════════════════════════════════════════════════════
            System.out.println("[DroAI] ✔ Acceso autorizado: "
                    + SesionUsuario.current().getCoUsuario()
                    + " (" + SesionUsuario.current().getNombreUsuario() + ")");

            com.droai.ui.MainFrame frame = new com.droai.ui.MainFrame();
            frame.setVisible(true);
        });
    }
}

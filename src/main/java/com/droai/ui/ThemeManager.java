package com.droai.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Gestor centralizado de temas para DroAI.
 *
 * <p>Singleton que mantiene las paletas Dark y Light, el estado actual
 * del tema, y notifica a todos los componentes registrados cuando
 * el usuario cambia el tema.
 *
 * <p>Uso típico:
 * <pre>
 *   // Obtener color actual:
 *   panel.setBackground(ThemeManager.get().background());
 *
 *   // Registrar listener para repintar al cambiar tema:
 *   ThemeManager.get().addThemeChangeListener(myRunnable);
 * </pre>
 */
public final class ThemeManager {

    // ── Singleton ──
    private static final ThemeManager INSTANCE = new ThemeManager();

    public static ThemeManager get() {
        return INSTANCE;
    }

    // ── Estado ──
    private boolean dark = true;

    // ── Listeners (WeakReference para evitar memory leaks) ──
    private final List<WeakReference<Runnable>> listeners = new ArrayList<>();

    private ThemeManager() {
    }

    // ═══════════════════════════════════════════════════════════════
    // API pública
    // ═══════════════════════════════════════════════════════════════

    public boolean isDark() {
        return dark;
    }

    /**
     * Aplica el tema inicial (Dark por defecto) con FlatLaf + paleta DroAI.
     * Debe invocarse una sola vez al inicio en {@code App.main()}.
     */
    public void initialize() {
        applyFlatLaf();
    }

    /**
     * Alterna entre tema oscuro y claro, actualiza FlatLaf y notifica
     * a todos los listeners registrados.
     */
    public void toggleTheme() {
        dark = !dark;
        applyFlatLaf();
        FlatLaf.updateUI();
        fireThemeChanged();
    }

    /**
     * Registra un listener que será invocado cada vez que el tema cambie.
     * Se almacena como {@link WeakReference} — al cerrar la ventana el GC
     * lo recogerá automáticamente.
     */
    public void addThemeChangeListener(Runnable listener) {
        listeners.add(new WeakReference<>(listener));
    }

    /**
     * Desregistra explícitamente un listener.
     */
    public void removeThemeChangeListener(Runnable listener) {
        listeners.removeIf(ref -> {
            Runnable r = ref.get();
            return r == null || r == listener;
        });
    }

    // ═══════════════════════════════════════════════════════════════
    // Colores Semánticos — Fondos
    // ═══════════════════════════════════════════════════════════════

    /** Fondo principal de la aplicación. */
    public Color background() {
        return dark ? new Color(17, 21, 28) : new Color(241, 245, 249);
    }

    /** Fondo de tarjetas y paneles elevados. */
    public Color cardBg() {
        return dark ? new Color(30, 35, 46) : new Color(255, 255, 255);
    }

    /** Fondo de tarjeta al pasar el mouse. */
    public Color cardHover() {
        return dark ? new Color(38, 44, 58) : new Color(240, 244, 248);
    }

    /** Borde de tarjetas/paneles. */
    public Color cardBorder() {
        return dark ? new Color(55, 62, 80) : new Color(203, 213, 225);
    }

    /** Fondo de campos de texto / inputs. */
    public Color bgField() {
        return dark ? new Color(38, 44, 58) : new Color(248, 250, 252);
    }

    /** Fondo de secciones internas. */
    public Color bgSection() {
        return dark ? new Color(30, 35, 46) : new Color(241, 245, 249);
    }

    /** Fondo de paneles secundarios. */
    public Color bgPanel() {
        return dark ? new Color(24, 28, 38) : new Color(248, 250, 252);
    }

    /** Fondo de diálogos. */
    public Color bgDialog() {
        return dark ? new Color(30, 33, 42) : new Color(248, 250, 252);
    }

    // ═══════════════════════════════════════════════════════════════
    // Colores Semánticos — Texto
    // ═══════════════════════════════════════════════════════════════

    /** Texto principal (títulos, contenido). */
    public Color textPrimary() {
        return dark ? new Color(248, 250, 252) : new Color(15, 23, 42);
    }

    /** Texto secundario (subtítulos, metadata). */
    public Color textSecondary() {
        return dark ? new Color(148, 163, 184) : new Color(71, 85, 105);
    }

    /** Etiquetas y labels menores. */
    public Color textLabel() {
        return dark ? new Color(100, 116, 139) : new Color(100, 116, 139);
    }

    /** Texto de valor puro (contraste total). */
    public Color textValue() {
        return dark ? Color.WHITE : new Color(15, 23, 42);
    }

    // ═══════════════════════════════════════════════════════════════
    // Colores Semánticos — Acentos
    // ═══════════════════════════════════════════════════════════════

    /** Acento principal (azul). */
    public Color accent() {
        return dark ? new Color(42, 107, 255) : new Color(29, 78, 216);
    }

    /** Acento verde. */
    public Color greenAccent() {
        return dark ? new Color(0, 210, 158) : new Color(5, 150, 105);
    }

    /** Acento naranja. */
    public Color orangeAccent() {
        return dark ? new Color(245, 158, 11) : new Color(217, 119, 6);
    }

    /** Acento púrpura. */
    public Color purpleAccent() {
        return dark ? new Color(168, 85, 247) : new Color(124, 58, 237);
    }

    /** Acento rojo. */
    public Color redAccent() {
        return dark ? new Color(239, 68, 68) : new Color(220, 38, 38);
    }

    /** Acento teal. */
    public Color tealAccent() {
        return dark ? new Color(0, 168, 157) : new Color(13, 148, 136);
    }

    // ═══════════════════════════════════════════════════════════════
    // Colores Semánticos — Bordes
    // ═══════════════════════════════════════════════════════════════

    /** Borde genérico. */
    public Color border() {
        return dark ? new Color(55, 62, 80) : new Color(203, 213, 225);
    }

    /** Color de caret para campos de texto. */
    public Color caret() {
        return dark ? new Color(100, 160, 255) : new Color(59, 130, 246);
    }

    // ═══════════════════════════════════════════════════════════════
    // Colores Semánticos — Tablas
    // ═══════════════════════════════════════════════════════════════

    /** Fondo de tabla. */
    public Color tableBg() {
        return dark ? new Color(24, 28, 38) : new Color(255, 255, 255);
    }

    /** Fila alternada de tabla. */
    public Color tableAlt() {
        return dark ? new Color(28, 33, 44) : new Color(248, 250, 252);
    }

    /** Fondo de header de tabla. */
    public Color tableHeader() {
        return dark ? new Color(34, 40, 54) : new Color(226, 232, 240);
    }

    /** Fila alternada genérica (fuera de tabla). */
    public Color rowAlt() {
        return dark ? new Color(30, 38, 52) : new Color(241, 245, 249);
    }

    // ═══════════════════════════════════════════════════════════════
    // Colores Semánticos — Botones
    // ═══════════════════════════════════════════════════════════════

    /** Botón verde (primario / éxito). */
    public Color btnGreenBg() {
        return dark ? new Color(0, 180, 130) : new Color(5, 150, 105);
    }

    /** Botón de acento (azul). */
    public Color btnAccentBg() {
        return dark ? new Color(42, 107, 255) : new Color(29, 78, 216);
    }

    /** Botón neutro. */
    public Color btnNeutralBg() {
        return dark ? new Color(55, 62, 80) : new Color(203, 213, 225);
    }

    /** Botón de advertencia (amarillo/ámbar). */
    public Color btnWarnBg() {
        return dark ? new Color(220, 160, 50) : new Color(217, 119, 6);
    }

    /** Botón rojo (peligro / eliminar). */
    public Color btnRedBg() {
        return dark ? new Color(200, 60, 60) : new Color(220, 38, 38);
    }

    // ═══════════════════════════════════════════════════════════════
    // Colores Semánticos — Estados
    // ═══════════════════════════════════════════════════════════════

    /** Rojo de error. */
    public Color errorRed() {
        return dark ? new Color(220, 60, 60) : new Color(220, 38, 38);
    }

    /** Verde de éxito. */
    public Color successGreen() {
        return dark ? new Color(0, 180, 130) : new Color(5, 150, 105);
    }

    /** Ámbar de advertencia. */
    public Color warnAmber() {
        return dark ? new Color(220, 160, 50) : new Color(217, 119, 6);
    }

    /** Verde de éxito para importación. */
    public Color successGreenImport() {
        return dark ? new Color(46, 125, 50) : new Color(22, 163, 74);
    }

    // ═══════════════════════════════════════════════════════════════
    // Colores Semánticos — Especiales
    // ═══════════════════════════════════════════════════════════════

    /** Dorado resaltado (producto destacado). */
    public Color highlightGold() {
        return dark ? new Color(255, 200, 80) : new Color(245, 158, 11);
    }

    /** Fondo semi-transparente de sección de precios. */
    public Color priceHeaderBg() {
        return dark ? new Color(42, 107, 255, 40) : new Color(29, 78, 216, 25);
    }

    /** Color de gradiente superior (para dashboards). */
    public Color gradientTop() {
        return dark ? new Color(42, 107, 255, 25) : new Color(29, 78, 216, 15);
    }

    /** Color de gradiente inferior (transparente). */
    public Color gradientBottom() {
        return dark ? new Color(42, 107, 255, 0) : new Color(29, 78, 216, 0);
    }

    /**
     * Determina el color de foreground del botón según su background para garantizar contraste.
     * Botones con fondo verde, naranja o amarillo usan texto oscuro; el resto usa blanco.
     */
    public Color btnForegroundFor(Color bg) {
        // Calcular luminosidad relativa
        double lum = (0.299 * bg.getRed() + 0.587 * bg.getGreen() + 0.114 * bg.getBlue()) / 255.0;
        return lum > 0.5 ? new Color(17, 21, 28) : Color.WHITE;
    }

    // ═══════════════════════════════════════════════════════════════
    // Internos
    // ═══════════════════════════════════════════════════════════════

    private void applyFlatLaf() {
        Map<String, String> palette = new HashMap<>();
        if (dark) {
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
    }

    private void fireThemeChanged() {
        Iterator<WeakReference<Runnable>> it = listeners.iterator();
        while (it.hasNext()) {
            Runnable r = it.next().get();
            if (r == null) {
                it.remove();
            } else {
                r.run();
            }
        }
    }
}

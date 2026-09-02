package com.droai.ui.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Utilidad centralizada para la gestión y aplicación del ícono oficial de DroAI.
 */
public final class IconHelper {

    private static final Logger logger = LoggerFactory.getLogger(IconHelper.class);
    private static final String LOGO_PATH = "/images/logo.png";

    private static Image baseImage = null;
    private static List<Image> multiResolutionIcons = null;

    private IconHelper() {
        // Utility class
    }

    /**
     * Obtiene la imagen base del ícono de la aplicación.
     */
    public static synchronized Image getAppIcon() {
        if (baseImage == null) {
            loadIcons();
        }
        return baseImage;
    }

    /**
     * Obtiene una lista de íconos en múltiples resoluciones (16, 24, 32, 48, 64, 128, 256 px)
     * para asegurar la máxima nitidez en barras de título, barra de tareas, Alt+Tab y pantallas HiDPI.
     */
    public static synchronized List<Image> getAppIcons() {
        if (multiResolutionIcons == null || multiResolutionIcons.isEmpty()) {
            loadIcons();
        }
        return multiResolutionIcons;
    }

    /**
     * Obtiene un ImageIcon del logo para componentes Swing (ej: JLabel en Headers o Dashboards).
     */
    public static ImageIcon getAppImageIcon(int width, int height) {
        Image img = getAppIcon();
        if (img == null) {
            return null;
        }
        Image scaled = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    /**
     * Aplica el ícono a cualquier ventana (JFrame, JDialog, JWindow).
     */
    public static void applyAppIcon(Window window) {
        if (window == null) {
            return;
        }
        try {
            List<Image> icons = getAppIcons();
            if (icons != null && !icons.isEmpty()) {
                window.setIconImages(icons);
            } else {
                Image icon = getAppIcon();
                if (icon != null) {
                    window.setIconImage(icon);
                }
            }
        } catch (Exception e) {
            logger.warn("No se pudo asignar el icono a la ventana: {}", e.getMessage());
        }
    }

    /**
     * Configura el ícono en la barra de tareas del sistema operativo (Java 9+ Taskbar API).
     */
    public static void setupTaskbarIcon() {
        try {
            if (Taskbar.isTaskbarSupported()) {
                Taskbar taskbar = Taskbar.getTaskbar();
                if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                    Image icon = getAppIcon();
                    if (icon != null) {
                        taskbar.setIconImage(icon);
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Taskbar API no disponible o no soportada en este entorno: {}", e.getMessage());
        }
    }

    private static void loadIcons() {
        try {
            URL resource = IconHelper.class.getResource(LOGO_PATH);
            if (resource != null) {
                baseImage = ImageIO.read(resource);
            } else {
                try (InputStream is = IconHelper.class.getResourceAsStream(LOGO_PATH)) {
                    if (is != null) {
                        baseImage = ImageIO.read(is);
                    }
                }
            }

            if (baseImage != null) {
                int[] sizes = {16, 24, 32, 48, 64, 128, 256};
                List<Image> list = new ArrayList<>();
                for (int size : sizes) {
                    list.add(baseImage.getScaledInstance(size, size, Image.SCALE_SMOOTH));
                }
                multiResolutionIcons = Collections.unmodifiableList(list);
            } else {
                logger.warn("No se encontró el recurso del logo en {}", LOGO_PATH);
                multiResolutionIcons = Collections.emptyList();
            }
        } catch (Exception e) {
            logger.error("Error al cargar icono de la aplicación desde {}: {}", LOGO_PATH, e.getMessage());
            multiResolutionIcons = Collections.emptyList();
        }
    }
}

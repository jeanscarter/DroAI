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
 * Utilidad centralizada para la gestión y aplicación del ícono oficial (logo.ico) de DroAI.
 */
public final class IconHelper {

    private static final Logger logger = LoggerFactory.getLogger(IconHelper.class);

    private static final String SQUARE_ICON_PATH = "/images/logo_square.png";
    private static final String BANNER_PATH = "/images/logo.png";

    private static Image baseImage = null;
    private static Image bannerImage = null;
    private static List<Image> multiResolutionIcons = null;
    private static boolean taskbarInitialized = false;

    private IconHelper() {
        // Utility class
    }

    /**
     * Obtiene la imagen base cuadrada del ícono de la aplicación (isotipo de logo.ico).
     */
    public static synchronized Image getAppIcon() {
        if (baseImage == null) {
            loadIcons();
        }
        return baseImage;
    }

    /**
     * Obtiene una lista de íconos en múltiples resoluciones (16, 20, 24, 32, 40, 48, 64, 96, 128, 256 px)
     * para asegurar máxima nitidez en barra de tareas, barra de título, Alt+Tab y pantallas HiDPI.
     */
    public static synchronized List<Image> getAppIcons() {
        if (multiResolutionIcons == null || multiResolutionIcons.isEmpty()) {
            loadIcons();
        }
        return multiResolutionIcons;
    }

    /**
     * Obtiene un ImageIcon del ícono cuadrado para componentes Swing (ej: Headers, Login, etc.).
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
     * Obtiene el banner horizontal oficial con tipografía completa ("DroActiva").
     */
    public static synchronized Image getBannerImage() {
        if (bannerImage == null) {
            try {
                URL resource = IconHelper.class.getResource(BANNER_PATH);
                if (resource != null) {
                    bannerImage = ImageIO.read(resource);
                } else {
                    try (InputStream is = IconHelper.class.getResourceAsStream(BANNER_PATH)) {
                        if (is != null) {
                            bannerImage = ImageIO.read(is);
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("No se pudo cargar el banner oficial: {}", e.getMessage());
            }
        }
        return bannerImage;
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
     * Configura el ícono en la barra de tareas del sistema operativo.
     * En Windows registra el AppUserModelID para independizar el botón de la barra de tareas de javaw.exe
     * y asociarlo directamente al ícono de DroAI.
     */
    public static synchronized void setupTaskbarIcon() {
        if (taskbarInitialized) {
            return;
        }
        taskbarInitialized = true;

        // 1. En Windows, registrar AppUserModelID explícito para la barra de tareas
        setWindowsAppUserModelId("DroAI.SistemaGestion.DroActiva");

        // 2. Usar Java Taskbar API estándar si está soportada
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

    /**
     * Registra el AppUserModelID en Windows usando la API Foreign Function & Memory de Java.
     */
    private static void setWindowsAppUserModelId(String appUserModelId) {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (!os.contains("win")) {
            return;
        }
        try {
            var linker = java.lang.foreign.Linker.nativeLinker();
            var lookup = java.lang.foreign.SymbolLookup.libraryLookup("shell32", java.lang.foreign.Arena.global());
            var fn = lookup.find("SetCurrentProcessExplicitAppUserModelID");
            if (fn.isPresent()) {
                var mh = linker.downcallHandle(
                    fn.get(),
                    java.lang.foreign.FunctionDescriptor.of(
                        java.lang.foreign.ValueLayout.JAVA_INT,
                        java.lang.foreign.ValueLayout.ADDRESS
                    )
                );
                try (var arena = java.lang.foreign.Arena.ofConfined()) {
                    var str = arena.allocateFrom(appUserModelId, java.nio.charset.StandardCharsets.UTF_16LE);
                    mh.invoke(str);
                    logger.info("Windows AppUserModelID registrado: {}", appUserModelId);
                }
            }
        } catch (Throwable t) {
            logger.debug("No se pudo invocar SetCurrentProcessExplicitAppUserModelID: {}", t.getMessage());
        }
    }

    private static void loadIcons() {
        try {
            // Prioridad 1: Ícono cuadrado de alta resolución extraído de logo.ico
            URL resource = IconHelper.class.getResource(SQUARE_ICON_PATH);
            if (resource != null) {
                baseImage = ImageIO.read(resource);
            } else {
                try (InputStream is = IconHelper.class.getResourceAsStream(SQUARE_ICON_PATH)) {
                    if (is != null) {
                        baseImage = ImageIO.read(is);
                    }
                }
            }

            // Fallback: Si no está logo_square.png, intentar banner o recurso general
            if (baseImage == null) {
                URL bannerRes = IconHelper.class.getResource(BANNER_PATH);
                if (bannerRes != null) {
                    baseImage = ImageIO.read(bannerRes);
                }
            }

            if (baseImage != null) {
                int[] sizes = {16, 20, 24, 32, 40, 48, 64, 96, 128, 256};
                List<Image> list = new ArrayList<>();
                for (int size : sizes) {
                    list.add(baseImage.getScaledInstance(size, size, Image.SCALE_SMOOTH));
                }
                multiResolutionIcons = Collections.unmodifiableList(list);
            } else {
                logger.warn("No se encontró el recurso del icono en {}", SQUARE_ICON_PATH);
                multiResolutionIcons = Collections.emptyList();
            }
        } catch (Exception e) {
            logger.error("Error al cargar icono de la aplicación: {}", e.getMessage());
            multiResolutionIcons = Collections.emptyList();
        }
    }
}

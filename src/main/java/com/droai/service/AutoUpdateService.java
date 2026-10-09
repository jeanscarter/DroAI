package com.droai.service;

import com.droai.ui.util.IconHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import java.util.concurrent.*;

/**
 * Servicio centralizado para la comprobación y aplicación de actualizaciones
 * automáticas de DroAI desde el repositorio central del servidor (\\SRVFS0101\Sistemas\dro-ai o X:\dro-ai).
 */
public final class AutoUpdateService {

    private static final Logger logger = LoggerFactory.getLogger(AutoUpdateService.class);

    private static final String SERVER_UNC_PATH = "\\\\SRVFS0101\\Sistemas\\dro-ai";
    private static final String SERVER_DRIVE_PATH = "X:\\dro-ai";

    private AutoUpdateService() {
        // Utility class
    }

    /**
     * Verifica en segundo plano con un límite de tiempo si existe una nueva versión
     * en el servidor. Si existe, descarga la actualización e inicia el proceso de reemplazo.
     *
     * @return true si se inició un proceso de actualización (la app debe cerrarse), false si continúa normal.
     */
    public static boolean checkAndUpdate() {
        return checkAndUpdate(false);
    }

    /**
     * Comprueba actualizaciones.
     *
     * @param manual true si fue invocado por el usuario desde un botón en la interfaz.
     * @return true si se inició la actualización.
     */
    public static boolean checkAndUpdate(boolean manual) {
        try {
            File localDir = getLocalProjectDir();
            if (localDir == null) {
                logger.debug("[AutoUpdate] No se pudo determinar el directorio local de instalación.");
                return false;
            }

            File serverDir = resolveServerDir();
            if (serverDir == null) {
                logger.info("[AutoUpdate] Servidor de actualizaciones no disponible (offline o fuera de red).");
                if (manual) {
                    JOptionPane.showMessageDialog(null,
                            "No se pudo contactar con el servidor de actualizaciones en:\\n" + SERVER_UNC_PATH +
                                    "\\n\\nVerifique su conexión a la red de la empresa.",
                            "Servidor No Disponible", JOptionPane.WARNING_MESSAGE);
                }
                return false;
            }

            // Si la aplicación se está ejecutando DIRECTAMENTE en el servidor o en entorno de desarrollo
            if (isSameOrServerLocation(localDir, serverDir)) {
                logger.debug("[AutoUpdate] Ejecución directa en el servidor o entorno de desarrollo. AutoUpdate omitido.");
                if (manual) {
                    JOptionPane.showMessageDialog(null,
                            "Esta instancia se está ejecutando directamente en la ubicación del servidor.\\nNo requiere actualización remota.",
                            "DroAI Servidor", JOptionPane.INFORMATION_MESSAGE);
                }
                return false;
            }

            // Ejecutar la comparación con timeout estricto de 1.8 segundos
            ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "DroAI-UpdateChecker");
                t.setDaemon(true);
                return t;
            });

            Future<Boolean> checkFuture = executor.submit(() -> isServerNewer(serverDir, localDir));
            boolean hasNewer = false;
            try {
                hasNewer = checkFuture.get(1800, TimeUnit.MILLISECONDS);
            } catch (TimeoutException te) {
                logger.info("[AutoUpdate] Verificación de red excedió el tiempo límite (1.8s). Continuando con versión local.");
                checkFuture.cancel(true);
                return false;
            } catch (Exception e) {
                logger.warn("[AutoUpdate] Error durante comprobación: {}", e.getMessage());
                return false;
            } finally {
                executor.shutdownNow();
            }

            if (!hasNewer) {
                logger.info("[AutoUpdate] DroAI está actualizado con la última versión del servidor.");
                if (manual) {
                    JOptionPane.showMessageDialog(null,
                            "¡DroAI ya cuenta con la última versión disponible en el servidor!",
                            "Actualización de DroAI", JOptionPane.INFORMATION_MESSAGE);
                }
                return false;
            }

            // Si hay una nueva versión, ejecutar el proceso de reemplazo
            logger.info("[AutoUpdate] ¡Nueva versión disponible en {}! Iniciando actualización...", serverDir.getAbsolutePath());
            aplicarActualizacion(serverDir, localDir);
            return true;

        } catch (Exception e) {
            logger.warn("[AutoUpdate] Excepción inesperada comprobando actualizaciones: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Resuelve cuál ruta del servidor está accesible en este momento.
     */
    private static File resolveServerDir() {
        try {
            File unc = new File(SERVER_UNC_PATH);
            if (unc.exists() && unc.isDirectory()) {
                return unc;
            }
        } catch (Exception ignored) {}

        try {
            File drive = new File(SERVER_DRIVE_PATH);
            if (drive.exists() && drive.isDirectory()) {
                return drive;
            }
        } catch (Exception ignored) {}

        return null;
    }

    /**
     * Obtiene el directorio base de la instalación local (ej: C:\Users\...\Documents\dro-ai).
     */
    public static File getLocalProjectDir() {
        try {
            File codeSource = new File(AutoUpdateService.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            
            // Si corre desde classes en desarrollo (target/classes)
            if (codeSource.isDirectory() && codeSource.getName().equalsIgnoreCase("classes")) {
                File target = codeSource.getParentFile();
                if (target != null && target.getName().equalsIgnoreCase("target")) {
                    return target.getParentFile();
                }
                return codeSource.getParentFile();
            }

            // Si corre desde target/DroAI.exe o target/dro-ai-1.0-SNAPSHOT.jar
            File parent = codeSource.getParentFile();
            if (parent != null && parent.getName().equalsIgnoreCase("target")) {
                return parent.getParentFile();
            }

            // Si corre desde la raíz del proyecto (DroAI.exe en la raíz)
            if (parent != null) {
                return parent;
            }

        } catch (Exception e) {
            logger.debug("[AutoUpdate] Error resolviendo getLocalProjectDir: {}", e.getMessage());
        }

        // Fallback al directorio actual de ejecución
        String userDir = System.getProperty("user.dir", ".");
        File f = new File(userDir);
        if (f.getName().equalsIgnoreCase("target")) {
            return f.getParentFile();
        }
        return f;
    }

    /**
     * Comprueba si la ruta local es el servidor mismo o un entorno de desarrollo.
     */
    private static boolean isSameOrServerLocation(File localDir, File serverDir) {
        try {
            String localCanonical = localDir.getCanonicalPath().toUpperCase(Locale.ROOT);
            String serverCanonical = serverDir.getCanonicalPath().toUpperCase(Locale.ROOT);

            if (localCanonical.equals(serverCanonical)) {
                return true;
            }

            // Si la ruta local está en X:\ o en el recurso compartido \\SRVFS0101
            if (localCanonical.startsWith("X:\\") || localCanonical.startsWith("\\\\SRVFS0101\\SISTEMAS")) {
                return true;
            }

            // Si detecta la estructura de código fuente de desarrollo en X:
            File srcDir = new File(localDir, "src" + File.separator + "main" + File.separator + "java");
            File pomFile = new File(localDir, "pom.xml");
            if (srcDir.exists() && pomFile.exists() && (localCanonical.contains("X:") || localCanonical.contains("SRVFS0101"))) {
                return true;
            }

        } catch (Exception ignored) {}

        return false;
    }

    /**
     * Compara las fechas y marcas de versión entre el servidor y la instalación local.
     */
    private static boolean isServerNewer(File serverDir, File localDir) {
        try {
            // 1. Comparar archivo de versión explícito version.txt si existe
            File serverVer = new File(serverDir, "version.txt");
            File localVer = new File(localDir, "version.txt");

            if (serverVer.exists()) {
                String sVerText = Files.readString(serverVer.toPath(), StandardCharsets.UTF_8).trim();
                if (!localVer.exists()) {
                    logger.info("[AutoUpdate] Servidor tiene version.txt pero local no. Se requiere actualización.");
                    return true;
                }
                String lVerText = Files.readString(localVer.toPath(), StandardCharsets.UTF_8).trim();
                if (!sVerText.isEmpty() && !sVerText.equals(lVerText)) {
                    logger.info("[AutoUpdate] Marca de versión diferente. Servidor: [{}], Local: [{}]", sVerText, lVerText);
                    return true;
                }
            }

            // 2. Comparar fecha de DroAI.exe en target
            File serverExe = new File(serverDir, "target" + File.separator + "DroAI.exe");
            File localExe = new File(localDir, "target" + File.separator + "DroAI.exe");

            if (!localExe.exists()) {
                localExe = new File(localDir, "DroAI.exe");
            }

            if (serverExe.exists() && localExe.exists()) {
                long serverTime = serverExe.lastModified();
                long localTime = localExe.lastModified();

                // Margen de 3000 ms (3 segundos) para evitar desfases de reloj entre estaciones
                if (serverTime - localTime > 3000L) {
                    logger.info("[AutoUpdate] DroAI.exe en servidor es más reciente. Servidor: {}, Local: {} (Dif: {} ms)",
                            serverTime, localTime, (serverTime - localTime));
                    return true;
                }
            }

            // 3. Comparar fecha del JAR sombreado en target
            File serverJar = new File(serverDir, "target" + File.separator + "dro-ai-1.0-SNAPSHOT.jar");
            File localJar = new File(localDir, "target" + File.separator + "dro-ai-1.0-SNAPSHOT.jar");

            if (serverJar.exists() && localJar.exists()) {
                long serverTime = serverJar.lastModified();
                long localTime = localJar.lastModified();

                if (serverTime - localTime > 3000L) {
                    logger.info("[AutoUpdate] JAR en servidor es más reciente. Servidor: {}, Local: {} (Dif: {} ms)",
                            serverTime, localTime, (serverTime - localTime));
                    return true;
                }
            }

        } catch (Exception e) {
            logger.warn("[AutoUpdate] Error comparando archivos: {}", e.getMessage());
        }

        return false;
    }

    /**
     * Muestra la notificación visual al usuario, genera el script temporal de actualización
     * e inicia el reemplazo cerrando la aplicación actual.
     */
    private static void aplicarActualizacion(File serverDir, File localDir) {
        try {
            // Mostrar ventana modal elegante informando al usuario
            mostrarDialogoActualizacion();

            // Detectar cuál ejecutable debe relanzarse tras la actualización
            File targetExe = new File(localDir, "target" + File.separator + "DroAI.exe");
            File rootExe = new File(localDir, "DroAI.exe");
            File targetJar = new File(localDir, "target" + File.separator + "dro-ai-1.0-SNAPSHOT.jar");

            String exeToLaunch;
            String workingDir;

            if (targetExe.exists()) {
                exeToLaunch = targetExe.getAbsolutePath();
                workingDir = targetExe.getParentFile().getAbsolutePath();
            } else if (rootExe.exists()) {
                exeToLaunch = rootExe.getAbsolutePath();
                workingDir = localDir.getAbsolutePath();
            } else {
                exeToLaunch = "javaw.exe -jar \"" + targetJar.getAbsolutePath() + "\"";
                workingDir = localDir.getAbsolutePath();
            }

            // Generar script .bat en la carpeta temporal de Windows (%TEMP%)
            File tempBat = File.createTempFile("droai_update_", ".bat");
            tempBat.deleteOnExit();

            try (PrintWriter pw = new PrintWriter(new FileWriter(tempBat, StandardCharsets.UTF_8))) {
                pw.println("@echo off");
                pw.println("chcp 65001 >nul");
                pw.println("title Actualizando DroAI a la última versión...");
                pw.println("color 0b");
                pw.println("echo ======================================================================");
                pw.println("echo                  ACTUALIZACION DE DROAI EN CURSO");
                pw.println("echo ======================================================================");
                pw.println("echo.");
                pw.println("echo [1/3] Esperando que la aplicacion anterior libere los archivos...");
                pw.println("timeout /t 2 /nobreak >nul");
                pw.println("taskkill /f /im DroAI.exe >nul 2>&1");
                pw.println();
                pw.println("echo [2/3] Descargando componentes actualizados desde el servidor:");
                pw.println("echo       " + serverDir.getAbsolutePath());
                pw.println("echo.");

                // Copiar target\DroAI.exe y JAR
                pw.println("if not exist \"" + localDir.getAbsolutePath() + "\\target\" mkdir \"" + localDir.getAbsolutePath() + "\\target\"");
                pw.println("if exist \"" + serverDir.getAbsolutePath() + "\\target\\DroAI.exe\" (");
                pw.println("    copy /Y \"" + serverDir.getAbsolutePath() + "\\target\\DroAI.exe\" \"" + localDir.getAbsolutePath() + "\\target\\DroAI.exe\" >nul");
                pw.println(")");
                pw.println("if exist \"" + serverDir.getAbsolutePath() + "\\target\\dro-ai-1.0-SNAPSHOT.jar\" (");
                pw.println("    copy /Y \"" + serverDir.getAbsolutePath() + "\\target\\dro-ai-1.0-SNAPSHOT.jar\" \"" + localDir.getAbsolutePath() + "\\target\\dro-ai-1.0-SNAPSHOT.jar\" >nul");
                pw.println(")");

                // Copiar DroAI.exe en la raíz local si existe
                pw.println("if exist \"" + localDir.getAbsolutePath() + "\\DroAI.exe\" (");
                pw.println("    if exist \"" + serverDir.getAbsolutePath() + "\\DroAI.exe\" (");
                pw.println("        copy /Y \"" + serverDir.getAbsolutePath() + "\\DroAI.exe\" \"" + localDir.getAbsolutePath() + "\\DroAI.exe\" >nul");
                pw.println("    ) else (");
                pw.println("        copy /Y \"" + serverDir.getAbsolutePath() + "\\target\\DroAI.exe\" \"" + localDir.getAbsolutePath() + "\\DroAI.exe\" >nul");
                pw.println("    )");
                pw.println(")");

                // Copiar archivos auxiliares (version.txt, Logo.ico, etc.)
                pw.println("if exist \"" + serverDir.getAbsolutePath() + "\\version.txt\" (");
                pw.println("    copy /Y \"" + serverDir.getAbsolutePath() + "\\version.txt\" \"" + localDir.getAbsolutePath() + "\\version.txt\" >nul");
                pw.println("    copy /Y \"" + serverDir.getAbsolutePath() + "\\version.txt\" \"" + localDir.getAbsolutePath() + "\\target\\version.txt\" >nul");
                pw.println(")");
                pw.println("if exist \"" + serverDir.getAbsolutePath() + "\\Logo.ico\" (");
                pw.println("    copy /Y \"" + serverDir.getAbsolutePath() + "\\Logo.ico\" \"" + localDir.getAbsolutePath() + "\\Logo.ico\" >nul");
                pw.println(")");

                pw.println();
                pw.println("echo [3/3] DroAI ha sido actualizado exitosamente.");
                pw.println("echo       Reiniciando el aplicativo...");
                pw.println("timeout /t 1 /nobreak >nul");
                pw.println();
                pw.println("cd /d \"" + workingDir + "\"");
                if (exeToLaunch.startsWith("javaw.exe")) {
                    pw.println("start \"\" " + exeToLaunch);
                } else {
                    pw.println("start \"\" \"" + exeToLaunch + "\"");
                }
                pw.println();
                pw.println(":: Auto-eliminar script temporal y salir");
                pw.println("(goto) 2>nul & del \"%~f0\"");
                pw.println("exit");
            }

            // Lanzar el script .bat con ProcessBuilder
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "start", "\"Actualizando DroAI\"", tempBat.getAbsolutePath());
            pb.start();

            // Salir limpiamente de la aplicación actual para liberar los archivos
            System.exit(0);

        } catch (Exception e) {
            logger.error("[AutoUpdate] Error ejecutando proceso de actualización: {}", e.getMessage(), e);
        }
    }

    /**
     * Muestra una ventana Swing elegante y clara mientras se prepara la actualización.
     */
    private static void mostrarDialogoActualizacion() {
        try {
            JDialog dialog = new JDialog((Frame) null, "Actualización Automática - DroAI", false);
            dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            dialog.setResizable(false);
            IconHelper.applyAppIcon(dialog);

            JPanel root = new JPanel(new BorderLayout(18, 16));
            root.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));

            JLabel lblLogo = new JLabel(IconHelper.getAppImageIcon(48, 48));
            root.add(lblLogo, BorderLayout.WEST);

            JPanel infoPanel = new JPanel(new GridLayout(2, 1, 4, 4));
            JLabel lblTitulo = new JLabel("Nueva versión detectada en el servidor");
            lblTitulo.setFont(lblTitulo.getFont().deriveFont(Font.BOLD, 14f));

            JLabel lblSub = new JLabel("DroAI se reiniciará automáticamente con la última versión...");
            lblSub.setForeground(UIManager.getColor("Label.disabledForeground") != null ?
                    UIManager.getColor("Label.disabledForeground") : Color.GRAY);

            infoPanel.add(lblTitulo);
            infoPanel.add(lblSub);
            root.add(infoPanel, BorderLayout.CENTER);

            JProgressBar progress = new JProgressBar();
            progress.setIndeterminate(true);
            progress.setPreferredSize(new Dimension(360, 10));
            root.add(progress, BorderLayout.SOUTH);

            dialog.setContentPane(root);
            dialog.pack();
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);

            // Breve pausa para renderizado y que el usuario vea la notificación
            Thread.sleep(700);

        } catch (Exception ignored) {}
    }
}

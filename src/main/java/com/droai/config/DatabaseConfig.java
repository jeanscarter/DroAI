package com.droai.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton HikariCP connection pool para SQL Server (DROA_A_DEV).
 * Configurar SERVER, USER y PASSWORD antes de ejecutar.
 */
public class DatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

    private static String SERVER;
    private static String PORT;
    private static String DATABASE;
    private static String USER;
    private static String PASSWORD;

    static {
        loadProperties();
    }

    private static void loadProperties() {
        Properties props = new Properties();
        File propFile = null;
        
        // Lista de posibles rutas para buscar db.properties
        java.util.List<File> candidateFiles = new java.util.ArrayList<>();
        
        // 1. Directorio de trabajo actual
        candidateFiles.add(new File("db.properties"));
        
        try {
            java.net.URL codeSourceUrl = DatabaseConfig.class.getProtectionDomain().getCodeSource().getLocation();
            if (codeSourceUrl != null) {
                File jarPath = new File(codeSourceUrl.toURI()).getParentFile();
                if (jarPath != null) {
                    // 2. Junto al JAR / clases
                    candidateFiles.add(new File(jarPath, "db.properties"));
                    
                    // 3. Un nivel arriba (ej: si se ejecuta desde target/classes o target/)
                    File parentDir = jarPath.getParentFile();
                    if (parentDir != null) {
                        candidateFiles.add(new File(parentDir, "db.properties"));
                        
                        // 4. Dos niveles arriba (ej: si se ejecuta desde target/classes/...)
                        File grandparentDir = parentDir.getParentFile();
                        if (grandparentDir != null) {
                            candidateFiles.add(new File(grandparentDir, "db.properties"));
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("No se pudo resolver las rutas relativas al JAR para buscar db.properties: {}", e.getMessage());
        }

        // Buscar el primer archivo existente
        for (File candidate : candidateFiles) {
            if (candidate.exists()) {
                propFile = candidate;
                break;
            }
        }

        // Si no se encuentra ninguno, usar el primero como referencia en el log
        if (propFile == null) {
            propFile = new File("db.properties");
        }

        logger.info("Intentando cargar propiedades de base de datos desde: {}", propFile.getAbsolutePath());

        if (propFile.exists()) {
            try (FileInputStream in = new FileInputStream(propFile)) {
                props.load(in);
                SERVER = props.getProperty("DB_SERVER", "srvdb0101");
                PORT = props.getProperty("DB_PORT", "1433");
                DATABASE = props.getProperty("DB_DATABASE", "DROA_A");
                USER = props.getProperty("DB_USER", "profit");
                PASSWORD = props.getProperty("DB_PASSWORD", new String(java.util.Base64.getDecoder().decode("cHJvZml0")));
                logger.info("Propiedades cargadas correctamente. Servidor: {}", SERVER);
                return;
            } catch (IOException e) {
                logger.error("Error al leer el archivo db.properties en la ruta: {}", propFile.getAbsolutePath(), e);
            }
        } else {
            logger.warn("No se encontró el archivo db.properties. Se usarán valores predeterminados de producción/desarrollo.");
        }

        // Valores por defecto (apuntando al servidor de producción/desarrollo srvdb0101)
        SERVER = "srvdb0101";
        PORT = "1433";
        DATABASE = "DROA_A";
        USER = "profit";
        PASSWORD = new String(java.util.Base64.getDecoder().decode("cHJvZml0"));
        logger.info("Usando valores por defecto de conexión. Servidor: {}", SERVER);
    }

    private static volatile HikariDataSource dataSource;

    private DatabaseConfig() {}

    public static DataSource getDataSource() {
        if (dataSource == null) {
            synchronized (DatabaseConfig.class) {
                if (dataSource == null) {
                    HikariConfig cfg = new HikariConfig();
                    cfg.setJdbcUrl("jdbc:sqlserver://" + SERVER + ":" + PORT
                            + ";databaseName=" + DATABASE
                            + ";encrypt=false;trustServerCertificate=true");
                    cfg.setUsername(USER);
                    cfg.setPassword(PASSWORD);
                    cfg.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
                    cfg.setMaximumPoolSize(5);
                    cfg.setMinimumIdle(2);
                    cfg.setConnectionTimeout(5_000);
                    cfg.setIdleTimeout(600_000);
                    cfg.setMaxLifetime(1_800_000);
                    cfg.setPoolName("DroAI-Pool");
                    dataSource = new HikariDataSource(cfg);
                }
            }
        }
        return dataSource;
    }

    public static java.sql.Connection getConnection() throws java.sql.SQLException {
        return getConnection(null);
    }

    public static java.sql.Connection getConnection(String databaseName) throws java.sql.SQLException {
        java.sql.Connection conn = getDataSource().getConnection();
        String targetCatalog = (databaseName != null && !databaseName.isBlank()) ? databaseName : DATABASE;
        if (targetCatalog != null && !targetCatalog.isBlank()) {
            try {
                if (!targetCatalog.equalsIgnoreCase(conn.getCatalog())) {
                    conn.setCatalog(targetCatalog);
                }
            } catch (java.sql.SQLException e) {
                logger.warn("No se pudo cambiar el catálogo/base de datos a {}: {}", targetCatalog, e.getMessage());
            }
        }
        return conn;
    }

    /**
     * Verifica la conexión con la base de datos.
     * Obtiene una conexión real del pool, la valida y la cierra inmediatamente.
     *
     * @return true si la conexión fue exitosa.
     * @throws RuntimeException con la causa si la conexión falla.
     */
    public static boolean testConnection() {
        try (java.sql.Connection conn = getDataSource().getConnection()) {
            boolean valid = conn.isValid(5); // timeout de 5 segundos
            if (valid) {
                logger.info("[DroAI-DB] ✔ Conexión exitosa a: {}", conn.getMetaData().getURL());
            } else {
                logger.error("[DroAI-DB] ✘ La conexión no es válida.");
            }
            return valid;
        } catch (java.sql.SQLException e) {
            logger.error("[DroAI-DB] ✘ Error al conectar a SQL Server en el host {}: {}", SERVER, e.getMessage());
            throw new RuntimeException("No se pudo conectar a la base de datos: " + e.getMessage(), e);
        }
    }

    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}

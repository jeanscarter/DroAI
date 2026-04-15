package com.droai.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Singleton HikariCP connection pool para SQL Server (DROA_A_DEV).
 * Configurar SERVER, USER y PASSWORD antes de ejecutar.
 */
public class DatabaseConfig {

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
        try (FileInputStream in = new FileInputStream("db.properties")) {
            props.load(in);
            SERVER = props.getProperty("DB_SERVER", "localhost");
            PORT = props.getProperty("DB_PORT", "1433");
            DATABASE = props.getProperty("DB_DATABASE", "DROA_A_DEV");
            USER = props.getProperty("DB_USER", "sa");
            PASSWORD = props.getProperty("DB_PASSWORD", "");
        } catch (IOException e) {
            System.err.println("Advertencia: No se pudo cargar db.properties. Asegúrese de crearlo a partir de db.properties.example.");
            e.printStackTrace();
            // Default fallbacks in case file isn't found
            SERVER = "localhost";
            PORT = "1433";
            DATABASE = "DROA_A_DEV";
            USER = "sa";
            PASSWORD = "";
        }
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
                    cfg.setConnectionTimeout(30_000);
                    cfg.setIdleTimeout(600_000);
                    cfg.setMaxLifetime(1_800_000);
                    cfg.setPoolName("DroAI-Pool");
                    dataSource = new HikariDataSource(cfg);
                }
            }
        }
        return dataSource;
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
                System.out.println("[DroAI-DB] ✔ Conexión exitosa a: "
                        + conn.getMetaData().getURL());
            } else {
                System.err.println("[DroAI-DB] ✘ La conexión no es válida.");
            }
            return valid;
        } catch (java.sql.SQLException e) {
            System.err.println("[DroAI-DB] ✘ Error al conectar: " + e.getMessage());
            throw new RuntimeException("No se pudo conectar a la base de datos: " + e.getMessage(), e);
        }
    }

    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}

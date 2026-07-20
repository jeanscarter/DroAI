package com.droai.model;

import java.net.InetAddress;

/**
 * Sesión de usuario autenticado contra Profit Plus (tabla {@code tusers}).
 *
 * <p>Almacena código de usuario, nombre, nivel de acceso y nombre de máquina para:
 * <ul>
 *   <li>Auditoría: inyectar {@code co_us_in}/{@code co_us_mo} en operaciones de BD.</li>
 *   <li>Trazabilidad: registrar el hostname (máquina) desde donde se realizan cambios,
 *       tal como hace Profit Plus en las tablas {@code saPista_*}.</li>
 *   <li>Control de acceso: restringir funcionalidades según nivel.</li>
 * </ul>
 *
 * <p>Niveles de Profit Plus:
 * <pre>
 *   0 = Administrador del sistema
 *   1 = Supervisor
 *   2 = Administrador
 *   3 = Operador
 *   4 = Cajero
 * </pre>
 */
public class SesionUsuario {

    private static volatile SesionUsuario instancia;

    private final String coUsuario;      // codusu de tusers
    private final String nombreUsuario;  // nombre de tusers
    private final int nivel;             // nivel de acceso
    private final String maquina;        // hostname del equipo (para auditoría Profit)

    private SesionUsuario(String coUsuario, String nombreUsuario, int nivel, String maquina) {
        this.coUsuario = coUsuario;
        this.nombreUsuario = nombreUsuario;
        this.nivel = nivel;
        this.maquina = maquina;
    }

    /**
     * Establece la sesión activa tras autenticación exitosa.
     * Captura automáticamente el nombre del equipo (hostname) para auditoría.
     */
    public static void iniciar(String coUsuario, String nombreUsuario, int nivel) {
        String hostname = resolverHostname();
        instancia = new SesionUsuario(coUsuario, nombreUsuario, nivel, hostname);
        System.out.println("[SesionUsuario] ✔ Sesión iniciada: "
                + coUsuario + " (" + nombreUsuario + ") — Nivel: " + nivel
                + " — Máquina: " + hostname);
    }

    /** Cierra la sesión activa. */
    public static void cerrar() { instancia = null; }

    /** @return sesión actual, o {@code null} si no hay sesión activa. */
    public static SesionUsuario current() { return instancia; }

    /** @return true si hay una sesión activa. */
    public static boolean isAutenticado() { return instancia != null; }

    public String getCoUsuario() { return coUsuario; }
    public String getNombreUsuario() { return nombreUsuario; }
    public int getNivel() { return nivel; }

    /**
     * Nombre del equipo (hostname) desde donde se inició la sesión.
     * Equivalente al campo {@code maquina} en las tablas {@code saPista_*} de Profit Plus.
     *
     * @return hostname del equipo, o "UNKNOWN" si no se pudo resolver.
     */
    public String getMaquina() { return maquina; }

    /**
     * Descripción textual del nivel de acceso.
     */
    public String getNivelDescripcion() {
        return switch (nivel) {
            case 0 -> "Administrador Sistema";
            case 1 -> "Supervisor";
            case 2 -> "Administrador";
            case 3 -> "Operador";
            case 4 -> "Cajero";
            default -> "Nivel " + nivel;
        };
    }

    @Override
    public String toString() {
        return "SesionUsuario{codusu='%s', nombre='%s', nivel=%d, maquina='%s'}"
                .formatted(coUsuario, nombreUsuario, nivel, maquina);
    }

    // ═══════════════════════════════════════════════════════════════
    //  Resolución de hostname
    // ═══════════════════════════════════════════════════════════════

    /**
     * Resuelve el nombre del equipo (hostname) de la máquina actual.
     * <p>Prioridad:
     * <ol>
     *   <li>Variable de entorno {@code COMPUTERNAME} (Windows nativo, más rápido)</li>
     *   <li>Variable de entorno {@code HOSTNAME} (Linux/Mac)</li>
     *   <li>{@code InetAddress.getLocalHost().getHostName()} (fallback universal)</li>
     * </ol>
     *
     * @return hostname del equipo, o "UNKNOWN" si no se pudo resolver.
     */
    private static String resolverHostname() {
        try {
            // Windows: COMPUTERNAME siempre disponible y rápido
            String computerName = System.getenv("COMPUTERNAME");
            if (computerName != null && !computerName.isBlank()) {
                return computerName.trim().toUpperCase();
            }
            // Linux/Mac fallback
            String hostname = System.getenv("HOSTNAME");
            if (hostname != null && !hostname.isBlank()) {
                return hostname.trim().toUpperCase();
            }
            // Fallback universal via DNS
            return InetAddress.getLocalHost().getHostName().toUpperCase();
        } catch (Exception e) {
            System.err.println("[SesionUsuario] ⚠ No se pudo resolver el hostname: " + e.getMessage());
            return "UNKNOWN";
        }
    }
}


package com.droai.model;

/**
 * Sesión de usuario autenticado contra Profit Plus (tabla {@code tusers}).
 *
 * <p>Almacena código de usuario, nombre y nivel de acceso para:
 * <ul>
 *   <li>Auditoría: inyectar {@code co_us_in}/{@code co_us_mo} en operaciones de BD.</li>
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

    private SesionUsuario(String coUsuario, String nombreUsuario, int nivel) {
        this.coUsuario = coUsuario;
        this.nombreUsuario = nombreUsuario;
        this.nivel = nivel;
    }

    /**
     * Establece la sesión activa tras autenticación exitosa.
     */
    public static void iniciar(String coUsuario, String nombreUsuario, int nivel) {
        instancia = new SesionUsuario(coUsuario, nombreUsuario, nivel);
        System.out.println("[SesionUsuario] ✔ Sesión iniciada: "
                + coUsuario + " (" + nombreUsuario + ") — Nivel: " + nivel);
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
        return "SesionUsuario{codusu='%s', nombre='%s', nivel=%d}"
                .formatted(coUsuario, nombreUsuario, nivel);
    }
}

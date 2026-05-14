package com.droai.service;

import com.droai.dao.UsuarioDAO;
import com.droai.dao.UsuarioDAO.AuthResult;
import com.droai.model.SesionUsuario;

import java.sql.SQLException;

/**
 * Servicio de autenticación contra la tabla {@code tusers} de Profit Plus.
 * Valida credenciales y establece la sesión global ({@link SesionUsuario}).
 */
public class UsuarioService {

    private final UsuarioDAO dao;

    public UsuarioService() {
        this.dao = new UsuarioDAO();
    }

    /**
     * Autentica un usuario y establece la sesión global.
     *
     * @param usuario  código de usuario (codusu).
     * @param password contraseña (clave).
     * @return true si la autenticación fue exitosa.
     * @throws AutenticacionException si las credenciales son inválidas.
     * @throws SQLException si hay un error de conexión a la BD.
     */
    public boolean autenticar(String usuario, String password)
            throws AutenticacionException, SQLException {

        AuthResult result = dao.autenticar(usuario, password);

        if (result == null) {
            throw new AutenticacionException(
                    "Credenciales inválidas. Verifique el usuario y contraseña de Profit Plus.");
        }

        SesionUsuario.iniciar(result.coUsuario(), result.nombre(), result.nivel());
        return true;
    }

    /** Cierra la sesión activa. */
    public void cerrarSesion() {
        SesionUsuario.cerrar();
    }

    /**
     * Excepción específica para errores de autenticación.
     */
    public static class AutenticacionException extends Exception {
        public AutenticacionException(String message) {
            super(message);
        }
    }
}

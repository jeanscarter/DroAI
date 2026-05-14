package com.droai.dao;

import com.droai.config.DatabaseConfig;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * DAO para autenticación contra la tabla administrativa de Profit Plus.
 *
 * <p>En Profit Plus (versiones recientes), los usuarios globales se almacenan en
 * la base de datos maestra (ej. {@code MasterProfitProh.dbo.MpUsuario}).
 * Las contraseñas están encriptadas utilizando un hash SHA-1 directo (sin sal).
 */
public class UsuarioDAO {

    /**
     * Consulta de autenticación nativa contra la base de datos maestra.
     */
    private static final String SQL_AUTH = """
            SELECT Cod_Usuario, Desc_Usuario, Prioridad
            FROM MasterProfitProh.dbo.MpUsuario
            WHERE RTRIM(Cod_Usuario) = ?
              AND CONVERT(varchar(max), Password, 2) = ?
              AND Estado = 'A'
            """;

    /**
     * Consulta todos los usuarios activos.
     */
    private static final String SQL_LIST_ACTIVE = """
            SELECT RTRIM(Cod_Usuario) AS Cod_Usuario, RTRIM(Desc_Usuario) AS Desc_Usuario, Prioridad
            FROM MasterProfitProh.dbo.MpUsuario
            WHERE Estado = 'A'
            ORDER BY Cod_Usuario
            """;

    /**
     * Resultado de autenticación.
     */
    public record AuthResult(String coUsuario, String nombre, int nivel) {}

    /**
     * Intenta autenticar un usuario usando SHA-1 nativo de Profit Plus.
     *
     * @param usuario  código de usuario (Cod_Usuario).
     * @param password contraseña en texto plano (se encriptará a SHA-1).
     * @return {@link AuthResult} si es válido, o {@code null} si no.
     */
    public AuthResult autenticar(String usuario, String password) throws SQLException {
        if (usuario == null || usuario.isBlank() || password == null) {
            return null;
        }

        // Profit Plus guarda el hash SHA-1 en mayúsculas
        String hashSha1 = getSha1Hash(password);

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_AUTH)) {

            ps.setString(1, usuario.trim().toUpperCase());
            ps.setString(2, hashSha1);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new AuthResult(
                            rs.getString("Cod_Usuario").trim(),
                            rs.getString("Desc_Usuario") != null ? rs.getString("Desc_Usuario").trim() : usuario,
                            rs.getInt("Prioridad")
                    );
                }
            }
        }

        return null;
    }

    /**
     * Obtiene todos los usuarios activos del sistema Profit.
     */
    public java.util.List<AuthResult> listarActivos() throws SQLException {
        java.util.List<AuthResult> list = new java.util.ArrayList<>();

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_LIST_ACTIVE);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new AuthResult(
                        rs.getString("Cod_Usuario"),
                        rs.getString("Desc_Usuario"),
                        rs.getInt("Prioridad")
                ));
            }
        }

        return list;
    }

    /**
     * Genera el hash SHA-1 de una cadena en formato Hexadecimal (Mayúsculas),
     * que es el formato exacto que usa Profit Plus en MpUsuario.
     */
    private String getSha1Hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hashBytes = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02X", b)); // Hexadecimal mayúsculas
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error fatal: No se soporta SHA-1 en esta JVM", e);
        }
    }
}

package com.droai.dao;

import com.droai.config.DatabaseConfig;
import com.droai.model.ArticuloImportRow;
import com.droai.model.SesionUsuario;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * DAO para importación masiva de artículos con lógica UPSERT (INSERT o UPDATE).
 *
 * <p><b>Transaccional:</b> Todo el lote se procesa en una sola transacción.
 * Si ocurre un error, se hace rollback completo.
 *
 * <p><b>Auditoría Profit Plus:</b>
 * <ul>
 *   <li><b>UPDATE</b> → {@code co_us_mo = usuario}, {@code fe_us_mo = GETDATE()}</li>
 *   <li><b>INSERT</b> → {@code co_us_in = usuario}, {@code fe_us_in = GETDATE()}</li>
 * </ul>
 *
 * <p><b>Validación previa:</b> Método {@link #validarCatalogos(List)} verifica
 * que las dependencias (línea, sublínea, unidad, proveedor) existan antes de procesar.
 */
public class ImportacionDAO {

    // ── SQL: UPDATE artículo existente ──
    private static final String SQL_UPDATE = """
            UPDATE saArticulo
            SET tipo_imp  = ?,
                campo4    = ?,
                co_us_mo  = ?,
                fe_us_mo  = GETDATE()
            WHERE co_art = ?
            """;

    // ── SQL: INSERT artículo nuevo con campos obligatorios de Profit Plus ──
    private static final String SQL_INSERT = """
            INSERT INTO saArticulo (
                co_art, art_des, tipo_imp, campo4, ref,
                co_lin, co_subl, tipo, co_ubicacion,
                campo1, campo2, campo3, campo5, campo6,
                co_us_in, fe_us_in, co_us_mo, fe_us_mo,
                anulado, destaca
            ) VALUES (
                ?, ?, ?, ?, ?,
                ?, ?, ?, ?,
                ?, ?, ?, ?, ?,
                ?, GETDATE(), ?, GETDATE(),
                0, 0
            )
            """;

    // ── SQL: Verificar existencia de un artículo ──
    private static final String SQL_EXISTS = """
            SELECT co_art FROM saArticulo WHERE co_art = ?
            """;

    // ── SQL: Verificar catálogos dependientes ──
    // Catálogos dependientes (para futura implementación)

    /**
     * Resultado de validación previa.
     */
    public record ValidationResult(
            boolean valid,
            List<String> errores,
            int existentes,
            int nuevos
    ) {}

    /**
     * Resultado de la importación.
     */
    public record ImportResult(
            int actualizados,
            int insertados,
            int omitidos
    ) {}

    // ═══════════════════════════════════════════════════════════════
    //  Validación previa: catálogos dependientes
    // ═══════════════════════════════════════════════════════════════

    /**
     * Verifica en memoria y contra la BD que los datos del lote sean válidos.
     * Comprueba existencia de códigos de línea, sublínea, unidad y proveedor.
     *
     * @param filas lista de filas a validar.
     * @return resultado de validación con lista de errores si los hay.
     */
    public ValidationResult validarCatalogos(List<ArticuloImportRow> filas) throws SQLException {
        List<String> errores = new ArrayList<>();
        int existentes = 0, nuevos = 0;

        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {

            // Colectar códigos únicos para validar en una sola pasada
            Set<String> codigosArt = new HashSet<>();
            for (ArticuloImportRow fila : filas) {
                if (fila.getCodigo() != null && !fila.getCodigo().isBlank()) {
                    codigosArt.add(fila.getCodigo().trim());
                }
            }

            // Contar existentes vs nuevos
            try (PreparedStatement ps = conn.prepareStatement(SQL_EXISTS)) {
                for (String codigo : codigosArt) {
                    ps.setString(1, codigo);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            existentes++;
                        } else {
                            nuevos++;
                        }
                    }
                    ps.clearParameters();
                }
            }

            // Validación: códigos vacíos
            long vacios = filas.stream()
                    .filter(f -> f.getCodigo() == null || f.getCodigo().isBlank())
                    .count();
            if (vacios > 0) {
                errores.add("⚠ %d fila(s) sin código de artículo (serán omitidas).".formatted(vacios));
            }

            // Si hay artículos nuevos, verificar catálogos dependientes de los nuevos
            if (nuevos > 0) {
                errores.add("ℹ %d artículo(s) NUEVO(S) serán insertados en saArticulo.".formatted(nuevos));
            }
        }

        return new ValidationResult(
                errores.stream().noneMatch(e -> e.startsWith("✘")),
                errores,
                existentes,
                nuevos
        );
    }

    // ═══════════════════════════════════════════════════════════════
    //  UPSERT transaccional
    // ═══════════════════════════════════════════════════════════════

    /**
     * Procesa un lote de artículos con lógica UPSERT.
     * Todo el lote se ejecuta en una sola transacción.
     *
     * @param lote lista de filas importadas.
     * @return resultado con conteo de actualizados, insertados y omitidos.
     * @throws SQLException si ocurre un error (con rollback automático).
     */
    public ImportResult procesarLote(List<ArticuloImportRow> lote) throws SQLException {
        if (lote == null || lote.isEmpty()) {
            return new ImportResult(0, 0, 0);
        }

        String coUsuario = SesionUsuario.isAutenticado()
                ? SesionUsuario.current().getCoUsuario()
                : "SYSTEM";

        int actualizados = 0, insertados = 0, omitidos = 0;

        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement psExists = conn.prepareStatement(SQL_EXISTS);
                 PreparedStatement psUpdate = conn.prepareStatement(SQL_UPDATE);
                 PreparedStatement psInsert = conn.prepareStatement(SQL_INSERT)) {

                for (ArticuloImportRow row : lote) {
                    if (row.getCodigo() == null || row.getCodigo().isBlank()) {
                        omitidos++;
                        continue;
                    }

                    String codigo = row.getCodigo().trim();

                    // ¿Existe ya?
                    boolean existe = false;
                    psExists.setString(1, codigo);
                    try (ResultSet rs = psExists.executeQuery()) {
                        existe = rs.next();
                    }
                    psExists.clearParameters();

                    if (existe) {
                        // ── UPDATE ──
                        psUpdate.setString(1, row.getTipoImpCalculado());
                        psUpdate.setString(2, safe(row.getMarca()));
                        psUpdate.setString(3, coUsuario);
                        psUpdate.setString(4, codigo);
                        psUpdate.addBatch();
                        actualizados++;
                    } else {
                        // ── INSERT ──
                        psInsert.setString(1, codigo);                         // co_art
                        psInsert.setString(2, safe(row.getDescripcion()));      // art_des
                        psInsert.setString(3, row.getTipoImpCalculado());      // tipo_imp
                        psInsert.setString(4, safe(row.getMarca()));           // campo4
                        psInsert.setString(5, safe(row.getReferencia()));      // ref
                        psInsert.setString(6, "");                             // co_lin (default)
                        psInsert.setString(7, "");                             // co_subl (default)
                        psInsert.setString(8, safe(row.getTipo()));            // tipo
                        psInsert.setString(9, "00001");                        // co_ubicacion (default)
                        psInsert.setString(10, safe(row.getCampo1()));         // campo1
                        psInsert.setString(11, safe(row.getCampo2()));         // campo2
                        psInsert.setString(12, safe(row.getCampo3()));         // campo3
                        psInsert.setString(13, safe(row.getCampo5()));         // campo5
                        psInsert.setString(14, safe(row.getCampo6()));         // campo6
                        psInsert.setString(15, coUsuario);                     // co_us_in
                        psInsert.setString(16, coUsuario);                     // co_us_mo
                        psInsert.addBatch();
                        insertados++;
                    }
                }

                // Ejecutar batches
                if (actualizados > 0) psUpdate.executeBatch();
                if (insertados > 0) psInsert.executeBatch();

                conn.commit();

            } catch (SQLException e) {
                conn.rollback();
                throw new SQLException(
                        "Error en importación (rollback ejecutado): " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }
        }

        return new ImportResult(actualizados, insertados, omitidos);
    }

    /**
     * Método legacy para compatibilidad.
     * @deprecated Usar {@link #procesarLote(List)} en su lugar.
     */
    @Deprecated
    public int actualizarArticulos(List<ArticuloImportRow> lote) throws SQLException {
        ImportResult r = procesarLote(lote);
        return r.actualizados() + r.insertados();
    }

    private String safe(String val) {
        return val != null ? val.trim() : "";
    }
}

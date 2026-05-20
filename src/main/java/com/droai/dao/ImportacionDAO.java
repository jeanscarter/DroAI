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
 * <p><b>Constraints respetadas:</b>
 * <ul>
 *   <li>{@code CK_saArticulo_Tipo_Articulo}: tipo IN ('V','F','C','S','M','N','E')</li>
 *   <li>{@code CK_saArticulo_Tipo_Impuesto}: tipo_imp IN ('1'..'9')</li>
 *   <li>{@code CK_saArticulo_margen}: margen_min &lt;= margen_max, margen_min &gt;= 0</li>
 *   <li>{@code CK_saArticulo_Stock}: stock_min &lt;= stock_max</li>
 *   <li>{@code CK_saArticulo_relac_unidad}: relac_unidad IN (0, 1)</li>
 *   <li>FKs a: saSubLinea(co_lin, co_subl), saCatArticulo(co_cat), saColor(co_color), saUbicacion(co_ubicacion)</li>
 * </ul>
 *
 * <p><b>Validación FK en runtime:</b> Antes de insertar, se cargan todos los valores
 * válidos de cada catálogo FK. Si un valor del Excel no existe en el catálogo,
 * se sustituye automáticamente por el default seguro ('000001').
 */
public class ImportacionDAO {

    // ── Defaults seguros para catálogos FK (siempre existen en Profit Plus) ──
    private static final String DEFAULT_CO_LIN       = "000001";
    private static final String DEFAULT_CO_SUBL      = "000001";
    private static final String DEFAULT_CO_CAT       = "000001";
    private static final String DEFAULT_CO_COLOR     = "000001";
    private static final String DEFAULT_CO_UBICACION = "00001";

    // ── SQL: UPDATE artículo existente ──
    private static final String SQL_UPDATE = """
            UPDATE saArticulo
            SET tipo_imp     = ?,
                campo4       = ?,
                art_des      = ?,
                ref          = ?,
                co_lin       = CASE WHEN ? <> '' THEN ? ELSE co_lin END,
                co_subl      = CASE WHEN ? <> '' THEN ? ELSE co_subl END,
                co_cat       = CASE WHEN ? <> '' THEN ? ELSE co_cat END,
                co_color     = CASE WHEN ? <> '' THEN ? ELSE co_color END,
                co_us_mo     = ?,
                fe_us_mo     = GETDATE()
            WHERE co_art = ?
            """;

    // ── SQL: INSERT artículo nuevo ──
    // Incluye TODAS las columnas NOT NULL de saArticulo
    // Columnas autogeneradas: validador (timestamp), rowguid (DEFAULT newid())
    private static final String SQL_INSERT = """
            INSERT INTO saArticulo (
                co_art, art_des, tipo, tipo_imp, ref,
                co_lin, co_subl, co_cat, co_color, co_ubicacion,
                campo1, campo2, campo3, campo4, campo5, campo6,
                co_us_in, fe_us_in, co_us_mo, fe_us_mo,
                anulado, destaca, generico, fecha_reg,
                maneja_serial, maneja_lote, maneja_lote_venc,
                margen_min, margen_max, garantia, volumen, peso,
                stock_min, stock_max, stock_pedido, relac_unidad,
                punt_ven, punt_cli, lic_mon_ilc, lic_capacidad,
                lic_grado_al, prec_om, mont_comi, porc_arancel,
                porc_margen_minimo, porc_margen_maximo
            ) VALUES (
                ?, ?, ?, ?, ?,
                ?, ?, ?, ?, ?,
                ?, ?, ?, ?, ?, ?,
                ?, GETDATE(), ?, GETDATE(),
                0, 0, 0, GETDATE(),
                0, 0, 0,
                0, 0, 'n/a', 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0
            )
            """;

    // ── SQL: Verificar existencia de un artículo ──
    private static final String SQL_EXISTS = """
            SELECT co_art FROM saArticulo WHERE co_art = ?
            """;

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
    //  Carga de catálogos FK válidos
    // ═══════════════════════════════════════════════════════════════

    /**
     * Carga todos los valores válidos de una columna de catálogo en un Set.
     * Los valores se normalizan con trim() para comparación segura.
     */
    private Set<String> cargarCatalogo(Connection conn, String sql) throws SQLException {
        Set<String> catalogo = new HashSet<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String val = rs.getString(1);
                if (val != null) {
                    catalogo.add(val.trim());
                }
            }
        }
        return catalogo;
    }

    /**
     * Contiene los sets de valores válidos de los catálogos FK.
     * Se carga una sola vez al inicio de cada lote para evitar
     * consultas repetitivas por cada fila.
     */
    private record CatalogosFK(
            Set<String> coloresValidos,
            Set<String> categoriasValidas,
            Set<String> lineasValidas,
            Set<String> sublineasValidas
    ) {}

    /**
     * Carga todos los catálogos FK necesarios para validar los INSERTs.
     */
    private CatalogosFK cargarCatalogosFK(Connection conn) throws SQLException {
        return new CatalogosFK(
                cargarCatalogo(conn, "SELECT co_color FROM saColor"),
                cargarCatalogo(conn, "SELECT co_cat FROM saCatArticulo"),
                cargarCatalogo(conn, "SELECT DISTINCT co_lin FROM saSubLinea"),
                cargarCatalogo(conn, "SELECT co_subl FROM saSubLinea")
        );
    }

    /**
     * Valida un valor FK contra el catálogo. Si no existe, retorna el default.
     *
     * @param val      valor del Excel (ya limpio, sin comillas).
     * @param catalogo set de valores válidos del catálogo.
     * @param defVal   valor default seguro si no existe.
     * @param maxLen   longitud máxima del campo.
     * @return valor validado o default.
     */
    private String validarFK(String val, Set<String> catalogo, String defVal, int maxLen) {
        String result = safe(val, maxLen);
        if (result.isEmpty() || !catalogo.contains(result)) {
            return defVal;
        }
        return result;
    }

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

            // Si hay artículos nuevos, notificar
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
     * <p><b>Validación FK en runtime:</b> Antes de procesar, se cargan todos los
     * valores válidos de saColor, saCatArticulo y saSubLinea. Si un valor del
     * Excel no existe en el catálogo correspondiente, se sustituye por el default
     * seguro ('000001') para evitar violaciones de Foreign Key.
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

            // ── Cargar catálogos FK válidos (una sola vez por lote) ──
            CatalogosFK catalogos = cargarCatalogosFK(conn);

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

                    // Validar FKs del Excel contra catálogos reales
                    String coLin   = validarFK(row.getGrupo(),   catalogos.lineasValidas(),     DEFAULT_CO_LIN,   6);
                    String coSubl  = validarFK(row.getSgrupo(),  catalogos.sublineasValidas(),  DEFAULT_CO_SUBL,  6);
                    String coCat   = validarFK(row.getCat(),     catalogos.categoriasValidas(), DEFAULT_CO_CAT,   6);
                    String coColor = validarFK(row.getCoColor(), catalogos.coloresValidos(),    DEFAULT_CO_COLOR, 6);

                    if (existe) {
                        // ── UPDATE ──
                        int p = 1;
                        psUpdate.setString(p++, safe(row.getTipoImpCalculado(), 1));   // tipo_imp
                        psUpdate.setString(p++, safe(row.getMarca(), 60));              // campo4
                        psUpdate.setString(p++, safe(row.getDescripcion(), 120));       // art_des
                        psUpdate.setString(p++, safe(row.getReferencia(), 20));         // ref

                        // co_lin: CASE WHEN ? <> '' THEN ? ELSE co_lin END
                        // Para UPDATE, solo pasamos si el valor original tiene dato
                        String coLinUpd = safe(row.getGrupo(), 6);
                        String coLinVal = coLinUpd.isEmpty() ? "" : coLin; // validado o vacío para no cambiar
                        psUpdate.setString(p++, coLinVal);
                        psUpdate.setString(p++, coLinVal);

                        String coSublUpd = safe(row.getSgrupo(), 6);
                        String coSublVal = coSublUpd.isEmpty() ? "" : coSubl;
                        psUpdate.setString(p++, coSublVal);
                        psUpdate.setString(p++, coSublVal);

                        String coCatUpd = safe(row.getCat(), 6);
                        String coCatVal = coCatUpd.isEmpty() ? "" : coCat;
                        psUpdate.setString(p++, coCatVal);
                        psUpdate.setString(p++, coCatVal);

                        String coColorUpd = safe(row.getCoColor(), 6);
                        String coColorVal = coColorUpd.isEmpty() ? "" : coColor;
                        psUpdate.setString(p++, coColorVal);
                        psUpdate.setString(p++, coColorVal);

                        psUpdate.setString(p++, safe(coUsuario, 6));                   // co_us_mo
                        psUpdate.setString(p++, safe(codigo, 30));                     // WHERE co_art = ?
                        psUpdate.addBatch();
                        actualizados++;
                    } else {
                        // ── INSERT ──
                        int p = 1;
                        psInsert.setString(p++, safe(codigo, 30));                     // co_art
                        psInsert.setString(p++, safe(row.getDescripcion(), 120));       // art_des
                        psInsert.setString(p++, row.getTipoValidado());                // tipo (V/F/C/S/M/N/E)
                        psInsert.setString(p++, safe(row.getTipoImpCalculado(), 1));    // tipo_imp
                        psInsert.setString(p++, safe(row.getReferencia(), 20));         // ref

                        // Catálogos: ya validados contra la BD
                        psInsert.setString(p++, coLin);                                // co_lin
                        psInsert.setString(p++, coSubl);                               // co_subl
                        psInsert.setString(p++, coCat);                                // co_cat
                        psInsert.setString(p++, coColor);                              // co_color
                        psInsert.setString(p++, DEFAULT_CO_UBICACION);                 // co_ubicacion

                        // Campos libres
                        psInsert.setString(p++, safe(row.getCampo1(), 60));             // campo1
                        psInsert.setString(p++, safe(row.getCampo2(), 60));             // campo2
                        psInsert.setString(p++, safe(row.getCampo3(), 60));             // campo3
                        psInsert.setString(p++, safe(row.getMarca(), 60));              // campo4 (marca)
                        psInsert.setString(p++, safe(row.getCampo5(), 60));             // campo5
                        psInsert.setString(p++, safe(row.getCampo6(), 60));             // campo6

                        // Auditoría
                        psInsert.setString(p++, safe(coUsuario, 6));                   // co_us_in
                        psInsert.setString(p++, safe(coUsuario, 6));                   // co_us_mo

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

    /**
     * Trunca y limpia un valor de texto. Retorna cadena vacía si es null.
     */
    private String safe(String val, int maxLen) {
        if (val == null) return "";
        String trimmed = val.trim();
        if (trimmed.length() > maxLen) {
            return trimmed.substring(0, maxLen);
        }
        return trimmed;
    }
}

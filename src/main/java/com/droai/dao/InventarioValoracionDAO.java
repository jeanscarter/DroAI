package com.droai.dao;

import com.droai.config.DatabaseConfig;
import com.droai.model.CapaCostoDTO;
import com.droai.model.InventarioValoradoRow;
import com.droai.model.KardexMovimientoValoradoRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/**
 * Acceso a datos para la Valuación de Inventarios por Capas FIFO y Kárdex Multimoneda.
 *
 * <p>Reglas de negocio aplicadas:
 * <ul>
 *   <li><b>Venta Firme:</b> Valuación por capas FIFO (PEPS) retroactivas en las compras
 *       (saNotaRecepcionCompra / saFacturaCompra) hasta completar la existencia al corte.</li>
 *   <li><b>Consignación:</b> Valuación con el último costo en USD reportado por compras
 *       (saCostoHistoricoEntrada tipo 'PROV') multiplicado por la tasa al corte.</li>
 *   <li><b>Reconstrucción histórica:</b> Reconstrucción del saldo retrospectivo en base a los
 *       movimientos de recepción, ventas, ajustes, traslados y devoluciones posteriores al corte.</li>
 * </ul>
 */
public class InventarioValoracionDAO {

    private static final Logger logger = LoggerFactory.getLogger(InventarioValoracionDAO.class);

    // Almacenes asignados para mercancía en consignación
    private static final Set<String> ALMACENES_CONSIGNACION = Set.of("02", "VC");

    /**
     * Fecha de corte de migración de la base de datos de Profit Plus.
     * Toda fecha previa o igual al 27/07/2026 se encuentra en DROA2_A (base histórica).
     * Toda fecha posterior al 27/07/2026 se encuentra en DROA_A (base actual de producción).
     */
    public static final LocalDate FECHA_CORTE_MIGRACION = LocalDate.of(2026, 7, 27);
    public static final String DB_ACTUAL = "DROA_A";
    public static final String DB_HISTORICA = "DROA2_A";

    /**
     * Determina la base de datos adecuada a consultar según la fecha de corte.
     *
     * @param fecha Fecha de corte o consulta
     * @return "DROA2_A" si la fecha es <= 27/07/2026, de lo contrario "DROA_A"
     */
    public static String resolverBaseDatos(LocalDate fecha) {
        if (fecha != null && !fecha.isAfter(FECHA_CORTE_MIGRACION)) {
            return DB_HISTORICA;
        }
        return DB_ACTUAL;
    }

    /**
     * Consulta la tasa de cambio vigente en saTasa para la fecha de corte dada.
     * Si no existe tasa para esa fecha exacta, busca la fecha inmediatamente anterior más reciente.
     */
    public double obtenerTasaUSD(LocalDate fechaCorte) {
        String targetDb = resolverBaseDatos(fechaCorte);
        String sql = """
            SELECT TOP 1 ISNULL(tasa_v, tasa_c) AS tasa
            FROM saTasa
            WHERE co_mone IN ('USD', 'US$')
              AND CAST(fecha AS DATE) <= ?
            ORDER BY fecha DESC, fe_us_mo DESC
            """;

        String sqlFallback = """
            SELECT TOP 1 ISNULL(tasa_v, tasa_c) AS tasa
            FROM saTasa
            WHERE co_mone IN ('USD', 'US$')
            ORDER BY fecha DESC, fe_us_mo DESC
            """;

        try (Connection conn = DatabaseConfig.getConnection(targetDb)) {
            if (fechaCorte != null) {
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setDate(1, java.sql.Date.valueOf(fechaCorte));
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            double t = rs.getDouble("tasa");
                            if (t > 0) return t;
                        }
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(sqlFallback);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double t = rs.getDouble("tasa");
                    if (t > 0) return t;
                }
            }
        } catch (SQLException e) {
            logger.error("[InventarioValoracionDAO] Error al consultar tasa USD en [{}]: {}", targetDb, e.getMessage(), e);
        }
        return 1.0;
    }

    /**
     * Reconstruye y calcula la valoración completa de inventario a la fecha de corte indicada.
     *
     * @param fechaCorte     Fecha de corte de la existencia (ej. 2026-08-31)
     * @param filtroTipo     "TODOS", "VENTA_FIRME" o "CONSIGNACION"
     * @param filtroAlmacen  Código de almacén específico (ej. "01", "02") o null/"TODOS"
     * @param filtroBusqueda Término de búsqueda para código, descripción o proveedor
     * @return Lista de artículos valorados con existencia > 0 a la fecha de corte
     */
    public List<InventarioValoradoRow> calcularInventarioValorado(LocalDate fechaCorte,
                                                                  String filtroTipo,
                                                                  String filtroAlmacen,
                                                                  String filtroBusqueda) {
        List<InventarioValoradoRow> resultado = new ArrayList<>();
        if (fechaCorte == null) {
            return resultado;
        }

        double tasaCorte = obtenerTasaUSD(fechaCorte);
        java.sql.Date sqlFechaCorte = java.sql.Date.valueOf(fechaCorte);

        // 1. Reconstrucción masiva de stock a fecha de corte
        String sqlStockCorte = """
            WITH StockActual AS (
                SELECT co_art, co_alma, SUM(stock) AS stock_actual
                FROM saStockAlmacen
                WHERE tipo = 'ACT'
                GROUP BY co_art, co_alma
            ),
            EntradasPost AS (
                SELECT r.co_art, r.co_alma, SUM(r.total_art) AS cant
                FROM saNotaRecepcionCompraReng r
                JOIN saNotaRecepcionCompra c ON r.doc_num = c.doc_num
                WHERE c.anulado = 0 AND CAST(c.fec_emis AS DATE) > ?
                GROUP BY r.co_art, r.co_alma
                UNION ALL
                SELECT r.co_art, r.co_alma, SUM(r.total_art) AS cant
                FROM saAjusteReng r
                JOIN saAjuste a ON r.ajue_num = a.ajue_num
                WHERE a.anulado = 0 AND r.co_tipo = 'EA' AND CAST(a.fecha AS DATE) > ?
                GROUP BY r.co_art, r.co_alma
                UNION ALL
                SELECT r.co_art, r.co_alma, SUM(r.total_art) AS cant
                FROM saDevolucionClienteReng r
                JOIN saDevolucionCliente d ON r.doc_num = d.doc_num
                WHERE d.anulado = 0 AND CAST(d.fec_emis AS DATE) > ?
                GROUP BY r.co_art, r.co_alma
                UNION ALL
                SELECT r.co_art, t.alm_dest AS co_alma, SUM(r.total_art) AS cant
                FROM saTrasladoReng r
                JOIN saTraslado t ON r.tras_num = t.tras_num
                WHERE t.anulado = 0 AND CAST(t.fecha AS DATE) > ?
                GROUP BY r.co_art, t.alm_dest
            ),
            SalidasPost AS (
                SELECT r.co_art, r.co_alma, SUM(r.total_art) AS cant
                FROM saNotaEntregaVentaReng r
                JOIN saNotaEntregaVenta n ON r.doc_num = n.doc_num
                WHERE n.anulado = 0 AND CAST(n.fec_emis AS DATE) > ?
                GROUP BY r.co_art, r.co_alma
                UNION ALL
                SELECT r.co_art, r.co_alma, SUM(r.total_art) AS cant
                FROM saFacturaVentaReng r
                JOIN saFacturaVenta f ON r.doc_num = f.doc_num
                WHERE f.anulado = 0 AND (r.num_doc IS NULL OR RTRIM(r.num_doc) = '') AND CAST(f.fec_emis AS DATE) > ?
                GROUP BY r.co_art, r.co_alma
                UNION ALL
                SELECT r.co_art, r.co_alma, SUM(r.total_art) AS cant
                FROM saAjusteReng r
                JOIN saAjuste a ON r.ajue_num = a.ajue_num
                WHERE a.anulado = 0 AND r.co_tipo = 'SA' AND CAST(a.fecha AS DATE) > ?
                GROUP BY r.co_art, r.co_alma
                UNION ALL
                SELECT r.co_art, r.co_alma, SUM(r.total_art) AS cant
                FROM saDevolucionProveedorReng r
                JOIN saDevolucionProveedor p ON r.doc_num = p.doc_num
                WHERE p.anulado = 0 AND CAST(p.fec_emis AS DATE) > ?
                GROUP BY r.co_art, r.co_alma
                UNION ALL
                SELECT r.co_art, t.alm_orig AS co_alma, SUM(r.total_art) AS cant
                FROM saTrasladoReng r
                JOIN saTraslado t ON r.tras_num = t.tras_num
                WHERE t.anulado = 0 AND CAST(t.fecha AS DATE) > ?
                GROUP BY r.co_art, t.alm_orig
            ),
            EntradasAgrupadas AS (
                SELECT co_art, co_alma, SUM(cant) AS total_ent
                FROM EntradasPost
                GROUP BY co_art, co_alma
            ),
            SalidasAgrupadas AS (
                SELECT co_art, co_alma, SUM(cant) AS total_sal
                FROM SalidasPost
                GROUP BY co_art, co_alma
            ),
            TodosArtAlma AS (
                SELECT co_art, co_alma FROM StockActual
                UNION
                SELECT co_art, co_alma FROM EntradasAgrupadas
                UNION
                SELECT co_art, co_alma FROM SalidasAgrupadas
            )
            SELECT 
                RTRIM(t.co_art) AS co_art,
                RTRIM(a.art_des) AS art_des,
                RTRIM(t.co_alma) AS co_alma,
                RTRIM(ISNULL(alm.des_alma, t.co_alma)) AS des_alma,
                RTRIM(ISNULL(l.lin_des, '')) AS linea,
                RTRIM(ISNULL(sl.subl_des, '')) AS sublinea,
                RTRIM(ISNULL(cat.cat_des, '')) AS categoria,
                RTRIM(ISNULL(ap.co_prov, '')) AS co_prov,
                RTRIM(ISNULL(p.prov_des, '')) AS nom_prov,
                CAST(a.rowguid AS VARCHAR(36)) AS art_rowguid,
                (ISNULL(stk.stock_actual, 0) - ISNULL(ent.total_ent, 0) + ISNULL(sal.total_sal, 0)) AS stock_al_corte
            FROM TodosArtAlma t
            JOIN saArticulo a ON t.co_art = a.co_art
            LEFT JOIN saAlmacen alm ON t.co_alma = alm.co_alma
            LEFT JOIN saLineaArticulo l ON a.co_lin = l.co_lin
            LEFT JOIN saSubLinea sl ON a.co_subl = sl.co_subl AND a.co_lin = sl.co_lin
            LEFT JOIN saCatArticulo cat ON a.co_cat = cat.co_cat
            LEFT JOIN (
                SELECT co_art, co_prov
                FROM saArtProveedorReng
                WHERE reng_num = 1
            ) ap ON a.co_art = ap.co_art
            LEFT JOIN saProveedor p ON ap.co_prov = p.co_prov
            LEFT JOIN StockActual stk ON t.co_art = stk.co_art AND t.co_alma = stk.co_alma
            LEFT JOIN EntradasAgrupadas ent ON t.co_art = ent.co_art AND t.co_alma = ent.co_alma
            LEFT JOIN SalidasAgrupadas sal ON t.co_art = sal.co_art AND t.co_alma = sal.co_alma
            WHERE (ISNULL(stk.stock_actual, 0) - ISNULL(ent.total_ent, 0) + ISNULL(sal.total_sal, 0)) > 0.0001
            """;

        if (filtroAlmacen != null && !filtroAlmacen.isBlank() && !"TODOS".equalsIgnoreCase(filtroAlmacen.trim())) {
            sqlStockCorte += " AND RTRIM(t.co_alma) = '" + filtroAlmacen.trim().replace("'", "") + "' ";
        }

        if (filtroBusqueda != null && !filtroBusqueda.isBlank()) {
            String busq = filtroBusqueda.trim().replace("'", "''");
            sqlStockCorte += " AND (a.co_art LIKE '%" + busq + "%' OR a.art_des LIKE '%" + busq + "%' OR p.prov_des LIKE '%" + busq + "%') ";
        }

        String targetDb = resolverBaseDatos(fechaCorte);
        logger.info("[InventarioValoracionDAO] Calculando inventario valorado para fecha [{}] en BD [{}]", fechaCorte, targetDb);

        try (Connection conn = DatabaseConfig.getConnection(targetDb)) {
            // Pre-cargar mapa de compras anteriores al corte para Venta Firme
            Map<String, List<CapaCompraRaw>> comprasPorArticulo = cargarComprasPrevias(conn, sqlFechaCorte);

            // Pre-cargar mapa de último costo de compras para Consignación (saCostoHistoricoEntrada)
            Map<String, Double> ultimosCostosHistoricos = cargarCostosHistoricosConsignacion(conn, sqlFechaCorte);

            try (PreparedStatement ps = conn.prepareStatement(sqlStockCorte)) {
                for (int i = 1; i <= 9; i++) {
                    ps.setDate(i, sqlFechaCorte);
                }

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String coArt = rs.getString("co_art");
                        String artDes = rs.getString("art_des");
                        String coAlma = rs.getString("co_alma");
                        String desAlma = rs.getString("des_alma");
                        String linea = rs.getString("linea");
                        String sublinea = rs.getString("sublinea");
                        String categoria = rs.getString("categoria");
                        String coProv = rs.getString("co_prov");
                        String nomProv = rs.getString("nom_prov");
                        String artRowguid = rs.getString("art_rowguid");
                        double existencia = rs.getDouble("stock_al_corte");

                        boolean esConsignacion = ALMACENES_CONSIGNACION.contains(coAlma.toUpperCase());
                        String tipoMercancia = esConsignacion ? "CONSIGNACION" : "VENTA FIRME";

                        // Filtrado por tipo si aplica
                        if ("VENTA_FIRME".equalsIgnoreCase(filtroTipo) && esConsignacion) {
                            continue;
                        }
                        if ("CONSIGNACION".equalsIgnoreCase(filtroTipo) && !esConsignacion) {
                            continue;
                        }

                        InventarioValoradoRow row = new InventarioValoradoRow();
                        row.setCoArt(coArt);
                        row.setArtDes(artDes);
                        row.setCoAlma(coAlma);
                        row.setDesAlma(desAlma);
                        row.setLinea(linea);
                        row.setSublinea(sublinea);
                        row.setCategoria(categoria);
                        row.setCoProv(coProv);
                        row.setNomProv(nomProv);
                        row.setTipoMercancia(tipoMercancia);
                        row.setExistencia(existencia);

                        double costoProvUsd = ultimosCostosHistoricos.getOrDefault(coArt.toUpperCase(),
                                              ultimosCostosHistoricos.getOrDefault(artRowguid != null ? artRowguid.toUpperCase() : "", 0.0));
                        row.setCostoProveedorUSD(costoProvUsd);
                        row.setCostoProveedorBS(costoProvUsd * tasaCorte);

                        if (esConsignacion) {
                            // ── CASO CONSIGNACIÓN ──
                            // Se valúa con el Costo Proveedor en USD reportado en saCostoHistoricoEntrada
                            double costoUsd = costoProvUsd;
                            if (costoUsd <= 0.0) {
                                // Fallback a compras NREC si no estaba en histórico
                                List<CapaCompraRaw> compras = comprasPorArticulo.get(coArt);
                                if (compras != null && !compras.isEmpty()) {
                                    costoUsd = compras.get(0).costUnitUSD;
                                }
                            }

                            double costoBs = costoUsd * tasaCorte;
                            row.setCostoPromedioUSD(costoUsd);
                            row.setCostoPromedioBS(costoBs);
                            row.setUltimoCostoUSD(costoUsd);
                            row.setUltimoCostoBS(costoBs);
                            row.setTotalValorizadoUSD(existencia * costoUsd);
                            row.setTotalValorizadoBS(existencia * costoBs);
                        } else {
                            // ── CASO VENTA FIRME (CAPAS FIFO) ──
                            List<CapaCompraRaw> compras = comprasPorArticulo.get(coArt);
                            List<CapaCostoDTO> capasUtilizadas = new ArrayList<>();

                            double cantFaltante = existencia;
                            double sumaPonderadaUSD = 0.0;
                            double sumaPonderadaBS = 0.0;
                            double ultimoCostoUSD = 0.0;
                            double ultimoCostoBS = 0.0;

                            if (compras != null && !compras.isEmpty()) {
                                ultimoCostoUSD = compras.get(0).costUnitUSD;
                                ultimoCostoBS = compras.get(0).costUnitBS;

                                for (CapaCompraRaw c : compras) {
                                    if (cantFaltante <= 0.0001) break;

                                    double cantTomar = Math.min(cantFaltante, c.totalArt);
                                    if (cantTomar > 0) {
                                        capasUtilizadas.add(new CapaCostoDTO(
                                                c.tipoDoc, c.docNum, c.fecha, c.totalArt,
                                                cantTomar, c.costUnitUSD, c.costUnitBS,
                                                c.tasa, c.coProv, c.nomProv
                                        ));

                                        sumaPonderadaUSD += (cantTomar * c.costUnitUSD);
                                        sumaPonderadaBS += (cantTomar * c.costUnitBS);
                                        cantFaltante -= cantTomar;
                                    }
                                }
                            }

                            // Si las compras no cubrieron toda la existencia, se usa el Costo Proveedor como capa base
                            if (cantFaltante > 0.0001) {
                                double costoFallbackUsd = (costoProvUsd > 0) ? costoProvUsd
                                        : ((ultimoCostoUSD > 0) ? ultimoCostoUSD : 0.0);
                                double costoFallbackBs = (costoFallbackUsd * tasaCorte);

                                capasUtilizadas.add(new CapaCostoDTO(
                                        "COSTO-PROVEEDOR", "-", fechaCorte.toString(), cantFaltante,
                                        cantFaltante, costoFallbackUsd, costoFallbackBs,
                                        tasaCorte, coProv, nomProv
                                ));

                                sumaPonderadaUSD += (cantFaltante * costoFallbackUsd);
                                sumaPonderadaBS += (cantFaltante * costoFallbackBs);
                                if (ultimoCostoUSD <= 0) ultimoCostoUSD = costoFallbackUsd;
                                if (ultimoCostoBS <= 0) ultimoCostoBS = costoFallbackBs;
                            }

                            double costoPromUSD = sumaPonderadaUSD / existencia;
                            double costoPromBS = costoPromUSD * tasaCorte; // Valuado a la tasa oficial del corte

                            row.setCostoPromedioUSD(costoPromUSD);
                            row.setCostoPromedioBS(costoPromBS);
                            row.setUltimoCostoUSD(ultimoCostoUSD);
                            row.setUltimoCostoBS(ultimoCostoUSD * tasaCorte);
                            row.setTotalValorizadoUSD(existencia * costoPromUSD);
                            row.setTotalValorizadoBS(existencia * costoPromBS);
                            row.setCapas(capasUtilizadas);
                        }

                        resultado.add(row);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("[InventarioValoracionDAO] Error al calcular inventario valorado: {}", e.getMessage(), e);
        }

        return resultado;
    }

    /**
     * Consulta el Kárdex de movimiento de un artículo específico en un rango de fechas.
     * Si el rango comprende fechas <= 27/07/2026, consulta DROA2_A.
     * Si comprende fechas > 27/07/2026, consulta DROA_A.
     * Si cruza la fecha de corte, combina de forma secuencial y continua ambas bases de datos.
     */
    public List<KardexMovimientoValoradoRow> consultarKardexProducto(String coArt,
                                                                    LocalDate desde,
                                                                    LocalDate hasta,
                                                                    String coAlma) {
        List<KardexMovimientoValoradoRow> movimientos = new ArrayList<>();
        if (coArt == null || coArt.isBlank() || desde == null || hasta == null) {
            return movimientos;
        }

        double tasaCorte = obtenerTasaUSD(hasta);
        double saldoInicial = obtenerSaldoInicial(coArt, desde, coAlma);

        // Fila 0: Saldo inicial
        movimientos.add(new KardexMovimientoValoradoRow(
                desde.minusDays(1).toString(),
                "SALDO-INICIAL",
                "-",
                (coAlma != null && !coAlma.isBlank() && !"TODOS".equalsIgnoreCase(coAlma)) ? coAlma : "TODOS",
                "Existencia inicial previa al periodo",
                "-",
                saldoInicial >= 0 ? saldoInicial : 0,
                0,
                saldoInicial,
                0, 0, 0, 0
        ));

        double runningSaldo = saldoInicial;
        boolean d1Hist = !desde.isAfter(FECHA_CORTE_MIGRACION);
        boolean d2Hist = !hasta.isAfter(FECHA_CORTE_MIGRACION);

        if (d1Hist && d2Hist) {
            consultarMovimientosEnBd(DB_HISTORICA, coArt, desde, hasta, coAlma, tasaCorte, runningSaldo, movimientos);
        } else if (!d1Hist && !d2Hist) {
            consultarMovimientosEnBd(DB_ACTUAL, coArt, desde, hasta, coAlma, tasaCorte, runningSaldo, movimientos);
        } else {
            // Rango transicional que atraviesa la migración
            runningSaldo = consultarMovimientosEnBd(DB_HISTORICA, coArt, desde, FECHA_CORTE_MIGRACION, coAlma, tasaCorte, runningSaldo, movimientos);
            consultarMovimientosEnBd(DB_ACTUAL, coArt, FECHA_CORTE_MIGRACION.plusDays(1), hasta, coAlma, tasaCorte, runningSaldo, movimientos);
        }

        return movimientos;
    }

    private double consultarMovimientosEnBd(String targetDb, String coArt, LocalDate dInicio, LocalDate dFin,
                                            String coAlma, double tasaCorte, double saldoBase,
                                            List<KardexMovimientoValoradoRow> salidaMovs) {
        if (dInicio.isAfter(dFin)) return saldoBase;

        String sqlMovs = """
            SELECT tipo_doc, RTRIM(doc_num) AS doc_num, CONVERT(VARCHAR(10), fecha, 120) AS fecha,
                   RTRIM(co_alma) AS co_alma, RTRIM(ISNULL(origen, '')) AS origen,
                   RTRIM(ISNULL(numero_lote, '')) AS numero_lote,
                   entrada, salida, cost_unit, cost_unit_om, tasa
            FROM (
                -- Compras NREC (Entradas)
                SELECT 'NREC' AS tipo_doc, r.doc_num, c.fec_emis AS fecha, r.co_alma,
                       ISNULL(p.prov_des, c.co_prov) AS origen,
                       ISNULL(r.lote_asignado, 0) AS lote_asignado,
                       ISNULL(le.numero_lote, '') AS numero_lote,
                       r.total_art AS entrada, 0 AS salida,
                       r.cost_unit, r.cost_unit_om, c.tasa
                FROM saNotaRecepcionCompraReng r
                JOIN saNotaRecepcionCompra c ON r.doc_num = c.doc_num
                LEFT JOIN saProveedor p ON c.co_prov = p.co_prov
                LEFT JOIN saLoteEntrada le ON le.rowguid_reng = r.rowguid
                WHERE RTRIM(r.co_art) = ? AND c.anulado = 0
                  AND CAST(c.fec_emis AS DATE) BETWEEN ? AND ?

                UNION ALL
                -- Ajustes EA (Entradas)
                SELECT 'AJUS-EA' AS tipo_doc, r.ajue_num, a.fecha, r.co_alma,
                       ISNULL(a.motivo, 'Ajuste de Entrada') AS origen,
                       r.lote_asignado, ISNULL(le.numero_lote, '') AS numero_lote,
                       r.total_art AS entrada, 0 AS salida,
                       r.cost_unit, 0 AS cost_unit_om, a.tasa
                FROM saAjusteReng r
                JOIN saAjuste a ON r.ajue_num = a.ajue_num
                LEFT JOIN saLoteEntrada le ON le.rowguid_reng = r.rowguid
                WHERE RTRIM(r.co_art) = ? AND a.anulado = 0 AND r.co_tipo = 'EA'
                  AND CAST(a.fecha AS DATE) BETWEEN ? AND ?

                UNION ALL
                -- Devoluciones Cliente (Entradas)
                SELECT 'DCLI' AS tipo_doc, r.doc_num, d.fec_emis AS fecha, r.co_alma,
                       ISNULL(cli.cli_des, d.co_cli) AS origen,
                       r.lote_asignado, ISNULL(le.numero_lote, '') AS numero_lote,
                       r.total_art AS entrada, 0 AS salida,
                       0 AS cost_unit, 0 AS cost_unit_om, d.tasa
                FROM saDevolucionClienteReng r
                JOIN saDevolucionCliente d ON r.doc_num = d.doc_num
                LEFT JOIN saCliente cli ON d.co_cli = cli.co_cli
                LEFT JOIN saLoteEntrada le ON le.rowguid_reng = r.rowguid
                WHERE RTRIM(r.co_art) = ? AND d.anulado = 0
                  AND CAST(d.fec_emis AS DATE) BETWEEN ? AND ?

                UNION ALL
                -- Notas de Entrega Venta (Salidas)
                SELECT 'NENT' AS tipo_doc, r.doc_num, n.fec_emis AS fecha, r.co_alma,
                       ISNULL(cli.cli_des, n.co_cli) AS origen,
                       r.lote_asignado, '' AS numero_lote,
                       0 AS entrada, r.total_art AS salida,
                       0 AS cost_unit, 0 AS cost_unit_om, n.tasa
                FROM saNotaEntregaVentaReng r
                JOIN saNotaEntregaVenta n ON r.doc_num = n.doc_num
                LEFT JOIN saCliente cli ON n.co_cli = cli.co_cli
                WHERE RTRIM(r.co_art) = ? AND n.anulado = 0
                  AND CAST(n.fec_emis AS DATE) BETWEEN ? AND ?

                UNION ALL
                -- Facturas Venta Directas (Salidas)
                SELECT 'FACT' AS tipo_doc, r.doc_num, f.fec_emis AS fecha, r.co_alma,
                       ISNULL(cli.cli_des, f.co_cli) AS origen,
                       r.lote_asignado, '' AS numero_lote,
                       0 AS entrada, r.total_art AS salida,
                       0 AS cost_unit, 0 AS cost_unit_om, f.tasa
                FROM saFacturaVentaReng r
                JOIN saFacturaVenta f ON r.doc_num = f.doc_num
                LEFT JOIN saCliente cli ON f.co_cli = cli.co_cli
                WHERE RTRIM(r.co_art) = ? AND f.anulado = 0 AND (r.num_doc IS NULL OR RTRIM(r.num_doc) = '')
                  AND CAST(f.fec_emis AS DATE) BETWEEN ? AND ?

                UNION ALL
                -- Ajustes SA (Salidas)
                SELECT 'AJUS-SA' AS tipo_doc, r.ajue_num, a.fecha, r.co_alma,
                       ISNULL(a.motivo, 'Ajuste de Salida') AS origen,
                       r.lote_asignado, '' AS numero_lote,
                       0 AS entrada, r.total_art AS salida,
                       r.cost_unit, 0 AS cost_unit_om, a.tasa
                FROM saAjusteReng r
                JOIN saAjuste a ON r.ajue_num = a.ajue_num
                WHERE RTRIM(r.co_art) = ? AND a.anulado = 0 AND r.co_tipo = 'SA'
                  AND CAST(a.fecha AS DATE) BETWEEN ? AND ?

                UNION ALL
                -- Devoluciones Proveedor (Salidas)
                SELECT 'DPRO' AS tipo_doc, r.doc_num, p.fec_emis AS fecha, r.co_alma,
                       ISNULL(prov.prov_des, p.co_prov) AS origen,
                       r.lote_asignado, '' AS numero_lote,
                       0 AS entrada, r.total_art AS salida,
                       0 AS cost_unit, 0 AS cost_unit_om, p.tasa
                FROM saDevolucionProveedorReng r
                JOIN saDevolucionProveedor p ON r.doc_num = p.doc_num
                LEFT JOIN saProveedor prov ON p.co_prov = prov.co_prov
                WHERE RTRIM(r.co_art) = ? AND p.anulado = 0
                  AND CAST(p.fec_emis AS DATE) BETWEEN ? AND ?
            ) t
            """;

        if (coAlma != null && !coAlma.isBlank() && !"TODOS".equalsIgnoreCase(coAlma.trim())) {
            sqlMovs += " WHERE RTRIM(co_alma) = '" + coAlma.trim().replace("'", "") + "' ";
        }

        sqlMovs += " ORDER BY fecha ASC, doc_num ASC ";

        double running = saldoBase;

        try (Connection conn = DatabaseConfig.getConnection(targetDb);
             PreparedStatement ps = conn.prepareStatement(sqlMovs)) {

            double costoProveedorUsd = obtenerCostoProveedor(conn, coArt, dFin);

            String art = coArt.trim();
            java.sql.Date d1 = java.sql.Date.valueOf(dInicio);
            java.sql.Date d2 = java.sql.Date.valueOf(dFin);

            int pIdx = 1;
            for (int i = 0; i < 7; i++) {
                ps.setString(pIdx++, art);
                ps.setDate(pIdx++, d1);
                ps.setDate(pIdx++, d2);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String tipoDoc = rs.getString("tipo_doc");
                    String docNum = rs.getString("doc_num");
                    String fecha = rs.getString("fecha");
                    String alm = rs.getString("co_alma");
                    String origen = rs.getString("origen");
                    String lote = rs.getString("numero_lote");
                    double entrada = rs.getDouble("entrada");
                    double salida = rs.getDouble("salida");
                    double costUnitBs = rs.getDouble("cost_unit");
                    double costUnitUsd = rs.getDouble("cost_unit_om");
                    double docTasa = rs.getDouble("tasa");
                    if (docTasa <= 0) docTasa = tasaCorte;

                    if (costUnitUsd <= 0 && costUnitBs > 0 && docTasa > 0) {
                        costUnitUsd = costUnitBs / docTasa;
                    } else if (costUnitBs <= 0 && costUnitUsd > 0 && docTasa > 0) {
                        costUnitBs = costUnitUsd * docTasa;
                    } else if (costUnitUsd <= 0 && costUnitBs <= 0 && costoProveedorUsd > 0) {
                        // Si el movimiento no registra costo unitario propio, toma el Costo Proveedor
                        costUnitUsd = costoProveedorUsd;
                        costUnitBs = costUnitUsd * docTasa;
                    }

                    running = running + entrada - salida;

                    double totalUsd = (entrada > 0 ? entrada : salida) * costUnitUsd;
                    double totalBs = (entrada > 0 ? entrada : salida) * costUnitBs;

                    salidaMovs.add(new KardexMovimientoValoradoRow(
                            fecha, tipoDoc, docNum, alm, origen, lote,
                            entrada, salida, running, costUnitUsd, costUnitBs, totalUsd, totalBs
                    ));
                }
            }
        } catch (SQLException e) {
            logger.error("[InventarioValoracionDAO] Error al consultar Kárdex de {} en BD [{}]: {}", coArt, targetDb, e.getMessage(), e);
        }

        return running;
    }

    /**
     * Reconstruye el saldo inicial acumulado a la fecha anterior al periodo de consulta.
     */
    private double obtenerSaldoInicial(String coArt, LocalDate desde, String coAlma) {
        LocalDate fechaCortePrevia = desde.minusDays(1);
        String targetDb = resolverBaseDatos(fechaCortePrevia);
        return obtenerSaldoInicialEnBd(targetDb, coArt, fechaCortePrevia, coAlma);
    }

    private double obtenerSaldoInicialEnBd(String targetDb, String coArt, LocalDate fechaCortePrevia, String coAlma) {
        String sql = """
            WITH StockActual AS (
                SELECT co_art, co_alma, SUM(stock) AS stock_actual
                FROM saStockAlmacen
                WHERE tipo = 'ACT' AND RTRIM(co_art) = ?
                GROUP BY co_art, co_alma
            ),
            EntradasPost AS (
                SELECT r.co_art, r.co_alma, SUM(r.total_art) AS cant
                FROM saNotaRecepcionCompraReng r
                JOIN saNotaRecepcionCompra c ON r.doc_num = c.doc_num
                WHERE RTRIM(r.co_art) = ? AND c.anulado = 0 AND CAST(c.fec_emis AS DATE) > ?
                GROUP BY r.co_art, r.co_alma
                UNION ALL
                SELECT r.co_art, r.co_alma, SUM(r.total_art) AS cant
                FROM saAjusteReng r
                JOIN saAjuste a ON r.ajue_num = a.ajue_num
                WHERE RTRIM(r.co_art) = ? AND a.anulado = 0 AND r.co_tipo = 'EA' AND CAST(a.fecha AS DATE) > ?
                GROUP BY r.co_art, r.co_alma
                UNION ALL
                SELECT r.co_art, r.co_alma, SUM(r.total_art) AS cant
                FROM saDevolucionClienteReng r
                JOIN saDevolucionCliente d ON r.doc_num = d.doc_num
                WHERE RTRIM(r.co_art) = ? AND d.anulado = 0 AND CAST(d.fec_emis AS DATE) > ?
                GROUP BY r.co_art, r.co_alma
            ),
            SalidasPost AS (
                SELECT r.co_art, r.co_alma, SUM(r.total_art) AS cant
                FROM saNotaEntregaVentaReng r
                JOIN saNotaEntregaVenta n ON r.doc_num = n.doc_num
                WHERE RTRIM(r.co_art) = ? AND n.anulado = 0 AND CAST(n.fec_emis AS DATE) > ?
                GROUP BY r.co_art, r.co_alma
                UNION ALL
                SELECT r.co_art, r.co_alma, SUM(r.total_art) AS cant
                FROM saFacturaVentaReng r
                JOIN saFacturaVenta f ON r.doc_num = f.doc_num
                WHERE RTRIM(r.co_art) = ? AND f.anulado = 0 AND (r.num_doc IS NULL OR RTRIM(r.num_doc) = '') AND CAST(f.fec_emis AS DATE) > ?
                GROUP BY r.co_art, r.co_alma
                UNION ALL
                SELECT r.co_art, r.co_alma, SUM(r.total_art) AS cant
                FROM saAjusteReng r
                JOIN saAjuste a ON r.ajue_num = a.ajue_num
                WHERE RTRIM(r.co_art) = ? AND a.anulado = 0 AND r.co_tipo = 'SA' AND CAST(a.fecha AS DATE) > ?
                GROUP BY r.co_art, r.co_alma
                UNION ALL
                SELECT r.co_art, r.co_alma, SUM(r.total_art) AS cant
                FROM saDevolucionProveedorReng r
                JOIN saDevolucionProveedor p ON r.doc_num = p.doc_num
                WHERE RTRIM(r.co_art) = ? AND p.anulado = 0 AND CAST(p.fec_emis AS DATE) > ?
                GROUP BY r.co_art, r.co_alma
            )
            SELECT (ISNULL((SELECT SUM(stock_actual) FROM StockActual), 0)
                   - ISNULL((SELECT SUM(cant) FROM EntradasPost), 0)
                   + ISNULL((SELECT SUM(cant) FROM SalidasPost), 0)) AS saldo_inicial
            """;

        try (Connection conn = DatabaseConfig.getConnection(targetDb);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            java.sql.Date dCorte = java.sql.Date.valueOf(fechaCortePrevia);
            String art = coArt.trim();

            ps.setString(1, art);
            ps.setString(2, art); ps.setDate(3, dCorte);
            ps.setString(4, art); ps.setDate(5, dCorte);
            ps.setString(6, art); ps.setDate(7, dCorte);
            ps.setString(8, art); ps.setDate(9, dCorte);
            ps.setString(10, art); ps.setDate(11, dCorte);
            ps.setString(12, art); ps.setDate(13, dCorte);
            ps.setString(14, art); ps.setDate(15, dCorte);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("saldo_inicial");
                }
            }
        } catch (SQLException e) {
            logger.error("[InventarioValoracionDAO] Error al obtener saldo inicial de {} en BD [{}]: {}", coArt, targetDb, e.getMessage(), e);
        }
        return 0.0;
    }

    /**
     * Carga en memoria todas las compras previas a la fecha de corte para armar las capas FIFO.
     */
    private Map<String, List<CapaCompraRaw>> cargarComprasPrevias(Connection conn, java.sql.Date sqlFechaCorte) throws SQLException {
        Map<String, List<CapaCompraRaw>> map = new HashMap<>();

        String sql = """
            SELECT RTRIM(r.co_art) AS co_art,
                   'NREC' AS tipo_doc,
                   RTRIM(r.doc_num) AS doc_num,
                   CONVERT(VARCHAR(10), c.fec_emis, 120) AS fecha,
                   r.total_art,
                   r.cost_unit,
                   r.cost_unit_om,
                   ISNULL(c.tasa, 1) AS tasa,
                   RTRIM(ISNULL(c.co_prov, '')) AS co_prov,
                   RTRIM(ISNULL(p.prov_des, '')) AS nom_prov
            FROM saNotaRecepcionCompraReng r
            JOIN saNotaRecepcionCompra c ON r.doc_num = c.doc_num
            LEFT JOIN saProveedor p ON c.co_prov = p.co_prov
            WHERE c.anulado = 0
              AND CAST(c.fec_emis AS DATE) <= ?
            ORDER BY c.fec_emis DESC, r.doc_num DESC
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, sqlFechaCorte);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String coArt = rs.getString("co_art");
                    double costBs = rs.getDouble("cost_unit");
                    double costUsd = rs.getDouble("cost_unit_om");
                    double tasa = rs.getDouble("tasa");

                    if (costUsd <= 0 && costBs > 0 && tasa > 0) {
                        costUsd = costBs / tasa;
                    } else if (costBs <= 0 && costUsd > 0 && tasa > 0) {
                        costBs = costUsd * tasa;
                    }

                    CapaCompraRaw capa = new CapaCompraRaw(
                            rs.getString("tipo_doc"),
                            rs.getString("doc_num"),
                            rs.getString("fecha"),
                            rs.getDouble("total_art"),
                            costUsd,
                            costBs,
                            tasa,
                            rs.getString("co_prov"),
                            rs.getString("nom_prov")
                    );

                    map.computeIfAbsent(coArt, k -> new ArrayList<>()).add(capa);
                }
            }
        }
        return map;
    }

    /**
     * Carga los últimos costos vigentes en USD de saCostoHistoricoEntrada para los productos (Costo Proveedor).
     * Mapea tanto por co_art como por art_rowguid.
     */
    private Map<String, Double> cargarCostosHistoricosConsignacion(Connection conn, java.sql.Date sqlFechaCorte) throws SQLException {
        Map<String, Double> map = new HashMap<>();

        String sql = """
            SELECT RTRIM(a.co_art) AS co_art,
                   CAST(ranked.cod_articulo_rowguid AS VARCHAR(36)) AS art_rowguid,
                   ranked.costo
            FROM (
                SELECT cod_articulo_rowguid, costo,
                       ROW_NUMBER() OVER (
                           PARTITION BY cod_articulo_rowguid
                           ORDER BY CASE WHEN tipo_doc = 'PROV' THEN 1 ELSE 2 END, fecha_emision DESC
                       ) AS rn
                FROM saCostoHistoricoEntrada
                WHERE costo > 0
                  AND CAST(fecha_emision AS DATE) <= ?
            ) ranked
            JOIN saArticulo a ON a.rowguid = ranked.cod_articulo_rowguid
            WHERE ranked.rn = 1
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, sqlFechaCorte);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String coArt = rs.getString("co_art");
                    String artRowguid = rs.getString("art_rowguid");
                    double costo = rs.getDouble("costo");
                    if (coArt != null && !coArt.isBlank()) {
                        map.put(coArt.trim().toUpperCase(), costo);
                    }
                    if (artRowguid != null && !artRowguid.isBlank()) {
                        map.put(artRowguid.trim().toUpperCase(), costo);
                    }
                }
            }
        }
        return map;
    }

    /**
     * Obtiene el Costo Proveedor más reciente en USD para un artículo a una fecha determinada.
     */
    public double obtenerCostoProveedor(Connection conn, String coArt, LocalDate fechaCorte) {
        if (coArt == null || coArt.isBlank()) return 0.0;
        String sql = """
            SELECT TOP 1 ce.costo
            FROM saArticulo a
            JOIN saCostoHistoricoEntrada ce ON a.rowguid = ce.cod_articulo_rowguid
            WHERE RTRIM(a.co_art) = ? AND ce.costo > 0
              AND CAST(ce.fecha_emision AS DATE) <= ?
            ORDER BY CASE WHEN ce.tipo_doc = 'PROV' THEN 1 ELSE 2 END, ce.fecha_emision DESC
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, coArt.trim());
            ps.setDate(2, java.sql.Date.valueOf(fechaCorte));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("costo");
                }
            }
        } catch (SQLException e) {
            logger.warn("[InventarioValoracionDAO] No se pudo obtener costo proveedor de {}: {}", coArt, e.getMessage());
        }
        return 0.0;
    }

    /**
     * Estructura interna para almacenar temporalmente una recepción de compra.
     */
    private static class CapaCompraRaw {
        final String tipoDoc;
        final String docNum;
        final String fecha;
        final double totalArt;
        final double costUnitUSD;
        final double costUnitBS;
        final double tasa;
        final String coProv;
        final String nomProv;

        CapaCompraRaw(String tipoDoc, String docNum, String fecha, double totalArt,
                      double costUnitUSD, double costUnitBS, double tasa,
                      String coProv, String nomProv) {
            this.tipoDoc = tipoDoc;
            this.docNum = docNum;
            this.fecha = fecha;
            this.totalArt = totalArt;
            this.costUnitUSD = costUnitUSD;
            this.costUnitBS = costUnitBS;
            this.tasa = tasa;
            this.coProv = coProv;
            this.nomProv = nomProv;
        }
    }
}

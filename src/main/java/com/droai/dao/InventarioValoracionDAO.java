package com.droai.dao;

import com.droai.config.DatabaseConfig;
import com.droai.model.CapaCostoDTO;
import com.droai.model.InventarioValoradoRow;
import com.droai.model.KardexMovimientoValoradoRow;
import com.droai.model.CostoVentaRow;
import com.droai.model.CostoVentasPeriodoResult;
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
                RTRIM(ISNULL(a.campo8, '')) AS campo8,
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
            Map<String, List<CapaCompraRaw>> comprasPorArticulo = cargarComprasPrevias(conn, targetDb, fechaCorte);

            // Pre-cargar mapa de último costo de compras para Consignación (saCostoHistoricoEntrada)
            Map<String, Double> ultimosCostosHistoricos = cargarCostosHistoricosConsignacion(conn, targetDb, fechaCorte);

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
                        if (costoProvUsd <= 0.0) {
                            String c8 = rs.getString("campo8");
                            if (c8 != null && !c8.isBlank()) {
                                try {
                                    costoProvUsd = Double.parseDouble(c8.trim().replace(",", "."));
                                } catch (Exception ignored) {}
                            }
                        }
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
                            double totUsd = existencia * costoUsd;
                            double totBs = existencia * costoBs;
                            row.setTotalValorizadoUSD(totUsd);
                            row.setTotalValorizadoBS(totBs);
                            row.setTotalReposicionBS(totBs);
                            row.setDiferencialCambiarioBS(0.0);
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

                            double costoPromUSD = existencia > 0 ? (sumaPonderadaUSD / existencia) : 0.0;
                            double costoPromBS = existencia > 0 ? (sumaPonderadaBS / existencia) : 0.0;

                            row.setCostoPromedioUSD(costoPromUSD);
                            row.setCostoPromedioBS(costoPromBS);
                            row.setUltimoCostoUSD(ultimoCostoUSD);
                            row.setUltimoCostoBS(ultimoCostoBS);
                            row.setTotalValorizadoUSD(sumaPonderadaUSD);
                            row.setTotalValorizadoBS(sumaPonderadaBS);
                            double reposicionBs = sumaPonderadaUSD * tasaCorte;
                            row.setTotalReposicionBS(reposicionBs);
                            row.setDiferencialCambiarioBS(reposicionBs - sumaPonderadaBS);
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

    public static class KardexConsultaResult {
        private List<KardexMovimientoValoradoRow> movimientos = new ArrayList<>();
        private String coArt = "";
        private String artDes = "";
        private double existenciaFecha = 0.0;
        private double costoPromedioUSD = 0.0;
        private double costoPromedioBS = 0.0;
        private double totalValorizadoUSD = 0.0;
        private double totalValorizadoBS = 0.0;
        private double totalCompradoPeriodo = 0.0;
        private double tasaCorte = 1.0;

        public KardexConsultaResult() {}

        public List<KardexMovimientoValoradoRow> getMovimientos() { return movimientos; }
        public void setMovimientos(List<KardexMovimientoValoradoRow> movimientos) { this.movimientos = movimientos; }

        public String getCoArt() { return coArt; }
        public void setCoArt(String coArt) { this.coArt = coArt; }

        public String getArtDes() { return artDes; }
        public void setArtDes(String artDes) { this.artDes = artDes; }

        public double getExistenciaFecha() { return existenciaFecha; }
        public void setExistenciaFecha(double existenciaFecha) { this.existenciaFecha = existenciaFecha; }

        public double getCostoPromedioUSD() { return costoPromedioUSD; }
        public void setCostoPromedioUSD(double costoPromedioUSD) { this.costoPromedioUSD = costoPromedioUSD; }

        public double getCostoPromedioBS() { return costoPromedioBS; }
        public void setCostoPromedioBS(double costoPromedioBS) { this.costoPromedioBS = costoPromedioBS; }

        public double getTotalValorizadoUSD() { return totalValorizadoUSD; }
        public void setTotalValorizadoUSD(double totalValorizadoUSD) { this.totalValorizadoUSD = totalValorizadoUSD; }

        public double getTotalValorizadoBS() { return totalValorizadoBS; }
        public void setTotalValorizadoBS(double totalValorizadoBS) { this.totalValorizadoBS = totalValorizadoBS; }

        public double getTotalCompradoPeriodo() { return totalCompradoPeriodo; }
        public void setTotalCompradoPeriodo(double totalCompradoPeriodo) { this.totalCompradoPeriodo = totalCompradoPeriodo; }

        public double getTasaCorte() { return tasaCorte; }
        public void setTasaCorte(double tasaCorte) { this.tasaCorte = tasaCorte; }
    }

    /**
     * Consulta el Kárdex de compras (entradas NREC) de un artículo específico en un rango de fechas
     * y calcula su existencia y valuación al corte (costo promedio y costo total en Bs y USD).
     */
    public KardexConsultaResult consultarKardexProducto(String coArt,
                                                        LocalDate desde,
                                                        LocalDate hasta,
                                                        String coAlma) {
        KardexConsultaResult result = new KardexConsultaResult();
        if (coArt == null || coArt.isBlank() || desde == null || hasta == null) {
            return result;
        }
        result.setCoArt(coArt.trim());

        double tasaCorte = obtenerTasaUSD(hasta);
        result.setTasaCorte(tasaCorte);

        List<KardexMovimientoValoradoRow> compras = new ArrayList<>();
        boolean d1Hist = !desde.isAfter(FECHA_CORTE_MIGRACION);
        boolean d2Hist = !hasta.isAfter(FECHA_CORTE_MIGRACION);

        double runningTotal = 0.0;
        if (d1Hist && d2Hist) {
            runningTotal = consultarComprasEnBd(DB_HISTORICA, coArt, desde, hasta, coAlma, tasaCorte, runningTotal, compras);
        } else if (!d1Hist && !d2Hist) {
            runningTotal = consultarComprasEnBd(DB_ACTUAL, coArt, desde, hasta, coAlma, tasaCorte, runningTotal, compras);
        } else {
            runningTotal = consultarComprasEnBd(DB_HISTORICA, coArt, desde, FECHA_CORTE_MIGRACION, coAlma, tasaCorte, runningTotal, compras);
            runningTotal = consultarComprasEnBd(DB_ACTUAL, coArt, FECHA_CORTE_MIGRACION.plusDays(1), hasta, coAlma, tasaCorte, runningTotal, compras);
        }

        result.setMovimientos(compras);
        result.setTotalCompradoPeriodo(runningTotal);

        // Calcular existencia a la fecha 'hasta' y la valuación exacta (Costo promedio y costo total en Bs y $)
        String targetDb = resolverBaseDatos(hasta);
        calcularValuacionArticuloAlCorte(targetDb, coArt.trim(), hasta, coAlma, tasaCorte, result);

        return result;
    }

    private double consultarComprasEnBd(String targetDb, String coArt, LocalDate dInicio, LocalDate dFin,
                                        String coAlma, double tasaCorte, double runningBase,
                                        List<KardexMovimientoValoradoRow> salidaMovs) {
        if (dInicio.isAfter(dFin)) return runningBase;

        String sql = """
            SELECT 'NREC' AS tipo_doc,
                   RTRIM(r.doc_num) AS doc_num,
                   CONVERT(VARCHAR(10), c.fec_emis, 120) AS fecha,
                   RTRIM(r.co_alma) AS co_alma,
                   RTRIM(ISNULL(p.prov_des, c.co_prov)) AS origen,
                   RTRIM(ISNULL(le.numero_lote, '')) AS numero_lote,
                   r.total_art AS entrada,
                   r.cost_unit,
                   r.cost_unit_om,
                   ISNULL(c.tasa, 1) AS tasa
            FROM saNotaRecepcionCompraReng r
            JOIN saNotaRecepcionCompra c ON r.doc_num = c.doc_num
            LEFT JOIN saProveedor p ON c.co_prov = p.co_prov
            LEFT JOIN saLoteEntrada le ON le.rowguid_reng = r.rowguid
            WHERE RTRIM(r.co_art) = ? AND c.anulado = 0
              AND CAST(c.fec_emis AS DATE) BETWEEN ? AND ?
            """;

        if (coAlma != null && !coAlma.isBlank() && !"TODOS".equalsIgnoreCase(coAlma.trim())) {
            sql += " AND RTRIM(r.co_alma) = '" + coAlma.trim().replace("'", "") + "' ";
        }
        sql += " ORDER BY c.fec_emis ASC, r.doc_num ASC ";

        double running = runningBase;

        try (Connection conn = DatabaseConfig.getConnection(targetDb);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, coArt.trim());
            ps.setDate(2, java.sql.Date.valueOf(dInicio));
            ps.setDate(3, java.sql.Date.valueOf(dFin));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String tipoDoc = rs.getString("tipo_doc");
                    String docNum = rs.getString("doc_num");
                    String fecha = rs.getString("fecha");
                    String alm = rs.getString("co_alma");
                    String origen = rs.getString("origen");
                    String lote = rs.getString("numero_lote");
                    double entrada = rs.getDouble("entrada");
                    double salida = 0.0;
                    double costUnitBs = rs.getDouble("cost_unit");
                    double costUnitUsd = rs.getDouble("cost_unit_om");
                    double docTasa = rs.getDouble("tasa");
                    if (docTasa <= 0) docTasa = tasaCorte;

                    if (costUnitUsd <= 0 && costUnitBs > 0 && docTasa > 0) {
                        costUnitUsd = costUnitBs / docTasa;
                    } else if (costUnitBs <= 0 && costUnitUsd > 0 && docTasa > 0) {
                        costUnitBs = costUnitUsd * docTasa;
                    }

                    running += entrada;

                    double totalUsd = entrada * costUnitUsd;
                    double totalBs = entrada * costUnitBs;

                    salidaMovs.add(new KardexMovimientoValoradoRow(
                            fecha, tipoDoc, docNum, alm, origen, lote,
                            entrada, salida, running, costUnitUsd, costUnitBs, totalUsd, totalBs
                    ));
                }
            }
        } catch (SQLException e) {
            logger.error("[InventarioValoracionDAO] Error al consultar compras de {} en BD [{}]: {}", coArt, targetDb, e.getMessage(), e);
        }

        return running;
    }

    private void calcularValuacionArticuloAlCorte(String targetDb, String coArt, LocalDate fechaCorte,
                                                 String coAlma, double tasaCorte, KardexConsultaResult result) {
        try (Connection conn = DatabaseConfig.getConnection(targetDb)) {
            // 1. Obtener datos del artículo
            String sqlArt = """
                SELECT RTRIM(a.art_des) AS art_des, a.tipo,
                       ISNULL(a.campo8, '') AS campo8,
                       ISNULL(p.prov_des, '') AS nom_prov,
                       ISNULL(ap.co_prov, '') AS co_prov
                FROM saArticulo a
                LEFT JOIN (SELECT co_art, co_prov FROM saArtProveedorReng WHERE reng_num = 1) ap ON a.co_art = ap.co_art
                LEFT JOIN saProveedor p ON ap.co_prov = p.co_prov
                WHERE RTRIM(a.co_art) = ?
                """;
            try (PreparedStatement ps = conn.prepareStatement(sqlArt)) {
                ps.setString(1, coArt.trim());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        result.setArtDes(rs.getString("art_des"));
                    }
                }
            }

            // 2. Reconstruir existencia a fechaCorte
            double existencia = obtenerSaldoInicialEnBd(targetDb, coArt, fechaCorte, coAlma);
            result.setExistenciaFecha(existencia);

            if (existencia <= 0.0001) {
                result.setCostoPromedioUSD(0.0);
                result.setCostoPromedioBS(0.0);
                result.setTotalValorizadoUSD(0.0);
                result.setTotalValorizadoBS(0.0);
                return;
            }

            // 3. Determinar tipo de mercancía (Consignación vs Venta Firme)
            boolean esConsignacion = false;
            if (coAlma != null && !coAlma.isBlank() && !"TODOS".equalsIgnoreCase(coAlma.trim())) {
                esConsignacion = ALMACENES_CONSIGNACION.contains(coAlma.trim().toUpperCase());
            } else {
                String sqlCheckConsig = """
                    SELECT TOP 1 co_alma FROM saStockAlmacen
                    WHERE RTRIM(co_art) = ? AND stock > 0
                    ORDER BY CASE WHEN RTRIM(co_alma) IN ('02', 'VC') THEN 1 ELSE 2 END
                    """;
                try (PreparedStatement ps = conn.prepareStatement(sqlCheckConsig)) {
                    ps.setString(1, coArt.trim());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            String alm = rs.getString("co_alma");
                            esConsignacion = alm != null && ALMACENES_CONSIGNACION.contains(alm.trim().toUpperCase());
                        }
                    }
                }
            }

            if (esConsignacion) {
                // Caso Consignación: Costo Proveedor
                double costoProvUsd = obtenerCostoProveedor(conn, coArt, fechaCorte);
                if (costoProvUsd <= 0) {
                    String sqlCampo8 = "SELECT campo8 FROM saArticulo WHERE RTRIM(co_art) = ?";
                    try (PreparedStatement ps = conn.prepareStatement(sqlCampo8)) {
                        ps.setString(1, coArt.trim());
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                String c8 = rs.getString("campo8");
                                if (c8 != null && !c8.isBlank()) {
                                    try {
                                        costoProvUsd = Double.parseDouble(c8.trim().replace(",", "."));
                                    } catch (Exception ignored) {}
                                }
                            }
                        }
                    }
                }

                double costoProvBs = costoProvUsd * tasaCorte;
                double totUsd = existencia * costoProvUsd;
                double totBs = existencia * costoProvBs;

                result.setCostoPromedioUSD(costoProvUsd);
                result.setCostoPromedioBS(costoProvBs);
                result.setTotalValorizadoUSD(totUsd);
                result.setTotalValorizadoBS(totBs);
            } else {
                // Caso Venta Firme: Capas FIFO
                String sqlCompras = """
                    SELECT r.total_art, r.cost_unit, r.cost_unit_om, ISNULL(c.tasa, 1) AS tasa
                    FROM saNotaRecepcionCompraReng r
                    JOIN saNotaRecepcionCompra c ON r.doc_num = c.doc_num
                    WHERE RTRIM(r.co_art) = ? AND c.anulado = 0
                      AND CAST(c.fec_emis AS DATE) <= ?
                    ORDER BY c.fec_emis DESC, r.doc_num DESC
                    """;

                double cantFaltante = existencia;
                double sumaUSD = 0.0;
                double sumaBS = 0.0;
                double ultimoCostoUsd = 0.0;

                try (PreparedStatement ps = conn.prepareStatement(sqlCompras)) {
                    ps.setString(1, coArt.trim());
                    ps.setDate(2, java.sql.Date.valueOf(fechaCorte));

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next() && cantFaltante > 0.0001) {
                            double totArt = rs.getDouble("total_art");
                            double cBs = rs.getDouble("cost_unit");
                            double cUsd = rs.getDouble("cost_unit_om");
                            double tasa = rs.getDouble("tasa");

                            if (cUsd <= 0 && cBs > 0 && tasa > 0) cUsd = cBs / tasa;
                            else if (cBs <= 0 && cUsd > 0 && tasa > 0) cBs = cUsd * tasa;

                            if (ultimoCostoUsd <= 0) ultimoCostoUsd = cUsd;

                            double cantTomar = Math.min(cantFaltante, totArt);
                            sumaUSD += (cantTomar * cUsd);
                            sumaBS += (cantTomar * cBs);
                            cantFaltante -= cantTomar;
                        }
                    }
                }

                // Si aún queda existencia sin cubrir por compras en DROA_A, continuar consumiendo capas de DROA2_A
if (cantFaltante > 0.0001 && DB_ACTUAL.equalsIgnoreCase(targetDb)) {
    LocalDate fechaCorteHist = fechaCorte.isAfter(FECHA_CORTE_MIGRACION) ? FECHA_CORTE_MIGRACION : fechaCorte;
    try (Connection connHist = DatabaseConfig.getConnection(DB_HISTORICA);
         PreparedStatement psHist = connHist.prepareStatement(sqlCompras)) {
        psHist.setString(1, coArt.trim());
        psHist.setDate(2, java.sql.Date.valueOf(fechaCorteHist));
        try (ResultSet rsHist = psHist.executeQuery()) {
            while (rsHist.next() && cantFaltante > 0.0001) {
                double totArt = rsHist.getDouble("total_art");
                double cBs = rsHist.getDouble("cost_unit");
                double cUsd = rsHist.getDouble("cost_unit_om");
                double tasa = rsHist.getDouble("tasa");

                if (cUsd <= 0 && cBs > 0 && tasa > 0) cUsd = cBs / tasa;
                else if (cBs <= 0 && cUsd > 0 && tasa > 0) cBs = cUsd * tasa;

                if (ultimoCostoUsd <= 0) ultimoCostoUsd = cUsd;

                double cantTomar = Math.min(cantFaltante, totArt);
                sumaUSD += (cantTomar * cUsd);
                sumaBS += (cantTomar * cBs);
                cantFaltante -= cantTomar;
            }
        }
    } catch (Exception e) {
        logger.warn("[InventarioValoracionDAO] Error al consultar compras históricas en [{}] para {}: {}", DB_HISTORICA, coArt, e.getMessage());
    }
}

if (cantFaltante > 0.0001) {
                    double fallbackUsd = obtenerCostoProveedor(conn, coArt, fechaCorte);
                    if (fallbackUsd <= 0) fallbackUsd = ultimoCostoUsd;
                    double fallbackBs = fallbackUsd * tasaCorte;
                    sumaUSD += (cantFaltante * fallbackUsd);
                    sumaBS += (cantFaltante * fallbackBs);
                }

                double costoPromUSD = sumaUSD / existencia;
                double costoPromBS = sumaBS / existencia;

                result.setCostoPromedioUSD(costoPromUSD);
                result.setCostoPromedioBS(costoPromBS);
                result.setTotalValorizadoUSD(sumaUSD);
                result.setTotalValorizadoBS(sumaBS);
            }

        } catch (SQLException e) {
            logger.error("[InventarioValoracionDAO] Error al calcular valuación corte para {}: {}", coArt, e.getMessage(), e);
        }
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
    private Map<String, List<CapaCompraRaw>> cargarComprasPrevias(Connection conn, String targetDb, LocalDate fechaCorte) throws SQLException {
        Map<String, List<CapaCompraRaw>> map = new HashMap<>();

        // 1. Cargar compras de la base principal (targetDb)
        poblarComprasDesdeBd(conn, java.sql.Date.valueOf(fechaCorte), map);

        // 2. Si la base actual es DROA_A (post-migración), cargar compras históricas de DROA2_A
        if (DB_ACTUAL.equalsIgnoreCase(targetDb)) {
            LocalDate fechaCorteHist = fechaCorte.isAfter(FECHA_CORTE_MIGRACION) ? FECHA_CORTE_MIGRACION : fechaCorte;
            try (Connection connHist = DatabaseConfig.getConnection(DB_HISTORICA)) {
                poblarComprasDesdeBd(connHist, java.sql.Date.valueOf(fechaCorteHist), map);
            } catch (Exception e) {
                logger.warn("[InventarioValoracionDAO] No se pudieron cargar compras históricas de [{}]: {}", DB_HISTORICA, e.getMessage());
            }
        }

        // 3. Ordenar capas cronológicamente descendente (más reciente primero) para aplicar FIFO
        for (List<CapaCompraRaw> list : map.values()) {
            list.sort((c1, c2) -> {
                int cmp = c2.fecha.compareTo(c1.fecha);
                if (cmp != 0) return cmp;
                return c2.docNum.compareTo(c1.docNum);
            });
        }

        return map;
    }

    private void poblarComprasDesdeBd(Connection conn, java.sql.Date sqlFechaCorte, Map<String, List<CapaCompraRaw>> map) throws SQLException {
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
    }

    /**
     * Carga los últimos costos vigentes en USD de saCostoHistoricoEntrada para los productos (Costo Proveedor).
     * Mapea tanto por co_art como por art_rowguid.
     */
    private Map<String, Double> cargarCostosHistoricosConsignacion(Connection conn, String targetDb, LocalDate fechaCorte) throws SQLException {
        Map<String, Double> map = new HashMap<>();

        // Si targetDb es DROA_A, cargar primero de DROA2_A como base histórica
        if (DB_ACTUAL.equalsIgnoreCase(targetDb)) {
            LocalDate fechaCorteHist = fechaCorte.isAfter(FECHA_CORTE_MIGRACION) ? FECHA_CORTE_MIGRACION : fechaCorte;
            try (Connection connHist = DatabaseConfig.getConnection(DB_HISTORICA)) {
                poblarCostosHistoricos(connHist, java.sql.Date.valueOf(fechaCorteHist), map);
            } catch (Exception e) {
                logger.warn("[InventarioValoracionDAO] No se pudieron cargar costos consignación de [{}]: {}", DB_HISTORICA, e.getMessage());
            }
        }

        // Cargar costos de targetDb (sobrescribirá con los costos más recientes si los hay)
        poblarCostosHistoricos(conn, java.sql.Date.valueOf(fechaCorte), map);

        return map;
    }

    private void poblarCostosHistoricos(Connection conn, java.sql.Date sqlFechaCorte, Map<String, Double> map) throws SQLException {
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
                WHERE tipo_doc = 'PROV' AND costo > 0
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

        // Si no se encontró en la conexión actual (DROA_A), verificar en la base histórica DROA2_A
        try (Connection connHist = DatabaseConfig.getConnection(DB_HISTORICA);
             PreparedStatement psHist = connHist.prepareStatement(sql)) {
            psHist.setString(1, coArt.trim());
            LocalDate fechaCorteHist = (fechaCorte != null && fechaCorte.isAfter(FECHA_CORTE_MIGRACION)) ? FECHA_CORTE_MIGRACION : fechaCorte;
            psHist.setDate(2, java.sql.Date.valueOf(fechaCorteHist));
            try (ResultSet rsHist = psHist.executeQuery()) {
                if (rsHist.next()) {
                    return rsHist.getDouble("costo");
                }
            }
        } catch (Exception ignored) {}

        return 0.0;
    }

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

    /**
     * Consulta y calcula el Costo de Ventas y Margen para todas las ventas realizadas en un período.
     * Reglas de costeo (según requerimiento contable):
     * - Venta Firme: Costo promedio ponderado del producto para el momento de realizarse la venta.
     * - Consignación: Último costo reportado por el proveedor (saCostoHistoricoEntrada tipo 'PROV')
     *   a la fecha de la venta.
     *
     * @param targetDb Base de datos de consulta (por defecto DROA_A).
     * @param desde Fecha de inicio del período.
     * @param hasta Fecha de fin del período.
     * @param filtroAlmacen Filtro opcional por almacén ("TODOS", "01", "02", etc.).
     * @param filtroTipo Filtro opcional por tipo de mercancía ("TODOS", "VENTA_FIRME", "CONSIGNACION").
     * @param filtroBusqueda Búsqueda opcional por código/nombre de artículo o cliente.
     * @return Objeto CostoVentasPeriodoResult con todos los renglones y totales consolidados.
     */
    public CostoVentasPeriodoResult consultarCostoVentasPeriodo(
            String targetDb,
            LocalDate desde,
            LocalDate hasta,
            String filtroAlmacen,
            String filtroTipo,
            String filtroBusqueda) {

        CostoVentasPeriodoResult result = new CostoVentasPeriodoResult();
        if (desde == null || hasta == null) return result;
        if (targetDb == null || targetDb.isBlank()) targetDb = DB_ACTUAL;

        long t0 = System.currentTimeMillis();
        logger.info("[InventarioValoracionDAO] Iniciando cálculo de Costo de Ventas del [{}] al [{}] en [{}]", desde, hasta, targetDb);

        try (Connection conn = DatabaseConfig.getConnection(targetDb)) {
            // 1. Cargar historial cronológico de costos reportados por proveedores (USD)
            Map<String, NavigableMap<String, Double>> histCostoProv = new HashMap<>();
            String sqlHist = """
                SELECT RTRIM(a.co_art) AS co_art,
                       CONVERT(VARCHAR(10), ce.fecha_emision, 120) AS fecha_emision,
                       ce.costo
                FROM saCostoHistoricoEntrada ce
                JOIN saArticulo a ON ce.cod_articulo_rowguid = a.rowguid
                WHERE ce.tipo_doc = 'PROV' AND ce.costo > 0
                  AND CAST(ce.fecha_emision AS DATE) <= ?
                ORDER BY a.co_art, ce.fecha_emision ASC
            """;

            if (DB_ACTUAL.equalsIgnoreCase(targetDb)) {
    try (Connection connHist = DatabaseConfig.getConnection(DB_HISTORICA);
         PreparedStatement psHist = connHist.prepareStatement(sqlHist)) {
        LocalDate fechaHist = hasta.isAfter(FECHA_CORTE_MIGRACION) ? FECHA_CORTE_MIGRACION : hasta;
        psHist.setDate(1, java.sql.Date.valueOf(fechaHist));
        try (ResultSet rsHist = psHist.executeQuery()) {
            while (rsHist.next()) {
                String art = rsHist.getString("co_art");
                String fec = rsHist.getString("fecha_emision");
                double costo = rsHist.getDouble("costo");
                histCostoProv.computeIfAbsent(art, k -> new TreeMap<>()).put(fec, costo);
            }
        }
    } catch (Exception e) {
        logger.warn("[InventarioValoracionDAO] Error al cargar histórico de costos proveedor de [{}]: {}", DB_HISTORICA, e.getMessage());
    }
}

try (PreparedStatement ps = conn.prepareStatement(sqlHist)) {
                ps.setDate(1, java.sql.Date.valueOf(hasta));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String art = rs.getString("co_art");
                        String fec = rs.getString("fecha_emision");
                        double costo = rs.getDouble("costo");
                        histCostoProv.computeIfAbsent(art, k -> new TreeMap<>()).put(fec, costo);
                    }
                }
            }

            // 2. Cargar costos de fallback desde campo8 de saArticulo (USD)
            Map<String, Double> campo8Map = new HashMap<>();
            String sqlCampo8 = "SELECT RTRIM(co_art) AS co_art, ISNULL(campo8, '') AS campo8 FROM saArticulo";
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlCampo8)) {
                while (rs.next()) {
                    String art = rs.getString("co_art");
                    String c8 = rs.getString("campo8");
                    if (c8 != null && !c8.isBlank()) {
                        try {
                            double c = Double.parseDouble(c8.trim().replace(",", "."));
                            if (c > 0) campo8Map.put(art, c);
                        } catch (Exception ignored) {}
                    }
                }
            }

            // 3. Cargar compras históricas (saNotaRecepcionCompra) hasta la fecha fin para Venta Firme
            Map<String, List<CapaCompraRaw>> comprasMap = new HashMap<>();
            String sqlCompras = """
                SELECT RTRIM(r.co_art) AS co_art,
                       CONVERT(VARCHAR(10), c.fec_emis, 120) AS fecha,
                       r.total_art, r.cost_unit, r.cost_unit_om, ISNULL(c.tasa, 1) AS tasa,
                       RTRIM(ISNULL(c.co_prov, '')) AS co_prov,
                       RTRIM(ISNULL(p.prov_des, '')) AS nom_prov
                FROM saNotaRecepcionCompraReng r
                JOIN saNotaRecepcionCompra c ON r.doc_num = c.doc_num
                LEFT JOIN saProveedor p ON c.co_prov = p.co_prov
                WHERE c.anulado = 0 AND CAST(c.fec_emis AS DATE) <= ?
                ORDER BY c.fec_emis ASC, r.doc_num ASC
            """;

            if (DB_ACTUAL.equalsIgnoreCase(targetDb)) {
    try (Connection connHist = DatabaseConfig.getConnection(DB_HISTORICA);
         PreparedStatement psHist = connHist.prepareStatement(sqlCompras)) {
        LocalDate fechaHist = hasta.isAfter(FECHA_CORTE_MIGRACION) ? FECHA_CORTE_MIGRACION : hasta;
        psHist.setDate(1, java.sql.Date.valueOf(fechaHist));
        try (ResultSet rsHist = psHist.executeQuery()) {
            while (rsHist.next()) {
                String art = rsHist.getString("co_art");
                String f = rsHist.getString("fecha");
                double cant = rsHist.getDouble("total_art");
                double cBs = rsHist.getDouble("cost_unit");
                double cUsd = rsHist.getDouble("cost_unit_om");
                double tasa = rsHist.getDouble("tasa");
                if (cUsd <= 0 && cBs > 0 && tasa > 0) cUsd = cBs / tasa;
                else if (cBs <= 0 && cUsd > 0 && tasa > 0) cBs = cUsd * tasa;

                comprasMap.computeIfAbsent(art, k -> new ArrayList<>())
                          .add(new CapaCompraRaw("NREC", "", f, cant, cUsd, cBs, tasa,
                                                 rsHist.getString("co_prov"), rsHist.getString("nom_prov")));
            }
        }
    } catch (Exception e) {
        logger.warn("[InventarioValoracionDAO] Error al cargar compras históricas de [{}]: {}", DB_HISTORICA, e.getMessage());
    }
}

try (PreparedStatement ps = conn.prepareStatement(sqlCompras)) {
                ps.setDate(1, java.sql.Date.valueOf(hasta));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String art = rs.getString("co_art");
                        String f = rs.getString("fecha");
                        double cant = rs.getDouble("total_art");
                        double cBs = rs.getDouble("cost_unit");
                        double cUsd = rs.getDouble("cost_unit_om");
                        double tasa = rs.getDouble("tasa");
                        if (cUsd <= 0 && cBs > 0 && tasa > 0) cUsd = cBs / tasa;
                        else if (cBs <= 0 && cUsd > 0 && tasa > 0) cBs = cUsd * tasa;

                        comprasMap.computeIfAbsent(art, k -> new ArrayList<>())
                                  .add(new CapaCompraRaw("NREC", "", f, cant, cUsd, cBs, tasa,
                                                         rs.getString("co_prov"), rs.getString("nom_prov")));
                    }
                }
            }

            for (List<CapaCompraRaw> list : comprasMap.values()) {
                list.sort((c1, c2) -> {
                    int cmp = c1.fecha.compareTo(c2.fecha);
                    if (cmp != 0) return cmp;
                    return c1.docNum.compareTo(c2.docNum);
                });
            }


            // 4. Consultar ventas del período (Notas de Entrega de Venta y Facturas de Venta directas)
            String sqlVentas = """
                SELECT 
                    'NENT' AS tipo_doc, RTRIM(n.doc_num) AS doc_num, CONVERT(VARCHAR(10), n.fec_emis, 120) AS fec_emis,
                    RTRIM(n.co_cli) AS co_cli, RTRIM(ISNULL(c.cli_des, '')) AS cli_des,
                    RTRIM(r.co_art) AS co_art, RTRIM(r.des_art) AS art_des, RTRIM(r.co_alma) AS co_alma,
                    r.total_art, r.prec_vta, r.prec_vta_om, ISNULL(n.tasa, 1) AS tasa
                FROM saNotaEntregaVentaReng r
                JOIN saNotaEntregaVenta n ON r.doc_num = n.doc_num
                LEFT JOIN saCliente c ON n.co_cli = c.co_cli
                WHERE n.anulado = 0 AND CAST(n.fec_emis AS DATE) BETWEEN ? AND ?

                UNION ALL

                SELECT 
                    'FACT' AS tipo_doc, RTRIM(f.doc_num) AS doc_num, CONVERT(VARCHAR(10), f.fec_emis, 120) AS fec_emis,
                    RTRIM(f.co_cli) AS co_cli, RTRIM(ISNULL(c.cli_des, '')) AS cli_des,
                    RTRIM(r.co_art) AS co_art, RTRIM(r.des_art) AS art_des, RTRIM(r.co_alma) AS co_alma,
                    r.total_art, r.prec_vta, r.prec_vta_om, ISNULL(f.tasa, 1) AS tasa
                FROM saFacturaVentaReng r
                JOIN saFacturaVenta f ON r.doc_num = f.doc_num
                LEFT JOIN saCliente c ON f.co_cli = c.co_cli
                WHERE f.anulado = 0 AND (r.num_doc IS NULL OR RTRIM(r.num_doc) = '')
                  AND CAST(f.fec_emis AS DATE) BETWEEN ? AND ?
                ORDER BY fec_emis ASC, doc_num ASC
            """;

            List<CostoVentaRow> items = new ArrayList<>();
            double totVentaUSD = 0.0, totVentaBS = 0.0;
            double totCostoUSD = 0.0, totCostoBS = 0.0;
            double totCostoFirmeUSD = 0.0, totCostoFirmeBS = 0.0;
            double totCostoConsigUSD = 0.0, totCostoConsigBS = 0.0;
            double totUnidades = 0.0;

            String busqUpper = (filtroBusqueda != null && !filtroBusqueda.isBlank()) ? filtroBusqueda.trim().toUpperCase() : null;
            String almUpper = (filtroAlmacen != null && !filtroAlmacen.isBlank() && !filtroAlmacen.startsWith("TODOS"))
                    ? filtroAlmacen.trim().substring(0, Math.min(2, filtroAlmacen.trim().length())).toUpperCase() : null;

            try (PreparedStatement ps = conn.prepareStatement(sqlVentas)) {
                ps.setDate(1, java.sql.Date.valueOf(desde));
                ps.setDate(2, java.sql.Date.valueOf(hasta));
                ps.setDate(3, java.sql.Date.valueOf(desde));
                ps.setDate(4, java.sql.Date.valueOf(hasta));

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String coArt = rs.getString("co_art");
                        String artDes = rs.getString("art_des");
                        String coCli = rs.getString("co_cli");
                        String cliDes = rs.getString("cli_des");
                        String coAlma = rs.getString("co_alma");
                        String fecVenta = rs.getString("fec_emis");
                        String tipoDoc = rs.getString("tipo_doc");
                        String docNum = rs.getString("doc_num");
                        double cant = rs.getDouble("total_art");
                        double pUsd = rs.getDouble("prec_vta_om");
                        double pBs = rs.getDouble("prec_vta");
                        double tasa = rs.getDouble("tasa");

                        if (cant <= 0.0001) continue;

                        boolean esConsig = ALMACENES_CONSIGNACION.contains(coAlma.toUpperCase());
                        String tipoMercancia = esConsig ? "CONSIGNACION" : "VENTA FIRME";

                        // Filtro de almacén
                        if (almUpper != null && !coAlma.toUpperCase().startsWith(almUpper)) {
                            continue;
                        }

                        // Filtro de tipo mercancía
                        if ("VENTA_FIRME".equalsIgnoreCase(filtroTipo) && esConsig) {
                            continue;
                        }
                        if ("CONSIGNACION".equalsIgnoreCase(filtroTipo) && !esConsig) {
                            continue;
                        }

                        // Filtro de texto de búsqueda
                        if (busqUpper != null) {
                            boolean match = (coArt != null && coArt.toUpperCase().contains(busqUpper))
                                    || (artDes != null && artDes.toUpperCase().contains(busqUpper))
                                    || (coCli != null && coCli.toUpperCase().contains(busqUpper))
                                    || (cliDes != null && cliDes.toUpperCase().contains(busqUpper))
                                    || (docNum != null && docNum.toUpperCase().contains(busqUpper));
                            if (!match) continue;
                        }

                        if (pUsd <= 0 && pBs > 0 && tasa > 0) pUsd = pBs / tasa;
                        else if (pBs <= 0 && pUsd > 0 && tasa > 0) pBs = pUsd * tasa;

                        double costUnitUsd = 0.0;
                        double costUnitBs = 0.0;

                        if (esConsig) {
                            // Regla Contable: Último costo reportado por el proveedor a la fecha de la venta
                            NavigableMap<String, Double> artHist = histCostoProv.get(coArt);
                            if (artHist != null) {
                                Map.Entry<String, Double> entry = artHist.floorEntry(fecVenta);
                                if (entry != null && entry.getValue() > 0) {
                                    costUnitUsd = entry.getValue();
                                }
                            }
                            if (costUnitUsd <= 0) {
                                costUnitUsd = campo8Map.getOrDefault(coArt, 0.0);
                            }
                            costUnitBs = costUnitUsd * tasa;

                            totCostoConsigUSD += (cant * costUnitUsd);
                            totCostoConsigBS += (cant * costUnitBs);
                        } else {
                            // Regla Contable: Costo promedio ponderado a la fecha de la venta
                            List<CapaCompraRaw> cList = comprasMap.get(coArt);
                            double sumU = 0, sumB = 0, sumQ = 0;
                            double lastU = 0, lastB = 0;
                            if (cList != null) {
                                for (CapaCompraRaw ci : cList) {
                                    if (ci.fecha.compareTo(fecVenta) <= 0) {
                                        sumU += (ci.totalArt * ci.costUnitUSD);
                                        sumB += (ci.totalArt * ci.costUnitBS);
                                        sumQ += ci.totalArt;
                                        lastU = ci.costUnitUSD;
                                        lastB = ci.costUnitBS;
                                    } else {
                                        break;
                                    }
                                }
                            }
                            if (sumQ > 0) {
                                costUnitUsd = sumU / sumQ;
                                costUnitBs = sumB / sumQ;
                            } else if (lastU > 0) {
                                costUnitUsd = lastU;
                                costUnitBs = lastB;
                            } else {
                                costUnitUsd = campo8Map.getOrDefault(coArt, 0.0);
                                costUnitBs = costUnitUsd * tasa;
                            }

                            totCostoFirmeUSD += (cant * costUnitUsd);
                            totCostoFirmeBS += (cant * costUnitBs);
                        }

                        double totalVentaRengUSD = cant * pUsd;
                        double totalVentaRengBS = cant * pBs;
                        double totalCostoRengUSD = cant * costUnitUsd;
                        double totalCostoRengBS = cant * costUnitBs;
                        double utilRengUSD = totalVentaRengUSD - totalCostoRengUSD;
                        double utilRengBS = totalVentaRengBS - totalCostoRengBS;
                        double margenRengPct = totalVentaRengUSD > 0 ? (utilRengUSD / totalVentaRengUSD) * 100 : 0.0;

                        CostoVentaRow row = new CostoVentaRow();
                        row.setTipoDoc(tipoDoc);
                        row.setDocNum(docNum);
                        row.setFecha(fecVenta);
                        row.setCoCli(coCli);
                        row.setCliDes(cliDes);
                        row.setCoArt(coArt);
                        row.setArtDes(artDes);
                        row.setCoAlma(coAlma);
                        row.setTipoMercancia(tipoMercancia);
                        row.setCantidad(cant);
                        row.setPrecioVentaUSD(pUsd);
                        row.setPrecioVentaBS(pBs);
                        row.setTotalVentaUSD(totalVentaRengUSD);
                        row.setTotalVentaBS(totalVentaRengBS);
                        row.setCostoUnitUSD(costUnitUsd);
                        row.setCostoUnitBS(costUnitBs);
                        row.setTotalCostoUSD(totalCostoRengUSD);
                        row.setTotalCostoBS(totalCostoRengBS);
                        row.setUtilidadUSD(utilRengUSD);
                        row.setUtilidadBS(utilRengBS);
                        row.setMargenPct(margenRengPct);
                        row.setTasaDoc(tasa);

                        items.add(row);

                        totUnidades += cant;
                        totVentaUSD += totalVentaRengUSD;
                        totVentaBS += totalVentaRengBS;
                        totCostoUSD += totalCostoRengUSD;
                        totCostoBS += totalCostoRengBS;
                    }
                }
            }

            double utilGlobalUSD = totVentaUSD - totCostoUSD;
            double utilGlobalBS = totVentaBS - totCostoBS;
            double margenGlobalPct = totVentaUSD > 0 ? (utilGlobalUSD / totVentaUSD) * 100 : 0.0;

            result.setItems(items);
            result.setTotalUnidades(totUnidades);
            result.setTotalVentaUSD(totVentaUSD);
            result.setTotalVentaBS(totVentaBS);
            result.setTotalCostoUSD(totCostoUSD);
            result.setTotalCostoBS(totCostoBS);
            result.setTotalCostoFirmeUSD(totCostoFirmeUSD);
            result.setTotalCostoFirmeBS(totCostoFirmeBS);
            result.setTotalCostoConsigUSD(totCostoConsigUSD);
            result.setTotalCostoConsigBS(totCostoConsigBS);
            result.setUtilidadUSD(utilGlobalUSD);
            result.setUtilidadBS(utilGlobalBS);
            result.setMargenPct(margenGlobalPct);

            long t1 = System.currentTimeMillis();
            logger.info("[InventarioValoracionDAO] Costo de Ventas calculado en {} ms. Renglones: {}, Ventas: ${}, Costo: ${}, Margen: {}%",
                    (t1 - t0), items.size(), String.format("%,.2f", totVentaUSD), String.format("%,.2f", totCostoUSD), String.format("%.2f", margenGlobalPct));

        } catch (SQLException e) {
            logger.error("[InventarioValoracionDAO] Error al consultar costo de ventas del período: {}", e.getMessage(), e);
        }

        return result;
    }

}

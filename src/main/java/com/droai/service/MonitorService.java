package com.droai.service;

import com.droai.dao.MatrizVentasDAO;
import com.droai.model.FacturaResumenRow;
import com.droai.model.MatrizVentasRow;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

/**
 * Servicio para el Monitor Situacional / Reportes de Ventas.
 *
 * <p>Procesa y agrega datos de la matriz de ventas para generar indicadores
 * clave de rendimiento (KPIs) y agrupaciones por tipo de impuesto (alícuota).
 *
 * <p>Reutiliza {@link MatrizVentasDAO} como fuente de datos primaria.
 */
public class MonitorService {

    private final MatrizVentasDAO matrizVentasDAO;

    public MonitorService() {
        this.matrizVentasDAO = new MatrizVentasDAO();
    }

    /**
     * Resultado completo del procesamiento del monitor situacional.
     *
     * @param montoTotal     monto total de ventas en Bs.
     * @param montoTotalUsd  monto total de ventas en USD ($) convertido fila por fila usando f.tasa.
     * @param totalDocumentos cantidad de documentos (facturas únicas).
     * @param totalUnidades  volumen total de artículos vendidos.
     * @param agrupaciones   lista de agrupaciones por tipo de impuesto.
     * @param rawRows        filas brutas para detalle adicional.
     */
    public record MonitorResult(
            double montoTotal,
            double montoTotalUsd,
            int totalDocumentos,
            double totalUnidades,
            List<AgrupacionImpuesto> agrupaciones,
            List<MatrizVentasRow> rawRows
    ) {}

    /**
     * Agrupación de ventas por tipo de impuesto / alícuota.
     *
     * @param grupo       nombre del grupo (ej: "ALICUOTA GENERAL", "EXENTO").
     * @param unidades    cantidad total de unidades vendidas en este grupo.
     * @param descripcion descripción del tipo de impuesto.
     * @param monto       monto total de ventas de este grupo.
     * @param porcentaje  porcentaje que representa sobre el total.
     */
    public record AgrupacionImpuesto(
            String grupo,
            double unidades,
            String descripcion,
            double monto,
            double porcentaje
    ) {}

    /**
     * Agrupación de ventas por línea de producto.
     *
     * @param codLinea código de la línea de producto.
     * @param linea    nombre o descripción de la línea.
     * @param unidades cantidad total de unidades vendidas.
     */
    public record AgrupacionLinea(
            String codLinea,
            String linea,
            double unidades
    ) {}

    /**
     * Procesa los datos de ventas para el período especificado.
     *
     * @param desde fecha de inicio del período (inclusive).
     * @param hasta fecha de fin del período (inclusive).
     * @return resultado con KPIs y agrupaciones.
     * @throws SQLException si ocurre un error de BD.
     * @throws IllegalArgumentException si las fechas son inválidas.
     */
    public MonitorResult procesar(LocalDate desde, LocalDate hasta) throws SQLException {
        if (desde == null || hasta == null) {
            throw new IllegalArgumentException("Las fechas 'desde' y 'hasta' no pueden ser nulas.");
        }
        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha 'desde' no puede ser posterior a 'hasta'.");
        }

        List<MatrizVentasRow> rows = matrizVentasDAO.fetchMatrizVentas(desde, hasta);

        // ── KPIs ──
        double montoTotal = 0;
        double montoTotalUsd = 0;
        double totalUnidades = 0;
        Set<String> documentosUnicos = new HashSet<>();

        // ── Agrupación por IVA% ──
        Map<String, double[]> grupoMap = new LinkedHashMap<>();
        // grupoMap: key = nombre del grupo, value = [unidades, monto]

        for (MatrizVentasRow row : rows) {
            double totalRenglon = row.getRenglonDg(); // r.reng_neto (Monto Neto del renglón en Bs)
            double tasa = row.getTasa() > 0 ? row.getTasa() : 1.0;

            montoTotal += totalRenglon;
            montoTotalUsd += (totalRenglon / tasa);
            totalUnidades += row.getCantidad();

            if (row.getNumero() != null) {
                documentosUnicos.add(row.getNumero().trim());
            }

            // Determinar grupo por % IVA
            String grupo = clasificarAlicuota(row.getIvaPct());
            grupoMap.computeIfAbsent(grupo, k -> new double[2]);
            double[] acum = grupoMap.get(grupo);
            acum[0] += row.getCantidad();  // unidades
            acum[1] += totalRenglon;        // monto
        }

        // ── Construir agrupaciones ──
        List<AgrupacionImpuesto> agrupaciones = new ArrayList<>();
        double montoFinal = montoTotal == 0 ? 1 : montoTotal; // evitar /0

        for (Map.Entry<String, double[]> entry : grupoMap.entrySet()) {
            double[] vals = entry.getValue();
            agrupaciones.add(new AgrupacionImpuesto(
                    entry.getKey(),
                    vals[0],
                    descripcionAlicuota(entry.getKey()),
                    vals[1],
                    (vals[1] / montoFinal) * 100
            ));
        }

        // Ordenar por monto descendente
        agrupaciones.sort((a, b) -> Double.compare(b.monto(), a.monto()));

        return new MonitorResult(
                montoTotal,
                montoTotalUsd,
                documentosUnicos.size(),
                totalUnidades,
                agrupaciones,
                rows
        );
    }

    /**
     * Clasifica un porcentaje de IVA en su grupo de alícuota.
     */
    private String clasificarAlicuota(double ivaPct) {
        if (ivaPct == 0) return "EXENTO";
        if (ivaPct == 8) return "ALICUOTA REDUCIDA";
        if (ivaPct == 16) return "ALICUOTA GENERAL";
        if (ivaPct == 31) return "ALICUOTA ADICIONAL";
        if (ivaPct > 0 && ivaPct < 8) return "ALICUOTA REDUCIDA";
        if (ivaPct > 8 && ivaPct <= 16) return "ALICUOTA GENERAL";
        if (ivaPct > 16) return "ALICUOTA ADICIONAL";
        return "OTROS (" + String.format("%.1f", ivaPct) + "%)";
    }

    /**
     * Devuelve una descripción legible para cada grupo de alícuota.
     */
    private String descripcionAlicuota(String grupo) {
        return switch (grupo) {
            case "EXENTO"             -> "Artículos exentos de IVA (0%)";
            case "ALICUOTA REDUCIDA"  -> "Artículos con alícuota reducida (8%)";
            case "ALICUOTA GENERAL"   -> "Artículos con alícuota general (16%)";
            case "ALICUOTA ADICIONAL" -> "Artículos con alícuota adicional (31%)";
            default                   -> grupo;
        };
    }

    /**
     * Agrupa una lista de renglones de venta por documento de factura.
     */
    public List<FacturaResumenRow> agruparFacturas(List<MatrizVentasRow> rawRows) {
        if (rawRows == null || rawRows.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, List<MatrizVentasRow>> porDoc = new LinkedHashMap<>();
        for (MatrizVentasRow r : rawRows) {
            String doc = r.getNumero() != null ? r.getNumero().trim() : "";
            porDoc.computeIfAbsent(doc, k -> new ArrayList<>()).add(r);
        }

        List<FacturaResumenRow> facturas = new ArrayList<>();
        for (Map.Entry<String, List<MatrizVentasRow>> entry : porDoc.entrySet()) {
            List<MatrizVentasRow> items = entry.getValue();
            MatrizVentasRow first = items.get(0);

            FacturaResumenRow f = new FacturaResumenRow();
            f.setNumero(entry.getKey());
            f.setFecha(first.getFecha());
            f.setCodCliente(first.getCodCliente());
            f.setNombreCliente(first.getNombreRazonSocial());
            f.setCodVen(first.getCoVen());
            f.setNombreVendedor(first.getNombreVendedor());
            f.setTasa(first.getTasa() > 0 ? first.getTasa() : 1.0);

            double bruto = 0;
            double neto = 0;
            double iva = 0;
            double total = 0;

            for (MatrizVentasRow r : items) {
                bruto += r.getTotalRenglon();
                neto += r.getRenglonDg();
                iva += r.getMontoIva();
                total += r.getTotRenglonIva();
            }

            double desc = bruto - neto;
            if (desc < 0) desc = 0;
            double descPct = bruto > 0 ? (desc / bruto) * 100.0 : 0.0;

            f.setMontoBruto(bruto);
            f.setDescuento(desc);
            f.setDescPct(descPct);
            f.setMontoNeto(neto);
            f.setIva(iva);
            f.setTotalGeneral(total);

            facturas.add(f);
        }

        return facturas;
    }

    /**
     * Agrupa una lista de renglones de venta por línea de producto.
     */
    public List<AgrupacionLinea> agruparLineas(List<MatrizVentasRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return new ArrayList<>();
        }
        Map<String, AgrupacionLinea> map = new LinkedHashMap<>();
        for (MatrizVentasRow r : rows) {
            String cod = r.getCodLinea() != null ? r.getCodLinea().trim() : "";
            String lin = r.getLinea() != null ? r.getLinea().trim() : "";
            String key = cod + "|" + lin;
            AgrupacionLinea existing = map.get(key);
            if (existing == null) {
                map.put(key, new AgrupacionLinea(cod, lin, r.getCantidad()));
            } else {
                map.put(key, new AgrupacionLinea(cod, lin, existing.unidades() + r.getCantidad()));
            }
        }
        List<AgrupacionLinea> list = new ArrayList<>(map.values());
        list.sort((a, b) -> Double.compare(b.unidades(), a.unidades()));
        return list;
    }
}

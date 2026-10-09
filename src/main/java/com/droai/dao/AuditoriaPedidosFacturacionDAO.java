package com.droai.dao;

import com.droai.config.DatabaseConfig;
import com.droai.model.AuditoriaPedidoRenglonRow;
import com.droai.model.AuditoriaPedidoRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/**
 * DAO para auditar la relación integral entre:
 * 1. Pedidos Web (Archivos TXT descargados al SIAD)
 * 2. Notas de Entrega (saNotaEntregaVenta en Profit Plus)
 * 3. Facturas de Venta (saFacturaVenta en Profit Plus)
 */
public class AuditoriaPedidosFacturacionDAO {

    private static final Logger logger = LoggerFactory.getLogger(AuditoriaPedidosFacturacionDAO.class);

    // Rutas candidatas para archivos procesados en SIAD
    private static final String[] RUTA_SIAD_CANDIDATAS = new String[]{
            "Y:\\ISB SISTEMAS\\SIAD\\PEDIDOS\\PROCESADOS",
            "\\\\SRVDB0101\\sysvol2\\ISB SISTEMAS\\SIAD\\PEDIDOS\\PROCESADOS"
    };

    /**
     * Localiza la ruta activa para los archivos de pedidos SIAD.
     */
    private File getCarpetaSiadProcesados() {
        for (String ruta : RUTA_SIAD_CANDIDATAS) {
            File dir = new File(ruta);
            if (dir.exists() && dir.isDirectory()) {
                return dir;
            }
        }
        return null;
    }

    /**
     * Ejecuta la auditoría completa para el rango de fechas especificado.
     */
    public List<AuditoriaPedidoRow> auditarPedidos(LocalDate desde, LocalDate hasta) {
        List<AuditoriaPedidoRow> resultado = new ArrayList<>();

        Timestamp tsDesde = Timestamp.valueOf(desde.atStartOfDay());
        Timestamp tsHasta = Timestamp.valueOf(hasta.atTime(23, 59, 59));

        // 1. Cargar Notas de Entrega desde Profit Plus
        String sqlNotas = """
            SELECT 
                n.doc_num,
                RTRIM(ISNULL(n.campo1, '')) AS pedido_web,
                n.descrip,
                n.fec_emis,
                RTRIM(n.co_cli) AS co_cli,
                RTRIM(ISNULL(c.cli_des, '')) AS cli_des,
                RTRIM(n.co_ven) AS co_ven,
                RTRIM(ISNULL(v.ven_des, '')) AS ven_des,
                n.total_neto,
                n.saldo,
                n.anulado,
                n.status
            FROM saNotaEntregaVenta n
            LEFT JOIN saCliente c ON n.co_cli = c.co_cli
            LEFT JOIN saVendedor v ON n.co_ven = v.co_ven
            WHERE n.fec_emis BETWEEN ? AND ?
            ORDER BY n.fec_emis DESC, n.doc_num DESC
            """;

        // 2. Cargar Renglones de Notas de Entrega y su estado de Facturación
        String sqlRenglones = """
            SELECT 
                nr.doc_num,
                nr.reng_num,
                RTRIM(nr.co_art) AS co_art,
                RTRIM(ISNULL(a.art_des, '')) AS art_des,
                nr.total_art,
                nr.prec_vta,
                RTRIM(ISNULL(nr.porc_desc, '')) AS porc_desc,
                nr.reng_neto,
                nr.pendiente,
                RTRIM(ISNULL(rf.doc_num, '')) AS factura_num,
                ISNULL(rf.total_art, 0) AS cant_facturada,
                ISNULL(rf.reng_neto, 0) AS monto_facturado
            FROM saNotaEntregaVentaReng nr
            INNER JOIN saNotaEntregaVenta n ON nr.doc_num = n.doc_num
            LEFT JOIN saArticulo a ON nr.co_art = a.co_art
            LEFT JOIN (
                SELECT fr.rowguid_doc, fr.doc_num, fr.total_art, fr.reng_neto
                FROM saFacturaVentaReng fr
                INNER JOIN saFacturaVenta f ON fr.doc_num = f.doc_num
                WHERE f.anulado = 0 AND fr.tipo_doc = 'NENT'
            ) rf ON nr.rowguid = rf.rowguid_doc
            WHERE n.fec_emis BETWEEN ? AND ?
            ORDER BY nr.doc_num, nr.reng_num
            """;

        Map<String, AuditoriaPedidoRow> mapNotas = new LinkedHashMap<>();
        Map<String, List<AuditoriaPedidoRenglonRow>> mapRenglonesNota = new HashMap<>();

        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {

            // Cargar cabeceras de Notas
            try (PreparedStatement ps = conn.prepareStatement(sqlNotas)) {
                ps.setTimestamp(1, tsDesde);
                ps.setTimestamp(2, tsHasta);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        AuditoriaPedidoRow row = new AuditoriaPedidoRow();
                        String docNum = rs.getString("doc_num").trim();
                        String pedWeb = rs.getString("pedido_web").trim();
                        if (pedWeb.isEmpty()) {
                            // Extraer del descrip si tiene "PEDIDO WEB 101469"
                            String desc = rs.getString("descrip");
                            if (desc != null && desc.contains("PEDIDO WEB ")) {
                                String[] parts = desc.split(" ");
                                for (int i = 0; i < parts.length - 1; i++) {
                                    if ("WEB".equalsIgnoreCase(parts[i]) && i + 1 < parts.length) {
                                        pedWeb = parts[i + 1].trim();
                                        break;
                                    }
                                }
                            }
                        }
                        if (pedWeb.isEmpty()) {
                            pedWeb = docNum;
                        }

                        row.setDocNumOriginal(docNum);
                        row.setNotaEntrega(docNum);
                        row.setPedidoWeb(pedWeb);
                        row.setFecEmis(rs.getTimestamp("fec_emis"));
                        row.setCoCli(rs.getString("co_cli"));
                        row.setCliDes(rs.getString("cli_des"));
                        row.setCoVen(rs.getString("co_ven"));
                        row.setVenDes(rs.getString("ven_des"));
                        row.setTotalMontoNota(rs.getDouble("total_neto"));
                        row.setAnulado(rs.getBoolean("anulado"));

                        mapNotas.put(docNum, row);
                    }
                }
            }

            // Cargar renglones de Notas
            try (PreparedStatement ps = conn.prepareStatement(sqlRenglones)) {
                ps.setTimestamp(1, tsDesde);
                ps.setTimestamp(2, tsHasta);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String docNum = rs.getString("doc_num").trim();
                        AuditoriaPedidoRenglonRow reng = new AuditoriaPedidoRenglonRow();
                        reng.setRengNum(rs.getInt("reng_num"));
                        reng.setCoArt(rs.getString("co_art"));
                        reng.setArtDes(rs.getString("art_des"));
                        reng.setCantNota(rs.getDouble("total_art"));
                        reng.setPrecVta(rs.getDouble("prec_vta"));
                        reng.setPorcDesc(rs.getString("porc_desc"));
                        reng.setRengNeto(rs.getDouble("reng_neto"));

                        String facNum = rs.getString("factura_num").trim();
                        double cantFact = rs.getDouble("cant_facturada");
                        double pendiente = rs.getDouble("pendiente");

                        reng.setFacturaDoc(facNum);
                        reng.setCantFact(cantFact);

                        if (cantFact > 0 || (pendiente <= 0.0001 && !facNum.isEmpty())) {
                            reng.setEstado("FACTURADO");
                            if (cantFact <= 0) reng.setCantFact(reng.getCantNota());
                        } else {
                            reng.setEstado("PENDIENTE_FACTURA");
                        }

                        mapRenglonesNota.computeIfAbsent(docNum, k -> new ArrayList<>()).add(reng);
                    }
                }
            }

        } catch (SQLException e) {
            logger.error("Error al consultar notas y facturas en Profit Plus", e);
        }

        // 3. Cargar y correlacionar archivos TXT de SIAD
        File dirSiad = getCarpetaSiadProcesados();
        Map<String, List<TxtItemInfo>> mapTxtPedidos = new HashMap<>();

        if (dirSiad != null && dirSiad.exists()) {
            File[] files = dirSiad.listFiles((d, name) -> name.toLowerCase().startsWith("pedido") && name.toLowerCase().endsWith(".txt"));
            if (files != null) {
                for (File f : files) {
                    try {
                        String pedId = f.getName().replace("pedido", "").replace(".txt", "").replace("PEDIDO", "").trim();
                        List<TxtItemInfo> items = leerTxtPedido(f);
                        if (!items.isEmpty()) {
                            mapTxtPedidos.put(pedId, items);
                        }
                    } catch (Exception ex) {
                        logger.warn("Error leyendo archivo TXT de SIAD: {}", f.getName(), ex);
                    }
                }
            }
        }

        // 4. Consolidar Auditoría
        // Vincular notas complementarias 'B' con su pedido original
        Map<String, AuditoriaPedidoRow> pedidosConsolidados = new LinkedHashMap<>();

        for (AuditoriaPedidoRow nota : mapNotas.values()) {
            String docNum = nota.getNotaEntrega();
            List<AuditoriaPedidoRenglonRow> renglonesNota = mapRenglonesNota.getOrDefault(docNum, new ArrayList<>());
            nota.setRenglones(new ArrayList<>(renglonesNota));

            // Calcular métricas de la Nota
            int rengNota = renglonesNota.size();
            double undNota = 0.0;
            int rengFact = 0;
            double undFact = 0.0;
            double montoFact = 0.0;
            Set<String> facturasSet = new LinkedHashSet<>();

            for (AuditoriaPedidoRenglonRow r : renglonesNota) {
                undNota += r.getCantNota();
                if ("FACTURADO".equals(r.getEstado())) {
                    rengFact++;
                    undFact += r.getCantFact();
                    montoFact += (r.getCantFact() / (r.getCantNota() > 0 ? r.getCantNota() : 1)) * r.getRengNeto();
                    if (r.getFacturaDoc() != null && !r.getFacturaDoc().isEmpty()) {
                        facturasSet.add(r.getFacturaDoc());
                    }
                }
            }

            nota.setTotalRengNota(rengNota);
            nota.setTotalUndNota(undNota);
            nota.setTotalRengFact(rengFact);
            nota.setTotalUndFact(undFact);
            nota.setTotalMontoFact(montoFact);
            nota.setTotalMontoPend(Math.max(0, nota.getTotalMontoNota() - montoFact));
            nota.setFacturas(String.join(", ", facturasSet));

            // Comparar con Pedido Web TXT
            String pedKey = nota.getPedidoWeb();
            // Quitar sufijo B o espacios si los tiene para buscar el TXT original
            String cleanPedId = pedKey.replaceAll("[^0-9]", "");
            List<TxtItemInfo> txtItems = mapTxtPedidos.get(cleanPedId);
            if (txtItems == null) {
                txtItems = mapTxtPedidos.get(pedKey);
            }

            if (txtItems != null && !txtItems.isEmpty()) {
                nota.setTotalRengWeb(txtItems.size());
                double undWeb = 0.0;
                for (TxtItemInfo ti : txtItems) {
                    undWeb += ti.cant;
                }
                nota.setTotalUndWeb(undWeb);

                // Cotejar renglones individuales: detectar qué artículos del TXT faltan en la Nota
                Map<String, Double> cantNotaPorArt = new HashMap<>();
                for (AuditoriaPedidoRenglonRow rn : renglonesNota) {
                    cantNotaPorArt.merge(rn.getCoArt(), rn.getCantNota(), Double::sum);
                }

                int rengFaltantes = 0;
                double undFaltantes = 0.0;
                List<String> faltantesDesc = new ArrayList<>();

                for (TxtItemInfo ti : txtItems) {
                    double cantEnNota = cantNotaPorArt.getOrDefault(ti.coArt, 0.0);
                    if (cantEnNota < ti.cant) {
                        double dif = ti.cant - cantEnNota;
                        rengFaltantes++;
                        undFaltantes += dif;
                        faltantesDesc.add(ti.coArt + " (faltan " + formatNum(dif) + " unds)");

                        // Agregar renglón virtual faltante al detalle
                        AuditoriaPedidoRenglonRow rFaltante = new AuditoriaPedidoRenglonRow();
                        rFaltante.setRengNum(renglonesNota.size() + rengFaltantes);
                        rFaltante.setCoArt(ti.coArt);
                        rFaltante.setArtDes(ti.descrip);
                        rFaltante.setCantWeb(ti.cant);
                        rFaltante.setCantNota(cantEnNota);
                        rFaltante.setCantFact(0.0);
                        rFaltante.setCantFaltante(dif);
                        rFaltante.setPrecVta(ti.precio);
                        rFaltante.setRengNeto(ti.subtotal);
                        rFaltante.setNroLote(ti.lote);
                        rFaltante.setEstado("NO_VIAJO_A_NOTA");
                        nota.getRenglones().add(rFaltante);
                    }
                }

                // Asignar cantWeb en renglones que sí viajaron
                Map<String, Double> cantWebPorArt = new HashMap<>();
                for (TxtItemInfo ti : txtItems) {
                    cantWebPorArt.merge(ti.coArt, ti.cant, Double::sum);
                }
                for (AuditoriaPedidoRenglonRow rn : nota.getRenglones()) {
                    if (!"NO_VIAJO_A_NOTA".equals(rn.getEstado())) {
                        rn.setCantWeb(cantWebPorArt.getOrDefault(rn.getCoArt(), rn.getCantNota()));
                        rn.setCantFaltante(Math.max(0, rn.getCantNota() - rn.getCantFact()));
                    }
                }

                nota.setTotalRengFaltantes(rengFaltantes);
                nota.setTotalUndFaltantes(undFaltantes);

                // Determinar Estado y Diagnóstico
                if (nota.isAnulado()) {
                    nota.setEstado("ANULADO");
                    nota.setDiagnostico("Documento anulado");
                } else if (rengFaltantes > 0 || undFaltantes > 0.001) {
                    nota.setEstado("CRITICO_FALTAN_RENGLONES");
                    nota.setDiagnostico("🔴 ALERTA TIPO 101469: Faltan " + rengFaltantes + " renglón(es) / " + formatNum(undFaltantes) + " unds en Nota de Entrega");
                } else if (rengFact > 0 && rengFact < rengNota) {
                    nota.setEstado("PARCIALMENTE_FACTURADO");
                    nota.setDiagnostico("🟠 Facturación Parcial: " + rengFact + " de " + rengNota + " renglones facturados");
                } else if (rengFact == 0) {
                    nota.setEstado("PENDIENTE_FACTURAR");
                    nota.setDiagnostico("🟡 Pendiente por facturar (" + rengNota + " renglones listos)");
                } else {
                    nota.setEstado("FACTURADO_TOTAL");
                    nota.setDiagnostico("🟢 Facturado completo (100% de renglones y unidades)");
                }

            } else {
                // No se encontró TXT en SIAD (posible pedido directo o manual)
                nota.setTotalRengWeb(rengNota);
                nota.setTotalUndWeb(undNota);
                nota.setTotalRengFaltantes(0);
                nota.setTotalUndFaltantes(0.0);

                for (AuditoriaPedidoRenglonRow rn : nota.getRenglones()) {
                    rn.setCantWeb(rn.getCantNota());
                    rn.setCantFaltante(Math.max(0, rn.getCantNota() - rn.getCantFact()));
                }

                if (nota.isAnulado()) {
                    nota.setEstado("ANULADO");
                    nota.setDiagnostico("Documento anulado");
                } else if (rengFact > 0 && rengFact < rengNota) {
                    nota.setEstado("PARCIALMENTE_FACTURADO");
                    nota.setDiagnostico("🟠 Facturación Parcial en Profit: " + rengFact + " de " + rengNota + " facturados");
                } else if (rengFact == 0) {
                    nota.setEstado("PENDIENTE_FACTURAR");
                    nota.setDiagnostico("🟡 Pendiente por facturar en Profit");
                } else {
                    nota.setEstado("FACTURADO_TOTAL");
                    nota.setDiagnostico("🟢 Facturado completo");
                }
            }

            pedidosConsolidados.put(docNum, nota);
        }

        resultado.addAll(pedidosConsolidados.values());
        return resultado;
    }

    private static String formatNum(double val) {
        if (val == (long) val) {
            return String.format("%d", (long) val);
        } else {
            return String.format(Locale.US, "%.2f", val);
        }
    }

    /**
     * Estructura interna auxiliar para items del archivo TXT de SIAD.
     */
    @SuppressWarnings("unused")
    private static class TxtItemInfo {
        String itemId;
        String coArt;
        String descrip;
        double cant;
        double precio;
        double subtotal;
        String lote;
        String estadoDesp;
    }

    /**
     * Lee un archivo de pedido TXT del SIAD y extrae la lista de renglones solicitados.
     */
    private List<TxtItemInfo> leerTxtPedido(File file) {
        List<TxtItemInfo> items = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.ISO_8859_1))) {
            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (lineNum == 1) continue; // Línea 1 es cabecera
                String[] parts = line.split("\\|");
                if (parts.length > 5) {
                    TxtItemInfo item = new TxtItemInfo();
                    item.itemId = parts.length > 1 ? parts[1].trim() : "";
                    item.coArt = parts.length > 2 ? parts[2].trim() : "";
                    item.descrip = parts.length > 3 ? parts[3].trim() : "";
                    try {
                        item.cant = parts.length > 4 ? Double.parseDouble(parts[4].trim().replace(",", ".")) : 0.0;
                    } catch (Exception ignored) { item.cant = 0.0; }
                    try {
                        item.precio = parts.length > 5 ? Double.parseDouble(parts[5].trim().replace(",", ".")) : 0.0;
                    } catch (Exception ignored) { item.precio = 0.0; }
                    try {
                        item.subtotal = parts.length > 17 ? Double.parseDouble(parts[17].trim().replace(",", ".")) : (item.cant * item.precio);
                    } catch (Exception ignored) { item.subtotal = item.cant * item.precio; }
                    item.lote = parts.length > 24 ? parts[24].trim() : "";
                    item.estadoDesp = parts.length > 48 ? parts[48].trim() : "";

                    if (!item.coArt.isEmpty()) {
                        items.add(item);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Error leyendo TXT {}", file.getName(), e);
        }
        return items;
    }
}

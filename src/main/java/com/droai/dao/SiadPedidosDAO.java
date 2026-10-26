package com.droai.dao;

import com.droai.config.DatabaseConfig;
import com.droai.model.AuditoriaPedidoRenglonRow;
import com.droai.model.AuditoriaPedidoRow;
import com.droai.model.SiadDiagnosticoDTO;
import com.droai.model.SiadDiagnosticoDTO.LoteFEFODTO;
import com.droai.model.SiadDiagnosticoDTO.RenglonDiagnosticoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;

/**
 * DAO especializado en la gestión, diagnóstico y resolución de pedidos en SIAD:
 * 1. Monitoreo de pedidos en cola de error (rechazados por SIAD).
 * 2. Diagnóstico automático de stock y lotes contra Profit Plus.
 * 3. Algoritmo FEFO para reasignación de lotes disponibles.
 * 4. Reposición de existencia física en Profit y reprocesamiento en 1-Clic.
 */
public class SiadPedidosDAO {

    private static final Logger logger = LoggerFactory.getLogger(SiadPedidosDAO.class);

    private static final String[] CANDIDATOS_BASE_SIAD = new String[]{
            "\\\\10.30.70.61\\sysvol2\\ISB SISTEMAS\\SIAD\\PEDIDOS",
            "\\\\SRVDB0101\\sysvol2\\ISB SISTEMAS\\SIAD\\PEDIDOS",
            "Y:\\ISB SISTEMAS\\SIAD\\PEDIDOS"
    };

    /**
     * Localiza la carpeta base de PEDIDOS en SIAD.
     */
    public static File getCarpetaSiadBase() {
        for (String ruta : CANDIDATOS_BASE_SIAD) {
            File dir = new File(ruta);
            if (dir.exists() && dir.isDirectory()) {
                return dir;
            }
        }
        return null;
    }

    /**
     * Localiza la carpeta de pedidos con error.
     */
    public static File getCarpetaSiadError() {
        File base = getCarpetaSiadBase();
        if (base != null) {
            File err = new File(base, "error");
            if (err.exists() && err.isDirectory()) {
                return err;
            }
        }
        return null;
    }

    /**
     * Localiza la carpeta activa de procesados.
     */
    public static File getCarpetaSiadProcesados() {
        File base = getCarpetaSiadBase();
        if (base != null) {
            File p1 = new File(base, "PROCESADOS");
            if (p1.exists() && p1.isDirectory()) return p1;
            File p2 = new File(base, "processed");
            if (p2.exists() && p2.isDirectory()) return p2;
        }
        return null;
    }

    /**
     * Localiza la carpeta de cola activa donde SIAD toma los pedidos (PEDIDOS).
     */
    public static File getCarpetaSiadCola() {
        return getCarpetaSiadBase();
    }

    public static class PedidoUbicacion {
        public File archivo;
        public String estado; // "ERROR", "COLA", "PROCESADO"
        public String mensajeError;

        public PedidoUbicacion(File archivo, String estado, String mensajeError) {
            this.archivo = archivo;
            this.estado = estado;
            this.mensajeError = mensajeError;
        }
    }

    /**
     * Busca directamente por nombre un pedido específico en error, cola o procesados (sin recorrer miles de archivos).
     * Ideal para conexiones VPN de baja latencia.
     */
    public PedidoUbicacion buscarPedidoDirecto(String pedId) {
        String cleanId = pedId.replaceAll("[^0-9]", "");
        if (cleanId.isEmpty()) return null;

        // 1. Probar en error
        File dirError = getCarpetaSiadError();
        if (dirError != null && dirError.exists()) {
            File f = new File(dirError, "pedido" + cleanId + ".txt");
            if (!f.exists()) f = new File(dirError, "PEDIDO" + cleanId + ".txt");
            if (f.exists()) {
                return new PedidoUbicacion(f, "ERROR", leerMensajeError(f));
            }
        }

        // 2. Probar en cola activa
        File dirCola = getCarpetaSiadCola();
        if (dirCola != null && dirCola.exists()) {
            File f = new File(dirCola, "pedido" + cleanId + ".txt");
            if (!f.exists()) f = new File(dirCola, "PEDIDO" + cleanId + ".txt");
            if (f.exists()) {
                return new PedidoUbicacion(f, "COLA", "Pedido en cola de espera de SIAD (listo para procesar)");
            }
        }

        // 3. Probar en procesados
        File dirProc = getCarpetaSiadProcesados();
        if (dirProc != null && dirProc.exists()) {
            File f = new File(dirProc, "pedido" + cleanId + ".txt");
            if (!f.exists()) f = new File(dirProc, "PEDIDO" + cleanId + ".txt");
            if (f.exists()) {
                return new PedidoUbicacion(f, "PROCESADO", "Pedido ya procesado e importado previamente");
            }
        }

        return null;
    }

    /**
     * Rastrea integralmente un pedido web en SIAD (Error, Cola, Procesados) y en Profit Plus (Notas, Facturas).
     * Ejecuta en milisegundos mediante búsquedas directas sin barridos masivos de red.
     */
    public com.droai.model.RastreoPedidoDTO rastrearPedidoCompleto(String pedId) {
        if (pedId == null || pedId.trim().isEmpty()) return null;
        String cleanId = pedId.replaceAll("[^0-9]", "");
        if (cleanId.isEmpty()) cleanId = pedId.trim();

        com.droai.model.RastreoPedidoDTO rastreo = new com.droai.model.RastreoPedidoDTO();
        rastreo.setPedidoId(cleanId);

        // 1. Ubicación en carpetas SIAD
        PedidoUbicacion ubi = buscarPedidoDirecto(cleanId);
        if (ubi != null && ubi.archivo != null) {
            rastreo.setUbicacionArchivo(ubi.estado);
            rastreo.setArchivoTxt(ubi.archivo);
            rastreo.setMensajeErrorSiad(ubi.mensajeError);
            if ("ERROR".equals(ubi.estado)) {
                File err1 = new File(ubi.archivo.getAbsolutePath() + ".error.txt");
                if (err1.exists()) rastreo.setArchivoError(err1);
                else {
                    File err2 = new File(ubi.archivo.getParentFile(), ubi.archivo.getName().replace(".txt", ".error.txt"));
                    if (err2.exists()) rastreo.setArchivoError(err2);
                }
            }

            // Diagnosticar renglones del archivo TXT
            SiadDiagnosticoDTO diag = diagnosticarPedido(ubi.archivo);
            rastreo.setDiagnostico(diag);
            rastreo.setCoCli(diag.getCoCli());
            rastreo.setCliDes(diag.getCliDes());
            rastreo.setFechaEmisionTxt(diag.getFechaEmision());

            // Extraer montos y unidades del archivo TXT
            double undTotal = 0.0;
            if (diag.getRenglones() != null) {
                rastreo.setTotalRenglonesTxt(diag.getRenglones().size());
                for (var r : diag.getRenglones()) {
                    undTotal += r.getCantSolicitada();
                }
            }
            rastreo.setTotalUnidadesTxt(undTotal);
        } else {
            rastreo.setUbicacionArchivo("NO_ENCONTRADO");
        }

        // 2. Consultar en Profit Plus si generó Nota de Entrega o Pedido
        String sqlNota = """
            SELECT TOP 1 
                n.doc_num, 
                n.fec_emis, 
                n.total_neto, 
                n.anulado, 
                n.status,
                RTRIM(n.co_cli) AS co_cli, 
                RTRIM(ISNULL(c.cli_des, '')) AS cli_des
            FROM saNotaEntregaVenta n
            LEFT JOIN saCliente c ON n.co_cli = c.co_cli
            WHERE RTRIM(ISNULL(n.campo1, '')) = ? 
               OR n.descrip LIKE ? 
               OR RTRIM(n.doc_num) = ?
            ORDER BY n.fec_emis DESC
            """;

        String sqlFacturas = """
            SELECT DISTINCT RTRIM(f.doc_num) AS fac_num
            FROM saFacturaVentaReng fr
            INNER JOIN saFacturaVenta f ON fr.doc_num = f.doc_num
            INNER JOIN saNotaEntregaVentaReng nr ON fr.rowguid_doc = nr.rowguid
            WHERE nr.doc_num = ? AND f.anulado = 0 AND fr.tipo_doc = 'NENT'
            """;

        String sqlPedido = """
            SELECT TOP 1 p.doc_num
            FROM saPedidoVenta p
            WHERE RTRIM(ISNULL(p.campo1, '')) = ? 
               OR p.descrip LIKE ? 
               OR RTRIM(p.doc_num) = ?
            ORDER BY p.fec_emis DESC
            """;

        try (Connection conn = DatabaseConfig.getConnection()) {
            // Nota de Entrega
            try (PreparedStatement ps = conn.prepareStatement(sqlNota)) {
                ps.setString(1, cleanId);
                ps.setString(2, "%" + cleanId + "%");
                ps.setString(3, cleanId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        rastreo.setExisteEnProfit(true);
                        String docNum = rs.getString("doc_num").trim();
                        rastreo.setDocNumNota(docNum);
                        rastreo.setFecEmisNota(rs.getTimestamp("fec_emis"));
                        rastreo.setTotalNetoNota(rs.getDouble("total_neto"));
                        rastreo.setAnuladaNota(rs.getBoolean("anulado"));
                        rastreo.setStatusNota(rs.getString("status"));
                        if (rastreo.getCoCli() == null || rastreo.getCoCli().isEmpty()) {
                            rastreo.setCoCli(rs.getString("co_cli"));
                            rastreo.setCliDes(rs.getString("cli_des"));
                        }

                        // Facturas
                        List<String> facts = new ArrayList<>();
                        try (PreparedStatement psF = conn.prepareStatement(sqlFacturas)) {
                            psF.setString(1, docNum);
                            try (ResultSet rsF = psF.executeQuery()) {
                                while (rsF.next()) {
                                    facts.add(rsF.getString("fac_num").trim());
                                }
                            }
                        }
                        rastreo.setFacturasProfit(String.join(", ", facts));
                    }
                }
            }

            // Pedido de Venta
            try (PreparedStatement ps = conn.prepareStatement(sqlPedido)) {
                ps.setString(1, cleanId);
                ps.setString(2, "%" + cleanId + "%");
                ps.setString(3, cleanId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        rastreo.setDocNumPedidoProfit(rs.getString("doc_num").trim());
                    }
                }
            }

        } catch (SQLException e) {
            logger.error("Error consultando documentos en Profit Plus para pedido {}", cleanId, e);
        }

        // 3. Sintetizar Estado y Diagnóstico
        if ("ERROR".equals(rastreo.getUbicacionArchivo())) {
            rastreo.setEstadoGeneral("REBOTADO_SIAD");
            rastreo.setResumenDiagnostico("🚨 REBOTADO POR SIAD: " + (rastreo.getMensajeErrorSiad() != null ? rastreo.getMensajeErrorSiad() : "Fallo de validación de stock o lotes"));
        } else if ("COLA".equals(rastreo.getUbicacionArchivo())) {
            rastreo.setEstadoGeneral("EN_COLA_SIAD");
            rastreo.setResumenDiagnostico("⏳ EN COLA ACTIVA: El archivo está en la cola esperando que el servicio SIAD lo procese e importe.");
        } else if (rastreo.isExisteEnProfit()) {
            rastreo.setEstadoGeneral("PROCESADO_PROFIT");
            String docInfo = "Nota de Entrega N° " + rastreo.getDocNumNota();
            if (rastreo.getFacturasProfit() != null && !rastreo.getFacturasProfit().isEmpty()) {
                docInfo += " (Factura(s): " + rastreo.getFacturasProfit() + ")";
            } else {
                docInfo += " (Pendiente por facturar)";
            }
            rastreo.setResumenDiagnostico("🟢 PROCESADO EN PROFIT: Generó " + docInfo);
        } else if ("PROCESADO".equals(rastreo.getUbicacionArchivo())) {
            rastreo.setEstadoGeneral("PROCESADO_SIAD");
            rastreo.setResumenDiagnostico("🟢 PROCESADO POR SIAD: Archivo marcado como procesado.");
        } else {
            rastreo.setEstadoGeneral("NO_ENCONTRADO");
            rastreo.setResumenDiagnostico("❓ NO ENCONTRADO: No se encontró archivo TXT en carpetas de SIAD ni registro en Profit Plus para el N° " + cleanId);
        }

        return rastreo;
    }

    /**
     * Cuenta cuántos pedidos están actualmente retenidos en error.
     */
    public static int contarPedidosEnError() {
        File dirError = getCarpetaSiadError();
        if (dirError == null || !dirError.exists()) return 0;

        File[] files = dirError.listFiles((dir, name) -> {
            String n = name.toLowerCase();
            return n.startsWith("pedido") && n.endsWith(".txt") && !n.endsWith(".error.txt") && !n.endsWith(".bak");
        });
        return files != null ? files.length : 0;
    }

    /**
     * Lista todos los pedidos que se encuentran en la carpeta de error de SIAD.
     */
    public List<AuditoriaPedidoRow> listarPedidosEnError(LocalDate desde, LocalDate hasta) {
        List<AuditoriaPedidoRow> list = new ArrayList<>();
        File dirError = getCarpetaSiadError();
        if (dirError == null || !dirError.exists()) return list;

        File[] files = dirError.listFiles((dir, name) -> {
            String n = name.toLowerCase();
            return n.startsWith("pedido") && n.endsWith(".txt") && !n.endsWith(".error.txt") && !n.endsWith(".bak");
        });
        if (files == null) return list;

        for (File f : files) {
            try {
                AuditoriaPedidoRow row = parsearPedidoError(f);
                if (row != null) {
                    // Filtrar por fecha si aplica
                    if (row.getFecEmis() != null && desde != null && hasta != null) {
                        LocalDate fEmis = row.getFecEmis().toLocalDateTime().toLocalDate();
                        if (fEmis.isBefore(desde) || fEmis.isAfter(hasta)) {
                            // Si está en error activo, incluirlo de todas formas para no ocultar problemas pendientes
                        }
                    }
                    list.add(row);
                }
            } catch (Exception ex) {
                logger.warn("Error parseando pedido en error {}", f.getName(), ex);
            }
        }
        return list;
    }

    /**
     * Lee un archivo de pedido en error y construye el objeto de auditoría.
     */
    private AuditoriaPedidoRow parsearPedidoError(File f) {
        String errorMsg = leerMensajeError(f);

        AuditoriaPedidoRow row = new AuditoriaPedidoRow();
        String pedId = f.getName().toLowerCase().replace("pedido", "").replace(".txt", "").trim();
        row.setPedidoWeb(pedId);
        row.setNotaEntrega("❌ REBOTADO SIAD");
        row.setDocNumOriginal("");
        row.setFacturas("—");
        row.setEstado("ERROR_SIAD");
        row.setDiagnostico("🚨 REBOTADO EN SIAD: " + errorMsg);

        List<AuditoriaPedidoRenglonRow> renglones = new ArrayList<>();
        double montoNeto = 0.0;
        double undWeb = 0.0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.ISO_8859_1))) {
            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                String[] parts = line.split("\\|");
                if (lineNum == 1) {
                    // Cabecera
                    if (parts.length > 1) row.setCoCli(parts[1].trim());
                    if (parts.length > 2) {
                        try {
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            java.util.Date d = sdf.parse(parts[2].trim());
                            row.setFecEmis(new Timestamp(d.getTime()));
                        } catch (Exception ignored) {}
                    }
                    if (parts.length > 10) row.setCliDes(parts[10].trim());
                    if (parts.length > 18) {
                        try {
                            montoNeto = Double.parseDouble(parts[18].trim().replace(",", "."));
                        } catch (Exception ignored) {}
                    }
                    if (parts.length > 25) row.setCoVen(parts[25].trim());
                    if (parts.length > 30) row.setVenDes(parts[30].trim());
                } else {
                    // Renglón de artículo
                    if (parts.length > 5) {
                        String coArt = parts.length > 2 ? parts[2].trim() : "";
                        String desArt = parts.length > 3 ? parts[3].trim() : "";
                        double cant = 0.0;
                        double precio = 0.0;
                        double subtotal = 0.0;
                        try {
                            cant = parts.length > 4 ? Double.parseDouble(parts[4].trim().replace(",", ".")) : 0.0;
                        } catch (Exception ignored) {}
                        try {
                            precio = parts.length > 5 ? Double.parseDouble(parts[5].trim().replace(",", ".")) : 0.0;
                        } catch (Exception ignored) {}
                        try {
                            subtotal = parts.length > 17 ? Double.parseDouble(parts[17].trim().replace(",", ".")) : (cant * precio);
                        } catch (Exception ignored) {}
                        String lote = parts.length > 24 ? parts[24].trim() : "";
                        String estadoDesp = parts.length > 48 ? parts[48].trim() : "";

                        if (!coArt.isEmpty()) {
                            undWeb += cant;
                            AuditoriaPedidoRenglonRow reng = new AuditoriaPedidoRenglonRow();
                            reng.setRengNum(lineNum - 1);
                            reng.setCoArt(coArt);
                            reng.setArtDes(desArt);
                            reng.setCantWeb(cant);
                            reng.setCantNota(0.0);
                            reng.setCantFact(0.0);
                            reng.setCantFaltante(cant);
                            reng.setPrecVta(precio);
                            reng.setRengNeto(subtotal);
                            reng.setNroLote(lote);

                            if (errorMsg != null && errorMsg.contains(coArt)) {
                                reng.setEstado("ERROR_LOTE_STOCK");
                            } else if ("NO FACTURADO".equalsIgnoreCase(estadoDesp)) {
                                reng.setEstado("NO_FACTURADO");
                            } else {
                                reng.setEstado("PENDIENTE_IMPORTAR");
                            }
                            renglones.add(reng);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            logger.warn("Error leyendo contenido de pedido en error {}", f.getName(), ex);
        }

        row.setRenglones(renglones);
        row.setTotalRengWeb(renglones.size());
        row.setTotalUndWeb(undWeb);
        row.setTotalRengNota(0);
        row.setTotalUndNota(0.0);
        row.setTotalRengFact(0);
        row.setTotalUndFact(0.0);
        row.setTotalRengFaltantes(renglones.size());
        row.setTotalUndFaltantes(undWeb);
        row.setTotalMontoNota(montoNeto);
        row.setTotalMontoFact(0.0);
        row.setTotalMontoPend(montoNeto);

        return row;
    }

    /**
     * Extrae el texto explicativo de un error emitido por SIAD para este archivo.
     */
    public static String leerMensajeError(File f) {
        File errFile1 = new File(f.getAbsolutePath() + ".error.txt");
        if (errFile1.exists()) {
            return leerArchivoTexto(errFile1);
        }
        File errFile2 = new File(f.getParentFile(), f.getName().replace(".txt", ".error.txt"));
        if (errFile2.exists()) {
            return leerArchivoTexto(errFile2);
        }
        return "Error en validación de stock o datos en SIAD";
    }

    private static String leerArchivoTexto(File f) {
        try {
            return new String(Files.readAllBytes(f.toPath()), StandardCharsets.ISO_8859_1).trim();
        } catch (Exception e) {
            return "No se pudo leer el archivo de error";
        }
    }

    /**
     * Realiza un diagnóstico exhaustivo de un pedido analizando renglón por renglón
     * contra el stock físico en saStockAlmacen y la disponibilidad en saLoteEntrada.
     */
    public SiadDiagnosticoDTO diagnosticarPedido(File archivoTxt) {
        SiadDiagnosticoDTO diag = new SiadDiagnosticoDTO();
        diag.setArchivoTxt(archivoTxt);
        File errFile = new File(archivoTxt.getAbsolutePath() + ".error.txt");
        if (errFile.exists()) diag.setArchivoError(errFile);
        diag.setErrorOriginal(leerMensajeError(archivoTxt));

        List<RenglonDiagnosticoDTO> listaRenglones = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(archivoTxt), StandardCharsets.ISO_8859_1))) {
            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                String[] parts = line.split("\\|");
                if (lineNum == 1) {
                    if (parts.length > 0) diag.setPedidoId(parts[0].trim());
                    if (parts.length > 1) diag.setCoCli(parts[1].trim());
                    if (parts.length > 2) diag.setFechaEmision(parts[2].trim());
                    if (parts.length > 10) diag.setCliDes(parts[10].trim());
                } else {
                    if (parts.length > 5) {
                        String coArt = parts.length > 2 ? parts[2].trim() : "";
                        String desArt = parts.length > 3 ? parts[3].trim() : "";
                        double cant = 0.0;
                        try {
                            cant = parts.length > 4 ? Double.parseDouble(parts[4].trim().replace(",", ".")) : 0.0;
                        } catch (Exception ignored) {}
                        String lote = parts.length > 24 ? parts[24].trim() : "";
                        String estadoDesp = parts.length > 48 ? parts[48].trim() : "";

                        // Si está marcado como NO FACTURADO, no provoca error en SIAD
                        if ("NO FACTURADO".equalsIgnoreCase(estadoDesp)) {
                            continue;
                        }

                        if (!coArt.isEmpty()) {
                            RenglonDiagnosticoDTO reng = new RenglonDiagnosticoDTO();
                            reng.setRengNum(lineNum - 1);
                            reng.setCoArt(coArt);
                            reng.setArtDes(desArt);
                            reng.setCantSolicitada(cant);
                            reng.setLoteSolicitado(lote);

                            // Consultar stock en Profit Plus
                            evaluarStockYLotes(reng);

                            listaRenglones.add(reng);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            logger.error("Error al diagnosticar archivo {}", archivoTxt.getName(), ex);
        }

        diag.setRenglones(listaRenglones);
        return diag;
    }

    /**
     * Consulta el stock real de almacén y lote en Profit Plus para un renglón.
     */
    private void evaluarStockYLotes(RenglonDiagnosticoDTO reng) {
        String sqlStock = "SELECT stock FROM saStockAlmacen WHERE RTRIM(co_art) = ? AND RTRIM(co_alma) = '01' AND RTRIM(tipo) = 'ACT'";
        String sqlLote = "SELECT ISNULL(SUM(stock_actual), 0) FROM saLoteEntrada WHERE RTRIM(co_art) = ? AND RTRIM(co_alma) = '01' AND RTRIM(numero_lote) = ?";
        String sqlFefo = """
            SELECT RTRIM(numero_lote) AS numero_lote,
                   stock_actual,
                   CONVERT(VARCHAR(10), fecha_expiracion, 120) AS fecha_venc
            FROM saLoteEntrada
            WHERE RTRIM(co_art) = ? AND RTRIM(co_alma) = '01' AND stock_actual > 0
            ORDER BY fecha_expiracion ASC, fe_us_in ASC
            """;

        try (Connection conn = DatabaseConfig.getConnection()) {
            // 1. Stock en almacén 01
            double stockAlma = 0.0;
            try (PreparedStatement ps = conn.prepareStatement(sqlStock)) {
                ps.setString(1, reng.getCoArt().trim());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) stockAlma = rs.getDouble("stock");
                }
            }
            reng.setStockAlmacen(stockAlma);

            // 2. Stock en lote solicitado
            double stockLote = 0.0;
            try (PreparedStatement ps = conn.prepareStatement(sqlLote)) {
                ps.setString(1, reng.getCoArt().trim());
                ps.setString(2, reng.getLoteSolicitado().trim());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) stockLote = rs.getDouble(1);
                }
            }
            reng.setStockLoteSolicitado(stockLote);

            // 3. Buscar lotes disponibles en FEFO
            List<LoteFEFODTO> fefo = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(sqlFefo)) {
                ps.setString(1, reng.getCoArt().trim());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        fefo.add(new LoteFEFODTO(
                                rs.getString("numero_lote"),
                                rs.getDouble("stock_actual"),
                                rs.getString("fecha_venc")
                        ));
                    }
                }
            }
            reng.setLotesDisponibles(fefo);

            // 4. Evaluar condición de error
            if (stockAlma < reng.getCantSolicitada()) {
                reng.setTieneError(true);
                reng.setTipoError("STOCK_INSUFICIENTE");
                reng.setMensajeError("Stock físico insuficiente en almacén (Disp: " + stockAlma + ", Sol: " + reng.getCantSolicitada() + ")");
            } else if (stockLote < reng.getCantSolicitada()) {
                reng.setTieneError(true);
                if (!fefo.isEmpty() && fefo.get(0).getCantDisponible() >= reng.getCantSolicitada()) {
                    reng.setTipoError("LOTE_AGOTADO_HAY_FEFO");
                    reng.setMensajeError("Lote " + reng.getLoteSolicitado() + " agotado. Lote FEFO " + fefo.get(0).getNumeroLote() + " disponible con " + fefo.get(0).getCantDisponible() + " uds.");
                } else {
                    reng.setTipoError("LOTE_INSUFICIENTE");
                    reng.setMensajeError("Lote " + reng.getLoteSolicitado() + " insuficiente (Disp: " + stockLote + ", Sol: " + reng.getCantSolicitada() + ")");
                }
            } else {
                reng.setTieneError(false);
                reng.setTipoError("OK");
                reng.setMensajeError("Existencia y lote correctos");
            }

        } catch (SQLException ex) {
            logger.error("Error al evaluar stock/lote para {}", reng.getCoArt(), ex);
            reng.setTieneError(true);
            reng.setTipoError("ERROR_SQL");
            reng.setMensajeError("Error consultando Profit Plus: " + ex.getMessage());
        }
    }

    /**
     * Reasigna el lote de un artículo directamente en el archivo TXT del pedido.
     */
    public boolean reasignarLoteEnPedidoTxt(File archivoTxt, String coArt, String loteNuevo) {
        if (archivoTxt == null || !archivoTxt.exists()) return false;

        List<String> lineas = new ArrayList<>();
        boolean modificado = false;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(archivoTxt), StandardCharsets.ISO_8859_1))) {
            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (lineNum > 1) {
                    String[] parts = line.split("\\|", -1);
                    if (parts.length > 24 && parts[2].trim().equalsIgnoreCase(coArt.trim())) {
                        parts[24] = loteNuevo.trim();
                        line = String.join("|", parts);
                        modificado = true;
                    }
                }
                lineas.add(line);
            }
        } catch (Exception ex) {
            logger.error("Error leyendo {} para reasignar lote", archivoTxt.getName(), ex);
            return false;
        }

        if (modificado) {
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(archivoTxt), StandardCharsets.ISO_8859_1))) {
                for (String l : lineas) {
                    writer.write(l);
                    writer.newLine();
                }
                logger.info("Lote reasignado exitosamente en {} para art {} -> nuevo lote: {}", archivoTxt.getName(), coArt, loteNuevo);
                return true;
            } catch (Exception ex) {
                logger.error("Error guardando {} tras reasignar lote", archivoTxt.getName(), ex);
                return false;
            }
        }
        return false;
    }

    /**
     * Nivelación o reposición segura de stock físico y lote en Profit Plus
     * (cuando el almacén confirma la existencia física del producto y lote).
     */
    public boolean reponerStockLoteProfit(String coArt, String coAlma, String nroLote, String fechaVenc, double cantidad, String usuario) throws SQLException {
        String coAlmaFinal = (coAlma == null || coAlma.trim().isEmpty()) ? "01" : coAlma.trim();
        String userFinal = (usuario == null || usuario.trim().isEmpty()) ? "DROAI" : usuario.trim();

        String sqlCheckAlma = "SELECT COUNT(*) FROM saStockAlmacen WHERE RTRIM(co_art) = ? AND RTRIM(co_alma) = ? AND RTRIM(tipo) = 'ACT'";
        String sqlUpdAlma = "UPDATE saStockAlmacen SET stock = stock + ? WHERE RTRIM(co_art) = ? AND RTRIM(co_alma) = ? AND RTRIM(tipo) = 'ACT'";
        String sqlInsAlma = "INSERT INTO saStockAlmacen (co_alma, co_art, tipo, stock, stock_des) VALUES (?, ?, 'ACT', ?, 0)";

        String sqlCheckLote = "SELECT TOP 1 rowguid FROM saLoteEntrada WHERE RTRIM(co_art) = ? AND RTRIM(co_alma) = ? AND RTRIM(numero_lote) = ? AND stock_actual >= 0";
        String sqlUpdLote = "UPDATE saLoteEntrada SET cantidad = cantidad + ?, stock_actual = stock_actual + ?, fe_us_mo = GETDATE(), co_us_mo = ? WHERE rowguid = ?";
        String sqlInsLote = """
            INSERT INTO saLoteEntrada (
                rowguid_reng, reng_num, tipo_doc, co_art, co_alma, numero_lote,
                fecha_inicio, fecha_expiracion, cantidad, stock_actual, precio,
                co_us_in, co_sucu_in, fe_us_in, co_us_mo, co_sucu_mo, fe_us_mo,
                revisado, trasnfe, rowguid
            ) VALUES (
                NEWID(), 1, 'AJST', ?, ?, ?,
                GETDATE(), ?, ?, ?, 0,
                ?, '01', GETDATE(), ?, '01', GETDATE(),
                NULL, NULL, NEWID()
            )
            """;

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Nivelar saStockAlmacen
                boolean existeAlma = false;
                try (PreparedStatement ps = conn.prepareStatement(sqlCheckAlma)) {
                    ps.setString(1, coArt.trim());
                    ps.setString(2, coAlmaFinal);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) existeAlma = rs.getInt(1) > 0;
                    }
                }

                if (existeAlma) {
                    try (PreparedStatement ps = conn.prepareStatement(sqlUpdAlma)) {
                        ps.setDouble(1, cantidad);
                        ps.setString(2, coArt.trim());
                        ps.setString(3, coAlmaFinal);
                        ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = conn.prepareStatement(sqlInsAlma)) {
                        ps.setString(1, coAlmaFinal);
                        ps.setString(2, coArt.trim());
                        ps.setDouble(3, cantidad);
                        ps.executeUpdate();
                    }
                }

                // 2. Nivelar saLoteEntrada
                String rowguidExistente = null;
                try (PreparedStatement ps = conn.prepareStatement(sqlCheckLote)) {
                    ps.setString(1, coArt.trim());
                    ps.setString(2, coAlmaFinal);
                    ps.setString(3, nroLote.trim());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) rowguidExistente = rs.getString("rowguid");
                    }
                }

                if (rowguidExistente != null) {
                    try (PreparedStatement ps = conn.prepareStatement(sqlUpdLote)) {
                        ps.setDouble(1, cantidad);
                        ps.setDouble(2, cantidad);
                        ps.setString(3, userFinal);
                        ps.setString(4, rowguidExistente);
                        ps.executeUpdate();
                    }
                } else {
                    String fVencFinal = (fechaVenc != null && !fechaVenc.trim().isEmpty()) ? fechaVenc.trim() : "2028-12-31";
                    try (PreparedStatement ps = conn.prepareStatement(sqlInsLote)) {
                        ps.setString(1, coArt.trim());
                        ps.setString(2, coAlmaFinal);
                        ps.setString(3, nroLote.trim());
                        ps.setString(4, fVencFinal);
                        ps.setDouble(5, cantidad);
                        ps.setDouble(6, cantidad);
                        ps.setString(7, userFinal);
                        ps.setString(8, userFinal);
                        ps.executeUpdate();
                    }
                }

                conn.commit();
                logger.info("Stock repuesto en Profit exitosamente: Art={}, Lote={}, Cant={}", coArt, nroLote, cantidad);
                return true;
            } catch (SQLException ex) {
                conn.rollback();
                logger.error("Error al reponer stock en Profit Plus (ROLLBACK)", ex);
                throw ex;
            }
        }
    }

    /**
     * Mueve el pedido corregido a la cola activa de SIAD para su procesamiento inmediato
     * y elimina los rastros de error para permitir la importación limpia.
     */
    public boolean reprocesarPedidoSiad(File archivoTxt) {
        File dirCola = getCarpetaSiadCola();
        if (dirCola == null || !dirCola.exists()) {
            logger.error("No se pudo localizar la carpeta de cola activa de SIAD");
            return false;
        }

        File destino = new File(dirCola, archivoTxt.getName());
        try {
            // Eliminar archivo de error previo si existe
            File err1 = new File(archivoTxt.getAbsolutePath() + ".error.txt");
            if (err1.exists()) err1.delete();

            File err2 = new File(archivoTxt.getParentFile(), archivoTxt.getName().replace(".txt", ".error.txt"));
            if (err2.exists()) err2.delete();

            // Mover el archivo TXT a la cola
            Files.move(archivoTxt.toPath(), destino.toPath(), StandardCopyOption.REPLACE_EXISTING);
            logger.info("Pedido {} movido exitosamente a la cola activa de SIAD: {}", archivoTxt.getName(), destino.getAbsolutePath());
            return true;
        } catch (Exception ex) {
            logger.error("Error al mover {} a la cola de SIAD", archivoTxt.getName(), ex);
            return false;
        }
    }
}

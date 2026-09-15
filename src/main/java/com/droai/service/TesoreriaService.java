package com.droai.service;

import com.droai.dao.TesoreriaDAO;
import com.droai.model.TesoreriaPagoItem;
import com.droai.model.TesoreriaPagoItem.DestinoProfit;
import com.droai.model.TesoreriaPagoItem.EstadoValidacion;

import org.apache.poi.ss.usermodel.*;
import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDate;
import java.util.*;

/**
 * Servicio de procesamiento y análisis para el módulo de Tesorería y Pagos.
 * Analiza el Excel diario, clasifica semánticamente las transacciones entre
 * Órdenes de Pago y Movimientos de Banco, y valida referencias contra Profit Plus.
 */
import org.apache.poi.openxml4j.util.ZipSecureFile;

public class TesoreriaService {

    static {
        // Permitir libros de gran volumen con más de 1000 entradas zip internas (archivos con 150+ hojas)
        try {
            ZipSecureFile.setMaxFileCount(15000);
            ZipSecureFile.setMinInflateRatio(0.0001);
        } catch (Throwable ignored) {}
    }

    private final TesoreriaDAO dao;

    public record ArqueoDiaResult(
            String nombreHoja,
            LocalDate fecha,
            double tasaDia,
            double totalAperturaBs,
            double totalCierreBs,
            List<TesoreriaPagoItem> pagos
    ) {}

    public TesoreriaService() {
        this.dao = new TesoreriaDAO();
    }

    public TesoreriaDAO getDao() {
        return dao;
    }

    /**
     * Obtiene los nombres de todas las hojas operativas (formato DD-MM) de un libro Excel.
     */
    public List<String> obtenerHojasDisponibles(File file) throws Exception {
        List<String> hojas = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(file);
             Workbook wb = WorkbookFactory.create(fis)) {
            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                String name = wb.getSheetName(i).trim();
                if (name.matches("^\\d{2}-\\d{2}.*")) {
                    hojas.add(name);
                }
            }
        }
        return hojas;
    }

    public ArqueoDiaResult procesarArqueoDia(File file, String sheetName) throws Exception {
        return cargarArqueoDia(file, sheetName);
    }

    /**
     * Valida en lote las referencias de la lista de pagos contra Profit Plus (saMovimientoBanco y saOrdenPago).
     */
    public void validarReferenciasContraProfit(List<TesoreriaPagoItem> items) {
        if (items == null || items.isEmpty()) return;
        Set<String> refs = new HashSet<>();
        for (TesoreriaPagoItem it : items) {
            String r = it.getReferencia() != null ? it.getReferencia().trim() : "";
            if (!r.isEmpty()) refs.add(r);
        }
        Map<String, String> existentes = dao.verificarReferenciasExistentes(refs);
        for (TesoreriaPagoItem it : items) {
            String r = it.getReferencia() != null ? it.getReferencia().trim() : "";
            if (!r.isEmpty() && existentes.containsKey(r)) {
                it.setEstadoValidacion(EstadoValidacion.YA_EXISTE);
                it.setMensajeValidacion(existentes.get(r));
            } else {
                it.setEstadoValidacion(EstadoValidacion.NUEVO);
                it.setMensajeValidacion("No encontrada en Profit: Lista para registrar");
            }
        }
    }

    /**
     * Carga y procesa una hoja específica del Excel de tesorería.
     * Lee apertura, cierre, tasa del día y tabla de pagos (ignorando filas fantasma bajo el TOTAL).
     */
    public ArqueoDiaResult cargarArqueoDia(File file, String sheetName) throws Exception {
        List<TesoreriaPagoItem> items = new ArrayList<>();
        LocalDate fecha = LocalDate.now();
        double tasa = 0.0;
        double aperturaBs = 0.0;
        double cierreBs = 0.0;

        try (FileInputStream fis = new FileInputStream(file);
             Workbook wb = WorkbookFactory.create(fis)) {

            Sheet sheet = wb.getSheet(sheetName);
            if (sheet == null) {
                throw new IllegalArgumentException("No se encontró la hoja '" + sheetName + "' en el archivo.");
            }

            // 1. Extraer Tasa de cambio y Fecha
            Row r2 = sheet.getRow(1); // Fila 2 (índice 1)
            if (r2 != null) {
                // Fecha en celda B2 (índice 1) o encabezado
                Cell cFecha = r2.getCell(1);
                fecha = parseLocalDate(cFecha);

                // Tasa en celda D2 (índice 3)
                Cell cTasa = r2.getCell(3);
                tasa = parseDoubleCell(cTasa);
            }

            // Alternativa para tasa si D2 estaba vacía (ver celda I3 / H3)
            if (tasa <= 0) {
                Row r3 = sheet.getRow(2);
                if (r3 != null) {
                    Cell cTasa3 = r3.getCell(8); // Col I (índice 8)
                    tasa = parseDoubleCell(cTasa3);
                }
            }

            // 2. Saldos de Apertura y Cierre
            Row r10 = sheet.getRow(9); // Fila 10: Total Apertura (Col B: índice 1)
            if (r10 != null) {
                aperturaBs = parseDoubleCell(r10.getCell(1));
            }
            Row r20 = sheet.getRow(19); // Fila 20: Total Cierre (Col B: índice 1)
            if (r20 != null) {
                cierreBs = parseDoubleCell(r20.getCell(1));
            }

            // 3. Procesar filas de pagos desde fila 5 (índice 4)
            int lastRowNum = sheet.getLastRowNum();
            int consecutiveEmptyRows = 0;
            for (int r = 4; r <= Math.min(250, lastRowNum); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                // Columna H (índice 7): Proveedor / Concepto
                String desc = getCellString(row.getCell(7)).trim();

                // DETECCIÓN DEL FINAL: Si encontramos la fila "TOTAL" o "TOTAL PAGOS", detenemos la lectura
                if (desc.equalsIgnoreCase("TOTAL") || desc.toUpperCase().startsWith("TOTAL ") || desc.equalsIgnoreCase("TOTAL:")) {
                    break; // Ignorar todas las filas inferiores (filas residuales/fantasmas)
                }

                // Si es fila de comisión bancaria general sin banco, saltar encabezado
                if (desc.equalsIgnoreCase("COMISIONES BANCARIAS") || desc.equalsIgnoreCase("TOTAL COMISIONES BANCARIAS")) {
                    continue;
                }

                // Extraer campos de la transacción
                String ref = getCellString(row.getCell(6)).trim(); // Col G: Ref Banco
                double montoBs = parseDoubleCell(row.getCell(8));   // Col I: Monto Bs
                double montoUSD = parseDoubleCell(row.getCell(9));  // Col J: Equivalente $
                String bancoStr = getCellString(row.getCell(10)).trim().toUpperCase(); // Col K: Banco
                String tipoPago = getCellString(row.getCell(11)).trim().toUpperCase(); // Col L: Tipo Pago

                // Detectar filas vacías consecutivas para cortar antes de filas residuales
                if (desc.isEmpty() && ref.isEmpty() && montoBs <= 0 && montoUSD <= 0) {
                    consecutiveEmptyRows++;
                    if (consecutiveEmptyRows >= 2) {
                        break; // Fin natural de la tabla de pagos
                    }
                    continue;
                }
                consecutiveEmptyRows = 0;

                // Si la referencia o descripción dicen TOTAL, cortar
                if (ref.equalsIgnoreCase("TOTAL") || desc.equalsIgnoreCase("TOTAL") || desc.startsWith("SUBTOTAL")) {
                    break;
                }

                // Calcular monto USD si vino en 0 pero hay tasa
                if (montoUSD <= 0 && tasa > 0 && montoBs > 0) {
                    montoUSD = montoBs / tasa;
                }

                TesoreriaPagoItem item = new TesoreriaPagoItem();
                item.setFilaExcel(r + 1);
                item.setFecha(fecha);
                item.setReferencia(ref);
                item.setDescripcion(desc);
                item.setMontoBs(montoBs);
                item.setMontoUSD(montoUSD);
                item.setBancoNombreOriginal(bancoStr);
                item.setTipoPagoOriginal(tipoPago.isEmpty() ? "TRANSFERENCIA" : tipoPago);

                // Mapear Banco a Código de Cuenta en Profit Plus
                item.setCodCta(resolverCodigoCuenta(bancoStr));

                // Clasificación Semántica Inteligente
                clasificarTransaccion(item);

                items.add(item);
            }
        }

        return new ArqueoDiaResult(sheetName, fecha, tasa, aperturaBs, cierreBs, items);
    }

    /**
     * Resuelve el código de cuenta bancaria de Profit Plus según el texto de banco.
     */
    public String resolverCodigoCuenta(String bancoStr) {
        if (bancoStr == null) return "0134";
        String b = bancoStr.toUpperCase().trim();
        if (b.contains("PROV") || b.contains("BBVA")) return "0108";
        if (b.contains("BANES")) return "0134";
        if (b.contains("BNC") || b.contains("CREDITO")) return "0191";
        if (b.contains("VENEZ")) return "0102";
        if (b.contains("MERC")) return "0105";
        return "0134"; // Default Banesco
    }

    /**
     * Motor de reglas semánticas para sugerir el Destino en Profit (Movimiento de Banco vs Orden de Pago)
     * y la Cuenta Contable de Gasto correspondiente.
     */
    private void clasificarTransaccion(TesoreriaPagoItem item) {
        String d = item.getDescripcion().toUpperCase();

        // 1. Traspasos entre cuentas propias
        if (d.contains("TRANSFERENCIA ENTRE CUENTAS") || d.contains("TRASPASO")) {
            item.setDestinoProfit(DestinoProfit.TRASPASO);
            item.setCodConcepto("000020");
            item.setDescConcepto("TRASPASO ENTRE CUENTAS");
            return;
        }

        // 2. Comisiones bancarias
        if (d.contains("COMISIONES BANCARIAS") || d.startsWith("BANCO BANESCO") || d.startsWith("BANCO PROVINCIAL") || d.equals("BNC")) {
            item.setDestinoProfit(DestinoProfit.MOVIMIENTO_BANCO);
            item.setCodConcepto("000011");
            item.setDescConcepto("COMISIONES BANCARIAS");
            return;
        }

        // 3. Transporte y Taxis (Carla / Oskarina / Personal)
        if (d.contains("TAXI") || d.contains("TRANSPORTE AL PERSONAL")) {
            item.setDestinoProfit(DestinoProfit.MOVIMIENTO_BANCO);
            item.setCodConcepto("000050");
            item.setDescConcepto("GASTOS DE TRANSPORTE DEL PERSONAL");
            return;
        }

        // 4. Comidas, Cenas, Desayunos del personal
        if (d.contains("CENA") || d.contains("DESAYUNO") || d.contains("COMIDA") || d.contains("ALMUERZO")) {
            item.setDestinoProfit(DestinoProfit.MOVIMIENTO_BANCO);
            item.setCodConcepto("000066");
            item.setDescConcepto("COMIDA DEL PERSONAL");
            return;
        }

        // 5. Combustible y Lubricantes para rutas
        if (d.contains("COMBUSTIBLE") || d.contains("GASOLINA") || d.contains("GAS OIL") || d.contains("DIESEL")) {
            item.setDestinoProfit(DestinoProfit.MOVIMIENTO_BANCO);
            item.setCodConcepto("000033");
            item.setDescConcepto("COMBUSTIBLES Y LUBRICANTES");
            return;
        }

        // 6. Peajes
        if (d.contains("PEAJE") || d.contains("PEAJES")) {
            item.setDestinoProfit(DestinoProfit.MOVIMIENTO_BANCO);
            item.setCodConcepto("000034");
            item.setDescConcepto("PEAJE");
            return;
        }

        // 7. Viáticos de choferes en ruta (Mara, Falcón, Plaza, Los Puertos)
        if (d.contains("VIATICO") || d.contains("VIATICOS") || d.contains("RUTA MARA") || d.contains("RUTA FALCON") || d.contains("RUTA PLAZA") || d.contains("RUTA LOS PUERTOS")) {
            item.setDestinoProfit(DestinoProfit.MOVIMIENTO_BANCO);
            item.setCodConcepto("000031");
            item.setDescConcepto("VIATICOS DEL PERSONAL");
            return;
        }

        // 8. Mantenimiento y repuestos de vehículos (Frank / Camionetas)
        if (d.contains("MECANICO") || d.contains("REPUESTO") || d.contains("CAMIONETA") || d.contains("RADIADOR") || d.contains("CRUCETA") || d.contains("CARDAN")) {
            item.setDestinoProfit(DestinoProfit.MOVIMIENTO_BANCO);
            item.setCodConcepto("000017");
            item.setDescConcepto("GASTOS DE VEHICULO");
            return;
        }

        // 9. Recargas celulares
        if (d.contains("RECARGA")) {
            item.setDestinoProfit(DestinoProfit.MOVIMIENTO_BANCO);
            item.setCodConcepto("000064");
            item.setDescConcepto("RECARGA DE SALDO CELULARES");
            return;
        }

        // 10. Proveedores y Laboratorios (Órdenes de Pago formales)
        if (d.contains("FACT") || d.contains("FACTURA") || d.contains("NC") || d.contains("ND")
                || d.contains("VALMOR") || d.contains("RONAVA") || d.contains("CRUMAR")
                || d.contains("CALOX") || d.contains("DOLLDER") || d.contains("MEGALABS")
                || d.contains("KIMICEG") || d.contains("BIOTECH") || d.contains("VINCENTI")
                || d.contains("VICENTI") || d.contains("BEHRENS") || d.contains("BIOMERC")) {
            item.setDestinoProfit(DestinoProfit.ORDEN_PAGO);
            item.setCodConcepto("P00001");
            item.setDescConcepto("PROVEEDOR (ORDEN DE PAGO)");
            return;
        }

        // 11. Alquiler de local
        if (d.contains("ALQUILER")) {
            item.setDestinoProfit(DestinoProfit.ORDEN_PAGO);
            item.setCodConcepto("000008");
            item.setDescConcepto("ALQUILER DE LOCAL");
            return;
        }

        // 12. Impuestos y Parafiscales (SENIAT, IVSS, BANAVIH, FAOV, INCES, PENSION)
        if (d.contains("SENIAT") || d.contains("ISLR")) {
            item.setDestinoProfit(DestinoProfit.ORDEN_PAGO);
            item.setCodConcepto("000058");
            item.setDescConcepto("ANTICIPO I.S.L.R.");
            return;
        }
        if (d.contains("IVSS") || d.contains("S.S.O") || d.contains("SEGURO SOCIAL")) {
            item.setDestinoProfit(DestinoProfit.ORDEN_PAGO);
            item.setCodConcepto("000007");
            item.setDescConcepto("SEGURO SOCIAL OBLIGATORIO S.S.O");
            return;
        }
        if (d.contains("BANAVIH") || d.contains("FAOV")) {
            item.setDestinoProfit(DestinoProfit.ORDEN_PAGO);
            item.setCodConcepto("000046");
            item.setDescConcepto("FONDO DE AHORRO OBLIGATORIO (F.A.O.V)");
            return;
        }

        // 13. Por defecto: Movimiento de Banco - Gastos Varios
        item.setDestinoProfit(DestinoProfit.MOVIMIENTO_BANCO);
        item.setCodConcepto("000018");
        item.setDescConcepto("GASTOS VARIOS");
    }

    /**
     * Valida la lista de pagos contra Profit Plus en tiempo real (Dry-Run / Solo Lectura)
     * para advertir sobre referencias bancarias que ya existen.
     */
    public void validarContraProfit(List<TesoreriaPagoItem> items) {
        if (items == null || items.isEmpty()) return;

        Set<String> referencias = new HashSet<>();
        for (TesoreriaPagoItem it : items) {
            if (it.getReferencia() != null && !it.getReferencia().isBlank()) {
                referencias.add(it.getReferencia().trim());
            }
        }

        Map<String, String> existentes = dao.verificarReferenciasExistentes(referencias);

        for (TesoreriaPagoItem it : items) {
            String ref = it.getReferencia() != null ? it.getReferencia().trim() : "";
            if (existentes.containsKey(ref)) {
                it.setEstadoValidacion(EstadoValidacion.YA_EXISTE);
                it.setMensajeValidacion(existentes.get(ref));
                it.setSeleccionado(false); // Desmarcar por seguridad
            } else {
                it.setEstadoValidacion(EstadoValidacion.NUEVO);
                it.setMensajeValidacion("No registrada previamente en Profit");
            }
        }
    }

    // Métodos auxiliares de parsing POI
    private double parseDoubleCell(Cell cell) {
        if (cell == null) return 0.0;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return cell.getNumericCellValue();
            }
            if (cell.getCellType() == CellType.STRING) {
                String s = cell.getStringCellValue().trim().replace(".", "").replace(",", ".");
                return Double.parseDouble(s);
            }
            if (cell.getCellType() == CellType.FORMULA) {
                CellValue cv = cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator().evaluate(cell);
                if (cv != null && cv.getCellType() == CellType.NUMERIC) {
                    return cv.getNumberValue();
                }
            }
        } catch (Exception ignored) {}
        return 0.0;
    }

    private String getCellString(Cell cell) {
        if (cell == null) return "";
        try {
            if (cell.getCellType() == CellType.STRING) return cell.getStringCellValue();
            if (cell.getCellType() == CellType.NUMERIC) {
                double val = cell.getNumericCellValue();
                if (val == Math.floor(val)) return String.format(Locale.US, "%.0f", val);
                return String.valueOf(val);
            }
            if (cell.getCellType() == CellType.FORMULA) {
                CellValue cv = cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator().evaluate(cell);
                if (cv != null) {
                    if (cv.getCellType() == CellType.STRING) return cv.getStringValue();
                    if (cv.getCellType() == CellType.NUMERIC) return String.format(Locale.US, "%.0f", cv.getNumberValue());
                }
            }
        } catch (Exception ignored) {}
        return "";
    }

    private LocalDate parseLocalDate(Cell cell) {
        if (cell == null) return LocalDate.now();
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                Date d = cell.getDateCellValue();
                return d.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            }
            String s = getCellString(cell).trim();
            if (s.contains("/")) {
                String[] parts = s.split("/");
                if (parts.length == 3) {
                    int day = Integer.parseInt(parts[0]);
                    int month = Integer.parseInt(parts[1]);
                    int year = Integer.parseInt(parts[2]);
                    if (year < 100) year += 2000;
                    return LocalDate.of(year, month, day);
                }
            }
        } catch (Exception ignored) {}
        return LocalDate.now();
    }

    public record ReporteEjecutivoData(
            double totalBs,
            double totalUSD,
            int totalTransacciones,
            int countMovimientosBanco,
            int countOrdenesPago,
            double tasaPonderada,
            Map<String, Double> porBancoBs,
            Map<String, Double> porBancoUSD,
            Map<String, Double> porConceptoBs,
            Map<String, Double> porConceptoUSD,
            Map<LocalDate, Double> porDiaBs,
            Map<LocalDate, Double> porDiaUSD,
            List<TesoreriaDAO.TransaccionEjecutivaInfo> transacciones
    ) {}

    public ReporteEjecutivoData generarReporteEjecutivo(LocalDate desde, LocalDate hasta, String codCta, String tipo) {
        List<TesoreriaDAO.TransaccionEjecutivaInfo> lista = dao.consultarReporteEjecutivo(desde, hasta, codCta, tipo);

        double totalBs = 0;
        double totalUSD = 0;
        int countMB = 0;
        int countOP = 0;

        Map<String, Double> porBancoBs = new LinkedHashMap<>();
        Map<String, Double> porBancoUSD = new LinkedHashMap<>();
        Map<String, Double> porConceptoBs = new LinkedHashMap<>();
        Map<String, Double> porConceptoUSD = new LinkedHashMap<>();
        Map<LocalDate, Double> porDiaBs = new TreeMap<>();
        Map<LocalDate, Double> porDiaUSD = new TreeMap<>();

        for (TesoreriaDAO.TransaccionEjecutivaInfo t : lista) {
            totalBs += t.montoBs();
            totalUSD += t.montoUSD();

            if ("Movimiento de Banco".equalsIgnoreCase(t.tipoOrigen())) countMB++;
            else countOP++;

            String bancoKey = t.bancoDesc() != null && !t.bancoDesc().isBlank() ? t.bancoDesc().trim() : t.codCta();
            porBancoBs.merge(bancoKey, t.montoBs(), Double::sum);
            porBancoUSD.merge(bancoKey, t.montoUSD(), Double::sum);

            String conceptoKey = t.conceptoDesc() != null && !t.conceptoDesc().isBlank() ? t.conceptoDesc().trim() : t.codConcepto();
            porConceptoBs.merge(conceptoKey, t.montoBs(), Double::sum);
            porConceptoUSD.merge(conceptoKey, t.montoUSD(), Double::sum);

            if (t.fecha() != null) {
                porDiaBs.merge(t.fecha(), t.montoBs(), Double::sum);
                porDiaUSD.merge(t.fecha(), t.montoUSD(), Double::sum);
            }
        }

        double tasaPond = totalUSD > 0 ? totalBs / totalUSD : dao.obtenerTasaUSD();

        return new ReporteEjecutivoData(
                totalBs, totalUSD, lista.size(), countMB, countOP, tasaPond,
                porBancoBs, porBancoUSD, porConceptoBs, porConceptoUSD, porDiaBs, porDiaUSD,
                lista
        );
    }

    public void exportarReporteEjecutivoExcel(File file, ReporteEjecutivoData data, LocalDate desde, LocalDate hasta) throws Exception {
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            // Estilos
            Font fontHeader = wb.createFont();
            fontHeader.setBold(true);
            fontHeader.setColor(IndexedColors.WHITE.getIndex());

            CellStyle styleHeader = wb.createCellStyle();
            styleHeader.setFont(fontHeader);
            styleHeader.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            styleHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            styleHeader.setAlignment(HorizontalAlignment.CENTER);

            DataFormat df = wb.createDataFormat();
            CellStyle styleBs = wb.createCellStyle();
            styleBs.setDataFormat(df.getFormat("#,##0.00"));

            CellStyle styleUSD = wb.createCellStyle();
            styleUSD.setDataFormat(df.getFormat("$#,##0.00"));

            // Hoja 1: Transacciones Detalladas
            Sheet sheet1 = wb.createSheet("Detalle Transacciones");
            String[] headers1 = {
                    "N° Doc", "Origen", "Banco / Cuenta", "Fecha", "Referencia",
                    "Beneficiario / Proveedor", "Concepto de Gasto", "Monto (Bs)", "Tasa", "Monto ($)"
            };

            Row headerRow1 = sheet1.createRow(0);
            for (int i = 0; i < headers1.length; i++) {
                Cell c = headerRow1.createCell(i);
                c.setCellValue(headers1[i]);
                c.setCellStyle(styleHeader);
            }

            int rowIdx = 1;
            for (TesoreriaDAO.TransaccionEjecutivaInfo t : data.transacciones()) {
                Row r = sheet1.createRow(rowIdx++);
                r.createCell(0).setCellValue(t.idDoc());
                r.createCell(1).setCellValue(t.tipoOrigen());
                r.createCell(2).setCellValue(t.bancoDesc());
                r.createCell(3).setCellValue(t.fecha() != null ? t.fecha().toString() : "");
                r.createCell(4).setCellValue(t.referencia());
                r.createCell(5).setCellValue(t.beneficiario());
                r.createCell(6).setCellValue(t.codConcepto() + " - " + t.conceptoDesc());

                Cell cBs = r.createCell(7);
                cBs.setCellValue(t.montoBs());
                cBs.setCellStyle(styleBs);

                r.createCell(8).setCellValue(t.tasa());

                Cell cUSD = r.createCell(9);
                cUSD.setCellValue(t.montoUSD());
                cUSD.setCellStyle(styleUSD);
            }

            for (int i = 0; i < headers1.length; i++) {
                sheet1.autoSizeColumn(i);
            }

            // Hoja 2: Resumen por Banco
            Sheet sheet2 = wb.createSheet("Resumen por Banco");
            Row headerRow2 = sheet2.createRow(0);
            String[] headers2 = {"Banco / Cuenta", "Total Egresos (Bs)", "Total Egresos ($)", "% del Total"};
            for (int i = 0; i < headers2.length; i++) {
                Cell c = headerRow2.createCell(i);
                c.setCellValue(headers2[i]);
                c.setCellStyle(styleHeader);
            }

            int rowIdx2 = 1;
            for (Map.Entry<String, Double> e : data.porBancoBs().entrySet()) {
                Row r = sheet2.createRow(rowIdx2++);
                r.createCell(0).setCellValue(e.getKey());

                Cell cBs = r.createCell(1);
                cBs.setCellValue(e.getValue());
                cBs.setCellStyle(styleBs);

                double usd = data.porBancoUSD().getOrDefault(e.getKey(), 0.0);
                Cell cUSD = r.createCell(2);
                cUSD.setCellValue(usd);
                cUSD.setCellStyle(styleUSD);

                double pct = data.totalBs() > 0 ? (e.getValue() / data.totalBs()) * 100.0 : 0;
                r.createCell(3).setCellValue(String.format(Locale.US, "%.1f%%", pct));
            }

            for (int i = 0; i < headers2.length; i++) {
                sheet2.autoSizeColumn(i);
            }

            // Hoja 3: Resumen por Concepto de Gasto
            Sheet sheet3 = wb.createSheet("Resumen por Concepto");
            Row headerRow3 = sheet3.createRow(0);
            String[] headers3 = {"Concepto de Gasto", "Total Egresos (Bs)", "Total Egresos ($)", "% del Total"};
            for (int i = 0; i < headers3.length; i++) {
                Cell c = headerRow3.createCell(i);
                c.setCellValue(headers3[i]);
                c.setCellStyle(styleHeader);
            }

            int rowIdx3 = 1;
            for (Map.Entry<String, Double> e : data.porConceptoBs().entrySet()) {
                Row r = sheet3.createRow(rowIdx3++);
                r.createCell(0).setCellValue(e.getKey());

                Cell cBs = r.createCell(1);
                cBs.setCellValue(e.getValue());
                cBs.setCellStyle(styleBs);

                double usd = data.porConceptoUSD().getOrDefault(e.getKey(), 0.0);
                Cell cUSD = r.createCell(2);
                cUSD.setCellValue(usd);
                cUSD.setCellStyle(styleUSD);

                double pct = data.totalBs() > 0 ? (e.getValue() / data.totalBs()) * 100.0 : 0;
                r.createCell(3).setCellValue(String.format(Locale.US, "%.1f%%", pct));
            }

            for (int i = 0; i < headers3.length; i++) {
                sheet3.autoSizeColumn(i);
            }

            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) {
                wb.write(fos);
            }
        }
    }
}

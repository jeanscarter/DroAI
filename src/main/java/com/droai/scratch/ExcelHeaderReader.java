package com.droai.scratch;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.File;
import java.io.FileInputStream;

public class ExcelHeaderReader {
    public static void main(String[] args) {
        String excelFilePath = "c:\\Users\\jeancarlos.ACTIVA\\Documents\\Projects\\dro-ai\\Ventas Mayo 2.025.xlsx";
        try (FileInputStream fis = new FileInputStream(new File(excelFilePath));
             Workbook workbook = new XSSFWorkbook(fis)) {
            
            Sheet sheet = workbook.getSheet("Matriz");
            if (sheet == null) {
                System.out.println("Sheet 'Matriz' not found. Available sheets: ");
                for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                    System.out.println("- " + workbook.getSheetName(i));
                }
                return;
            }
            
            System.out.println("--- Headers in sheet 'Matriz' ---");
            // Assuming headers are in the first row (index 0) or close to it
            Row headerRow = null;
            for (int i = 0; i < Math.min(10, sheet.getLastRowNum() + 1); i++) {
                Row r = sheet.getRow(i);
                if (r != null && r.getLastCellNum() > 2) { // mostly non-empty row
                    headerRow = r;
                    break;
                }
            }
            
            if (headerRow != null) {
                for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                    Cell cell = headerRow.getCell(i);
                    String header = cell == null ? "NULL" : cell.toString();
                    System.out.println("Column " + i + ": " + header);
                }
            } else {
                System.out.println("Could not find header row.");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

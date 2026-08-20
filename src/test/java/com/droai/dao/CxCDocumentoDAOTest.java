package com.droai.dao;

import com.droai.model.CxCDocumentoRow;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CxCDocumentoDAOTest {

    @Test
    public void testGetDbLabel() {
        assertEquals("DROA_A", CxCDocumentoDAO.getDbLabel(LocalDate.of(2025, 1, 1), LocalDate.now()));
    }

    @Test
    public void testDeterminarAnalista() {
        assertEquals("J-S", CxCDocumentoDAO.determinarAnalista("AB", "ANSONY"));
        assertEquals("D-H", CxCDocumentoDAO.determinarAnalista("CC", "CINTHIA"));
        assertEquals("F-E", CxCDocumentoDAO.determinarAnalista("EC", "EULER"));
    }

    @Test
    public void testFetchDocumentosCxCProductionOnly() {
        CxCDocumentoDAO dao = new CxCDocumentoDAO();
        LocalDate hasta = LocalDate.now();
        LocalDate desde = hasta.minusDays(15);

        List<CxCDocumentoRow> rows = dao.fetchDocumentosCxC(desde, hasta, hasta);
        assertNotNull(rows);

        // Si hay registros, verificar campos grupoCliente, iva, neto, saldo y ordenamiento
        for (int i = 0; i < rows.size(); i++) {
            CxCDocumentoRow r = rows.get(i);
            assertTrue(r.getTasa() > 0, "La tasa debe ser mayor a 0");
            assertNotNull(r.getFactura());
            assertNotNull(r.getCodigoCliente());
            assertNotNull(r.getCliente());
            assertNotNull(r.getGrupoCliente());

            // Validar orden ascendente por Vencimiento y luego Factura
            if (i > 0) {
                CxCDocumentoRow prev = rows.get(i - 1);
                if (prev.getFechaVencimiento() != null && r.getFechaVencimiento() != null) {
                    assertTrue(
                        !r.getFechaVencimiento().isBefore(prev.getFechaVencimiento()),
                        "Las filas deben estar ordenadas por fecha de vencimiento ascendente"
                    );
                    if (r.getFechaVencimiento().isEqual(prev.getFechaVencimiento())) {
                        String factPrev = prev.getFactura() != null ? prev.getFactura() : "";
                        String factCurr = r.getFactura() != null ? r.getFactura() : "";
                        assertTrue(
                            factCurr.compareToIgnoreCase(factPrev) >= 0,
                            "Para misma fecha de vencimiento, debe estar ordenado por factura A-Z"
                        );
                    }
                }
            }
        }
    }
}

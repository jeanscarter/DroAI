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

        // Si hay registros, verificar campos grupoCliente, iva, neto, saldo
        for (CxCDocumentoRow r : rows) {
            assertTrue(r.getTasa() > 0, "La tasa debe ser mayor a 0");
            assertNotNull(r.getFactura());
            assertNotNull(r.getCodigoCliente());
            assertNotNull(r.getCliente());
            // grupoCliente no debe ser null
            assertNotNull(r.getGrupoCliente());
        }
    }
}

package com.droai.service;

import com.droai.model.ComisionRow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ComisionesServiceTest {

    @Test
    @DisplayName("Debe calcular los totales de comisiones correctamente")
    public void testCalcularTotales() {
        ComisionesService service = new ComisionesService();
        List<ComisionRow> rows = new ArrayList<>();

        ComisionRow r1 = new ComisionRow();
        r1.setMontoDocumento(247343.30);
        r1.setMontoCobrado(247343.30);
        r1.setBaseComision(247343.30);
        r1.setMontoComision(3710.15);
        rows.add(r1);

        ComisionRow r2 = new ComisionRow();
        r2.setMontoDocumento(19868.98);
        r2.setMontoCobrado(19868.98);
        r2.setBaseComision(19868.98);
        r2.setMontoComision(298.03);
        rows.add(r2);

        ComisionesService.TotalesComisiones tot = service.calcularTotales(rows);

        assertEquals(2, tot.getTotalRegistros());
        assertEquals(267212.28, tot.getTotalMontoDoc(), 0.01);
        assertEquals(267212.28, tot.getTotalMontoCobrado(), 0.01);
        assertEquals(267212.28, tot.getTotalBaseComision(), 0.01);
        assertEquals(4008.18, tot.getTotalMontoComision(), 0.01);
    }

    @Test
    @DisplayName("Debe consultar comisiones sin duplicar facturas para un vendedor o cliente")
    public void testDeduplicacionComisiones() {
        ComisionesService service = new ComisionesService();
        java.time.LocalDate desde = java.time.LocalDate.of(2026, 9, 1);
        java.time.LocalDate hasta = java.time.LocalDate.of(2026, 9, 20);

        List<ComisionRow> rows = service.consultarComisiones(desde, hasta, "TODOS");
        if (rows != null && !rows.isEmpty()) {
            java.util.Set<String> seen = new java.util.HashSet<>();
            for (ComisionRow r : rows) {
                String key = r.getTipoDoc() + ":" + r.getNumeroDocumento() + ":" + r.getCodigoVendedor();
                org.junit.jupiter.api.Assertions.assertFalse(seen.contains(key),
                        "Documento duplicado encontrado en comisiones: " + key);
                seen.add(key);
            }
        }
    }
}

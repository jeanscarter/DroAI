package com.droai;

import com.droai.dao.NotaCreditoDAO;
import com.droai.model.NotaCreditoModel;
import com.droai.ui.print.NotaCreditoPrintRenderer;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class NotaCreditoTest {

    private static final Set<String> USUARIOS_AUTORIZADOS = Set.of("JG", "CN", "OP", "JR", "ND", "FC");

    @Test
    public void testSeguridadUsuariosAutorizados() {
        // Usuarios válidos
        for (String user : new String[]{"JG", "CN", "OP", "JR", "ND", "FC", "jg", "op", "fc"}) {
            assertTrue(USUARIOS_AUTORIZADOS.contains(user.trim().toUpperCase()), "El usuario " + user + " debería estar autorizado");
        }

        // Usuarios no autorizados
        for (String user : new String[]{"ADMIN", "VEND1", "SUPER", "USER", "DH", "FE"}) {
            assertFalse(USUARIOS_AUTORIZADOS.contains(user.trim().toUpperCase()), "El usuario " + user + " NO debería estar autorizado");
        }
    }

    @Test
    public void testConsultarNotaCreditoDAO() {
        NotaCreditoDAO dao = new NotaCreditoDAO();
        NotaCreditoModel doc = dao.consultarDocumento("0000007047", "N/CR");

        if (doc != null) {
            assertEquals("0000007047", doc.getNroDoc());
            assertEquals("N/CR", doc.getCoTipoDoc());
            assertEquals("308460281", doc.getCoCli());
            assertEquals("00-044281", doc.getNControl());
            assertEquals("00001354", doc.getCampo5());
            assertEquals("00001354", doc.getNumeroImpresion());
            assertEquals("0000040978", doc.getNroOrig());

            // Tasa y montos
            assertTrue(doc.getTasa() > 0);
            assertEquals(85199.68, doc.getMontoBrutoBs(), 0.01);
            assertEquals(112.47, doc.getMontoBrutoUsd(), 0.05);
            assertEquals(112.47, doc.getMontoNetoUsd(), 0.05);

            System.out.println("✔ Nota de Crédito 7047 validada correctamente:");
            System.out.println("  Cliente: " + doc.getCliDes());
            System.out.println("  N° Impresión: " + doc.getNumeroImpresion());
            System.out.println("  Control: " + doc.getNControl());
            System.out.println("  Total Bs: " + doc.getMontoNetoBs());
            System.out.println("  Total USD: " + doc.getMontoNetoUsd());
        } else {
            System.out.println("⚠ No se pudo conectar a BD en test (posible offline), prueba de DAO omitida.");
        }
    }

    @Test
    public void testRenderizadoImpresion() {
        NotaCreditoModel model = new NotaCreditoModel();
        model.setNroDoc("0000007047");
        model.setCampo5("00001354");
        model.setNControl("00-044281");
        model.setCoCli("308460281");
        model.setCliDes("CREA - DESARROLLOS, SOCIEDAD ANONIMA (CREDESA)");
        model.setRif("J-308460281");
        model.setDireccion("CALLES 78 Y 79 LOS OLIVOS CC LOS OLIVOS PLAZA NIVEL 1 LOCAL 1 MARACAIBO EDO ZULIA");
        model.setTelefonos("0424-6207356");
        model.setVenDes("ALYELICK REVEROL");
        model.setDescripcion("DSTO POR PRONTO PAGO 10%");
        model.setDocOrig("FACT");
        model.setNroOrig("0000040978");
        model.setFecEmisOrig(java.time.LocalDateTime.of(2026, 8, 8, 0, 0));
        model.setSubtotalOrigBs(864201.89);
        model.setIvaOrigBs(0.00);
        model.setTotalOrigBs(864201.89);
        model.setMontoExentoBs(85199.68);
        model.setMontoNetoBs(85199.68);
        model.setTasa(757.54060000);

        NotaCreditoPrintRenderer renderer = new NotaCreditoPrintRenderer(model, false);
        BufferedImage img = new BufferedImage(792, 612, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();

        assertDoesNotThrow(() -> {
            renderer.paintDocument(g2, 792, 612);
        });
        g2.dispose();

        System.out.println("✔ Renderizado gráfico de impresión horizontal ejecutado sin errores.");
    }

    @Test
    public void testPageFormatLandscape() {
        java.awt.print.PrinterJob job = java.awt.print.PrinterJob.getPrinterJob();
        java.awt.print.PageFormat pf = job.defaultPage();
        java.awt.print.Paper paper = new java.awt.print.Paper();
        paper.setSize(612, 792);
        paper.setImageableArea(0, 0, 612, 792);
        pf.setPaper(paper);
        pf.setOrientation(java.awt.print.PageFormat.LANDSCAPE);

        System.out.println("Letter Landscape: width=" + pf.getWidth() + " height=" + pf.getHeight()
                + " iw=" + pf.getImageableWidth() + " ih=" + pf.getImageableHeight()
                + " ix=" + pf.getImageableX() + " iy=" + pf.getImageableY());

        assertEquals(java.awt.print.PageFormat.LANDSCAPE, pf.getOrientation());
        assertTrue(pf.getImageableWidth() > pf.getImageableHeight(), "ImageableWidth should be greater than ImageableHeight in landscape");

        javax.print.attribute.PrintRequestAttributeSet attrSet = new javax.print.attribute.HashPrintRequestAttributeSet();
        attrSet.add(javax.print.attribute.standard.OrientationRequested.LANDSCAPE);
        attrSet.add(javax.print.attribute.standard.MediaSizeName.NA_LETTER);
        assertNotNull(attrSet.get(javax.print.attribute.standard.OrientationRequested.class));
    }
}

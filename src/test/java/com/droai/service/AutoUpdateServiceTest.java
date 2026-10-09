package com.droai.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

public class AutoUpdateServiceTest {

    @Test
    @DisplayName("Debe resolver el directorio local de instalación")
    public void testGetLocalProjectDir() {
        File dir = AutoUpdateService.getLocalProjectDir();
        assertNotNull(dir, "El directorio local no debe ser nulo");
        assertTrue(dir.exists(), "El directorio local debe existir");
    }

    @Test
    @DisplayName("En entorno de desarrollo o servidor no debe intentar auto-actualizarse")
    public void testCheckAndUpdateEnServidor() {
        // En la máquina de desarrollo o montada en X:, checkAndUpdate debe retornar false sin error
        boolean triggered = AutoUpdateService.checkAndUpdate(false);
        assertFalse(triggered, "En servidor o dev no debe disparar actualización sobre sí mismo");
    }
}

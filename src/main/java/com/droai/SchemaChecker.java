package com.droai;

import com.droai.dao.CatalogoFiltrosDAO;
import com.droai.dao.CatalogoFiltrosDAO.OptionItem;
import java.util.List;

public class SchemaChecker {
    public static void main(String[] args) {
        System.out.println("=== Testing CatalogoFiltrosDAO ===");
        CatalogoFiltrosDAO dao = new CatalogoFiltrosDAO();

        List<OptionItem> lineas = dao.obtenerLineas();
        System.out.println("Grupos (Líneas) cargados: " + lineas.size());
        if (!lineas.isEmpty()) {
            System.out.println(" Ejemplo primer grupo: " + lineas.get(0));
            List<OptionItem> sublineas = dao.obtenerSubLineas(lineas.get(0).code());
            System.out.println(" SubGrupos del primer grupo: " + sublineas.size());
        }

        List<OptionItem> provs = dao.obtenerProveedores();
        System.out.println("Proveedores cargados: " + provs.size());
        if (!provs.isEmpty()) {
            System.out.println(" Ejemplo primer proveedor: " + provs.get(0));
        }

        List<OptionItem> almacenes = dao.obtenerAlmacenes();
        System.out.println("Almacenes cargados: " + almacenes.size());
        if (!almacenes.isEmpty()) {
            System.out.println(" Ejemplo primer almacén: " + almacenes.get(0));
        }
    }
}




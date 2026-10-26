package com.homeolive.modelo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Catálogo vendible agrupado como categoría → presentaciones. Es lo que consume el formulario de registro
 * para los desplegables en cascada (el producto lo escribe el vendedor).
 */
public record Catalogo(List<Categoria> categorias) {

    public record Categoria(String nombre, List<Opcion> presentaciones) {
    }

    public record Opcion(long id, String etiqueta, BigDecimal precio) {
    }

    public static Catalogo desde(List<Presentacion> presentaciones) {
        Map<String, List<Opcion>> arbol = new LinkedHashMap<>();
        for (Presentacion p : presentaciones) {
            arbol.computeIfAbsent(p.categoria(), k -> new ArrayList<>())
                    .add(new Opcion(p.id(), p.etiqueta(), p.precio()));
        }
        List<Categoria> categorias = new ArrayList<>();
        arbol.forEach((nombre, opciones) -> categorias.add(new Categoria(nombre, List.copyOf(opciones))));
        return new Catalogo(List.copyOf(categorias));
    }
}

package com.homeolive.modelo;

/**
 * Fila de la tabla de categorías: la categoría, cuántas presentaciones activas tiene y cuántos productos
 * distintos se han registrado en ella.
 */
public record CategoriaProductoVista(long id, String nombre, int productos, int presentaciones) {

    public String codigo() {
        return String.format("CAT-%03d", id);
    }

    public boolean tieneProductos() {
        return productos > 0;
    }
}

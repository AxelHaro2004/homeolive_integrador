package com.homeolive.modelo;

/** Categoría de producto (CATEGORIA_PRODUCTO). {@code activa = false} significa eliminada lógicamente. */
public record CategoriaProducto(long id, String nombre, boolean activa) {

    public String codigo() {
        return String.format("CAT-%03d", id);
    }
}

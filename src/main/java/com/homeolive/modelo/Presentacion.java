package com.homeolive.modelo;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Presentación de una categoría (CATEGORIA_PRESENTACION): el tamaño o formato en que se vende (30 ml,
 * 45 gr, 60 cápsulas) con su precio unitario. {@code activa} ya considera también a la categoría.
 */
public record Presentacion(long id, long idCategoria, String categoria, int valor, String unidad,
                           BigDecimal precio, boolean activa) {

    /** Texto como se muestra al vender: «30 ml», «60 cápsulas». */
    public String etiqueta() {
        return valor + " " + unidad;
    }

    public String precioTexto() {
        return String.format(Locale.US, "S/ %.2f", precio);
    }

    public String codigo() {
        return String.format("PRE-%03d", id);
    }
}

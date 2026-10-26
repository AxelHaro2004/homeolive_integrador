package com.homeolive.dao.jdbc;

import org.springframework.jdbc.support.KeyHolder;

import java.util.Map;

/**
 * Lee la clave autogenerada de un INSERT. H2 2.x devuelve también las columnas con valor por defecto
 * (por ejemplo, las fechas), por eso no se usa {@code getKey()}: se busca la columna por su nombre.
 */
final class ClavesGeneradas {

    private ClavesGeneradas() {
    }

    static long id(KeyHolder claves, String columna) {
        Map<String, Object> fila = claves.getKeys();
        if (fila != null) {
            for (Map.Entry<String, Object> e : fila.entrySet()) {
                if (e.getKey().equalsIgnoreCase(columna) && e.getValue() instanceof Number n) {
                    return n.longValue();
                }
            }
        }
        throw new IllegalStateException("La base de datos no devolvió la clave generada «" + columna + "».");
    }
}

package com.homeolive.dao;

/**
 * Registro de productos vendidos. El producto es un nombre libre: se guarda una sola vez (sin importar
 * mayúsculas) y se combina con la presentación elegida (MEDICINA_TALONARIO).
 */
public interface CatalogoDao {

    /** Devuelve el id de la combinación producto + presentación, creándola si todavía no existe. */
    long obtenerOCrearMedicina(String nombreProducto, long idPresentacion);
}

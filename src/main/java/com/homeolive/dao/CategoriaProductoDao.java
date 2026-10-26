package com.homeolive.dao;

import com.homeolive.modelo.CategoriaProducto;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Acceso a datos de CATEGORIA_PRODUCTO. La eliminación es lógica (columna {@code activo}). */
public interface CategoriaProductoDao {

    /** Todas las categorías, activas e inactivas. */
    List<CategoriaProducto> listarTodas();

    Optional<CategoriaProducto> buscarPorId(long id);

    long insertar(String nombre);

    boolean actualizar(long id, String nombre);

    /** Vuelve a activar una categoría eliminada lógicamente, con el nombre indicado. */
    boolean reactivar(long id, String nombre);

    /** Eliminación lógica. */
    boolean desactivar(long id);

    /** Productos distintos registrados en pedidos por categoría (clave: id_categoria). */
    Map<Long, Integer> productosPorCategoria();

    /** Productos distintos registrados en pedidos, en total. */
    int totalProductosAsociados();
}

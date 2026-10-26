package com.homeolive.dao;

import com.homeolive.modelo.Presentacion;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Acceso a datos de CATEGORIA_PRESENTACION. La eliminación es lógica (columna {@code activo}). */
public interface PresentacionDao {

    /** Todas las presentaciones, activas e inactivas, ordenadas por categoría y medida. */
    List<Presentacion> listarTodas();

    Optional<Presentacion> buscarPorId(long id);

    Optional<Presentacion> buscarPorClave(long idCategoria, int valor, String unidad);

    long insertar(long idCategoria, int valor, String unidad, BigDecimal precio);

    boolean actualizar(long id, int valor, String unidad, BigDecimal precio);

    /** Vuelve a activar una presentación eliminada lógicamente, con el precio indicado. */
    boolean reactivar(long id, BigDecimal precio);

    boolean desactivar(long id);

    int desactivarPorCategoria(long idCategoria);

    /** Cantidad de ítems de pedidos que usan la presentación. */
    int contarUsosEnPedidos(long idPresentacion);

    /** Presentaciones activas por categoría (clave: id_categoria). */
    Map<Long, Integer> contarActivasPorCategoria();
}

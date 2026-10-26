package com.homeolive.modelo;

import com.homeolive.dao.CategoriaProductoDao;
import com.homeolive.dao.PresentacionDao;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Reglas de CATEGORIA_PRODUCTO: nombre obligatorio y sin duplicados (ignorando mayúsculas y tildes).
 * Eliminar una categoría es una eliminación lógica: deja de ofrecerse al vender (junto con sus
 * presentaciones), pero los pedidos ya registrados la conservan. Crear una con el nombre de una eliminada
 * la reactiva.
 */
@Component
public class CategoriaProductoModelo {

    public static final int LARGO_MAXIMO_NOMBRE = 100;

    private final CategoriaProductoDao categorias;
    private final PresentacionDao presentaciones;

    public CategoriaProductoModelo(CategoriaProductoDao categorias, PresentacionDao presentaciones) {
        this.categorias = categorias;
        this.presentaciones = presentaciones;
    }

    public List<CategoriaProductoVista> listar(String filtro) {
        Map<Long, Integer> productos = categorias.productosPorCategoria();
        Map<Long, Integer> conPresentaciones = presentaciones.contarActivasPorCategoria();
        String texto = filtro == null ? "" : filtro.trim().toLowerCase(Locale.ROOT);
        return categorias.listarTodas().stream()
                .filter(CategoriaProducto::activa)
                .filter(c -> texto.isEmpty() || c.nombre().toLowerCase(Locale.ROOT).contains(texto))
                .map(c -> new CategoriaProductoVista(c.id(), c.nombre(),
                        productos.getOrDefault(c.id(), 0), conPresentaciones.getOrDefault(c.id(), 0)))
                .toList();
    }

    /** Categoría activa por id (las eliminadas lógicamente no se devuelven). */
    public Optional<CategoriaProducto> buscar(long id) {
        return categorias.buscarPorId(id).filter(CategoriaProducto::activa);
    }

    public int total() {
        return (int) categorias.listarTodas().stream().filter(CategoriaProducto::activa).count();
    }

    public int totalProductos() {
        return categorias.totalProductosAsociados();
    }

    @Transactional
    public CategoriaProducto crear(String nombre) {
        String limpio = validarNombre(nombre);
        Optional<CategoriaProducto> existente = buscarPorNombre(limpio, null);
        if (existente.isPresent()) {
            if (existente.get().activa()) {
                throw repetida(limpio);
            }
            categorias.reactivar(existente.get().id(), limpio);
            return new CategoriaProducto(existente.get().id(), limpio, true);
        }
        try {
            return new CategoriaProducto(categorias.insertar(limpio), limpio, true);
        } catch (DuplicateKeyException e) {
            throw repetida(limpio);
        }
    }

    @Transactional
    public CategoriaProducto actualizar(long id, String nombre) {
        String limpio = validarNombre(nombre);
        if (buscar(id).isEmpty()) {
            throw new ReglaNegocioException("La categoría que intenta editar ya no existe.");
        }
        if (buscarPorNombre(limpio, id).isPresent()) {
            throw repetida(limpio);
        }
        categorias.actualizar(id, limpio);
        return new CategoriaProducto(id, limpio, true);
    }

    /** Eliminación lógica de la categoría y de sus presentaciones. */
    @Transactional
    public void eliminar(long id) {
        if (buscar(id).isEmpty()) {
            throw new ReglaNegocioException("La categoría que intenta eliminar ya no existe.");
        }
        presentaciones.desactivarPorCategoria(id);
        categorias.desactivar(id);
    }

    private Optional<CategoriaProducto> buscarPorNombre(String nombre, Long excluirId) {
        String clave = normalizar(nombre);
        return categorias.listarTodas().stream()
                .filter(c -> normalizar(c.nombre()).equals(clave) && (excluirId == null || c.id() != excluirId))
                .findFirst();
    }

    private String validarNombre(String nombre) {
        String limpio = nombre == null ? "" : nombre.trim().replaceAll("\\s+", " ");
        if (limpio.isEmpty()) {
            throw new ReglaNegocioException("El nombre de la categoría es obligatorio.");
        }
        if (limpio.length() > LARGO_MAXIMO_NOMBRE) {
            throw new ReglaNegocioException("El nombre no puede superar los " + LARGO_MAXIMO_NOMBRE + " caracteres.");
        }
        return limpio;
    }

    private static ReglaNegocioException repetida(String nombre) {
        return new ReglaNegocioException("Ya existe una categoría con el nombre «" + nombre + "».");
    }

    private static String normalizar(String texto) {
        String sinTildes = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return sinTildes.toLowerCase(Locale.ROOT);
    }
}

package com.homeolive.modelo;

import com.homeolive.dao.CategoriaProductoDao;
import com.homeolive.dao.PresentacionDao;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Reglas de CATEGORIA_PRESENTACION. Una presentación es el tamaño o formato en que se vende una categoría
 * (30 ml, 45 gr, 60 cápsulas) con su precio unitario. Eliminar es lógico; crear una igual a una eliminada la
 * reactiva con el nuevo precio. El precio de los pedidos ya registrados no cambia (queda congelado).
 */
@Component
public class PresentacionModelo {

    public static final Set<String> UNIDADES = Set.of("ml", "gr", "cápsulas");
    public static final BigDecimal PRECIO_MAXIMO = new BigDecimal("99999.99");
    private static final int VALOR_MAXIMO = 99999;

    private final PresentacionDao presentaciones;
    private final CategoriaProductoDao categorias;

    public PresentacionModelo(PresentacionDao presentaciones, CategoriaProductoDao categorias) {
        this.presentaciones = presentaciones;
        this.categorias = categorias;
    }

    /** Presentaciones vigentes, ordenadas por categoría y medida. */
    public List<Presentacion> listar() {
        return presentaciones.listarTodas().stream().filter(Presentacion::activa).toList();
    }

    public Optional<Presentacion> buscar(long id) {
        return presentaciones.buscarPorId(id).filter(Presentacion::activa);
    }

    @Transactional
    public Presentacion crear(long idCategoria, String valorTexto, String unidad, String precioTexto) {
        CategoriaProducto categoria = categorias.buscarPorId(idCategoria).filter(CategoriaProducto::activa)
                .orElseThrow(() -> new ReglaNegocioException("Selecciona una categoría válida."));
        int valor = validarValor(valorTexto);
        String u = validarUnidad(unidad);
        BigDecimal precio = validarPrecio(precioTexto);

        Optional<Presentacion> existente = presentaciones.buscarPorClave(idCategoria, valor, u);
        if (existente.isPresent()) {
            Presentacion previa = existente.get();
            if (previa.activa()) {
                throw repetida(valor, u, categoria.nombre());
            }
            presentaciones.reactivar(previa.id(), precio);
            return new Presentacion(previa.id(), idCategoria, categoria.nombre(), valor, u, precio, true);
        }
        try {
            long id = presentaciones.insertar(idCategoria, valor, u, precio);
            return new Presentacion(id, idCategoria, categoria.nombre(), valor, u, precio, true);
        } catch (DuplicateKeyException e) {
            throw repetida(valor, u, categoria.nombre());
        }
    }

    /**
     * Cambia medida, unidad y precio. Si la presentación ya se usó en pedidos solo puede cambiar su precio,
     * para no alterar lo que dicen los pedidos antiguos.
     */
    @Transactional
    public Presentacion actualizar(long id, String valorTexto, String unidad, String precioTexto) {
        Presentacion actual = buscar(id)
                .orElseThrow(() -> new ReglaNegocioException("La presentación que intenta editar ya no existe."));
        int valor = validarValor(valorTexto);
        String u = validarUnidad(unidad);
        BigDecimal precio = validarPrecio(precioTexto);

        boolean cambiaMedida = valor != actual.valor() || !u.equals(actual.unidad());
        if (cambiaMedida) {
            if (presentaciones.contarUsosEnPedidos(id) > 0) {
                throw new ReglaNegocioException("La presentación " + actual.etiqueta() + " ya tiene pedidos registrados: "
                        + "solo puedes cambiar su precio. Para cambiar la medida, elimínala y crea una nueva.");
            }
            Optional<Presentacion> otra = presentaciones.buscarPorClave(actual.idCategoria(), valor, u)
                    .filter(p -> p.id() != id);
            if (otra.isPresent()) {
                throw repetida(valor, u, actual.categoria());
            }
        }
        try {
            presentaciones.actualizar(id, valor, u, precio);
        } catch (DuplicateKeyException e) {
            throw repetida(valor, u, actual.categoria());
        }
        return new Presentacion(id, actual.idCategoria(), actual.categoria(), valor, u, precio, true);
    }

    /** Eliminación lógica: deja de ofrecerse al vender, los pedidos ya registrados la conservan. */
    @Transactional
    public void eliminar(long id) {
        if (buscar(id).isEmpty()) {
            throw new ReglaNegocioException("La presentación que intenta eliminar ya no existe.");
        }
        presentaciones.desactivar(id);
    }

    private static int validarValor(String texto) {
        try {
            int valor = Integer.parseInt(texto == null ? "" : texto.trim());
            if (valor >= 1 && valor <= VALOR_MAXIMO) {
                return valor;
            }
        } catch (NumberFormatException ignorada) {
            // se informa abajo con el mismo mensaje
        }
        throw new ReglaNegocioException("La medida debe ser un número entero entre 1 y " + VALOR_MAXIMO + ".");
    }

    private static String validarUnidad(String unidad) {
        String limpia = unidad == null ? "" : unidad.trim();
        if (!UNIDADES.contains(limpia)) {
            throw new ReglaNegocioException("Selecciona la unidad: ml, gr o cápsulas.");
        }
        return limpia;
    }

    private static BigDecimal validarPrecio(String texto) {
        BigDecimal precio;
        try {
            precio = new BigDecimal((texto == null ? "" : texto.trim()).replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new ReglaNegocioException("El precio no es válido.");
        }
        if (precio.signum() <= 0 || precio.compareTo(PRECIO_MAXIMO) > 0) {
            throw new ReglaNegocioException("El precio debe ser mayor que 0 y no superar S/ 99999.99.");
        }
        if (precio.stripTrailingZeros().scale() > 2) {
            throw new ReglaNegocioException("El precio admite como máximo 2 decimales.");
        }
        return precio.setScale(2);
    }

    private static ReglaNegocioException repetida(int valor, String unidad, String categoria) {
        return new ReglaNegocioException("Ya existe la presentación " + valor + " " + unidad + " en «" + categoria + "».");
    }
}

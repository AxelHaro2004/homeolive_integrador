package com.homeolive.dao;

import com.homeolive.modelo.EstadoPedido;
import com.homeolive.modelo.NuevoPedido;
import com.homeolive.modelo.Pedido;
import com.homeolive.modelo.TipoEnvio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Acceso a datos de PEDIDO_VERIFICADO y DETALLE_PEDIDO. */
public interface PedidoDao {

    /** Inserta el pedido con sus ítems y devuelve su id. */
    long insertar(NuevoPedido pedido);

    Optional<Pedido> buscarPorId(long id);

    /** Todos los pedidos, del más reciente al más antiguo. */
    List<Pedido> listar();

    List<Pedido> listarPorEstado(EstadoPedido estado);

    List<Pedido> listarPorCliente(long idCliente);

    /** Pedidos registrados entre ambas fechas, inclusive. */
    List<Pedido> listarPorPeriodo(LocalDate desde, LocalDate hasta);

    /** Los últimos pedidos con movimiento (por fecha de último cambio de estado). */
    List<Pedido> listarRecientes(int limite);

    /**
     * Cambia el estado solo si el pedido sigue en el estado {@code desde}. Devuelve {@code false} si
     * otro usuario ya lo había cambiado (evita transiciones duplicadas).
     */
    boolean cambiarEstado(long id, EstadoPedido desde, EstadoPedido hacia, LocalDateTime cuando);

    boolean actualizarAdmin(long id, long idCliente, String ubicacion, TipoEnvio envio, EstadoPedido estado,
                            LocalDateTime fechaActualizacion);

    boolean eliminar(long id);

    int contarPorCliente(long idCliente);

    Map<EstadoPedido, Integer> contarPorEstado();

    int contarEntregadosEntre(LocalDateTime desde, LocalDateTime hasta);

    /** Siguiente N° de pedido, p. ej. {@code PED-1049}. */
    String siguienteCodigoPedido();

    /** Siguiente N° de venta, p. ej. {@code VEN-5082}. */
    String siguienteCodigoVenta();
}

package com.homeolive.modelo;

import com.homeolive.dao.EmpaquetadoDao;
import com.homeolive.dao.PedidoDao;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * Ciclo de vida del pedido. Cada rol avanza solo su tramo:
 * <ul>
 *   <li>Químico: En preparación → Listo para envío.</li>
 *   <li>Gestor, motorizado: Listo para envío → Entregado (cliente confirma recepción).</li>
 *   <li>Gestor, courier: Listo para envío → Por confirmar (courier + id_pedido_courier) → Entregado
 *       (plataforma en «listo para recoger» y cliente avisado por llamada).</li>
 * </ul>
 */
@Component
public class FlujoPedidoModelo {

    private static final int LARGO_MAXIMO_ID_COURIER = 50;
    private static final Pattern ID_COURIER = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._/-]*$");

    private final PedidoDao pedidos;
    private final EmpaquetadoDao empaquetado;
    private final Clock reloj;

    public FlujoPedidoModelo(PedidoDao pedidos, EmpaquetadoDao empaquetado, Clock reloj) {
        this.pedidos = pedidos;
        this.empaquetado = empaquetado;
        this.reloj = reloj;
    }

    @Transactional
    public Pedido marcarListoParaEnvio(long idPedido) {
        Pedido pedido = obtener(idPedido);
        exigirEstado(pedido, EstadoPedido.EN_PREPARACION, "marcarse como «listo para envío»");
        cambiar(pedido, EstadoPedido.LISTO_PARA_ENVIO);
        return obtener(idPedido);
    }

    @Transactional
    public Pedido marcarPorConfirmar(long idPedido, String courierTexto, String idPedidoCourierTexto,
                                     long idEmpleado) {
        Pedido pedido = obtener(idPedido);
        exigirEstado(pedido, EstadoPedido.LISTO_PARA_ENVIO, "pasar a «por confirmar»");
        if (pedido.envio().esMotorizado()) {
            throw new ReglaNegocioException(
                    "Los pedidos por motorizado no pasan por «por confirmar»: se marcan directamente como entregados.");
        }
        TipoEnvio courier = TipoEnvio.deTexto(courierTexto).filter(TipoEnvio::esCourier)
                .orElseThrow(() -> new ReglaNegocioException("Selecciona el courier: Olva o Shalom."));
        String idCourier = validarIdCourierOpcional(idPedidoCourierTexto);
        if (idCourier.isEmpty()) {
            throw new ReglaNegocioException("El id_pedido_courier es obligatorio.");
        }

        LocalDateTime ahora = LocalDateTime.now(reloj);
        if (courier != pedido.envio()) {
            // El courier realmente usado manda sobre el que se eligió al registrar la venta.
            pedidos.actualizarAdmin(idPedido, pedido.cliente().id(), pedido.ubicacion(), courier,
                    pedido.estado(), pedido.fechaActualizacion());
        }
        cambiar(pedido, EstadoPedido.POR_CONFIRMAR);
        empaquetado.registrar(idPedido, courier, idCourier, idEmpleado, ahora);
        return obtener(idPedido);
    }

    @Transactional
    public Pedido marcarEntregado(long idPedido, ConfirmacionEntrega confirmacion, long idEmpleado) {
        Pedido pedido = obtener(idPedido);
        switch (pedido.estado()) {
            case LISTO_PARA_ENVIO -> {
                if (pedido.envio().esCourier()) {
                    throw new ReglaNegocioException(
                            "Los pedidos por courier deben pasar primero por «por confirmar».");
                }
                if (!confirmacion.clienteConfirmoRecepcion()) {
                    throw new ReglaNegocioException("Confirma que el cliente recibió su pedido.");
                }
                cambiar(pedido, EstadoPedido.ENTREGADO);
                empaquetado.registrar(idPedido, TipoEnvio.MOTORIZADO, null, idEmpleado, LocalDateTime.now(reloj));
            }
            case POR_CONFIRMAR -> {
                if (!confirmacion.plataformaListoParaRecoger()) {
                    throw new ReglaNegocioException(
                            "El pedido solo pasa a «entregado» cuando la plataforma del courier indica «listo para recoger».");
                }
                if (!confirmacion.clienteAvisadoPorLlamada()) {
                    throw new ReglaNegocioException(
                            "Antes de marcarlo como entregado debes llamar al cliente para avisarle que ya puede recoger su pedido.");
                }
                cambiar(pedido, EstadoPedido.ENTREGADO);
            }
            default -> throw new ReglaNegocioException(
                    "El pedido " + pedido.codigo() + " no puede marcarse como entregado desde «"
                            + pedido.estado().etiqueta() + "».");
        }
        return obtener(idPedido);
    }

    /** Normaliza el id del courier; devuelve cadena vacía si no se indicó. Lanza error si es inválido. */
    static String validarIdCourierOpcional(String texto) {
        String limpio = texto == null ? "" : texto.trim();
        if (limpio.isEmpty()) {
            return "";
        }
        if (limpio.length() > LARGO_MAXIMO_ID_COURIER) {
            throw new ReglaNegocioException(
                    "El id_pedido_courier no puede superar los " + LARGO_MAXIMO_ID_COURIER + " caracteres.");
        }
        if (!ID_COURIER.matcher(limpio).matches()) {
            throw new ReglaNegocioException("El id_pedido_courier solo admite letras, números y los signos . _ / -");
        }
        return limpio;
    }

    private Pedido obtener(long idPedido) {
        return pedidos.buscarPorId(idPedido)
                .orElseThrow(() -> new ReglaNegocioException("El pedido solicitado no existe."));
    }

    private void exigirEstado(Pedido pedido, EstadoPedido esperado, String accion) {
        if (pedido.estado() != esperado) {
            throw new ReglaNegocioException("El pedido " + pedido.codigo() + " no puede " + accion
                    + " porque está «" + pedido.estado().etiqueta() + "».");
        }
    }

    private void cambiar(Pedido pedido, EstadoPedido nuevo) {
        if (!pedidos.cambiarEstado(pedido.id(), pedido.estado(), nuevo, LocalDateTime.now(reloj))) {
            throw new ReglaNegocioException(
                    "El pedido ya cambió de estado. Actualiza la pantalla e inténtalo nuevamente.");
        }
    }
}

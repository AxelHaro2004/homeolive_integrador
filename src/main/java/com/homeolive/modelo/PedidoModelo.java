package com.homeolive.modelo;

import com.homeolive.dao.CatalogoDao;
import com.homeolive.dao.EmpaquetadoDao;
import com.homeolive.dao.PedidoDao;
import com.homeolive.dao.PresentacionDao;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Reglas de negocio del registro, consulta y administración de pedidos.
 * El avance de estado por rol vive en {@link FlujoPedidoModelo}.
 */
@Component
public class PedidoModelo {

    public static final int CANTIDAD_MAXIMA_POR_ITEM = 999;
    private static final int INTENTOS_CODIGO = 3;

    private final PedidoDao pedidos;
    private final PresentacionDao presentaciones;
    private final CatalogoDao catalogo;
    private final EmpaquetadoDao empaquetado;
    private final ClienteModelo clientes;
    private final Clock reloj;

    public PedidoModelo(PedidoDao pedidos, PresentacionDao presentaciones, CatalogoDao catalogo,
                        EmpaquetadoDao empaquetado, ClienteModelo clientes, Clock reloj) {
        this.pedidos = pedidos;
        this.presentaciones = presentaciones;
        this.catalogo = catalogo;
        this.empaquetado = empaquetado;
        this.clientes = clientes;
        this.reloj = reloj;
    }

    /* ------------------------------------------------------------------ */
    /* Consultas                                                          */
    /* ------------------------------------------------------------------ */

    public List<Pedido> listar() {
        return pedidos.listar();
    }

    public List<Pedido> listarPorEstado(EstadoPedido estado) {
        return pedidos.listarPorEstado(estado);
    }

    public List<Pedido> listarPorCliente(long idCliente) {
        return pedidos.listarPorCliente(idCliente);
    }

    public List<Pedido> listarPorPeriodo(LocalDate desde, LocalDate hasta) {
        return pedidos.listarPorPeriodo(desde, hasta);
    }

    public List<Pedido> listarRecientes(int limite) {
        return pedidos.listarRecientes(limite);
    }

    public Optional<Pedido> buscar(long id) {
        return pedidos.buscarPorId(id);
    }

    public Pedido obtener(long id) {
        return pedidos.buscarPorId(id)
                .orElseThrow(() -> new ReglaNegocioException("El pedido solicitado no existe."));
    }

    /** Vista previa del próximo N° de pedido (el definitivo se asigna al guardar). */
    public String siguienteCodigoPedido() {
        return pedidos.siguienteCodigoPedido();
    }

    /** Vista previa del próximo N° de venta (el definitivo se asigna al guardar). */
    public String siguienteCodigoVenta() {
        return pedidos.siguienteCodigoVenta();
    }

    public ResumenPanel resumenPanel() {
        Map<EstadoPedido, Integer> porEstado = pedidos.contarPorEstado();
        LocalDate hoy = LocalDate.now(reloj);
        int entregadosHoy = pedidos.contarEntregadosEntre(hoy.atStartOfDay(), hoy.plusDays(1).atStartOfDay());
        return new ResumenPanel(porEstado.get(EstadoPedido.EN_PREPARACION),
                porEstado.get(EstadoPedido.LISTO_PARA_ENVIO),
                porEstado.get(EstadoPedido.POR_CONFIRMAR),
                entregadosHoy);
    }

    /* ------------------------------------------------------------------ */
    /* Registro (vendedor)                                                */
    /* ------------------------------------------------------------------ */

    /**
     * Registra una venta: valida al cliente, la ubicación, el tipo de envío, el pago y los ítems; crea o
     * reutiliza al cliente por DNI; congela el precio de cada ítem y deja el pedido «en preparación».
     */
    @Transactional
    public Pedido registrar(SolicitudPedido solicitud) {
        DatosCliente datos = clientes.validar(new DatosCliente(solicitud.nombre(), solicitud.dni(),
                solicitud.telefono(), solicitud.ubicacion()));
        Ubicacion ubicacion = Ubicacion.deTexto(datos.ubicacion()).orElseThrow();
        TipoEnvio envio = validarEnvio(solicitud.envio(), ubicacion);
        if (!solicitud.pagoConfirmado()) {
            throw new ReglaNegocioException("Solo se registra el pedido cuando el cliente ya realizó el pago.");
        }
        List<ItemPedido> items = construirItems(solicitud.lineas());

        Cliente cliente = clientes.obtenerOCrearPorDni(datos);
        return crearPedido(cliente.id(), ubicacion, envio, items);
    }

    /** Alta de un pedido para un cliente existente (administrador). */
    @Transactional
    public Pedido registrarParaCliente(long idCliente, String ubicacionTexto, String envioTexto,
                                       List<SolicitudPedido.Linea> lineas) {
        Cliente cliente = clientes.obtener(idCliente);
        Ubicacion ubicacion = validarUbicacion(ubicacionTexto);
        TipoEnvio envio = validarEnvio(envioTexto, ubicacion);
        List<ItemPedido> items = construirItems(lineas);
        return crearPedido(cliente.id(), ubicacion, envio, items);
    }

    /* ------------------------------------------------------------------ */
    /* Administración (administrador)                                     */
    /* ------------------------------------------------------------------ */

    /**
     * Edita un pedido. El monto y los ítems no se modifican (el precio queda congelado al momento de la
     * venta). Si cambia el estado o la logística, se mantiene coherente el registro de empaquetado.
     */
    @Transactional
    public Pedido actualizarAdmin(long id, long idCliente, String ubicacionTexto, String envioTexto,
                                  String estadoTexto, String idCourierTexto, long idEmpleado) {
        Pedido actual = obtener(id);
        clientes.obtener(idCliente);
        Ubicacion ubicacion = validarUbicacion(ubicacionTexto);
        TipoEnvio envio = validarEnvio(envioTexto, ubicacion);
        EstadoPedido estado = EstadoPedido.deTexto(estadoTexto)
                .orElseThrow(() -> new ReglaNegocioException("Selecciona un estado válido."));

        String idCourier = envio.esMotorizado() ? "" : FlujoPedidoModelo.validarIdCourierOpcional(idCourierTexto);
        if (estado == EstadoPedido.POR_CONFIRMAR) {
            if (envio.esMotorizado()) {
                throw new ReglaNegocioException("Los pedidos por motorizado no pasan por «por confirmar».");
            }
            if (idCourier.isEmpty()) {
                throw new ReglaNegocioException("Un pedido «por confirmar» necesita el id_pedido_courier.");
            }
        }

        LocalDateTime ahora = LocalDateTime.now(reloj);
        LocalDateTime fecha = estado != actual.estado() ? ahora : actual.fechaActualizacion();
        pedidos.actualizarAdmin(id, idCliente, ubicacion.etiqueta(), envio, estado, fecha);

        String idCourierActual = actual.idCourier() == null ? "" : actual.idCourier();
        boolean cambioLogistico = estado != actual.estado() || envio != actual.envio()
                || !Objects.equals(idCourier, idCourierActual);
        if (cambioLogistico) {
            empaquetado.eliminarPorPedido(id);
            boolean entregado = estado == EstadoPedido.ENTREGADO;
            if (envio.esCourier() && !idCourier.isEmpty()
                    && (estado == EstadoPedido.POR_CONFIRMAR || entregado)) {
                empaquetado.registrar(id, envio, idCourier, idEmpleado, ahora);
            } else if (envio.esMotorizado() && entregado) {
                empaquetado.registrar(id, TipoEnvio.MOTORIZADO, null, idEmpleado, ahora);
            }
        }
        return obtener(id);
    }

    @Transactional
    public void eliminar(long id) {
        obtener(id);
        pedidos.eliminar(id);
    }

    /* ------------------------------------------------------------------ */
    /* Utilidades                                                         */
    /* ------------------------------------------------------------------ */

    private Pedido crearPedido(long idCliente, Ubicacion ubicacion, TipoEnvio envio, List<ItemPedido> items) {
        BigDecimal monto = items.stream().map(ItemPedido::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDateTime ahora = LocalDateTime.now(reloj);
        for (int intento = 1; ; intento++) {
            try {
                long id = pedidos.insertar(new NuevoPedido(pedidos.siguienteCodigoPedido(),
                        pedidos.siguienteCodigoVenta(), idCliente, ubicacion.etiqueta(), envio,
                        EstadoPedido.EN_PREPARACION, monto, ahora, items));
                return obtener(id);
            } catch (DuplicateKeyException e) {
                // Otro registro simultáneo tomó el mismo número: se vuelve a calcular.
                if (intento >= INTENTOS_CODIGO) {
                    throw new ReglaNegocioException("No se pudo generar el número de pedido. Inténtalo nuevamente.");
                }
            }
        }
    }

    private List<ItemPedido> construirItems(List<SolicitudPedido.Linea> lineas) {
        if (lineas == null || lineas.isEmpty()) {
            throw new ReglaNegocioException("Agrega al menos un ítem al pedido.");
        }
        // Las filas repetidas (misma presentación y mismo producto) se suman en un solo ítem.
        Map<String, SolicitudPedido.Linea> agrupadas = new LinkedHashMap<>();
        for (SolicitudPedido.Linea linea : lineas) {
            if (linea == null || linea.idPresentacion() == null) {
                throw new ReglaNegocioException("Selecciona la categoría y la presentación en cada ítem.");
            }
            if (linea.cantidad() < 1) {
                throw new ReglaNegocioException("La cantidad de cada ítem debe ser al menos 1.");
            }
            String texto = linea.producto() == null ? "" : linea.producto().trim().replaceAll("\\s+", " ");
            agrupadas.merge(linea.idPresentacion() + "|" + texto.toLowerCase(Locale.ROOT), linea,
                    (a, b) -> new SolicitudPedido.Linea(a.idPresentacion(), a.producto(), a.cantidad() + b.cantidad()));
        }
        return agrupadas.values().stream().map(l -> {
            Presentacion presentacion = presentaciones.buscarPorId(l.idPresentacion()).filter(Presentacion::activa)
                    .orElseThrow(() -> new ReglaNegocioException("La presentación seleccionada no existe en el catálogo."));
            if (l.cantidad() > CANTIDAD_MAXIMA_POR_ITEM) {
                throw new ReglaNegocioException("La cantidad máxima por ítem es " + CANTIDAD_MAXIMA_POR_ITEM + ".");
            }
            String producto = Validaciones.producto(l.producto(), presentacion.categoria());
            long idMedicina = catalogo.obtenerOCrearMedicina(producto, presentacion.id());
            return new ItemPedido(idMedicina, presentacion.categoria(), producto, presentacion.etiqueta(),
                    l.cantidad(), presentacion.precio());
        }).toList();
    }

    private static Ubicacion validarUbicacion(String texto) {
        return Ubicacion.deTexto(texto)
                .orElseThrow(() -> new ReglaNegocioException("Selecciona la ubicación: Lima o Provincia."));
    }

    private static TipoEnvio validarEnvio(String texto, Ubicacion ubicacion) {
        TipoEnvio envio = TipoEnvio.deTexto(texto)
                .orElseThrow(() -> new ReglaNegocioException("Selecciona el tipo de envío."));
        if (!ubicacion.permite(envio)) {
            throw new ReglaNegocioException("Para envíos a provincia solo está disponible Olva o Shalom.");
        }
        return envio;
    }
}

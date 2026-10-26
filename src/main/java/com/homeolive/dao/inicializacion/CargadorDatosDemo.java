package com.homeolive.dao.inicializacion;

import com.homeolive.dao.*;
import com.homeolive.modelo.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Carga clientes y pedidos de ejemplo en la base en memoria: unos pedidos recientes en cada estado y
 * un histórico determinista de seis meses para que la exportación de reportes tenga información.
 * Se desactiva con {@code homeolive.demo.datos=false}.
 */
@Component
@Order(2)
@ConditionalOnProperty(name = "homeolive.demo.datos", havingValue = "true", matchIfMissing = true)
public class CargadorDatosDemo implements ApplicationRunner {

    private static final List<String> PRODUCTOS_FLORES = List.of("Calma Kids", "Ansiedad", "TDAH", "Estrés", "Miedo");
    private static final List<String> PRODUCTOS_HOMEOPATICOS =
            List.of("Defensas", "Migraña", "Hipertensión", "Digestivo", "Insomnio");

    private final ClienteDao clientes;
    private final PedidoDao pedidos;
    private final PresentacionDao presentaciones;
    private final CatalogoDao catalogo;
    private final EmpaquetadoDao empaquetado;
    private final EmpleadoDao empleados;

    public CargadorDatosDemo(ClienteDao clientes, PedidoDao pedidos, PresentacionDao presentaciones,
                             CatalogoDao catalogo, EmpaquetadoDao empaquetado, EmpleadoDao empleados) {
        this.clientes = clientes;
        this.pedidos = pedidos;
        this.presentaciones = presentaciones;
        this.catalogo = catalogo;
        this.empaquetado = empaquetado;
        this.empleados = empleados;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!pedidos.listarRecientes(1).isEmpty()) {
            return;
        }
        LocalDate hoy = LocalDate.now(Zona.LIMA);
        long gestor = empleados.buscarIdPorDni("43458169").orElseThrow();

        Cliente maria = cliente("María Fernanda Rojas", "72541863", "987 654 321", "Lima");
        Cliente jorge = cliente("Jorge Luis Salazar", "43812675", "946 120 883", "Provincia");
        Cliente ana = cliente("Ana Lucía Paredes", "70186324", "912 446 720", "Lima");
        Cliente carlos = cliente("Carlos Mendoza Ruiz", "46925718", "966 105 244", "Provincia");
        Cliente valeria = cliente("Valeria Torres Peña", "74152680", "980 630 115", "Lima");
        List<Cliente> todos = List.of(maria, jorge, ana, carlos, valeria);

        // Histórico determinista (PED-0965 … PED-1042).
        Random azar = new Random(20260221L);
        int numero = 1042;
        for (int i = 0; i < 78; i++) {
            LocalDate dia = hoy.minusDays(2 + azar.nextInt(178));
            LocalDateTime fecha = dia.atTime(8 + azar.nextInt(10), azar.nextInt(60));
            Cliente cliente = todos.get(azar.nextInt(todos.size()));
            TipoEnvio envio = "Lima".equals(cliente.ubicacion())
                    ? TipoEnvio.MOTORIZADO
                    : (azar.nextBoolean() ? TipoEnvio.OLVA : TipoEnvio.SHALOM);
            EstadoPedido estado = dia.isBefore(hoy.minusDays(7))
                    ? EstadoPedido.ENTREGADO
                    : EstadoPedido.values()[azar.nextInt(EstadoPedido.values().length)];
            if (envio.esMotorizado() && estado == EstadoPedido.POR_CONFIRMAR) {
                estado = EstadoPedido.ENTREGADO;
            }
            String courier = envio.esCourier() && estado != EstadoPedido.EN_PREPARACION && estado != EstadoPedido.LISTO_PARA_ENVIO
                    ? (envio == TipoEnvio.OLVA ? "OLV-" : "SHA-") + (1200000 + azar.nextInt(8000000))
                    : null;

            List<ItemPedido> items = new ArrayList<>();
            int cuantos = 1 + azar.nextInt(2);
            for (int j = 0; j < cuantos; j++) {
                int dado = azar.nextInt(10);
                if (dado < 2) {
                    items.add(item("Spirulina", "Spirulina", azar.nextBoolean() ? "45 gr" : "60 cápsulas", 1 + azar.nextInt(3)));
                } else if (dado < 4) {
                    items.add(item("Omega 3", "Omega 3", "60 cápsulas", 1 + azar.nextInt(2)));
                } else if (dado < 7) {
                    items.add(item("Flores de Bach", PRODUCTOS_FLORES.get(azar.nextInt(PRODUCTOS_FLORES.size())),
                            "30 ml", 1 + azar.nextInt(2)));
                } else {
                    items.add(item("Homeopático", PRODUCTOS_HOMEOPATICOS.get(azar.nextInt(PRODUCTOS_HOMEOPATICOS.size())),
                            azar.nextBoolean() ? "60 ml" : "120 ml", 1 + azar.nextInt(2)));
                }
            }
            registrar(String.format("PED-%04d", numero), String.format("VEN-%04d", numero + 4033),
                    cliente, envio, estado, fecha, courier, gestor, fusionar(items));
            numero--;
        }

        // Pedidos recientes en cada estado (PED-1043 … PED-1048).
        registrar("PED-1043", "VEN-5076", maria, TipoEnvio.OLVA, EstadoPedido.ENTREGADO,
                hoy.minusDays(41).atTime(16, 20), "OLV-7710254", gestor,
                List.of(item("Homeopático", "Migraña", "60 ml", 2)));
        registrar("PED-1044", "VEN-5077", valeria, TipoEnvio.MOTORIZADO, EstadoPedido.ENTREGADO,
                hoy.minusDays(1).atTime(11, 4), null, gestor,
                List.of(item("Flores de Bach", "Ansiedad", "30 ml", 1)));
        registrar("PED-1045", "VEN-5078", carlos, TipoEnvio.SHALOM, EstadoPedido.POR_CONFIRMAR,
                hoy.minusDays(1).atTime(15, 12), "SHA-9384512", gestor,
                List.of(item("Flores de Bach", "TDAH", "30 ml", 2), item("Homeopático", "Defensas", "120 ml", 1)));
        registrar("PED-1046", "VEN-5079", ana, TipoEnvio.MOTORIZADO, EstadoPedido.LISTO_PARA_ENVIO,
                hoy.minusDays(1).atTime(17, 35), null, gestor,
                List.of(item("Spirulina", "Spirulina", "45 gr", 2)));
        registrar("PED-1047", "VEN-5080", jorge, TipoEnvio.OLVA, EstadoPedido.LISTO_PARA_ENVIO,
                hoy.atTime(8, 16), null, gestor,
                List.of(item("Homeopático", "Hipertensión", "60 ml", 1)));
        registrar("PED-1048", "VEN-5081", maria, TipoEnvio.MOTORIZADO, EstadoPedido.EN_PREPARACION,
                hoy.atTime(9, 42), null, gestor,
                List.of(item("Flores de Bach", "Calma Kids", "30 ml", 2), item("Omega 3", "Omega 3", "60 cápsulas", 1)));
    }

    private Cliente cliente(String nombre, String dni, String telefono, String ubicacion) {
        long id = clientes.insertar(new DatosCliente(nombre, dni, telefono, ubicacion));
        return new Cliente(id, nombre, dni, telefono, ubicacion);
    }

    private void registrar(String codigoPedido, String codigoVenta, Cliente cliente, TipoEnvio envio,
                           EstadoPedido estado, LocalDateTime fecha, String idCourier, long idEmpleado,
                           List<ItemPedido> items) {
        BigDecimal monto = items.stream().map(ItemPedido::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        long id = pedidos.insertar(new NuevoPedido(codigoPedido, codigoVenta, cliente.id(), cliente.ubicacion(),
                envio, estado, monto, fecha, items));
        // Los pedidos que ya salieron del almacén tienen su empaquetado registrado.
        if (estado == EstadoPedido.POR_CONFIRMAR || estado == EstadoPedido.ENTREGADO) {
            if (envio.esCourier() && idCourier != null) {
                empaquetado.registrar(id, envio, idCourier, idEmpleado, fecha.plusHours(3));
            } else if (envio.esMotorizado()) {
                empaquetado.registrar(id, TipoEnvio.MOTORIZADO, null, idEmpleado, fecha.plusHours(3));
            }
        }
    }

    private ItemPedido item(String categoria, String producto, String etiqueta, int cantidad) {
        Presentacion p = presentaciones.listarTodas().stream()
                .filter(x -> x.categoria().equals(categoria) && x.etiqueta().equals(etiqueta))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No hay una presentación " + etiqueta + " en la categoría " + categoria));
        long idMedicina = catalogo.obtenerOCrearMedicina(producto, p.id());
        return new ItemPedido(idMedicina, p.categoria(), producto, p.etiqueta(), cantidad, p.precio());
    }

    /** Une ítems repetidos (misma presentación) para respetar la regla de un ítem por presentación. */
    private List<ItemPedido> fusionar(List<ItemPedido> items) {
        List<ItemPedido> fusionados = new ArrayList<>();
        for (ItemPedido it : items) {
            int pos = -1;
            for (int i = 0; i < fusionados.size(); i++) {
                if (fusionados.get(i).idMedicina() == it.idMedicina()) {
                    pos = i;
                }
            }
            if (pos < 0) {
                fusionados.add(it);
            } else {
                ItemPedido previo = fusionados.get(pos);
                fusionados.set(pos, new ItemPedido(previo.idMedicina(), previo.categoria(), previo.producto(),
                        previo.presentacion(), previo.cantidad() + it.cantidad(), previo.precioUnitario()));
            }
        }
        return fusionados;
    }
}

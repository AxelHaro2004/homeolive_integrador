package com.homeolive.datos;

import com.homeolive.modelo.Cliente;
import com.homeolive.modelo.EstadoPedido;
import com.homeolive.modelo.ItemPedido;
import com.homeolive.modelo.Pedido;
import com.homeolive.modelo.TipoEnvio;

import java.math.BigDecimal;
import java.util.List;


public final class DatosDemo {

    public static final List<String> CATEGORIAS =
            List.of("Flores de Bach", "Homeopático", "Spirulina", "Omega 3");

    public static final List<String> PRESENTACIONES =
            List.of("30 ml", "60 ml", "120 ml", "45 gr", "60 cápsulas");

    private static final Cliente MARIA = new Cliente(1, "María Fernanda Rojas", "72541863", "987 654 321", "Lima");
    private static final Cliente JORGE = new Cliente(2, "Jorge Luis Salazar", "43812675", "946 120 883", "Provincia");
    private static final Cliente ANA = new Cliente(3, "Ana Lucía Paredes", "70186324", "912 446 720", "Lima");
    private static final Cliente CARLOS = new Cliente(4, "Carlos Mendoza Ruiz", "46925718", "966 105 244", "Provincia");
    private static final Cliente VALERIA = new Cliente(5, "Valeria Torres Peña", "74152680", "980 630 115", "Lima");

    private static final List<Cliente> CLIENTES = List.of(MARIA, JORGE, ANA, CARLOS, VALERIA);

    private static final List<Pedido> PEDIDOS = List.of(
            new Pedido("PED-1048", "VEN-5081", MARIA, TipoEnvio.MOTORIZADO, EstadoPedido.EN_PREPARACION,
                    new BigDecimal("168.00"), "Hoy, 09:42",
                    List.of(new ItemPedido("Flores de Bach", "Calma Kids", "30 ml", 2),
                            new ItemPedido("Omega 3", "Omega 3", "60 cápsulas", 1)),
                    null),
            new Pedido("PED-1047", "VEN-5080", JORGE, TipoEnvio.OLVA, EstadoPedido.LISTO_PARA_ENVIO,
                    new BigDecimal("125.00"), "Hoy, 08:16",
                    List.of(new ItemPedido("Homeopático", "Hipertensión", "60 ml", 1)),
                    null),
            new Pedido("PED-1046", "VEN-5079", ANA, TipoEnvio.MOTORIZADO, EstadoPedido.LISTO_PARA_ENVIO,
                    new BigDecimal("92.00"), "Ayer, 17:35",
                    List.of(new ItemPedido("Spirulina", "Spirulina", "45 gr", 2)),
                    null),
            new Pedido("PED-1045", "VEN-5078", CARLOS, TipoEnvio.SHALOM, EstadoPedido.POR_CONFIRMAR,
                    new BigDecimal("210.00"), "Ayer, 15:12",
                    List.of(new ItemPedido("Flores de Bach", "TDAH", "30 ml", 2),
                            new ItemPedido("Homeopático", "Defensas", "120 ml", 1)),
                    "SHA-9384512"),
            new Pedido("PED-1044", "VEN-5077", VALERIA, TipoEnvio.MOTORIZADO, EstadoPedido.ENTREGADO,
                    new BigDecimal("78.00"), "Ayer, 11:04",
                    List.of(new ItemPedido("Flores de Bach", "Ansiedad", "30 ml", 1)),
                    null),
            new Pedido("PED-1043", "VEN-5076", MARIA, TipoEnvio.OLVA, EstadoPedido.ENTREGADO,
                    new BigDecimal("154.00"), "05 Jun, 16:20",
                    List.of(new ItemPedido("Homeopático", "Migraña", "60 ml", 2)),
                    "OLV-7710254")
    );

    private DatosDemo() {
    }

    public static List<Cliente> clientes() {
        return CLIENTES;
    }

    public static List<Pedido> pedidos() {
        return PEDIDOS;
    }

    public static List<Pedido> pedidosEnEstado(EstadoPedido estado) {
        return PEDIDOS.stream().filter(p -> p.estado() == estado).toList();
    }

    public static List<Pedido> pedidosDeCliente(long idCliente) {
        return PEDIDOS.stream().filter(p -> p.cliente().id() == idCliente).toList();
    }

    public static Cliente cliente(long id) {
        return CLIENTES.stream().filter(c -> c.id() == id).findFirst().orElse(CLIENTES.get(0));
    }
}

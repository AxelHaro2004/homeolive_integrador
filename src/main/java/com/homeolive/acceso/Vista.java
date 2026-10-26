package com.homeolive.acceso;

import java.util.Arrays;
import java.util.Optional;

public enum Vista {

    INICIO("/inicio", "Inicio", "Inicio",
            "Resumen general de la operación", "home"),
    REGISTRO_PEDIDOS("/registro-pedidos", "registro-pedidos", "Registro de pedidos",
            "Registra una venta y genera un nuevo pedido", "plus"),
    CLIENTES_PEDIDOS("/clientes-pedidos", "clientes-pedidos", "Pedidos por cliente",
            "Consulta el historial de compras de cada cliente", "users"),
    RESUMEN_VENTAS("/resumen-ventas", "resumen-ventas", "Resumen de ventas",
            "Consulta consolidada de ventas registradas", "chart"),
    RESUMEN_PEDIDOS("/resumen-pedidos", "resumen-pedidos", "Resumen de pedidos",
            "Seguimiento de todos los pedidos y sus estados", "clipboard"),
    PEDIDOS_PENDIENTES("/pedidos-pendientes", "pedidos-pendientes", "Pedidos pendientes",
            "Pedidos que esperan preparación en laboratorio", "box"),
    ENVIOS_PENDIENTES("/envios-pendientes", "envios-pendientes", "Envíos pendientes",
            "Gestión de despachos y confirmaciones de entrega", "truck"),
    ADMIN_CLIENTES("/admin-clientes", "admin-clientes", "Administrar clientes",
            "Crea, edita y organiza la base de clientes", "users"),
    ADMIN_PEDIDOS("/admin-pedidos", "admin-pedidos", "Administrar pedidos",
            "Control completo de los pedidos registrados", "clipboard"),
    CATEGORIAS_PRESENTACIONES("/categorias-presentaciones", "categorias-presentaciones", "Categorías y presentaciones",
            "Crea, edita y elimina categorías y presentaciones del catálogo de venta", "tag");

    private final String ruta;
    private final String etiquetaMenu;
    private final String titulo;
    private final String descripcion;
    private final String icono;

    Vista(String ruta, String etiquetaMenu, String titulo, String descripcion, String icono) {
        this.ruta = ruta;
        this.etiquetaMenu = etiquetaMenu;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.icono = icono;
    }

    public String ruta() {
        return ruta;
    }

    public String etiquetaMenu() {
        return etiquetaMenu;
    }

    public String titulo() {
        return titulo;
    }

    public String descripcion() {
        return descripcion;
    }

    public String icono() {
        return icono;
    }

    /**
     * Vista a la que pertenece una ruta. Una ruta pertenece a la vista si es igual a ella o cuelga de
     * ella (por ejemplo, {@code /categorias-presentaciones/categorias/3/editar} pertenece a {@code /categorias-presentaciones}),
     * de modo que las acciones POST quedan protegidas con el mismo permiso que la pantalla.
     */
    public static Optional<Vista> porRuta(String ruta) {
        return Arrays.stream(values())
                .filter(v -> ruta.equals(v.ruta) || ruta.startsWith(v.ruta + "/"))
                .findFirst();
    }
}

package com.homeolive.acceso;

import java.util.List;

public enum Rol {

    VENDEDOR("Vendedor", "Andrea", "AV",
            Vista.INICIO, Vista.REGISTRO_PEDIDOS, Vista.CLIENTES_PEDIDOS),

    AGENTE_FINANZAS("Agente de Finanzas", "Diego", "DA",
            Vista.INICIO, Vista.RESUMEN_VENTAS),

    QUIMICO_FARMACEUTICO("Químico Farmacéutico", "Diego", "DQ",
            Vista.INICIO, Vista.PEDIDOS_PENDIENTES),

    GESTOR_ENVIOS("Gestor de envíos", "Diego", "DG",
            Vista.INICIO, Vista.ENVIOS_PENDIENTES),

    ADMINISTRADOR("Administrador", "Lucía", "LA",
            Vista.INICIO, Vista.ADMIN_CLIENTES, Vista.ADMIN_PEDIDOS, Vista.CLIENTES_PEDIDOS,
            Vista.RESUMEN_PEDIDOS, Vista.RESUMEN_VENTAS);

    public static final String SESION = "rol";

    private static final String APELLIDO_DEMO = "Ramírez";

    private final String etiqueta;
    private final String nombrePila;
    private final String iniciales;
    private final List<Vista> vistas;

    Rol(String etiqueta, String nombrePila, String iniciales, Vista... vistas) {
        this.etiqueta = etiqueta;
        this.nombrePila = nombrePila;
        this.iniciales = iniciales;
        this.vistas = List.of(vistas);
    }

    public String etiqueta() {
        return etiqueta;
    }

    public String nombrePila() {
        return nombrePila;
    }

    public String nombreCompleto() {
        return nombrePila + " " + APELLIDO_DEMO;
    }

    public String iniciales() {
        return iniciales;
    }

    public List<Vista> vistas() {
        return vistas;
    }

    public boolean puede(Vista vista) {
        return vistas.contains(vista);
    }

    public List<Vista> accesosRapidos() {
        return vistas.stream().filter(v -> v != Vista.INICIO).limit(3).toList();
    }

    public boolean puedeExportar() {
        return this == ADMINISTRADOR;
    }

    public static Rol deClave(String clave) {
        if (clave == null) {
            return null;
        }
        try {
            return Rol.valueOf(clave);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

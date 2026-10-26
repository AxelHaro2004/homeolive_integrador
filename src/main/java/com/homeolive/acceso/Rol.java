package com.homeolive.acceso;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum Rol {

    VENDEDOR("Vendedor", "vendedor",
            Vista.INICIO, Vista.REGISTRO_PEDIDOS, Vista.CLIENTES_PEDIDOS),

    AGENTE_FINANZAS("Agente de Finanzas", "agente_finanzas",
            Vista.INICIO, Vista.RESUMEN_VENTAS),

    QUIMICO_FARMACEUTICO("Químico Farmacéutico", "quimico_farmaceutico",
            Vista.INICIO, Vista.PEDIDOS_PENDIENTES),

    GESTOR_ENVIOS("Gestor de envíos", "gestor_envios",
            Vista.INICIO, Vista.ENVIOS_PENDIENTES),

    ADMINISTRADOR("Administrador", "administrador",
            Vista.INICIO, Vista.ADMIN_CLIENTES, Vista.ADMIN_PEDIDOS, Vista.CATEGORIAS_PRESENTACIONES,
            Vista.CLIENTES_PEDIDOS, Vista.RESUMEN_PEDIDOS, Vista.RESUMEN_VENTAS);

    private final String etiqueta;
    private final String claveBd;
    private final List<Vista> vistas;

    Rol(String etiqueta, String claveBd, Vista... vistas) {
        this.etiqueta = etiqueta;
        this.claveBd = claveBd;
        this.vistas = List.of(vistas);
    }

    public String etiqueta() {
        return etiqueta;
    }

    /** Valor guardado en USUARIO.rol_sistema. */
    public String claveBd() {
        return claveBd;
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

    public boolean puedeAdministrarCatalogo() {
        return puede(Vista.CATEGORIAS_PRESENTACIONES);
    }

    public static Optional<Rol> deClaveBd(String clave) {
        return Arrays.stream(values()).filter(r -> r.claveBd.equals(clave)).findFirst();
    }
}

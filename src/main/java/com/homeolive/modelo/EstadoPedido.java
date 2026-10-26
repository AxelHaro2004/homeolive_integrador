package com.homeolive.modelo;

import java.util.Arrays;
import java.util.Optional;

public enum EstadoPedido {

    EN_PREPARACION("En preparación", "status-en-preparacion"),
    LISTO_PARA_ENVIO("Listo para envío", "status-listo-para-envio"),
    POR_CONFIRMAR("Por confirmar", "status-por-confirmar"),
    ENTREGADO("Entregado", "status-entregado");

    private final String etiqueta;
    private final String css;

    EstadoPedido(String etiqueta, String css) {
        this.etiqueta = etiqueta;
        this.css = css;
    }

    public String etiqueta() {
        return etiqueta;
    }

    public String css() {
        return css;
    }

    public static Optional<EstadoPedido> deTexto(String texto) {
        if (texto == null) {
            return Optional.empty();
        }
        String limpio = texto.trim();
        return Arrays.stream(values())
                .filter(e -> e.name().equalsIgnoreCase(limpio) || e.etiqueta.equalsIgnoreCase(limpio))
                .findFirst();
    }
}

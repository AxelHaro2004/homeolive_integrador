package com.homeolive.modelo;

import java.util.Arrays;
import java.util.Optional;

public enum TipoEnvio {

    MOTORIZADO("Motorizado"),
    OLVA("Olva"),
    SHALOM("Shalom");

    private final String etiqueta;

    TipoEnvio(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String etiqueta() {
        return etiqueta;
    }

    public boolean esMotorizado() {
        return this == MOTORIZADO;
    }

    public boolean esCourier() {
        return !esMotorizado();
    }

    /** Acepta la etiqueta («Olva») o el nombre del enum («OLVA»), sin distinguir mayúsculas. */
    public static Optional<TipoEnvio> deTexto(String texto) {
        if (texto == null) {
            return Optional.empty();
        }
        String limpio = texto.trim();
        return Arrays.stream(values())
                .filter(t -> t.name().equalsIgnoreCase(limpio) || t.etiqueta.equalsIgnoreCase(limpio))
                .findFirst();
    }
}

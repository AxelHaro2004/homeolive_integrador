package com.homeolive.modelo;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Ubicación de entrega. Regla del negocio: en Lima el cliente puede elegir Motorizado, Olva o
 * Shalom; en provincia solo Olva o Shalom.
 */
public enum Ubicacion {

    LIMA("Lima"),
    PROVINCIA("Provincia");

    private final String etiqueta;

    Ubicacion(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String etiqueta() {
        return etiqueta;
    }

    public boolean permite(TipoEnvio envio) {
        return this == LIMA || envio.esCourier();
    }

    public List<TipoEnvio> enviosDisponibles() {
        return Arrays.stream(TipoEnvio.values()).filter(this::permite).toList();
    }

    public static Optional<Ubicacion> deTexto(String texto) {
        if (texto == null) {
            return Optional.empty();
        }
        String limpio = texto.trim();
        return Arrays.stream(values())
                .filter(u -> u.name().equalsIgnoreCase(limpio) || u.etiqueta.equalsIgnoreCase(limpio))
                .findFirst();
    }
}

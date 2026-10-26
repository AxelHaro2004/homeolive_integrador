package com.homeolive.modelo;

import java.time.ZoneId;

/** Zona horaria de la operación: las fechas del sistema se calculan con hora de Lima. */
public final class Zona {

    public static final ZoneId LIMA = ZoneId.of("America/Lima");

    private Zona() {
    }
}

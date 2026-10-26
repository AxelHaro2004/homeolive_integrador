package com.homeolive.modelo;

import java.util.ArrayList;
import java.util.List;

/** Opción del desplegable de mes usado en la exportación de reportes. */
public record OpcionMes(int valor, String nombre) {

    public static List<OpcionMes> todos() {
        List<OpcionMes> opciones = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            opciones.add(new OpcionMes(i, PeriodoReporte.MESES.get(i - 1)));
        }
        return List.copyOf(opciones);
    }
}

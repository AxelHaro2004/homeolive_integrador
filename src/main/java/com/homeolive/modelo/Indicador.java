package com.homeolive.modelo;

public record Indicador(String etiqueta, int valor, String icono, String tono) {

    public String valorTexto() {
        return String.format("%02d", valor);
    }
}

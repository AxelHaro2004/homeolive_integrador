package com.homeolive.modelo;

/** Error de una regla del negocio. Su mensaje está pensado para mostrarse tal cual al usuario. */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}

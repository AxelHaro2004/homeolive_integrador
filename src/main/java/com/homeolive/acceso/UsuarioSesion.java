package com.homeolive.acceso;

import jakarta.servlet.http.HttpSession;

import java.io.Serializable;

/**
 * Datos del usuario autenticado que se guardan en la sesión HTTP. No incluye la contraseña ni su hash.
 */
public record UsuarioSesion(long idUsuario, long idEmpleado, String nombre, String username, Rol rol)
        implements Serializable {

    public static final String SESION = "usuario";

    public String iniciales() {
        StringBuilder sb = new StringBuilder();
        for (String palabra : nombre.trim().split("\\s+")) {
            if (!palabra.isEmpty() && sb.length() < 2) {
                sb.append(palabra.charAt(0));
            }
        }
        return sb.toString().toUpperCase();
    }

    public String nombrePila() {
        String[] palabras = nombre.trim().split("\\s+");
        return palabras.length == 0 ? nombre : palabras[0];
    }

    /** @return el usuario de la sesión, o {@code null} si no hay sesión o no inició sesión. */
    public static UsuarioSesion de(HttpSession sesion) {
        return sesion == null ? null : (UsuarioSesion) sesion.getAttribute(SESION);
    }
}

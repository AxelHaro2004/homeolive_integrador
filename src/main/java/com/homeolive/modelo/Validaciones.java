package com.homeolive.modelo;

import java.util.regex.Pattern;

/** Validación y normalización de los datos que ingresa el usuario. */
public final class Validaciones {

    public static final int LARGO_MAXIMO_NOMBRE = 100;

    private static final Pattern NOMBRE = Pattern.compile("^\\p{L}[\\p{L} .'’-]*$");
    private static final Pattern DNI = Pattern.compile("^\\d{8}$");
    private static final Pattern PRODUCTO = Pattern.compile("^[\\p{L}\\p{N}][\\p{L}\\p{N} .'’()/+&-]*$");

    private Validaciones() {
    }

    public static String nombrePersona(String texto) {
        String limpio = texto == null ? "" : texto.trim().replaceAll("\\s+", " ");
        if (limpio.isEmpty()) {
            throw new ReglaNegocioException("El nombre y apellidos del cliente son obligatorios.");
        }
        if (limpio.length() > LARGO_MAXIMO_NOMBRE) {
            throw new ReglaNegocioException("El nombre no puede superar los " + LARGO_MAXIMO_NOMBRE + " caracteres.");
        }
        if (!NOMBRE.matcher(limpio).matches()) {
            throw new ReglaNegocioException("El nombre solo puede contener letras, espacios, punto, apóstrofo y guion.");
        }
        return limpio;
    }

    /**
     * Nombre del producto escrito por el vendedor. Si lo deja vacío se usa el nombre de la categoría: hay
     * productos (Spirulina, Omega 3) cuya categoría ya es el producto.
     */
    public static String producto(String texto, String categoria) {
        String limpio = texto == null ? "" : texto.trim().replaceAll("\\s+", " ");
        if (limpio.isEmpty()) {
            return categoria;
        }
        if (limpio.length() > LARGO_MAXIMO_NOMBRE) {
            throw new ReglaNegocioException("El nombre del producto no puede superar los " + LARGO_MAXIMO_NOMBRE + " caracteres.");
        }
        if (!PRODUCTO.matcher(limpio).matches()) {
            throw new ReglaNegocioException("El nombre del producto contiene caracteres no permitidos.");
        }
        return limpio;
    }

    public static String dni(String texto) {
        String limpio = texto == null ? "" : texto.trim();
        if (!DNI.matcher(limpio).matches()) {
            throw new ReglaNegocioException("El DNI debe tener exactamente 8 dígitos.");
        }
        return limpio;
    }

    /** Acepta 9 dígitos (con espacios, guiones o prefijo +51) y los devuelve como «987 654 321». */
    public static String telefono(String texto) {
        String digitos = texto == null ? "" : texto.replaceAll("\\D", "");
        if (digitos.length() == 11 && digitos.startsWith("51")) {
            digitos = digitos.substring(2);
        }
        if (digitos.length() != 9 || digitos.charAt(0) != '9') {
            throw new ReglaNegocioException("El teléfono debe tener 9 dígitos y empezar con 9.");
        }
        return digitos.substring(0, 3) + " " + digitos.substring(3, 6) + " " + digitos.substring(6);
    }
}

package com.homeolive.modelo;

public record Cliente(long id, String nombre, String dni, String telefono, String ubicacion) {

    public String iniciales() {
        StringBuilder sb = new StringBuilder();
        for (String palabra : nombre.trim().split("\\s+")) {
            if (!palabra.isEmpty() && sb.length() < 2) {
                sb.append(palabra.charAt(0));
            }
        }
        return sb.toString().toUpperCase();
    }
}

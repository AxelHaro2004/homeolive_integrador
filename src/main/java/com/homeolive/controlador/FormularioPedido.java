package com.homeolive.controlador;

import com.homeolive.modelo.SolicitudPedido;
import jakarta.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * Conversión de las filas de ítems del formulario (campos repetidos: presentación, producto y cantidad).
 * Se leen directamente de la petición porque el producto es texto libre y puede llevar comas.
 */
final class FormularioPedido {

    private FormularioPedido() {
    }

    static List<SolicitudPedido.Linea> lineas(HttpServletRequest request) {
        String[] presentaciones = valores(request, "presentacion");
        String[] productos = valores(request, "producto");
        String[] cantidades = valores(request, "cantidad");
        List<SolicitudPedido.Linea> lineas = new ArrayList<>();
        for (int i = 0; i < presentaciones.length; i++) {
            String producto = i < productos.length ? productos[i] : "";
            String cantidad = i < cantidades.length ? cantidades[i] : "";
            lineas.add(new SolicitudPedido.Linea(aLong(presentaciones[i]), producto, aEntero(cantidad)));
        }
        return lineas;
    }

    /** JSON de las filas, para volver a pintarlas si el formulario se rechaza. */
    static String json(List<SolicitudPedido.Linea> lineas) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < lineas.size(); i++) {
            SolicitudPedido.Linea l = lineas.get(i);
            if (i > 0) {
                sb.append(',');
            }
            sb.append("{\"id\":").append(l.idPresentacion() == null ? "null" : l.idPresentacion())
                    .append(",\"producto\":\"").append(escapar(l.producto()))
                    .append("\",\"cantidad\":").append(l.cantidad()).append('}');
        }
        return sb.append(']').toString();
    }

    private static String[] valores(HttpServletRequest request, String nombre) {
        String[] valores = request.getParameterValues(nombre);
        return valores == null ? new String[0] : valores;
    }

    private static String escapar(String texto) {
        if (texto == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (char c : texto.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '<' -> sb.append("\\u003c");
                case '>' -> sb.append("\\u003e");
                case '&' -> sb.append("\\u0026");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }

    private static Long aLong(String texto) {
        try {
            return texto == null || texto.isBlank() ? null : Long.valueOf(texto.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int aEntero(String texto) {
        try {
            return texto == null || texto.isBlank() ? 0 : Integer.parseInt(texto.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}

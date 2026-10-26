package com.homeolive.modelo;

/** Datos de un cliente tal como llegan del formulario, antes de validarlos. */
public record DatosCliente(String nombre, String dni, String telefono, String ubicacion) {
}

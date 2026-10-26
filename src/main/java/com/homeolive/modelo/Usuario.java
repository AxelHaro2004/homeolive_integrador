package com.homeolive.modelo;

import com.homeolive.acceso.Rol;

/** Cuenta de acceso (USUARIO) unida al empleado que la usa. Guarda el hash BCrypt, nunca la clave. */
public record Usuario(long id, String username, String correo, String passwordHash, Rol rol,
                      long idEmpleado, String nombreEmpleado) {
}

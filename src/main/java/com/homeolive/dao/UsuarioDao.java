package com.homeolive.dao;

import com.homeolive.acceso.Rol;
import com.homeolive.modelo.Usuario;

import java.util.Optional;

/** Acceso a datos de USUARIO (unido a EMPLEADO). */
public interface UsuarioDao {

    /** Busca por nombre de usuario o por correo, sin distinguir mayúsculas. */
    Optional<Usuario> buscarPorUsernameOCorreo(String identificador);

    boolean existeUsername(String username);

    long insertar(String username, String correo, String passwordHash, Rol rol, long idEmpleado);
}

package com.homeolive.dao;

import com.homeolive.modelo.Cliente;
import com.homeolive.modelo.DatosCliente;

import java.util.List;
import java.util.Optional;

/** Acceso a datos de CLIENTE. Los datos que recibe ya vienen validados por el Model. */
public interface ClienteDao {

    List<Cliente> listar();

    Optional<Cliente> buscarPorId(long id);

    Optional<Cliente> buscarPorDni(String dni);

    long insertar(DatosCliente datos);

    boolean actualizar(long id, DatosCliente datos);

    boolean eliminar(long id);
}

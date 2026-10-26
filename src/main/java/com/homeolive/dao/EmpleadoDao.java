package com.homeolive.dao;

import java.util.OptionalLong;

/** Acceso a datos de EMPLEADO. */
public interface EmpleadoDao {

    OptionalLong buscarIdPorDni(String dni);
}

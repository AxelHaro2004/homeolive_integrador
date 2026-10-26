package com.homeolive.modelo;

import com.homeolive.dao.ClienteDao;
import com.homeolive.dao.PedidoDao;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Reglas de negocio de CLIENTE: datos válidos, DNI único y sin borrar clientes con pedidos. */
@Component
public class ClienteModelo {

    private final ClienteDao clientes;
    private final PedidoDao pedidos;

    public ClienteModelo(ClienteDao clientes, PedidoDao pedidos) {
        this.clientes = clientes;
        this.pedidos = pedidos;
    }

    public List<Cliente> listar() {
        return clientes.listar();
    }

    public Cliente obtener(long id) {
        return clientes.buscarPorId(id)
                .orElseThrow(() -> new ReglaNegocioException("El cliente solicitado no existe."));
    }

    /** Valida y normaliza los datos del formulario (nombre, DNI, teléfono y ubicación). */
    public DatosCliente validar(DatosCliente datos) {
        String ubicacion = Ubicacion.deTexto(datos.ubicacion())
                .orElseThrow(() -> new ReglaNegocioException("Selecciona la ubicación: Lima o Provincia."))
                .etiqueta();
        return new DatosCliente(Validaciones.nombrePersona(datos.nombre()), Validaciones.dni(datos.dni()),
                Validaciones.telefono(datos.telefono()), ubicacion);
    }

    @Transactional
    public Cliente crear(DatosCliente datos) {
        DatosCliente d = validar(datos);
        if (clientes.buscarPorDni(d.dni()).isPresent()) {
            throw dniRepetido(d.dni());
        }
        try {
            long id = clientes.insertar(d);
            return new Cliente(id, d.nombre(), d.dni(), d.telefono(), d.ubicacion());
        } catch (DuplicateKeyException e) {
            throw dniRepetido(d.dni());
        }
    }

    @Transactional
    public Cliente actualizar(long id, DatosCliente datos) {
        DatosCliente d = validar(datos);
        if (clientes.buscarPorId(id).isEmpty()) {
            throw new ReglaNegocioException("El cliente que intenta editar ya no existe.");
        }
        if (clientes.buscarPorDni(d.dni()).filter(otro -> otro.id() != id).isPresent()) {
            throw dniRepetido(d.dni());
        }
        try {
            clientes.actualizar(id, d);
        } catch (DuplicateKeyException e) {
            throw dniRepetido(d.dni());
        }
        return new Cliente(id, d.nombre(), d.dni(), d.telefono(), d.ubicacion());
    }

    @Transactional
    public void eliminar(long id) {
        Cliente cliente = clientes.buscarPorId(id)
                .orElseThrow(() -> new ReglaNegocioException("El cliente que intenta eliminar ya no existe."));
        int total = pedidos.contarPorCliente(id);
        if (total > 0) {
            throw new ReglaNegocioException("No se puede eliminar a «" + cliente.nombre() + "»: tiene " + total
                    + (total == 1 ? " pedido registrado." : " pedidos registrados."));
        }
        clientes.eliminar(id);
    }

    /**
     * Cliente de un pedido: si el DNI ya existe se reutiliza (no se duplica), conservando su nombre y
     * actualizando teléfono y ubicación; si no existe, se crea.
     */
    @Transactional
    public Cliente obtenerOCrearPorDni(DatosCliente datos) {
        DatosCliente d = validar(datos);
        return clientes.buscarPorDni(d.dni()).map(existente -> {
            DatosCliente actualizado = new DatosCliente(existente.nombre(), existente.dni(), d.telefono(), d.ubicacion());
            clientes.actualizar(existente.id(), actualizado);
            return new Cliente(existente.id(), existente.nombre(), existente.dni(), d.telefono(), d.ubicacion());
        }).orElseGet(() -> crear(d));
    }

    private static ReglaNegocioException dniRepetido(String dni) {
        return new ReglaNegocioException("Ya existe un cliente con el DNI " + dni + ".");
    }
}

package com.homeolive.controlador;

import com.homeolive.acceso.Vista;
import com.homeolive.modelo.ClienteModelo;
import com.homeolive.modelo.DatosCliente;
import com.homeolive.modelo.ReglaNegocioException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Vista admin-clientes (administrador): CRUD de clientes. Escritura con POST + redirect (PRG). */
@Controller
public class AdminClienteController {

    private final ClienteModelo clientes;

    public AdminClienteController(ClienteModelo clientes) {
        this.clientes = clientes;
    }

    @GetMapping("/admin-clientes")
    public String listar(Model model) {
        model.addAttribute("vista", Vista.ADMIN_CLIENTES);
        model.addAttribute("clientes", clientes.listar());
        return "admin-clientes";
    }

    @PostMapping("/admin-clientes")
    public String crear(@RequestParam(name = "nombre", defaultValue = "") String nombre,
                        @RequestParam(name = "dni", defaultValue = "") String dni,
                        @RequestParam(name = "telefono", defaultValue = "") String telefono,
                        @RequestParam(name = "ubicacion", defaultValue = "") String ubicacion,
                        RedirectAttributes avisos) {
        try {
            clientes.crear(new DatosCliente(nombre, dni, telefono, ubicacion));
            avisos.addFlashAttribute("mensaje", "Cliente creado correctamente.");
        } catch (ReglaNegocioException e) {
            avisos.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin-clientes";
    }

    @PostMapping("/admin-clientes/{id}/editar")
    public String editar(@PathVariable long id,
                         @RequestParam(name = "nombre", defaultValue = "") String nombre,
                         @RequestParam(name = "dni", defaultValue = "") String dni,
                         @RequestParam(name = "telefono", defaultValue = "") String telefono,
                         @RequestParam(name = "ubicacion", defaultValue = "") String ubicacion,
                         RedirectAttributes avisos) {
        try {
            clientes.actualizar(id, new DatosCliente(nombre, dni, telefono, ubicacion));
            avisos.addFlashAttribute("mensaje", "Cliente actualizado correctamente.");
        } catch (ReglaNegocioException e) {
            avisos.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin-clientes";
    }

    @PostMapping("/admin-clientes/{id}/eliminar")
    public String eliminar(@PathVariable long id, RedirectAttributes avisos) {
        try {
            clientes.eliminar(id);
            avisos.addFlashAttribute("mensaje", "Cliente eliminado correctamente.");
        } catch (ReglaNegocioException e) {
            avisos.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin-clientes";
    }
}

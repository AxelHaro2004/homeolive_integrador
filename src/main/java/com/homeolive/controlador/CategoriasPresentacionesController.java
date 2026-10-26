package com.homeolive.controlador;

import com.homeolive.acceso.Vista;
import com.homeolive.modelo.CategoriaProducto;
import com.homeolive.modelo.CategoriaProductoModelo;
import com.homeolive.modelo.PresentacionModelo;
import com.homeolive.modelo.ReglaNegocioException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Vista categorias-presentaciones (administrador): crear, editar y eliminar (lógicamente) categorías y
 * presentaciones. Las escrituras siguen el patrón POST + redirect (PRG).
 */
@Controller
public class CategoriasPresentacionesController {

    private static final String RUTA = "/categorias-presentaciones";

    private final CategoriaProductoModelo categorias;
    private final PresentacionModelo presentaciones;

    public CategoriasPresentacionesController(CategoriaProductoModelo categorias, PresentacionModelo presentaciones) {
        this.categorias = categorias;
        this.presentaciones = presentaciones;
    }

    @GetMapping(RUTA)
    public String listar(@RequestParam(name = "q", required = false) String filtro, Model model) {
        model.addAttribute("vista", Vista.CATEGORIAS_PRESENTACIONES);
        model.addAttribute("categorias", categorias.listar(filtro));
        model.addAttribute("filtro", filtro == null ? "" : filtro);
        model.addAttribute("totalCategorias", categorias.total());
        model.addAttribute("totalProductos", categorias.totalProductos());
        model.addAttribute("presentaciones", presentaciones.listar());
        model.addAttribute("categoriasActivas", categorias.listar(null));
        model.addAttribute("unidades", PresentacionModelo.UNIDADES.stream().sorted().toList());
        return "categorias-presentaciones";
    }

    /* ---------------------------- Categorías ---------------------------- */

    @PostMapping(RUTA + "/categorias")
    public String crearCategoria(@RequestParam(name = "nombre", required = false) String nombre,
                                 RedirectAttributes avisos) {
        try {
            CategoriaProducto creada = categorias.crear(nombre);
            avisos.addFlashAttribute("mensaje", "Categoría «" + creada.nombre() + "» creada correctamente.");
        } catch (ReglaNegocioException e) {
            avisos.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + RUTA;
    }

    @PostMapping(RUTA + "/categorias/{id}/editar")
    public String editarCategoria(@PathVariable long id, @RequestParam(name = "nombre", required = false) String nombre,
                                  RedirectAttributes avisos) {
        try {
            categorias.actualizar(id, nombre);
            avisos.addFlashAttribute("mensaje", "Categoría actualizada correctamente.");
        } catch (ReglaNegocioException e) {
            avisos.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + RUTA;
    }

    @PostMapping(RUTA + "/categorias/{id}/eliminar")
    public String eliminarCategoria(@PathVariable long id, RedirectAttributes avisos) {
        try {
            categorias.eliminar(id);
            avisos.addFlashAttribute("mensaje", "Categoría eliminada correctamente (con sus presentaciones).");
        } catch (ReglaNegocioException e) {
            avisos.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + RUTA;
    }

    /* -------------------------- Presentaciones -------------------------- */

    @PostMapping(RUTA + "/presentaciones")
    public String crearPresentacion(@RequestParam(name = "categoria", defaultValue = "0") long idCategoria,
                                    @RequestParam(name = "valor", defaultValue = "") String valor,
                                    @RequestParam(name = "unidad", defaultValue = "") String unidad,
                                    @RequestParam(name = "precio", defaultValue = "") String precio,
                                    RedirectAttributes avisos) {
        try {
            var creada = presentaciones.crear(idCategoria, valor, unidad, precio);
            avisos.addFlashAttribute("mensaje", "Presentación " + creada.etiqueta() + " creada en «"
                    + creada.categoria() + "».");
        } catch (ReglaNegocioException e) {
            avisos.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + RUTA;
    }

    @PostMapping(RUTA + "/presentaciones/{id}/editar")
    public String editarPresentacion(@PathVariable long id,
                                     @RequestParam(name = "valor", defaultValue = "") String valor,
                                     @RequestParam(name = "unidad", defaultValue = "") String unidad,
                                     @RequestParam(name = "precio", defaultValue = "") String precio,
                                     RedirectAttributes avisos) {
        try {
            presentaciones.actualizar(id, valor, unidad, precio);
            avisos.addFlashAttribute("mensaje", "Presentación actualizada correctamente.");
        } catch (ReglaNegocioException e) {
            avisos.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + RUTA;
    }

    @PostMapping(RUTA + "/presentaciones/{id}/eliminar")
    public String eliminarPresentacion(@PathVariable long id, RedirectAttributes avisos) {
        try {
            presentaciones.eliminar(id);
            avisos.addFlashAttribute("mensaje", "Presentación eliminada correctamente.");
        } catch (ReglaNegocioException e) {
            avisos.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + RUTA;
    }
}

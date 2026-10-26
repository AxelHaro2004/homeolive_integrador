package com.homeolive.controlador;

import com.homeolive.modelo.Catalogo;
import com.homeolive.modelo.CatalogoModelo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Catálogo en JSON (categoría → producto → presentación) para los desplegables en cascada. */
@RestController
public class CatalogoController {

    private final CatalogoModelo catalogo;

    public CatalogoController(CatalogoModelo catalogo) {
        this.catalogo = catalogo;
    }

    @GetMapping("/catalogo")
    public Catalogo catalogo() {
        return catalogo.arbol();
    }
}

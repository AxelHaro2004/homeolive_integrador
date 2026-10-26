package com.homeolive.modelo;

import com.homeolive.dao.PresentacionDao;
import org.springframework.stereotype.Component;

@Component
public class CatalogoModelo {

    private final PresentacionDao presentaciones;

    public CatalogoModelo(PresentacionDao presentaciones) {
        this.presentaciones = presentaciones;
    }

    /** Categorías con sus presentaciones vigentes (y precio), en el orden en que se muestran al vender. */
    public Catalogo arbol() {
        return Catalogo.desde(presentaciones.listarTodas().stream().filter(Presentacion::activa).toList());
    }
}

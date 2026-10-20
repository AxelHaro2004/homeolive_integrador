package com.homeolive.modelo;

public enum TipoEnvio {

    MOTORIZADO("Motorizado"),
    OLVA("Olva"),
    SHALOM("Shalom");

    private final String etiqueta;

    TipoEnvio(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String etiqueta() {
        return etiqueta;
    }

    public boolean esMotorizado() {
        return this == MOTORIZADO;
    }

    public boolean esCourier() {
        return !esMotorizado();
    }
}

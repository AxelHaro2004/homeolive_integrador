package com.homeolive.modelo;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limita los intentos fallidos de inicio de sesión por usuario: tras {@value #MAXIMO} fallos dentro de la
 * ventana de {@code 15 minutos}, el acceso queda bloqueado hasta que venza la ventana (frena la
 * adivinación de contraseñas por fuerza bruta).
 */
@Component
public class ControlIntentos {

    public static final int MAXIMO = 5;
    private static final Duration VENTANA = Duration.ofMinutes(15);

    private final Clock reloj;
    private final Map<String, Deque<Instant>> fallos = new ConcurrentHashMap<>();

    public ControlIntentos(Clock reloj) {
        this.reloj = reloj;
    }

    public boolean estaBloqueado(String usuario) {
        Deque<Instant> lista = fallos.get(usuario);
        if (lista == null) {
            return false;
        }
        synchronized (lista) {
            depurar(lista);
            return lista.size() >= MAXIMO;
        }
    }

    public void registrarFallo(String usuario) {
        Deque<Instant> lista = fallos.computeIfAbsent(usuario, k -> new ArrayDeque<>());
        synchronized (lista) {
            depurar(lista);
            lista.addLast(Instant.now(reloj));
        }
    }

    public void limpiar(String usuario) {
        fallos.remove(usuario);
    }

    private void depurar(Deque<Instant> lista) {
        Instant limite = Instant.now(reloj).minus(VENTANA);
        while (!lista.isEmpty() && lista.peekFirst().isBefore(limite)) {
            lista.removeFirst();
        }
    }
}

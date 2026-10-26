(function () {
  'use strict';

  var $ = function (selector, root) { return (root || document).querySelector(selector); };
  var $$ = function (selector, root) { return Array.prototype.slice.call((root || document).querySelectorAll(selector)); };

  /* ---------- Menú lateral (móvil) ---------- */
  var sidebar = $('#sidebar');
  var overlay = $('#sidebar-overlay');
  if (sidebar && overlay) {
    var abrirMenu = function () { sidebar.classList.add('sidebar-open'); overlay.hidden = false; };
    var cerrarMenu = function () { sidebar.classList.remove('sidebar-open'); overlay.hidden = true; };
    $$('[data-menu-abrir]').forEach(function (b) { b.addEventListener('click', abrirMenu); });
    overlay.addEventListener('click', cerrarMenu);
    $$('#sidebar a').forEach(function (a) { a.addEventListener('click', cerrarMenu); });
  }

  /* ---------- Catálogo en cascada: categoría → producto → presentación ---------- */
  var arbol = null;
  var esperandoCatalogo = [];

  function cargarCatalogo(alTerminar) {
    if (arbol) { alTerminar(); return; }
    esperandoCatalogo.push(alTerminar);
    if (esperandoCatalogo.length > 1) { return; }
    window.fetch('/catalogo', { credentials: 'same-origin', headers: { 'Accept': 'application/json' } })
        .then(function (r) {
          if (r.redirected || (r.url && r.url.indexOf('/login') !== -1)) {
            throw new Error('La sesión expiró; vuelve a iniciar sesión.');
          }
          if (!r.ok) { throw new Error('El servidor respondió ' + r.status + ' al pedir /catalogo.'); }
          return r.json();
        })
        .then(function (datos) {
          arbol = datos.categorias || [];
          esperandoCatalogo.splice(0).forEach(function (f) { f(); });
        })
        .catch(function (e) {
          console.error('Error cargando el catálogo:', e);
          esperandoCatalogo = [];
          window.alert('No se pudo cargar el catálogo de productos. Recarga la página.\n\nDetalle: ' + (e && e.message ? e.message : e));
        });
  }

  function opcion(valor, texto) {
    var o = document.createElement('option');
    o.value = valor;
    o.textContent = texto;
    return o;
  }
  function vaciar(select, textoInicial) {
    select.innerHTML = '';
    select.appendChild(opcion('', textoInicial));
  }
  function campo(fila, nombre) { return $('[data-campo="' + nombre + '"]', fila); }

  function poblarCategorias(fila) {
    var s = campo(fila, 'categoria');
    vaciar(s, 'Selecciona categoría');
    arbol.forEach(function (c, i) { s.appendChild(opcion(String(i), c.nombre)); });
    if (arbol.length === 1) { s.selectedIndex = 1; }
    poblarPresentaciones(fila);
  }
  function poblarPresentaciones(fila) {
    var categoria = arbol[campo(fila, 'categoria').value];
    var s = campo(fila, 'presentacion');
    vaciar(s, categoria ? 'Selecciona presentación' : 'Selecciona categoría');
    if (categoria) {
      categoria.presentaciones.forEach(function (p) {
        s.appendChild(opcion(String(p.id), p.etiqueta + ' · S/ ' + Number(p.precio).toFixed(2)));
      });
      if (categoria.presentaciones.length === 1) { s.selectedIndex = 1; }
    }
  }
  function seleccionarPresentacion(fila, idPresentacion) {
    for (var c = 0; c < arbol.length; c++) {
      for (var k = 0; k < arbol[c].presentaciones.length; k++) {
        if (arbol[c].presentaciones[k].id === idPresentacion) {
          campo(fila, 'categoria').value = String(c);
          poblarPresentaciones(fila);
          campo(fila, 'presentacion').value = String(idPresentacion);
          return;
        }
      }
    }
  }

  document.addEventListener('change', function (e) {
    var select = e.target.closest ? e.target.closest('select[data-campo]') : null;
    if (!select || !arbol) { return; }
    var fila = select.closest('[data-fila-catalogo]');
    if (!fila) { return; }
    if (select.dataset.campo === 'categoria') { poblarPresentaciones(fila); }
  });

  function repoblarFilasCatalogo(raiz) {
    if (!arbol) { return; }
    $$('[data-fila-catalogo]', raiz).forEach(poblarCategorias);
  }
  if ($('[data-fila-catalogo]')) {
    cargarCatalogo(function () { repoblarFilasCatalogo(document); document.dispatchEvent(new Event('catalogo-listo')); });
  }

  /* ---------- Provincia solo permite courier (Olva o Shalom) ---------- */
  function acoplarEnvio(contenedor) {
    var ubicacion = $('[name="ubicacion"]', contenedor);
    var envio = $('[name="envio"]', contenedor);
    if (!ubicacion || !envio) { return; }
    var aplicar = function () {
      var motorizado = $$('option', envio).filter(function (o) { return o.value === 'Motorizado' || o.textContent === 'Motorizado'; })[0];
      var provincia = ubicacion.value === 'Provincia';
      if (motorizado) {
        motorizado.hidden = provincia;
        motorizado.disabled = provincia;
      }
      if (provincia && envio.value === 'Motorizado') { envio.value = 'Olva'; }
    };
    ubicacion.addEventListener('change', aplicar);
    aplicar();
  }
  $$('form').forEach(acoplarEnvio);

  /* ---------- Modales ---------- */
  function abrirModal(boton) {
    var modal = document.getElementById(boton.getAttribute('data-modal-abrir'));
    if (!modal) { return; }
    var form = $('form', modal);
    if (form) {
      form.reset();
      // Formularios con acción base: al editar se envía a <base>/<id>/<sufijo> (por omisión «editar»).
      if (form.dataset.base) {
        var sufijo = form.dataset.sufijo || 'editar';
        form.setAttribute('action', boton.dataset.id
            ? form.dataset.base + '/' + boton.dataset.id + '/' + sufijo
            : form.dataset.base);
      }
    }
    repoblarFilasCatalogo(modal);
    Object.keys(boton.dataset).forEach(function (clave) {
      if (clave === 'modalAbrir') { return; }
      var valor = boton.dataset[clave];
      var campoForm = $('[name="' + clave + '"]', modal);
      if (campoForm) { campoForm.value = valor; }
      $$('[data-slot="' + clave + '"]', modal).forEach(function (el) { el.textContent = valor; });
    });
    var editando = !!boton.dataset.id;
    $$('[data-solo-crear]', modal).forEach(function (el) { el.style.display = editando ? 'none' : ''; });
    $$('[data-solo-editar]', modal).forEach(function (el) { el.style.display = editando ? '' : 'none'; });
    var ubicacion = $('[name="ubicacion"]', modal);
    if (ubicacion) { ubicacion.dispatchEvent(new Event('change')); }
    modal.hidden = false;
    var primero = $('input:not([readonly]):not([type="checkbox"]), select', modal);
    if (primero) { primero.focus(); }
  }
  function cerrarModal(modal) { modal.hidden = true; }

  $$('[data-modal-abrir]').forEach(function (boton) {
    boton.addEventListener('click', function () { abrirModal(boton); });
  });
  $$('.modal-backdrop').forEach(function (modal) {
    modal.addEventListener('mousedown', function (e) { if (e.target === modal) { cerrarModal(modal); } });
    $$('[data-modal-cerrar]', modal).forEach(function (b) { b.addEventListener('click', function () { cerrarModal(modal); }); });
    var form = $('form', modal);
    if (form && form.getAttribute('data-enviar') !== 'real') {
      form.addEventListener('submit', function (e) { e.preventDefault(); cerrarModal(modal); });
    }
  });
  document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape') { $$('.modal-backdrop').forEach(cerrarModal); }
  });

  /* ---------- Buscador de tablas ---------- */
  $$('input[data-filtro]').forEach(function (input) {
    var tabla = document.getElementById(input.getAttribute('data-filtro'));
    if (!tabla) { return; }
    input.addEventListener('input', function () {
      var texto = input.value.trim().toLowerCase();
      $$('tbody tr', tabla).forEach(function (fila) {
        if (fila.querySelector('.empty-cell')) { return; }
        fila.hidden = texto !== '' && fila.textContent.toLowerCase().indexOf(texto) === -1;
      });
    });
  });

  /* ---------- Avisos y confirmaciones ---------- */
  $$('[data-aviso]').forEach(function (b) {
    b.addEventListener('click', function () { window.alert(b.getAttribute('data-aviso')); });
  });
  $$('[data-confirmar]').forEach(function (b) {
    b.addEventListener('click', function (event) {
      if (!window.confirm(b.getAttribute('data-confirmar'))) {
        event.preventDefault();
        return;
      }
      // Los formularios y botones de envío dejan que el navegador envíe el formulario.
      if (b.tagName === 'FORM' || (b.tagName === 'BUTTON' && b.type === 'submit')) { return; }
      var fila = b.closest('tr');
      if (fila) { fila.remove(); }
    });
  });

  /* ---------- Filtros que se aplican al cambiar (mes y año de los resúmenes) ---------- */
  $$('select[data-autoenviar]').forEach(function (select) {
    select.addEventListener('change', function () { select.form.submit(); });
  });

  /* ---------- Selector de cliente (recarga con ?cliente=ID) ---------- */
  $$('select[data-navegar]').forEach(function (select) {
    select.addEventListener('change', function () {
      var url = new URL(window.location.href);
      url.searchParams.set(select.getAttribute('data-navegar'), select.value);
      window.location.href = url.toString();
    });
  });

  /* ---------- Preparación de pedidos (químico): el pedido se libera con todos los ítems listos ---------- */
  $$('[data-prep-form]').forEach(function (form) {
    var tarjeta = form.closest('.work-order');
    var boton = $('[data-prep-boton]', form);
    var items = $$('[data-prep-item]', tarjeta);
    var revisar = function () {
      var todos = items.every(function (i) { return i.classList.contains('prep-done'); });
      boton.disabled = !todos;
      boton.title = todos ? '' : 'Marca todos los ítems como preparados para continuar';
    };
    items.forEach(function (item) {
      item.addEventListener('click', function () { item.classList.toggle('prep-done'); revisar(); });
    });
    revisar();
  });

  /* ---------- Registro de pedidos ---------- */
  var formPedido = $('#form-pedido');
  if (formPedido) {
    var lista = $('#lista-items');
    var contador = $('#contador-items');

    var actualizarItems = function () {
      var filas = $$('.item-row', lista);
      filas.forEach(function (fila, i) {
        $('.item-index', fila).textContent = ('0' + (i + 1)).slice(-2);
        $('.remove-item', fila).disabled = filas.length === 1;
      });
      contador.textContent = filas.length + (filas.length === 1 ? ' ítem' : ' ítems') + ' en el pedido';
    };

    var agregarFila = function () {
      var copia = $('.item-row', lista).cloneNode(true);
      $$('input', copia).forEach(function (i) { i.value = i.type === 'number' ? '1' : ''; });
      lista.appendChild(copia);
      if (arbol) { poblarCategorias(copia); }
      actualizarItems();
      return copia;
    };

    $('#agregar-item').addEventListener('click', agregarFila);

    lista.addEventListener('click', function (e) {
      var boton = e.target.closest('.remove-item');
      if (boton && !boton.disabled) {
        boton.closest('.item-row').remove();
        actualizarItems();
      }
    });

    $('#dni').addEventListener('input', function (e) { e.target.value = e.target.value.replace(/\D/g, ''); });

    // Si el servidor rechazó el formulario, se vuelven a pintar los ítems que ya había elegido.
    document.addEventListener('catalogo-listo', function () {
      var previas = [];
      try { previas = JSON.parse(formPedido.dataset.lineas || '[]'); } catch (e) { previas = []; }
      previas.forEach(function (linea, i) {
        var fila = i === 0 ? $('.item-row', lista) : agregarFila();
        if (linea.id !== null) { seleccionarPresentacion(fila, linea.id); }
        $('input[name="producto"]', fila).value = linea.producto || '';
        $('input[name="cantidad"]', fila).value = linea.cantidad > 0 ? linea.cantidad : 1;
      });
      actualizarItems();
    });

    actualizarItems();
  }
})();

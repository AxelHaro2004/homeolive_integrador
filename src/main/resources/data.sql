INSERT INTO categoria_producto (nombre_categoria) VALUES ('Flores de Bach');
INSERT INTO categoria_producto (nombre_categoria) VALUES ('Homeopático');
INSERT INTO categoria_producto (nombre_categoria) VALUES ('Spirulina');
INSERT INTO categoria_producto (nombre_categoria) VALUES ('Omega 3');

INSERT INTO categoria_presentacion (id_categoria, presentacion, unidad_medida, precio_unitario)
SELECT id_categoria, 30, 'ml', 42.00 FROM categoria_producto WHERE nombre_categoria = 'Flores de Bach';
INSERT INTO categoria_presentacion (id_categoria, presentacion, unidad_medida, precio_unitario)
SELECT id_categoria, 60, 'ml', 58.00 FROM categoria_producto WHERE nombre_categoria = 'Homeopático';
INSERT INTO categoria_presentacion (id_categoria, presentacion, unidad_medida, precio_unitario)
SELECT id_categoria, 120, 'ml', 85.00 FROM categoria_producto WHERE nombre_categoria = 'Homeopático';
INSERT INTO categoria_presentacion (id_categoria, presentacion, unidad_medida, precio_unitario)
SELECT id_categoria, 45, 'gr', 36.00 FROM categoria_producto WHERE nombre_categoria = 'Spirulina';
INSERT INTO categoria_presentacion (id_categoria, presentacion, unidad_medida, precio_unitario)
SELECT id_categoria, 60, 'cápsulas', 49.00 FROM categoria_producto WHERE nombre_categoria = 'Spirulina';
INSERT INTO categoria_presentacion (id_categoria, presentacion, unidad_medida, precio_unitario)
SELECT id_categoria, 60, 'cápsulas', 49.00 FROM categoria_producto WHERE nombre_categoria = 'Omega 3';

INSERT INTO empleado (nombre_empleado, dni_empleado, cargo, telefono_empleado) VALUES ('Andrea Ramírez Soto',  '40125836', 'vendedor',             '911 000 001');
INSERT INTO empleado (nombre_empleado, dni_empleado, cargo, telefono_empleado) VALUES ('Diego Alarcón Vega',   '41236947', 'agente_finanzas',      '911 000 002');
INSERT INTO empleado (nombre_empleado, dni_empleado, cargo, telefono_empleado) VALUES ('Camila Ríos Herrera',  '42347058', 'quimico_farmaceutico', '911 000 003');
INSERT INTO empleado (nombre_empleado, dni_empleado, cargo, telefono_empleado) VALUES ('Jorge Palacios Nuñez', '43458169', 'gestor_envios',        '911 000 004');
INSERT INTO empleado (nombre_empleado, dni_empleado, cargo, telefono_empleado) VALUES ('Lucía Ramírez Castro', '44569270', 'administracion',      '911 000 005');

-- Solo se controlan los materiales medibles por unidades; cinta, papel burbuja y papel film no se controlan.
INSERT INTO insumo_empaquetado (nombre, stock, unidad) VALUES ('Caja brandeada', 200, 'unds.');
INSERT INTO insumo_empaquetado (nombre, stock, unidad) VALUES ('Sticker de frágil', 500, 'unds.');

# Guía de formato del folleto — Rizoma

Rizoma sigue armando el folleto con estilos como siempre (lindo para los clientes).
Estas pocas reglas garantizan que la app lo pueda **importar sin errores**.
Ejemplo con los estilos ya aplicados: [`plantilla_folleto_estilado.xlsx`](plantilla_folleto_estilado.xlsx).

## 1) El color define el nivel
- **Categoría** → relleno **rojo**
- **Marca / sub-sección** → relleno **naranja**
- **Producto** → sin relleno

🔑 **Regla de oro:** si una fila **no** tiene precio, **tiene** que tener color (es un título). Nunca un título en blanco.

## 2) Precio siempre con `$`
- Anteponer siempre `$` (nunca un número suelto).
- Un solo formato de miles, ej: `$10.300`, `$1.200`.
- Cuidado con la letra `O` en vez del cero (`$251O` ❌ → `$2510` ✔).

## 3) Todo producto bajo su categoría
- Que no queden productos sueltos sin un banner de categoría (rojo) arriba.
- Si empieza una sección nueva, ponele su banner rojo.

## 4) Un precio por celda / notas aparte
- Cada presentación en su columna: `250 cc $..` | `500 cc $..` | `1 litro $..`.
- No poner dos precios separados por `/` en la misma celda (la fracción `1/2 kg` está bien).
- Notas (origen, "sin stock", "envase vidrio", sabores) en su propia celda, **sin** `$`.

## Red de seguridad
Al subir el folleto al panel admin, la app muestra una **previsualización**: cuántas
categorías/productos entendió y una lista de **avisos** (filas sin precio, títulos sin
color, precios sin `$`, etc.). Revisá los avisos **antes de confirmar** la importación.

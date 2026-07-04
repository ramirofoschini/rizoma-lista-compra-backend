# Rizoma — Backend de pedidos

API REST del sistema de pedidos de **Rizoma** (bio almacén orgánico). Expone el catálogo,
recibe los pedidos y ofrece un panel admin con carga masiva por Excel.

## Stack
- **Spring Boot 4.1** · Java 21
- **PostgreSQL** (Neon en producción) + **Flyway** (migraciones + seed del catálogo)
- **Apache POI** para la carga masiva del Excel normalizado
- Spring Security (Basic Auth para el panel admin)

## Requisitos
- JDK 21+
- PostgreSQL 14+ (o una base en [Neon](https://neon.tech))

## Configuración (variables de entorno)
| Variable | Default | Descripción |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/rizoma` | URL JDBC de Postgres |
| `DB_USER` / `DB_PASSWORD` | `rizoma` / `rizoma` | Credenciales de la base |
| `ADMIN_USER` / `ADMIN_PASSWORD` | `admin` / `rizoma-admin` | **Cambiar en prod.** Login del panel |
| `CORS_ORIGINS` | `http://localhost:4200` | Orígenes del frontend (separados por coma) |
| `WHATSAPP_DESTINO` | `5492215346770` | WhatsApp de Rizoma (formato internacional sin `+`) |
| `PORT` | `8080` | Puerto (Render lo inyecta automáticamente) |

## Correr en local
```bash
# 1) Crear la base
createdb rizoma           # o crearla en Neon y exportar DB_URL/DB_USER/DB_PASSWORD

# 2) Levantar (Flyway crea el esquema y carga los 751 productos)
./mvnw spring-boot:run
```
La API queda en `http://localhost:8080`.

## Endpoints
### Público
- `GET  /api/catalogo` — catálogo agrupado por categoría (solo activos)
- `GET  /api/config` — WhatsApp destino + días de entrega
- `POST /api/pedidos` — crear pedido `{ cliente, diaEntrega, items:[{presentacionId, cantidad}] }`
- `GET  /api/pedidos/{id}` — ver un pedido

### Admin (Basic Auth)
- `GET    /api/admin/productos` — todos (incl. inactivos)
- `POST   /api/admin/productos` — alta
- `PUT    /api/admin/productos/{id}` — edición
- `DELETE /api/admin/productos/{id}` — baja lógica
- `POST   /api/admin/import` (multipart `file`) — carga masiva del Excel normalizado

## Base de datos
- `V1__schema.sql` — esquema (producto, presentacion, cliente, pedido, pedido_item)
- `V2__seed_catalogo.sql` — catálogo inicial (generado desde el Excel normalizado)
- Los pedidos guardan **snapshot** de nombre/etiqueta/precio: cambiar precios no altera pedidos viejos.

## Deploy en Render
1. Web Service → build `./mvnw -DskipTests clean package`, start `java -jar target/pedidos-0.0.1-SNAPSHOT.jar`.
2. Base de datos en Neon; setear `DB_URL`, `DB_USER`, `DB_PASSWORD`.
3. Setear `ADMIN_PASSWORD`, `CORS_ORIGINS` (URL del frontend) y `WHATSAPP_DESTINO`.

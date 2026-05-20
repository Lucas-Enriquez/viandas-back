# Guia para probar Viandas API localmente con curl

Esta guia asume que vas a correr el backend en `http://localhost:8080` y que tenes PostgreSQL local.

En Windows, usa `curl.exe` para evitar el alias de PowerShell. En Linux/macOS podes usar `curl`.

## 1. Variables de entorno

Configura estas variables antes de levantar la app. Si corres desde IntelliJ, ponelas en `Run Configuration > Environment variables`.

```text
DB_URL=jdbc:postgresql://localhost:5432/viandas
DB_USERNAME=postgres
DB_PASSWORD=<tu-password-postgres>

VIANDAS_BOOTSTRAP_KEY=dev-bootstrap-key
VIANDAS_JWT_SECRET=dev-secret-change-me-dev-secret-change-me
VIANDAS_JWT_EXPIRATION_MINUTES=15
VIANDAS_REFRESH_EXPIRATION_DAYS=30
VIANDAS_PUBLIC_BASE_URL=http://localhost:8080

# Vacio para probar Google Login en modo dev.
GOOGLE_CLIENT_ID=

# Opcionales
VIANDAS_NEARBY_THRESHOLD_METERS=600
VIANDAS_DELIVERY_SESSION_TTL_MINUTES=180
```

Para tests:

```text
TEST_DB_URL=jdbc:postgresql://localhost:5432/viandas_test
TEST_DB_USERNAME=postgres
TEST_DB_PASSWORD=<tu-password-postgres>
```

Notas:

- `VIANDAS_BOOTSTRAP_KEY` protege `POST /internal/bootstrap/cook`.
- `VIANDAS_JWT_SECRET` debe ser largo y privado en entornos reales.
- `VIANDAS_JWT_EXPIRATION_MINUTES` define cuanto dura el access token.
- `VIANDAS_REFRESH_EXPIRATION_DAYS` define cuanto dura el refresh token.
- `VIANDAS_PUBLIC_BASE_URL` se usa para generar el link que despues compartiria la app.
- Si `GOOGLE_CLIENT_ID` esta vacio, `/auth/google` acepta tokens dev como `dev:cliente@test.com:Cliente Demo`.
- Si configuras `GOOGLE_CLIENT_ID`, `/auth/google` valida el ID token contra Google.

## 2. Base de datos

Crea las bases si no existen:

```bash
psql -U postgres -c "CREATE DATABASE viandas;"
psql -U postgres -c "CREATE DATABASE viandas_test;"
```

Si Flyway ya corrio una version vieja de `V1` en una DB local de prueba, lo mas simple es recrear la DB `viandas` antes de volver a levantar la app.

## 3. Levantar la API

```bash
mvn spring-boot:run
```

En los ejemplos uso:

```text
BASE=http://localhost:8080
MENU_DATE=2026-04-29
```

Si usas Windows, reemplaza `curl` por `curl.exe`.

## 4. Crear cook inicial

```bash
curl -s -X POST "http://localhost:8080/internal/bootstrap/cook" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Cook Demo",
    "email": "cook@viandas.test",
    "password": "secret123",
    "bootstrapKey": "dev-bootstrap-key"
  }'
```

La respuesta trae `accessToken` y `refreshToken`. Copia el `accessToken` para las siguientes llamadas:

```text
COOK_TOKEN=<accessToken de la respuesta>
COOK_REFRESH_TOKEN=<refreshToken de la respuesta>
```

Si el cook ya existe, logueate:

```bash
curl -s -X POST "http://localhost:8080/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "cook@viandas.test",
    "password": "secret123"
  }'
```

Renovar sesion:

```bash
curl -s -X POST "http://localhost:8080/auth/refresh" \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "COOK_REFRESH_TOKEN"
  }'
```

Cerrar sesion:

```bash
curl -s -X POST "http://localhost:8080/auth/logout" \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "COOK_REFRESH_TOKEN"
  }'
```

## 5. Crear empresa con ubicacion

```bash
curl -s -X POST "http://localhost:8080/companies" \
  -H "Authorization: Bearer COOK_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Acme",
    "address": "Av. Corrientes 1234, CABA",
    "notes": "Entrada por recepcion",
    "latitude": -34.6037,
    "longitude": -58.3816,
    "locationSource": "MANUAL",
    "whatsappGroupLabel": "Grupo Acme"
  }'
```

Reemplaza `COOK_TOKEN` por el token real. La respuesta trae `id` y `slug`.

```text
COMPANY_ID=<id de la empresa>
COMPANY_SLUG=<slug de la empresa, por ejemplo acme>
```

Probar geocoding preview:

```bash
curl -s -X POST "http://localhost:8080/companies/COMPANY_ID/location/geocode-preview" \
  -H "Authorization: Bearer COOK_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "address": "Av. Corrientes 1234, CABA"
  }'
```

Por ahora debe responder `NOT_CONFIGURED`, porque el geocoder real quedo atras de un adapter.

## 6. Crear menu

```bash
curl -s -X POST "http://localhost:8080/menus" \
  -H "Authorization: Bearer COOK_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "companyId": COMPANY_ID,
    "date": "2026-04-29",
    "orderClosesAt": "23:30:00"
  }'
```

La respuesta trae `id`.

```text
MENU_ID=<id del menu>
```

## 7. Agregar items al menu

```bash
curl -s -X POST "http://localhost:8080/menus/MENU_ID/items" \
  -H "Authorization: Bearer COOK_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Churrasquito de cerdo con pure mixto",
    "price": 9500,
    "category": "PLATO",
    "remainingStock": 10
  }'
```

```bash
curl -s -X POST "http://localhost:8080/menus/MENU_ID/items" \
  -H "Authorization: Bearer COOK_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Milanesa de peceto con guarnicion",
    "price": 10000,
    "category": "MINUTA",
    "remainingStock": 8
  }'
```

Categorias validas: `PLATO`, `MINUTA`, `ENSALADA`.

Guarda los IDs devueltos:

```text
ITEM_1_ID=<id del primer item>
ITEM_2_ID=<id del segundo item>
```

## 8. Publicar menu y obtener texto para compartir

```bash
curl -s -X PATCH "http://localhost:8080/menus/MENU_ID/publish" \
  -H "Authorization: Bearer COOK_TOKEN"
```

La respuesta trae:

```json
{
  "publicLinkId": 1,
  "publicUrl": "http://localhost:8080/m/acme/2026-04-29?t=<token>",
  "whatsappText": "..."
}
```

El `publicUrl` representa la URL futura de la PWA. Para probar el backend directamente, usa `/public/menus`.

Guarda el token de query string:

```text
PUBLIC_TOKEN=<valor de t en publicUrl>
```

Ver menu publico:

```bash
curl -s -X GET "http://localhost:8080/public/menus/COMPANY_SLUG/2026-04-29?t=PUBLIC_TOKEN"
```

## 9. Login de cliente con Google en modo dev

Con `GOOGLE_CLIENT_ID` vacio:

```bash
curl -s -X POST "http://localhost:8080/auth/google" \
  -H "Content-Type: application/json" \
  -d '{
    "idToken": "dev:cliente@viandas.test:Cliente Demo"
  }'
```

La respuesta trae `accessToken` y `refreshToken`.

```text
CUSTOMER_TOKEN=<accessToken del cliente>
CUSTOMER_REFRESH_TOKEN=<refreshToken del cliente>
```

## 10. Crear pedido publico

```bash
curl -s -X POST "http://localhost:8080/public/menus/COMPANY_SLUG/2026-04-29/orders?t=PUBLIC_TOKEN" \
  -H "Authorization: Bearer CUSTOMER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "items": [
      {
        "menuItemId": ITEM_1_ID,
        "quantity": 1,
        "comment": "Sin sal"
      },
      {
        "menuItemId": ITEM_2_ID,
        "quantity": 1,
        "comment": "Con papas"
      }
    ]
  }'
```

La respuesta trae `id`.

```text
ORDER_ID=<id del pedido>
```

Consultar estado actual desde la PWA/navegador:

```bash
curl -s -X GET "http://localhost:8080/public/menus/COMPANY_SLUG/2026-04-29/orders/current?t=PUBLIC_TOKEN" \
  -H "Authorization: Bearer CUSTOMER_TOKEN"
```

## 11. Ver pedidos del cook

```bash
curl -s -X GET "http://localhost:8080/orders/today" \
  -H "Authorization: Bearer COOK_TOKEN"
```

Abrir stream SSE en otra terminal:

```bash
curl -N "http://localhost:8080/orders/stream?companyId=COMPANY_ID" \
  -H "Authorization: Bearer COOK_TOKEN"
```

Luego crea otro pedido o cambia un estado para ver eventos.

## 12. Cambiar estado del pedido

```bash
curl -s -X PATCH "http://localhost:8080/orders/ORDER_ID/preparing" \
  -H "Authorization: Bearer COOK_TOKEN"
```

```bash
curl -s -X PATCH "http://localhost:8080/orders/ORDER_ID/out-for-delivery" \
  -H "Authorization: Bearer COOK_TOKEN"
```

```bash
curl -s -X PATCH "http://localhost:8080/orders/ORDER_ID/delivered" \
  -H "Authorization: Bearer COOK_TOKEN"
```

Otros endpoints:

```text
PATCH /orders/{id}/cancel
```

## 13. Probar tracking de reparto

Iniciar sesion de reparto:

```bash
curl -s -X POST "http://localhost:8080/delivery-sessions" \
  -H "Authorization: Bearer COOK_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "companyId": COMPANY_ID,
    "menuId": MENU_ID
  }'
```

La respuesta trae `id`.

```text
DELIVERY_ID=<id de la sesion de reparto>
```

Enviar ubicacion. El backend redondea coordenadas y nunca expone lat/lng al cliente:

```bash
curl -s -X PATCH "http://localhost:8080/delivery-sessions/DELIVERY_ID/location" \
  -H "Authorization: Bearer COOK_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "latitude": -34.6040,
    "longitude": -58.3820,
    "accuracyMeters": 25
  }'
```

Consultar estado del cliente otra vez:

```bash
curl -s -X GET "http://localhost:8080/public/menus/COMPANY_SLUG/2026-04-29/orders/current?t=PUBLIC_TOKEN" \
  -H "Authorization: Bearer CUSTOMER_TOKEN"
```

Finalizar reparto:

```bash
curl -s -X POST "http://localhost:8080/delivery-sessions/DELIVERY_ID/finish" \
  -H "Authorization: Bearer COOK_TOKEN"
```

## 14. Registrar token PWA push

Esto no envia push real todavia; guarda el device token y el servicio de notificaciones queda como no-op local.

```bash
curl -s -X POST "http://localhost:8080/me/notification-devices" \
  -H "Authorization: Bearer CUSTOMER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "token": "fake-web-push-token",
    "platform": "WEB"
  }'
```

## 15. Stock broadcast

```bash
curl -s -X POST "http://localhost:8080/stock-broadcast" \
  -H "Authorization: Bearer COOK_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "companyId": COMPANY_ID,
    "menuId": MENU_ID,
    "availableItemIds": [ITEM_1_ID, ITEM_2_ID],
    "message": "Ya salimos"
  }'
```

## 16. Ejecutar tests

```bash
mvn -q test
```

Si falla con autenticacion de PostgreSQL, revisa:

```text
TEST_DB_URL
TEST_DB_USERNAME
TEST_DB_PASSWORD
```

Tambien podes correr solo compilacion:

```bash
mvn -q -DskipTests compile
```

## 17. Flujo minimo esperado

1. Crear cook.
2. Crear empresa con direccion/coordenadas.
3. Crear menu e items.
4. Publicar menu.
5. Copiar `whatsappText` y `publicUrl`.
6. Cliente hace login Google dev.
7. Cliente crea pedido publico.
8. Cook ve pedido por `GET /orders/today` o SSE.
9. Cook inicia reparto y manda ubicacion.
10. Cliente consulta `/orders/current` y ve estado aproximado.

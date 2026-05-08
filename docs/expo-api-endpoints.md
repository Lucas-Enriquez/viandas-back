# Viandas API para Expo

Documentacion orientada a construir las pantallas mobile en Expo.

Base local sugerida:

```txt
http://localhost:8080
```

En celular fisico, `localhost` apunta al telefono. Usar la IP LAN de la PC, por ejemplo:

```txt
http://192.168.0.10:8080
```

## Convenciones

Todos los endpoints devuelven `ApiResponse<T>`, excepto `GET /orders/stream`, que usa Server-Sent Events.

```ts
type ApiResponse<T> = {
  success: boolean;
  data: T | null;
  message: string;
  errors: ApiError[] | null;
  meta?: Record<string, unknown> | null;
};

type ApiError = {
  field?: string | null;
  message: string;
};
```

Autenticacion:

```http
Authorization: Bearer {accessToken}
```

Formatos:

| Tipo | Formato |
|---|---|
| UUID | string, ejemplo `018f0a7b-9b6d-7c8e-9f01-23456789abcd` |
| LocalDate | `YYYY-MM-DD`, ejemplo `2026-05-05` |
| LocalTime | `HH:mm:ss`, ejemplo `11:30:00` |
| Instant | ISO UTC, ejemplo `2026-05-05T14:30:00Z` |
| Money | number decimal, ejemplo `6500.00` |

Roles:

```ts
type UserRole = "COOK" | "EMPLOYEE" | "CUSTOMER";
type MenuScope = "COMPANY" | "GLOBAL";
type MenuStatus = "DRAFT" | "PUBLISHED";
type MenuItemCategory = "PLATO" | "MINUTA" | "ENSALADA";
type LocationSource = "MANUAL" | "GEOCODED";
type OrderStatus =
  | "RECEIVED"
  | "PREPARING"
  | "OUT_FOR_DELIVERY"
  | "NEARBY"
  | "DELIVERED"
  | "CANCELLED";
type DeliverySessionStatus = "ACTIVE" | "FINISHED" | "EXPIRED";
type DeliveryPublicSignal =
  | "OUT_FOR_DELIVERY"
  | "NEARBY"
  | "DELIVERED"
  | "UNKNOWN";
```

## Tipos Compartidos

```ts
type AuthUserResponse = {
  id: string;
  name: string;
  email: string;
  role: UserRole;
};

type AuthResponse = {
  accessToken: string;
  refreshToken: string;
  user: AuthUserResponse;
};

type CompanyResponse = {
  id: string;
  name: string;
  slug: string;
  address: string | null;
  notes: string | null;
  latitude: number | null;
  longitude: number | null;
  locationSource: LocationSource | null;
  whatsappGroupLabel: string | null;
};

type UserContextResponse = {
  user: AuthUserResponse;
  company: {
    id: string;
    name: string;
    slug: string;
  } | null;
};

type MenuCompanyResponse = {
  id: string;
  name: string;
  slug: string;
};

type MenuItemResponse = {
  id: string;
  name: string;
  price: number;
  category: MenuItemCategory;
  photoUrl: string | null;
  remainingStock: number | null;
  availableCompanyIds: string[];
};

type MenuResponse = {
  id: string;
  companyId: string | null;
  companyName: string | null;
  scope: MenuScope;
  companies: MenuCompanyResponse[];
  date: string;
  status: MenuStatus;
  orderClosesAt: string;
  items: MenuItemResponse[];
};

type PublicMenuResponse = {
  id: string;
  companyName: string;
  companySlug: string;
  date: string;
  orderClosesAt: string;
  canOrder: boolean;
  items: MenuItemResponse[];
};

type OrderItemResponse = {
  menuItemId: string;
  name: string;
  unitPrice: number;
  quantity: number;
  comment: string | null;
};

type OrderResponse = {
  id: string;
  companyId: string;
  menuId: string;
  status: OrderStatus;
  deliverySignal: DeliveryPublicSignal;
  customerName: string;
  totalAmount: number;
  createdAt: string;
  items: OrderItemResponse[];
};

type CurrentOrderResponse = {
  hasOrder: boolean;
  canOrder: boolean;
  message: string;
  order: OrderResponse | null;
};
```

## Manejo De Errores

Ejemplo de validacion:

```json
{
  "success": false,
  "data": null,
  "message": "One or more request fields are invalid",
  "errors": [
    {
      "field": "email",
      "message": "Debe ingresar un email valido"
    }
  ],
  "meta": {
    "status": 400,
    "path": "/auth/login",
    "timestamp": "2026-05-05T14:30:00Z"
  }
}
```

Codigos comunes:

| Codigo | Uso |
|---|---|
| 400 | Body invalido, enum invalido, request incompleto |
| 401 | Falta token, token invalido, credenciales invalidas |
| 403 | Rol incorrecto o acceso denegado |
| 404 | Recurso inexistente |
| 405 | Metodo HTTP incorrecto |
| 409 | Conflicto de negocio: email repetido, pedido duplicado, stock insuficiente |
| 500 | Error inesperado |

## Auth

### POST `/auth/login`

Publico. Login con email/password para `COOK` y `EMPLOYEE`.

No usar para `CUSTOMER`; customer entra por Google.

Request:

```json
{
  "email": "cook@mail.com",
  "password": "password123"
}
```

Response `ApiResponse<AuthResponse>`:

```json
{
  "success": true,
  "data": {
    "accessToken": "jwt",
    "refreshToken": "opaque-refresh-token",
    "user": {
      "id": "uuid",
      "name": "Lucas",
      "email": "cook@mail.com",
      "role": "COOK"
    }
  },
  "message": "Sesion iniciada",
  "errors": null,
  "meta": null
}
```

### POST `/auth/google`

Publico. Login/registro de `CUSTOMER` con Google.

Request:

```json
{
  "idToken": "google-id-token"
}
```

Response: `ApiResponse<AuthResponse>`.

Reglas:

- Crea `CUSTOMER` si no existe.
- Si el email existe para `COOK` o `EMPLOYEE`, responde `409`.

### POST `/auth/refresh`

Publico. Renueva sesion. El refresh token rota: el viejo deja de servir.

Request:

```json
{
  "refreshToken": "opaque-refresh-token"
}
```

Response: `ApiResponse<AuthResponse>`.

Uso en Expo:

1. Si un request responde `401`, intentar `/auth/refresh`.
2. Guardar el nuevo `accessToken` y el nuevo `refreshToken`.
3. Reintentar el request original.
4. Si refresh falla, limpiar storage y mandar a login.

### POST `/auth/logout`

Publico. Revoca el refresh token enviado.

Request:

```json
{
  "refreshToken": "opaque-refresh-token"
}
```

Response:

```json
{
  "success": true,
  "data": null,
  "message": "Sesion cerrada",
  "errors": null,
  "meta": null
}
```

### POST `/internal/bootstrap/cook`

**Solo disponible con perfil `dev`** (no existe en producción). Crea un `COOK`.

Request:

```json
{
  "name": "Cook Demo",
  "email": "cook@mail.com",
  "password": "password123",
  "bootstrapKey": "dev-bootstrap-key"
}
```

Response: `ApiResponse<AuthResponse>`.

## Me / Contexto

### GET `/me/context`

Auth requerida. Devuelve el usuario logueado y contexto de empresa.

Response `ApiResponse<UserContextResponse>`:

```json
{
  "success": true,
  "data": {
    "user": {
      "id": "uuid",
      "name": "Empleado Demo",
      "email": "empleado@mail.com",
      "role": "EMPLOYEE"
    },
    "company": {
      "id": "uuid",
      "name": "Reino Ceramicos S.A",
      "slug": "reino-ceramicos-s-a"
    }
  },
  "message": "Contexto obtenido",
  "errors": null,
  "meta": null
}
```

Reglas:

- `EMPLOYEE`: `company` viene desde `CompanyMembership`.
- `COOK` y `CUSTOMER`: `company` es `null`.
- Si `EMPLOYEE` no tiene empresa, responde error.
- Si `EMPLOYEE` tuviera mas de una empresa, responde `409`.

## Companies

Todos requieren auth de `COOK`.

### GET `/companies`

Lista las empresas del cook.

Response: `ApiResponse<CompanyResponse[]>`.

### POST `/companies`

Crea empresa.

Request:

```json
{
  "name": "Reino Ceramicos S.A",
  "address": "Av Agustin M Garcia 9501",
  "notes": "Entrada por Diego Palma a la izquierda",
  "latitude": -34.3921711,
  "longitude": -58.6627832,
  "locationSource": "MANUAL",
  "whatsappGroupLabel": "Reino Caseritas"
}
```

Response: `ApiResponse<CompanyResponse>`.

Validaciones:

- `name` requerido.
- `latitude`: entre -90 y 90.
- `longitude`: entre -180 y 180.
- `locationSource`: `MANUAL` o `GEOCODED`.

### GET `/companies/{id}`

Obtiene una empresa del cook.

Response: `ApiResponse<CompanyResponse>`.

### PATCH `/companies/{id}`

Actualiza datos generales de la empresa.

Request: igual a `POST /companies`.

Response: `ApiResponse<CompanyResponse>`.

### PATCH `/companies/{id}/location`

Actualiza solo ubicacion.

Request:

```json
{
  "address": "Av Agustin M Garcia 9501",
  "latitude": -34.3921711,
  "longitude": -58.6627832,
  "locationSource": "MANUAL"
}
```

Response: `ApiResponse<CompanyResponse>`.

### POST `/companies/{id}/location/geocode-preview`

Preview de geocoding. Hoy responde `NOT_CONFIGURED` con lista vacia.

Request:

```json
{
  "address": "Av Agustin M Garcia 9501"
}
```

Response:

```ts
type GeocodePreviewResponse = {
  status: string;
  candidates: {
    formattedAddress: string;
    latitude: number;
    longitude: number;
    provider: string;
  }[];
};
```

### DELETE `/companies/{id}`

Elimina la empresa y **todos sus datos relacionados** de forma permanente: pedidos, menúes, sesiones de reparto, invitaciones, membresías, etc. No hay forma de deshacer esta accion.

El front debe pedir confirmacion al usuario antes de llamar este endpoint (por ejemplo, un input donde escriba "eliminar").

Response: `204 No Content`.

Errores posibles:
- `403` si la empresa no le pertenece al cook autenticado.
- `404` si no existe.

## Invitations

### POST `/companies/{id}/invitations`

Auth `COOK`. Crea una invitacion individual para una empresa.

Request:

```json
{
  "email": "empleado@mail.com"
}
```

Response:

```ts
type InvitationResponse = {
  token: string;  // token opaco (string), no UUID
  email: string;
  expiresAt: string;
  link: string;
};
```

Reglas:

- Dura 72 horas.
- Es de un solo uso.
- El email del accept debe coincidir con el email invitado.
- El `token` es un string opaco. Usarlo tal cual en la URL.

### GET `/invitations/{token}`

Publico. Preview de invitacion individual. `{token}` es string opaco.

Response:

```ts
type InvitationValidationResponse = {
  companyName: string;
  email: string;
  expiresAt: string;
};
```

### POST `/invitations/{token}/accept`

Publico. Registra un `EMPLOYEE`, lo asocia a la empresa y lo deja logueado. `{token}` es string opaco.

Request:

```json
{
  "name": "Empleado Demo",
  "email": "empleado@mail.com",
  "password": "password123"
}
```

Response: `ApiResponse<AuthResponse>`.

Errores de negocio:

- `409` si el email ya existe.
- `409` si la invitacion ya fue usada o vencio.
- `400` si el email no coincide con la invitacion.

### GET `/companies/{id}/global-invitation`

Auth `COOK`. Devuelve el estado de la invitacion global activa de la empresa, sin crear una nueva.
Util para mostrar en la pantalla de empresa cuántos la usaron y si sigue activa.

Response: `ApiResponse<GlobalInvitationPreviewResponse>`.

- `404` si no hay ninguna activa.

### POST `/companies/{id}/global-invitation`

Auth `COOK`. Crea (o renueva) el link global de la empresa. Revoca el anterior si existia.
El `token` plain solo aparece en este response — es la unica vez que se puede ver.
Guardarlo en el front para poder compartirlo despues.

Request:

```json
{
  "maxUses": 20
}
```

`maxUses` puede ser `null` (sin limite).

Response:

```ts
type GlobalInvitationResponse = {
  token: string;
  company: string;
  expiresAt: string;
  link: string;   // = {publicBaseUrl}/global-invitation/{token}
};
```

Reglas:

- Dura 365 dias por defecto (configurable en el servidor).
- Si ya habia una invitacion global activa para esa empresa, se revoca.
- El `link` es el que se comparte con los empleados para que se registren.
- **Guardar el `link` del response**: no hay forma de recuperar el token despues.

### GET `/global-invitation/{token}`

Publico. Preview de invitacion global. Mostrar antes de que el empleado acepte.

Response:

```ts
type GlobalInvitationPreviewResponse = {
  company: string;
  expiresAt: string;
  usable: boolean;
  maxUses: number | null;
  usedCount: number;
};
```

### POST `/global-invitation/{token}/accept`

Publico. Registra un `EMPLOYEE`, lo asocia a la empresa y lo deja logueado.

Request:

```json
{
  "name": "Empleado Demo",
  "email": "empleado@mail.com",
  "password": "password123"
}
```

Response: `ApiResponse<AuthResponse>`.

Reglas:

- Si `maxUses` se alcanza, la invitacion queda inactiva.
- Si el email ya existe, responde `409`.
- No convierte `CUSTOMER` a `EMPLOYEE`.

## Menus

### GET `/menus`

Auth `COOK`. Lista menus del cook.

Query params opcionales:

| Param | Tipo |
|---|---|
| `companyId` | UUID |
| `date` | `YYYY-MM-DD` |

Response: `ApiResponse<MenuResponse[]>`.

### POST `/menus`

Auth `COOK`. Crea menu.

Menu por empresa:

```json
{
  "scope": "COMPANY",
  "companyId": "uuid",
  "date": "2026-05-05",
  "orderClosesAt": "11:30:00"
}
```

Tambien funciona sin `scope`, porque `COMPANY` es default:

```json
{
  "companyId": "uuid",
  "date": "2026-05-05",
  "orderClosesAt": "11:30:00"
}
```

Menu global:

```json
{
  "scope": "GLOBAL",
  "companyIds": ["uuid-empresa-1", "uuid-empresa-2"],
  "date": "2026-05-05",
  "orderClosesAt": "11:30:00"
}
```

Response: `ApiResponse<MenuResponse>`.

Reglas:

- Menu `COMPANY`: maximo uno por empresa/fecha.
- Menu `GLOBAL`: maximo uno por cook/fecha.
- En `GLOBAL`, `companyIds` es obligatorio.

### POST `/menus/{id}/clone`

Auth `COOK`. Crea un nuevo menu en `DRAFT` copiando los items de uno existente.
Pensado para el flujo diario: clonar el menu de ayer, ajustar lo que cambio, publicar.

Request:

```json
{
  "date": "2026-05-08",
  "orderClosesAt": "11:30:00"
}
```

- `date` requerido.
- `orderClosesAt` opcional. Si se omite, se hereda del menu original.

Response: `ApiResponse<MenuResponse>` — el nuevo menu en `DRAFT` con todos los items copiados.

Que se copia:

- Todos los items: nombre, precio, categoria, foto, stock, empresas disponibles por item.
- Para menus `GLOBAL`: las empresas asignadas al menu.
- Para menus `COMPANY`: la misma empresa.

Que NO se copia:

- Status (siempre `DRAFT`).
- Links publicos.
- Pedidos.

Errores de negocio:

- `409` si ya existe un menu para esa empresa/fecha (`COMPANY`) o para ese cook/fecha (`GLOBAL`).

### POST `/menus/{id}/items`

Auth `COOK`. Agrega item al menu.

Request:

```json
{
  "name": "Milanesa con pure",
  "price": 6500,
  "category": "PLATO",
  "photoUrl": "https://cdn.app/milanesa.jpg",
  "remainingStock": 20,
  "availableCompanyIds": ["uuid-empresa-1"]
}
```

Response: `ApiResponse<MenuItemResponse>`.

Reglas:

- `availableCompanyIds` solo aplica a menus `GLOBAL`.
- En menu `GLOBAL`, si `availableCompanyIds` es `null` o `[]`, el item aplica a todas las empresas asignadas al menu.
- `remainingStock` es compartido entre todas las empresas.

### PATCH `/menus/{id}/publish`

Auth `COOK`. Publica menu y genera link publico.

Response:

```ts
type ShareMessageResponse = {
  publicLinkId: string;
  publicUrl: string;
  whatsappText: string;
};
```

Links generados:

| Scope | URL publica |
|---|---|
| `COMPANY` | `{publicBaseUrl}/m/{companySlug}/{date}?t={token}` |
| `GLOBAL` | `{publicBaseUrl}/m/global/{date}?t={token}` |

Para consumir desde API:

| Scope | Endpoint API |
|---|---|
| `COMPANY` | `GET /public/menus/{companySlug}/{date}?t={token}` |
| `GLOBAL` | `GET /employee/menus/global/{date}?t={token}` |

### GET `/menus/{id}/share-message`

Auth `COOK`. Devuelve el mensaje/link de un menu ya publicado.

Response: `ApiResponse<ShareMessageResponse>`.

Si el menu no esta publicado, responde `409`.

### GET `/public/menus/{companySlug}/{date}?t={token}`

Publico. Obtiene menu publicado por empresa.

Response: `ApiResponse<PublicMenuResponse>`.

Uso:

- Pantalla de menu por link de empresa.
- El menu puede verse sin auth.
- Para pedir, el usuario debe estar autenticado como `CUSTOMER`.

### GET `/employee/menus/global/{date}?t={token}`

Auth `EMPLOYEE`. Obtiene menu global publicado.

Response: `ApiResponse<PublicMenuResponse>`.

Reglas:

- El empleado debe pertenecer a una empresa.
- La empresa del empleado debe estar asignada al menu global.
- Los items se filtran por `availableCompanyIds`.

## Orders

### POST `/public/menus/{companySlug}/{date}/orders?t={token}`

Auth `CUSTOMER`. Crea pedido sobre menu por empresa.

Request:

```json
{
  "items": [
    {
      "menuItemId": "uuid-item",
      "quantity": 1,
      "comment": "Sin sal"
    }
  ]
}
```

Response: `ApiResponse<OrderResponse>`.

Reglas:

- Solo `CUSTOMER`.
- Un customer puede tener un solo pedido activo por menu.
- Si el horario de cierre paso, responde `409`.
- Si no hay stock suficiente, responde `409`.

### GET `/public/menus/{companySlug}/{date}/orders/current?t={token}`

Auth `CUSTOMER`. Obtiene el pedido actual del customer para ese menu.

Response: `ApiResponse<CurrentOrderResponse>`.

### POST `/employee/menus/global/{date}/orders?t={token}`

Auth `EMPLOYEE`. Crea pedido sobre menu global.

Request:

```json
{
  "items": [
    {
      "menuItemId": "uuid-item",
      "quantity": 1,
      "comment": null
    }
  ]
}
```

Response: `ApiResponse<OrderResponse>`.

Reglas:

- El backend resuelve `companyId` desde `CompanyMembership`.
- Un empleado puede tener un solo pedido activo por menu.
- El item debe estar disponible para la empresa del empleado.

### GET `/employee/menus/global/{date}/orders/current?t={token}`

Auth `EMPLOYEE`. Obtiene pedido actual del empleado para ese menu global.

Response: `ApiResponse<CurrentOrderResponse>`.

### GET `/orders/today`

Auth `COOK`. Lista pedidos de hoy del cook.

Response: `ApiResponse<OrderResponse[]>`.

### GET `/orders/stream`

Auth `COOK`. Server-Sent Events para recibir cambios en pedidos, delivery y stock.

Query opcional:

| Param | Tipo | Uso |
|---|---|---|
| `companyId` | UUID | Escuchar una empresa concreta |

Eventos posibles:

| Evento | Payload |
|---|---|
| `order.created` | `OrderResponse` |
| `order.updated` | `OrderResponse` |
| `delivery.started` | `DeliverySessionResponse` |
| `delivery.location` | `DeliverySessionResponse` |
| `delivery.finished` | `DeliverySessionResponse` |
| `stock.broadcast` | `StockBroadcastResponse` |

Ejemplo Expo/web con `EventSource` o polyfill SSE:

```ts
const stream = new EventSource(`${API_URL}/orders/stream`, {
  headers: { Authorization: `Bearer ${accessToken}` },
});

stream.addEventListener("order.created", (event) => {
  const order = JSON.parse(event.data) as OrderResponse;
});
```

### PATCH `/orders/{id}/preparing`

Auth `COOK`. Marca pedido como `PREPARING`.

Response: `ApiResponse<OrderResponse>`.

### PATCH `/orders/{id}/out-for-delivery`

Auth `COOK`. Marca pedido como `OUT_FOR_DELIVERY`.

Response: `ApiResponse<OrderResponse>`.

### PATCH `/orders/{id}/delivered`

Auth `COOK`. Marca pedido como `DELIVERED`.

Response: `ApiResponse<OrderResponse>`.

### PATCH `/orders/{id}/cancel`

Auth `COOK`. Marca pedido como `CANCELLED`.

Response: `ApiResponse<OrderResponse>`.

### POST `/stock-broadcast`

Auth `COOK`. Avisa disponibilidad de stock para una empresa/menu.

Request:

```json
{
  "companyId": "uuid-empresa",
  "menuId": "uuid-menu",
  "availableItemIds": ["uuid-item-1", "uuid-item-2"],
  "message": "Quedan disponibles estos platos"
}
```

Response:

```ts
type StockBroadcastResponse = {
  id: string;
  sentAt: string;
  availableItemIds: string[];
};
```

Reglas:

- Si el menu es global, `companyId` debe ser una empresa asignada a ese menu.
- Los items deben pertenecer al menu y estar disponibles para esa empresa.

## Delivery

Todos requieren auth `COOK`.

### POST `/delivery-sessions`

Inicia reparto para una empresa/menu.

Request:

```json
{
  "companyId": "uuid-empresa",
  "menuId": "uuid-menu"
}
```

Response:

```ts
type DeliverySessionResponse = {
  id: string;
  companyId: string;
  menuId: string;
  status: DeliverySessionStatus;
  publicSignal: DeliveryPublicSignal;
  startedAt: string;
  finishedAt: string | null;
  expiresAt: string;
  lastLocationAt: string | null;
};
```

Reglas:

- Marca pedidos abiertos como `OUT_FOR_DELIVERY`.
- Para menu global, la empresa debe estar asignada al menu.
- La sesion expira por config, default 180 minutos.

### PATCH `/delivery-sessions/{id}/location`

Actualiza ubicacion del reparto.

Request:

```json
{
  "latitude": -34.3921711,
  "longitude": -58.6627832,
  "accuracyMeters": 15
}
```

Response: `ApiResponse<DeliverySessionResponse>`.

Reglas:

- Redondea coordenadas a 3 decimales antes de persistir.
- Si esta dentro del umbral configurado, `publicSignal = NEARBY`.
- Si esta cerca, marca pedidos abiertos como `NEARBY`.

### POST `/delivery-sessions/{id}/finish`

Finaliza reparto.

Response: `ApiResponse<DeliverySessionResponse>`.

## Notification Devices

### POST `/me/notification-devices`

Auth requerida para cualquier rol. Registra/actualiza el token push del dispositivo.

Request:

```json
{
  "token": "expo-or-fcm-token",
  "platform": "ios"
}
```

Response:

```ts
type DeviceResponse = {
  id: string;
  platform: string;
  lastSeenAt: string;
};
```

Nota actual: el backend guarda dispositivos, pero el envio FCM real esta como noop/log.

## Flujos Expo Sugeridos

### Flujo Diario Del COOK (caso de uso principal)

Este es el flujo que el cook ejecuta cada mañana para las ~40 empresas.

**1. Obtener el menu de ayer para clonar**

```
GET /menus?date={ayer}
```

Buscar el menu global con `scope = "GLOBAL"` de la fecha anterior.
Guardar su `id`.

**2. Clonar al dia de hoy**

```
POST /menus/{idDeAyer}/clone
{ "date": "2026-05-08" }
```

Responde el nuevo menu en `DRAFT` con todos los items.
Mostrar pantalla de edicion donde el cook puede:
- Cambiar precios.
- Cambiar stock (`remainingStock`).
- Agregar items nuevos (`POST /menus/{id}/items`).
- El scope, las empresas asignadas y las empresas por item ya vienen copiadas.

**3. Publicar**

```
PATCH /menus/{id}/publish
```

Responde `ShareMessageResponse`:

```ts
{
  publicLinkId: string;
  publicUrl: string;    // link para empleados: {base}/m/global/{date}?t={token}
  whatsappText: string; // texto listo para pegar en WhatsApp
}
```

**Guardar el `publicUrl` y el `whatsappText`** en el estado local para el paso siguiente.

**4. Enviar a cada grupo de WhatsApp**

El backend genera el texto una sola vez por publicacion.
El front muestra una pantalla con la lista de las empresas asignadas al menu y un boton
"Copiar mensaje" por empresa. Cada empresa tiene su propio share message porque el link
incluye el `companySlug`:

- Menu `GLOBAL`: todos comparten la misma URL (`/m/global/{date}?t=...`).
  El cook copia el mismo texto para todos los grupos.
- Menu `COMPANY`: cada empresa tiene URL distinta (`/m/{slug}/{date}?t=...`).
  Si necesita links por empresa, usar `GET /menus/{id}/share-message` por cada una.

Para obtener el mensaje en cualquier momento despues de publicar:

```
GET /menus/{id}/share-message
```

**5. Monitorear pedidos**

```
GET /orders/today          // listado inicial
GET /orders/stream         // SSE tiempo real
PATCH /orders/{id}/...     // cambiar estado
```

---

### Flujo COOK (setup inicial — solo una vez por empresa)

1. Login: `POST /auth/login`.
2. Guardar `accessToken` y `refreshToken`.
3. Contexto: `GET /me/context`.
4. Crear empresas: `POST /companies` (una vez por empresa).
5. Generar link de registro para cada empresa:
   ```
   POST /companies/{id}/global-invitation
   { "maxUses": null }
   ```
   **Guardar el `link` del response** — es la unica vez que se puede ver el token.
   Este link dura 365 dias y sirve para todos los ingresos nuevos.
6. Crear el primer menu global:
   ```
   POST /menus
   { "scope": "GLOBAL", "companyIds": [...], "date": "...", "orderClosesAt": "11:30:00" }
   ```
   Agregar items: `POST /menus/{id}/items`.
   Publicar: `PATCH /menus/{id}/publish`.
   A partir del dia siguiente usar el flujo de clone.

### Flujo COOK (eliminar empresa)

Accion destructiva e irreversible. Elimina la empresa y todo lo asociado: pedidos, menus, invitaciones, membresías, sesiones de reparto, etc.

1. El cook elige una empresa desde la lista: `GET /companies`.
2. El front muestra un modal de confirmacion con un input donde debe escribir `"eliminar"` antes de habilitar el boton.
3. Al confirmar:
   ```
   DELETE /companies/{id}
   ```
   Responde `204 No Content`.
4. Si responde `204`: remover la empresa del estado local y navegar de vuelta al listado.
5. Si responde `403` o `404`: mostrar error — la empresa no existe o no le pertenece.

**Importante:** la comprobacion del texto "eliminar" es responsabilidad del front. El backend no valida ninguna confirmacion; simplemente ejecuta el borrado si el cook es el dueno de la empresa.

### Flujo EMPLOYEE Con Link Global

El empleado recibe el link de su empresa (guardado por el cook en el setup inicial).
El link tiene la forma: `{publicBaseUrl}/global-invitation/{token}`.

1. Abrir la app con ese link.
2. Preview: `GET /global-invitation/{token}` — mostrar nombre de empresa y estado.
3. Si `usable = true`, mostrar formulario de registro.
4. Registro: `POST /global-invitation/{token}/accept`.
5. Guardar `accessToken` y `refreshToken`.
6. Contexto: `GET /me/context` — confirma empresa asignada.
7. El cook le comparte el link del menu del dia: `/m/global/{date}?t={menuToken}`.
8. Menu: `GET /employee/menus/global/{date}?t={menuToken}`.
9. Pedido actual: `GET /employee/menus/global/{date}/orders/current?t={menuToken}`.
10. Si puede pedir: `POST /employee/menus/global/{date}/orders?t={menuToken}`.
11. Registrar push: `POST /me/notification-devices`.

Nota: el link de registro (`/global-invitation/{token}`) y el link del menu (`/m/global/...`)
son dos links distintos con tokens distintos. El primero es para registrarse, el segundo es diario.

### Flujo EMPLOYEE Con Invitacion Individual

1. Abrir link `/invite/{token}` (token es string opaco, no UUID).
2. Preview: `GET /invitations/{token}`.
3. Registro: `POST /invitations/{token}/accept`.
4. Guardar tokens.
5. Contexto: `GET /me/context`.
6. Continuar con flujo de menu global cuando el cook comparta el link diario.

### Flujo CUSTOMER

1. Login: `POST /auth/google`.
2. Guardar tokens.
3. Abrir link de menu empresa `/m/{companySlug}/{date}?t={token}`.
4. Menu: `GET /public/menus/{companySlug}/{date}?t={token}`.
5. Pedido actual: `GET /public/menus/{companySlug}/{date}/orders/current?t={token}`.
6. Si puede pedir: `POST /public/menus/{companySlug}/{date}/orders?t={token}`.
7. Registrar push: `POST /me/notification-devices`.

## Pantallas Minimas Para Expo

| Rol | Pantalla | Endpoints |
|---|---|---|
| Todos | Splash/session restore | `/auth/refresh`, `/me/context` |
| COOK/EMPLOYEE | Login email/password | `/auth/login` |
| CUSTOMER | Login Google | `/auth/google` |
| EMPLOYEE | Preview invitacion individual | `GET /invitations/{token}` |
| EMPLOYEE | Preview invitacion global | `GET /global-invitation/{token}` |
| EMPLOYEE | Registro por invitacion | `POST /invitations/{token}/accept`, `POST /global-invitation/{token}/accept` |
| COOK | Empresas | `GET /companies` |
| COOK | Detalle/editar empresa | `GET /companies/{id}`, `PATCH /companies/{id}`, `PATCH /companies/{id}/location` |
| COOK | Crear empresa | `POST /companies` |
| COOK | Confirmar eliminacion empresa | `DELETE /companies/{id}` (requiere confirmacion en front) |
| COOK | Invitaciones | `GET /companies/{id}/global-invitation`, `POST /companies/{id}/global-invitation`, `POST /companies/{id}/invitations` |
| COOK | Menus | `GET /menus`, `POST /menus`, `POST /menus/{id}/clone`, `POST /menus/{id}/items`, `PATCH /menus/{id}/publish` |
| COOK | Pedidos de hoy | `GET /orders/today`, `GET /orders/stream` |
| COOK | Estado pedido | `PATCH /orders/{id}/preparing`, `/out-for-delivery`, `/delivered`, `/cancel` |
| COOK | Reparto | `/delivery-sessions` |
| EMPLOYEE | Menu global | `GET /employee/menus/global/{date}` |
| EMPLOYEE | Pedido global | `GET/POST /employee/menus/global/{date}/orders...` |
| CUSTOMER | Menu empresa | `GET /public/menus/{companySlug}/{date}` |
| CUSTOMER | Pedido empresa | `GET/POST /public/menus/{companySlug}/{date}/orders...` |

## Notas De Implementacion Para El Front

- Guardar `accessToken` y `refreshToken` en storage seguro.
- El `accessToken` dura poco; no asumir que una sesion rota implica login manual.
- Nunca mandar `companyId` desde el front para empleado en pedidos globales: lo resuelve el backend.
- Para links compartidos, parsear siempre `date` y `t` desde la URL.
- Para menus globales, el usuario debe estar logueado como `EMPLOYEE` antes de pedir el menu.
- Para menus por empresa, el usuario puede ver el menu sin login, pero para pedir debe estar logueado como `CUSTOMER`.
- Todos los identificadores son UUID string.
- El token de invitacion global (`GlobalInvitationResponse.token`) solo se expone una vez al crearlo.
  Guardarlo junto con el `link`. No hay endpoint para recuperarlo despues — solo se puede regenerar
  (lo que invalida el anterior).
- Los tokens de invitacion (global e individual) son strings opacos, no UUIDs. Tratarlos como strings.
- El flujo diario del cook usa `POST /menus/{id}/clone` para arrancar con base, no `POST /menus` desde cero.
  El menu clonado sale en `DRAFT`; publicar con `PATCH /menus/{id}/publish` cuando este listo.
- `DELETE /companies/{id}` es irreversible y borra absolutamente todo lo asociado a la empresa.
  El front debe exigir confirmacion explicita (ej: input con el texto "eliminar") antes de llamar el endpoint.
  El backend no valida ningun texto de confirmacion — la proteccion es responsabilidad del front.

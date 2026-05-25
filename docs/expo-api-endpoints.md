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
type UserRole = "COOK" | "EMPLOYEE";
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
      "name": "Acme S.A",
      "slug": "acme-s-a"
    }
  },
  "message": "Contexto obtenido",
  "errors": null,
  "meta": null
}
```

Reglas:

- `EMPLOYEE`: `company` viene desde `CompanyMembership`.
- `COOK`: `company` es `null`.
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
  "name": "Acme S.A",
  "address": "Av Agustin M Garcia 9501",
  "notes": "Entrada por Diego Palma a la izquierda",
  "latitude": -34.3921711,
  "longitude": -58.6627832,
  "locationSource": "MANUAL",
  "whatsappGroupLabel": "Grupo Acme"
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
Pensado para el flujo diario: clonar el ultimo menu del cook, ajustar lo que cambio, publicar.

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

Auth `COOK`. Publica el menu.

Response:

```ts
type ShareMessageResponse = {
  publicUrl: string;    // deep link a la app: {base}/m/{scope}/{date}
  whatsappText: string; // texto listo para pegar en WhatsApp
};
```

URLs generadas:

| Scope | URL |
|---|---|
| `COMPANY` | `{publicBaseUrl}/m/{companySlug}/{date}` |
| `GLOBAL` | `{publicBaseUrl}/m/global/{date}` |

El link no lleva token. El empleado que lo abre debe estar logueado en la app.

### GET `/menus/{id}/share-message`

Auth `COOK`. Devuelve el mensaje del menu publicado (mismo formato que `publish`).

Response: `ApiResponse<ShareMessageResponse>`.

Si el menu no esta publicado, responde `409`.

### GET `/employee/menus/{date}`

Auth `EMPLOYEE`. Obtiene el menu publicado de la empresa del empleado para esa fecha.

Response: `ApiResponse<PublicMenuResponse>`.

Reglas:

- El empleado debe pertenecer a una empresa (via `CompanyMembership`).
- El backend resuelve tanto menus `COMPANY` como `GLOBAL` asignados a la empresa del empleado.
- Los items se filtran por `availableCompanyIds`.
- Si no hay menu publicado para esa fecha, responde `404`.

## Orders

### POST `/employee/menus/{date}/orders`

Auth `EMPLOYEE`. Crea pedido para el menu del dia.

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

- El backend resuelve menu y empresa desde la membresia del empleado.
- Un empleado puede tener un solo pedido activo por menu.
- Si el horario de cierre paso, responde `409`.
- Si no hay stock suficiente, responde `409`.
- El item debe estar disponible para la empresa del empleado.

### GET `/employee/menus/{date}/orders/current`

Auth `EMPLOYEE`. Obtiene el pedido actual del empleado para ese dia.

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

Auth requerida. Registra/actualiza el token push del dispositivo.

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

**1. Obtener el ultimo menu del cook para clonar**

```
GET /menus
```

Sin filtros: devuelve los menus del cook ordenados por `menuDate DESC, id DESC`.
Tomar el primer resultado — es el ultimo menu creado (puede ser de ayer, del viernes, o de hace mas tiempo si hubo un fin de semana o feriado).
Guardar su `id`. Conviene mostrar en la UI la fecha de ese menu (`menuDate`) para que el cook sepa de donde sale el clon.

**2. Clonar al dia de hoy**

```
POST /menus/{idDelUltimo}/clone
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
  publicUrl: string;    // deep link: {base}/m/global/{date}
  whatsappText: string; // texto listo para pegar en WhatsApp
}
```

**Guardar el `whatsappText`** para el paso siguiente.

**4. Enviar a cada grupo de WhatsApp**

El texto generado incluye los items del menu y el link a la app (sin token).
El empleado que toca el link debe estar logueado — la app lo lleva directo al menu del dia.

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

### Flujo EMPLOYEE (registro)

El empleado recibe el link de su empresa (guardado por el cook en el setup inicial).
El link tiene la forma: `{publicBaseUrl}/global-invitation/{token}`.

1. Abrir la app con ese link.
2. Preview: `GET /global-invitation/{token}` — mostrar nombre de empresa y estado.
3. Si `usable = true`, mostrar formulario de registro.
4. Registro: `POST /global-invitation/{token}/accept`.
5. Guardar `accessToken` y `refreshToken`.
6. Contexto: `GET /me/context` — confirma empresa asignada.
7. Registrar push: `POST /me/notification-devices`.

### Flujo EMPLOYEE (ver menu y pedir)

Una vez registrado, el empleado puede ver el menu del dia directamente desde la app, sin necesitar ningun link con token.

1. Menu del dia: `GET /employee/menus/{date}`.
   - `date` = fecha de hoy en formato `YYYY-MM-DD`.
   - Responde `404` si el cook no publico menu para ese dia.
2. Pedido actual: `GET /employee/menus/{date}/orders/current`.
3. Si `canOrder = true`: `POST /employee/menus/{date}/orders`.

El cook puede seguir compartiendo el `whatsappText` por WhatsApp como aviso, pero el link
incluido es un deep link a la app — no lleva token ni requiere nada especial para abrirlo.

### Flujo EMPLOYEE Con Invitacion Individual

1. Abrir link `/invite/{token}` (token es string opaco, no UUID).
2. Preview: `GET /invitations/{token}`.
3. Registro: `POST /invitations/{token}/accept`.
4. Guardar tokens.
5. Contexto: `GET /me/context`.
6. Continuar con flujo de menu (ver seccion anterior).

## Pantallas Minimas Para Expo

| Rol | Pantalla | Endpoints |
|---|---|---|
| Todos | Splash/session restore | `/auth/refresh`, `/me/context` |
| COOK/EMPLOYEE | Login email/password | `/auth/login` |
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
| EMPLOYEE | Menu del dia | `GET /employee/menus/{date}` |
| EMPLOYEE | Pedido del dia | `GET /employee/menus/{date}/orders/current`, `POST /employee/menus/{date}/orders` |

## Notas De Implementacion Para El Front

- Guardar `accessToken` y `refreshToken` en storage seguro.
- El `accessToken` dura poco; no asumir que una sesion rota implica login manual.
- Nunca mandar `companyId` desde el front para empleado en pedidos: lo resuelve el backend desde la membresia.
- Para navegar al menu del dia desde el link de WhatsApp, parsear solo `date` desde la URL (ya no hay `t` token).
- El empleado debe estar logueado antes de acceder al menu — no hay vista publica sin auth.
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

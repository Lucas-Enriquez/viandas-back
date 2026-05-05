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

Publico, solo dev/internal. Crea un `COOK`.

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
  token: string;
  email: string;
  expiresAt: string;
  link: string;
};
```

Reglas:

- Dura 72 horas.
- Es de un solo uso.
- El email del accept debe coincidir con el email invitado.

### GET `/invitations/{token}`

Publico. Preview de invitacion individual.

Response:

```ts
type InvitationValidationResponse = {
  companyName: string;
  email: string;
  expiresAt: string;
};
```

### POST `/invitations/{token}/accept`

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

Errores de negocio:

- `409` si el email ya existe.
- `409` si la invitacion ya fue usada o vencio.
- `400` si el email no coincide con la invitacion.

### POST `/companies/{id}/global-invitation`

Auth `COOK`. Crea un link global para que varios empleados de una misma empresa se registren.

Request:

```json
{
  "maxUses": 20
}
```

`maxUses` puede ser `null`.

Response:

```ts
type GlobalInvitationResponse = {
  token: string;
  company: string;
  expiresAt: string;
  link: string;
};
```

Reglas:

- Dura 72 horas.
- Si ya habia una invitacion global activa para esa empresa, se revoca.
- El `link` apunta a `/global-invitation/{token}` para que Expo muestre preview antes de aceptar.

### GET `/global-invitation/{token}`

Publico. Preview de invitacion global.

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

### Flujo COOK

1. Login: `POST /auth/login`.
2. Guardar `accessToken` y `refreshToken`.
3. Contexto: `GET /me/context`.
4. Empresas:
   - listar `GET /companies`;
   - crear `POST /companies`;
   - editar `PATCH /companies/{id}`.
5. Invitaciones:
   - individual `POST /companies/{id}/invitations`;
   - global `POST /companies/{id}/global-invitation`.
6. Menus:
   - listar `GET /menus`;
   - crear menu company/global `POST /menus`;
   - agregar items `POST /menus/{id}/items`;
   - publicar `PATCH /menus/{id}/publish`.
7. Pedidos:
   - listado inicial `GET /orders/today`;
   - tiempo real `GET /orders/stream`;
   - cambiar estado con endpoints `PATCH /orders/{id}/...`.
8. Reparto:
   - iniciar `POST /delivery-sessions`;
   - actualizar ubicacion `PATCH /delivery-sessions/{id}/location`;
   - finalizar `POST /delivery-sessions/{id}/finish`.

### Flujo EMPLOYEE Con Link Global

1. Abrir link `/global-invitation/{token}`.
2. Preview: `GET /global-invitation/{token}`.
3. Registro: `POST /global-invitation/{token}/accept`.
4. Guardar tokens.
5. Contexto: `GET /me/context`.
6. Abrir link de menu global `/m/global/{date}?t={token}`.
7. Menu: `GET /employee/menus/global/{date}?t={token}`.
8. Pedido actual: `GET /employee/menus/global/{date}/orders/current?t={token}`.
9. Si puede pedir: `POST /employee/menus/global/{date}/orders?t={token}`.
10. Registrar push: `POST /me/notification-devices`.

### Flujo EMPLOYEE Con Invitacion Individual

1. Abrir link `/invite/{token}`.
2. Preview: `GET /invitations/{token}`.
3. Registro: `POST /invitations/{token}/accept`.
4. Guardar tokens.
5. Contexto: `GET /me/context`.
6. Continuar con flujo de menu global si el cook compartio un link global.

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
| COOK | Empresas | `/companies` |
| COOK | Crear/editar empresa | `POST /companies`, `PATCH /companies/{id}`, `PATCH /companies/{id}/location` |
| COOK | Invitaciones | `POST /companies/{id}/invitations`, `POST /companies/{id}/global-invitation` |
| COOK | Menus | `GET /menus`, `POST /menus`, `POST /menus/{id}/items`, `PATCH /menus/{id}/publish` |
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

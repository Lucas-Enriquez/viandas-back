<div align="center">

# Viandas API

**Backend del sistema de viandas: cocineros gestionan menús, empresas reciben pedidos, empleados ordenan y siguen el delivery en vivo.**

[![Java](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Flyway](https://img.shields.io/badge/Flyway-CC0200?logo=flyway&logoColor=white)](https://flywaydb.org/)
[![JWT](https://img.shields.io/badge/Auth-JWT%20%2B%20OAuth-000000?logo=jsonwebtokens&logoColor=white)](https://jwt.io/)
[![Maven](https://img.shields.io/badge/Maven-C71A36?logo=apachemaven&logoColor=white)](https://maven.apache.org/)

</div>

---

## Qué es

API REST para una plataforma B2B de gestión de viandas. Modela tres actores principales:

- **Cocinero (`COOK`)** — arma menús, publica platos, gestiona productos e inventario, recibe pedidos y entrega.
- **Empresa (`COMPANY`)** — cliente del cocinero. Tiene ubicación, empleados invitados y consume menús asignados.
- **Empleado (`EMPLOYEE`)** — hace pedidos sobre el menú vigente y sigue el delivery en tiempo real.

Lo que diferencia al sistema:

- **Delivery con tracking en vivo** vía sesiones geolocalizadas y señales públicas (`OUT_FOR_DELIVERY` / `NEARBY` / `DELIVERED`).
- **Stream de órdenes en tiempo real** con Server-Sent Events (`GET /orders/stream`).
- **Menús con doble alcance**: `COMPANY` (específicos por cliente) o `GLOBAL` (catálogo común).
- **Invitaciones globales** que permiten sumar empleados a una empresa con un único link reutilizable.

---

## Stack

| Capa | Tecnología |
|---|---|
| Runtime | Java 21, Spring Boot 4.0 |
| Persistencia | PostgreSQL + Spring Data JPA, Flyway para migraciones |
| Seguridad | Spring Security, JWT (`jjwt`), Google OAuth (`tokeninfo`), Bucket4j para rate limiting |
| Almacenamiento de imágenes | Cloudinary |
| Notificaciones push | Firebase Admin SDK |
| Email transaccional | Resend |
| Observabilidad | Sentry + Logstash JSON encoder |
| Tests | Spring Boot Test, Security Test, Validation Test, WebMVC Test |

---

## Estructura

Organización por **módulos de dominio**, cada uno con sus propias capas (`web`, `application`, `domain`, `persistence`, `dto`).

```
src/main/java/com/viandas/api/
├── auth/           Login, Google OAuth, JWT, refresh tokens, password reset
├── company/        Empresas, ubicaciones, membership, geocoding
├── delivery/       Sesiones de delivery con tracking GPS y señales públicas
├── invitation/     Invitaciones por empresa y globales (link compartido)
├── menu/           Menús, items, categorías (PLATO / MINUTA / ENSALADA)
├── notification/   Registro de devices y push via Firebase
├── order/          Órdenes, estados, stream SSE
├── product/        Catálogo de productos con fotos en Cloudinary
├── user/           Contexto del usuario actual
└── shared/         EnvironmentGuard, EmailService, PhotoUploadService, ApiResponse
```

---

## Quick start

### Requisitos

- JDK 21
- PostgreSQL 14+
- Maven (o usar el wrapper `./mvnw` / `mvnw.cmd`)

### 1. Clonar y configurar variables

```bash
git clone <repo-url> viandas-api
cd viandas-api
cp .env.example .env
```

Completá `.env` con tus credenciales (ver tabla abajo). El archivo nunca se commitea.

### 2. Base de datos

```bash
createdb viandas
createdb viandas_test   # para correr los tests
```

Las migraciones de Flyway corren automáticamente al levantar la app.

### 3. Correr la app

```bash
# Perfil dev (habilita BootstrapController para crear el primer COOK)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# O desde IntelliJ: VM options → -Dspring.profiles.active=dev
```

La API queda en `http://localhost:8080`.

### 4. Tests

```bash
./mvnw test
```

---

## Variables de entorno

| Variable | Obligatoria en prod | Descripción |
|---|---|---|
| `DB_URL` | sí | JDBC URL de Postgres |
| `DB_USERNAME` / `DB_PASSWORD` | sí | Credenciales de la DB |
| `VIANDAS_JWT_SECRET` | sí | Secreto JWT, mínimo 32 caracteres |
| `VIANDAS_JWT_EXPIRATION_MINUTES` | no | Default `15` |
| `VIANDAS_REFRESH_EXPIRATION_DAYS` | no | Default `30` |
| `VIANDAS_BOOTSTRAP_KEY` | solo dev | Clave para el endpoint de bootstrap del primer cocinero |
| `VIANDAS_PUBLIC_BASE_URL` | sí | URL pública usada en mails (ej. reset password) |
| `VIANDAS_CORS_ORIGINS` | sí | Orígenes permitidos, separados por coma |
| `GOOGLE_CLIENT_ID` | sí | Client ID para validar `id_token` de Google |
| `CLOUDINARY_CLOUD_NAME` / `CLOUDINARY_API_KEY` / `CLOUDINARY_API_SECRET` | sí | Cuenta de Cloudinary para fotos |
| `FIREBASE_CREDENTIALS_JSON` | sí | Service account JSON inline para push notifications |
| `RESEND_API_KEY` / `RESEND_FROM` | sí | Envío de emails transaccionales |
| `SENTRY_DSN` / `SENTRY_ENV` | sí | Observabilidad y reporte de errores |

> `EnvironmentGuard` corre al arranque en cualquier perfil distinto de `dev` y **rompe el boot** si alguna variable crítica falta o conserva un valor por defecto. Es imposible levantar producción con secretos de desarrollo.

---

## Endpoints principales

Todas las respuestas siguen el envelope `ApiResponse<T>` excepto el stream SSE.

| Método | Path | Auth | Descripción |
|---|---|---|---|
| `POST` | `/auth/login` | público | Login con email + password |
| `POST` | `/auth/google` | público | Login con Google `id_token` |
| `POST` | `/auth/refresh` | público | Refresh del access token |
| `POST` | `/auth/logout` | bearer | Revoca el refresh token |
| `POST` | `/auth/forgot-password` | público | Dispara mail con link de reset |
| `POST` | `/auth/reset-password` | público | Cambia password con token de reset |
| `GET` | `/me` | bearer | Contexto del usuario actual |
| `*` | `/companies/**` | COOK | Empresas, ubicaciones, membership |
| `*` | `/menus/**` | COOK / EMPLOYEE | Menús e items |
| `*` | `/products/**` | COOK | Productos + upload de fotos |
| `*` | `/orders/**` | COOK / EMPLOYEE | Órdenes y estados |
| `GET` | `/orders/stream` | bearer | **SSE** de cambios de estado en tiempo real |
| `*` | `/delivery/**` | COOK | Sesiones de delivery y tracking |
| `*` | `/invitations/**` | COOK | Invitaciones a empresas y globales |
| `*` | `/notifications/devices/**` | bearer | Registro de devices para push |

Ejemplos completos de payloads y flujos en [`docs/expo-api-endpoints.md`](docs/expo-api-endpoints.md) y [`docs/testing-guide.md`](docs/testing-guide.md).

---

## Seguridad

- JWT firmado con HS256, secreto validado (mín. 32 chars, no default).
- Refresh tokens persistidos y revocables.
- **Rate limiting** en endpoints de auth via Bucket4j.
- Google OAuth validado contra `oauth2.googleapis.com/tokeninfo`.
- CORS configurable por entorno, con guard que bloquea `localhost` fuera de `dev`.
- Bootstrap del primer cocinero (`/internal/bootstrap/cook`) **solo expuesto en perfil `dev`**.
- Secretos siempre por variable de entorno, nunca en el repo.

---

## Convenciones

- Respuestas envueltas en `ApiResponse<T>` con `success`, `data`, `message`, `errors`, `meta`.
- IDs en formato UUID v7.
- Fechas en ISO 8601 (`LocalDate`, `LocalTime`, `Instant` UTC).
- Migraciones Flyway versionadas (`V1__…sql`, `V2__…sql`, …) en `src/main/resources/db/migration`.
- Profile `dev` para desarrollo local, profile `test` para integración.

---

## Licencia

Sin licencia pública por el momento. Si vas a usarlo, abrí un issue o contactá al autor.

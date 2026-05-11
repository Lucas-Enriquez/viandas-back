# Backlog

Cosas pendientes derivadas del feature de inventario de productos + fotos. Ordenado por impacto/urgencia, no por dificultad.

---

## Inmediato (a hacer ya)

### 1. Integración del frontend con los nuevos contratos
**Por qué**: El cambio de `AddMenuItemRequest` es **breaking** — el contrato viejo (`name/price/category/photoUrl` directo) ya no funciona como único modo. El front tiene que adaptarse o se le rompen los menús.
**Scope**:
- Actualizar el form de agregar item para usar selector de inventario (`productId`) o modo libre.
- Implementar el flujo de subida de fotos en 3 pasos (sign → upload directo a Cloudinary → crear con `photoPublicId`).
- Pantalla nueva de ABM de productos.
- Recomendación: hacer resize en cliente antes de subir (ver el doc de hand-off).

### 2. Setear env vars de Cloudinary en dev/staging/prod
**Por qué**: Sin `CLOUDINARY_CLOUD_NAME/API_KEY/API_SECRET` el endpoint `/products/uploads/sign` devuelve 409. La API arranca igual pero las fotos no funcionan.
**Scope**: 5 minutos. Crear cuenta en Cloudinary si no existe → tomar credenciales → setear en cada entorno.

### 3. Tests del feature
**Por qué**: Hoy solo existe `ViandasApiApplicationTests.contextLoads()`. Cero cobertura del CRUD de productos, validación de `photoPublicId`, branch de `addItem` (productId vs free-form), validación cruzada.
**Scope** (mínimo razonable):
- `ProductServiceTest`: happy path CRUD, ownership cross-cook (no leakage), nombre único case-insensitive.
- `MenuServiceTest.addItem`: ambas ramas + rechazo de "productId con campos sueltos".
- `PhotoUploadServiceTest`: validación de `publicId` (carpeta correcta, traversal `..`).

---

## Corto plazo (próximas 2-3 semanas)

### 4. Categorías dinámicas de productos
**Por qué**: Hoy `MenuItemCategory` es un enum hardcoded (`PLATO`, `MINUTA`, `ENSALADA`). Para agregar "Postre" o "Bebida" hay que tocar código + redeploy. Si el cook quiere personalizar el catálogo, no puede.
**Scope** (medio): nueva entidad `ProductCategory` cook-scoped, FK desde `Product` y `MenuItem`, migración con backfill de las 3 default por cook, ABM de categorías, reemplazar los `if` hardcodeados de `MenuService.buildShareText` por iteración por categorías ordenadas.
**Decisión pendiente**: ¿categorías por cook o globales? (Por cook es más flexible, ~30% más código.)

### 5. Eliminar foto del producto vía endpoint dedicado
**Por qué**: Hoy para borrar una foto hay que hacer `PATCH /products/{id}` con todos los campos y `photoPublicId: null`. Es feo.
**Scope** (chico): `DELETE /products/{id}/photo` que setea `photoPublicId` y `photoUrl` a `null`. ~20 LOC.

### 6. Exponer `product_id` en `MenuItemResponse`
**Por qué**: El backend ya guarda la FK pero el frontend no la ve. Sin esto no se pueden hacer reportes de "qué tanto se vendió del producto X" desde la UI.
**Scope** (chico): agregar campo opcional `productId: UUID` al record `MenuItemResponse` y al mapper en `MenuService.toItemResponse`. ~3 líneas.

---

## Mediano plazo

### 7. Cleanup de assets huérfanos en Cloudinary
**Por qué**: Por la semántica de snapshot, cuando se reemplaza o borra la foto de un Product, el asset viejo en Cloudinary queda vivo (porque MenuItems anteriores pueden tenerlo snapshoteado). A largo plazo eso infla la cuota de Cloudinary.
**Opciones**:
- **A. Job manual periódico**: script que liste todos los `photoPublicId` referenciados en `products` + `menu_items` (vía URL parsing) y borre lo que no aparezca en Cloudinary.
- **B. No hacer nada** y limpiar manualmente desde el panel de Cloudinary cuando moleste. (Probablemente la mejor decisión hasta que sea un problema real.)

### 8. Transformaciones de imagen on-the-fly
**Por qué**: Hoy guardamos solo el URL "raw" de Cloudinary. Para listas/thumbnails podríamos servir versiones más chicas (WebP, resize a 200x200) usando los URL transforms de Cloudinary, sin tocar el backend.
**Scope** (chico, lo puede hacer el front): cambiar el URL en cliente:
- Original: `https://res.cloudinary.com/{cloud}/image/upload/{publicId}`
- Thumbnail: `https://res.cloudinary.com/{cloud}/image/upload/c_fill,w_200,h_200,q_auto,f_auto/{publicId}`

Cero cambios en backend. Vale la pena documentar en el doc de hand-off.

### 9. Reportes / analytics por producto
**Por qué**: Ahora que `menu_items.product_id` existe, podemos contestar "cuánto vendí del producto X este mes". Es la razón por la que metimos la FK.
**Scope**: depende de qué reportes quieran. Endpoints tipo:
- `GET /reports/products/sales?from=YYYY-MM-DD&to=YYYY-MM-DD` → suma ventas por product_id agrupadas.
- `GET /products/{id}/stats` → vista por producto.

Necesita decisión de producto sobre qué métricas.

---

## Largo plazo / nice-to-have

### 10. Bulk import de productos
**Por qué**: Para cooks que migran de Excel/papel.
**Scope** (medio): endpoint `POST /products/import` que acepta CSV/JSON con array de productos, valida en lote, devuelve resultado por fila.

### 11. Stock global por producto
**Por qué**: Hoy `MenuItem.remainingStock` es por día/menú. Si en algún momento se quiere stock acumulativo (preparaciones que sobran y se llevan al día siguiente), va a hacer falta. Se descartó en el diseño actual.
**Scope** (alto): nueva columna en `Product`, lógica de descuento al crear orders, sincronización con `remainingStock` de `MenuItem`. **No tocar hasta que sea un dolor real.**

### 12. `.claude/` al `.gitignore`
**Por qué**: `.claude/settings.local.json` se está stageando en cada sesión. Es config local de IDE/AI, no debería ir al repo.
**Scope** (1 línea): agregar `.claude/` al `.gitignore`.

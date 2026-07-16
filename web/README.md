# Product Service — Frontend Angular

SPA en Angular 21 para la plataforma Product Service. Provee una interfaz Material Design para crear, ver, actualizar y eliminar productos a través de la API de Spring Boot MVC, incluyendo un toggle para alternar entre el listado de productos activos e inactivos.

## Autenticación

La app requiere login — `POST /api/v1/auth/login` (ver [`api/README.md`](../api/README.md) para las credenciales demo). El `AuthService` (`src/app/auth/auth.service.ts`) guarda el JWT en `localStorage`, un `HttpInterceptorFn` (`auth.interceptor.ts`) lo agrega como header `Authorization: Bearer <token>` en cada request y redirige a `/login` ante cualquier 401 (salvo el propio login, para no generar un loop), y un `CanActivateFn` (`auth.guard.ts`) protege las rutas de `/products`.

**Verificado end-to-end** contra el stack real de `docker-compose` (Postgres real, sin mocks): login con `admin`/`admin123` → JWT real → `GET /api/v1/products/active` autorizado → 200 con datos reales; sin token o con credenciales incorrectas → 401.

## Stack Tecnológico

| Categoría | Tecnología |
|---|---|
| Framework | Angular 21 (componentes standalone) |
| Librería de UI | Angular Material (tema M3) |
| HTTP | Angular HttpClient + RxJS |
| Formularios | Angular Reactive Forms |
| Tests | Vitest + Angular TestBed |
| Build | Angular CLI, Docker multi-stage (nginx) |

## Cómo Empezar

```bash
npm install
npm start
```

Se abre en `http://localhost:4200`. Se conecta a la API en `http://localhost:8080` por defecto.

Para apuntar a un backend distinto, editar `src/environments/environment.ts`:

```ts
export const environment = {
  production: false,
  apiUrl: 'http://your-api-host:8080'
};
```

Para builds de producción, el archivo equivalente es `src/environments/environment.prod.ts`.

## Correr las Pruebas

```bash
npm test
```

| Suite | Archivo | Qué cubre |
|---|---|---|
| Bootstrap de la app | `app.spec.ts` | Creación del componente raíz y título |
| Servicio HTTP | `product.service.spec.ts` | Llamadas de HttpClient, mapeo de request/response |
| Listado de productos | `product-list.component.spec.ts` | Renderizado de tabla, estado de carga |
| Formulario de productos | `product-form.component.spec.ts` | Validación de formulario, modos crear/editar |
| Servicio de auth | `auth/auth.service.spec.ts` | Login, logout, persistencia en `localStorage` |
| Login | `auth/login.component.spec.ts` | Envío del formulario, navegación, mensaje de error |
| Guard de rutas | `auth/auth.guard.spec.ts` | Permite/bloquea navegación según sesión |
| Interceptor HTTP | `auth/auth.interceptor.spec.ts` | Header Bearer, redirect a `/login` en 401 |

E2E con Playwright (`e2e/login.spec.ts`): redirect sin sesión, login exitoso, credenciales inválidas, logout. `e2e/products.spec.ts` siembra una sesión falsa en `localStorage` vía `page.addInitScript` antes de cada test (las llamadas a la API ya están mockeadas ahí, no hace falta un login real).

## Build de Producción

```bash
npm run build
```

La salida va a `dist/`. El `Dockerfile` usa un build multi-stage (Node → nginx) y sirve los assets compilados en el puerto 80.

## Estructura del Proyecto

```
src/app/
├── auth/
│   ├── auth.service.ts       Signals para token/username/role, login()/logout(), persiste en localStorage
│   ├── auth.interceptor.ts   HttpInterceptorFn — agrega el Bearer header, redirige a /login en 401
│   ├── auth.guard.ts         CanActivateFn — protege las rutas de /products
│   └── login.component.ts    Formulario de login (Reactive Forms + Angular Material)
├── core/
│   ├── models/        product.model.ts       PageResponse<T>, Product, ProductRequest
│   └── services/      product.service.ts     getActive()/getInactive() (desenvuelven el PageResponse paginado), create/update/delete
└── products/
    ├── product-list/ vista de listado con tabla de Material — checkbox "Show inactive" (label sin traducir en la UI) alterna entre GET /active y GET /inactive
    └── product-form/ diálogo de creación / edición
```

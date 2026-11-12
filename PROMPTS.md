# Prompts del backend — pégalos en Claude Code, uno por uno

**Antes del paso 1:** crea el repo `uco-kudos-backend`, clónalo, copia dentro `CLAUDE.md`, `.env.example`, `Dockerfile`, `.github/` y `src/main/resources/db/migration/` de este kit, y copia la carpeta de documentación a `docs/`. Crea además un `.gitignore` con `.env`, `target/`, `.idea/`. Exporta tus variables de entorno en la terminal donde corras Claude Code (nunca en archivos del repo).

Cada paso: **Qué hace** (para sustentar) · **Prompt** · **Cómo verificar** · **Commit**.

---

## B1 · Esqueleto, arquitectura y ArchUnit
**Qué hace:** crea el proyecto Maven con Spring Boot 4.1.1 / Java 25, los paquetes hexagonales vacíos, `application.yml` con variables de entorno, Flyway con las migraciones ya incluidas, Actuator y las pruebas ArchUnit que impiden violar la arquitectura.
**Prompt:**
> Lee CLAUDE.md y docs/04-arquitectura.md. Crea el `pom.xml` (Java 25, Spring Boot 4.1.1, starters: webmvc, validation, jdbc, flyway, security-oauth2-resource-server, actuator, test; postgresql; resilience4j-spring-boot4 2.4.0; archunit-junit5 1.5.1; jacoco-maven-plugin; sonar-maven-plugin no hace falta declararlo). Crea la estructura de paquetes de CLAUDE.md, `Application`, `application.yml` (puerto `${PORT:8080}`, datasource por env, Flyway con placeholders `seed_dest_correo`/`seed_dest_celular`, actuator health, logs JSON si es sencillo) y una prueba ArchUnit con AR-01..AR-06 (puede fallar vacía; hazla pasar). Las migraciones V1 y V2 ya están en `src/main/resources/db/migration`; no las modifiques. Ejecuta `mvn -B verify` y corrige. No hagas commit.

**Verificar:** `mvn -B verify` verde.  **Commit:** `chore: esqueleto hexagonal, Flyway y pruebas de arquitectura`

---

## B2 · Dominio y puertos
**Qué hace:** el corazón sin frameworks: `Kudo.crear` valida invariantes (autor ≠ destinatario, categoría activa, texto no vacío); evento `KudoPublicado`; excepciones; interfaces (puertos) de entrada y salida.
**Prompt:**
> Implementa el dominio y los puertos según docs/12-diseno-alto-y-detallado.md §3.1: `Persona`, `Categoria`, `Kudo` (factory `Kudo.crear`), `Adjunto`, `KudoPublicado` (record inmutable con kudoId, autorId, destinatarioId, traceId), `ReglaDeNegocioException`. Puertos de entrada: `PublicarKudoUseCase`, `NotificarKudoPublicadoUseCase`, `IniciarSesionUseCase`, `ListarCategoriasUseCase`, `ListarDestinatariosUseCase`, `ConsultarAuditoriaUseCase`. Puertos de salida: `KudoRepositoryPort`, `PersonaRepositoryPort`, `CategoriaRepositoryPort`, `FileStoragePort`, `AuditLogPort`, `DomainEventPublisherPort`, `NotificacionRepositoryPort`, `EmailSenderPort`, `WhatsAppSenderPort`, `PushNotifierPort`. Dominio sin imports de Spring. Pruebas unitarias de `Kudo.crear` (casos válido, autor=destinatario, categoría inactiva, texto vacío). `mvn -B verify`. No hagas commit.

**Commit:** `feat(dominio): Kudo, evento KudoPublicado y puertos`

---

## B3 · Persistencia JDBC
**Qué hace:** adaptadores de salida con JDBC que implementan los puertos de repositorio y de auditoría.
**Prompt:**
> Implementa con `JdbcClient` los adaptadores `KudoJdbcRepository`, `PersonaJdbcRepository`, `CategoriaJdbcRepository`, `NotificacionJdbcRepository` y `JdbcAuditLogAdapter` (insertan en `auditoria` con el `trace_id` recibido; solo INSERT/SELECT) en `infraestructura.output.adapter.persistencia` y `.auditoria`. Respeta el esquema de `V1__esquema_base.sql` (UUID, tablas `contenido`, `adjunto`, `notificacion`, `auditoria`). Sin JPA. Agrega pruebas solo si no requieren Docker. `mvn -B verify`. No hagas commit.

**Commit:** `feat(persistencia): repositorios JDBC y auditoría`

---

## B4 · Seguridad: login y JWT
**Qué hace:** `POST /api/auth/login` verifica BCrypt y emite un JWT HS256 (Nimbus) con id y rol; el resto de endpoints exige `Bearer`. CORS solo para el origen del frontend.
**Prompt:**
> Implementa `IniciarSesionService` + `AuthController` (`POST /api/auth/login` con correo y clave; solo cuentas `ACTIVA`), emisión de JWT HS256 con `JwtEncoder` de Nimbus usando `JWT_SECRET` (claims: sub=personaId, rol, exp 60 min), `SecurityConfig` (stateless, `oauth2ResourceServer().jwt()`, público: login y `/actuator/health`; resto autenticado; `/api/auditoria` solo ADMIN), CORS con `CORS_ALLOWED_ORIGIN`, y `GET /api/categorias` y `GET /api/personas/destinatarios` (id y nombre de personas activas, excluyendo al usuario autenticado). Manejador global de errores `problem+json` con `traceId`. Filtro que genera `trace_id` por petición y lo pone en MDC y en el encabezado `X-Trace-Id`. Pruebas de login correcto/incorrecto. `mvn -B verify`. No hagas commit.

**Verificar:** `curl -X POST .../api/auth/login` con `autor@uco-kudos.local` / `Kudos2026*` devuelve token.  **Commit:** `feat(seguridad): login JWT, CORS, errores problem+json y trace_id`

---

## B5 · Publicar Kudo (guardar)
**Qué hace:** el caso de uso transaccional (páginas 6 del diagrama de secuencia).
**Prompt:**
> Implementa `PublicarKudoService` (`@Transactional`) siguiendo la página 6 de docs/diagramas/UCO-Kudos.drawio y docs/12 §3.1: validar autor/destinatario activos y distintos, categoría activa; `Kudo.crear`; por cada adjunto `FileStoragePort.guardar` (calcula sha256; valida tipo pdf/png/jpg y tamaño máximo 5 MB); persistir; `AuditLogPort.registrar(KUDO_PUBLICADO)`; `DomainEventPublisherPort.publicar(KudoPublicado)`. Adaptador `SupabaseStorageAdapter` con `RestClient` contra la API REST de Supabase Storage (bucket privado `SUPABASE_BUCKET`, cabecera con `SUPABASE_SERVICE_KEY`) y `SpringDomainEventPublisher` (ApplicationEventPublisher). `KudoController`: `POST /api/kudos` multipart (`destinatarioId`, `categoriaId`, `contenido`, `adjuntos[]`) → `201` con `{id, destinatarioId, categoriaId, creadoEn, adjuntos, traceId}`; 422 en reglas de negocio, 400 en validación. Pruebas unitarias del servicio con dobles. `mvn -B verify`. No hagas commit.

**Commit:** `feat(kudos): publicar Kudo con adjuntos, auditoría y evento de dominio`

---

## B6 · Publicar Kudo (notificaciones)
**Qué hace:** el flujo asíncrono tras el COMMIT (página 7): tres canales independientes con reintento y circuit breaker.
**Prompt:**
> Implementa según la página 7 del diagrama: `NotificacionesKudoListener` (`@TransactionalEventListener(AFTER_COMMIT)` + `@Async` con un `TaskExecutor` propio) que llama a `NotificarKudoPublicadoUseCase`; `NotificarKudoPublicadoService` que ejecuta los 3 canales de forma independiente (la excepción de uno no afecta a los otros), guarda `notificacion` (ENVIADA/FALLIDA/OMITIDA, intentos, último error) y audita `NOTIFICACION_CORREO_ENVIADA|_FALLIDA`, `NOTIFICACION_WHATSAPP_ENVIADA|_FALLIDA`, `NOTIFICACION_PUSH_ENVIADA|_OMITIDA_DESCONECTADO` con el mismo `trace_id`. Adaptadores con `RestClient` (sin SDK): `BrevoEmailSenderAdapter` (`POST https://api.brevo.com/v3/smtp/email`, cabecera `api-key`), `TwilioWhatsAppSenderAdapter` (`POST https://api.twilio.com/2010-04-01/Accounts/{SID}/Messages.json`, auth básica, `From=TWILIO_WHATSAPP_FROM`, `To=whatsapp:<celular>`), y `SsePushNotifierAdapter` (registro de `SseEmitter` por usuario; si no hay conexión → OMITIDA_DESCONECTADO) con `GET /api/notificaciones/stream`. Resilience4j `@Retry` y `@CircuitBreaker` en correo y WhatsApp. Pruebas: un canal falla y los otros dos continúan; Kudo no se revierte. `mvn -B verify`. No hagas commit.

**Verificar (local, con tus variables):** publicar un Kudo y ver correo + WhatsApp + push.  **Commit:** `feat(notificaciones): correo, WhatsApp y push SSE independientes con resiliencia`

---

## B7 · Auditoría y observabilidad
**Prompt:**
> Implementa `GET /api/auditoria?traceId=` (solo ADMIN) que devuelve las filas de `auditoria` ordenadas; verifica que el `trace_id` aparece en los logs JSON; expón `/actuator/health` y `/actuator/metrics` (este último protegido) y un contador Micrometer de notificaciones enviadas/fallidas por canal. `mvn -B verify`. No hagas commit.

**Commit:** `feat(observabilidad): consulta de auditoría por trace_id y métricas`

---

## B8 · Contenedor, CI/CD y despliegue (lo haces tú, con ayuda)
1. `docker build -t uco-kudos-backend .` local; debe construir.
2. Sube a GitHub (`git add . && git commit && git push`). En **Settings → Secrets and variables → Actions** crea: secrets `SONAR_TOKEN`, `RENDER_DEPLOY_HOOK_URL`; variables `SONAR_ORG`, `SONAR_PROJECT_KEY`, `API_URL`.
3. En Render: *New → Web Service → Docker* desde el repo, rama `main`, **Auto-Deploy desactivado** (lo dispara el workflow), health check `/actuator/health`, y todas las variables de `.env.example`. Copia el *Deploy Hook* a `RENDER_DEPLOY_HOOK_URL`.
4. Importa el repo en SonarQube Cloud (desactiva el análisis automático si da conflicto con el de CI).

**Prompt (si algo falla):**
> Lee el log del workflow fallido (te lo pego) y propón el cambio mínimo para ponerlo verde sin bajar controles de seguridad.

**Commit:** `ci: pipeline CI/CD con Sonar y Trivy`

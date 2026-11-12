# CLAUDE.md — uco-kudos-backend

Proyecto integrador final de Ingeniería del Software II (UCO). **Individual**: el autor debe poder explicar y modificar en vivo cualquier línea. Por eso: código simple, pequeño y legible; nada "mágico" que no se pueda explicar.

## Alcance (línea base — NO construyas nada más)
1. `POST /api/auth/login` (JWT).
2. `GET /api/categorias`, `GET /api/personas/destinatarios` (usuarios activos para elegir destinatario).
3. **Publicar Kudo**: `POST /api/kudos` (multipart) → guarda → auditoría → evento `KudoPublicado` → correo (Brevo) + WhatsApp (Twilio) + push (SSE), cada uno independiente.
4. `GET /api/notificaciones/stream` (SSE), `GET /api/auditoria?traceId=` (solo ADMIN), `GET /actuator/health`.
No implementes registro, aprobaciones, réplicas, calificaciones, reportes ni vetos (están solo diseñados).

## Lee antes de cada paso
`docs/` (copia de la documentación): `04-arquitectura.md`, `11-modelo-de-datos-y-api.md`, `12-diseno-alto-y-detallado.md` §3.1, `06-seguridad.md`, `07-adjuntos-y-observabilidad.md`. Los diagramas de secuencia están en `docs/diagramas/UCO-Kudos.drawio` (págs. 6 y 7).

## Stack (no cambies versiones sin avisar)
Java 25 · Spring Boot 4.1.1 · Maven · PostgreSQL (Supabase) con **JDBC (`JdbcClient`/`NamedParameterJdbcTemplate`), sin JPA** · Flyway · Spring Security + `spring-boot-starter-security-oauth2-resource-server` (JWT HS256 con Nimbus, **no jjwt**) · BCrypt · `RestClient` para Brevo, Twilio y Supabase Storage (**sin SDKs de proveedor**) · Resilience4j 2.4.0 (`resilience4j-spring-boot4`) · Actuator + Micrometer · JUnit 5 + Mockito + ArchUnit 1.5.1 + JaCoCo.
Boot 4 usa starters modulares (`spring-boot-starter-webmvc`, `-flyway`, `-security-oauth2-resource-server`, `-webmvc-test`, `-security-test`) y Jackson 3. Si una dependencia no resuelve, **detente y repórtalo**; no inventes versiones.

## Arquitectura hexagonal (reglas ejecutadas por ArchUnit)
Raíz: `co.edu.uco.kudos`
```
dominio.{modelo,evento,excepcion}
servicios.input.{port,adapter,dto}     # port = casos de uso (interfaces), adapter = implementaciones (*Service)
servicios.output.port                  # puertos de salida
infraestructura.input.{controlador,evento,stream}
infraestructura.output.adapter.{persistencia,almacenamiento,correo,whatsapp,push,auditoria}
infraestructura.configuracion
```
- AR-01 `dominio` no depende de `servicios` ni `infraestructura`. AR-02 `dominio` no importa Spring/JDBC/SDK.
- AR-03 `servicios` no depende de `infraestructura`.
- AR-04 controladores y listeners **solo** llaman puertos de entrada (`*UseCase`).
- AR-05 casos de uso hablan con el exterior **solo** por puertos de salida.
- AR-06 adaptadores de salida implementan interfaces de `servicios.output.port`. AR-07 nada de filas/entidades en la API (DTO). AR-08 sin ciclos.
- Nombres de clases y variables en **español** (como el dominio): `PublicarKudoService`, `KudoJdbcRepository`, `KudoPublicado`…

## Flujo Publicar Kudo (obligatorio)
Controller → `PublicarKudoUseCase.publicar(PublicarKudoCommand)` → `PublicarKudoService` (`@Transactional`): busca autor/destinatario/categoría, `Kudo.crear(...)` (invariantes), guarda adjuntos (`FileStoragePort`), persiste (`KudoRepositoryPort`), audita `KUDO_PUBLICADO` (`AuditLogPort`), publica `KudoPublicado` (`DomainEventPublisherPort`) → `COMMIT` → `201`.
`NotificacionesKudoListener` (`@TransactionalEventListener(AFTER_COMMIT)` + `@Async` con ejecutor propio) → `NotificarKudoPublicadoUseCase` → `NotificarKudoPublicadoService`: tres canales **independientes** (`EmailSenderPort`, `WhatsAppSenderPort`, `PushNotifierPort`); cada resultado se guarda en `notificacion` y en `auditoria` (`NOTIFICACION_CORREO_ENVIADA|_FALLIDA`, `NOTIFICACION_WHATSAPP_ENVIADA|_FALLIDA`, `NOTIFICACION_PUSH_ENVIADA|_OMITIDA_DESCONECTADO`). La falla de un canal **no** deshace el Kudo ni afecta a los demás. Todo con el mismo `trace_id` (filtro que lo crea, lo pone en MDC y lo devuelve en respuestas/errores).

## Seguridad y secretos
- **Nunca** pongas secretos en el código, logs ni commits. Todo por variables de entorno (ver `.env.example`): `DB_URL, DB_USER, DB_PASSWORD, JWT_SECRET, SUPABASE_URL, SUPABASE_SERVICE_KEY, SUPABASE_BUCKET, BREVO_API_KEY, MAIL_FROM, TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, TWILIO_WHATSAPP_FROM, CORS_ALLOWED_ORIGIN, SEED_DEST_CORREO, SEED_DEST_CELULAR`.
- Flyway placeholders: `spring.flyway.placeholders.seed_dest_correo=${SEED_DEST_CORREO:destinatario@example.com}` (y `seed_dest_celular`).
- Errores como `application/problem+json` con `traceId`; sin trazas de pila hacia el cliente. Adjuntos: validar tipo (pdf/png/jpg) y tamaño; bucket privado; URLs firmadas.
- `server.port=${PORT:8080}`; `server.shutdown=graceful`; logs en JSON a stdout.

## Pruebas (mínimo defendible)
Unitarias de dominio (`Kudo.crear`) y de `PublicarKudoService` y `NotificarKudoPublicadoService` con dobles de los puertos (un canal falla y los demás siguen); ArchUnit AR-01..AR-06; JaCoCo activo. Sin Docker/Testcontainers salvo que el autor lo pida.

## Definición de terminado de cada paso
`mvn -B verify` en verde · explicación breve (3–5 líneas) de qué se hizo y por qué, apta para sustentar · lista de archivos tocados · mensaje de commit sugerido. **No hagas `git commit` ni `git push`**: lo hace el autor.

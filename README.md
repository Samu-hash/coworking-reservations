# Coworking Reservations API

Microservicio REST para gestionar reservas de espacios de coworking (salas, puestos, cabinas).
Prueba técnica. Traté de que fuera una base que se pueda llevar a producción, no un demo que
"funciona y ya": donde había que elegir, prioricé que la concurrencia, la seguridad y la resiliencia
estuvieran sólidas antes que sumar features a medias.

## Cómo lo corro

Con Docker Compose (levanta Postgres, un mock de pagos y la app):

```bash
cp .env.example .env          # define JWT_SECRET (mínimo 32 chars)
docker compose up --build
```

La app queda en `http://localhost:8080`. El compose no arranca la app hasta que Postgres pasa su
healthcheck, así que no hay carrera de "la app levantó antes que la base". Flyway corre las
migraciones al arrancar (incluida la extensión `btree_gist` y la constraint de anti-solape).

- Swagger UI: `http://localhost:8080/swagger-ui.html` (hay un botón *Authorize* para pegar el token).
- Actuator: `http://localhost:8080/actuator/health`, `/actuator/circuitbreakers` (este último pide token de ADMIN).
- Admin sembrado por Flyway para poder operar de una: **admin@coworking.sv / admin1234**.

En `requests.http` está el flujo completo de ejemplo (registro, login, crear espacio, reservar,
intento de solape, confirmar, reporte). Se corre desde VS Code (REST Client) o IntelliJ.

Para desarrollo sin Docker: necesitás un Postgres local en `localhost:5432` (base/usuario/clave
`coworking`) y `./mvnw spring-boot:run` con el perfil `dev` (que es el default).

## Cómo está organizado

Arquitectura en capas (controller / service / repository / dto / mapper), pero agrupada
**por feature** (`space`, `reservation`, `payment`, `report`, `security`, `notification`) en vez de
por tipo. Para tocar "reservas" no salto entre cinco carpetas, y cada feature deja su repositorio
`package-private`. El paquete `reservation` es el gordo (tiene el State, el anti-solape, la
confirmación con pago); `space` es un CRUD casi pelado. Esa asimetría es a propósito: no todo
merece la misma ceremonia.

Las entidades JPA nunca salen por el API: todo va en DTOs (`record` de Java 17), y el mapeo de salida
lo hace MapStruct. La construcción de entidades la decide el service, no el mapper (MapStruct copia
datos, el service decide datos).

## Decisiones que tomé (y por qué)

**Anti-solape bajo concurrencia.** Es la parte que más cuidé. Puse doble candado: un pre-check en el
service (`existsOverlap`) que da un `409` amable en el caso normal, y una *exclusion constraint* de
Postgres (`EXCLUDE USING gist (space_id WITH =, tstzrange(start,end,'[)') WITH &&)`) como garantía
real. El pre-check no alcanza para la concurrencia (dos requests pueden verlo "libre" a la vez): si la
carrera se cuela, la constraint corta el segundo insert con un `23P01` y lo traduzco al mismo `409`,
así el usuario nunca ve un `500`. La constraint es *parcial* (`where status in ('PENDING_PAYMENT','CONFIRMED')`)
para que una reserva cancelada libere el horario sola, sin borrar la fila. Elegí esto sobre un lock
pesimista porque la constraint serializa solo los inserts que de verdad se pisan, no todas las reservas
del espacio; y sobre `SERIALIZABLE + retry` porque mueve el invariante al motor y me deja en el nivel
de aislamiento barato. Hay un test de integración con dos hilos peleando por el mismo slot que lo prueba.

**El pago va fuera de la transacción.** La validación de pago es un servicio externo lento/inestable.
Mantener una transacción de base abierta durante esa llamada HTTP es cómo se agota el pool en
producción, así que la reserva se crea `PENDING_PAYMENT` y se commitea; el pago se llama después
(fuera de la transacción) y el resultado se aplica en una transacción corta con `TransactionTemplate`.

**Circuit breaker con fallback coherente.** La llamada al gateway va envuelta con Resilience4j. Si el
circuito abre o el gateway tarda/falla, el fallback deja la reserva en `PENDING_PAYMENT` en vez de
botarle un `500` al usuario: no se pierde la intención, se reconcilia después. El estado del circuito
sale en `/actuator/circuitbreakers` y en `/actuator/health`. Los umbrales están en `application.yml`;
dejé la mitad de las opciones en su default a propósito, no hace falta afinarlas todas para un mock.

**State para el ciclo de vida de la reserva.** Las transiciones válidas (`PENDING_PAYMENT → CONFIRMED
→ COMPLETED`, y `CANCELLED` desde varias) viven en el enum `ReservationStatus`: cada estado declara a
dónde puede saltar y `Reservation.transitionTo()` valida antes de mover el campo. Así, cancelar una
reserva ya completada tira `409` en vez de dejar el dato inconsistente, y no hay un `switch` regado por
el service que haya que tocar por cada estado nuevo. Lo hice con enum y no con una clase por estado
porque, para cinco estados sin comportamiento propio más allá de "a dónde puedo ir", una clase por
estado es ceremonia que no paga (y ensucia el mapeo JPA). Si mañana cada estado tuviera lógica
divergente, lo reabriría como `sealed interface`.

**Notificación como evento de dominio.** Al confirmar publico un `ReservationConfirmedEvent` y un
listener lo consume con `@Async` + `@TransactionalEventListener(AFTER_COMMIT)`. Async para no colgar la
respuesta HTTP, y after-commit para no "mandar el correo" de una reserva que al final hizo rollback. Un
`@Async` pelado no me daba esa garantía. La invalidación del cache del reporte no la colgué de este
evento sino directo en los métodos que cambian ocupación (confirmar/cancelar), porque cancelar también
la cambia y no dispara este evento.

**JWT stateless y ownership en el service.** El servicio no guarda sesión, así que autenticación por
token (el token lleva el `uid` para resolver dueño sin ir a la base). El rol lo checa `@PreAuthorize`,
pero "ver solo mis reservas" no se resuelve con el rol: eso lo valida el service comparando el `uid`
del token contra el dueño de la reserva. A un recurso ajeno devuelvo `404`, no `403`, para no filtrar
que existe. La seguridad de método no debería depender solo de una anotación.

**Cache del reporte con Caffeine, no Redis.** El reporte de ocupación es caro si lo pega un dashboard
seguido y el dato no cambia al minuto, así que lo cacheo por rango con `@Cacheable` y lo invalido con
`@CacheEvict` en las mutaciones. Caffeine y no Redis porque es un servicio de una sola instancia para
la prueba y no quería sumar otra pieza de infra; si esto escalara a varias réplicas, lo movería a Redis
por coherencia de cache.

Otras piezas, más rápido: perfiles `dev/prod` con `@ConfigurationProperties` (nada de `@Value` regado);
Bean Validation en los DTOs con manejo centralizado de errores en un `@RestControllerAdvice` que
devuelve siempre el mismo `ApiError`; `@ManyToOne(LAZY)` explícito + `@EntityGraph`/`Specification` para
no caer en N+1; el reporte es una query nativa de agregación (una sola pasada). Los tests de integración
corren contra Postgres real con Testcontainers y no H2 a propósito: la exclusion constraint y `tstzrange`
que sostienen todo el anti-solape no existen en H2, así que testear ahí sería probar otro código.

## Lo que menos me convence / haría distinto con más tiempo

Un par de simplificaciones que asumí a propósito: una reserva toma el **espacio completo**, no cupos por
asiento, así que `capacity` es informativa (si el negocio quisiera reservar por asiento, el anti-solape
tendría que contar reservas simultáneas contra la capacidad, no rechazar cualquier solape); y la
ocupación la calculo sobre 24h/día, no sobre un horario operativo.

Siendo honesto, lo que menos me cierra es la frontera transaccional del pago. Lo dejé correcto para el
caso feliz (pago fuera de la transacción, resultado aplicado en una transacción corta), pero la
interacción fina entre el circuit breaker y esa segunda transacción la resolvería con más calma; quedó
anotada con un `FIXME` en el código. Con más tiempo también metería: un job idempotente que reconcilie
las `PENDING_PAYMENT` que quedaron colgadas y pase a `COMPLETED` las que ya vencieron (hoy no hay
barrido); refresh tokens con revocación (hoy el access token dura 1h y listo); y tarifas más ricas
(hora pico, descuento por duración), que es justo donde metería un `Strategy` por tipo de espacio —
hoy la tarifa es lineal.

## Correr los tests

```bash
./mvnw verify
```

Corre los unitarios (Mockito) y los de integración (`*IT`, con Testcontainers). **Necesita un Docker
en marcha.** Nota: con Docker Engine muy nuevo (>=29) la auto-negociación de docker-java falla; por eso
fijé `docker.api.version=1.44` en el `pom` (Docker >=25 lo soporta). Si corren un Docker anterior a 25,
override con `-Ddocker.api.version=1.43`.

El test que más mira vale la pena: `ReservationOverlapConcurrencyIT` lanza dos hilos a reservar el
mismo espacio en el mismo horario y verifica que solo uno gana. `PaymentCircuitBreakerIT` tumba el
gateway con WireMock y confirma que el circuito abre y la reserva queda `PENDING_PAYMENT`.

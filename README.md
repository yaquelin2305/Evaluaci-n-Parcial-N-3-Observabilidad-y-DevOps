# Microservicio de Tickets — Observabilidad y DevOps (EP3)

Este repositorio contiene el **Microservicio de Tickets** desarrollado en Java 17 con Spring Boot bajo **Arquitectura Hexagonal (Puertos y Adaptadores)**. En la Evaluación Parcial 3 se extiende el pipeline CI/CD con **observabilidad, métricas de sistema, análisis de calidad y despliegue automatizado en la nube (AWS EC2)**.

**Asignatura:** Ingeniería DevOps — DOY0101
**Integrantes:** Yaquelin Rugel y Yeider Catari
**Organización GitHub:** [yaquelin2305](https://github.com/yaquelin2305)

---

## 1. Stack tecnológico y dependencias

| Tecnología | Versión | Rol |
|---|---|---|
| Java | 17 | Lenguaje principal |
| Spring Boot | 3.3.2 | Framework del microservicio |
| Spring Boot Actuator | 3.3.2 | Exposición de endpoints de salud y métricas |
| Spring Data JPA | 3.3.2 | Persistencia con abstracción de repositorios |
| PostgreSQL | 16 (Alpine) | Base de datos relacional |
| Docker / Docker Compose | v5.2.0 | Contenerización y orquestación de servicios |
| GitHub Actions | — | CI/CD automatizado en la nube |
| SonarCloud | — | Análisis estático de código y quality gate |
| JaCoCo | 0.8.12 | Reporte de cobertura de pruebas |
| AWS CloudWatch Agent | — | Recolección de métricas de sistema y logs desde EC2 |
| Amazon EC2 | t3.micro / Amazon Linux 2023 | Entorno de despliegue en la nube (AWS Academy) |

---

## 2. Arquitectura del microservicio (Hexagonal)

El microservicio implementa **Arquitectura Hexagonal** (también llamada Puertos y Adaptadores), que separa el dominio del negocio de los detalles de infraestructura. Esto garantiza que la lógica de negocio sea completamente independiente de frameworks, bases de datos o canales de entrada.

```
src/main/java/com/tickets/
│
├── domain/                        ← Núcleo del dominio (sin dependencias externas)
│   ├── model/
│   │   ├── Ticket.java            ← Entidad del dominio: id, descripcion, estado
│   │   └── User.java              ← Entidad de usuario: id, username, password
│   └── ports/
│       ├── TicketRepository.java  ← Puerto de salida: contrato de persistencia de tickets
│       ├── NotificationService.java ← Puerto de salida: contrato de notificaciones
│       └── UserService.java       ← Puerto de salida: contrato de autenticación
│
├── application/                   ← Casos de uso (orquestación de la lógica de negocio)
│   └── usecase/
│       ├── CreateTicketUseCase.java ← Crea un ticket con estado "ABIERTO" y notifica
│       └── GetTicketUseCase.java    ← Busca un ticket por ID
│
├── entrypoints/                   ← Adaptadores de entrada (HTTP)
│   ├── controller/
│   │   ├── TicketController.java  ← REST controller para el dominio de tickets
│   │   └── UserController.java    ← REST controller para registro y login de usuarios
│   └── dto/
│       └── CreateTicketRequest.java ← DTO de creación de ticket
│
└── infrastructure/                ← Adaptadores de salida (persistencia, notificaciones)
    ├── adapter/
    │   ├── TicketRepositoryImpl.java   ← Implementación del puerto de persistencia con JPA
    │   └── NotificationServiceImpl.java ← Implementación del puerto de notificaciones
    ├── entity/
    │   └── TicketEntity.java      ← Entidad JPA mapeada a tabla en PostgreSQL
    └── repository/
        ├── JpaTicketRepository.java   ← Repositorio Spring Data JPA para tickets
        └── UserRepository.java        ← Repositorio Spring Data JPA para usuarios
```

---

## 3. Endpoints de la API

La aplicación expone los siguientes endpoints en el puerto `8081`. Para probarla en vivo con la instancia EC2 activa, reemplazar `localhost` por la IP pública de la instancia.

### Tickets

| Método | Endpoint | Descripción | Body (JSON) |
|---|---|---|---|
| `GET` | `/tickets/ping` | Verificación rápida de que el servicio responde | — |
| `POST` | `/tickets` | Crea un nuevo ticket con estado `ABIERTO` | `{"id": 1, "descripcion": "Descripción del problema"}` |
| `GET` | `/tickets/{id}` | Obtiene un ticket por su ID | — |

**Ejemplo de prueba con curl:**
```bash
# Verificar disponibilidad
curl http://localhost:8081/tickets/ping

# Crear un ticket
curl -X POST http://localhost:8081/tickets \
  -H "Content-Type: application/json" \
  -d '{"id": 1, "descripcion": "Fallo en el sistema de pagos"}'

# Obtener un ticket por ID
curl http://localhost:8081/tickets/1
```

**Respuesta de `POST /tickets`:**
```json
{
  "id": 1,
  "descripcion": "Fallo en el sistema de pagos",
  "estado": "ABIERTO"
}
```

### Autenticación

| Método | Endpoint | Descripción | Body (JSON) |
|---|---|---|---|
| `POST` | `/auth/register` | Registra un nuevo usuario | `{"username": "user1", "password": "pass123"}` |
| `POST` | `/auth/login` | Autentica un usuario; responde `"Login OK"` o `"Error"` | `{"username": "user1", "password": "pass123"}` |

### Observabilidad (Spring Boot Actuator)

| Endpoint | Descripción |
|---|---|
| `GET /actuator/health` | Estado de la aplicación y conexión a la base de datos |
| `GET /actuator/metrics` | Métricas de JVM, HTTP y sistema |
| `GET /actuator/info` | Versión (`1.0.0`) y nombre (`tickets-app`) de la aplicación |

---

## 4. Suite de pruebas unitarias

Las pruebas se ejecutan con **JUnit 5** y **Mockito**, sin levantar el contexto completo de Spring para máxima velocidad. Cubren tres capas de la arquitectura hexagonal.

### `CreateTicketUseCaseTest`
Ubicación: `src/test/java/com/tickets/application/usecase/`

Prueba la lógica del caso de uso de creación de tickets usando mocks de `TicketRepository` y `NotificationService`.

| Test | Qué valida |
|---|---|
| `debeCrearTicketConEstadoAbierto` | El ticket retornado tiene el `id` y `descripcion` correctos, y su `estado` es siempre `"ABIERTO"` |
| `debeGuardarTicketYNotificar` | El repositorio recibe exactamente 1 llamada a `save()` y el servicio de notificación recibe exactamente 1 llamada a `sendNotification()` con el mensaje correcto |

### `GetTicketUseCaseTest`
Ubicación: `src/test/java/com/tickets/application/usecase/`

Prueba el caso de uso de consulta con mock de `TicketRepository`.

| Test | Qué valida |
|---|---|
| `debeRetornarTicketCuandoExiste` | Cuando el repositorio devuelve un ticket para el ID `1L`, el caso de uso lo retorna íntegro con `id`, `descripcion` y `estado` correctos |
| `debeRetornarNullCuandoNoExiste` | Cuando el repositorio devuelve `null` para el ID `99L`, el caso de uso también retorna `null` sin lanzar excepción |

### `TicketTest`
Ubicación: `src/test/java/com/tickets/domain/model/`

Prueba la entidad pura del dominio, sin ninguna dependencia externa.

| Test | Qué valida |
|---|---|
| `testPropiedadesDelModeloTicket` | El modelo `Ticket` instanciado con el constructor vacío y cargado vía setters expone los valores correctos por sus getters (`id`, `descripcion`, `estado`) |

### `TicketControllerTest`
Ubicación: `src/test/java/com/tickets/entrypoints/controller/`

Prueba la capa HTTP usando `@WebMvcTest` con `MockMvc` (sin base de datos ni contexto completo).

| Test | Qué valida |
|---|---|
| `testEndpointCrearTicketDeberiaRetornarStatusOkYElTicketCreado` | Un `POST /tickets` con JSON válido retorna `HTTP 200 OK` y el cuerpo de respuesta contiene `id`, `descripcion` y `estado: "ABIERTO"` correctos |

### `TicketsApplicationTests`
Verificación de que el contexto de Spring Boot arranca sin errores (smoke test).

### Cobertura de pruebas (JaCoCo)
El reporte de cobertura se genera automáticamente en cada ejecución del pipeline y se publica como artefacto `jacoco-report-<sha>` en GitHub Actions.

| Paquete | Cobertura |
|---|---|
| `application.usecase` | 100% |
| `domain.model` | ~70% |
| `entrypoints.controller` | ~50% |
| **Total** | **~32%** |

---

## 5. Estrategia de ramas (GitFlow)

El proyecto aplica **GitFlow** como estrategia de control de versiones colaborativo:

```
main          → código en producción; solo recibe merges desde release/* o hotfix/*
develop       → integración continua; base de todas las feature/*
feature/*     → una rama por funcionalidad, se abre desde develop y cierra con PR a develop
release/*     → preparación de entrega; se abre desde develop y cierra en main y develop
hotfix/*      → corrección urgente en producción; se abre desde main y cierra en main y develop
```

**Reglas aplicadas:**
- Ningún push directo a `main` ni a `develop`.
- Toda funcionalidad nueva parte de una rama `feature/` creada desde `develop`.
- Los nombres de rama usan kebab-case.
- El pipeline se ejecuta automáticamente en cada `push` y `pull_request` a `main` o `develop`.

---

## 6. Arquitectura del pipeline CI/CD

El pipeline (`.github/workflows/ci.yml`) se dispara en `push` y `pull_request` sobre `main` y `develop`. Tres jobs encadenados con `needs`:

```
[ Push / PR a main o develop ]
               │
               ▼
┌─────────────────────────────────────┐
│  JOB 1: Tests & SonarCloud         │  → GitHub Cloud (ubuntu-latest)
│  - Pruebas unitarias (JUnit)        │
│  - Reporte Surefire (artefacto)     │
│  - Análisis SonarCloud + JaCoCo     │
│  - Compilación y publicación JAR    │
└─────────────────────────────────────┘
               │ quality gate aprobado
               ▼
┌─────────────────────────────────────┐
│  JOB 2: Build & Push Docker Image  │  → GitHub Cloud (ubuntu-latest)
│  - Build multietapa (Dockerfile)    │
│  - Push a GHCR con tags latest/SHA  │
└─────────────────────────────────────┘
               │ imagen publicada
               ▼
┌─────────────────────────────────────┐
│  JOB 3: Deploy en EC2              │  → Self-hosted runner (EC2 AWS)
│  - docker compose up -d             │
│  - App (8081) + PostgreSQL activos  │
└─────────────────────────────────────┘
```

### Mecanismo de parada ante fallas críticas (IE6)

Si SonarCloud rechaza el quality gate (bugs críticos, vulnerabilidades o cobertura insuficiente), el job `sonar` falla con código de error distinto de cero. Los jobs `build-image` y `deploy` tienen `needs: sonar` / `needs: build-image`, por lo que **nunca se ejecutan**. El pipeline se interrumpe automáticamente antes de generar o desplegar imagen alguna, protegiendo el entorno productivo.

---

## 7. Contenerización

### Dockerfile (multietapa)

```
Etapa 1 — build:    maven:3.9.8-eclipse-temurin-17-alpine → compila y genera el JAR
Etapa 2 — runtime:  eclipse-temurin:17-jre-alpine         → imagen final ligera
```

Prácticas de seguridad aplicadas en el `Dockerfile`:
- **Usuario no-root:** se crea el usuario `devopsuser` y la aplicación corre bajo ese usuario, nunca como `root`.
- **Imagen mínima:** Alpine reduce la superficie de ataque al mínimo.
- **Puerto declarado:** `EXPOSE 8081`.

### Docker Compose (`docker-compose.yml`)

Orquesta dos servicios:

| Servicio | Imagen | Puerto | Rol |
|---|---|---|---|
| `tickets_db` | `postgres:16-alpine` | 5432 (interno) | Base de datos PostgreSQL |
| `app` | `ghcr.io/yaquelin2305/tickets-app:<sha>` | 8081 | Microservicio Spring Boot |

**Configuraciones de resiliencia:**
- `depends_on` con `condition: service_healthy` — la app espera a que PostgreSQL pase su healthcheck (`pg_isready`) antes de arrancar.
- `restart_policy: on-failure` con `max_attempts: 3` — reinicio automático ante caídas.
- Límites de recursos: `cpus: '0.50'`, `memory: 512M` por servicio.
- Volumen persistente `postgres_data` para que los datos sobrevivan reinicios.

---

## 8. Observabilidad con AWS CloudWatch (IE1 / IE2)

### 8.1 Spring Boot Actuator

La aplicación expone tres endpoints de observabilidad listos para ser consultados por herramientas de monitoreo:

| Endpoint | Descripción |
|---|---|
| `/actuator/health` | Estado de la app y de la conexión a PostgreSQL. Muestra `"status": "UP"` cuando todo funciona. |
| `/actuator/metrics` | Métricas internas de JVM (heap, GC), HTTP (latencias, conteo de peticiones) y sistema. |
| `/actuator/info` | Nombre (`tickets-app`) y versión (`1.0.0`) de la aplicación. |

### 8.2 CloudWatch Agent en EC2

El CloudWatch Agent instalado en la instancia EC2 recolecta automáticamente **cada 60 segundos**:

**Métricas de sistema (namespace `CWAgent`):**

| Métrica | Descripción |
|---|---|
| `cpu_usage_user` + `cpu_usage_system` | Uso de CPU por procesos de usuario y sistema |
| `cpu_usage_idle` | Porcentaje de CPU libre |
| `mem_used_percent` | Porcentaje de memoria RAM utilizada |
| `disk_used_percent` | Porcentaje de disco utilizado (partición `/`) |
| `disk_inodes_free` | Inodos libres disponibles |
| `net_bytes_recv` + `net_bytes_sent` | Tráfico de red entrante y saliente |
| `diskio_io_time` | Tiempo activo de operaciones de disco I/O |

**Logs de contenedores Docker:**

| Parámetro | Valor |
|---|---|
| Ruta monitoreada | `/var/lib/docker/containers/*/*.log` |
| Log group en CloudWatch | `/tickets-app/docker` |
| Permisos requeridos | El agente corre como `root` para leer los archivos de log de Docker (permisos `600`) |

### 8.3 Cómo las métricas apoyan decisiones técnicas (IE4)

| Métrica / Evento | Decisión técnica que habilita |
|---|---|
| `cpu_usage_user` > 80% sostenido | Escalar la instancia EC2 (t3.small → t3.medium) o revisar queries ineficientes en PostgreSQL |
| `mem_used_percent` > 85% | Aumentar el límite `memory: 512M` en `docker-compose.yml` o revisar memory leaks en la JVM |
| Errores `ERROR`/`WARN` en logs Docker | Detectar excepciones no controladas antes de que el usuario las reporte; priorizar hotfix |
| `disk_used_percent` > 70% | Ejecutar `docker system prune` para limpiar imágenes antiguas y recuperar espacio |
| `/actuator/health` retorna `DOWN` | El `restart_policy` de Compose reinicia el contenedor; si persiste, revisar conectividad con PostgreSQL |

---

## 9. Dashboard de métricas en CloudWatch (IE3)

Se configuró el dashboard `tickets-app-dashboard` en AWS CloudWatch con 7 widgets integrados al proceso CI/CD:

| Widget | Métrica / Fuente | Tipo de visualización |
|---|---|---|
| **CPU** | `cpu_usage_user` + `cpu_usage_system` (CWAgent) | Línea (serie temporal) |
| **Memoria** | `mem_used_percent` (CWAgent) | Número en tiempo real |
| **Disco** | `disk_used_percent` (CWAgent) | Medidor (gauge) |
| **Red** | `net_bytes_recv` + `net_bytes_sent` (CWAgent) | Línea (serie temporal) |
| **Logs de contenedores** | Log group `/tickets-app/docker` | Widget de logs |
| **Cobertura de Pruebas** | Reporte JaCoCo — artefacto `jacoco-report-<sha>` de GitHub Actions | Texto personalizado |
| **Tiempo de Despliegue** | Duración de cada job del pipeline (GitHub Actions) | Texto personalizado |

---

## 10. Políticas de cumplimiento (IE5)

| Herramienta | Política aplicada |
|---|---|
| **SonarCloud** | Quality gate bloquea el pipeline si hay bugs críticos, vulnerabilidades o cobertura por debajo del umbral definido. Si falla, los jobs siguientes (`build-image`, `deploy`) nunca se ejecutan. |
| **Branch protection** | Las ramas `main` y `develop` requieren PR aprobado y status checks verdes antes de merge. Push directo bloqueado. |
| **Dependabot** | Escaneo semanal de dependencias Maven y GitHub Actions (`.github/dependabot.yml`); abre PRs automáticos para versiones con CVE conocidos. |
| **Dockerfile — usuario no-root** | La aplicación corre como `devopsuser`, eliminando el riesgo de escalada de privilegios dentro del contenedor. |
| **Gestión de Secrets** | Todas las credenciales (`SONAR_TOKEN`, `DB_PASSWORD`, `GITHUB_TOKEN`) se gestionan como GitHub Secrets; nunca aparecen en texto plano en el código fuente ni en los logs del pipeline. |

---

## 11. Infraestructura de despliegue en la nube (IE2)

| Componente | Detalle |
|---|---|
| Proveedor | AWS Academy |
| Instancia | EC2 t3.micro |
| Sistema Operativo | Amazon Linux 2023 |
| Orquestación | Docker Compose v5.2.0 |
| Runner CI/CD | GitHub Actions Self-Hosted Runner (servicio `systemd` en la EC2) |
| Observabilidad | CloudWatch Agent (instalado y gestionado desde la consola AWS) |
| Perfil IAM | `LabInstanceProfile` con permisos de escritura en CloudWatch Metrics y Logs |

**Servicios activos en la EC2 tras el deploy:**

| Servicio | Puerto expuesto | Imagen |
|---|---|---|
| `app` (tickets-app) | `8081` (acceso externo) | `ghcr.io/yaquelin2305/tickets-app:<sha>` |
| `tickets_db` (PostgreSQL) | `5432` (solo red interna Docker) | `postgres:16-alpine` |

---

## 12. Secrets requeridos en GitHub Actions

| Secret | Descripción |
|---|---|
| `SONAR_TOKEN` | Token de autenticación de SonarCloud |
| `SONAR_PROJECT_KEY` | Clave del proyecto en SonarCloud |
| `SONAR_ORGANIZATION` | Nombre de la organización en SonarCloud (`yaquelin2305`) |
| `DB_PASSWORD` | Contraseña de PostgreSQL (inyectada al contenedor en el deploy) |

---

## 13. Guía rápida de ejecución (GitFlow)

```bash
# 1. Crear rama feature desde develop
git checkout develop && git pull origin develop
git checkout -b feature/nombre-funcionalidad

# 2. Desarrollar, commitear en español
git add ArchivoCambiado.java
git commit -m "feat: descripción breve del cambio"
git push origin feature/nombre-funcionalidad

# 3. Abrir PR hacia develop en GitHub
# 4. El pipeline valida automáticamente — SonarCloud bloquea si hay fallas críticas
# 5. Merge solo si todos los status checks pasan
# 6. Al completar la release, abrir PR de develop → main
```

---

## 14. Evidencias de funcionamiento

### Evidencia 1: Pipeline CI/CD exitoso — 3 jobs en verde

Pipeline ejecutado correctamente mostrando los 3 jobs encadenados completados: Tests & SonarCloud Analysis, Build & Push Docker Image y Deploy with Docker Compose. El pipeline completo se ejecutó en aproximadamente 2 minutos con todos los status checks en verde.

<img width="1303" height="439" alt="Pipeline CI/CD exitoso con 3 jobs en verde" src="https://github.com/user-attachments/assets/edaa36a7-01a8-4146-9430-fc6f5764ced7" />

---

### Evidencia 2: Contenedores activos en EC2 tras el deploy

Salida del comando `docker ps` en la instancia EC2 Amazon Linux 2023, mostrando los dos contenedores en estado `healthy` o `Up`: la aplicación `tickets-app` escuchando en el puerto `8081` y la base de datos `postgres:16-alpine` en la red interna Docker.

<img width="1883" height="376" alt="docker ps con contenedores activos en EC2" src="https://github.com/user-attachments/assets/73fefb0a-f204-4013-ae8c-94bd430e10ea" />

---

### Evidencia 3: Dashboard CloudWatch — métricas de sistema en tiempo real

Dashboard `tickets-app-dashboard` mostrando las métricas recolectadas por el CloudWatch Agent: uso de CPU (cpu_usage_user + cpu_usage_system), porcentaje de memoria utilizada, porcentaje de disco utilizado y tráfico de red (bytes recibidos y enviados).

<img width="1653" height="548" alt="Dashboard CloudWatch con métricas de sistema" src="https://github.com/user-attachments/assets/7abb0815-8428-44aa-83a6-597fee7d17ab" />

---

### Evidencia 4: Dashboard CloudWatch — logs de contenedores Docker

Widget de logs del dashboard `tickets-app-dashboard` mostrando entradas en tiempo real del log group `/tickets-app/docker`. El CloudWatch Agent captura los logs de todos los contenedores Docker desde `/var/lib/docker/containers/*/*.log` y los centraliza en CloudWatch Logs.

<img width="1444" height="488" alt="Logs de Docker centralizados en CloudWatch" src="https://github.com/user-attachments/assets/221a1687-e42f-468b-8ca2-6bbfbc1e7ea6" />

---

### Evidencia 5: Dashboard CloudWatch — widgets de cobertura y tiempo de despliegue

Widgets de texto del dashboard mostrando la cobertura de pruebas reportada por JaCoCo por paquete y el tiempo de duración de cada job del último pipeline exitoso (total aproximado: 2m 27s), integrando métricas del proceso CI/CD directamente en el dashboard de observabilidad.

<img width="1882" height="284" alt="Widgets de cobertura JaCoCo y tiempo de despliegue" src="https://github.com/user-attachments/assets/7f4b38e6-b58a-4aa8-a96a-d59ddccd0f9f" />

---

### Evidencia 6: SonarCloud — Quality Gate y detención del pipeline (falla crítica)

SonarCloud detectó issues de seguridad en el código (vulnerabilidades críticas en `UserController`) y el Quality Gate falló. Al fallar el job `sonar`, los jobs `build-image` y `deploy` nunca se ejecutaron, demostrando que el mecanismo de interrupción del pipeline ante fallas críticas funciona correctamente (IE6).

<img width="951" height="298" alt="SonarCloud Quality Gate Failed — pipeline detenido" src="https://github.com/user-attachments/assets/f62a2631-4af3-45b3-8286-71ef5633c7d4" />

---

### Evidencia 7: Branch Protection activa en GitHub (falta por agregar)

Configuración de reglas de protección de ramas `main` y `develop` en GitHub mostrando la regla "Require a pull request before merging" activa. *(Captura pendiente — requiere acceso de administrador al repositorio para visualizar la configuración completa.)*

---

## 15. Declaración de uso de Inteligencia Artificial

En el desarrollo de este proyecto se utilizó **Google Gemini** como herramienta de apoyo para la generación de configuraciones YAML, actualización de dependencias Maven, estructuración de documentación técnica y asistencia en la resolución de problemas de configuración del CloudWatch Agent. Las decisiones de arquitectura, análisis técnico, justificaciones y reflexiones individuales fueron realizadas íntegramente por los integrantes del equipo. Todo contenido generado con IA fue revisado y validado antes de integrarse al repositorio.

Criterios de uso alineados con: [https://bibliotecas.duoc.cl/ia](https://bibliotecas.duoc.cl/ia)

---

## 16. Reflexión académica

**Estudiante: Yaquelin Rugel**

Asignatura: Ingeniería DevOps

El desarrollo e implementación de este pipeline completo representó un gran desafío de aprendizaje conceptual y técnico en mi formación. A nivel de infraestructura, comprender la separación estricta de responsabilidades a través de un Dockerfile optimizado en múltiples etapas y restringir por completo los accesos de ejecución del software mediante un usuario no-root me demostró con claridad que la seguridad informática y de operaciones no es un añadido opcional del final de un proyecto, sino un pilar estructural que debe nacer desde la misma concepción de la primera línea de código. Asimismo, la automatización del flujo híbrido combinando GitHub Actions con un Runner en nuestra propia máquina me enseñó el valor real del Despliegue Continuo (CD): lograr que el código se pruebe, audite y despliegue solo mediante el control trazable del SHA de los commits me brindó una perspectiva real de cómo se gobierna, protege y escala el software moderno en entornos industriales reales de producción.

---

**Estudiante: Yeider Catari**

Asignatura: Ingeniería DevOps

Honestamente el pipeline nunca nos funcionó a la primera, siempre había algo que ajustar, pero eso mismo fue lo que más me enseñó: buscar la causa, entender el error y resolverlo. Lo que más me aportó de esta entrega fue aprender a usar CloudWatch para monitorear la instancia EC2 en tiempo real, ver el consumo de CPU, memoria y disco del microservicio y construir un dashboard que centralizara todo eso. También nos costó bastante que los logs de Docker aparecieran en el dashboard; tuvimos que investigar los permisos del agente de CloudWatch y resolver conflictos en los archivos de configuración, pero cuando por fin funcionó se notó el valor de tener esa visibilidad. A nivel de equipo, el trabajo con Yaquelin fue efectivo: entre los dos fuimos aprendiendo y resolviendo los problemas a medida que aparecían.

# Microservicio de Tickets - Observabilidad y DevOps

Este repositorio contiene el **Microservicio de Tickets** desarrollado en Java 17 con Spring Boot bajo Arquitectura Hexagonal. En esta entrega se extiende el pipeline CI/CD con **observabilidad, métricas de sistema, análisis de calidad y despliegue automatizado en la nube (AWS EC2)**.

---

## 1. Stack tecnológico y dependencias

| Tecnología | Versión | Rol |
|---|---|---|
| Java | 17 | Lenguaje principal |
| Spring Boot | 3.3.2 | Framework del microservicio |
| Spring Boot Actuator | 3.3.2 | Exposición de métricas y health checks |
| Spring Data JPA | 3.3.2 | Persistencia |
| PostgreSQL | 16 (Alpine) | Base de datos relacional |
| Docker / Docker Compose | v5.2.0 | Contenerización y orquestación |
| GitHub Actions | — | CI/CD automatizado |
| SonarCloud | — | Análisis estático y quality gate |
| JaCoCo | 0.8.12 | Cobertura de pruebas |
| AWS CloudWatch Agent | — | Recolección de métricas y logs |
| Amazon EC2 | t3.micro / Amazon Linux 2023 | Entorno de despliegue en la nube |

---

## 2. Estrategia de ramas (GitFlow)

El proyecto aplica **GitFlow** como estrategia de control de versiones:

```
main          → código en producción; solo recibe merges desde release/* o hotfix/*
develop       → integración continua; base de todas las feature/*
feature/*     → una rama por funcionalidad, se abre desde develop y cierra con PR a develop
release/*     → preparación de entrega; se abre desde develop y cierra en main y develop
hotfix/*      → corrección urgente en producción; se abre desde main y cierra en main y develop
```

**Reglas:**
- Ningún push directo a `main` ni a `develop`.
- Toda funcionalidad nueva parte de una rama `feature/` y entra por Pull Request.
- El pipeline se ejecuta automáticamente en cada PR y push a `main` o `develop`.

---

## 3. Arquitectura del pipeline CI/CD

El pipeline (`.github/workflows/ci.yml`) se dispara en `push` y `pull_request` sobre `main` y `develop`. Tres jobs encadenados con `needs`:

```
[ Push / PR a main o develop ]
               │
               ▼
┌─────────────────────────────────────┐
│  JOB 1: Tests & SonarCloud         │  → GitHub Cloud (ubuntu-latest)
│  - Pruebas unitarias (JUnit)        │
│  - Análisis SonarCloud              │
│  - Reporte JaCoCo (cobertura)       │
│  - Artefacto JAR                    │
└─────────────────────────────────────┘
               │ quality gate aprobado
               ▼
┌─────────────────────────────────────┐
│  JOB 2: Build & Push Docker Image  │  → GitHub Cloud (ubuntu-latest)
│  - Build multietapa (Dockerfile)    │
│  - Push a GHCR con tag SHA          │
└─────────────────────────────────────┘
               │ imagen publicada
               ▼
┌─────────────────────────────────────┐
│  JOB 3: Deploy en EC2              │  → Self-hosted runner (EC2 AWS)
│  - docker compose up -d             │
│  - App + PostgreSQL en producción   │
└─────────────────────────────────────┘
```

### Mecanismo de parada ante fallas críticas (IE6)

Si SonarCloud rechaza el quality gate (bugs, vulnerabilidades o cobertura insuficiente), el job `sonar` falla y los jobs `build-image` y `deploy` **nunca se ejecutan**. El pipeline se interrumpe automáticamente antes de generar o desplegar imagen alguna.

---

## 4. Observabilidad con AWS CloudWatch (IE1 / IE2)

### 4.1 Spring Boot Actuator

La aplicación expone los siguientes endpoints de observabilidad en el puerto 8081:

| Endpoint | Descripción |
|---|---|
| `/actuator/health` | Estado de la app y conexión a la base de datos |
| `/actuator/metrics` | Métricas de JVM, HTTP y sistema |
| `/actuator/info` | Versión y nombre de la aplicación |

### 4.2 CloudWatch Agent en EC2

El CloudWatch Agent instalado en la instancia EC2 recolecta automáticamente cada 60 segundos:

**Métricas de sistema:**
- CPU: `cpu_usage_user`, `cpu_usage_system`, `cpu_usage_idle`
- Memoria: `mem_used_percent`
- Disco: `disk_used_percent`, `disk_inodes_free`
- Red: `net_bytes_recv`, `net_bytes_sent`
- Disco I/O: `diskio_io_time`

**Logs de contenedores Docker:**
- Ruta monitoreada: `/var/lib/docker/containers/*/*.log`
- Log group en CloudWatch: `/tickets-app/docker`
- Filtro: líneas que contengan `ERROR` o `WARN`

### 4.3 Cómo las métricas apoyan decisiones técnicas (IE4)

| Métrica | Decisión técnica que habilita |
|---|---|
| CPU > 80% sostenido | Escalar la instancia o revisar queries ineficientes |
| `mem_used_percent` > 85% | Ajustar límites de memoria en docker-compose.yml |
| Errores `ERROR\|WARN` en logs | Detectar fallos en la app antes de que el usuario lo reporte |
| `disk_used_percent` > 70% | Limpiar imágenes Docker antiguas o ampliar volumen |
| `/actuator/health` status DOWN | Reinicio automático del contenedor (restart_policy en Compose) |

---

## 5. Dashboard de métricas (IE3)

El dashboard de CloudWatch consolida en una sola vista:

- **Utilización de CPU (%)** — permite detectar picos anómalos durante el despliegue
- **Memoria usada (%)** — valida que los límites del Compose sean suficientes
- **Errores en logs** — muestra la tasa de errores de la aplicación en tiempo real
- **Tráfico de red** — indica actividad de usuarios o pipelines activos
- **Cobertura de pruebas** — reportada por JaCoCo en cada ejecución del pipeline (artefacto `jacoco-report-<sha>`)

---

## 6. Políticas de cumplimiento (IE5)

| Herramienta | Política |
|---|---|
| **SonarCloud** | Quality gate bloquea el pipeline si hay bugs críticos, vulnerabilidades o cobertura < umbral definido |
| **Branch protection** | `main` y `develop` requieren PR aprobado y status checks verdes antes de merge |
| **Dependabot** | Escaneo semanal de dependencias Maven y GitHub Actions; abre PRs automáticos para versiones con CVE |
| **Dockerfile** | Usuario no-root (`devopsuser`) — principio de menor privilegio |
| **Secrets** | Credenciales gestionadas como GitHub Secrets; nunca en texto plano en el código |

---

## 7. Infraestructura de despliegue (IE2)

| Componente | Detalle |
|---|---|
| Proveedor | AWS Academy |
| Instancia | EC2 t3.micro |
| SO | Amazon Linux 2023 |
| IP pública | 54.165.199.216 |
| Orquestación | Docker Compose v5.2.0 |
| Runner CI/CD | Self-hosted GitHub Actions runner (servicio systemd) |
| Observabilidad | CloudWatch Agent (instalado vía SSM) |
| IAM | LabInstanceProfile con permisos CloudWatch |

**Servicios activos en la EC2:**

| Servicio | Puerto | Imagen |
|---|---|---|
| tickets-app | 8081 | `ghcr.io/<repo>/tickets-app:<sha>` |
| tickets_db | 5432 (interno) | `postgres:16-alpine` |

---

## 8. Guía rápida de ejecución

### Secrets requeridos en GitHub Actions

| Secret | Descripción |
|---|---|
| `SONAR_TOKEN` | Token de autenticación SonarCloud |
| `SONAR_PROJECT_KEY` | Clave del proyecto en SonarCloud |
| `SONAR_ORGANIZATION` | Organización en SonarCloud |
| `DB_PASSWORD` | Contraseña de PostgreSQL |

### Flujo de trabajo

```bash
# 1. Crear rama feature desde develop
git checkout develop && git pull origin develop
git checkout -b feature/mi-funcionalidad

# 2. Desarrollar y commitear (en español)
git add archivo.java
git commit -m "feat: descripción breve del cambio"
git push origin feature/mi-funcionalidad

# 3. Abrir PR hacia develop en GitHub
# 4. El pipeline valida automáticamente — merge solo si pasa el quality gate
```

---

## 9. Evidencias de funcionamiento

### Evidencia 1: Despliegue de la aplicación y contenedores activos
<img width="1123" height="555" alt="Image" src="https://github.com/user-attachments/assets/23ab17d2-4074-4c53-ab99-586f114f03fd" />

### Evidencia 2: Monitoreo y logs en Docker Desktop
<img width="1210" height="430" alt="Image" src="https://github.com/user-attachments/assets/40a23cd1-0cb1-4d43-bf38-29cfe285137b" />

### Evidencia 3: Respuesta exitosa del endpoint /tickets/ping
<img width="708" height="150" alt="Image" src="https://github.com/user-attachments/assets/84a3558a-4857-4ef2-a7f6-a31d10860ce4" />

---

## 10. Declaración de uso de Inteligencia Artificial

En el desarrollo de este proyecto se utilizó **Claude Code (Anthropic)** como herramienta de apoyo para generación de configuraciones YAML, actualización de dependencias Maven, estructuración de documentación técnica y revisión de sintaxis. Las decisiones de arquitectura, análisis técnico, justificaciones y reflexiones individuales fueron realizadas íntegramente por los integrantes del equipo. Todo uso de IA fue revisado y validado antes de integrarse al repositorio. Referencia de citación: https://bibliotecas.duoc.cl/ia

---

## 11. Reflexión académica

**Estudiante: Yaquelin Rugel**

Asignatura: Ingeniería DevOps

El desarrollo e implementación de este pipeline completo representó un gran desafío de aprendizaje conceptual y técnico en mi formación. A nivel de infraestructura, comprender la separación estricta de responsabilidades a través de un Dockerfile optimizado en múltiples etapas y restringir por completo los accesos de ejecución del software mediante un usuario no-root me demostró con claridad que la seguridad informática y de operaciones no es un añadido opcional del final de un proyecto, sino un pilar estructural que debe nacer desde la misma concepción de la primera línea de código. Asimismo, la automatización del flujo híbrido combinando GitHub Actions con un Runner en nuestra propia máquina me enseñó el valor real del Despliegue Continuo (CD): lograr que el código se pruebe, audite y despliegue solo mediante el control trazable del SHA de los commits me brindó una perspectiva real de cómo se gobierna, protege y escala el software moderno en entornos industriales reales de producción.

---

**Estudiante: Yeider Catari**

Asignatura: Ingeniería DevOps

Al inicio del ciclo académico me parecía un proceso excesivo y complejo configurar una arquitectura tan robusta y detallada para un microservicio pequeño. Sin embargo, en el instante en el que vi el pipeline interactuar por primera vez, ver cómo la nube compila, SonarCloud audita la calidad en segundos y la consola de mi propia computadora descarga y actualiza la infraestructura local de Docker de forma 100% automática y sin intervención manual, logré comprender la importancia real de estas metodologías. En un equipo de desarrollo industrial real, este nivel de automatización ahorra cientos de horas de trabajo repetitivo y erradica por completo la típica excusa de desarrollo de "en mi máquina local sí funciona". Comprendí que los errores detectados de forma temprana en las etapas iniciales de la Integración Continua (CI) son infinitamente más económicos y fáciles de solucionar que las fallas que logran llegar a producción, y ese factor por sí solo justifica plenamente el esfuerzo de arquitectura detrás de toda la configuración.

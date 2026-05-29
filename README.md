Markdown
# 🎟️ Microservicio de Tickets - Capa DevOps Avanzada

Este repositorio contiene la solución del **Microservicio de Tickets** desarrollado en Java 17 con Spring Boot bajo los lineamientos de la Arquitectura Hexagonal. En esta entrega, se ha consolidado un ciclo completo de **Integración Continua (CI)** y **Entrega Continua (CD)** totalmente automatizado a través de **GitHub Actions**, incorporando empaquetado seguro, análisis estático de código, gobernanza estricta de accesos y despliegues controlados en entornos aislados.

---

## 🚀 1. Arquitectura del Pipeline de CI/CD

El flujo de automatización se encuentra estructurado de manera secuencial a través de dependencias jerárquicas (`needs`) dentro de `.github/workflows/ci.yml`. El pipeline está programado para ejecutarse ante eventos de `push` y `pull_request`, garantizando que ningún código defectuoso o vulnerable llegue a la rama principal sin validación previa.

### 📊 Diagrama del Flujo Automatizado
```text
  [ Evento de Git ]
         │
         ▼
 ┌───────────────┐
 │    Stage 1    │
 │ Test & Sonar  │ ──► (Genera artefactos de pruebas, Jacoco y compila el .JAR)
 └───────────────┘
         │ (Si todo pasa con éxito)
         ▼
 ┌───────────────┐
 │    Stage 2    │
 │ Build & Push  │ ──► (Construye la imagen Docker y la publica en GHCR)
 └───────────────┘
         │ (Si la imagen sube correctamente)
         ▼
 ┌───────────────┐
 │    Stage 3    │
 │  Deploy App   │ ──► (Simula el despliegue multi-contenedor con Docker Compose)
 └───────────────┘
🛠️ 2. Descripción de las Etapas del Pipeline y Cumplimiento de Indicadores
🧪 Etapa 1: Análisis de Calidad, Cobertura y Generación de Artefactos (sonar)
Ejecución de Pruebas: Se lanzan de forma automatizada los tests unitarios clave (CreateTicketUseCaseTest, GetTicketUseCaseTest, TicketTest) con Maven.

Auditoría y Reportes (IE2): Utilizando la acción actions/upload-artifact@v4, se capturan y exportan los resultados de Surefire (reporte-pruebas-unitarias) e incluso si una prueba falla, el artefacto se genera obligatoriamente para facilitar el diagnóstico.

Calidad y Cobertura: Se integra la herramienta JaCoCo para auditar el porcentaje de cobertura del código y se publica su reporte (jacoco-report). Adicionalmente, el proyecto se vincula directamente con la organización en SonarCloud (yaquelin2305) para bloquear el pipeline si no se superan las métricas mínimas de seguridad.

Compilación (IE4): Al finalizar con éxito el análisis, se empaqueta la aplicación generando el archivo binario ejecutable (.jar) y guardándolo como artefacto trazable.

🐳 Etapa 2: Contenerización y Empaquetado Multietapa (build-image)
Dockerfile Optimizado (IE1, IE5): Se utiliza un esquema de Construcción Multi-etapa (Multi-stage). La fase de compilación utiliza una imagen robusta de Maven (maven:3.9.8-eclipse-temurin-17-alpine) para resolver dependencias físicas aisladas, mientras que la fase final migra únicamente el .jar compilado a una imagen JRE de Alpine extremadamente ligera (eclipse-temurin:17-jre-alpine), reduciendo drásticamente la superficie de ataque y el tamaño en disco.

Seguridad y Menor Privilegio (IE3): Se rompe el uso del usuario por defecto root. Se crea explícitamente el grupo devopsgroup y el usuario restringido devopsuser para ejecutar los comandos internos del contenedor de manera segura.

Gobernanza y Publicación: Se elevan dinámicamente los privilegios del GITHUB_TOKEN mediante la propiedad permissions: packages: write, lo que permite el logeo y subida automatizada de la imagen hacia el registro de paquetes oficial de GitHub Container Registry (ghcr.io).

📦 Etapa 3: Orquestación y Despliegue Simulado (deploy)
Orquestación Multi-contenedor (IE1): Mediante docker-compose.yml, el pipeline simula un despliegue completo de la solución tecnológica interactuando en red. Levanta concurrentemente el contenedor de la aplicación Spring Boot (app) y el contenedor del motor de base de datos relacional PostgreSQL (tickets_db).

Trazabilidad Absoluta: Cumpliendo con estrictos estándares de gobernanza, cada imagen construida es etiquetada unívocamente usando el identificador corto del commit actual de Git (${{ github.sha }}). Durante el despliegue en Docker Compose, se inyecta la variable de entorno IMAGE_TAG para asegurar la reproducibilidad exacta del software en ejecución.

⚙️ 3. Ejecución y Configuración en Entornos Locales
🔑 3.1 Carga de Secretos en el Repositorio
Para que el pipeline funcione correctamente, se configuraron las siguientes variables en la sección de Settings > Secrets and variables > Actions de GitHub:

SONAR_TOKEN: Token de autenticación de SonarCloud.

SONAR_PROJECT_KEY: Identificador único del proyecto en la plataforma.

SONAR_ORGANIZATION: Nombre de la organización (yaquelin2305).

DB_PASSWORD: Credencial segura para la inicialización y conexión del motor PostgreSQL.

💻 3.2 Despliegue Manual con Docker Compose
Si se desea replicar localmente la infraestructura multi-contenedor, ejecute los siguientes comandos en la raíz del proyecto:

Bash
# Definir variables mínimas necesarias en tu terminal local
export DB_PASSWORD=tu_clave_segura_aqui

# Construir las imágenes y levantar los servicios en segundo plano
docker compose up -d

# Validar que ambos servicios se encuentren saludables y corriendo
docker ps
🎓 4. Reflexión Académica 
Estudiante: Yaquelin Rugel

Asignatura:  Ingenieria DevOps 

"El desarrollo e implementación de este pipeline representó un gran desafío de aprendizaje conceptual y técnico. A nivel de infraestructura, comprender la separación de responsabilidades a través de un Dockerfile multi-etapa y restringir los accesos de ejecución mediante un usuario no-root me demostró que la seguridad no es un añadido del final, sino un pilar fundamental desde la misma línea de código. Asimismo, la automatización del pipeline con GitHub Actions me enseñó el valor real de la Integración Continua: lograr corregir errores de permisos en tiempo de compilación y asegurar que todo cambio se audite de extremo a extremo mediante el SHA de los commits me brindó una visión clara de cómo se gobierna y protege el software moderno en entornos de producción reales."
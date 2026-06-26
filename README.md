Markdown
# 🎟️ Microservicio de Tickets - Capa DevOps Avanzada

Este repositorio contiene la solución del **Microservicio de Tickets** desarrollado en Java 17 con Spring Boot bajo los lineamientos de la Arquitectura Hexagonal. En esta entrega, se ha consolidado un ciclo completo de **Integración Continuo (CI)** y **Despliegue Continuo (CD)** totalmente automatizado a través de **GitHub Actions**, incorporando empaquetado seguro, análisis estático de código, gobernanza estricta de accesos y despliegues controlados en entornos aislados de infraestructura local mediante un **Self-Hosted Runner**.

---

## 📋 1. ¿De qué se trata el Proyecto? (Inducción para Nuevos Desarrolladores)

Este microservicio se encarga de la gestión del ciclo de vida de tickets de soporte técnico o de atención. Al estar diseñado bajo **Arquitectura Hexagonal (Puertos y Adaptadores)**, el núcleo del negocio (las reglas de qué es un ticket) está completamente desacoplado de la base de datos, de la web y de los controladores de Spring Framework. Esto permite que el sistema sea altamente mantenible, escalable y fácil de probar de forma aislada.

---

## 🚀 2. Arquitectura del Pipeline de CI/CD

El flujo de automatización se encuentra estructurado de manera secuencial a través de dependencias jerárquicas (`needs`) dentro de `.github/workflows/ci.yml`. El pipeline está programado para ejecutarse ante eventos de `push` y `pull_request` en las ramas `main` y `develop`, garantizando que ningún código defectuoso o vulnerable llegue a producción sin validación previa.

### 📊 Diagrama del Flujo Automatizado e Infraestructura híbrida

```text
  [ Evento de Git: Push / PR ]
               │
               ▼
 ┌────────────────────────────────────────┐
 │      STAGE 1: Tests & SonarCloud       │  --> Ejecuta en la Nube (GitHub Cloud)
 │      (Compila, testea y audita)        │  --> Genera reportes de JaCoCo y Surefire
 └────────────────────────────────────────┘
               │ (Si el Quality Gate es Exitoso)
               ▼
 ┌────────────────────────────────────────┐
 │     STAGE 2: Build & Push Docker       │  --> Ejecuta en la Nube (GitHub Cloud)
 │     (Construye y publica la imagen)   │  --> Sube la imagen empaquetada a GHCR
 └────────────────────────────────────────┘
               │ (Si la imagen se publica con éxito)
               ▼
 ┌────────────────────────────────────────┐
 │   STAGE 3: Deploy con Docker Compose   │  --> Descarga la orden en tu Máquina Local
 │   (Despliegue local automatizado)      │  --> El Self-Hosted Runner levanta Docker Desktop
 └────────────────────────────────────────┘
🛠️ 3. Descripción Detallada de las Etapas del Pipeline
🧪 Etapa 1: Análisis de Calidad, Cobertura y Generación de Artefactos (sonar)
Ejecuta en la nube de GitHub (runs-on: ubuntu-latest)

A. ¿Qué hace SonarCloud en este proyecto?
SonarCloud actúa como un inspector de código automatizado (un filtro de calidad estricto antes de compilar). Cada vez que subes código, SonarCloud lo analiza línea por línea para buscar:

Bugs: Errores de lógica en Java que podrían romper la aplicación en producción.

Vulnerabilidades / Security Hotspots: Brechas de seguridad, contraseñas expuestas en texto plano o malas prácticas de inyección de dependencias.

Code Smells (Olores de código): Código duplicado, funciones demasiado largas o variables que no se usan, garantizando que el software sea limpio y fácil de mantener para un desarrollador nuevo.

B. Diccionario de Pruebas Unitarias Automatizadas
El pipeline ejecuta la suite de pruebas unitarias implementada con JUnit y Mockito. Cada test tiene una responsabilidad única:

CreateTicketUseCaseTest: Evalúa la lógica de negocio pura del núcleo (Domain/UseCases) al momento de registrar un ticket. Verifica que las reglas de negocio (campos obligatorios, estados iniciales del ticket) se cumplan de manera estricta sin interactuar con la base de datos real utilizando mocks.

GetTicketUseCaseTest: Valida los casos de uso de consultas de información. Asegura que los filtros, búsquedas y excepciones (como cuando un ticket solicitado no existe) respondan con las reglas del negocio adecuadas.

TicketTest: Es una prueba de caja negra sobre la entidad del dominio Ticket. Comprueba que el constructor, las mutaciones de estado (por ejemplo, pasar de estado 'Abierto' a 'Cerrado') y los objetos de valor internos mantengan la integridad de los datos.

TicketControllerTest: Prueba el adaptador de entrada de la capa de infraestructura (Web/API). Valida los mapeos de las rutas HTTP, las respuestas de los códigos de estado (200 OK, 201 Created, 400 Bad Request) y que los JSON recibidos por los clientes externos se serialicen correctamente hacia la arquitectura interna.

C. Auditoría y Reportes (IE2)
Utilizando la acción oficial actions/upload-artifact@v4, el pipeline captura y exporta de forma permanente:

Reportes de Surefire (reporte-pruebas-unitarias): Se genera de manera obligatoria mediante la propiedad if: always(). Si un test unitario falla, el pipeline se detiene pero expone el reporte para que el equipo pueda auditar el error rápidamente.

Reporte de Cobertura JaCoCo (jacoco-report): Mide exactamente qué porcentaje del código fuente de Java está cubierto por las pruebas unitarias, asegurando que los desarrolladores no suban lógicas sin sus respectivos archivos de validación.

Compilación y Artefacto JAR (IE4): Una vez que todas las pruebas y SonarCloud dan el visto bueno, el pipeline ejecuta ./mvnw clean package -DskipTests para generar el artefacto compilado final de producción (.jar) y lo almacena como un archivo inmutable y rastreable en los servidores de GitHub.

🐳 Etapa 2: Contenerización y Empaquetado Multietapa (build-image)
Ejecuta en la nube de GitHub (runs-on: ubuntu-latest)

A. Dockerfile Optimizado con Enfoque Multi-etapa (IE1, IE5)
Para cumplir con las métricas de eficiencia industrial, el archivo Dockerfile divide el proceso en dos capas totalmente independientes:

Fase de Compilación (Build Stage): Utiliza una imagen pesada y completa de desarrollo (maven:3.9.8-eclipse-temurin-17-alpine) que se encarga de descargar las dependencias del ecosistema Java y preparar el entorno de empaquetado de manera aislada.

Fase de Ejecución Final (Run Stage): Una vez obtenido el binario, el pipeline desecha la imagen de Maven y migra únicamente el archivo .jar resultante a una imagen de ejecución ultra-ligera de Linux Alpine (eclipse-temurin:17-jre-alpine). Esto reduce el tamaño de la imagen final en más de un 70%, disminuyendo drásticamente el espacio en disco y eliminando herramientas innecesarias en entornos de ejecución.

B. Principio de Menor Privilegio y Seguridad de Accesos (IE3)
Para mitigar ataques de escalada de privilegios dentro de los servidores, se rompe por completo el uso del usuario administrador por defecto (root). Dentro del Dockerfile, se configuran comandos explícitos para inyectar un entorno de seguridad restringido:

Se crea un grupo seguro del sistema operativo llamado devopsgroup.

Se añade un usuario del sistema sin permisos de administración llamado devopsuser.

Se asigna la propiedad de la carpeta de ejecución al nuevo usuario y se activa la directiva USER devopsuser. De este modo, si la aplicación sufre un intento de vulneración externa en producción, el atacante quedará atrapado en un entorno de Linux totalmente aislado y sin permisos de escritura ni ejecución en el sistema anfitrión.

C. Gobernanza y Repositorio de Paquetes
Para garantizar que las imágenes sean privadas, seguras y rastreables, los permisos del flujo se elevan dinámicamente mediante permissions: packages: write. Esto permite realizar un inicio de sesión seguro automatizado en el registro oficial de contenedores de GitHub, publicando la imagen final en GitHub Container Registry (ghcr.io) etiquetada de forma única bajo el identificador SHA corto de Git del commit actual.

📦 Etapa 3: Orquestación y Despliegue con Docker Compose (deploy)
Ejecuta en el entorno físico local del desarrollador (runs-on: self-hosted)

A. Orquestación Multi-contenedor en Red Local (IE1)
El despliegue se gestiona de manera declarativa mediante el archivo docker-compose.yml. Al configurarse bajo la directiva runs-on: self-hosted, la orden viaja desde los servidores de GitHub directamente hacia el agente de software local instalado en la computadora de desarrollo.

El archivo orquesta concurrentemente dos servicios interconectados a través de una red aislada virtual:

Servicio de Base de Datos (tickets_db): Un contenedor basado en el motor relacional e industrial postgres:16-alpine, el cual inicializa los esquemas y tablas de la aplicación utilizando la contraseña segura inyectada desde los secretos del pipeline (DB_PASSWORD).

Servicio de la Aplicación (app): El contenedor del microservicio de Spring Boot que descarga de forma automatizada la imagen compilada en la etapa anterior desde GitHub Container Registry. Este contenedor se expone externamente a través del puerto 8081 de la máquina local.

B. Trazabilidad de Versiones (SHA Tagging)
Cumpliendo con las directrices de gobernanza DevOps, se prohíbe el uso de etiquetas estáticas ambiguas. El archivo docker-compose.yml lee dinámicamente la variable de entorno IMAGE_TAG: ${{ github.sha }} inyectada por el pipeline de Git, lo que asegura que el contenedor local se destruya, se actualice y levante exactamente la última versión limpia que se acaba de aprobar en la plataforma web.

⚙️ 4. Guía de Ejecución y Puesta en Marcha (Para Nuevos Integrantes)
Si eres un desarrollador nuevo en el equipo, sigue estos pasos en orden para poner a funcionar toda la infraestructura automatizada en tu computadora local:

🔑 4.1 Carga Inicial de Secretos en GitHub
Antes de realizar cualquier cambio o push, asegúrate de que el repositorio en GitHub tenga configuradas las siguientes variables de entorno seguras ingresando en la web a: Settings > Secrets and variables > Actions:

SONAR_TOKEN: Token generado desde tu cuenta de SonarCloud para autenticar el análisis.

SONAR_PROJECT_KEY: Identificador asignado a tu proyecto en la consola de SonarCloud.

SONAR_ORGANIZATION: Nombre asignado a tu organización de SonarCloud (yaquelin2305).

DB_PASSWORD: Una credencial alfanumérica segura que utilizará PostgreSQL para proteger los datos de tickets.

💻 4.2 Configuración del Puente Local (Self-Hosted Runner en Windows)
Para que GitHub Actions pueda entrar a tu computadora y gestionar Docker Desktop de manera automática, debes dejar un agente de escucha encendido:

Abre una consola de PowerShell como Administrador en tu máquina y ejecuta los comandos para preparar el directorio del agente de automatización:

PowerShell
mkdir \actions-runner; cd \actions-runner
Descarga y descomprime el paquete oficial de ejecución de GitHub Actions:

PowerShell
Invoke-WebRequest -Uri "[https://github.com/actions/runner/releases/download/v2.334.0/actions-runner-win-x64-2.334.0.zip](https://github.com/actions/runner/releases/download/v2.334.0/actions-runner-win-x64-2.334.0.zip)" -OutFile actions-runner-win-x64-2.334.0.zip
Add-Type -AssemblyName System.IO.Compression.FileSystem ; [System.IO.Compression.ZipFile]::ExtractToDirectory("$PWD\actions-runner-win-x64-2.334.0.zip", "$PWD")
Vincula el agente local con tu repositorio en la nube usando el token de registro único proporcionado por la interfaz de GitHub:

PowerShell
./config.cmd --url [https://github.com/yaquelin2305/tickets-app-cicd](https://github.com/yaquelin2305/tickets-app-cicd) --token <TU_TOKEN_DE_GITHUB_ACTIONS>
(Presiona la tecla Enter a todas las preguntas de configuración por defecto que aparezcan en pantalla para finalizar exitosamente).

Enciende el canal de escucha e integración ejecutando:

PowerShell
./run.cmd
(Verás el mensaje de estado continuo: Listening for Jobs. Deja esta consola abierta a un lado de tu monitor; es el motor que procesará los despliegues de Docker automáticamente).

🐳 4.3 Inicializar el Motor de Contenedores
Abre la aplicación de escritorio Docker Desktop en tu computadora de desarrollo.

Espera unos segundos hasta verificar que el indicador visual de estado en la esquina inferior izquierda se encuentre en color Verde con el mensaje de estado "Engine running".

🚀 5. Ciclo de Trabajo Diario y Despliegue Automatizado (Cómo probar los cambios)
Una vez configurado el entorno, el flujo para desarrollar y ver los cambios reflejados de forma continua en tiempo real sigue este estándar:

Crear una rama de desarrollo limpia en tu terminal local para empaquetar tus modificaciones:

PowerShell
git checkout -b feature/mi-actualizacion
Modificar el código de negocio o los controladores en tu IDE (por ejemplo, editar el archivo TicketController.java para personalizar los mensajes del endpoint de pruebas de salud /ping).

Confirmar y enviar tu rama local hacia GitHub:

PowerShell
git add .
git commit -m "feat: personalizar endpoint ping de la aplicacion"
git push origin feature/mi-actualizacion
Generar el Pull Request y aplicar Merge en la Web: Ingresa a GitHub, abre el Pull Request desde tu rama hacia develop y aprueba la fusión de código.

Verificación del Ciclo Automatizado: El pipeline despertará de inmediato. Verás cómo las etapas de compilación y calidad se completan de forma exitosa en la nube, y automáticamente notarás cómo tu consola local de PowerShell (./run.cmd) empieza a escribir líneas de código de forma autónoma, descargando la nueva imagen y refrescando tu Docker Desktop local en tiempo real.

Validación del Software: Abre tu navegador favorito e ingresa a la dirección local de escucha: http://localhost:8081/tickets/ping. ¡Verás tus cambios de código e integrantes reflejados en pantalla de manera instantánea y automática sin haber ejecutado ningún comando manual de despliegue!

---

## 📸 6. Evidencias de Funcionamiento Exitoso

A continuación, se presentan las capturas de pantalla que validan el correcto despliegue local, la ejecución del pipeline y la respuesta del microservicio:

### 🔹 Evidencia 1: Despliegue de la Aplicación y Contenedores Activos
<img width="1123" height="555" alt="Image" src="https://github.com/user-attachments/assets/23ab17d2-4074-4c53-ab99-586f114f03fd" />

### 🔹 Evidencia 2: Monitoreo y Logs en Docker Desktop
<img width="1210" height="430" alt="Image" src="https://github.com/user-attachments/assets/40a23cd1-0cb1-4d43-bf38-29cfe285137b" />

### 🔹 Evidencia 3: Respuesta Exitosa del Endpoint (/tickets/ping) con Integrantes
<img width="708" height="150" alt="Image" src="https://github.com/user-attachments/assets/87c19049-4457-4857-9d40-c357b4c0036f" />



🤖 6. Declaración de Uso de Inteligencia Artificial
En el desarrollo y documentación avanzada de este proyecto se utilizó la asistencia tecnológica de Google Gemini como herramienta de co-pilotaje y apoyo técnico para la optimización de sintaxis en scripts Yaml, depuración de errores de sockets en entornos de red locales de Windows, redacción estandarizada de mensajes semánticos de Git (Commits) y estructuración de la documentación técnica formal presente en este archivo educativo. Las decisiones estructurales de código, lógica y justificaciones del modelo de arquitectura hexagonal fueron completadas en su totalidad por los ingenieros del equipo.

🎓 7. Reflexión Académica
Estudiante: Yaquelin Rugel

Asignatura: Ingeniería DevOps

El desarrollo e implementación de este pipeline completo representó un gran desafío de aprendizaje conceptual y técnico en mi formación. A nivel de infraestructura, comprender la separación estricta de responsabilidades a través de un Dockerfile optimizado en múltiples etapas y restringir por completo los accesos de ejecución del software mediante un usuario no-root me demostró con claridad que la seguridad informática y de operaciones no es un añadido opcional del final de un proyecto, sino un pilar estructural que debe nacer desde la misma concepción de la primera línea de código. Asimismo, la automatización del flujo híbrido combinando GitHub Actions con un Runner en nuestra propia máquina me enseñó el valor real del Despliegue Continuo (CD): lograr que el código se pruebe, audite y despliegue solo mediante el control trazable del SHA de los commits me brindó una perspectiva real de cómo se gobierna, protege y escala el software moderno en entornos industriales reales de producción.

Estudiante: Yeider Catari

Asignatura: Ingeniería DevOps

Al inicio del ciclo académico me parecía un proceso excesivo y complejo configurar una arquitectura tan robusta y detallada para un microservicio pequeño. Sin embargo, en el instante en el que vi el pipeline interactuar por primera vez, ver cómo la nube compila, SonarCloud audita la calidad en segundos y la consola de mi propia computadora descarga y actualiza la infraestructura local de Docker de forma 100% automática y sin intervención manual, logré comprender la importancia real de estas metodologías. En un equipo de desarrollo industrial real, este nivel de automatización ahorra cientos de horas de trabajo repetitivo y erradica por completo la típica excusa de desarrollo de 'en mi máquina local sí funciona'. Comprendí que los errores detectados de forma temprana en las etapas iniciales de la Integración Continua (CI) son infinitamente más económicos y fáciles de solucionar que las fallas que logran llegar a producción, y ese factor por sí solo justifica plenamente el esfuerzo de arquitectura detrás de toda la configuración.
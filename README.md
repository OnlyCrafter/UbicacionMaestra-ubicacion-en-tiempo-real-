# ¡Ubicación Maestra! 📍

**Ubicación Maestra** es una aplicación móvil desarrollada en **Kotlin** con **Android Studio** que permite el monitoreo de ubicaciones en tiempo real y la gestión colaborativa a través de funciones avanzadas como grupos de seguimiento, geovallas, y comunicación a través de un bot de Telegram. Ideal para familias, equipos de trabajo, y situaciones que requieren supervisión geolocalizada.
- **Creditos**: Esta es una aplicación en fase prototipo para obtener el título de Ingeniero en Comunicaciones y Electrónica, diseñado y creado por Diego Mauricio Hernández Quintero y Simon Reyes Cristian Yael
---

## Funcionalidades Principales

### Gestión de Usuarios
- **Inicio de sesión y registro**: Los usuarios pueden registrarse con su correo electrónico y contraseña o iniciar sesión si ya tienen una cuenta.
- **Recuperación de contraseña**: Funcionalidad para restablecer contraseñas olvidadas.
- **Edición de datos personales**: Modifica información básica como nombre, foto de perfil y detalles de contacto.

### Geolocalización y Seguimiento
- **Permisos de geolocalización**: Solicitud y manejo eficiente de permisos para compartir ubicación.
- **Ubicación en tiempo real**: Los usuarios pueden compartir su posición actual con otros miembros o grupos.
- **Historial de ubicaciones**: Consulta de los movimientos realizados en un rango de tiempo.
- **Geovallas**: Configuración de zonas geográficas específicas para recibir alertas al entrar o salir de ellas.
- **Consulta de ubicaciones**:
  - **Individual**: Ver la posición de un miembro específico.
  - **Grupal**: Ver a todos los miembros de un grupo en un mapa interactivo.

### Comunicación y Seguridad
- **Suscripción al Bot de Telegram**: Vincula la cuenta para recibir notificaciones importantes.
- **Comunicación Bot-Usuario**: Alertas y mensajes automatizados enviados directamente al usuario.
- **Botón de pánico**: Función de emergencia para notificar rápidamente a contactos seleccionados.
- **Grupos de seguimiento**:
  - Crear o unirse a grupos para facilitar el monitoreo colaborativo.
  - Gestión de permisos dentro del grupo.

### Datos y Preferencias
- **Consulta de datos del entorno**: Información útil basada en la ubicación (por ejemplo, clima, puntos de interés).
- **Extras**: Configuración de preferencias y acceso a servicios adicionales.

---

## Tecnologías Utilizadas

### Desarrollo
- **Kotlin**: Lenguaje principal para la aplicación.
- **Android Studio**: IDE utilizado para el desarrollo.
- **Google Maps API**: Para integración de mapas interactivos.
- **Firebase**:
  - **Authentication**: Gestión de usuarios (registro, inicio de sesión, recuperación de contraseña).
  - **Realtime Database**: Almacenamiento y sincronización de datos en tiempo real.
  - **Cloud Messaging (FCM)**: Notificaciones push.
  - **Firestore**: Almacenamiento avanzado de datos de usuarios y configuraciones.
  
### Comunicación
- **Telegram API**: Integración con un bot para notificaciones y mensajes automatizados.

---

## Instalación y Configuración

### Requisitos Previos
- **Android Studio** instalado (versión recomendada: Flamingo o superior).
- Cuenta en **Firebase** con servicios configurados.
- **Google Maps API Key** activa.
- **OpenWeather API Key** activa.

### ESTOS VALORES TIENE QUE SER MODIFICADOS PARA SU CORRECTO FUNCIONAMIENTO:
<resources>
    <string name="google_maps_api_key">TU_GOOGLE_MAPS_API_KEY</string>
    <string name="telegram_bot_token">TU_TELEGRAM_BOT_TOKEN</string>
</resources>

### Y DESCARGAR LAS CREDENCIALES:
google-services.json



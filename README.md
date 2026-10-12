# Proyecto1 - SmartBus

Aplicación móvil Android de transporte público con dos roles:

- **Pasajero**: ve rutas y paraderos, buses en tiempo real, hora estimada de llegada (ETA calculada sobre el trazado de la ruta) y gana puntos por abordaje validado.
- **Conductor**: registro con verificación documental (DNI, licencia, SOAT), empresa y ruta con convenio. Al ponerse en servicio comparte su ubicación real en tiempo real para que el pasajero lo vea (estilo inDrive). Incluye seguridad: rechazo de ubicaciones simuladas, filtro de precisión y corte automático de la ubicación si el conductor se desvía de su ruta (350 m por más de 20 min). Los puntos se guardan por cuenta.

## Estructura

```
SmartBus/               Proyecto Gradle (módulo app)
├─ app/src/main/java/com/ejemplo/smartbus/
│  ├─ data/             Repositorios, Room (DB), modelos, simulador de flota
│  ├─ data/driver/      Módulo conductor (registro, verificación, empresa/ruta)
│  ├─ location/         Servicios GPS: pasajero (abordaje) y conductor (tiempo real)
│  ├─ navigation/       Navegación por rol (pasajero/conductor)
│  └─ ui/               Pantallas Compose (mapa, onboarding conductor, etc.)
└─ build.gradle.kts     Configuración del proyecto (la clave de Maps se inyecta por gradle)
```

## Requisitos

- Android Studio (AGP compatible con el `build.gradle.kts`).
- JDK 17+.
- Clave de Google Maps.

## Clave de Google Maps

La clave NO se sube al repositorio (seguridad). Después de clonar, crea o edita el archivo:

```
SmartBus/local.properties
```

con la línea:

```
MAPS_API_KEY=TU_CLAVE_DE_GOOGLE_MAPS
```

El resto se inyecta solo desde `app/build.gradle.kts` hacia el `AndroidManifest.xml`.

## Compilar

```bash
cd SmartBus
./gradlew.bat assembleDebug
```

El APK queda en `SmartBus/app/build/outputs/apk/debug/`.

## Notas

- La base de datos local (Room) ya trae sembradas rutas y paraderos de ejemplo.
- Cuando exista el backend/feed real del operador, se conecta a través de la interfaz `VehiclePositionSource` sin cambiar el resto de la app.
# ShellGuard

Auditoria de privacidad y monitor de red para Android. Herramienta de codigo abierto enfocada en deteccion de rastreadores, atribucion de trafico por aplicacion y bloqueo DNS de dominios de terceros.

## Caracteristicas

### Auditoria de paquetes instalados
- Lista completa de aplicaciones del dispositivo con permisos solicitados
- Clasificacion por aplicaciones de sistema y de usuario
- Busqueda y filtrado en tiempo real

### Deteccion de rastreadores
- Escaneo de archivos DEX dentro de cada APK instalado
- Identificacion de SDKs de analisis, publicidad y geolocalizacion mediante firmas conocidas
- Base de datos local de rastreadores (Google Ads, Firebase Analytics, Facebook Ads, AppsFlyer, Adjust, entre otros)
- Funcionamiento completamente offline

### Monitor de red
- Intercepcion de paquetes IP/TCP/UDP mediante VpnService local
- Atribucion de conexiones a aplicaciones mediante getConnectionOwnerUid (Android 10+)
- Registro en tiempo real de conexiones con IP origen, destino, puertos y protocolo
- Buffer circular de 1000 eventos

### Bloqueo DNS
- Intercepcion de consultas DNS (UDP/53)
- Lista de dominios de rastreadores bloqueados por defecto
- Respuesta NXDOMAIN para dominios bloqueados
- Registro de eventos DNS con indicador de bloqueo

## Requisitos

- Android 8.0 (API 26) o superior
- Para atribucion por aplicacion: Android 10 (API 29) o superior

## Construccion

```bash
./gradlew assembleDebug
```

El APK se genera en `app/build/outputs/apk/debug/app-debug.apk`.

## Pruebas

```bash
./gradlew testDebugUnitTest
```

## Instalacion

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Uso

1. Abrir la aplicacion principal para ver la lista de paquetes instalados
2. Pulsar "Escanear rastreadores" para analizar los APKs en busca de SDKs de terceros
3. Acceder a "Monitor de red" desde el menu principal
4. Pulsar "Iniciar" y aceptar el permiso de VPN del sistema
5. Las conexiones apareceran en la lista con la aplicacion responsable

## Arquitectura

| Componente | Funcion |
|---|---|
| `MainActivity` | Lista de aplicaciones con auditoria de permisos y rastreadores |
| `DexScanner` | Lectura de APKs y busqueda de firmas en archivos DEX |
| `TrackerDb` | Base de datos local de rastreadores conocidos |
| `NetworkMonitorService` | Servicio VpnService, parser de paquetes, bloqueo DNS |
| `NetworkMonitorActivity` | Interfaz de monitoreo en tiempo real |

## Limitaciones conocidas

- Solo IPv4. El trafico IPv6 no es interceptado actualmente
- DNS-over-HTTPS (DoH) no pasa por el filtro local
- La lista de dominios bloqueados no es editable desde la interfaz
- La atribucion por UID puede fallar en conexiones efimeras

## Licencia

Ver archivo LICENSE.

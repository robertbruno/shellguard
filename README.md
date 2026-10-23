# ShellGuard

Auditoria de privacidad y monitor de red para Android. Herramienta de codigo abierto enfocada en deteccion de rastreadores, atribucion de trafico por aplicacion y bloqueo DNS de dominios de terceros.

## Licencia

AGPLv3. Ver archivo LICENSE.

## Caracteristicas

### Auditoria de paquetes instalados
- Lista completa de aplicaciones del dispositivo con permisos solicitados
- Clasificacion por aplicaciones de sistema y de usuario
- Busqueda y filtrado en tiempo real

### Deteccion de rastreadores
- Escaneo de archivos DEX dentro de cada APK instalado
- Identificacion de SDKs de analisis, publicidad y geolocalizacion mediante firmas conocidas
- Base de datos local de 28 rastreadores (Google Ads, Firebase Analytics, Facebook Ads, AppsFlyer, Adjust, entre otros)
- Funcionamiento completamente offline

### Monitor de red
- Intercepcion de paquetes mediante VpnService local
- Soporte IPv4 e IPv6
- Atribucion de conexiones a aplicaciones mediante getConnectionOwnerUid (Android 10+)
- Registro en tiempo real de conexiones con direccion origen, destino, puertos y protocolo
- Buffer circular de 1000 eventos

### Bloqueo DNS
- Intercepcion de consultas DNS sobre UDP/53 (IPv4 e IPv6)
- Lista de dominios de rastreadores bloqueados por defecto
- Respuesta NXDOMAIN para dominios bloqueados
- Interfaz para agregar y quitar dominios con validacion de formato
- Registro de eventos DNS con indicador de bloqueo

### Recuperacion de archivos en papelera
- Listado de archivos multimedia que el sistema mantiene en papelera (Android 11+)
- Restauracion a la ubicacion original mediante confirmacion del sistema
- Indicacion de los dias restantes antes de la eliminacion definitiva
- No aplica recuperacion a bajo nivel: el cifrado por archivo de Android descarta la clave al borrar, por lo que los bloques recuperados no se pueden descifrar

### Deteccion de DNS cifrado
- Identificacion de trafico DNS-over-HTTPS (puerto 443) y DNS-over-TLS (puerto 853)
- Rangos IPv4 e IPv6 de proveedores conocidos: Cloudflare, Google, Quad9, AdGuard, NextDNS
- Coincidencia por CIDR con mascara de bits
- Indicacion del proveedor en el registro de conexiones

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
2. Pulsar "Escanear trackers" para analizar los APKs en busca de SDKs de terceros
3. Pulsar "Monitor de red" para abrir el registro de conexiones
4. Pulsar "Iniciar" y aceptar el permiso de VPN del sistema
5. Las conexiones apareceran con la aplicacion responsable y, si corresponde, el proveedor de DNS cifrado detectado
6. Pulsar "Bloqueos DNS" desde la pantalla principal para administrar la lista de dominios bloqueados

## Arquitectura

| Componente | Funcion |
|---|---|
| `MainActivity` | Lista de aplicaciones con auditoria de permisos y rastreadores |
| `DexScanner` | Lectura de APKs y busqueda de firmas en archivos DEX |
| `TrackerDb` | Base de datos local de rastreadores conocidos |
| `NetworkMonitorService` | Servicio VpnService, parser de paquetes IPv4/IPv6, bloqueo DNS |
| `NetworkMonitorActivity` | Interfaz de monitoreo en tiempo real |
| `BlocklistActivity` | Administracion de dominios bloqueados |
| `DomainValidator` | Validacion de formato de dominios |
| `DohDetector` | Deteccion de proveedores de DNS cifrado por rango de direcciones |
| `BlocklistStore` | Persistencia de la lista de dominios bloqueados |
| `RecoveryStore` | Consulta y restauracion de archivos en papelera del sistema |
| `RecoveryActivity` | Interfaz de recuperacion de archivos |

## Limitaciones conocidas

- La deteccion de DNS cifrado se basa en direcciones de proveedores conocidos. Cualquier trafico HTTPS hacia esas direcciones se marca como posible DNS cifrado, lo que puede producir falsos positivos
- El trafico DNS cifrado se detecta pero no se bloquea
- La atribucion por UID puede fallar en conexiones efimeras, devolviendo un valor no valido
- La recuperacion de archivos se limita a lo que el sistema mantiene en papelera: archivos multimedia borrados en los ultimos 30 dias a traves del indice de medios. No recupera archivos cuyo plazo vencio, ni documentos del almacenamiento privado de otras aplicaciones

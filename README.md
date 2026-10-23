# ShellGuard

Auditoria de privacidad y monitor de red para Android. Herramienta de codigo abierto enfocada en deteccion de rastreadores, atribucion de trafico por aplicacion y bloqueo DNS de dominios de terceros.

Todo el analisis ocurre en el dispositivo. La aplicacion no envia informacion a servidores externos ni requiere cuenta.

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
- Funcionamiento sin conexion

### Monitor de red
- Intercepcion de paquetes mediante VpnService local
- Soporte IPv4 e IPv6
- Atribucion de conexiones a aplicaciones mediante getConnectionOwnerUid (Android 10+)
- Registro en tiempo real con direccion origen, destino, puertos y protocolo
- Buffer circular de 1000 eventos

### Bloqueo DNS
- Intercepcion de consultas DNS sobre UDP/53 (IPv4 e IPv6)
- Lista inicial de 21 dominios de rastreadores, editable desde la interfaz
- Respuesta NXDOMAIN para dominios bloqueados
- El bloqueo cubre el dominio exacto y sus subdominios
- La lista se guarda en el dispositivo y se conserva entre reinicios
- Validacion de formato al agregar dominios

### Deteccion de DNS cifrado
- Identificacion de trafico DNS-over-HTTPS (puerto 443) y DNS-over-TLS (puerto 853)
- Rangos IPv4 e IPv6 de proveedores conocidos: Cloudflare, Google, Quad9, AdGuard, NextDNS
- Coincidencia por CIDR con mascara de bits
- Indicacion del proveedor en el registro de conexiones

### Recuperacion de archivos en papelera
- Listado de archivos multimedia que el sistema mantiene en papelera (Android 11+)
- Restauracion a la ubicacion original mediante confirmacion del sistema
- Indicacion de los dias restantes antes de la eliminacion definitiva
- Alcance limitado a la papelera del sistema; ver Limitaciones conocidas

### Interfaz
- Paleta semantica fija para estados: correcto, advertencia, alto, critico, informativo y neutro
- Modo claro y oscuro segun la configuracion del sistema
- Los estados se indican con texto e icono ademas de color, nunca solo con color
- Tarjetas de ayuda contraibles en cada pantalla que explican el alcance y los limites de la funcion
- Tipografia monoespaciada para identificadores tecnicos: direcciones IP, dominios y nombres de paquete

## Requisitos

| Funcion | Version minima |
|---|---|
| Auditoria de paquetes y rastreadores | Android 8.0 (API 26) |
| Monitor de red y bloqueo DNS | Android 8.0 (API 26) |
| Atribucion de trafico por aplicacion | Android 10 (API 29) |
| Recuperacion de archivos en papelera | Android 11 (API 30) |

## Construccion

```bash
./gradlew assembleDebug
```

El APK se genera en `app/build/outputs/apk/debug/app-debug.apk`.

## Pruebas

```bash
./gradlew testDebugUnitTest
```

20 pruebas unitarias sobre el escaneo de firmas DEX, la validacion de dominios, la deteccion de proveedores de DNS cifrado en IPv4 e IPv6, la persistencia de la lista de bloqueo y el formateo de tamanos y plazos.

## Instalacion

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Uso

### Auditoria de aplicaciones

La pantalla principal lista las aplicaciones instaladas con sus permisos. "Escanear rastreadores" lee los archivos DEX de cada APK y busca firmas conocidas. Las aplicaciones de sistema se omiten del escaneo.

### Monitor de red

1. Pulsar "Monitor de red" y despues "Iniciar"
2. Aceptar la solicitud de VPN del sistema; la conexion es local y el trafico no sale del dispositivo hacia servidores de ShellGuard
3. Cada fila muestra el destino, la aplicacion responsable cuando el sistema puede identificarla, y marcas de bloqueo o de DNS cifrado cuando corresponde

### Bloqueo DNS

"Bloqueos DNS" administra la lista de dominios. Al agregar un dominio se valida el formato y se rechazan duplicados. "Restaurar lista inicial" aparece cuando la lista difiere de la original.

### Recuperacion de archivos

"Recuperar archivos" solicita permiso de lectura de medios y lista los archivos en papelera. Al seleccionar archivos y pulsar "Restaurar", el sistema pide confirmacion porque los archivos pertenecen a las aplicaciones que los crearon.

## Arquitectura

| Componente | Funcion |
|---|---|
| `MainActivity` | Lista de aplicaciones con auditoria de permisos y rastreadores |
| `DexScanner` | Lectura de APKs y busqueda de firmas en archivos DEX |
| `TrackerDb` | Base de datos local de rastreadores conocidos |
| `NetworkMonitorService` | Servicio VpnService, parser de paquetes IPv4/IPv6, bloqueo DNS |
| `NetworkMonitorActivity` | Interfaz de monitoreo en tiempo real |
| `BlocklistActivity` | Administracion de dominios bloqueados |
| `BlocklistStore` | Persistencia de la lista de dominios bloqueados |
| `DomainValidator` | Validacion de formato de dominios |
| `DohDetector` | Deteccion de proveedores de DNS cifrado por rango de direcciones |
| `RecoveryActivity` | Interfaz de recuperacion de archivos |
| `RecoveryStore` | Consulta y restauracion de archivos en papelera del sistema |
| `Formatting` | Formateo de tamanos de archivo y plazos de vencimiento |
| `ui/theme` | Paleta semantica, esquemas claro y oscuro, tipografia y escala de espaciado |
| `ui/components` | Tarjeta de ayuda contraible e indicador de estado |

## Limitaciones conocidas

### Deteccion de DNS cifrado
La identificacion se basa en las direcciones de proveedores conocidos, de modo que cualquier trafico HTTPS hacia esas direcciones aparece marcado como posible DNS cifrado aunque no lo sea. El trafico DNS cifrado se detecta pero no se bloquea: al ir cifrado, el filtro local no puede leer que dominio se consulta.

### Atribucion por aplicacion
Requiere Android 10 o superior y consulta el estado del socket en el momento de procesar el paquete. En conexiones muy breves el socket puede haberse cerrado antes de la consulta, en cuyo caso la aplicacion aparece como no identificada.

### Recuperacion de archivos
El alcance es la papelera del sistema: archivos multimedia borrados en los ultimos 30 dias a traves del indice de medios. No recupera archivos cuyo plazo ya vencio, ni los que se borraron sin pasar por el indice, ni documentos del almacenamiento privado de otras aplicaciones.

La recuperacion a bajo nivel que emplean herramientas como testdisk no es aplicable al almacenamiento interno de Android: el cifrado por archivo descarta la clave correspondiente al borrar, de modo que los bloques recuperados no se pueden descifrar. Ese analisis solo tiene sentido en medios extraibles sin cifrar y requiere acceso de superusuario.

### Verificacion
Las pruebas unitarias cubren la logica de analisis y formateo. Los flujos que dependen del sistema (establecimiento de la VPN, solicitud de permisos de medios y confirmacion de restauracion) no estan cubiertos por pruebas automatizadas.

## Documentacion

| Documento | Contenido |
|---|---|
| [Herramientas Linux en Android](docs/analisis-herramientas-linux-android.md) | Viabilidad de integrar tcpdump, nmap y testdisk: capacidades, requisitos de root, restricciones de Google Play y empaquetado de binarios |
| [Aplicaciones de seguridad de codigo abierto](docs/analisis-apps-seguridad-open-source-android.md) | Panorama de herramientas de privacidad y auditoria para Android |

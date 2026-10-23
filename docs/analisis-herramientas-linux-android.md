# Integración de herramientas Linux en Android: tcpdump, nmap y testdisk

Análisis de viabilidad técnica y de distribución para incorporar utilidades clásicas de Linux en una aplicación Android. Android comparte núcleo con Linux, por lo que las tres herramientas son compilables para ARM64; la restricción real no es técnica sino de políticas de distribución y del modelo de permisos de Android.

## 1. Capacidades por herramienta

### tcpdump — auditoría de tráfico de red

| Aspecto | Detalle |
|---|---|
| Función | Captura y analiza el tráfico que entra y sale de las interfaces de red |
| Uso en la aplicación | Registro de paquetes IP en tiempo real, identificación de servidores y dominios contactados por aplicaciones instaladas, generación de archivos `.pcap` |
| Requisito técnico | Privilegios de superusuario para acceder a interfaces de bajo nivel, o redirección del tráfico mediante `VpnService` |
| Viabilidad | Alta |

ShellGuard ya implementa la alternativa sin root: `VpnService` con parser propio de paquetes IPv4/IPv6. `tcpdump` aportaría captura en formato `.pcap` estándar y filtros BPF, útiles para análisis externo en Wireshark.

### nmap — escaneo de puertos y descubrimiento de red

| Aspecto | Detalle |
|---|---|
| Función | Escanea redes locales, descubre hosts, identifica puertos abiertos y versiones de servicios |
| Uso en la aplicación | Auditoría de la red Wi-Fi conectada, detección de dispositivos no reconocidos, verificación de puertos expuestos del propio dispositivo |
| Requisito técnico | Binario compilado para `arm64-v8a` vía NDK. Los escaneos TCP connect no requieren root; los escaneos SYN y la detección de sistema operativo sí |
| Viabilidad | Alta |

### testdisk — recuperación de datos y particiones

| Aspecto | Detalle |
|---|---|
| Función | Recupera particiones perdidas y archivos borrados de medios de almacenamiento |
| Uso en la aplicación | Análisis de tarjetas microSD y particiones secundarias |
| Requisito técnico | Lectura directa de bloques en `/dev/block/...`, lo que exige privilegios de superusuario |
| Viabilidad | Media-baja |

La utilidad en móvil es sensiblemente menor que en equipos de escritorio. El cifrado basado en archivos (FBE), obligatorio desde Android 10, cifra cada archivo con claves derivadas de credenciales del usuario. Un archivo borrado en almacenamiento interno deja bloques cifrados cuyas claves el sistema ya descartó: la recuperación es inviable aunque se recuperen los bloques.

Escenarios donde sí es viable:

- Tarjetas microSD formateadas como almacenamiento portátil, sin cifrar
- Dispositivos de almacenamiento USB conectados por OTG
- Particiones secundarias sin cifrado en dispositivos con root

No es viable en almacenamiento interno cifrado, que es el caso de la mayoría de dispositivos actuales. Cualquier función de recuperación debe declarar esta limitación antes de que el usuario inicie un análisis, para no generar expectativas incorrectas.

## 2. Distribución en Google Play

### Prohibición de código ejecutable descargado

La política de Google Play prohíbe que una aplicación descargue e interprete código ejecutable desde fuentes externas tras la instalación. Los binarios deben venir empaquetados en el APK al momento de la publicación, dentro de `jniLibs/` o como recursos nativos. No pueden descargarse en tiempo de ejecución.

### Privilegios de superusuario

Las aplicaciones que solicitan acceso root reciben revisión adicional. Para pasar revisión:

- La aplicación no debe exigir root para instalarse ni bloquear su funcionamiento básico si no lo tiene
- Debe ofrecer un modo estándar basado en APIs públicas de Android
- Las funciones que requieren root deben solicitarse bajo acción explícita del usuario, con advertencia previa

### Clasificación de la aplicación

Herramientas de análisis de red pueden clasificarse erróneamente como software de intrusión. La ficha de la tienda debe centrarse en auditoría de privacidad, diagnóstico de red y análisis defensivo, y evitar terminología asociada a intrusión o extracción de credenciales.

### Resumen de viabilidad

| Herramienta | Viabilidad técnica | Admisible en Google Play | Condición |
|---|---|---|---|
| `tcpdump` | Alta (root o `VpnService`) | Sí | Binario empaquetado; enfoque declarado en privacidad y diagnóstico |
| `nmap` | Alta (binario ARM64) | Sí | Declarar que el escaneo es de redes propias |
| `testdisk` | Media-baja (cifrado FBE) | Sí | Limitado a medios accesibles; sin promesas sobre almacenamiento interno cifrado |

F-Droid impone menos restricciones sobre esta categoría de software y es un canal de distribución natural para herramientas de auditoría. Publicar en ambos canales con la misma base de código es viable si los binarios se empaquetan de forma estática.

## 3. Consideraciones de implementación

### Empaquetado de binarios

Los ejecutables se ubican en `app/src/main/jniLibs/arm64-v8a/`. Android extrae a `nativeLibraryDir` únicamente archivos que siguen el patrón `lib*.so`, de modo que un binario debe nombrarse `libtcpdump.so` para quedar en un directorio con permiso de ejecución. Desde Android 10, los archivos copiados a almacenamiento privado de la aplicación no son ejecutables (`W^X`), por lo que extraer binarios desde `assets/` en tiempo de ejecución no funciona.

### Tamaño del paquete

Cada binario estático añade peso por arquitectura. Una estimación conservadora para `arm64-v8a`:

| Componente | Tamaño aproximado |
|---|---|
| `tcpdump` + `libpcap` | 1.2 MB |
| `nmap` (sin scripts NSE) | 3 MB |
| `testdisk` + `ncurses` | 2 MB |

Publicar un App Bundle permite que cada dispositivo descargue solo su arquitectura.

### Ejecución y captura de salida

`ProcessBuilder` con `redirectErrorStream(true)` permite leer salida combinada línea por línea. La lectura debe ocurrir fuera del hilo principal y el proceso debe terminarse de forma explícita cuando el usuario abandona la pantalla, ya que un proceso huérfano sobrevive a la actividad que lo lanzó.

## 4. Estado en ShellGuard

| Capacidad | Estado |
|---|---|
| Captura de tráfico | Implementada mediante `VpnService` con parser propio, sin binarios externos |
| Exportación `.pcap` | No implementada |
| Escaneo de red local | No implementado |
| Recuperación de archivos | No implementada |

La captura de tráfico ya cubre el caso de uso principal de `tcpdump` sin requerir root ni binarios externos. La exportación a `.pcap` es la adición de mayor valor y no exige incorporar el binario: puede generarse desde los paquetes que el servicio ya intercepta, escribiendo la cabecera de archivo y las cabeceras por paquete del formato libpcap.

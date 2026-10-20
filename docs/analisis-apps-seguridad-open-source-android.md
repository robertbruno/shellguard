# Análisis: Aplicaciones Android Open Source de Seguridad y Privacidad

## 1. Contexto

El código fuente de aplicaciones Android de código abierto (open source) para seguridad y privacidad se puede encontrar directamente en repositorios públicos como GitHub, GitLab o a través de catálogos especializados como F-Droid.

**Fuentes principales:**
- [F-Droid](https://f-droid.org/es/)
- [It's FOSS – Open Source Android Apps](https://itsfoss.com/open-source-android-apps/)

---

## 2. Principales Aplicaciones de Seguridad Android Open Source

### 2.1 Bitwarden (Gestor de Contraseñas)

| Aspecto | Detalle |
|---|---|
| **Función** | Almacena y cifra contraseñas, claves de acceso (passkeys) y datos sensibles |
| **Código fuente** | [Repositorio oficial de Bitwarden en GitHub](https://github.com/bitwarden) |
| **Licencia** | GPLv3 |

**Análisis:**
- Cifrado de extremo a extremo (AES-256-CBC + PBKDF2/Argon2id para derivación de clave).
- El vault nunca sale del dispositivo sin cifrar; el servidor solo almacena blobs cifrados.
- Arquitectura: cliente Android (Kotlin) + API autoalojable (Rust/Node.js).
- Auditable de punta a punta: tanto el cliente como el servidor son open source.

### 2.2 2FAS Auth (Autenticación de Dos Factores)

| Aspecto | Detalle |
|---|---|
| **Función** | Genera códigos temporales de un solo uso (TOTP/HOTP) para verificar cuentas |
| **Código fuente** | [Repositorio de 2FAS en GitHub](https://github.com/twofas) |
| **Licencia** | GPLv3 |

**Análisis:**
- Compatible con la especificación RFC 6238 (TOTP) y RFC 4226 (HOTP).
- Los secretos se almacenan localmente y se puede cifrar el backup.
- No requiere cuenta ni backend propio; funciona 100% offline.
- Categorizado como de los top autenticadores open source.

**Referencias:**
- [Top 5 Open Source Authenticator Apps](https://dev.to/ambujb/top-5-open-source-authenticator-apps-4ake)
- [Prey Project – Apps de Seguridad](https://preyproject.com/es/blog/app-de-seguridad-de-datos-y-dispositivos)

### 2.3 AOSP / GrapheneOS (Sistema y Seguridad a Nivel de Plataforma)

| Aspecto | Detalle |
|---|---|
| **Función** | Sistema operativo base y endurecimiento extremo de la privacidad |
| **Código fuente** | [AOSP](https://source.android.com/docs/setup/download) / [GrapheneOS](https://grapheneos.org/) |
| **Licencia** | Apache 2.0 (AOSP) / MIT (GrapheneOS) |

**Análisis:**
- **AOSP**: base de Android. Auditable completamente; permite compilar el SO desde cero y verificar la cadena de suministro (supply chain).
- **GrapheneOS**: hardening sobre AOSP. Mitigaciones de memoria (hardened_malloc), kernel reforzado, sandboxing extendido, permisos de red/sensores más estrictos.
- Elimina la dependencia de Google Play Services en lo posible; usa sandboxed Google Play como opción.

**Referencias:**
- [Android Security – Google Developers](https://developers.google.com/android/security)

---

## 3. Dónde Buscar Más Código y Auditar Aplicaciones

### 3.1 F-Droid

Catálogo y repositorio enfocado exclusivamente en aplicaciones libres y de código abierto para Android. Cada app incluye el enlace directo a su repositorio de código fuente, lo que facilita la auditoría.

- Sitio: [https://f-droid.org/es/](https://f-droid.org/es/)

### 3.2 OWASP MobSF

Marco de trabajo (framework) para análisis de seguridad y pruebas de penetración en aplicaciones móviles. Permite análisis estático (SAST) y dinámico (DAST) de APKs.

- Proyecto: [OWASP Mobile Security](https://owasp.org/www-project-mobile-security-testing-guide/)

**Herramientas complementarias de auditoría:**

| Herramienta | Tipo | Uso |
|---|---|---|
| MobSF | SAST/DAST | Análisis automático de APKs |
| JADX | Decompilador | Ver código Java/Kotlin desde APK |
| apktool | Reversing | Decompilar/recursos/smali |
| Frida | Instrumentación | Hook en runtime |
| Burp Suite | Proxy | Análisis de tráfico HTTPS |

---

## 5. VPNs Open Source para Android

| VPN | Código fuente | Licencia | Destaca por |
|---|---|---|---|
| **WireGuard** | [wireguard-android](https://git.zx2c4.com/wireguard-android/) | GPLv2 | Protocolo moderno, ~4000 líneas de código auditable, integrado al kernel Linux |
| **OpenVPN for Android** (Arne Schwabe) | [schwabe/ics-openvpn](https://github.com/schwabe/ics-openvpn) | GPLv2 | Cliente OpenVPN nativo, altamente configurable |
| **Mullvad VPN** | [mullvad/mullvadvpn-app](https://github.com/mullvad/mullvadvpn-app) | GPLv3 | Cliente completo sin cuentas (solo número), WireGuard/OpenVPN, auditorías públicas |
| **IVPN** | [ivpn/android-app](https://github.com/ivpn/android-app) | GPLv3 | Sin registros, multi-hop, auditable |
| **Proton VPN** | [ProtonVPN/android-app](https://github.com/ProtonVPN/android-app) | GPLv3 | Integración con Proton ecosystem, Secure Core |

**Análisis:**
- **WireGuard** es el protocolo de referencia actual: criptografía moderna (Curve25519, ChaCha20, Poly1305), código mínimo → superficie de ataque pequeña y auditable.
- Mullvad e IVPN son los clientes más transparentes: sin email obligatorio, publican auditorías independientes, permiten pagar de forma anónima.
- Clientes WireGuard de terceros (como los anteriores) usan el módulo kernel/servicio `wireguard-go`, lo que simplifica la integración.

---

## 6. Licencia Recomendada para ShellGuard

### Restricción importante de las licencias open source

Ninguna licencia aprobada por OSI puede prohibir que *otros* comercialicen tu código: eso contradice la definición de open source. Para lograr "autoría protegida + solo nosotros comercializamos" existen dos caminos:

### Opción A — Copyleft fuerte + CLA (recomendada si se quiere seguir siendo open source)

- **Licencia: AGPLv3** (GNU Affero GPL v3).
  - Protege la autoría: cualquier obra derivada debe distribuirse bajo la misma licencia, con atribución obligatoria.
  - Cubre el caso SaaS: si alguien ofrece ShellGuard como servicio, también debe liberar su código modificado (a diferencia de GPLv3, que solo obliga al distribuir).
  - **Caveat:** permite que terceros comercialicen servicios derivados, siempre liberando sus modificaciones.
- **CLA (Contributor License Agreement):** cada contribuidor externo cede derechos a la empresa. Eso permite ofrecer una **licencia dual**: versión AGPLv3 gratuita + licencia comercial propietaria a clientes que no quieran copyleft. Es el modelo de MongoDB (antes de cambiar), MySQL, Qt.

### Opción B — Business Source License 1.1 (si la exclusividad comercial es no negociable)

- No es open source (OSI no la aprueba; es "source-available").
- Permite ver y modificar el código, pero prohíbe uso comercial hasta una fecha de conversión, en la que pasa a GPLv2.
- Modelo usado por Sentry, CockroachDB (antes), MariaDB (MaxScale), Ghostfolio.

### Recomendación

| Escenario | Licencia |
|---|---|
| Mantener comunidad open source + ingresos por licencia dual | **AGPLv3 + CLA** |
| Exclusividad comercial total | **BUSL-1.1** (source-available, no OSI) |

> **Nota legal:** consultar con asesor legal antes de publicar; la elegibilidad del CLA y los términos exactos de la licencia dual deben documentarse en `CONTRIBUTING.md` y `LICENSE`.

---

## 7. Conclusiones

| Hallazgo | Implicación |
|---|---|
| El ecosistema open source Android tiene alternativas maduras | Bitwarden, 2FAS y GrapheneOS son referentes auditables |
| La transparencia del código permite verificar claims de privacidad | Cualquiera puede auditar qué datos se envían y cómo se cifran |
| F-Droid actúa como curador confiable | Reduce el riesgo de apps con trackers o código malicioso |
| Herramientas como MobSF estandarizan la auditoría | Permiten análisis sistemático de cualquier APK |

> **Nota:** Este documento es un análisis de referencia. Para profundizar en alguna categoría específica (VPNs, cifrado de mensajes, antivirus, bloqueo de rastreadores) o compilar alguna de estas apps, ver la sección correspondiente del repositorio.

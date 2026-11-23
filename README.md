# CarView — reproductor Android inspirado en CarStream

**CarView es un proyecto independiente, no una copia binaria de CarStream 2.0.5 ni una versión oficial.** Se ha creado como punto de partida funcional para Android. Su interfaz oscura y sus funciones están inspiradas en los reproductores de vídeo tipo CarStream.

## Qué incluye

- Navegador integrado que abre la versión web de YouTube (`https://m.youtube.com/`).
- Búsqueda de vídeos escribiendo texto y búsqueda por voz (si el sistema dispone de reconocimiento de voz).
- Atrás, adelante, inicio, recargar y cuadro de direcciones.
- Reproducción de vídeo web a pantalla completa cuando el sitio lo permite.
- Modo escritorio/móvil configurable.
- Favoritos con opción de guardar, abrir y eliminar.
- Historial local de las últimas 35 direcciones.
- Apertura de vídeos del móvil con el selector de documentos Android (`video/*`). El códec de MP4/MKV depende del dispositivo.
- Interfaz horizontal y vertical y tratamiento de barras del sistema en Android recientes.
- Uso exclusivo de permiso de Internet; sin ROOT, sin permisos para leer todo el almacenamiento, sin rastreadores incluidos.

## Compatibilidad y límites esenciales

**Android Auto (el sistema que proyecta el móvil en el coche):** no se puede garantizar que esta app se muestre o reproduzca vídeo allí. Google indica en su documentación actual que las aplicaciones de vídeo aún no son una categoría general admitida en Android Auto; las apps aparcadas compatibles dependen de la plataforma, del vehículo y de su versión. Este proyecto **no** contiene código de proyección no oficial, modificación del sistema ni trucos para eludir restricciones de movimiento. No basta con instalar el APK para que aparezca como una app Android Auto.

**Android Automotive OS:** se ha identificado la aplicación como categoría `video` y se han hecho opcionales ciertas características de hardware de coche. Eso no garantiza que supere las verificaciones de publicación, compatibilidad o calidad para coches, ni sustituye las pruebas en un emulador o vehículo compatibles.

**YouTube en WebView:** es una página web ajena. Google puede bloquear el inicio de sesión, limitar la reproducción, cambiar el diseño o impedir determinadas funciones. No se incluye API privada de YouTube, descarga de contenido ni eliminación de anuncios. Los enlaces "Abrir página en navegador" permiten utilizar el navegador del dispositivo.

**Seguridad:** la app pausa el reproductor local y el WebView cuando Android envía el evento de pausa; no lleva detector independiente de movimiento. **Úsala únicamente con el vehículo estacionado.**

## Requisitos

- Android Studio con JDK 17 o superior.
- Android SDK Platform 35.
- Gradle 8.9 y Android Gradle Plugin 8.7.3.
- Teléfono o emulador Android 8.0 (API 26) o posterior.
- Acceso a Internet **desde tu equipo** para que Android Studio descargue el plugin Gradle.

## Compilación automática con GitHub Actions

El repositorio incluye [`.github/workflows/build-apk.yml`](.github/workflows/build-apk.yml): prepara Java 17, Android SDK 35 y Gradle 8.9, ejecuta `:app:assembleDebug` y publica el APK como artefacto descargable. La compilación puede iniciarse desde **Actions → Compilar APK Android → Run workflow** o al subir cambios a `main`.

Lee **[GITHUB_ACTIONS.md](GITHUB_ACTIONS.md)** para crear tu repositorio, subir el código y descargar `app-debug.apk`. El archivo generado es una compilación **debug**; no es un APK de publicación ni confirma compatibilidad con Android Auto.

## Cómo abrir y generar un APK

1. Descomprime el ZIP de este proyecto.
2. En **Android Studio**, selecciona **Open** y elige la carpeta `CarView-Android`.
3. Acepta el sincronizado de Gradle. En caso de que Android Studio solicite una distribución, selecciona **Gradle 8.9**, según `gradle/wrapper/gradle-wrapper.properties`.
4. Instala Android SDK 35 desde el SDK Manager si te lo pide.
5. Selecciona `app` y luego **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
6. El archivo generado aparecerá normalmente en `app/build/outputs/apk/debug/app-debug.apk`.
7. Instálalo en tu móvil Android para comprobar YouTube y la reproducción de archivos locales.

**Nota:** este ZIP no contiene `gradle-wrapper.jar`, scripts `gradlew` ni archivos generados por Android Studio. Si necesitas compilar mediante terminal, ejecuta `gradle wrapper --gradle-version 8.9` desde la raíz usando una instalación local de Gradle, o deja que Android Studio configure el entorno. El ZIP contiene todo el código fuente, manifiesto, recursos y configuración del proyecto. **No contiene un APK ya compilado**, pues el entorno de creación de este archivo no tiene Android SDK/Gradle instalado.

## Estructura

- `app/src/main/java/es/carview/app/MainActivity.java`: navegación, búsqueda, modo escritorio, favoritos, historial y reproducción local.
- `app/src/main/res/layout/activity_main.xml`: interfaz oscura y botones.
- `app/src/main/res/drawable/`: iconos originales y fondos.
- `app/src/main/AndroidManifest.xml`: permisos y configuración.
- `app/build.gradle`: compilación Android.

## Qué falta para que sea "exactamente CarStream 2.0.5"

No hemos obtenido ni incluido el código específico del fork "2.0.5 by Paesani2006". La integración no oficial con la proyección de Android Auto, el comportamiento exacto de su menú, posibles opciones ROOT y diferencias particulares del fork **no están implementadas**. Para una adaptación fiel haría falta evaluar ese código concreto y contrastarlo con un dispositivo y coche reales, sin eludir las medidas de seguridad del sistema.

### Fuentes de referencia

- Proyecto histórico de CarStream: https://github.com/thekirankumar/carstream-android-auto (licencia Apache 2.0; este proyecto no reutiliza sus archivos de código).
- Google sobre apps de vídeo aparcadas: https://developer.android.com/agents/skills/cars/android-for-cars-parked-apps/skill
- Google sobre compatibilidad en Android Auto: https://developer.android.com/training/cars/parked/auto
- Google sobre reproducción de vídeo en Android Automotive OS: https://developer.android.com/training/cars/parked/video

## Privacidad

Favoritos, modo escritorio e historial se guardan **en tu dispositivo** mediante `SharedPreferences`. Las cookies del sitio web las gestiona el `WebView` de Android. No existe un backend propio para recoger datos ni se requieren cuentas para la app. Puedes eliminar los datos en Ajustes de Android → Aplicaciones → CarView → Almacenamiento → Borrar datos.

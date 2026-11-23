# Publicar en GitHub y compilar el APK

## 1. Crear y subir un repositorio a GitHub

Elige **una** de las opciones. Puedes hacerlo sin instalar Android Studio ni Android SDK en tu ordenador.

**Opción A — GitHub CLI (si tienes `gh` instalado):**

Esta opción crea el repositorio privado directamente; **no lo crees previamente** en la web. Abre una terminal dentro de la carpeta del proyecto y ejecuta:

```bash
git init -b main
git add .
git commit -m "CarView Android y compilacion automatica del APK"
gh repo create carview-android --private --source=. --remote=origin --push
```

**Opción B — Git, creando el repositorio en la web:**

Abre https://github.com/new, pon el nombre **carview-android** y elige **Private** (o Public, si quieres compartir el código). No marques "Add a README", ".gitignore" ni "Choose a license": el proyecto ya los incluye. Después, en una terminal abierta dentro de la carpeta del proyecto:

```bash
git init -b main
git add .
git commit -m "CarView Android y compilacion automatica del APK"
git remote add origin https://github.com/TU_USUARIO/carview-android.git
git push -u origin main
```

Reemplaza `TU_USUARIO` por tu usuario de GitHub. Si la terminal pide iniciar sesión, autentícate mediante Git Credential Manager o GitHub CLI, según tu entorno. **No uses tu contraseña de GitHub como contraseña de Git**.

> Si usas el archivo `.bundle` adjunto, también puedes hacer `git clone CarView-GitHub-Actions.bundle carview-android`, entrar en esa carpeta, crear el repositorio vacío en GitHub, añadir `origin` y ejecutar `git push -u origin main`.

## 3. Ejecutar GitHub Actions

1. Entra en el repositorio que acabas de crear.
2. Abre la pestaña **Actions**.
3. Selecciona **Compilar APK Android**.
4. Haz clic en **Run workflow** y selecciona `main`. También se ejecuta con cada `git push` a `main`.
5. Cuando termine correctamente la ejecución, entra en ella.
6. En **Artifacts**, descarga **CarView-debug-APK** y descomprime el ZIP que ofrece GitHub.
7. El archivo resultante será `app-debug.apk`, instalable para pruebas en teléfonos Android compatibles.

El APK de depuración viene firmado automáticamente con la clave **debug** del entorno de compilación. **No es una versión de producción** ni un APK pensado para distribuir como release estable. Para publicar o actualizar una app instalada con una clave estable se necesita un mecanismo separado de firma de release.

## 4. Si falla la compilación

Abre la ejecución fallida en **Actions**, entra en **Compilar y guardar APK de depuración** y despliega el paso rojo.

- Errores al descargar dependencias: comprueba la disponibilidad de Maven Central, Google Maven y los servicios de Gradle; vuelve a ejecutar el trabajo.
- Mensaje de **SDK**: confirma que la acción de instalar SDK 35 se ejecutó correctamente.
- Error en Java, Gradle o recursos: copia el mensaje completo del paso **Compilar APK debug** para identificar la línea y corregirla.

## Consideraciones importantes

- No necesitas montar un SDK Android en tu ordenador: GitHub Actions lo instala en su entorno de Ubuntu.
- El proyecto utiliza Android Gradle Plugin **8.7.3**, Gradle **8.9**, Java **17** y SDK **35**.
- No se ha ejecutado una compilación real de GitHub Actions al generar este proyecto; el resultado debe confirmarse en el repositorio una vez publicado.
- CarView es un proyecto independiente, no una réplica exacta de CarStream 2.0.5. Que compile **no significa** que se muestre en Android Auto: esa compatibilidad no está implementada.
- Usa el vídeo únicamente con el vehículo estacionado.


## CarView Android Auto (audio)

El APK generado contiene el servicio de biblioteca de audio local para Android Auto. El vídeo no se proyecta en Android Auto ni se desactivan restricciones de movimiento. Lee `ANDROID_AUTO.md`.

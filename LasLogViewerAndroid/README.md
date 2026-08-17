# LAS Log Viewer — Android

A native Android shell around the same offline LAS log viewer / formation
evaluation tool (LAS parsing, Vshale, porosity, Archie Sw, net pay), built
with a WebView so the parsing and petrophysics logic — already tested in the
desktop version — runs unchanged. The Kotlin code only adds two things a
plain WebView can't do:

1. Opens Android's system file picker for "Open LAS file" (works with any
   .las file on the device, an SD card, or a cloud-synced folder).
2. Opens Android's "Save As" dialog for "Export processed CSV" (WebView
   doesn't handle browser-style blob downloads).

No permissions are declared in the manifest — both flows go through
Android's Storage Access Framework, so no runtime permission prompts are
needed, and nothing the app does touches the network.

## Build it — without installing Android Studio

Android Studio's own footprint (IDE + SDK + emulator images) commonly runs
8–12 GB, so here's a way to get the same APK with **zero local install**:
a GitHub Actions workflow is already included at
`.github/workflows/build-apk.yml`. It builds the debug APK on GitHub's
own servers.

1. Create a new (public or private) repo on [github.com](https://github.com)
   and push this folder to it:
   ```
   cd LasLogViewerAndroid
   git init
   git add .
   git commit -m "LAS log viewer Android app"
   git branch -M main
   git remote add origin https://github.com/<you>/<repo>.git
   git push -u origin main
   ```
2. On GitHub, open the **Actions** tab of the repo — a "Build debug APK"
   run starts automatically on push (or click **Run workflow** to trigger
   it manually).
3. When it finishes (~2–3 minutes), open the run and download the
   **LAS-Log-Viewer-debug-apk** artifact from the bottom of the page —
   that's your installable `.apk`. Copy it to your phone and install it
   (you'll need to allow "install from this source" once).

This uses GitHub's free Actions minutes and needs nothing installed on
your computer beyond `git`.

## Build it — with Android Studio

If you do have the disk space, or already have Android Studio installed:

1. Open Android Studio → **Open** → select this `LasLogViewerAndroid`
   folder.
2. Let Gradle sync (first sync needs internet once; everything after that
   runs offline).
3. **Run ▶** to try it on a device/emulator, or **Build → Build Bundle(s)
   / APK(s) → Build APK(s)** for an installable file at
   `app/build/outputs/apk/debug/app-debug.apk`.
4. For a release build you intend to keep, use **Build → Generate Signed
   Bundle / APK** and create a signing key when prompted.

## Project layout

```
app/src/main/
  java/com/wireline/laslog/MainActivity.kt   — WebView shell, file picker, CSV export bridge
  assets/las_log_viewer.html                 — the full app: LAS parser + log tracks + formation eval (unchanged from the desktop version, plus the Android CSV export hook)
  res/                                        — app name, theme, adaptive launcher icon
  AndroidManifest.xml
```

## Changing anything about the app itself

Almost everything you'd want to change — curve auto-detection patterns,
the Vshale/porosity/Sw formulas, cutoffs, colors, track layout — lives in
`assets/las_log_viewer.html`, in plain JavaScript/CSS. Edit that file and
rebuild; you don't need to touch the Kotlin unless you're changing how
files are opened or saved.

## Minimum Android version

`minSdk = 26` (Android 8.0, 2017) — covers effectively all active
devices and is required for the adaptive launcher icon used here.

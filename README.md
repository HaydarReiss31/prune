# Prune

An Android debloater built on [Shevery-API](https://github.com/HmnDev-Tech/Shevery-API). It lists the apps on your device, shows community removal advice for each one, and removes the ones you pick for the current user, without root. Removed apps can be brought back at any time.

## Features

- Lists system apps by default, with an option to include user apps
- Removal advice from the [Universal Android Debloater Next Generation](https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation) lists, bundled with the app (Recommended, Advanced, Expert, Unsafe, Unlisted)
- Search, filter by rating, and sort by name, package name or risk
- Tap to select several apps (with a haptic tick and a live counter), or select every Recommended app in one tap
- Risk summary in the confirmation dialog, e.g. "3 Recommended · 1 Expert"
- **Undo** bar after a removal: one tap restores everything you just removed
- Details sheet with the description, source list, and the packages an app depends on or is needed by
- **Removed** tab to restore apps (`pm install-existing`)
- Option to keep app data when removing, so a restore brings your settings back
- Extra confirmation for Expert and Unsafe apps
- Built-in protection: Prune refuses to remove core system packages, itself, the Shevery manager, the default launcher and the enabled keyboards
- Material 3 interface with dynamic colors on Android 12+

Apps are removed with `pm uninstall --user 0` (optionally with `-k`) through `SheveryPrivilege.newProcess(...)`. Commands are run as an argument array, so nothing is interpreted by a shell.

## Requirements

- Android 7.0 (API 24) or newer
- [Shevery](https://github.com/HmnDev-Tech/shevery) installed and running (root, ADB, or wireless debugging)

## Usage

1. Start the Shevery server.
2. Open Prune and tap **Request permission**, then approve the prompt from Shevery.
3. Tap apps to select them and press **Remove**.
4. To undo, open the **Removed** tab, select the apps and press **Restore**.

The ratings are community guidance, not a guarantee. Removing the wrong system app can cause a bootloop that needs a factory reset, so review each entry and back up your data first.

## Building

Prune includes the `aidl`, `shared`, `api` and `provider` modules of Shevery-API as source modules, so the Shevery-API repository must be present in a `Shevery-API/` folder at the project root.

### GitHub Actions

Push the project to a GitHub repository. The **Build APK** workflow checks out Shevery-API itself, builds a debug APK, and uploads it as the `Prune-debug` artifact. For reproducible builds, replace `ref: main` in `.github/workflows/build.yml` with a specific commit SHA.

### Local build

Requires JDK 21 and the Android SDK (platform 36, build-tools 36.0.0).

```bash
git clone https://github.com/HmnDev-Tech/Shevery-API
./gradlew :app:assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/`.

The plugin versions in `settings.gradle` (Android Gradle Plugin 9.4.0-alpha07) and the Gradle wrapper match the ones used by Shevery-API. If Shevery-API updates its toolchain, update them to match.

## Project structure

```
app/src/main/
  assets/uad_lists.json         bundled removal list (compact export of the UAD-ng lists)
  java/com/haydareiss/pkgx/
    MainActivity.java           list, selection, details sheet, remove/restore flow
    AppAdapter.java             list adapter
    AppEntry.java               list item model
    UadRepository.java          loads the bundled removal list
    UadInfo.java                removal advice for one package
    Safety.java                 packages that can never be removed
    PrivilegedShell.java        runs commands through the Shevery server
    IconLoader.java             lazy icon loading
    LevelUi.java                colors and labels for the rating tags
    PkgXApp.java                dynamic colors
Shevery-API/                    cloned separately (not tracked)
```

## License

Prune is licensed under the GNU General Public License v3.0 (see `LICENSE`), the same license as the bundled Universal Android Debloater Next Generation data.

## Credits

- [Shevery](https://github.com/HmnDev-Tech/shevery) and [Shevery-API](https://github.com/HmnDev-Tech/Shevery-API) by HmnDev-Tech
- [Universal Android Debloater Next Generation](https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation) for the removal lists
- Interface inspired by [Canta](https://github.com/samolego/Canta)

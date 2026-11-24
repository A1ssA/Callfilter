# CallGuard — Projet Android Natif (Kotlin + Jetpack Compose)

Application de filtrage d'appels 100 % hors-ligne basée sur `CallScreeningService`, `RoleManager`, Room, Jetpack DataStore et `AlarmManager`.

## Option 1 : Compiler l'APK dans Android Studio (recommandé)
1. Décompressez cette archive sur votre ordinateur.
2. Ouvrez Android Studio (Ladybug ou plus récent) puis cliquez sur **Open** et sélectionnez le dossier `CallGuard-Android`.
3. Attendez la synchronisation Gradle.
4. Cliquez sur **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. Récupérez votre APK dans `app/build/outputs/apk/debug/app-debug.apk` et installez-le sur votre smartphone Android 12+ (API 31+).

## Option 2 : Compiler l'APK sans Android Studio (via GitHub Actions)
1. Créez un dépôt GitHub (public ou privé) et poussez le contenu de `CallGuard-Android`.
2. Ouvrez l'onglet **Actions** sur GitHub : le workflow `Build CallGuard APK` (`.github/workflows/build-apk.yml`) compile automatiquement l'APK.
3. Téléchargez l'artefact `CallGuard-debug-apk` contenant `app-debug.apk`.

## Activation sur votre smartphone
- Ouvrez **CallGuard** et appuyez sur **Activer le filtrage natif** (ou vérifiez *Paramètres > Applications > Applications par défaut > ID de l'appelant et spam*).
- Accordez les autorisations Contacts et Journal d'appels pour activer les exceptions Favoris et Appels répétés.
- Sur Xiaomi/Samsung/Oppo, désactivez les restrictions de batterie pour CallGuard afin de garantir la précision d'`AlarmManager`.

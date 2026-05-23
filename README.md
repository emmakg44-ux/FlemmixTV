# 📺 Flemmix TV — APK Android TV

Application Android TV qui charge **flemmix.win** avec bloqueur de pubs intégré
et navigation optimisée pour télécommande.

---

## ✨ Fonctionnalités

- 🚫 **Bloqueur de pubs** : 60+ domaines publicitaires bloqués + injection JS anti-popups
- 📺 **Optimisé TV** : navigation D-Pad, plein écran immersif
- ▶️ **Vidéo plein écran** : support du mode plein écran natif
- ⚡ **User-Agent TV** : meilleure compatibilité des lecteurs vidéo
- 🎨 **Style Netflix** : barre de progression rouge, fond noir

---

## 🔨 Comment builder l'APK

### Option A — Android Studio (recommandé)

1. **Télécharger Android Studio** : https://developer.android.com/studio
2. Ouvrir le dossier `FlemmixTV` dans Android Studio
3. Attendre la synchronisation Gradle
4. Aller dans **Build > Build Bundle(s) / APK(s) > Build APK(s)**
5. L'APK sera dans `app/build/outputs/apk/debug/app-debug.apk`

### Option B — Build en ligne (gratuit, sans installation)

1. Créer un compte GitHub gratuit : https://github.com
2. Créer un nouveau repository et uploader tout ce dossier
3. Créer ce fichier `.github/workflows/build.yml` :

```yaml
name: Build APK
on: [push]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Build APK
        run: |
          chmod +x gradlew
          ./gradlew assembleDebug
      - uses: actions/upload-artifact@v3
        with:
          name: FlemmixTV-APK
          path: app/build/outputs/apk/debug/app-debug.apk
```

4. Push le code → GitHub Actions va builder automatiquement
5. Télécharger l'APK dans l'onglet **Actions > Artifacts**

---

## 📲 Installation sur Android TV

1. Sur ta TV : **Paramètres > Sécurité > Sources inconnues** → Activer
2. Copier l'APK sur une clé USB ou utiliser une app comme **Send Files to TV**
3. Ouvrir le gestionnaire de fichiers de la TV
4. Naviguer jusqu'à l'APK et l'installer
5. L'app **Flemmix** apparaît dans le launcher TV

---

## 🎮 Contrôles télécommande

| Touche       | Action                  |
|-------------|-------------------------|
| OK / Entrée  | Cliquer / Sélectionner  |
| ← Retour     | Page précédente         |
| ↑↓←→         | Scroll / Navigation     |
| ⏯ Play/Pause | Play/Pause vidéo        |
| Menu         | Afficher aide           |

---

## 🔧 Personnalisation

Pour changer le site chargé, modifier dans `MainActivity.java` :
```java
private static final String TARGET_URL = "https://flemmix.win/";
```

Pour ajouter des domaines à bloquer, ajouter dans `AD_DOMAINS` :
```java
"nouveaudomaine.com",
```

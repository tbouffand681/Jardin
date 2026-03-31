# 🌱 SemisJardin — Application Android

Application Android native (Kotlin, Material 3) pour gérer le calendrier de vos semis au jardin.

---

## ✨ Fonctionnalités

### 📅 Calendrier des semis
- Enregistrement de chaque semis (plante, date, emplacement, quantité)
- **Calcul automatique** de la date de récolte et de libération du sol selon la durée d'occupation définie par plante
- Barre de progression visuelle par semis
- Suivi du statut : Semé → Levée → Croissance → Récolté
- Statistiques : nombre de semis actifs, semis du mois

### 📚 Bibliothèque de plantes
- **15 plantes/légumes pré-chargés** : Tomate, Courgette, Carotte, Laitue, Haricot vert, Basilic, Radis, Poireau, Concombre, Potiron, Épinard, Persil, Ciboulette, Poivron, Betterave
- Fiche détaillée par plante : mois de semis, durée d'occupation, espacement, exposition, besoins en eau, germination, conseils
- Filtrage par catégorie (légume, aromate, etc.)
- Recherche par nom commun ou latin
- Ajout et modification de plantes personnalisées

### 🌦️ Météo & Croissance
- Météo en temps réel via **OpenWeatherMap** (géolocalisation automatique ou saisie de ville)
- **Conseil de semis** adapté à la température actuelle
- Tableau de bord : semis actifs par stade de croissance
- Alertes des récoltes imminentes (30 jours)
- Notifications quotidiennes via WorkManager

---

## 🏗️ Architecture

```
MVVM + Repository Pattern
├── data/
│   ├── model/          — Plant, Sowing, WeatherData (data classes)
│   ├── db/             — Room Database, DAOs, Retrofit Weather API
│   └── repository/     — SemisRepository (source unique de vérité)
├── ui/
│   ├── calendar/       — CalendarFragment, SowingsAdapter, AddSowingBottomSheet
│   ├── library/        — LibraryFragment, PlantAdapter, AddEditPlantDialog
│   └── growth/         — GrowthFragment (météo + suivi)
└── util/               — WorkManager, BootReceiver
```

**Stack technique :**
- Kotlin + Coroutines + Flow
- Room (base de données locale)
- Retrofit + Gson (API météo)
- Navigation Component + BottomNavigation
- Material 3 (Material You)
- WorkManager (notifications)
- ViewBinding

---

## 🚀 Installation

### Prérequis
- Android Studio Hedgehog (2023.1) ou plus récent
- Android SDK 34
- JDK 17

### Étapes

1. **Cloner / ouvrir le projet** dans Android Studio
   ```
   File → Open → sélectionner le dossier SemisJardin
   ```

2. **Obtenir une clé API OpenWeatherMap** (gratuite)
   - Créer un compte sur https://openweathermap.org/api
   - Copier votre clé API

3. **Configurer la clé API** dans `app/build.gradle` :
   ```groovy
   buildConfigField "String", "WEATHER_API_KEY", '"VOTRE_CLE_API_ICI"'
   ```

4. **Synchroniser Gradle** : `File → Sync Project with Gradle Files`

5. **Lancer l'application** sur un émulateur ou appareil physique (API 26+)

---

## 📱 Captures d'écran (wireframes)

```
┌─────────────────────┐   ┌─────────────────────┐   ┌─────────────────────┐
│  🌱 Mes Semis       │   │  📚 Bibliothèque     │   │  🌦️ Météo           │
│  ┌──────┐┌──────┐   │   │  🔍 [Rechercher...] │   │  ┌────────────────┐ │
│  │5semis││3 cours│  │   │  [Tout][Légume][...]│   │  │ 📍 Paris       │ │
│  └──────┘└──────┘   │   │  ┌──────┐ ┌──────┐  │   │  │     18°C       │ │
│  Mars 2024          │   │  │  🍅  │ │  🥕  │  │   │  │ Partiellement  │ │
│  ┌─────────────────┐│   │  │Tomate│ │Carotte│ │   │  │ nuageux        │ │
│  │🍅 Tomate        ││   │  └──────┘ └──────┘  │   │  └────────────────┘ │
│  │📍 Carré A       ││   │  ┌──────┐ ┌──────┐  │   │  ✅ Conditions       │
│  │████████░░ 80%   ││   │  │  🥒  │ │  🥬  │  │   │  idéales pour semer │
│  │Dans 12 jours    ││   │  │Courg.│ │Laitue│  │   │                     │
│  └─────────────────┘│   │  └──────┘ └──────┘  │   │  🔔 Récoltes 30j    │
│          [+ Semis]  │   │           [+ Plante] │   │  🌱 Tomate — 5j     │
└─────────────────────┘   └─────────────────────┘   └─────────────────────┘
     [📅 Calendrier]          [📚 Bibliothèque]           [🌱 Météo]
```

---

## 🔧 Personnalisation

### Ajouter des plantes
Via l'interface : onglet Bibliothèque → bouton **"+ Nouvelle plante"**

Ou directement dans `SemisRepository.kt` → fonction `populateDefaultPlants()`

### Modifier la durée des notifications
Dans `BootReceiver.kt` :
```kotlin
PeriodicWorkRequestBuilder<HarvestReminderWorker>(1, TimeUnit.DAYS)
// Changer 1 par la fréquence souhaitée
```

### Changer le seuil d'alerte récolte
Dans `GrowthFragment.kt` et `HarvestReminderWorker.kt` :
```kotlin
daysLeft in 0..30  // Changer 30 par le nombre de jours souhaité
```

---

## 📋 Permissions requises

| Permission | Usage |
|---|---|
| `INTERNET` | API météo OpenWeatherMap |
| `ACCESS_FINE_LOCATION` | Géolocalisation pour la météo |
| `ACCESS_COARSE_LOCATION` | Géolocalisation approximative |
| `POST_NOTIFICATIONS` | Rappels de récolte (Android 13+) |
| `RECEIVE_BOOT_COMPLETED` | Reprogrammer les notifications après redémarrage |

---

## 🗺️ Roadmap (idées d'évolution)

- [ ] Export/import des données (CSV, sauvegarde cloud)
- [ ] Calendrier visuel mensuel (vue grille)
- [ ] Photos par semis (journal visuel)
- [ ] Rotation des cultures (alerte si même famille deux ans de suite)
- [ ] Prévisions météo 7 jours
- [ ] Widget écran d'accueil
- [ ] Mode hors ligne complet (cache météo)

---

## 📄 Licence

Projet libre — usage personnel et modification librement autorisés.

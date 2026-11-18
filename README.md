# 📍 GeoTasks – Standort-Erinnerungen (Kotlin / Android)

Eine übersichtliche Android-App im Stil von **Google Tasks**, mit der du dir Erinnerungen nach **Standort** setzen kannst. Sobald du in die Nähe des gewählten Ortes kommst, erscheint automatisch eine **Popup-Notification** mit deiner Erinnerung.

## ✨ Features

- **Übersichtliches Google-Tasks-Design (Material 3 & Jetpack Compose)**:
  - Offene Aufgaben oben, erledigte Aufgaben einklappbar darunter (und jederzeit mit einem Tap reaktivierbar).
- **2 Wege zur Standort-Auswahl**:
  1. **Favoriten-Buttons (Schnellwahl)**: Lege eigene Favoriten (z.B. *🏠 Zuhause*, *💼 Arbeit*, *🛒 Supermarkt*, *🏋️ Sport*) mit individuellem Namen, Icon und Standard-Radius fest und wähle sie direkt per Button-Klick aus.
  2. **Freie Kartenauswahl (OpenStreetMap / osmdroid)**: Wähle jeden beliebigen Punkt auf der Karte oder über die integrierte **Adresssuche (Nominatim)** – komplett ohne API-Key.
- **Einstellbarer Radius per Slider**:
  - Stufenlos zwischen **50 m und 1000 m** (Standard: 150 m) einstellbar, inklusive visueller Kreis-Vorschau auf der Karte.
  - Löst zuverlässig beim **Betreten** (`GEOFENCE_TRANSITION_ENTER`) des Bereichs aus.
- **Popup-Notification & Direkt-Erledigen**:
  - High-Priority Heads-Up Notification beim Erreichen des Standorts mit direktem **„Erledigt“**-Button im Banner.
  - Wahlweise **einmalig** oder **wiederkehrend** (jedes Mal bei Ankunft).

## 📲 Download & Installation (APK)

Die fertige APK wird bei jedem Push auf `main` vollautomatisch über **GitHub Actions** gebaut und unter [**Releases**](../../releases) veröffentlicht:

1. Öffne den Bereich [**Releases**](../../releases) hier im Repository.
2. Lade die Datei **`GeoTasks-Reminder.apk`** aus dem neuesten Release herunter und installiere sie auf deinem Android-Gerät.
3. Erlaube beim Start den Standortzugriff (**„Immer zulassen“**) sowie **Benachrichtigungen**.

## 🛠️ Tech-Stack

- **Kotlin** & **Jetpack Compose** (Material Design 3)
- **Jetpack Room** (Lokale SQLite Datenbank für Aufgaben & Favoriten-Buttons)
- **Google Play Services Location (`GeofencingClient`)** (Akkuschonende Hintergrund-Standortüberwachung)
- **OSMDroid (`OpenStreetMap`)** & **Nominatim Geocoding**
- **GitHub Actions CI/CD** (Automatischer APK-Build & Release-Upload)

## ATA v1.3.1

### Samsung Routinen
- **Mehr Aktionen für Routinen:** Routinen bieten nur die App-Shortcuts an, die ATA veröffentlicht. Bisher waren das nur die 4 Aktionen des Icon-Menüs. Neu unter *Einstellungen → Menü & Widgets → Samsung Routinen*: zusätzliche Aktionen auswählen (je nach Gerät bis zu ca. 11 weitere). Das Icon-Menü zeigt weiterhin nur die 4 gewählten Aktionen.
- **Deutlicher Hinweis** in *Menü & Widgets* und *Automation*, welche Aktionen in Routinen verfügbar sind. Erscheinen die zusätzlichen Aktionen auf einem Gerät nicht, gehen in Routinen nur die 4 Icon-Menü-Aktionen.

### Technik
- **targetSdk 37 (Android 17)**, compileSdk 37. Läuft weiterhin ab Android 12 (minSdk unverändert).
- Toolchain aktualisiert: Android Gradle Plugin 9.4.1, Gradle 9.8.2, Kotlin 2.4.21, kotlinx-serialization 1.12.0.
- NFC-Tag- und Shortcut-Bildschirme berücksichtigen Status- und Navigationsleiste (Edge-to-Edge).

### Hinweise
- BLE-Funktionen, NFC-Schlüsselkarte und Uhr-Direktsteuerung sind weiterhin **BETA** und nicht an einem echten Auto verifiziert.

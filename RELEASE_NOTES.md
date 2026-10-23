## ATA v1.9.6 — bereit für Google Play

### Google Play
- ATA kommt als **ATA - Car Automatisation** in den Play Store, für Handy und Wear OS.
- Die Play-Version hat einen eigenen Paketnamen: **`de.juliandigitalproducts.ata`**.
  - Sie ist eine eigene App neben der APK-Version (`org.tfv.ata`) und aktualisiert diese nicht.
  - Handy und Uhr müssen aus derselben Quelle kommen: beide aus Play oder beide als APK.
  - Die APK-Version von GitHub bleibt `org.tfv.ata`, wird wie gewohnt aktualisiert und behält ihre Kopplung mit dem Auto.
- Jedes Release enthält jetzt zusätzlich die signierten **App Bundles** für Play: `ATA-phone-v1.9.6.aab` und `ATA-wear-v1.9.6.aab`.
- Die Uhr-Version hat eine eigene Versionsnummer (Handy + 1.000.000), weil Play für jedes Bundle eine eigene braucht. Für APK-Nutzer ändert sich nichts, Updates installieren wie gewohnt.

### App-Name
- Der App-Name auf dem Startbildschirm heißt jetzt **„ATA - Car Automatisation“**, der Untertitel auf der Startseite „Car Automatisation“.

### ATA unterstützen
- Der Text sagt jetzt klar: Eine Spende ist **rein freiwillig und schaltet nichts frei**. ATA bleibt kostenlos mit allen Funktionen, ohne Werbung und ohne In-App-Käufe.
- In der Play-Version gibt es keinen PayPal-Button. Dort steht stattdessen **„ATA bei Google Play bewerten“**.
- Die APK-Version von GitHub behält den PayPal-Button.

### Uhr einrichten
- Ist ATA auf der Uhr nicht installiert, verweist die Play-Version auf **Google Play** statt auf die Release-Seite.
- Der Hinweis ohne verbundene Uhr nennt beide Wege: Play Store auf der Uhr oder Wear-APK.

### Datenschutz
- Neue **Datenschutzerklärung** (Deutsch und Englisch) unter `docs/privacy-policy.md`: keine Datenerhebung, kein Server, alles bleibt auf dem Gerät. Sie erklärt Bluetooth, Standort, NFC, Benachrichtigungen und die übrigen Berechtigungen.

### Für Entwickler
- CI baut zusätzlich die Play-Bundles und prüft Manifest, Berechtigungen, Signatur und 16-KB-Ausrichtung nativer Bibliotheken.
- Store-Texte (Deutsch und Englisch) und Grafiken liegen in `fastlane/metadata/android/`, die Anleitung für die Play Console in `docs/play/PLAY_CONSOLE.md`.
- `tools/play-graphics.py` baut Icon, Feature-Grafik und die Screenshots für Handy, 7"- und 10"-Tablet und Wear OS aus den CI-Screenshots.
- README: veralteten Hinweis zur Sitzheizung entfernt (die Sitzheizung je Sitz gibt es seit 1.9.2).

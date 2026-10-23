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
- Die Seite **„ATA unterstützen“** mit dem PayPal-Button ist aus der App entfernt, in der APK- und in der Play-Version.
- Spenden sind weiter über die README auf GitHub möglich: rein freiwillig, sie schalten nichts frei. ATA bleibt kostenlos mit allen Funktionen, ohne Werbung und ohne In-App-Käufe.

### Ladekreis
- Die grünen Punkte, die beim Laden durch den Ring wandern, sind entfernt, am Handy und auf der Uhr. Sie liefen nicht sauber.
- Ladelimit-Strich, gestrichelter Bogen bis zum Limit und die pulsierende Spitze bleiben.

### Uhr einrichten
- Ist ATA auf der Uhr nicht installiert, verweist die Play-Version auf **Google Play** statt auf die Release-Seite.
- Der Hinweis ohne verbundene Uhr nennt beide Wege: Play Store auf der Uhr oder Wear-APK.

### Demo-Modus
- Ein Tipp auf der Uhr im Demo-Modus aktualisiert den Demo-Status auf der Uhr jetzt sofort (bisher erst nach dem nächsten Abruf).

### Datenschutz
- Neue **Datenschutzerklärung** (Deutsch und Englisch) unter `docs/privacy-policy.md`: keine Datenerhebung, kein Server, alles bleibt auf dem Gerät. Sie erklärt Bluetooth, Standort, NFC, Benachrichtigungen und die übrigen Berechtigungen.

### Für Entwickler
- CI baut zusätzlich die Play-Bundles und prüft Manifest, Berechtigungen, Signatur und 16-KB-Ausrichtung nativer Bibliotheken.
- Store-Texte (Deutsch und Englisch) und Grafiken liegen in `fastlane/metadata/android/`, die Anleitung für die Play Console in `docs/play/PLAY_CONSOLE.md`, die Versionshinweise im Play-Format (`<de-DE>…</de-DE><en-US>…</en-US>`) in `docs/play/release-notes.txt`.
- `tools/play-graphics.py` baut Icon, Feature-Grafik und die Screenshots für Handy, 7"- und 10"-Tablet und Wear OS aus den CI-Screenshots.
- README: veralteten Hinweis zur Sitzheizung entfernt (die Sitzheizung je Sitz gibt es seit 1.9.2).

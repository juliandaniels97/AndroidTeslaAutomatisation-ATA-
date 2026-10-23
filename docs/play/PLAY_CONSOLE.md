# Google Play: Anleitung für die Play Console

Alles, was du für ATA in der Play Console ausfüllst, Schritt für Schritt. Texte, die Google liest
(Begründungen, Hinweise für die Prüfung), stehen auf Englisch zum Kopieren in Code-Blöcken.

| | |
|---|---|
| App-Name | **ATA - Car Automatisation** |
| Paketname | **`de.juliandigitalproducts.ata`** (Handy und Uhr) |
| Standardsprache | Deutsch (de-DE), dazu Englisch (en-US) |
| Kategorie | App → **Autos & Fahrzeuge** |
| Preis | Kostenlos, alle Länder |
| Kontakt-E-Mail | juliandigitalproducts+kontakt@gmail.com |
| Website | https://github.com/juliandigitalproducts/AndroidTeslaAutomatisation-ATA- |
| Datenschutzerklärung | https://github.com/juliandigitalproducts/AndroidTeslaAutomatisation-ATA-/blob/main/docs/privacy-policy.md |
| Bundles | Am GitHub-Release: `ATA-phone-vX.Y.Z.aab` und `ATA-wear-vX.Y.Z.aab` |

## 1. Was CI für Play baut

- `ATA-phone-vX.Y.Z.aab`: Handy und Tablet, alle ABIs, versionCode = CI-Laufnummer.
- `ATA-wear-vX.Y.Z.aab`: Wear OS, versionCode = CI-Laufnummer **+ 1.000.000**.
  - Play nimmt jeden versionCode nur einmal an, über alle Bundles einer App hinweg.
  - Mit dem Abstand kollidieren Handy und Uhr nie.
- Beide sind mit dem bisherigen Release-Schlüssel signiert. Er ist der **Upload-Schlüssel**.
- Die Play-Builds (`-PataPlay`) unterscheiden sich von den APKs:
  - Paketname `de.juliandigitalproducts.ata`
  - kein PayPal-Button, stattdessen „ATA bei Google Play bewerten“
  - Hinweise zur Uhr verweisen auf Google Play statt auf die Release-Seite
- Den PayPal-Button wieder in die Play-Version holen: `ataPlayDonate=true` in `gradle.properties`.
  Das rate ich ab, siehe Abschnitt 9.

## 2. App anlegen

1. Play Console → **App erstellen**.
2. Felder ausfüllen:
   - App-Name: `ATA - Car Automatisation`
   - Standardsprache: Deutsch – de-DE
   - App oder Spiel: App
   - Kostenlos oder kostenpflichtig: Kostenlos
3. Erklärungen bestätigen (Programmrichtlinien, US-Exportgesetze).

## 3. App-Signatur (Play App Signing)

- Beim ersten Upload fragt Play nach dem App-Signaturschlüssel. Wähle **„Von Google generierten Schlüssel verwenden“** (Standard).
- Das hochgeladene Bundle ist mit dem Release-Schlüssel signiert. Play merkt sich ihn als **Upload-Schlüssel**:
  SHA-256 `fda413d879cfff088095db5b71591896245c4ecc51e16aed108acce1c47a20e6`.
- Play signiert die Installationen auf den Geräten mit Googles eigenem Schlüssel.
  - Darum lassen sich Play- und GitHub-Version nicht gegenseitig aktualisieren.
  - Das gilt ohnehin, denn sie haben verschiedene Paketnamen.
  - Handy und Uhr aus Play haben denselben Schlüssel und finden sich über den Data Layer.
- Geht der Upload-Schlüssel verloren: Play Console → Einrichten → App-Signatur → Upload-Schlüssel zurücksetzen.

## 4. Store-Eintrag (Hauptdatei des Store-Eintrags)

Die Texte liegen in `fastlane/metadata/android/<Sprache>/`:

| Feld | Datei | Grenze |
|---|---|---|
| App-Name | `title.txt` | 30 Zeichen |
| Kurzbeschreibung | `short_description.txt` | 80 Zeichen |
| Vollständige Beschreibung | `full_description.txt` | 4000 Zeichen |
| Versionshinweise | `changelogs/default.txt` | 500 Zeichen |

Grafiken liegen in `fastlane/metadata/android/<Sprache>/images/`:

| Feld | Datei(en) | Format |
|---|---|---|
| App-Symbol | `icon.png` | 512 × 512 PNG |
| Feature-Grafik | `featureGraphic.png` | 1024 × 500, ohne Transparenz |
| Screenshots Smartphone | `phoneScreenshots/1–8.png` | 1080 × 1920 (9:16) |
| Screenshots 7-Zoll-Tablet | `sevenInchScreenshots/*.png` | 1920 × 1080 (16:9) |
| Screenshots 10-Zoll-Tablet | `tenInchScreenshots/*.png` | 2560 × 1440 (16:9) |
| Screenshots Wear OS | `wearScreenshots/*.png` | 454 × 454 (1:1, ohne Rahmen) |

So legst du die Sprachen an:
- Deutsch ist die Standardsprache.
- Englisch: **Übersetzungen verwalten → Eigene Übersetzungen hinzufügen → Englisch (USA) – en-US**.
  Dort die Texte und Bilder aus `en-US` einfügen.
- Die Wear-OS-Screenshots erscheinen erst, wenn Wear OS als Formfaktor hinzugefügt ist (Abschnitt 6).

Zur Marke:
- Der Name enthält kein „Tesla“. Beschreibung und Kurzbeschreibung nennen Tesla nur als „für Tesla-Fahrzeuge“.
- Die Beschreibung enthält den Hinweis, dass ATA nicht mit Tesla verbunden ist.
- Icon und Grafiken zeigen kein Tesla-Logo.
- Wenn Google trotzdem wegen geistigen Eigentums ablehnt, lege mit diesem Text Einspruch ein:

```
ATA is an independent app that controls the user's own vehicle over Bluetooth using the vehicle maker's
publicly documented vehicle-command protocol. The brand name is used only to describe compatibility
("for Tesla vehicles"), the listing states clearly that the app is not affiliated with or endorsed by
Tesla, Inc., and neither the app name, icon nor graphics use Tesla's name or logo.
```

## 5. App-Inhalte (Richtlinien → App-Inhalte)

### Datenschutzerklärung
```
https://github.com/juliandigitalproducts/AndroidTeslaAutomatisation-ATA-/blob/main/docs/privacy-policy.md
```

### Werbung
**Nein**, die App enthält keine Werbung.

### App-Zugriff
Wähle **„Alle oder einige Funktionen sind eingeschränkt“** und lege eine Anleitung an. Benutzername und Passwort leer lassen.

```
No account or login is needed. The real functions need the user's own car over Bluetooth, so for review
please use the built-in demo mode: open ATA → Settings → Demo mode (turn it on). The Vehicle tab then
shows a connected demo car (50 %, charging at 22 kW). Every button works and changes the demo car's
state; nothing is sent over Bluetooth. The Wear OS app shows the demo car's status and runs its actions on it while the
phone app is in demo mode (pull down on the Vehicle tab once to send the demo status to the watch).
```

### Einstufung des Inhalts (IARC-Fragebogen)
- E-Mail: juliandigitalproducts+kontakt@gmail.com
- Kategorie: **„Alle anderen App-Typen“** (Dienstprogramm, Produktivität, Kommunikation oder Sonstiges)
- Alle Fragen mit **Nein** beantworten:
  - keine Gewalt, keine Sexualität, keine Glücksspiele
  - keine nutzergenerierten Inhalte und keine Kommunikation zwischen Nutzern
  - kein Teilen des Standorts mit anderen Nutzern
  - keine digitalen Käufe
- Erwartet: USK 0 / PEGI 3 / „Jedes Alter“.

### Zielgruppe und Inhalte
- Zielaltersgruppe: **18 und älter**. Die App steuert ein Auto und richtet sich an Fahrzeughalter.
- „Könnte die App für Kinder ansprechend sein?“: **Nein**.

### Datensicherheit
- Werden Nutzerdaten erhoben oder weitergegeben? **Nein**.
  Die App hat keine Internetberechtigung. Sie sendet nichts an einen Server und enthält keine Analyse-, Werbe- oder Crash-SDKs.
- Danach fragt Play nichts mehr ab. Die Zusammenfassung zeigt „Keine Daten erhoben“ und „Keine Daten weitergegeben“.
- Warum der Fehlerbericht nicht zählt:
  - Er wird nur auf Tippen angezeigt und in die Zwischenablage kopiert.
  - Auf Wunsch öffnet ATA ein vorausgefülltes GitHub-Issue im Browser, das du selbst abschickst.
  - Das ist vom Nutzer selbst ausgelöstes Teilen, keine Erhebung durch die App.
- Daten, die nur auf dem Gerät verarbeitet werden (Standort für „Auto finden“, Fahrzeugstatus), gelten nicht als erhoben.

### Weitere Erklärungen
| Formular | Antwort |
|---|---|
| Regierungs-App | Nein |
| Finanzfunktionen | Keine |
| Gesundheit | Keine |
| Nachrichten-App | Nein |
| Werbe-ID | **Nein**, die App nutzt keine Werbe-ID |

### Berechtigungen und APIs
Für Vordergrund-Standort, Bluetooth, NFC, Overlay, Boot und Benachrichtigungen gibt es bei Play
**kein eigenes Formular**. Die Begründungen unten brauchst du nur, wenn Google nachfragt
(z. B. bei der Prüfung oder per E-Mail). Pflicht ist nur die Erklärung zum **Vordergrunddienst**.

#### Vordergrunddienst-Berechtigungen (Pflicht)
- Typ: **Verbundenes Gerät** (`FOREGROUND_SERVICE_CONNECTED_DEVICE`).
- Aufgaben, die du auswählst:
  - „Kommunikation mit einem verbundenen Gerät (Bluetooth)“
  - falls angeboten: „Andere“ mit dem Text unten

Beschreibung:
```
ATA controls the user's own car over Bluetooth Low Energy. It uses a foreground service of type
connectedDevice in two cases:
1. Running a single vehicle command (e.g. lock, climate on) that the user started from a widget, an app
   shortcut, a Samsung routine, an NFC tag or the Wear OS watch. The service connects to the car over BLE,
   sends the signed command and stops after a few seconds, showing a notification while it runs.
2. Optional "Unlock when approaching": while the user has turned this on, the service watches the
   Bluetooth signal strength of the user's car and unlocks it once when the user walks up. A persistent
   notification is shown and the user can turn it off at any time in the app.
Both tasks must keep running when the screen is off or the app is in the background, because the user
starts them from outside the app (widget, routine, watch, walking up to the car). Deferring them with
WorkManager would make the car react minutes late, which defeats their purpose.
```
- Auswirkung, wenn verschoben oder unterbrochen:
```
The command would not reach the car (the door stays locked or unlocked, climate doesn't start), or the
car would not unlock when the user arrives.
```
- Video-Link: eine kurze Bildschirmaufnahme (30–60 s), z. B. als nicht gelistetes YouTube-Video oder
  freigegebener Google-Drive-Link. Darin zu sehen:
  1. Einstellungen → Verhalten, Status, Bluetooth & NFC → **Entriegeln beim Herangehen** einschalten.
     Die Benachrichtigung des Dienstes erscheint.
  2. Am Startbildschirm über das ATA-Widget eine Aktion (z. B. „Verriegeln“) ausführen.
     Die kurze Benachrichtigung „läuft“ erscheint, danach das Ergebnis.
  3. Wenn möglich am Auto, sonst im Demo-Modus mit sichtbarer Benachrichtigung.

#### Standort (nur Vordergrund, kein Formular; Text für Rückfragen)
```
ATA requests ACCESS_FINE_LOCATION / ACCESS_COARSE_LOCATION only while in use. Android delivers Bluetooth
LE scan results of the user's car (its BLE advertisement / iBeacon) only to apps with location access,
and ATA needs them to find and connect to the car. The same permission is used on the device only for
"Find my car" (the phone's position when it was last at the car, shown as direction and distance) and
for "Unlock when approaching" (Bluetooth signal strength of the car). ATA does not request background
location, has no internet permission and never sends location anywhere.
```

#### Über anderen Apps einblenden (`SYSTEM_ALERT_WINDOW`, optional)
```
Optional, asked only when the user enables it in Settings → Automation. Android blocks activity starts
from the background; with this permission a tap on the Wear OS watch or an automation (Samsung routine,
Tasker) can open the vehicle maker's app or the navigation app on the phone while ATA is in the
background. ATA never draws overlays over other apps.
```

#### NFC und Kartenemulation (HCE)
```
NFC is used to run actions from NFC tags the user wrote with ATA, and for host card emulation: the phone
(or watch) can act as the user's key card for the car. The HCE service is disabled by default and only
enabled when the user turns on the key card in the settings.
```

#### Beim Start ausführen (`RECEIVE_BOOT_COMPLETED`)
```
Only restarts the optional "Unlock when approaching" service after a reboot or app update, if the user
turned it on. Nothing else runs at boot.
```

#### Benachrichtigungen (`POST_NOTIFICATIONS`)
```
Shows the result of vehicle commands started from widgets, routines or the watch, confirmations for
critical actions (e.g. unlock while the phone is locked), and the required notification of the
foreground services.
```

## 6. Wear OS hinzufügen

1. **Einrichten → Erweiterte Einstellungen → Formfaktoren → Hinzufügen → Wear OS**.
2. Die Wear-OS-Screenshots hochladen (`wearScreenshots/`).
3. Den Wear-OS-Richtlinien zustimmen.
4. Für Wear OS gibt es **eigene Tracks**:
   - In **Testen** und **Produktion** oben rechts den Formfaktor **„Nur Wear OS“** wählen.
   - Dort das Bundle `ATA-wear-vX.Y.Z.aab` hochladen.
   - Das Handy-Bundle kommt in die normalen Tracks („Smartphone, Tablet, …“).
5. Google prüft Wear-Apps zusätzlich auf die Wear-OS-Qualitätsrichtlinien. Die Uhr-App braucht die Handy-App.
   - Das ist im Manifest erklärt (`com.google.android.wearable.standalone=false`).
   - Im Store steht dann „benötigt die Smartphone-App“.

## 7. Hochladen: interner Test → geschlossener Test → Produktion

1. **GitHub-Release öffnen** und `ATA-phone-vX.Y.Z.aab` und `ATA-wear-vX.Y.Z.aab` herunterladen.
2. **Interner Test** (Testen → Interner Test), Formfaktor „Smartphone, Tablet …“:
   1. Neuen Release erstellen und `ATA-phone-vX.Y.Z.aab` hochladen.
   2. Beim ersten Mal den Google-generierten App-Signaturschlüssel bestätigen (Abschnitt 3).
   3. Versionshinweise: den Inhalt von [`docs/play/release-notes.txt`](release-notes.txt) komplett einfügen.
      Er enthält Deutsch und Englisch in den Tags `<de-DE>…</de-DE>` und `<en-US>…</en-US>`; Play ordnet sie selbst zu.
   4. Tester: eine E-Mail-Liste anlegen, mindestens deine eigene Adresse.
   5. Release prüfen und für den internen Test einführen.
3. **Interner Test, Formfaktor „Nur Wear OS“**:
   1. `ATA-wear-vX.Y.Z.aab` hochladen.
   2. Dieselbe Testerliste verwenden und einführen.
4. Über den Teilnahme-Link installieren:
   - Am Handy ATA installieren.
   - Auf der Uhr den Play Store öffnen, ATA erscheint dort.
   - Demo-Modus, Kopplung und die Uhr testen.
5. **Geschlossener Test** (nötig für neue private Entwicklerkonten):
   - Mindestens **12 Tester** müssen **14 Tage am Stück** angemeldet sein, bevor du die Produktion beantragen kannst.
   - Lege einen geschlossenen Track an (z. B. „Alpha“) mit beiden Bundles (Handy-Track und Wear-OS-Track).
   - Tester per E-Mail-Liste oder Google Group eintragen und den Link teilen.
   - Bei einem Organisationskonto entfällt dieser Schritt.
6. **Produktion beantragen**:
   - Dashboard → „Zugriff auf Produktion beantragen“.
   - Danach Produktion → neuer Release für beide Formfaktoren: Handy-Bundle und Wear-Bundle.
   - Die Bundles aus dem Test kannst du übernehmen („Aus Mediathek hinzufügen“).
   - Länder: **alle**.
   - Die erste Prüfung dauert oft einige Tage.
7. **Spätere Updates**: Jedes GitHub-Release enthält neue Bundles mit höherem versionCode. Beide in den
   gewünschten Track hochladen, das Handy-Bundle im Handy-Track, das Wear-Bundle im Wear-OS-Track.
   Die Versionshinweise stehen jeweils aktuell in `docs/play/release-notes.txt` (max. 500 Zeichen pro Sprache).

## 8. Vor dem Einreichen prüfen

- [ ] Datenschutzerklärung erreichbar (Link oben im Browser öffnen).
- [ ] App-Zugriff mit der Demo-Modus-Anleitung ausgefüllt.
- [ ] Datensicherheit: „Keine Daten erhoben“.
- [ ] Vordergrunddienst-Erklärung mit Video-Link.
- [ ] Einstufung, Zielgruppe, Werbung, Werbe-ID ausgefüllt.
- [ ] Store-Eintrag Deutsch und Englisch mit Icon, Feature-Grafik und Screenshots.
- [ ] Wear OS als Formfaktor hinzugefügt, Wear-Screenshots da, Wear-Bundle im Wear-OS-Track.

## 9. Spenden-Button und Play-Richtlinie

- Play verlangt für Zahlungen in Apps grundsätzlich Google Play Billing.
- Ausgenommen sind nur **steuerbefreite Spenden** (für anerkannte gemeinnützige Organisationen).
  Diese dürfen dann gerade nicht über Play Billing laufen.
- Eine PayPal-Spende an einen privaten Entwickler fällt nicht unter diese Ausnahme.
- Google hat Apps mit solchen Spendenlinks schon gesperrt oder abgemahnt, zuletzt AnkiDroid im Jahr 2026.
  Der Hinweis „freiwillig, schaltet nichts frei“ schützt davor nicht.
- Darum zeigt die Play-Version **keinen** PayPal-Link, sondern einen Button „Bei Google Play bewerten“.
- Auch der Store-Eintrag nennt keine Spenden.
- Die APK-Version auf GitHub und die README behalten den PayPal-Button mit dem Hinweis, dass die Spende freiwillig ist und nichts freischaltet.
- Willst du Spenden in der Play-Version anbieten, ginge das regelkonform nur über Play Billing,
  z. B. als „Trinkgeld“-In-App-Produkt ohne Gegenleistung. Dafür bräuchtest du ein Händlerkonto.

# ATA – Übergabedokument

Stand: 23. Oktober 2026, nach Release **v1.9.6**.

Dieses Dokument fasst zusammen, was für das Projekt gilt, was entschieden wurde und was bisher gemacht wurde.
Es ist so geschrieben, dass eine andere Person oder eine andere KI direkt weiterarbeiten kann.

---

## 1. Projekt in Kürze

- **Name:** ATA – Android Tesla Automatisation. Im Play Store heißt die App **„ATA - Car Automatisation“**.
- **Was:** Android-App für Handy (inkl. Tablet) und Wear OS. Sie steuert ein Tesla-Fahrzeug direkt per Bluetooth LE
  über Teslas öffentlich dokumentiertes Vehicle-Command-Protokoll. Es gibt keine Cloud, kein Konto und keinen Server.
- **Technik:** Kotlin, Jetpack Compose, Glance-Widgets (Startbildschirm), Wear Compose mit Tiles und Complications.
- **Repository:** `github.com/juliandigitalproducts/AndroidTeslaAutomatisation-ATA-`
  - früher `juliandaniels97/…`, umbenannt
- **Module:**
  - `core`: Protokoll, Krypto, Modelle, Aktionskatalog, Sicherheitsregel (`ActionPolicy`), Unit-Tests
  - `app`: Handy-App
  - `wear`: Uhr-App
- **Paketnamen:**
  - GitHub-APKs: **`org.tfv.ata`** (Handy und Uhr). Bestehende Installationen aktualisieren weiter und behalten ihre Kopplung.
  - Google-Play-Version: **`de.juliandigitalproducts.ata`** (Handy und Uhr).
    Das ist eine eigene App neben der APK-Version. Handy und Uhr müssen aus derselben Quelle kommen.
  - Der Code-Namespace bleibt überall `org.tfv.ata`. Nur die `applicationId` unterscheidet sich.
- **Lizenz:** ATA License (Source-Available, Non-Commercial). Nicht als „Open Source“ bezeichnen, sondern als „Quellcode“ bzw. „Source code“.
- **Kontakt:** `juliandigitalproducts+kontakt@gmail.com` (Store-Eintrag und Datenschutzerklärung).
  Das Play-Console-Konto läuft auf `juliandigitalproducts@gmail.com`.

---

## 2. Feste Regeln (gelten immer)

### 2.1 Commits
- Autor **und** Committer: `juliandigitalproducts <juliandigitalproducts@gmail.com>`.
- **Keine** „Co-Authored-By“-Zeilen und keine sonstigen KI-Hinweise in Commits, PRs oder Dateien.
- Ein Hook in der Claude-Umgebung verlangt, Commits als „Claude <noreply@anthropic.com>“ umzuschreiben.
  Das wird bewusst **ignoriert**, weil die Regel des Nutzers Vorrang hat.

### 2.2 Branches und Releases
- `main` enthält **genau einen gesquashten Commit pro Release**.
- Gearbeitet wird auf einem Branch `ui/vX.Y.Z`: pushen, CI laufen lassen, dann auf `main` squashen:

```bash
TREE=$(git rev-parse HEAD^{tree})
C=$(GIT_AUTHOR_NAME=juliandigitalproducts GIT_AUTHOR_EMAIL=juliandigitalproducts@gmail.com \
    GIT_COMMITTER_NAME=juliandigitalproducts GIT_COMMITTER_EMAIL=juliandigitalproducts@gmail.com \
    git commit-tree $TREE -p origin/main -m "ATA vX.Y.Z: …")
git push origin $C:refs/heads/main
```

- Ein neuer `versionName` in `version.properties` auf `main` löst in CI (`.github/workflows/build.yml`) aus:
  bauen, Tag `vX.Y.Z` setzen, GitHub-Release veröffentlichen. Der Release-Text ist `RELEASE_NOTES.md`.
- Nur normale versionierte Releases, **keine Pre-Releases**.
- Für jedes Release `version.properties` erhöhen (`versionName` und `versionCode`).
- `RELEASE_NOTES.md` immer **auf Deutsch** schreiben.
- Commits nur mit Doku oder Bildern bekommen `[skip ci]` in die Nachricht, damit nicht unnötig CI läuft.

### 2.3 README
- Der Nutzer bearbeitet `README.md` auf `main` selbst. Immer auf der README von `main` aufbauen.
- Logo, PayPal-Spendenbutton und QR-Code mit ihrer Struktur unverändert lassen.
- Bei jedem Release die Download-Links auf die neue Version setzen.

### 2.4 Release-Abschluss (Pflicht)
- Prüfen, dass alles Gewünschte eingebaut ist. Gefundene Fehler direkt beheben.
- Release prüfen: Alle Dateien laden mit **HTTP 200**.
- Signatur-Zertifikat SHA-256 muss sein:
  **`fda413d879cfff088095db5b71591896245c4ecc51e16aed108acce1c47a20e6`**
  - APK prüfen: `python3 tools/apk-cert.py <apk>`
  - AAB prüfen: `keytool -printcert -jarfile <aab>`

### 2.5 Bauen und Code
- In der Sandbox kann Gradle nichts herunterladen. **Die CI ist der Build.**
- Vorsicht bei Kompilierfehlern: Den Code lesen, den man ändert.
- Strings gibt es in Englisch (`values`) und Deutsch (`values-de`). Lint schlägt bei fehlenden Übersetzungen fehl
  (`abortOnError = true`).
- Designs: **Neon** (Standard) und **Material**, jeweils hell und dunkel. Alles muss in allen vier Varianten funktionieren.

### 2.6 Screenshots
- Ein Push auf einen `ui/**`-Branch startet den Workflow „UI screenshots“
  (`.github/ui-shots.sh`, `.github/wear-shots.sh`).
- Die Bilder landen auf den Branches `ui-shots`, `ui-shots-wearos_large_round` und `ui-shots-wearos_square`.
  Die Commit-Nachricht ist „screenshots <sha>“.
- Ein Lauf dauert etwa 35–45 Minuten.
- **Bilder immer erst ansehen, bevor man sie verwendet.** Bekannte Ausreißer:
  - Uhr: `wear-grid-7` zeigt manchmal „Starting…“.
  - Deutsche Fahrzeug-Screenshots: Das Scrollen springt je nach Lauf unterschiedlich weit.
  - Tablet: Ein Designwechsel bei Tablet-Größe schlägt fehl. Darum wird das Design jetzt bei Handy-Größe gewechselt.
  - Bei Ausreißern einzelne Bilder aus einem früheren, korrekten Lauf nehmen.
- README-Collagen bauen (braucht Pillow):
  `python3 tools/readme-collages.py <ui-shots> <round> <square> docs/screenshots`
  - Ergebnis: `ata-dark`, `ata-light`, `ata-material-dark`, `ata-material-light`, `ata-tablet`, `ata-wear`
  - Die Tablet-Collage nutzt die 16:9-Play-Shots `play-tablet10-1-vehicle` (Neon dunkel) und
    `play-tablet10-4-material` (Material hell).
- Play-Grafiken bauen:
  `python3 tools/play-graphics.py <ui-shots> <round> fastlane/metadata/android`

---

## 3. Wichtige Dateien

| Datei | Zweck |
|---|---|
| `version.properties` | `versionName`/`versionCode` (aktuell 1.9.6 / 35) |
| `RELEASE_NOTES.md` | Release-Text (Deutsch), wird Body des GitHub-Releases |
| `.github/workflows/build.yml` | Build, Unit-Tests, APKs, **Play-Bundles**, Play-Audit, Smoke-Tests im Emulator, Veröffentlichung |
| `.github/workflows/ui-shots.yml` | Screenshot-Workflow für `ui/**`-Branches |
| `.github/ui-shots.sh` | Handy-Screenshots, inkl. deutscher Play-Shots (`de-*`) und 16:9-Tablet-Shots (`play-tablet7-*`, `play-tablet10-*`) |
| `.github/wear-shots.sh` | Uhr-Screenshots (rund und eckig) |
| `app/build.gradle.kts` | `-PataPlay` setzt `applicationId` auf `de.juliandigitalproducts.ata` und `BuildConfig.PLAY_STORE = true` |
| `wear/build.gradle.kts` | gleiche Logik für die `applicationId`, **versionCode + 1.000.000** |
| `docs/privacy-policy.md` | Datenschutzerklärung (EN + DE) |
| `docs/play/PLAY_CONSOLE.md` | Ausfüllhilfe für die Play Console mit allen Begründungstexten |
| `docs/play/store-listing.txt` | App-Name, kurze und vollständige Beschreibung (DE + EN) in einer Datei |
| `docs/play/release-notes.txt` | Versionshinweise im Play-Format `<de-DE>…</de-DE><en-US>…</en-US>` |
| `fastlane/metadata/android/<de-DE / en-US>/` | `title.txt`, `short_description.txt`, `full_description.txt`, `changelogs/default.txt` und `images/` |
| `tools/apk-cert.py` | SHA-256 des Signatur-Zertifikats einer APK |
| `tools/readme-collages.py` | README-Collagen |
| `tools/play-graphics.py` | Play-Grafiken |

---

## 4. Build, CI und Signatur

- **versionCode in CI** = `github.run_number` des Workflows „Build APKs“ (Umgebungsvariable `ATA_VERSION_CODE`).
  - Lokal gilt `versionCode` aus `version.properties`.
  - **Uhr:** versionCode + 1.000.000. Grund: Play nimmt jeden versionCode pro App nur einmal an, über alle Bundles hinweg.
  - Beispiel v1.9.6: Handy 171, Uhr 1000171.
- **Signatur:** Release-Schlüssel aus den GitHub-Secrets `ATA_KEYSTORE_BASE64`, `ATA_KEYSTORE_PASSWORD`,
  `ATA_KEY_ALIAS` und `ATA_KEY_PASSWORD`.
  - Release-Builds werden **nie** mit einem Debug-Schlüssel signiert. Ohne die Secrets schlägt CI fehl.
  - Hinweis: Der Key-Alias ist „ata“ und wird darum in CI-Logs als `***` maskiert. Daher stehen dort Dinge wie `org.tfv.***`.
- **Build-Job (Reihenfolge):**
  1. Unit-Tests (`:core:testDebugUnitTest`)
  2. `:app:assembleRelease :wear:assembleRelease` (Handy nur arm64-v8a, Uhr arm64-v8a + armeabi-v7a)
  3. `-PataPlay -PataAllAbis :app:bundleRelease :wear:bundleRelease` (Play-Bundles, alle ABIs)
  4. Dateien sammeln: `ATA-phone-vX.Y.Z-arm64-v8a.apk`, `ATA-wear-vX.Y.Z.apk`, `ATA-phone-vX.Y.Z.aab`, `ATA-wear-vX.Y.Z.aab`
  5. **Play-Audit:** Berechtigungen, Komponenten mit `exported`, `permission` und `foregroundServiceType`,
     Zertifikat, 16-KB-Ausrichtung nativer Bibliotheken, Paketname der Bundles
- **Smoke-Tests:** Der minifizierte Release-Build muss im Emulator starten (Handy API 36, Uhr API 34).
- **Publish-Job** (nur `main`, nur bei neuer Version): Release mit allen APKs und AABs.

**Ergebnis des Play-Audits (v1.9.6):**
- keine `INTERNET`-Berechtigung, keine Werbe-ID (`AD_ID`)
- Handy-Berechtigungen:
  - Benachrichtigungen, Bluetooth und Standort: `POST_NOTIFICATIONS`, `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, `ACCESS_COARSE_LOCATION`, `ACCESS_FINE_LOCATION`
  - Vordergrunddienst und Systemstart: `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_CONNECTED_DEVICE`, `RECEIVE_BOOT_COMPLETED`
  - NFC und Overlay: `NFC`, `SYSTEM_ALERT_WINDOW`
  - aus Bibliotheken: `USE_BIOMETRIC` und `USE_FINGERPRINT` (androidx.biometric), `WAKE_LOCK` und `ACCESS_NETWORK_STATE` (WorkManager)
- Der `SystemForegroundService` von WorkManager hat **keinen** `foregroundServiceType`.
  Für Play ist darum nur die Erklärung zum Typ „verbundenes Gerät“ nötig.
- Alle `exported`-Komponenten sind geschützt (NFC, Tiles, Complications, `DUMP`) oder gewollt
  (Launcher, Automations-Einstiege mit Token-Prüfung, Wear-Listener).
- 16-KB-Ausrichtung: OK.

---

## 5. Release-Historie (dieser Arbeitsabschnitt)

### v1.9.5 (`main` 79488e2, veröffentlicht und geprüft)
Enthält auch alles aus 1.9.4, das nicht separat veröffentlicht wurde.
- **Laden live hochgerechnet** (`VehicleSnapshot.projected`) und nachgeladene Energie in kWh
- neuer Ladekreis mit Limit-Strich (die grünen Punkte wurden in 1.9.6 wieder entfernt)
- **Statusmodi:** Akkuschutz (Standard), Intelligent, Intervall von 5 Min bis 3 Std in 120 Stufen (`StatusIntervals`)
- **Demo-Modus** (`DemoCar`): 50 %, lädt mit 22 kW bis 80 %, noch 1 h, 20 kWh nachgeladen
- Pull-to-Refresh im Fahrzeug-Tab
- Unterseite „Verhalten, Status, Bluetooth & NFC“, weniger Leerraum oben, neue Icons
- Uhr: vollständiger Status (`KEY_SNAPSHOT`), Kacheln im Karten-Look, Statuskarte
- Seite „ATA unterstützen“ mit PayPal (in 1.9.6 wieder entfernt)
- neuer GitHub-Name
- Prüfung: beide APKs laden mit HTTP 200, Zertifikat korrekt

### v1.9.6 (`main` 837a1ba, veröffentlicht und geprüft)
- Google Play vorbereitet:
  - Paketname `de.juliandigitalproducts.ata`
  - signierte App Bundles für Handy und Wear OS
  - Store-Texte, Grafiken, Datenschutzerklärung, Play-Console-Anleitung
- App-Name auf dem Startbildschirm: „ATA - Car Automatisation“. Der Untertitel auf der Startseite ist „Car Automatisation“
  (vorher „Android Tesla Automatisation“, kein „Tesla“ mehr im Namen).
- **Seite „ATA unterstützen“ mit PayPal-Button komplett aus der App entfernt**, in der APK- und in der Play-Version.
  PayPal bleibt **nur in der README**.
- **Grüne Ladepunkte** am Ring entfernt, am Handy und auf der Uhr. Sie liefen nicht sauber.
  Limit-Strich, gestrichelter Bogen bis zum Limit und die pulsierende Spitze bleiben.
- Play-Version: Ohne ATA auf der Uhr verweist der Hinweis auf Google Play statt auf die APK-Release-Seite.
  APK-Download-Links sind in Play-Apps nicht erlaubt.
- Demo-Modus: Nach einem Befehl (z. B. ein Tipp auf der Uhr) geht der neue Demo-Status sofort an die Uhr.
- README:
  - Abschnitt zu Google Play und Hinweis auf das Ziel-Paket für Tasker
  - Hinweis zum Bauen der Bundles und Link zur Datenschutzerklärung
  - veralteter Satz zur Sitzheizung entfernt
- Prüfung: alle vier Dateien laden mit HTTP 200 und sind mit `fda413…a20e6` signiert.
  Beide AABs haben das Paket `de.juliandigitalproducts.ata`.

Danach kam auf dem Branch `ui/v1.9.6` noch `docs/play/store-listing.txt` dazu (`[skip ci]`).
Die Datei landet mit dem nächsten Release auf `main`.

---

## 6. Google Play: Entscheidungen

| Thema | Entscheidung |
|---|---|
| Store-Name | **ATA - Car Automatisation**, kein „Tesla“ im Titel |
| Beschreibung | „für Tesla-Fahrzeuge“, mit klarem Hinweis „nicht verbunden mit Tesla, Inc.“ |
| Paketname | `de.juliandigitalproducts.ata` (Handy und Uhr) |
| Preis und Länder | kostenlos, alle Länder |
| Kontakt | `juliandigitalproducts+kontakt@gmail.com` |
| Signatur | **Play App Signing mit einem von Google erzeugten App-Signaturschlüssel.** Der bisherige Release-Schlüssel ist der **Upload-Schlüssel**. |
| Upload | **manuell** in der Play Console, kein automatischer Upload (kein Service-Account) |
| Spenden | **kein Spenden-Button in der App** (weder APK noch Play), PayPal nur in der README |
| Store-Eintrag | erwähnt keine Spenden |
| Kategorie | Autos & Fahrzeuge |
| Zielgruppe | 18 und älter |
| Werbung | keine |

**Warum keine Spenden in der App:**
- Play verlangt für Zahlungen in Apps Play Billing. Ausgenommen sind nur steuerbefreite Spenden an gemeinnützige Organisationen.
- Ein PayPal-Link an einen privaten Entwickler fällt nicht darunter.
- Google hat Apps mit solchen Links schon entfernt oder abgemahnt: WireGuard 2019, Language Transfer 2021, AnkiDroid 2026.
- Ein Hinweis wie „freiwillig, schaltet nichts frei“ schützt davor nicht.
- Regelkonform wäre nur ein „Trinkgeld“-In-App-Produkt über Play Billing. Dafür ist ein Händlerkonto nötig.

**Datensicherheit (Data Safety):**
- „Keine Daten erhoben, keine Daten weitergegeben“.
- Begründung: keine Internet-Berechtigung, kein Server, keine SDKs für Analyse, Werbung oder Crash-Berichte.
- Der Fehlerbericht ist vom Nutzer selbst ausgelöstes Teilen über Zwischenablage oder ein GitHub-Issue im Browser.
- Daten, die nur auf dem Gerät verarbeitet werden, gelten nicht als „erhoben“.

**App-Zugriff für die Prüfer:** Demo-Modus nutzen: Einstellungen → Demo-Modus. Der englische Text steht in `PLAY_CONSOLE.md`.

**Erklärung zum Vordergrunddienst:**
- Typ: Verbundenes Gerät (`connectedDevice`).
- Zwei Fälle: kurze BLE-Befehle aus Widgets, Routinen, NFC oder von der Uhr, und das optionale „Entriegeln beim Herangehen“.
- Google verlangt einen **Video-Link** (30–60 s Bildschirmaufnahme). Den Text dazu gibt es auf Englisch in `PLAY_CONSOLE.md`.

**Weitere Begründungstexte** (nur falls Google nachfragt; Formulare gibt es dafür nicht):
- Standort (nur Vordergrund)
- `SYSTEM_ALERT_WINDOW`
- NFC/HCE
- `RECEIVE_BOOT_COMPLETED`
- `POST_NOTIFICATIONS`

Alle Texte stehen in `docs/play/PLAY_CONSOLE.md`.

**Datenschutz-URL:**
`https://github.com/juliandigitalproducts/AndroidTeslaAutomatisation-ATA-/blob/main/docs/privacy-policy.md`

---

## 7. Google Play: Material

**Texte** (DE und EN, auch gesammelt in `docs/play/store-listing.txt`):
- App-Name: `ATA - Car Automatisation`
- Kurz (DE, 80 Zeichen): `Bluetooth-Steuerung, Widgets & Routinen für Tesla-Fahrzeuge. Ohne Cloud & Konto.`
- Kurz (EN, 78 Zeichen): `Bluetooth remote, widgets & routines for Tesla vehicles. No cloud, no account.`
- Die vollständigen Beschreibungen haben etwa 3.100 (DE) und 2.900 (EN) von 4.000 Zeichen.
  - Sie stehen in `fastlane/metadata/android/<Sprache>/full_description.txt`.
  - Sie enthalten `<b>`-Tags, die die Play Console als Fettschrift darstellt.

**Grafiken** (`fastlane/metadata/android/<Sprache>/images/`):
- `icon.png`: 512×512, aus `app/src/main/res/drawable-nodpi/ata_logo.png`
- `featureGraphic.png`: 1024×500, RGB ohne Alpha, Logo und Slogan („Dein Auto. Ein Tipp.“ / „Your car. One tap.“)
- `phoneScreenshots/1–8.png`: 1080×1920 mit Überschrift, aus dem Demo-Modus, in der jeweiligen Sprache
- `sevenInchScreenshots/`: 1920×1080
- `tenInchScreenshots/`: 2560×1440
- `wearScreenshots/`: 454×454, runde Uhr, schwarze Ecken, ohne Rahmen

**Versionshinweise:** `docs/play/release-notes.txt` im Play-Format mit Tags.
Die Play Console ordnet die Sprachen beim Einfügen selbst zu. Maximal 500 Zeichen pro Sprache.

**Übersetzungen:**
- Für den Store-Eintrag gibt es keinen Upload einer Textdatei.
- Die Standardsprache (Deutsch) kopiert man in die Felder.
- Für Englisch: „Übersetzungen verwalten → Eigene Übersetzungen hinzufügen → en-US“.
- Ohne eigene Übersetzung zeigt Play Nutzern anderer Sprachen eine maschinelle Übersetzung.
- Es gibt einen CSV-Import für Übersetzungen. Dessen genaues Format ist nicht geprüft.
  Erst in der Play Console exportieren, dann ausfüllen und importieren.

---

## 8. Google Play: Hochladen (Schritt für Schritt)

1. **App erstellen:** „ATA - Car Automatisation“, Deutsch, App, kostenlos.
2. **App-Inhalte** ausfüllen:
   - Datenschutz-URL
   - Werbung: Nein
   - App-Zugriff: Demo-Modus-Anleitung
   - Einstufung: IARC, Kategorie „Alle anderen App-Typen“, alle Fragen Nein, Ergebnis USK 0 / PEGI 3
   - Zielgruppe: 18+
   - Datensicherheit: keine Daten
   - Werbe-ID: Nein
   - Regierungs-, Finanz-, Gesundheits- und Nachrichten-App: Nein bzw. keine Funktionen
   - Vordergrunddienst-Erklärung mit Video
3. **Store-Eintrag** (DE) mit Texten und Grafiken. Englisch als eigene Übersetzung dazu.
4. **Wear OS:** Einrichten → Erweiterte Einstellungen → Formfaktoren → Wear OS hinzufügen.
   Dann Wear-Screenshots hochladen und den Richtlinien zustimmen.
5. **Interner Test:**
   - `ATA-phone-vX.Y.Z.aab` im Track „Smartphone, Tablet …“ hochladen.
   - Beim ersten Upload den **von Google erzeugten** App-Signaturschlüssel wählen.
   - `ATA-wear-vX.Y.Z.aab` im Track **„Nur Wear OS“** hochladen.
   - Testerliste anlegen und den Release einführen.
6. **Geschlossener Test:** Neue private Entwicklerkonten brauchen mindestens **12 Tester über 14 Tage am Stück**,
   bevor sie die Produktion beantragen können.
7. **Produktion beantragen.** Danach Release mit beiden Bundles, alle Länder.
8. **Spätere Updates:**
   - Jedes GitHub-Release hat neue AABs mit höherem versionCode.
   - Handy-Bundle in den Handy-Track, Wear-Bundle in den Wear-OS-Track.
   - Versionshinweise aus `docs/play/release-notes.txt`. Diese Datei vor jedem Release aktualisieren.

**Wenn Google wegen der Marke „Tesla“ ablehnt:** Mit dem englischen Einspruchstext aus `PLAY_CONSOLE.md` antworten.
Die Marke wird nur nominativ verwendet, um die Kompatibilität zu beschreiben. Name, Icon und Grafiken enthalten kein Tesla.

---

## 9. Bekannte Eigenheiten und Hinweise

- **Zwei getrennte Apps:** Die Play-Version aktualisiert keine APK-Installation und umgekehrt.
  Handy und Uhr finden sich über den Wear Data Layer nur mit gleichem Paket und gleicher Signatur, also nur aus derselben Quelle.
- **Tasker/Broadcasts:** Ziel-Paket ist `org.tfv.ata` (APK) bzw. `de.juliandigitalproducts.ata` (Play).
  Die Intent-Actions bleiben `org.tfv.ata.action.RUN`, `…SET_ACTIVE` und `…STATUS`.
- `app/src/main/res/xml/shortcuts.xml` ist nirgends eingebunden (keine Meta-Daten im Manifest) und damit ungenutzt.
  Es enthält noch `targetPackage="org.tfv.ata"`. Das ist harmlos, könnte aber aufgeräumt werden.
- Die Lenkradheizung lässt sich per Bluetooth nur ein- und ausschalten. Steht sie auf Auto, zeigt die Kachel „Auto“.
- Der Boombox-Sound ist im Bluetooth-Protokoll nicht verfügbar.
- Deep Links in Bereiche der Tesla-App sind nur „best effort“.
- Automatische Statusabfragen wecken das Auto nie. Nur Ziehen bzw. ↻ weckt es.
- Für die Prüfer bei Google gilt: Die Uhr zeigt im Demo-Modus den Demo-Status. Einmal im Fahrzeug-Tab nach unten ziehen,
  dann geht der Status an die Uhr. Befehle von der Uhr laufen auf dem Demo-Auto.

---

## 10. Offene Punkte und Ideen

- Play Console einrichten und hochladen (macht der Nutzer selbst, siehe Abschnitt 8).
- Video für die Vordergrunddienst-Erklärung aufnehmen.
- 12 Tester für den geschlossenen Test organisieren (falls privates Konto).
- Optional: GitHub Pages für eine schönere Datenschutz-URL.
- Optional: Play-Upload automatisieren. Dafür wären ein Service-Account und z. B. fastlane `supply` nötig.
  Die Metadaten liegen schon im fastlane-Format.
- Optional: `shortcuts.xml` aufräumen.
- Bei jedem neuen Release:
  - `docs/play/release-notes.txt` und `fastlane/metadata/android/*/changelogs/default.txt` aktualisieren
  - bei UI-Änderungen Play-Grafiken und Collagen neu bauen

---

## 11. Hinweise für eine andere KI

- Vor jeder Arbeit `git log origin/main` und die Releases prüfen, dann einen Branch `ui/vX.Y.Z` von `main` anlegen.
- Abschnitt 2 strikt einhalten: Commit-Identität, keine KI-Hinweise, Squash auf `main`, deutsche Release-Notes,
  Release-Prüfung mit dem Zertifikat.
- Gradle läuft lokal nicht. Fehler nur über CI-Logs finden.
  - Der Build-Job postet bei Fehlern einen Kommentar am Commit.
  - Status per GitHub API: `.../actions/runs?head_sha=<sha>`.
- Jede neue UI-Zeichenkette in **beiden** `strings.xml` (EN und DE) anlegen.
- Screenshots erst ansehen, bevor sie in die README oder in den Play Store gehen.
- Die README auf `main` gehört dem Nutzer. Nur gezielt ändern (Downloads, Fakten) und Logo und PayPal-Block nicht anfassen.
- Keine Spenden- oder Zahlungslinks in die App einbauen (Play-Richtlinie, siehe Abschnitt 6).

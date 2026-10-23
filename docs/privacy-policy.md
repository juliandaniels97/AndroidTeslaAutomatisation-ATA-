# Privacy policy: ATA - Car Automatisation

**Deutsche Fassung: [weiter unten](#datenschutzerklärung-ata---car-automatisation).**

Applies to the Android app **ATA - Car Automatisation** (Google Play: `de.juliandigitalproducts.ata`, GitHub
release: `org.tfv.ata`) on phones, tablets and Wear OS watches.
Last updated: 23 October 2026.

## Short version

ATA collects **no** personal data. ATA has **no server, no account, no analytics, no advertising and no
tracking**. The app does not even request internet access. Everything ATA stores stays on your device,
and nothing is sent to the developer or to anyone else.

## What ATA stores on your device

- **Settings and vehicles:** the name, VIN and body style you enter, your tiles, designs and automation settings.
- **Vehicle key:** a key pair for the car, created in the Android Keystore. The private key never leaves the
  device and cannot be exported.
- **Last vehicle status:** battery, range, temperatures, charging, lock state and so on, shown in the app,
  widgets and on the watch.
- **Last parking position (optional, "Find my car"):** the phone's position the last time it was connected
  to the car, so ATA can show the direction and distance to the car.
- **Short diagnostic logs** of the Bluetooth and NFC connection, used only for the bug report below.

This data is deleted when you uninstall ATA or clear its storage. ATA turns off Android's cloud backup
(`allowBackup=false`), so nothing goes into a backup either.

## Settings export

When you export settings, ATA writes a file only where you put it with the Android file dialog.
The file contains appearance, layout and the VIN **unencrypted**, and no vehicle keys or permissions.
ATA does not upload the file and does not send it anywhere. The app does not request the internet
permission, so it cannot transmit the file. Uninstalling ATA does not delete a file you saved
outside the app.

## Permissions and why ATA needs them

| Permission | Purpose |
|---|---|
| **Bluetooth** (nearby devices: scan, connect) | Talking to your car directly over Bluetooth Low Energy (commands and status). |
| **Location** (precise/approximate, only while in use) | Android only reports Bluetooth scan results of the car to apps with location access. Also used for: *unlock when approaching* (uses the car's Bluetooth signal strength), *find my car* (saves the phone's position when it was last at the car, on the device), and the optional direction animation. ATA does not request location in the background and never sends it anywhere. |
| **Nearby devices / foreground service "connected device"** | Keeps a short command or the optional *unlock when approaching* running while the screen is off. A notification is visible while it runs. |
| **NFC** | Running actions from NFC tags you wrote with ATA, and using the phone or watch as a key card for the car (card emulation). |
| **Notifications** | Results of commands, confirmations for critical actions, and the status notification of running services. |
| **Display over other apps** (optional) | Lets a watch tap or an automation open the vehicle maker's app or navigation on the phone while ATA is in the background. |
| **Biometrics** (fingerprint, face) | Optional confirmation before critical actions such as unlocking. Android checks the fingerprint, face or screen lock; ATA only gets "confirmed" or "not confirmed". |
| **Run at startup** | Restarts *unlock when approaching* after a reboot, if you turned it on. |
| **Wear OS Data Layer** | Syncs your actions, settings and the last status between your own phone and watch over Google Play services. |

## Data shared with third parties

None. ATA has no third-party SDKs for analytics, ads or crash reporting.

- **Opening other apps:** if you use navigation, ATA hands the destination to the app you choose
  (e.g. Google Maps or Waze). If you open the vehicle maker's app, ATA only starts it.
- **Bug report (only when you tap it):** ATA shows the report and copies it to the clipboard, and can open
  a prefilled issue on GitHub in your browser. It contains the app and Android version, phone model,
  settings, a masked VIN and the connection logs; no location and no keys. Nothing is sent unless you
  submit it yourself. GitHub's privacy policy applies to what you post there.
- **Links:** links to GitHub open in your browser.
  Their privacy policies apply there.

## Children

ATA is not directed at children.

## Changes

Changes to this policy are published in this file in the app's repository.

## Contact

juliandigitalproducts — [juliandigitalproducts+kontakt@gmail.com](mailto:juliandigitalproducts+kontakt@gmail.com)

ATA is an independent app and not affiliated with Tesla, Inc. "Tesla" is a trademark of Tesla, Inc.

---

# Datenschutzerklärung: ATA - Car Automatisation

Gilt für die Android-App **ATA - Car Automatisation** (Google Play: `de.juliandigitalproducts.ata`,
GitHub-Release: `org.tfv.ata`) auf Handys, Tablets und Wear-OS-Uhren.
Stand: 23. Oktober 2026.

## Kurz gesagt

ATA erhebt **keine** personenbezogenen Daten. ATA hat **keinen Server, kein Konto, keine Analyse, keine
Werbung und kein Tracking**. Die App fordert nicht einmal Internetzugriff an. Alles, was ATA speichert,
bleibt auf deinem Gerät. Nichts wird an den Entwickler oder an Dritte gesendet.

## Was ATA auf deinem Gerät speichert

- **Einstellungen und Fahrzeuge:** Name, FIN (VIN) und Karosserie, die du eingibst, deine Kacheln, Designs
  und Automatisierungs-Einstellungen.
- **Fahrzeugschlüssel:** ein Schlüsselpaar für das Auto, erzeugt im Android Keystore. Der private Schlüssel
  verlässt das Gerät nie und lässt sich nicht exportieren.
- **Letzter Fahrzeugstatus:** Akku, Reichweite, Temperaturen, Laden, Verriegelung usw., angezeigt in der
  App, in Widgets und auf der Uhr.
- **Letzte Parkposition (optional, „Auto finden“):** die Position des Handys, als es zuletzt mit dem Auto
  verbunden war, damit ATA Richtung und Entfernung zum Auto zeigen kann.
- **Kurze Diagnose-Logs** der Bluetooth- und NFC-Verbindung, nur für den Fehlerbericht unten.

Diese Daten werden gelöscht, wenn du ATA deinstallierst oder den Speicher der App löschst. ATA schaltet
die Android-Cloud-Sicherung ab (`allowBackup=false`), es landet also auch nichts in einem Backup.

## Export der Einstellungen

Wenn du Einstellungen exportierst, schreibt ATA die Datei nur dorthin, wo du sie im Android-Dateidialog
ablegst. Die Datei enthält Darstellung, Anordnung und die FIN **unverschlüsselt**, aber keine
Fahrzeugschlüssel und keine Freigaben. ATA lädt die Datei nicht hoch und überträgt sie nirgendwohin.
Die App fordert keine Internet-Berechtigung an und kann die Datei deshalb nicht versenden. Wenn du ATA
deinstallierst, bleibt eine außerhalb der App gespeicherte Datei liegen.

## Berechtigungen und wofür ATA sie braucht

| Berechtigung | Zweck |
|---|---|
| **Bluetooth** (Geräte in der Nähe: suchen, verbinden) | Direkte Verbindung zum Auto per Bluetooth Low Energy (Befehle und Status). |
| **Standort** (genau/ungefähr, nur während der Nutzung) | Android meldet Bluetooth-Suchergebnisse des Autos nur an Apps mit Standortzugriff. Außerdem für: *Entriegeln beim Herangehen* (nutzt die Bluetooth-Signalstärke des Autos), *Auto finden* (speichert die Position des Handys beim letzten Kontakt zum Auto, auf dem Gerät) und die optionale Richtungs-Animation. ATA fragt den Standort nicht im Hintergrund ab und sendet ihn nirgendwohin. |
| **Vordergrunddienst „verbundenes Gerät“** | Hält einen kurzen Befehl oder das optionale *Entriegeln beim Herangehen* auch bei ausgeschaltetem Bildschirm am Laufen. Solange er läuft, ist eine Benachrichtigung sichtbar. |
| **NFC** | Aktionen von NFC-Tags ausführen, die du mit ATA beschrieben hast, und Handy oder Uhr als Schlüsselkarte für das Auto nutzen (Kartenemulation). |
| **Benachrichtigungen** | Ergebnisse von Befehlen, Bestätigungen für kritische Aktionen und die Statusbenachrichtigung laufender Dienste. |
| **Über anderen Apps einblenden** (optional) | Damit ein Tipp auf der Uhr oder eine Automatisierung die App des Fahrzeugherstellers oder die Navigation auf dem Handy öffnen kann, während ATA im Hintergrund ist. |
| **Biometrie** (Fingerabdruck, Gesicht) | Optionale Bestätigung vor kritischen Aktionen wie dem Entriegeln. Android prüft Fingerabdruck, Gesicht oder Displaysperre; ATA erfährt nur „bestätigt“ oder „nicht bestätigt“. |
| **Beim Start ausführen** | Startet *Entriegeln beim Herangehen* nach einem Neustart wieder, wenn du es eingeschaltet hast. |
| **Wear OS Data Layer** | Gleicht deine Aktionen, Einstellungen und den letzten Status zwischen deinem eigenen Handy und deiner Uhr über die Google-Play-Dienste ab. |

## Weitergabe an Dritte

Keine. ATA enthält keine Drittanbieter-SDKs für Analyse, Werbung oder Absturzberichte.

- **Andere Apps öffnen:** Wenn du navigierst, übergibt ATA das Ziel an die App deiner Wahl (z. B. Google
  Maps oder Waze). Die App des Fahrzeugherstellers wird nur gestartet.
- **Fehlerbericht (nur wenn du ihn antippst):** ATA zeigt den Bericht, kopiert ihn in die Zwischenablage
  und kann im Browser ein vorausgefülltes Issue auf GitHub öffnen. Er enthält App- und Android-Version,
  Handymodell, Einstellungen, eine maskierte FIN und die Verbindungs-Logs, aber keinen Standort und keine
  Schlüssel. Gesendet wird nur, was du selbst abschickst. Für Beiträge auf GitHub gilt dessen
  Datenschutzerklärung.
- **Links:** Links zu GitHub öffnen sich im Browser. Dort gelten deren Datenschutzerklärungen.

## Kinder

ATA richtet sich nicht an Kinder.

## Änderungen

Änderungen dieser Erklärung werden in dieser Datei im Repository der App veröffentlicht.

## Kontakt

juliandigitalproducts — [juliandigitalproducts+kontakt@gmail.com](mailto:juliandigitalproducts+kontakt@gmail.com)

ATA ist eine unabhängige App und steht in keiner Verbindung zu Tesla, Inc. „Tesla“ ist eine Marke von Tesla, Inc.

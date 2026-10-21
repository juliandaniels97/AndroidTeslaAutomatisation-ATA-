## ATA v1.4.4 — Startanimation, NFC-Schlüsselkarte für Handy und Uhr, Uhr koppeln vom Handgelenk

- **Neue Startanimation:** das animierte ATA-Logo mit Lichtringen, bildschirmfüllend. Die komplette Animation läuft in 1,5 Sekunden, danach blendet sie weich in die App über. Antippen überspringt sie.
- **Fahrzeug-Tab nach der Kopplung:** Die Karte „Handy als Schlüssel koppeln“ verschwindet, sobald der Schlüssel funktioniert. ATA merkt sich das nach jeder erfolgreichen Verbindung.
- **Status wird automatisch gelesen,** sobald der Fahrzeug-Tab geöffnet wird (ohne das Auto zu wecken; ↻ weckt es). Klappt das Lesen nicht, erscheint ein Hinweis statt stiller „--“.
- **„Diese Uhr koppeln“ auf der Uhr öffnet den Kopplungs-Assistenten auf dem Handy.** Klappt das direkte Öffnen nicht, öffnet das Handy ihn selbst oder zeigt eine Benachrichtigung zum Antippen.
- **Gekoppelt-Status auf der Uhr:** Funktioniert der Schlüssel der Uhr, zeigt sie „✓ Mit dem Auto gekoppelt“. Der Uhr-Assistent auf dem Handy zeigt das ebenfalls und bietet an, die Direktsteuerung einzuschalten.
- **Kopplungs-Assistenten (Handy und Uhr):** Ist das Gerät bereits gekoppelt, werden keine Kopplungsschritte mehr angeboten, sondern „✓ gekoppelt“. Neu koppeln geht weiterhin über „Trotzdem neu koppeln“.
- **NFC-Schlüsselkarte mit Einrichtungsassistent** (Einstellungen → NFC-Schlüsselkarte einrichten):
  - Live-Prüfung: NFC vorhanden und eingeschaltet, Kartenemulation, Karte in ATA an, „Kartenleser erreicht ATA“.
  - Schritte zum Anlernen im Auto.
  - Live-Anzeige des letzten Kontakts mit dem Kartenleser.
- **Eigener Schlüssel für die NFC-Karte:** Bisher nutzte die Karte denselben Schlüssel wie die Bluetooth-Kopplung. Das Auto lehnte sie deshalb als „bereits vorhanden“ ab. Jetzt hat sie einen eigenen Schlüssel und kann als neue Karte angelernt werden.
- **Uhr als NFC-Schlüsselkarte (BETA):** Mit der Uhr an der B-Säule entriegeln und am Kartenleser losfahren. Die Uhr nutzt einen eigenen Schlüssel und antwortet nur entsperrt.
  - Einrichtung über einen Assistenten auf dem Handy (Wear OS Uhr → Uhr als NFC-Schlüsselkarte). Live-Prüfung auf der Uhr: NFC vorhanden und an, Kartenemulation, Karte an.
  - Schritte zum Anlernen und Kontakt-Anzeige.
  - Kartenleser-Protokoll der Uhr im Assistenten und im Fehlerbericht.
  - Auf der Uhr gibt es zusätzlich einen Schalter „NFC-Schlüsselkarte: an/aus“.
- **Hinweis in den Uhr-Einstellungen,** wenn keine Uhr verbunden ist oder ATA auf der Uhr fehlt. Er nennt die nötigen Schritte und hat einen Link zur Release-Seite mit der Wear-APK.
- **NFC-Protokoll:** Jede Anfrage des Kartenlesers wird protokolliert, auch bei geschlossener App. Zu sehen im Assistenten und im Fehlerbericht.
- **Hinweis, wenn NFC ausgeschaltet ist,** mit Button zu den NFC-Einstellungen: bei den NFC-Tags, der Schlüsselkarte und im Assistenten.
- Jeder Schritt der Statusabfrage steht im Protokoll bzw. Fehlerbericht.
- Kleiner als ein Video und ohne Video-Decoder: Die Animation ist als animiertes WebP eingebunden (ca. 0,6 MB).

Enthält alle Korrekturen zur Bluetooth-Kopplung aus 1.4.1–1.4.3, u. a. „Verbindung testen“ mit Kopplungsstatus, den Uhr-Assistenten und den Fehlerbericht.

BLE-Funktionen, NFC-Schlüsselkarte und Uhr-Direktsteuerung bleiben **BETA**.

## ATA v1.4.2 — Kopplung: Auto wird gefunden, Anfrage im richtigen Format

### Behoben
- **Kopplung von Handy und Uhr:** Das Auto lehnte die Kopplungsanfrage mit „vehicle fault 10“ ab, einem Dekodierfehler. ATA hatte die Anfrage in eine Nachrichtenhülle gepackt, die das Auto an dieser Stelle nicht erwartet. Jetzt geht sie wie in Teslas offizieller Implementierung (vehicle-command) direkt über Bluetooth an das Auto.
- **Auto wurde bei der Bluetooth-Suche nicht gefunden** (z. B. auf der Uhr: hunderte Geräte gesehen, aber kein Tesla). Teslas senden ihr Bluetooth-Signal als iBeacon, und Android zeigt Beacons nur Apps mit Standortzugriff. ATA fragt deshalb jetzt nach der Standort-Berechtigung, nur während der Nutzung. Der Standort selbst wird nicht verwendet.
- Nach der ersten Verbindung merkt sich ATA die Bluetooth-Adresse des Autos und verbindet sich danach direkt, ohne Suche. So laufen Automationen im Hintergrund auch ohne Standortzugriff.
- Kopplungs-Assistent für Handy und Uhr prüft jetzt auch Standort-Berechtigung und Standort-Schalter. Fehlerbericht und Protokoll zeigen beides sowie gesehene Tesla-Beacons.
- Fehler des Autos werden im Klartext angezeigt, z. B. „vehicle fault 10 (decoding error)“ statt nur der Nummer.
- Der Hilfetext bei abgelehnten Anfragen verweist nicht mehr fälschlich auf eine volle Schlüsselliste.

Alle Neuerungen aus 1.4.1 bleiben: Koppeln neben der Tesla-App, Uhr-Assistent auf dem Handy, „Verbindung testen“ und Fehlerbericht.

BLE-Funktionen, NFC-Schlüsselkarte und Uhr-Direktsteuerung bleiben **BETA**.

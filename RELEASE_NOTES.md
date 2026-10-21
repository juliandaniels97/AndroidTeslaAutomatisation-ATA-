## ATA v1.4.3 — Kopplung: „Schlüssel schon vorhanden“ wird erkannt

### Behoben
- **Kopplung brach nach dem Auflegen der Schlüsselkarte ab**, obwohl das Auto die Meldung gezeigt hatte. Das Auto antwortete mit „Schlüssel ist bereits in der Schlüsselliste“ (Code 13). ATA hat das als Fehler gewertet. Jetzt gilt es als Erfolg: ATA prüft per sicherem Handshake, dass das Auto den Schlüssel akzeptiert, und meldet „gekoppelt“.
- Antworten des Autos zur Schlüsselliste werden im Protokoll und in der App im Klartext angezeigt, z. B. „key list 4 (key list full)“.

### Neu
- **„Verbindung testen“ zeigt jetzt auch, ob das Gerät schon gekoppelt ist**, auf dem Handy und auf der Uhr. So siehst du ohne neue Kopplung, ob Bluetooth-Aktionen funktionieren.

Alle Neuerungen aus 1.4.1 und 1.4.2 bleiben: Koppeln neben der Tesla-App, Uhr-Assistent auf dem Handy, Standortzugriff für die Bluetooth-Suche und Fehlerbericht.

BLE-Funktionen, NFC-Schlüsselkarte und Uhr-Direktsteuerung bleiben **BETA**.

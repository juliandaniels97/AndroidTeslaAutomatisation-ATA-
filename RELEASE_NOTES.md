## ATA v1.4.1 — Kopplung von Handy und Uhr: robuster, mit Assistent und Fehlerbericht

### Koppeln neben der Tesla-App
- ATA legt einen **eigenen, zusätzlichen Schlüssel** an. Der Handy-Schlüssel der Tesla-App bleibt unverändert und funktioniert weiter.
- Ist das Auto schon über die Tesla-App mit dem Handy verbunden, nutzt ATA diese bestehende Bluetooth-Verbindung mit, statt vergeblich nach dem Auto zu suchen.
- Antworten des Autos werden jetzt passend zum Auto abonniert (Indications oder Notifications). Bisher konnten sie je nach Auto ausbleiben.
- Antworten an die Tesla-App, die über dieselbe Verbindung laufen, werden sauber herausgefiltert.
- Nach dem Senden der Kopplungsanfrage wartet ATA ruhig auf die Bestätigung mit der Schlüsselkarte. Die Abfrage im Auto wird nicht mehr durch Zwischenanfragen gestört. Erst danach prüft ATA einmal, ob das Auto den Schlüssel angenommen hat.
- Schlägt die erste Bluetooth-Verbindung fehl (unter Android häufig), versucht ATA es automatisch noch einmal.

### Diagnose im Kopplungs-Assistenten
- **Verbindung testen:** Prüft ohne Koppeln, ob ATA das Auto findet und eine Antwort bekommt.
- **Technische Details:** Protokoll des letzten Versuchs mit Button „Protokoll kopieren“. Es zeigt, welche Teslas in Reichweite waren, ob die Verbindung stand und was das Auto geantwortet hat. Bitte bei Problemen mitschicken.

### Uhr koppeln – Assistent auf dem Handy
- Neu unter **Wear OS Uhr → Uhr koppeln**: Das Handy steuert die Kopplung der Uhr. Die Uhr meldet jeden Schritt und ihr Protokoll zurück.
- Live-Prüfung auf der Uhr, jeweils mit Hinweis oder Button zum Beheben: ATA installiert und erreichbar, Wear OS 4+, Fahrzeug übertragen, Bluetooth-Berechtigung, Bluetooth an.
- „Verbindung testen“ direkt von der Uhr aus, dann „Uhr koppeln“ mit Fortschritt und Schlüsselkarten-Hinweis. Nach erfolgreicher Kopplung wird die Direktsteuerung auf der Uhr automatisch eingeschaltet.
- Die Uhr übernimmt das Fahrzeug vom Handy jetzt auch, wenn ATA auf der Uhr gerade nicht geöffnet ist. Sie fragt beim Öffnen gleich nach der Bluetooth-Berechtigung.

### Fehlerbericht direkt aus der App
- **Einstellungen → Fehlerbericht senden** (auch aus beiden Kopplungs-Assistenten): sammelt App- und Geräteinfos, Einrichtung sowie die Bluetooth-Protokolle von **Handy und Uhr**.
- **Auf GitHub melden** öffnet ein vorausgefülltes Issue im Projekt. Alternativ per E-Mail oder Messenger teilen oder kopieren.
- Die VIN wird gekürzt, Schlüssel oder Tokens sind nicht enthalten.

BLE-Funktionen, NFC-Schlüsselkarte und Uhr-Direktsteuerung bleiben **BETA**.

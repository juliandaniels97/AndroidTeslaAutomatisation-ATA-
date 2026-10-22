## ATA v1.9.1 — NFC-Schlüsselkarte fertig, Furzgeräusch entfernt, neue Symbole

### NFC-Schlüsselkarte
- Die **NFC-Schlüsselkarte** (Handy und Uhr) ist **kein BETA mehr**. Sie funktioniert am Fahrzeug.
- Auch sonst gibt es in der App **keine BETA-Hinweise mehr**, z. B. bei der Navigations-App „Tesla“.
- Der Platzhalter **„Entriegeln bei Annäherung“** ist aus den Einstellungen verschwunden. Der Schalter hatte keine Funktion.

### Boombox entfernt
- **Furzgeräusch und Ortungston** sind entfernt. Tesla bietet die Boombox nur über die Cloud (Fleet API) an, nicht über Bluetooth.
- Die bisher gesendete Nachricht war im Tesla-Protokoll der Befehl für den **Gastmodus**. Das Fahrzeug hat ihn korrekt mit „insufficient privileges“ abgelehnt.
- Alle übrigen Bluetooth-Befehle wurden mit Teslas offiziellem Protokoll abgeglichen und stimmen.

### Symbole
- **Frunk** und **Kofferraum** haben jetzt **verschiedene Symbole**: Box für den Frunk, Koffer für den Kofferraum. Das gilt in App, Vollbild-Ansicht, Widgets und auf der Uhr.

### Fehlerbericht
- Das Bluetooth-Protokoll im Fehlerbericht zeigt jetzt bei jedem Befehl, **welche Aktion** gesendet wurde.

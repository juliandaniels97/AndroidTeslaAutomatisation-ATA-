## ATA v1.9.1 — Entriegeln bei Annäherung, neuer Kompass, NFC-Schlüsselkarte fertig

### Entriegeln bei Annäherung (neu)
- **Einstellungen → Entriegeln bei Annäherung:** Das Auto entriegelt einmal, wenn du mit dem Handy darauf zugehst.
- **So funktioniert es:**
  - ATA misst die Stärke des Bluetooth-Signals des Autos, auch bei ausgeschaltetem Bildschirm.
  - Hält die Tesla-App bereits die Verbindung, liest ATA die Signalstärke direkt daraus.
- **Entfernung** einstellbar von ca. 2 bis 20 m.
- **Nur einmal pro Annäherung:** erst nachdem du weg warst, nie direkt nach dem Start neben dem Auto, und nicht zweimal innerhalb von zwei Minuten.
- **Sicherheit:**
  - Bei gesperrtem Handy fragt ATA per Benachrichtigung, außer du erlaubst es ausdrücklich („Auch bei gesperrtem Handy“).
  - Es wird ausschließlich entriegelt, nie etwas anderes geöffnet.
- Eine stille Benachrichtigung zeigt, solange die Funktion aktiv ist. Sie startet nach einem Neustart automatisch wieder.

### Kompass „Auto finden“ fertig
- **Richtige Richtung:**
  - Berücksichtigt die Bildschirmdrehung und ob das Handy flach liegt oder aufrecht gehalten wird.
  - Bezieht sich auf **geografisch Nord**, mit Missweisungs-Korrektur am aktuellen Standort.
- **Kompassrose:** dreht sich mit, das Auto ist am Rand markiert, dazu ein großer Pfeil. Er dreht immer den kurzen Weg, ruhig und geglättet.
- **Kurze Vibration**, wenn das Handy genau Richtung Auto zeigt.
- **Hinweise:**
  - Kalibrieren nötig (Handy in Form einer Acht schwenken).
  - GPS-Position noch zu ungenau.
  - Ungefähre Gehzeit zum Auto.
- Die Richtungs-Animation beim Ausführen von Aktionen nutzt denselben, korrigierten Kompass.

### NFC-Schlüsselkarte
- Die **NFC-Schlüsselkarte** (Handy und Uhr) ist **kein BETA mehr**. Sie funktioniert am Fahrzeug.
- Auch sonst gibt es in der App **keine BETA-Hinweise mehr**, z. B. bei der Navigations-App „Tesla“.

### Boombox entfernt
- **Furzgeräusch und Ortungston** sind entfernt. Tesla bietet die Boombox nur über die Cloud (Fleet API) an, nicht über Bluetooth.
- Die bisher gesendete Nachricht war im Tesla-Protokoll der Befehl für den **Gastmodus**. Das Fahrzeug hat ihn korrekt mit „insufficient privileges“ abgelehnt.
- Alle übrigen Bluetooth-Befehle wurden mit Teslas offiziellem Protokoll abgeglichen und stimmen.

### Symbole
- **Frunk** und **Kofferraum** haben jetzt **verschiedene Symbole**: Box für den Frunk, Koffer für den Kofferraum. Das gilt in App, Vollbild-Ansicht, Widgets und auf der Uhr.

### Fehlerbericht
- Das Bluetooth-Protokoll im Fehlerbericht zeigt jetzt bei jedem Befehl, **welche Aktion** gesendet wurde.

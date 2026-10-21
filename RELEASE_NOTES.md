## ATA v1.4.0 — Kopplungs-Assistent, Anleitung & neues Startlogo

### Handy koppeln (Bluetooth) – repariert
- **Fehler behoben, durch den beim Koppeln „nichts passierte“:** Die Anfrage „Schlüssel hinzufügen“ wurde ohne die von Tesla erwartete Nachrichtenhülle (RoutableMessage an VCSEC) gesendet, deshalb ignorierte das Auto sie. Jetzt läuft sie wie in Teslas offizieller Implementierung (vehicle-command).
- Erfolg wird geprüft: ATA meldet „gekoppelt“ erst, wenn das Auto den Schlüssel tatsächlich akzeptiert.
- Robustere Suche: Das Auto wird direkt aus dem Scan-Ergebnis verbunden, mit einem Rückfall für Handys, deren Bluetooth-Namensfilter nicht greift.
- Klare Meldung, wenn Bluetooth aus ist.
- **Neuer Kopplungs-Assistent** (Fahrzeug-Tab → „Starten“ oder Anleitung):
  - Live-Prüfung aller Voraussetzungen, jeweils mit Button zum Beheben: BLE an, Fahrzeug mit gültiger VIN, Berechtigung, Bluetooth an.
  - Checkliste am Auto.
  - Fortschritt Schritt für Schritt: suchen → verbinden → Schlüsselkarte auflegen.
  - Gut sichtbarer Hinweis, dass die **Plastik-Schlüsselkarte** aufgelegt wird, nicht das Handy.
  - Bei Fehlern konkrete Tipps zur Lösung.

### NFC: ATA und Tesla-App nebeneinander
- Die Abfrage **„Tesla oder NFC-Schlüsselkarte“** am Kartenleser kommt nicht mehr. Die NFC-Schlüsselkarte von ATA ist jetzt nur noch aktiv, wenn sie in den Einstellungen eingeschaltet ist, und ist standardmäßig aus.
- Ist die Tesla-App installiert und die Schlüsselkarte trotzdem eingeschaltet, antwortet ATA nur, solange ATA geöffnet ist. Sonst ist wie gewohnt die Tesla-App zuständig.
- Hinweis mit direktem Link zu den NFC-Einstellungen von Android.
- NFC-Tags für Aktionen funktionieren unverändert.

### Interaktive Anleitung
- Neu unter **Einstellungen → Anleitung & Einrichtung**: ein Bereich pro Funktion mit Live-Status, nummerierten Schritten und Buttons, die direkt zur richtigen Stelle führen.
- Themen: Fahrzeug, Samsung Routinen & Automationen, Apps im Hintergrund öffnen, Widgets, NFC-Tags, Uhr, Bluetooth-Steuerung, NFC-Schlüsselkarte.

### Optik
- **Neue Startanimation** mit dem Original-Logo im Hochformat: bildschirmfüllend, zoomt beim Öffnen eine Sekunde lang leicht hinein. Der System-Startbildschirm hat dieselbe Farbe, damit kein Farbsprung entsteht.
- **Icons überall in den Auswahllisten:** Menü & Widgets (Icon-Menü, Samsung Routinen, Widget), Wear-OS-Uhr und Automation. Kritische Aktionen sind rot, die ganze Zeile ist antippbar.

BLE-Funktionen, NFC-Schlüsselkarte und Uhr-Direktsteuerung bleiben **BETA**.

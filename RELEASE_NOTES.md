## ATA v1.9.3 — Sitze mit Heizen/Lüften und Automatik, grüne Ladepunkte, neue Widgets

### Sitze: eine Kachel für Heizen und Lüften
- Die **Sitzkachel** fasst Sitzheizung und Sitzbelüftung zusammen. Der **Schalter Heizen / Lüften** oben in der Kachel wechselt zwischen beiden.
  - **Heizen:** alle Sitze einzeln, jeweils An/Aus und Stufe 1–3.
  - **Lüften:** Fahrer und Beifahrer, jeweils An/Aus und Stufe 1–3.
- Die bisherige Kachel „Sitzbelüftung“ geht in der neuen Sitzkachel auf. Gespeicherte Layouts werden automatisch umgestellt.

### Auto-Modus
- **Sitze:** Fahrer und Beifahrer haben einen **„A“-Knopf** für die Sitz-Automatik des Autos. Ist sie an, steht „Auto“ am Sitz.
  - Eine Stufe von Hand zu wählen beendet die Automatik für diesen Sitz, wie im Auto.
  - Neu als Aktion: „Sitz-Automatik Fahrer/Beifahrer“, auch für Kurzbefehle, NFC und Automationen.
- **Lenkradheizung:** Die Kachel zeigt **„Auto“**, wenn das Auto die Heizung selbst steuert, sonst die Stufe (Niedrig/Hoch).
  - Einschalten lässt sich die Lenkrad-Automatik nicht. Teslas Bluetooth-Protokoll hat dafür keinen Befehl.

### Laden
- **„Voll in …“** steht jetzt auch direkt am Fahrzeug: an der Plakette unter dem Auto (z. B. „⚡ 48 kW · voll in 1 h 20 min“) und in der Vollbildansicht.
- **Neue Ladeanimation:** grüne Punkte wandern langsam am Akkuring entlang bis zum aktuellen Ladestand.
  - Bei mehr kW etwas schneller, aber nie hektisch.
  - Dazu die pulsierende Spitze und der gestrichelte Bogen bis zum Ladelimit.
  - In Neon und Material.

### Widgets im Look der App
- **Status-Widget:**
  - Karten mit Icons wie die Kacheln der App.
  - Beim Laden mit kW und „voll in …“.
  - Schloss-Symbol und Akku-Balken.
  - Aktualisiert sich nach jedem Befehl.
- **Aktionen-Widget:** Kacheln mit Icon und Namen, 2 bis 4 pro Reihe je nach Breite. Kritische Aktionen in Rot.
- **Auto-finden-Widget:** als Karte mit Kompass-Icon.
- **Neu: Einstellungen → Design → Widgets:**
  - **Wie die App**, **Neon** oder **Material**.
  - Optional ein **durchscheinender Hintergrund**.
  - Die Widgets folgen dem hellen/dunklen Theme der App.

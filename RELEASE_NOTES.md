## ATA v1.9.2 — Multikacheln, Sitzheizung pro Sitz, Lautstärke, Laden mit kW, neue Struktur

### Neue Struktur
- **Favoriten** ganz links (statt „Aktionen“), dann **Fahrzeug** und **Einstellungen**.
- **Fahrzeug** ist beim ersten Start die Standardansicht. Danach öffnet ATA den Tab, der beim Schließen offen war.
- Die bisherigen **Aktionen** (Navigation, Tesla-App …) stehen jetzt am **Ende des Fahrzeug-Tabs**.
  - Sie sind auch ohne eingerichtetes Bluetooth aktiv.
  - Wie alle Kacheln lassen sie sich verschieben.
- **Favoriten:** Im Bearbeiten-Modus (Kachel lange drücken) mit ☆ markieren.
- **Neu: Einstellungen → Design.** Dort ist alles zum Aussehen zusammengefasst: Neon/Material, hell/dunkel, dynamische Farben, 3D-Auto, Multikacheln und Kacheln pro Reihe.

### Multikacheln (Standard: an)
- **Mehrere Aktionen in einer Kachel.** Nur die Buttons darin sind klickbar, nicht die ganze Kachel. Der Status steht direkt in der Kachel.
- **Größen:** Multikacheln belegen 2 Plätze (2×1) oder 4 Plätze (2×2) und lassen sich wie bisher verschieben.
- **Die Kacheln:**
  - **Verriegelung:** Zu-/Aufsperren mit Status.
  - **Türen** Fahrer/Beifahrer als Doppelkachel.
  - **Frunk & Kofferraum, Fenster, Ladeklappe.**
  - **Sitzheizung (2×2):** alle Sitze einzeln. Pro Sitz ein An/Aus-Button und 3 Punkte für Stufe 1–3.
  - **Sitzbelüftung** Fahrer/Beifahrer.
  - **Klima (2×2):** An/Aus, Enteisen, Temperatur Fahrer und Beifahrer getrennt.
  - **Laden (2×2):** Start/Stopp, Ladestand, kW, Restzeit, Ladelimit und Ampere.
  - **Lautstärke:** leiser, lauter und ein Schieber.
  - **Medien, Klima halten** (Aus/Halten/Hund/Camp), **Überhitzungsschutz** (Aus/Klima/nur Lüfter).
  - **Wächter, Lenkradheizung, Bioweapon** mit An/Aus-Status.
- **Schieber** (Lautstärke, Temperatur, Limit, Ampere): hoch und runter wischen. Der Befehl geht erst beim **Loslassen** raus, also nur einer statt vieler. Antippen oben/unten ändert um eine Stufe.
- **Ausschalten** unter Einstellungen → Design → Multikacheln: dann gibt es wieder eine Kachel pro Aktion, mit Schieber-Dialog für Werte.

### Neue Funktionen
- **Sitzheizung pro Sitz:** vorne links/rechts, hinten links/Mitte/rechts, jeweils Aus/Stufe 1–3.
- **Sitzbelüftung** vorne, Aus/Stufe 1–3.
- **Lautstärke:** auf einen Wert setzen (0–10, in Schritten), lauter und leiser.
- **Temperatur nur für Fahrer oder Beifahrer.** Die andere Seite behält ihre Einstellung.
- **Überhitzungsschutz nur mit Lüfter.**
- Alle neuen Aktionen gibt es auch als Kurzbefehl, für Widgets, NFC und Automationen.

### Status
- Die Kacheln zeigen den echten Zustand des Autos:
  - Sitzstufen, Lenkradheizung, Enteisen, Klima halten
  - Türen, Frunk/Kofferraum, Fenster, Ladeklappe
  - Lautstärke und der laufende Titel
- Nach einem Befehl zeigen die Kacheln den neuen Zustand sofort an.
- **Laden:** „lädt mit X kW“, dazu Restzeit bis voll, Ladelimit und Ampere.

### Akkuring beim Laden
- Lichttropfen fließen den Ring entlang, **schneller bei mehr kW**.
- Die Spitze pulsiert, und ein gestrichelter Bogen zeigt den Weg bis zum Ladelimit.
- Am Auto erscheint eine Plakette „⚡ X kW“.
- Funktioniert in Neon und Material, hell und dunkel.

### 3D-Ansicht
- **Neue Punkte** zum Antippen:
  - **Sitze:** Sitzheizung und Belüftung direkt einstellen.
  - **Lenkrad:** Lenkradheizung.
- **Klima** und **Ladeklappe** öffnen jetzt zusätzlich ihre Multikachel. Die **Türen** funktionieren wie bisher.
- Die Sitze im Auto glühen warm, solange eine Sitzheizung läuft.

### Responsiv
- **Kacheln pro Reihe:** einstellbar 2 bis 6 oder Auto (Handy 3, Tablet mehr). Die Namen bleiben bei jeder Größe sichtbar.
- **Tablet/Querformat:** das Auto links, die Kacheln rechts daneben.
- Alle Kacheln haben Icons. Das Lenkrad hat ein eigenes Icon.

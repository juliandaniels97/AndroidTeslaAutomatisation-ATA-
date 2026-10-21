## ATA v1.4.5 — Fahrzeugstatus per Bluetooth, Auto-finden-Kompass, feste Werte für Routinen

### Behoben
- **Fahrzeugstatus (Akku, Reichweite, Temperaturen) über Bluetooth:** Das Auto lehnte die Abfrage mit „vehicle fault 25“ ab (Antwort zu groß für eine Bluetooth-Nachricht). ATA fragt Akku, Klima und Türen jetzt einzeln ab, wie Teslas `tesla-control`, und fügt die Antworten zusammen. Dafür musst du nicht im Auto sitzen: Bluetooth-Reichweite genügt.
- Fehler des Autos ab Nummer 20 werden im Protokoll im Klartext angezeigt.

### Neu
- **Auto finden:** Live-Kompass mit Pfeil, Entfernung und Himmelsrichtung zum Auto (Fahrzeug-Tab → Kompass-Symbol).
  - Die Autoposition speichert ATA automatisch bei jedem Bluetooth-Kontakt mit dem Auto (dann ist das Handy wenige Meter entfernt). Alternativ geht es manuell über „Hier geparkt“.
  - Dazu „In Karten zeigen“.
- **Widget „ATA Auto finden“:** zeigt Entfernung und Richtung (Pfeil nach Norden ausgerichtet). Antippen öffnet den Live-Kompass. Widgets können dem Kompass des Handys nicht live folgen, deshalb ist der drehende Pfeil in der App.
- **Feste Werte für Shortcuts** (Einstellungen → Menü & Widgets → Samsung Routinen → „Festen Wert hinzufügen“), z. B. „Temperatur einstellen: 21 °C“ oder „Ladelimit: 80 %“.
  - Sie werden als App-Verknüpfungen veröffentlicht. Samsung Routinen und das Icon-Menü führen sie ohne Nachfrage aus.

### Neue Aktionen (BETA, per Bluetooth)
- **Fahrertür öffnen** und **Beifahrertür öffnen**: öffnet die jeweilige Tür (je nach Modell muss das Auto vorher entriegelt sein). Gilt als kritische Aktion mit Bestätigung. Ob dein Modell das Öffnen per Befehl unterstützt, zeigt der Test am Auto.
- **Kofferraum schließen** (bei elektrischer Heckklappe).
- **Fahren ohne Schlüssel** (ca. 2 Minuten, kritisch mit Bestätigung).
- **Musik:** Play/Pause, nächster und vorheriger Titel.
- **Klima:** Klima-Modus aus, Enteisen aus, Lenkradheizung aus.
- **Bioschutz-Modus** an/aus, **Überhitzungsschutz** an/aus.
- Alle neuen Aktionen gibt es in der App, im Widget, auf der Uhr, in Routinen und auf NFC-Tags.

### Optik
- **Neue Fahrzeuggrafik** im Fahrzeug-Tab: das Auto von oben mit Rädern, Spiegeln, Glasdach, Scheinwerfern und Rückleuchten (statt der bisherigen schlichten Form).
- **Akku-Ring:** farbig nach Ladestand (blau, ab 30 % gelb, ab 15 % rot, beim Laden grün mit Lauflicht). Gestrichelt, solange noch kein Wert bekannt ist.
- **Zustände direkt am Auto:**
  - Ladeanschluss leuchtet beim Laden.
  - Glasdach färbt sich bei aktiver Klimatisierung.
  - Darunter Symbole für verriegelt/offen, Laden, Klima und Wächter-Modus.

BLE-Funktionen, NFC-Schlüsselkarte und Uhr-Direktsteuerung bleiben **BETA**.

## ATA v1.9.5 — Laden live hochgerechnet, neuer Ladekreis, Demo-Modus, Statusmodi, Uhr im Kachel-Look, Spenden

Enthält alles aus 1.9.4 (wurde nicht separat veröffentlicht) und zusätzlich:

### ATA unterstützen
- **Einstellungen → ATA unterstützen** (bei „Über“): ein Dankeschön und ein **„Mit PayPal spenden“**-Button mit PayPal-Logo. Ganz freiwillig, ATA bleibt kostenlos.

### Neuer GitHub-Name
- Links in der App (Fehlerbericht, Uhr-Download) und in der README zeigen auf **github.com/juliandigitalproducts**.

### Laden
- **Live hochgerechnet:** ATA merkt sich den letzten Ladestatus und rechnet ihn weiter. Aus „voll in 6 h“ von vor 3 Stunden wird „voll in 3 h“.
  - Akkustand, Reichweite und die nachgeladene Energie steigen dazu ungefähr mit, berechnet aus Ladeleistung und Restladedauer.
  - Das gilt in der App, im Widget und auf der Uhr.
- **Nachgeladene Energie** mit einer Nachkommastelle, z. B. „+20,4 kWh“:
  - in der Lade-Kachel
  - unter den Werten am Fahrzeug, zusammen mit dem Ladelimit
  - im Widget und auf der Uhr
- **Neuer Ladekreis:**
  - Grüne Punkte kommen aus dem leeren Teil des Rings und wandern langsam in den geladenen Bereich. Bei 20 % laufen sie also aus den fehlenden 80 % in die 20 %.
  - Ein **Strich auf dem Ring** zeigt das Ladelimit, immer und nicht nur beim Laden.

### Fahrzeugstatus: drei Modi
Zu finden unter Einstellungen → Verhalten, Status, Bluetooth & NFC.
1. **Akkuschutz (Standard):** nur manuell. Im Fahrzeug-Tab **nach unten ziehen** oder ↻ tippen. Keine Hintergrundabfragen.
2. **Intelligent:** beim App-Start, danach regelmäßig, solange das Auto am Strom hängt und das Handy benutzt wird. Der Abstand ist per Schieber einstellbar, Standard 30 Min.
3. **Intervall:** fester Abstand, **auch im Hintergrund**. Der Schieber reicht von 5 Min bis 3 Std in 120 Stufen, Standard 30 Min.
- Automatische Abfragen wecken das Auto nie. Wer vorher ein Intervall eingestellt hatte, behält es.

### Demo-Modus (Einstellungen → Demo-Modus, Standard: aus)
- Ein erfundenes, verbundenes Model Y zum Ausprobieren ohne Auto. Alle Kacheln reagieren wie beim echten Auto, es wird nichts gesendet.
- Startet immer mit demselben Stand: **50 %, lädt mit 22 kW bis 80 %, noch 1 h, 20 kWh nachgeladen**. Auch nach Aus- und wieder Einschalten.

### Bedienung und Struktur
- **Nach unten ziehen** im Fahrzeug-Tab fragt den Status ab.
- **Weniger Leerraum oben:** Die Kopfzeilen (ATA, Fahrzeug, Einstellungen …) sitzen jetzt direkt unter der Statusleiste.
- **Einstellungen → „Verhalten, Status, Bluetooth & NFC“** (neue Unterseite) fasst zusammen: Verhalten, Fahrzeugstatus, Bluetooth & NFC und Entriegeln bei Annäherung.
- **Eigene Icons** für Fenster lüften/schließen, Fahrer-/Beifahrertür und Ladeklappe öffnen/schließen.

### Uhr (Wear OS)
- Die Uhr bekommt den **kompletten Status** vom Handy: Ladeleistung, Restzeit, nachgeladene Energie und Ladelimit.
- **Kacheln im Karten-Look** der App. Eine Kachel leuchtet, wenn das Auto in ihrem Zustand ist, z. B. „Verriegeln“ bei verriegeltem Auto.
- **Neue Statuskarte** (nach links wischen):
  - Akkuring mit grünen Ladepunkten und Limit-Strich
  - Akkustand und Reichweite
  - beim Laden kW, Restzeit und kWh
- Der Rand zeigt beim Laden Leistung und Restzeit.

### Lenkradheizung
- Teslas Bluetooth-Protokoll kann die Lenkradheizung nur **ein- und ausschalten**. Stufen oder Auto lassen sich nicht setzen. Ein steuert das Auto die Heizung selbst, zeigt die Kachel „Auto“.

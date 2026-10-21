## ATA v1.3.0 — sichere Automationen

Samsung Routines, Shortcuts, Widgets und Tasker funktionieren weiter, jetzt mit klaren Regeln, wer was auslösen darf.

### Sicherheit
- **Jeder Befehl wird geprüft** (`ActionPolicy`): ATA-Shortcuts, Widgets, Routinen und Tasker mit Token sind vertrauenswürdig. Fremde Apps, Web-Links und fremde NFC-Tags dürfen nur Basis-Aktionen und das, was du unter *Einstellungen → Automation* freigibst.
- **Kritische Aktionen** (Entriegeln, Kofferraum, Frunk, Ladeklappe öffnen, Fenster lüften, Wächter aus) fragen bei Automationen per Benachrichtigung nach (optional mit Biometrie). Pro Aktion kannst du „ohne Nachfrage“ erlauben.
- Kein Web-Link-Einstieg mehr (`ata://` aus dem Browser löst nichts aus).
- NFC-Tags haben einen eigenen, widerrufbaren Token; kritische Aktionen per Tag fragen immer nach.
- NFC-Schlüsselkarte antwortet nur bei entsperrtem Telefon (Schutz gegen Relay-Angriffe). Die Challenge-Antwort ist korrigiert.
- Uhr: kritische Aktionen brauchen einen zweiten Tipp und laufen nie von einer gesperrten Uhr.
- Status-Broadcast (verriegelt ja/nein) nur noch per Opt-in.
- Releases werden nie mehr mit einem Debug-Schlüssel signiert; der öffentliche Debug-Keystore wurde entfernt.

### Routinen & Automation
- Unkritische BLE-Befehle laufen aus Routinen **auch bei gesperrtem Telefon** (abschaltbar).
- Ergebnis als Benachrichtigung („Klima an ✓“ oder Grund des Fehlers).
- Befehle blockieren den Bildschirm nicht mehr; mehrere Routine-Schritte hintereinander gehen nicht mehr verloren.
- Verzögerungen (`delay_ms`) laufen im Hintergrund, Basis-Aktionen danach als „Tippen zum Öffnen“.
- Neue Shortcuts **„ATA aktiv“ / „ATA passiv“** und ein **„ATA-Shortcut“-Picker** (Create-Shortcut) für jede Aktion samt Wert.
- *Einstellungen → Automation*: Token kopieren/erneuern (Shortcuts und Widgets werden automatisch aktualisiert), NFC-Tags widerrufen.

### BLE
- Antworten des Autos werden wirklich ausgewertet (Fehler, „beschäftigt“, entschlüsselte Antwort, Replay-Schutz); „OK“ heißt jetzt OK.
- Wecken nur, wenn das Auto schläft; Hintergrund-Statusabfragen **wecken das Auto nicht** mehr.
- Klare Meldungen: Auto nicht gefunden, Berechtigung fehlt, Schlüssel nicht gekoppelt, Gerät nicht unterstützt.
- Koppeln wartet auf die Bestätigung im Auto und zeigt das Ergebnis (Rolle: Fahrer).
- Reichweite wie in der Tesla-App, Wächter-Status, „verriegelt“ ist bei unbekanntem Zustand nicht mehr grün.
- Unit-Tests mit Teslas offiziellen Protokoll-Testvektoren laufen in jeder CI.

### Hinweise
- BLE-Funktionen, NFC-Schlüsselkarte und Uhr-Direktsteuerung sind weiterhin **BETA** und noch nicht an einem echten Auto verifiziert.
- Für Ergebnisse und Bestätigungen bitte Benachrichtigungen für ATA erlauben (*Einstellungen → Automation*).

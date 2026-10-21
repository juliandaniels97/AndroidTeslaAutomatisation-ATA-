## ATA v1.3.4 — Uhr öffnet Apps auf dem Handy

- **Behoben:** Ein Tipp auf der Uhr auf *Tesla-App öffnen*, *Nach Hause navigieren* usw. hat auf dem Handy die App nicht geöffnet, die Uhr meldete aber trotzdem „✓“. Android verbietet Apps im Hintergrund, andere Apps zu öffnen. ATA hat deshalb nur eine Benachrichtigung gezeigt, ohne Benachrichtigungsrecht passierte gar nichts.
- **Neu:** Unter *Einstellungen → Wear OS Uhr* (und *Automation*) kannst du **„Über anderen Apps einblenden“** erlauben. Dann öffnen Uhr-Tipps und Automationen die Tesla-App bzw. Navigation **direkt auf dem Handy**. Ohne diese Berechtigung erscheint weiterhin eine Benachrichtigung zum Antippen, und die Uhr sagt das jetzt auch ehrlich.
- **Behoben:** Ein Absturz, wenn eine Basis-Aktion im Hintergrund keine passende App fand (Toast aus einem Hintergrund-Thread).
- **Uhr-Direktsteuerung:** Findet die Uhr das Auto nicht oder ist sie nicht gekoppelt, übernimmt automatisch das Handy.

BLE-Funktionen, NFC-Schlüsselkarte und Uhr-Direktsteuerung bleiben **BETA**.

## ATA v1.3.2 — Absturz beim Start behoben

- **Behoben:** v1.3.1 stürzte direkt beim Start ab (alle Android-Versionen). Ursache war der Code-Optimierer (R8) des neuen Android Gradle Plugin 9: Er hat einen Konstruktor entfernt, den WorkManager beim App-Start per Reflection braucht (`WorkDatabase_Impl`). Die nötigen Keep-Regeln sind ergänzt.
- **Neu im Build:** Jedes Release wird vor der Veröffentlichung automatisch auf einem Android-16-Emulator (Telefon) und einem Wear-OS-Emulator (Uhr) gestartet. Stürzt die App ab, wird nichts veröffentlicht.
- Alle Neuerungen aus v1.3.1 (targetSdk 37, mehr Aktionen für Samsung Routinen) sind enthalten.

BLE-Funktionen, NFC-Schlüsselkarte und Uhr-Direktsteuerung bleiben **BETA**.

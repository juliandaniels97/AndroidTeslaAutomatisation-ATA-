# ATA — Android Tesla Automatisation

ATA turns your phone and watch into a fast remote and automation hub for your Tesla.
It is built for **shortcuts, Samsung routines, home‑screen widgets and Wear OS**, so a single
tap — or an automation — triggers exactly the action you want.

> **Not affiliated with Tesla, Inc.** "Tesla" is a trademark of Tesla, Inc.
> Use at your own risk. See [Legal](#legal).
>
> **Commercial use requires prior written permission** from the author — see [Legal](#legal).

## Downloads

Built automatically for **64‑bit ARM (arm64‑v8a)** from `main`:

- 📱 Phone APK: **[ATA-phone-v1.2.2-arm64-v8a.apk](https://github.com/juliandaniels97/AndroidTeslaAutomatisation-ATA-/releases/download/apk-main/ATA-phone-v1.2.2-arm64-v8a.apk)**
- ⌚ Wear OS APK: **[ATA-wear-v1.2.2-arm64-v8a.apk](https://github.com/juliandaniels97/AndroidTeslaAutomatisation-ATA-/releases/download/apk-main/ATA-wear-v1.2.2-arm64-v8a.apk)**

All builds are signed with the same key, so new versions install as an update over the old one.

## Requirements

| | Phone | Watch |
|---|---|---|
| OS | Android 12+ (minSdk 31) | Wear OS 3+ (Galaxy Watch 4 and newer, minSdk 30) |
| CPU | arm64‑v8a | arm64‑v8a |
| Needs the phone? | — | Relays to the phone by default, or **direct control** over BLE once the watch is paired |

## Two feature sets, one app

ATA groups everything into the **Basic features** (navigation & app links) and the **BLE features**
(direct Bluetooth control). Both work **without a Tesla account in ATA, without the Fleet API and
without any cloud connection** — which is also gentle on the car's battery, because nothing keeps
polling the vehicle over the internet.

### Basic features — routing & app links

- **Open the Tesla app** or jump toward its sections (Charging, Climate, Controls, Location).
- **Navigate**: send *Home*, *Work* or any address to Google Maps, Waze or the system handler.
- **Share a destination** via the Android share sheet.

> Deep links into specific Tesla‑app sections (Charging, Climate, …) are **not** officially
> supported by Tesla — only opening the app is guaranteed. ATA tries several candidate links and, if
> none is handled by the installed Tesla‑app version, simply opens the app on its main screen.

### BLE features — direct control over Bluetooth (experimental / BETA)

The BLE features talk **directly to the car over Bluetooth Low Energy** using Tesla's publicly
documented vehicle‑command protocol. The cryptographic layer (ECDH key agreement, HMAC session
verification, AES‑GCM command signing, metadata serialization) is implemented from that spec and
**verified byte‑for‑byte against Tesla's published test vectors**.

They are **off by default**. Enable them under **Settings → Experimental → Direct BLE control
(BETA)**. Then:

1. Add your vehicle (VIN) under **Settings → Vehicles**.
2. On the **Vehicle** tab, tap **Pair this device**. On the first connection the car asks to trust
   this device's key — compare the fingerprint shown in ATA, tap your key card on the console and
   confirm in the car. ATA's key is an EC‑P‑256 key kept in the Android Keystore and never leaves
   the phone.
3. The BLE buttons (lock/unlock, climate, charging, trunk/frunk, windows, sentry, climate
   keeper/dog/camp, honk, flash, charge limit, charging amps, set temperature, defrost, wake) then
   send commands over BLE.

#### Live status

With the BLE features enabled, ATA can read the car's **battery, range, inside/outside temperature,
charging and lock state** over BLE. Pull the latest reading any time with the **refresh** button on
the Vehicle tab, or let ATA poll automatically on the interval you choose under **Settings → Status
interval** (15 min minimum, battery‑friendly; `Off` disables it). Polling only runs while the BLE
features are on, the app is **active**, a vehicle is selected and Bluetooth permission is granted.
The last snapshot is cached on device and shown on the Vehicle tab and the status widget, with a
relative "updated X ago" timestamp.

## Security

- **External triggers** (Samsung Routines, Tasker, other apps) can run **every** action, basic and
  BLE alike, and read the car status — so you can automate anything. You decide what is exposed by
  which actions you enable and add to ATA; the BLE features as a whole stay behind the experimental
  flag, and the app must be **active**.
- BLE commands additionally require an **unlocked phone**, and — for the critical actions that
  physically open the car (**unlock, charge port, trunk, frunk**) — an in‑app **confirmation** and,
  if enabled, **biometric/device‑credential** authentication. Those four are the only actions drawn
  in red; everything else is blue.
- ATA's own shortcuts/widgets/watch and NFC tags carry a per‑install token so they are recognised as
  trusted, but triggering is no longer restricted to them.

## Automation

### Home‑screen shortcuts
Any action — Basic **and** BLE — can be pinned to the launcher (**Actions** tab → *Add to home
screen*). A pinned BLE shortcut asks for Bluetooth permission on first use and still requires the
BLE features to be enabled and the phone unlocked.

### App‑icon menu
Long‑pressing the ATA icon shows up to **4 configurable actions** (Settings → *Menu & widgets*),
which can include BLE actions as well.

### Samsung Routines & Tasker
- **Activity / shortcut** (recommended for Samsung Routines): use the home‑screen shortcuts above.
- **Broadcast** for Tasker and power users:

  ```
  Action:  org.tfv.ata.action.RUN
  Extras:  action_id   = e.g. "navigate_home" | "open_charging" | "open_tesla_app"
           value       = optional (e.g. an address for "navigate_address")
           delay_ms    = optional delay before running
  ```
  (External broadcasts run the Basic features only.)

- **Active / passive switching**:

  ```
  Action:  org.tfv.ata.action.SET_ACTIVE
  Extra:   active = true | false
  ```

- **Status as a trigger** (react to the car): whenever ATA polls the car over BLE it fires a
  broadcast you can catch in Tasker/automation (and, via Tasker, in Samsung Routines):

  ```
  Action:  org.tfv.ata.action.STATUS
  Extras:  battery_percent (int), range_km (int), charging (bool), locked (bool),
           climate_on (bool), inside_temp_c (float), outside_temp_c (float), vehicle_id (string)
  ```
  Enable BLE features and a **Status interval** (Settings) so the broadcast fires automatically; the
  manual refresh on the Vehicle tab fires it too.

A BLE action triggered from the watch or from ATA's own shortcuts runs in a short foreground service
so it also works while the phone is in your pocket.

### Widgets
Two Glance widgets: a compact **status** widget and an **actions** widget (actions chosen in
Settings → *Menu & widgets*, Basic and BLE).

## Wear OS

The watch app (separate APK) shows your **live status** (battery, range, lock) and a list of action
chips. Includes a **tile** and a tappable **complication**.

- **Relay mode (default):** tapping a chip sends the action to the phone, which performs it (Basic
  *and* BLE).
- **Direct control (BLE):** the watch can also talk to the car itself, without the phone in range.
  The phone's key is hardware‑backed and cannot be copied, so the watch uses **its own** key and is
  paired with the car once:
  1. On the phone, select the vehicle (Actions tab) — its VIN syncs to the watch automatically, so
     you never type a VIN on the watch.
  2. On the watch, enable **Direct control** (grants Bluetooth permission) and tap **Pair this
     watch**; confirm the new key in the car.
  3. BLE chips then run straight from the watch; Basic actions still relay to the phone.
- **Configure from the phone:** choose which actions appear on the watch under
  **Settings → Wear OS watch**; the list, the selected vehicle and the status sync automatically over
  the Wear Data Layer.

## NFC

Two independent NFC features — you can use either, both, or neither.

### NFC tags (tap‑to‑run)

**Settings → NFC tags** writes an action to an NFC tag or sticker (`ata://run?action=…`). Tapping the
phone on that tag runs the action — e.g. stick an **Unlock** tag near the car and tap the phone to
unlock over BLE. Function‑A tags carry the per‑install token, so only your phone runs them.

### Tesla key card emulation — phone *as* the card (BETA)

**Settings → Experimental → NFC key card** turns the phone itself into a Tesla **key card** via host
card emulation (HCE). The car's center‑console reader talks to the phone over NFC exactly as it would
to a plastic card, so this works even with the screen off and no app open — and alongside the BLE
phone key, not instead of it.

It reuses the **same hardware‑backed key** as the BLE phone key (one enrolment covers both): the
private key is generated in the Android Keystore and never leaves the secure element. The handshake is
standard Tesla VCSEC — an ECDH key agreement on the shared P‑256 key, then an `AES‑128` challenge
response — so no secret is ever stored in the app or transmitted.

**Enrol it like any card:** in the car, **Controls → Locks → Add key**, tap an already‑paired key or
phone to authorise, then hold this phone flat and still against the card reader on the center console
until the car confirms. After that, tap to lock/unlock like a card.

- **Only when phone unlocked** (optional, on by default): require the phone to be unlocked before it
  will answer the reader, so a lost locked phone can't open the car.
- Needs a phone with NFC + HCE (almost all modern Androids). Enabling the feature registers ATA as the
  card service; make sure no other wallet app is set to handle the Tesla card AID.

## Settings

- **Theme** (System/Light/Dark) + Material You dynamic color; **language** follows the system
  (English & German).
- **Navigation app**: Google Maps, Waze or system.
- **Confirm critical actions**, **biometric lock** (both apply to the BLE features).
- **Point animation to car** (optional): uses your location (permission prompted) to aim the neon
  activation animation toward the car's last‑known spot — the spot is remembered whenever a BLE
  command succeeds (i.e. when you were next to the car).
- **Active toggle** for routines; **multi‑vehicle** (name, VIN, home & work addresses).
- **Experimental → Direct BLE control (BETA)** and **NFC key card** (phone as a Tesla key card, BETA).
- **Status interval**: how often ATA polls the car for a battery/range/climate snapshot over BLE
  (Off, or 15 min and up). Requires the BLE features.
- **Proximity unlock (BETA)** is reserved for an upcoming release (see known limitations) and currently has no
  effect.

## Known limitations

- The BLE commands, the NFC key card and direct watch control are built to Tesla's published
  protocol but **not yet verified on a real car**, so they may not work until tested on the vehicle.
- **Proximity unlock** is not implemented — the setting is a placeholder and has no effect.
- **Seat heater** has no BLE command mapped yet; the tile reports it as unavailable.
- Tesla‑app section deep links are best‑effort and may just open the app on its main screen.

## Build it yourself

Requires JDK 17 and the Android SDK.

```bash
git clone https://github.com/juliandaniels97/AndroidTeslaAutomatisation-ATA-.git
cd AndroidTeslaAutomatisation-ATA-
./gradlew :app:assembleRelease :wear:assembleRelease
```

Outputs: `app/build/outputs/apk/release/` and `wear/build/outputs/apk/release/`.

| Module | Purpose |
|---|---|
| `:core` | Shared models, action catalog, Basic deep links, VIN helpers, BLE protocol/crypto |
| `:app` | Phone app (Compose UI, shortcuts, widgets, automation, Wear bridge) |
| `:wear` | Wear OS app (Compose, tile, complication) |

## Release signing

Release APKs are signed with a real key when four repository secrets are present (otherwise CI falls
back to the shared debug key so forks still build). With the same key across builds, new versions
install as an update over the old one. Set the secrets under
**GitHub → Settings → Secrets and variables → Actions**:

| Secret | Value |
|---|---|
| `ATA_KEYSTORE_BASE64` | your keystore (`.jks`) as base64: `base64 -w0 release-keystore.jks` |
| `ATA_KEYSTORE_PASSWORD` | keystore password |
| `ATA_KEY_ALIAS` | key alias |
| `ATA_KEY_PASSWORD` | key password |

Create a keystore once with:

```bash
keytool -genkey -v -keystore release-keystore.jks -keyalg RSA -keysize 2048 \
  -validity 10000 -alias ata
```

Keep the `.jks` file and its passwords safe and **out of git** — losing them means you can't ship
signed updates that install over an existing install.

## Legal

- Licensed under the **ATA License (Source‑Available, Non‑Commercial)** — see [LICENSE](LICENSE).
  You may use, modify and share ATA for **personal, educational and other non‑commercial purposes**.
- **Commercial use requires prior written permission** from the author
  ([github.com/juliandaniels97](https://github.com/juliandaniels97)). This includes selling it,
  bundling it into a paid or ad‑supported product or service, or otherwise using it to generate
  revenue.
- See [NOTICE](NOTICE) for third‑party attributions (Tesla vehicle‑command protocol, Apache‑2.0).
- ATA is **not affiliated with, endorsed by, or sponsored by Tesla, Inc.** "Tesla" and related
  marks are trademarks of Tesla, Inc., used only to describe interoperability.
- ATA stores your data **only on your device** and needs no account or internet of its own for its
  core features. You are responsible for complying with local laws when automating your vehicle.

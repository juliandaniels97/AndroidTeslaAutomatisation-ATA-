# ATA — Android Tesla Automatisation

ATA turns your phone and watch into a fast remote and automation hub for your Tesla.
It is built for **shortcuts, Samsung routines, home‑screen widgets and Wear OS**, so a single
tap — or an automation — triggers exactly the action you want.

> **Not affiliated with Tesla, Inc.** "Tesla" is a trademark of Tesla, Inc.
> Use at your own risk. See [Legal](#legal).
>
> **Commercial use requires prior written permission** from the author — see [Legal](#legal).

<img width="100" alt="Neon-ATA-Logo mit Flügelblitz" src="https://github.com/user-attachments/assets/bb4cdebf-f178-4cd7-a835-c01ea2eead8a" />


## Support the project

ATA is free for personal use. If it saves you a few taps every day, you can support the development.
A donation is purely voluntary and unlocks nothing: ATA stays free with all features, without ads or in‑app purchases.

<a href="https://www.paypal.com/donate/?hosted_button_id=YHEH2ZKAV4E9Y"><img alt="Donate with PayPal" src="https://img.shields.io/badge/Donate-PayPal-00457C?style=for-the-badge&logo=paypal&logoColor=white" /></a>

<a href="https://www.paypal.com/donate/?hosted_button_id=YHEH2ZKAV4E9Y"><img width="160" alt="QR code: donate with PayPal" src="docs/donate-paypal-qr.png" /></a>

## Downloads

Latest release **v1.9.7** — phone APK for **64‑bit ARM (arm64‑v8a)**, watch APK for **64‑ and 32‑bit ARM (arm64‑v8a + armeabi‑v7a)**:

- 📱 Phone APK: **[ATA-phone-v1.9.7-arm64-v8a.apk](https://github.com/juliandigitalproducts/AndroidTeslaAutomatisation-ATA-/releases/download/v1.9.7/ATA-phone-v1.9.7-arm64-v8a.apk)**
- ⌚ Wear OS APK: **[ATA-wear-v1.9.7.apk](https://github.com/juliandigitalproducts/AndroidTeslaAutomatisation-ATA-/releases/download/v1.9.7/ATA-wear-v1.9.7.apk)**

The newest version is always at [Releases → latest](https://github.com/juliandigitalproducts/AndroidTeslaAutomatisation-ATA-/releases/latest).
All releases are listed under [Releases](https://github.com/juliandigitalproducts/AndroidTeslaAutomatisation-ATA-/releases).

**Google Play:** ATA is coming to Google Play as **ATA - Car Automatisation** (package
`de.juliandigitalproducts.ata`, phone and Wear OS). The Play version is a separate app next to the
APK version (`org.tfv.ata`): it doesn't update an APK install, and the watch app has to come from the
same source as the phone app. Each release also carries the
signed App Bundles for Play (`ATA-phone-vX.Y.Z.aab`, `ATA-wear-vX.Y.Z.aab`).
All release builds are signed with the same release key, so new versions install as an update over
the old one. (Very early builds were signed with a public debug key; if ATA does not update over such a build,
uninstall it once and pair the phone with the car again.)

## Play Store release

### Als Tester mitmachen (Google Play, interner Test)
1. Schick eine E-Mail an [juliandigitalproducts+beta@gmail.com](mailto:juliandigitalproducts+beta@gmail.com) mit der Adresse deines Google-Kontos, das du im Play Store nutzt. Julian trägt dich in die Testerliste ein.
2. Wenn du eingetragen bist, öffne mit genau diesem Google-Konto den Teilnahme-Link und tippe auf „Tester werden“:
   - Handy: https://play.google.com/apps/internaltest/4700908852362636296
   - Uhr (Wear OS, optional): https://play.google.com/apps/internaltest/4699692142762427331
3. Installiere die App über den Link auf der Teilnahmeseite oder über https://play.google.com/store/apps/details?id=de.juliandigitalproducts.ata. Im Play Store muss dasselbe Konto aktiv sein. Bis die App auftaucht, können ein paar Minuten vergehen.
4. Die Uhr-App installierst du über den Play Store auf der Uhr oder über „Auf Uhr installieren“ auf dem Handy.

Hinweis: Solange Google die App noch nicht geprüft hat, erscheint sie im Play Store unter dem vorläufigen Namen „de.juliandigitalproducts.ata (unreviewed)“, ohne Icon und mit fast leerer Store-Seite. Das ist normal.
Die Play-Version ist eine eigene App neben der GitHub-Version. Einstellungen und FIN kannst du ab 1.9.7 per Export und Import übernehmen. Die Fahrzeugschlüssel musst du einmal neu anlernen.

### Join as a tester (Google Play, internal test)
1. Email [juliandigitalproducts+beta@gmail.com](mailto:juliandigitalproducts+beta@gmail.com) with the address of the Google account you use in the Play Store. Julian will add you to the tester list.
2. Once you're added, open the opt-in link while signed in with that exact Google account and tap "Become a tester":
   - Phone: https://play.google.com/apps/internaltest/4700908852362636296
   - Watch (Wear OS, optional): https://play.google.com/apps/internaltest/4699692142762427331
3. Install the app from the opt-in page or via https://play.google.com/store/apps/details?id=de.juliandigitalproducts.ata. The same account must be active in the Play Store. It can take a few minutes to show up.
4. Install the watch app from the Play Store on your watch or via "Install on watch" on your phone.

Note: Until Google has reviewed the app, it appears in the Play Store under the temporary name "de.juliandigitalproducts.ata (unreviewed)", without an icon and with an almost empty store page. That's expected.
The Play version is a separate app next to the GitHub version. From 1.9.7 you can carry over settings and VIN via export/import. Vehicle keys need to be paired again once.

## Screenshots

**Neon — dark mode**

<img width="1832" alt="ATA neon design in dark mode: favourites, vehicle with multi-tiles, seat heating and charging tiles, full-screen car, design settings" src="docs/screenshots/ata-dark.png" />

**Neon — light mode**

<img width="1832" alt="ATA neon design in light mode: favourites, vehicle with multi-tiles, seat heating and charging tiles, full-screen car, design settings" src="docs/screenshots/ata-light.png" />

**Material — dark mode**

<img width="1832" alt="ATA Material design in dark mode: favourites, painted car, multi-tiles, full-screen car with animation, settings" src="docs/screenshots/ata-material-dark.png" />

**Material — light mode**

<img width="1832" alt="ATA Material design in light mode: favourites, painted car, multi-tiles, full-screen car with animation, settings" src="docs/screenshots/ata-material-light.png" />

**Tablet — car on the left, tiles on the right**

<img width="1832" alt="ATA on a tablet in landscape: the car on the left, the multi-tiles on the right, dark and light" src="docs/screenshots/ata-tablet.png" />

**Wear OS**

<img width="1832" alt="ATA on Wear OS: tile grid on round and square watches, action list" src="docs/screenshots/ata-wear.png" />

## Requirements

| | Phone | Watch |
|---|---|---|
| OS | Android 12+ (minSdk 31, targets Android 17 / API 37) | Wear OS 3+ (Galaxy Watch 4 and newer, minSdk 30, targets API 37) |
| CPU | arm64‑v8a | arm64‑v8a or armeabi‑v7a (32‑bit, e.g. Galaxy Watch 6) — one APK for both |
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

### BLE features — direct control over Bluetooth

The BLE features talk **directly to the car over Bluetooth Low Energy** using Tesla's publicly
documented vehicle‑command protocol. The cryptographic layer (ECDH key agreement, HMAC session
verification, AES‑GCM command signing, metadata serialization) is implemented from that spec and
**verified byte‑for‑byte against Tesla's published test vectors**.

They are **off by default**. Enable them under **Settings → Behavior, status, Bluetooth & NFC → Direct BLE
control**. To look around first, turn on **Settings → Demo mode** (see below).
Some functions are **vehicle‑specific** (e.g. opening single doors or the power liftgate)
and don't work on every model. Then:

1. Add your vehicle (VIN) under **Settings → Vehicles**.
2. On the **Vehicle** tab, tap **Pair this device**. On the first connection the car asks to trust
   this device's key — compare the fingerprint shown in ATA, tap your key card on the console and
   confirm in the car. ATA's key is an EC‑P‑256 key kept in the Android Keystore and never leaves
   the phone.
3. The BLE buttons then send commands over BLE:
   - lock/unlock, open driver or passenger door, trunk/frunk, windows
   - climate: on/off, temperature for both sides or for driver / passenger only, keeper/dog/camp,
     defrost, steering‑wheel heater, bioweapon defence, cabin overheat protection (on, fan only, off)
   - **seat heaters per seat** (front left/right, rear left/centre/right; off or level 1–3) and
     **ventilated front seats** (off or level 1–3), **automatic seat climate** for the front seats
   - charging: start/stop, limit, amps, charge port
   - media: play/pause, next, previous, **volume** (set 0–10 in steps, up, down)
   - sentry, honk, flash, wake, remote drive

#### Live status

With the BLE features enabled, ATA reads over BLE:

- **battery, range, inside/outside temperature, lock state**
- **charging:** whether the car is charging and **with how many kW**, plus time to full, charge limit and amps
- **seat heater levels**, steering‑wheel heater, defrost and climate‑keeper mode
- **doors, frunk/trunk, windows, charge port**
- **volume** and the current title

The tiles show this state, and after a command they show the new state right away.

**While charging**, ATA keeps the reading going between two reads: from the reported power and time
to full it counts the time down and raises battery, range and energy added (e.g. *full in 6 h* read
3 h ago shows *full in 3 h*). The energy added shows with one decimal (*+20.4 kWh*).

**When ATA reads the status** (Settings → *Behavior, status, Bluetooth & NFC* → *Vehicle status*):

- **Battery saver (default):** only when you ask: **pull down on the Vehicle tab** or tap ↻.
- **Smart:** when the app starts, then regularly while the car is plugged in and the phone is in use
  (every 30 min by default, adjustable from 5 min to 3 h).
- **Interval:** at a fixed interval, **also in the background**, 5 min to 3 h in 120 steps (default 30 min).

**Automatic reads never wake the car:** ATA first asks the security controller (which stays awake)
and, if the car sleeps, only refreshes the lock state and keeps the rest from the last reading ("car
asleep" is shown). Pulling down / ↻ wakes the car. Reads only run while the BLE features are on, the
app is **active**, a vehicle is selected and Bluetooth permission is granted.
The last snapshot is cached on device and shown on the Vehicle tab, the status widget, the watch and
its complications, with a relative "updated X ago" timestamp.

#### Demo mode

**Settings → Demo mode** shows a made‑up, connected Model Y so you can try everything without a car:
it starts at **50 %, charging at 22 kW to an 80 % limit, 1 h to go, 20 kWh added**, and every button
changes its state as the real car would. Nothing is sent over Bluetooth. Each time you turn it on it
starts from that same state again.

## Design

Everything about the look is on **Settings → Design**: design, theme, dynamic colour, 3D car,
multi‑tiles and tiles per row. Both designs come in light and dark:

- **Neon (standard):** glowing lines on a dark showroom, a neon car with a floor reflection.
- **Material:** calm Material 3 surfaces, flat tonal tiles, and a **painted car close to the real one**:
  black glass roof, dark windows, black rocker panels and bumpers, dark aero wheels, light strips and a
  clear‑coat sheen when it turns. Doors and lids open as painted panels; the effects use Material
  colours without glow.

## Tabs & tiles

- **Favorites** (left), **Vehicle** (opens on first start) and **Settings**. ATA reopens the tab you
  had open when you closed it.
- The **Vehicle** tab holds all functions: car functions first, then the app actions (navigation,
  Tesla app …). The app actions work without Bluetooth. Every tile can be moved.
- **Edit mode:** long‑press a tile. Drag to move, ✕ to remove, ☆ to add to the favourites.
- **Multi‑tiles** (on by default, **Settings → Design → Multi‑tiles**): related actions share one tile.
  - Only the buttons inside are tappable, and each tile shows the car's state.
  - Multi‑tiles take 2 (2×1) or 4 (2×2) places:
    - lock/unlock, doors (driver/passenger), frunk & trunk, windows, charge port, media
    - **seats (2×2):** heating and ventilation in one tile, switched with **Heat / Vent** inside it. Every
      seat on its own, with an on/off button and three dots for level 1–3; the front seats also have an
      **"A"** button for the car's automatic seat climate
    - **climate (2×2):** on/off, defrost, driver and passenger temperature
    - **charging (2×2):** start/stop, battery, kW, time to full, limit, amps
    - volume, climate keeper, overheat protection
    - sentry, steering‑wheel heater (shows **Auto** when the car runs it by itself, else low/high; Tesla's
      Bluetooth protocol can only switch it on or off), bioweapon defence
  - **Sliders** (volume, temperature, limit, amps): touch, hold briefly (it vibrates), then slide up or
    down; a quick swipe scrolls the page. The command is sent only when you let go, so the car gets one
    command instead of dozens. A tap on the upper or lower half
    changes the value by one step.
  - Off: one tile per action, with a slider dialog for values.
- **Tiles per row:** 2–6 or Auto (3 on phones, more on tablets). Names stay visible at every size.
- **Tablets / landscape:** the car on the left, the tiles on the right.
- **Pull down** on the Vehicle tab to read the status (wakes the car).

## Vehicle view

The Vehicle tab shows your car inside a **battery ring** (dark blue 100–80 %, blue 80–20 %, yellow
20–10 %, red below 10 %, with a subtle animated glow).

A **tick on the ring** marks the charge limit.

**While charging:**
- The tip pulses, and a dashed arc shows the way to the charge limit.
- A chip under the car shows **⚡ X kW · full in …**, a line below the values the **energy added**
  and the limit; the full‑screen view shows it too.

- **Six body styles:** Model 3, Model Y, Model S, Model X, Cybertruck and Roadster. ATA picks the
  style from the VIN; you can override it under **Settings → Vehicles → Edit** (with a live preview).
  The steering wheel, driver door and charge port sit on the left (left‑hand drive).
- **Colour per vehicle:** a Tesla paint for the Material design (Pearl White, Solid Black, Stealth Grey,
  Midnight Silver, Quicksilver, Deep Blue, Ultra Red, Stainless) and a line colour for the neon design.
- **3D (default):** a minimalist neon car that turns slowly, with rims, mirrors, door handles, seats,
  the battery pack in the floor (cells light up with the charge level) and a reflection on the floor.
  Tap it for the **full‑screen view**: turn it with one finger (it follows the finger), **pinch to zoom**, and tap the glowing
  spots on the car to run actions right there:
  - doors, frunk, trunk, windows, lights
  - **seats** (seat heating and ventilation)
  - **steering wheel** (heater)
  - climate and charge port, which also show their multi‑tile

  The seats glow warm while a seat heater is on.
- **2D:** switch off **Settings → Design → 3D vehicle view** for a calm, solid side view of your
  car instead.
- **Animations after each action:** doors swing open, lids lift, the charge port opens and the cable
  plugs in with flowing energy, windows go down, lights flash, wake lights up the light bars, unlock
  lights the door handles and puddle lights, mirrors fold when locking, air flows from the vents,
  music shows an equaliser, sentry sweeps a red radar beam, remote drive spins the wheels, bioweapon
  defence surrounds the car (not the cabin) with green fog. **Show animations** in the full‑screen view
  plays them all without sending anything to the car.

#### Unlock when walking up

**Settings → Behavior, status, Bluetooth & NFC → Unlock when approaching** unlocks the car once when you walk up to it with the phone. ATA watches
the strength of the car's Bluetooth signal (filtered by the car's BLE name, so it also works with the screen off,
or read from the Tesla app's existing connection) and unlocks when it rises above the distance you set
(about 2–20 m). It only fires after you were away from the car, never right after starting next to it and not twice
within two minutes. With the phone locked, ATA asks with a notification first, unless you allow it while locked.
A quiet notification shows while it is on.

## Security

ATA can open your car, so every request is checked by one policy (`ActionPolicy`) before it runs.
The trust level comes **only** from ATA's own per‑install token — never from what the caller claims:

| Who triggers | Basic actions | BLE, non‑critical (climate, charging, lock …) | BLE, critical (unlock, trunk, frunk, charge port open, vent windows, sentry off) |
|---|---|---|---|
| ATA app (tap) | ✅ | ✅ | ✅ after confirmation (+ biometrics if enabled) |
| **ATA shortcuts, widgets, Samsung Routines, Tasker with token** | ✅ | ✅ — also while the phone is locked (switchable) | Notification **"Run …?"** → tap (+ biometrics if enabled). Per action you can allow it without asking (phone unlocked; optionally also locked) |
| ATA watch app | ✅ | ✅ | ✅ after a confirming second tap on the unlocked watch |
| NFC tag written by ATA | ✅ | ✅ | always asks via notification |
| Proximity unlock (walking up to the car) | — | — | **Unlock only**: straight away with the phone unlocked; with the phone locked a notification asks first (unless allowed) |
| Other apps, links, foreign tags (no token) | ✅ | only actions you allow under **Settings → Automation**, phone unlocked | ❌ never |

- The BLE features stay off until you turn them on under **Settings → Behavior, status, Bluetooth & NFC**, and
  automations only run while ATA is **active**.
- There is no web‑link (`BROWSABLE`) entry point, so a web page can't trigger commands.
- **Settings → Automation** shows the token (copy it into Tasker), renews it (ATA re‑issues its own
  shortcuts and widgets) and can invalidate all NFC tags at once.
- The NFC key card only answers while the phone is **unlocked** (a locked phone could otherwise be
  relayed to the car).

## Automation

### Samsung Routines (recommended)

> ⚠️ **Which actions can a routine run?** Samsung Routines only offers the **app shortcuts** ATA
> publishes: the **4 actions of the icon menu** (Settings → *Menu & widgets*) plus the extra
> **routine actions** you tick under Settings → *Menu & widgets* → *Samsung Routines* (up to about 11
> more, depending on the device). Actions that need a value (address, charge limit …) can't be used
> in routines. If an extra action doesn't appear in Routines on your device, only the 4 icon‑menu
> actions work there — put the ones you need into the icon menu.

Samsung Routines starts **app shortcuts**, and ATA's shortcuts carry the token, so routines are
trusted:

1. Choose the actions under Settings → *Menu & widgets*: up to 4 in the **icon menu** and more under
   **Samsung Routines**.
2. In the routine, add the action **Open an app / app shortcut**, pick ATA and the action.
3. For **ATA active / passive** there are shortcuts too (*ATA active*, *ATA passive* in the shortcut
   picker) — routines can't send broadcasts.

What happens when the routine runs:
- **Non‑critical BLE actions** (climate, charging, lock, sentry on …) run right away, even with the
  phone locked in your pocket, and a notification shows the result ("Climate on ✓" or the reason it
  failed).
- **Critical actions** (unlock, trunk, frunk, charge port, vent windows, sentry off) post a
  notification **"Run Unlock?"** — one tap (and biometrics if enabled) and it runs. To run one without
  asking, tick it under **Settings → Automation → Critical actions**.
- **Basic actions** (navigate, open the Tesla app) open immediately when the routine starts the
  shortcut; with a delay, ATA posts a "tap to open" notification because Android doesn't let
  background apps open other apps.
- Allow notifications for ATA (Settings → Automation shows a button) so you see results and
  confirmations.

### Home‑screen shortcuts, app‑icon menu, widgets
Any action can be pinned to the launcher; long‑pressing the ATA icon shows up to **4 configurable
actions**. All of them carry the token and behave like Samsung Routines above.

### Tasker & broadcasts
Target package: `org.tfv.ata` (APK from GitHub) or `de.juliandigitalproducts.ata` (Google Play).
```
Action:  org.tfv.ata.action.RUN
Extras:  action_id   = e.g. "climate_on" | "navigate_home" | "charge_limit"
         token       = your ATA token (Settings → Automation → Copy)
         value       = optional (address, charge limit 50–100, amps 1–48, temperature 15–28)
         vehicle_id  = optional
         delay_ms    = optional delay (up to 6 h, approximate)
```
Without `token`, a broadcast counts as an other app (see [Security](#security)). Instead of the
broadcast you can also use Tasker's *App shortcut* action with an **ATA shortcut**.

```
Action:  org.tfv.ata.action.SET_ACTIVE      Extras: active = true | false, token = your ATA token
```

**Status as a trigger** (opt‑in, Settings → Automation → *Send status broadcast*): after each status
poll ATA sends

```
Action:  org.tfv.ata.action.STATUS
Extras:  battery_percent (int), range_km (int), charging (bool), locked (bool, omitted if unknown),
         climate_on (bool), inside_temp_c (float), outside_temp_c (float), vehicle_id (string)
```
Every app on the phone can read this broadcast, so it is off by default.

BLE commands run in a short foreground service (or a background job when Android doesn't allow the
service), so they also work while the phone is in your pocket.

### Widgets

The widgets look like the app's tiles: cards with icons, neon or Material, light or dark.
Choose under **Settings → Design → Widgets**: *Like the app*, *Neon* or *Material*, optionally with a
see‑through background.

- **Status:**
  - battery with bar, range, inside and outside temperature, lock state
  - while charging, the kW and the time until full
  - responsive from 1×1 to 4×2: every size shows every value, only arranged differently
  - updates after every command
- **Actions:** the actions chosen in Settings → *Menu & widgets* (Basic and BLE), as tiles with icon and name
  (2–4 per row).
- **Find my car:** distance and direction to where the car was last reached over Bluetooth.

## Wear OS

The watch app (separate APK) controls the car from your wrist:

- **Big tiles with icons** on the first screen. Round watches arrange them honeycomb‑style in rows of
  **1/2/1** (4 tiles) or **2/3/2** (7 tiles), square watches as **2×2** or **3×3**. Choose 4 or 7 (9)
  under **Settings → Wear OS watch → Watch layout** on the phone; the first actions you picked become
  tiles.
- **Tiles in the app's card look:** a tile lights up when the car is in its state (e.g. *Lock* while
  locked, *Climate on* while the climate runs).
- **Swipe left** for the **status card**: battery ring (with the green charging dots and the limit
  tick), battery, range and, while charging, power, time to full and energy added, carried forward
  like on the phone.
- **Swipe left again** for all other actions as a list, plus direct control, the NFC key card and pairing.
- **Status around the edge:** battery and range next to the time; along the bottom of the rim the
  inside/outside temperature and lock state, or while charging the power and time to full.
- **Complications for any watch face:** each status value is its own source (*ATA battery* — also
  as a ring —, *ATA range*, *ATA inside/outside temperature*, *ATA lock state*), plus four action
  slots *ATA action 1–4* whose actions you choose on the phone (**Settings → Wear OS watch →
  Watch‑face complications**). Critical actions ask for a confirming tap.
- A **tile** for the tile carousel.

- **Relay mode (default):** tapping a chip sends the action to the phone running ATA, which performs
  it (Basic *and* BLE) and reports the result back to the watch.
- **Opening apps on the phone** (e.g. *Open Tesla app*, *Navigate home* from the watch): Android only
  lets ATA open another app from the background if you allow **"Display over other apps"** for ATA
  (Settings → *Wear OS watch* has a button). Without it, the phone shows a notification you tap to
  open the app, and the watch tells you so. If the switch is greyed out: App info → ATA → ⋮ →
  *Allow restricted settings*.
- In **direct control**, if the watch can't reach the car or isn't paired, it hands the command to
  the phone instead.
- **Critical actions** (unlock, trunk …) need a **second tap** to confirm and never run from a locked
  watch.
- **Direct control (BLE, Wear OS 4 / Android 12 or newer):** the watch can also talk to the car
  itself, without the phone in range. The phone's key is hardware‑backed and cannot be copied, so the
  watch uses **its own** key (Driver role) and is paired with the car once:
  1. On the phone, select the vehicle (Favorites tab, car menu) — its VIN syncs to the watch automatically, so
     you never type a VIN on the watch.
  2. On the watch, enable **Direct control** (grants Bluetooth permission) and tap **Pair this
     watch**; confirm the new key in the car with your key card.
  3. BLE chips then run straight from the watch; Basic actions still relay to the phone.
- **Configure from the phone:** choose which actions appear on the watch under
  **Settings → Wear OS watch**; the list, the selected vehicle and the status sync automatically over
  the Wear Data Layer.

## NFC

Two independent NFC features — you can use either, both, or neither.

### NFC tags (tap‑to‑run)

**Settings → NFC tags** writes an action to an NFC tag or sticker (`ata://run?action=…`). Tapping the
phone on that tag runs the action. Tags carry a separate NFC token, so only your phone runs them,
and **Settings → Automation → Invalidate all NFC tags** revokes every tag at once. Because anyone can
read and copy a tag, critical actions from a tag (e.g. an *Unlock* tag) always ask for confirmation
first.

### Tesla key card emulation — phone *as* the card

**Settings → Bluetooth & NFC → NFC key card** turns the phone itself into a Tesla **key card** via host
card emulation (HCE). The car's center‑console reader talks to the phone over NFC exactly as it would
to a plastic card, so no app has to be open — and it works alongside the BLE phone key, not instead
of it. It only answers while the phone is **unlocked**.

It reuses the **same hardware‑backed key** as the BLE phone key (one enrolment covers both): the
private key is generated in the Android Keystore and never leaves the secure element. The handshake is
standard Tesla VCSEC — an ECDH key agreement on the shared P‑256 key, then an `AES‑128` challenge
response — so no secret is ever stored in the app or transmitted.

**Enrol it like any card:** in the car, **Controls → Locks → Add key**, tap an already‑paired key or
phone to authorise, then hold this phone flat and still against the card reader on the center console
until the car confirms. After that, tap to lock/unlock like a card.

- **Phone must be unlocked:** a locked phone never answers the reader, so neither a lost phone nor a
  relay attack on a phone in your pocket can open (or drive) the car.
- Needs a phone with NFC + HCE (almost all modern Androids). Enabling the feature registers ATA as the
  card service; make sure no other wallet app is set to handle the Tesla card AID.

## Settings

- **Design** page: Neon or Material, theme (System/Light/Dark), Material You dynamic color, 3D vehicle view,
  multi‑tiles, tiles per row. The **language** follows the system (English & German).
- **Navigation app**: Google Maps, Waze or system.
- **Confirm critical actions**, **biometric lock** (apply in the app and when confirming an
  automation).
- **Automation**: what routines, Tasker, widgets, NFC tags and other apps may run; token; status
  broadcast (see [Security](#security)).
- **Point animation to car** (optional): uses your location (permission prompted) to aim the neon
  activation animation toward the car's last‑known spot — the spot is remembered whenever a BLE
  command succeeds (i.e. when you were next to the car).
- **Active toggle** for routines; **multi‑vehicle** (name, VIN, home & work addresses).
- **Behavior, status, Bluetooth & NFC** page: confirmations, haptics, biometric lock, direction
  animation, **vehicle status** (battery saver / smart / interval, see [Live status](#live-status)),
  **Direct BLE control**, **NFC key card** and **unlock when approaching**.
- **Demo mode**: a made‑up connected car to try everything (see [Demo mode](#demo-mode)).
- **3D vehicle view** on or off (off: solid 2D side view); vehicle **type** per vehicle.

## Known limitations

- **Vehicle‑specific functions** (single doors, power liftgate, bioweapon defence …) only work on cars that
  have them.
- The **boombox** (fart and locate sound) is not part of Tesla's Bluetooth protocol (only the cloud API
  offers it), so ATA can't trigger it.
- **Samsung Routines** only lists ATA's published app shortcuts (icon menu + routine actions, see
  above); whether the extra routine actions appear depends on the One UI version.
- Delays (`delay_ms`) are approximate: Android may postpone background work by a few minutes.
- Tesla‑app section deep links are best‑effort and may just open the app on its main screen.

## Build it yourself

Requires JDK 17 and the Android SDK.

```bash
git clone https://github.com/juliandigitalproducts/AndroidTeslaAutomatisation-ATA-.git
cd AndroidTeslaAutomatisation-ATA-
./gradlew :app:assembleRelease :wear:assembleRelease
```

Outputs: `app/build/outputs/apk/release/` and `wear/build/outputs/apk/release/`.

Google Play App Bundles (package `de.juliandigitalproducts.ata`, no APK download links; the
watch bundle's versionCode is the phone's + 1,000,000):

```bash
./gradlew -PataPlay -PataAllAbis :app:bundleRelease :wear:bundleRelease
```

Store listing texts and graphics are in `fastlane/metadata/android/` (`tools/play-graphics.py` builds the
graphics from the CI screenshots); the Play Console guide is in [docs/play/](docs/play/PLAY_CONSOLE.md).

| Module | Purpose |
|---|---|
| `:core` | Shared models, action catalog + policy, Basic deep links, VIN helpers, BLE protocol/crypto (unit‑tested with Tesla's vectors: `./gradlew :core:testDebugUnitTest`) |
| `:app` | Phone app (Compose UI, shortcuts, widgets, automation, Wear bridge) |
| `:wear` | Wear OS app (Compose tile grid, tile, complications) |

## Release signing

Release APKs are signed only with the real release key from four repository secrets. Without them
the CI build **fails** (and a local release build stays unsigned) — ATA never falls back to a debug
key, because an update signed with a known key could take over the paired vehicle key. With the same
key across builds, new versions install as an update over the old one. Set the secrets under
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
  ([github.com/juliandigitalproducts](https://github.com/juliandigitalproducts)). This includes selling it,
  bundling it into a paid or ad‑supported product or service, or otherwise using it to generate
  revenue.
- See [NOTICE](NOTICE) for third‑party attributions (Tesla vehicle‑command protocol, Apache‑2.0).
- ATA is **not affiliated with, endorsed by, or sponsored by Tesla, Inc.** "Tesla" and related
  marks are trademarks of Tesla, Inc., used only to describe interoperability.
- ATA stores your data **only on your device** and needs no account or internet of its own for its
  core features. See the [privacy policy](docs/privacy-policy.md). You are responsible for complying with local laws when automating your vehicle.

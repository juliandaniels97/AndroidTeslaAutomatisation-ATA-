"""Builds the Google Play store graphics from the CI screenshots (demo mode).
Usage: play-graphics.py <phone shots dir> <wear round dir> <out dir, e.g. fastlane/metadata/android>

Writes per language (en-US, de-DE) in fastlane's layout:
  images/icon.png                 512x512
  images/featureGraphic.png       1024x500 (no alpha)
  images/phoneScreenshots/        1080x1920 (9:16) with a caption
  images/sevenInchScreenshots/    1920x1080 (16:9)
  images/tenInchScreenshots/      2560x1440 (16:9)
  images/wearScreenshots/         454x454 (1:1, round watch, no frame)
Needs Pillow and a bold TTF/OTF font (Inter or DejaVu)."""
import sys, os, glob
from PIL import Image, ImageDraw, ImageFilter, ImageFont

phone, round_dir, out = sys.argv[1:4]
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LOGO = os.path.join(ROOT, 'app/src/main/res/drawable-nodpi/ata_logo.png')

def font(size, bold=True):
    names = ['/usr/share/fonts/opentype/inter/Inter-Bold.otf' if bold else '/usr/share/fonts/opentype/inter/Inter-Medium.otf',
             '/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf' if bold else '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf']
    for n in names:
        if os.path.exists(n):
            return ImageFont.truetype(n, size)
    return ImageFont.load_default(size)

def background(w, h, dark=True):
    bg = Image.new('RGBA', (w, h), (8, 10, 15, 255) if dark else (236, 240, 245, 255))
    glow = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse((w * .05, -h * .25, w * .95, h * .55), fill=(79, 180, 255, 55 if dark else 40))
    bg.alpha_composite(glow.filter(ImageFilter.GaussianBlur(min(w, h) // 8)))
    return bg

def rounded(im, radius):
    mask = Image.new('L', im.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, im.size[0] - 1, im.size[1] - 1), radius, fill=255)
    im.putalpha(mask)
    return im

def shadow(canvas, box, radius):
    sh = Image.new('RGBA', canvas.size, (0, 0, 0, 0))
    x0, y0, x1, y1 = box
    ImageDraw.Draw(sh).rounded_rectangle((x0 + 6, y0 + 14, x1 + 6, y1 + 14), radius, fill=(0, 0, 0, 170))
    canvas.alpha_composite(sh.filter(ImageFilter.GaussianBlur(18)))

def wrap(draw, text, fnt, width):
    lines, line = [], ''
    for word in text.split():
        test = (line + ' ' + word).strip()
        if draw.textlength(test, font=fnt) <= width or not line:
            line = test
        else:
            lines.append(line)
            line = word
    return lines + [line]

def centered(draw, y, text, fnt, width, fill, canvas_width, spacing=1.18):
    for line in wrap(draw, text, fnt, width):
        draw.text(((canvas_width - draw.textlength(line, font=fnt)) / 2, y), line, font=fnt, fill=fill)
        y += int(fnt.size * spacing)
    return y

def crop_bars(im):
    """Cuts the emulator's status bar and gesture bar (1080x2400 → 1080x2249)."""
    w, h = im.size
    top, bottom = int(h * .036), int(h * .027)
    return im.crop((0, top, w, h - bottom))

def phone_shot(src, title, sub, path):
    W, H = 1080, 1920
    canvas = background(W, H)
    d = ImageDraw.Draw(canvas)
    y = centered(d, 70, title, font(74), W - 120, (255, 255, 255, 255), W)
    centered(d, y + 14, sub, font(40, bold=False), W - 160, (170, 200, 230, 255), W)
    top = 330
    ph = H - top - 50
    im = crop_bars(Image.open(src).convert('RGBA'))
    pw = int(ph * im.size[0] / im.size[1])
    im = im.resize((pw, ph), Image.LANCZOS)
    x = (W - pw) // 2
    r = int(pw * .07)
    shadow(canvas, (x, top, x + pw, top + ph), r)
    canvas.alpha_composite(rounded(im, r), (x, top))
    border = Image.new('RGBA', (pw, ph), (0, 0, 0, 0))
    ImageDraw.Draw(border).rounded_rectangle((0, 0, pw - 1, ph - 1), r, outline=(255, 255, 255, 60), width=3)
    canvas.alpha_composite(border, (x, top))
    canvas.convert('RGB').save(path, optimize=True)

def feature_graphic(lang, path):
    W, H = 1024, 500
    canvas = background(W, H)
    logo = Image.open(LOGO).convert('RGBA').resize((340, 340), Image.LANCZOS)
    canvas.alpha_composite(rounded(logo, 60), (60, (H - 340) // 2))
    d = ImageDraw.Draw(canvas)
    head = {'en-US': 'Your car. One tap.', 'de-DE': 'Dein Auto. Ein Tipp.'}[lang]
    sub = {'en-US': 'Bluetooth control, widgets, routines and Wear OS. No cloud, no account.',
           'de-DE': 'Steuerung per Bluetooth, Widgets, Routinen und Wear OS. Ohne Cloud, ohne Konto.'}[lang]
    x, y = 450, 150
    d.text((x, y), head, font=font(50), fill=(255, 255, 255, 255))
    y += 80
    for line in wrap(d, sub, font(28, bold=False), W - x - 50):
        d.text((x, y), line, font=font(28, bold=False), fill=(170, 200, 230, 255))
        y += 38
    canvas.convert('RGB').save(path, optimize=True)

# (EN shot, DE shot, EN caption, EN subline, DE caption, DE subline)
PHONE = [
    ('dark-2-vehicle', 'de-dark-2-vehicle', 'Your car at a glance', 'Battery, range, temperatures and charging, live over Bluetooth',
     'Dein Auto auf einen Blick', 'Akku, Reichweite, Temperaturen und Laden, live per Bluetooth'),
    ('dark-1-home', 'de-dark-1-home', 'One tap, done', 'Favorites for lock, climate, frunk and navigation',
     'Ein Tipp, fertig', 'Favoriten für Schloss, Klima, Frunk und Navigation'),
    ('dark-2d-vehicle', 'de-dark-2d-vehicle', 'Charging, doors, windows', 'Charge limit, amps, charge port, frunk and trunk',
     'Laden und Sitzheizung', 'Ladelimit und Ampere, Sitzheizung für jeden Sitz'),
    ('dark-2c-vehicle', 'de-dark-2c-vehicle', 'Climate and seats', 'Seat heating per seat, steering wheel heater, defrost',
     'Klima und Sitze', 'Sitzheizung je Sitz, Lenkradheizung, Entfrosten'),
    ('dark-21-car3d', 'de-dark-21-car3d', 'Tap the car itself', 'A 3D car you can turn, with glowing spots for every action',
     'Tippe direkt aufs Auto', 'Ein 3D-Auto zum Drehen, mit leuchtenden Punkten für jede Aktion'),
    ('light-2-vehicle', 'de-light-2-vehicle', 'Light or dark', 'Neon or Material design, both in light and dark',
     'Hell oder dunkel', 'Neon- oder Material-Design, jeweils hell und dunkel'),
    ('m-dark-2-vehicle', 'de-m-dark-2-vehicle', 'Material design', 'A painted car that looks like yours',
     'Material-Design', 'Ein lackiertes Auto, das aussieht wie deins'),
    ('dark-7-automationroutinestasker', 'de-dark-7-automationroutinestasker', 'Automate it', 'Samsung Routines, Tasker, widgets, shortcuts and NFC tags',
     'Automatisieren', 'Samsung Routinen, Tasker, Widgets, Verknüpfungen und NFC-Tags'),
]

def ensure(path):
    os.makedirs(path, exist_ok=True)
    for f in glob.glob(os.path.join(path, '*.png')):
        os.remove(f)
    return path

for lang in ['en-US', 'de-DE']:
    base = os.path.join(out, lang, 'images')
    os.makedirs(base, exist_ok=True)
    Image.open(LOGO).convert('RGBA').resize((512, 512), Image.LANCZOS).save(os.path.join(base, 'icon.png'), optimize=True)
    feature_graphic(lang, os.path.join(base, 'featureGraphic.png'))
    shots = ensure(os.path.join(base, 'phoneScreenshots'))
    for i, (en, de, t_en, s_en, t_de, s_de) in enumerate(PHONE, 1):
        src = os.path.join(phone, (de if lang == 'de-DE' else en) + '.png')
        if not os.path.exists(src):  # no German shot: use the English one
            src = os.path.join(phone, en + '.png')
        if os.path.exists(src):
            phone_shot(src, t_de if lang == 'de-DE' else t_en, s_de if lang == 'de-DE' else s_en, os.path.join(shots, '%d.png' % i))
    for inch, folder, size in [('7', 'sevenInchScreenshots', (1920, 1080)), ('10', 'tenInchScreenshots', (2560, 1440))]:
        dst = ensure(os.path.join(base, folder))
        for i, f in enumerate(sorted(glob.glob(os.path.join(phone, 'play-tablet%s-*.png' % inch))), 1):
            Image.open(f).convert('RGB').resize(size, Image.LANCZOS).save(os.path.join(dst, '%d.png' % i), optimize=True)
    dst = ensure(os.path.join(base, 'wearScreenshots'))
    for i, name in enumerate(['wear-grid-4', 'wear-status', 'wear-grid-7', 'wear-list'], 1):
        f = os.path.join(round_dir, name + '.png')
        if os.path.exists(f):
            im = Image.open(f).convert('RGBA')
            flat = Image.new('RGB', im.size, (0, 0, 0))
            flat.paste(im, mask=im.split()[3])
            flat.save(os.path.join(dst, '%d.png' % i), optimize=True)
    print('wrote', base)

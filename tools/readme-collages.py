"""Builds the README collages (1832x800) from CI screenshots.
Usage: collage.py <phone shots dir> <wear round dir> <wear square dir> <out dir>"""
import sys, os
from PIL import Image, ImageDraw, ImageFilter

phone, round_dir, square_dir, out = sys.argv[1:5]
W, H = 1832, 800

def rounded(im, radius):
    mask = Image.new('L', im.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, im.size[0] - 1, im.size[1] - 1), radius, fill=255)
    im.putalpha(mask)
    return im

def circle(im):
    mask = Image.new('L', im.size, 0)
    ImageDraw.Draw(mask).ellipse((0, 0, im.size[0] - 1, im.size[1] - 1), fill=255)
    im.putalpha(mask)
    return im

def shadow(canvas, box, radius, dark, ellipse=False):
    sh = Image.new('RGBA', canvas.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(sh)
    x0, y0, x1, y1 = box
    col = (0, 0, 0, 150 if dark else 70)
    if ellipse:
        d.ellipse((x0 + 6, y0 + 12, x1 + 6, y1 + 12), fill=col)
    else:
        d.rounded_rectangle((x0 + 6, y0 + 12, x1 + 6, y1 + 12), radius, fill=col)
    canvas.alpha_composite(sh.filter(ImageFilter.GaussianBlur(14)))

def background(dark):
    bg = Image.new('RGBA', (W, H), (8, 10, 15, 255) if dark else (236, 240, 245, 255))
    glow = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse((W * .15, -H * .4, W * .85, H * .9), fill=(79, 180, 255, 40 if dark else 30))
    bg.alpha_composite(glow.filter(ImageFilter.GaussianBlur(120)))
    return bg

def phone_collage(names, dark, path):
    canvas = background(dark)
    n = len(names)
    margin, gap = 36, 26
    ph = H - 2 * margin
    pw = int(ph * 1080 / 2400)
    total = n * pw + (n - 1) * gap
    x = (W - total) // 2
    for name in names:
        im = Image.open(os.path.join(phone, name + '.png')).convert('RGBA').resize((pw, ph), Image.LANCZOS)
        r = int(pw * .09)
        shadow(canvas, (x, margin, x + pw, margin + ph), r, dark)
        canvas.alpha_composite(rounded(im, r), (x, margin))
        border = Image.new('RGBA', (pw, ph), (0, 0, 0, 0))
        ImageDraw.Draw(border).rounded_rectangle((0, 0, pw - 1, ph - 1), r, outline=(255, 255, 255, 40) if dark else (0, 0, 0, 40), width=2)
        canvas.alpha_composite(border, (x, margin))
        x += pw + gap
    canvas.convert('RGB').save(path, optimize=True)
    print('wrote', path)

def wear_collage(path):
    canvas = background(True)
    items = [(round_dir, 'wear-grid-4', True), (round_dir, 'wear-grid-7', True), (round_dir, 'wear-status', True),
             (square_dir, 'wear-grid-4', False), (square_dir, 'wear-grid-7', False), (square_dir, 'wear-status', False)]
    items = [it for it in items if os.path.exists(os.path.join(it[0], it[1] + '.png'))]
    n = len(items)
    gap = 30
    size = min(int((W - 80 - gap * (n - 1)) / n), 520)
    total = n * size + (n - 1) * gap
    x = (W - total) // 2
    y = (H - size) // 2
    for d, name, is_round in items:
        im = Image.open(os.path.join(d, name + '.png')).convert('RGBA').resize((size, size), Image.LANCZOS)
        # Watch bezel.
        bezel = 10
        shadow(canvas, (x - bezel, y - bezel, x + size + bezel, y + size + bezel), int(size * .2), True, ellipse=is_round)
        frame = Image.new('RGBA', (size + 2 * bezel, size + 2 * bezel), (0, 0, 0, 0))
        fd = ImageDraw.Draw(frame)
        if is_round:
            fd.ellipse((0, 0, size + 2 * bezel - 1, size + 2 * bezel - 1), fill=(40, 44, 52, 255), outline=(90, 98, 110, 255), width=3)
        else:
            fd.rounded_rectangle((0, 0, size + 2 * bezel - 1, size + 2 * bezel - 1), int(size * .2), fill=(40, 44, 52, 255), outline=(90, 98, 110, 255), width=3)
        canvas.alpha_composite(frame, (x - bezel, y - bezel))
        canvas.alpha_composite(circle(im) if is_round else rounded(im, int(size * .16)), (x, y))
        x += size + gap
    top = max(0, y - 60)
    canvas.crop((0, top, W, min(H, y + size + 60))).convert('RGB').save(path, optimize=True)
    print('wrote', path)

os.makedirs(out, exist_ok=True)
if os.path.isdir(round_dir): wear_collage(os.path.join(out, 'ata-wear.png'))
phone_collage(['dark-1-home', 'dark-2-vehicle', 'dark-2c-vehicle', 'dark-demo-3', 'dark-2e-design'], True, os.path.join(out, 'ata-dark.png'))
phone_collage(['light-1-home', 'light-2-vehicle', 'light-2c-vehicle', 'light-demo-3', 'light-2e-design'], False, os.path.join(out, 'ata-light.png'))
if os.path.exists(os.path.join(phone, 'm-dark-1-home.png')):
    phone_collage(['m-dark-1-home', 'm-dark-2-vehicle', 'm-dark-2c-vehicle', 'm-dark-demo-3', 'm-dark-5-settings'], True, os.path.join(out, 'ata-material-dark.png'))
    phone_collage(['m-light-1-home', 'm-light-2-vehicle', 'm-light-2c-vehicle', 'm-light-demo-3', 'm-light-5-settings'], False, os.path.join(out, 'ata-material-light.png'))

def tablet_collage(names, path):
    canvas = background(True)
    margin, gap = 50, 40
    tw = (W - 2 * margin - gap * (len(names) - 1)) // len(names)
    w0, h0 = Image.open(os.path.join(phone, names[0] + '.png')).size
    th = int(tw * h0 / w0)
    y = (H - th) // 2
    x = margin
    for name in names:
        im = Image.open(os.path.join(phone, name + '.png')).convert('RGBA').resize((tw, th), Image.LANCZOS)
        r = int(tw * .03)
        shadow(canvas, (x, y, x + tw, y + th), r, True)
        canvas.alpha_composite(rounded(im, r), (x, y))
        x += tw + gap
    canvas.crop((0, y - 40, W, y + th + 40)).convert('RGB').save(path, optimize=True)
    print('wrote', path)

# The 16:9 Play tablet shots switch the design at phone size and are reliable; the older 16:10 pair is the fallback.
if os.path.exists(os.path.join(phone, 'play-tablet10-1-vehicle.png')):
    tablet_collage(['play-tablet10-1-vehicle', 'play-tablet10-4-material'], os.path.join(out, 'ata-tablet.png'))
elif os.path.exists(os.path.join(phone, 'tablet-neon-dark.png')):
    tablet_collage(['tablet-neon-dark', 'tablet-material-light'], os.path.join(out, 'ata-tablet.png'))

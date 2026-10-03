import os
import math
from PIL import Image, ImageDraw, ImageFont

output_dir = r"C:\Users\paul\AndroidStudioProjects\load_tracker_pro\store_assets"
os.makedirs(output_dir, exist_ok=True)

# Exact Colors from app_logo.xml
COLOR_NAVY_DARK = (15, 23, 42)      # #0F172A
COLOR_NAVY_LIGHT = (30, 41, 59)     # #1E293B
COLOR_SAFETY_ORANGE = (255, 107, 0) # #FF6B00
COLOR_BLACK_ACCENT = (2, 6, 23)     # #020617
COLOR_WHITE_SLATE = (248, 250, 252) # #F8FAFC
COLOR_BLUE_LINE = (59, 130, 246)    # #3B82F6

def draw_exact_logo_emblem(draw, center_x, center_y, scale=1.0):
    # Scale factor from 108x108 viewport to target px size
    s = scale

    # Outer Safety Orange Shield Boundary
    shield_pts = []
    for deg in range(0, 360, 2):
        rad = math.radians(deg)
        # Shield curve approximation
        r = 160 * s
        x = center_x + r * math.cos(rad)
        y = center_y + r * math.sin(rad)
        shield_pts.append((x, y))

    # Safety Orange Shield
    draw.polygon(shield_pts, fill=COLOR_SAFETY_ORANGE)

    # Inner Black Accent
    inner_pts = [(center_x + (pt[0] - center_x) * 0.92, center_y + (pt[1] - center_y) * 0.92) for pt in shield_pts]
    draw.polygon(inner_pts, fill=COLOR_BLACK_ACCENT)

    # White Horizontal Divider Line
    line_y = center_y - 45 * s
    draw.rectangle([center_x - 120 * s, line_y - 4 * s, center_x + 120 * s, line_y + 4 * s], fill=COLOR_WHITE_SLATE)

    # Center GPS Pin
    pin_r = 50 * s
    pin_cy = center_y + 10 * s
    draw.ellipse([center_x - pin_r, pin_cy - pin_r, center_x + pin_r, pin_cy + pin_r], fill=COLOR_WHITE_SLATE)

    inner_pin_r = 42 * s
    draw.ellipse([center_x - inner_pin_r, pin_cy - inner_pin_r, center_x + inner_pin_r, pin_cy + inner_pin_r], fill=COLOR_NAVY_DARK)

    # Dollar Sign $
    try:
        font_dollar = ImageFont.truetype("arialbd.ttf", int(50 * s))
    except:
        font_dollar = ImageFont.load_default()

    draw.text((center_x, pin_cy - 2 * s), "$", fill=COLOR_SAFETY_ORANGE, font=font_dollar, anchor="mm")

def create_exact_app_icon():
    width, height = 512, 512
    img = Image.new("RGBA", (width, height), COLOR_NAVY_DARK)
    draw = ImageDraw.Draw(img)

    # Background Gradient #0F172A -> #1E293B
    for y in range(height):
        r = int(COLOR_NAVY_DARK[0] + (COLOR_NAVY_LIGHT[0] - COLOR_NAVY_DARK[0]) * (y / height))
        g = int(COLOR_NAVY_DARK[1] + (COLOR_NAVY_LIGHT[1] - COLOR_NAVY_DARK[1]) * (y / height))
        b = int(COLOR_NAVY_DARK[2] + (COLOR_NAVY_LIGHT[2] - COLOR_NAVY_DARK[2]) * (y / height))
        draw.line([(0, y), (width, y)], fill=(r, g, b, 255))

    # Speed Lines Overlay
    draw.polygon([(-50, 80), (562, -180), (562, -90), (-50, 170)], fill=(59, 130, 246, 30))
    draw.polygon([(-50, 220), (562, -40), (562, 50), (-50, 310)], fill=(59, 130, 246, 25))
    draw.polygon([(-50, 360), (562, 100), (562, 190), (-50, 450)], fill=(255, 107, 0, 45))

    # Render Exact Shield Emblem
    draw_exact_logo_emblem(draw, 256, 220, scale=1.1)

    # Bottom Text "LOAD TRACKER PRO"
    try:
        font_title = ImageFont.truetype("arialbd.ttf", 38)
        font_sub = ImageFont.truetype("arialbd.ttf", 22)
    except:
        font_title = ImageFont.load_default()
        font_sub = ImageFont.load_default()

    draw.text((256, 415), "LOAD TRACKER", fill=COLOR_WHITE_SLATE, font=font_title, anchor="mm")
    draw.text((256, 455), "PRO", fill=COLOR_SAFETY_ORANGE, font=font_sub, anchor="mm")

    path = os.path.join(output_dir, "app_icon_512x512.png")
    img.save(path, "PNG")
    print(f"Updated App Icon from app_logo.xml: {path}")

def create_exact_feature_graphic():
    width, height = 1024, 500
    img = Image.new("RGBA", (width, height), COLOR_NAVY_DARK)
    draw = ImageDraw.Draw(img)

    # Background Gradient
    for y in range(height):
        r = int(COLOR_NAVY_DARK[0] + (COLOR_NAVY_LIGHT[0] - COLOR_NAVY_DARK[0]) * (y / height))
        g = int(COLOR_NAVY_DARK[1] + (COLOR_NAVY_LIGHT[1] - COLOR_NAVY_DARK[1]) * (y / height))
        b = int(COLOR_NAVY_DARK[2] + (COLOR_NAVY_LIGHT[2] - COLOR_NAVY_DARK[2]) * (y / height))
        draw.line([(0, y), (width, y)], fill=(r, g, b, 255))

    # Speed Lines
    draw.polygon([(-50, 180), (1074, -120), (1074, -30), (-50, 270)], fill=(59, 130, 246, 25))
    draw.polygon([(-50, 320), (1074, 20), (1074, 110), (-50, 410)], fill=(255, 107, 0, 40))

    # Render Logo Emblem on Left Side
    draw_exact_logo_emblem(draw, 180, 250, scale=1.0)

    # Title & Subtitle Typography on Right Side
    try:
        font_main = ImageFont.truetype("arialbd.ttf", 52)
        font_sub = ImageFont.truetype("arialbd.ttf", 26)
        font_badge = ImageFont.truetype("arialbd.ttf", 20)
    except:
        font_main = ImageFont.load_default()
        font_sub = ImageFont.load_default()
        font_badge = ImageFont.load_default()

    draw.text((380, 160), "LOAD TRACKER PRO", fill=COLOR_WHITE_SLATE, font=font_main)
    draw.text((380, 230), "GPS Trip Auditing  •  IFTA Mileage  •  Flatbed Payroll", fill=COLOR_SAFETY_ORANGE, font=font_sub)

    # Feature Badges
    badges = ["🛰️ Real-Time GPS Tracking", "💵 Exact Payroll Math", "🗺️ Offline IFTA Audit", "📷 OCR Rate Scanner"]
    for i, b in enumerate(badges):
        bx = 380 + (i % 2) * 310
        by = 310 + (i // 2) * 55
        draw.rounded_rectangle([bx, by, bx + 290, by + 45], radius=12, fill=(2, 6, 23, 220), outline=COLOR_SAFETY_ORANGE, width=2)
        draw.text((bx + 145, by + 22), b, fill=COLOR_WHITE_SLATE, font=font_badge, anchor="mm")

    path = os.path.join(output_dir, "feature_graphic_1024x500.png")
    img.save(path, "PNG")
    print(f"Updated Feature Graphic from app_logo.xml: {path}")

if __name__ == "__main__":
    create_exact_app_icon()
    create_exact_feature_graphic()

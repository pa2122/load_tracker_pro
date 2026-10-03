import os
from PIL import Image, ImageDraw, ImageFont

output_dir = r"C:\Users\paul\AndroidStudioProjects\load_tracker_pro\store_assets"
os.makedirs(output_dir, exist_ok=True)

# Colors
NAVY_DARK = (15, 23, 42)      # #0F172A
NAVY_LIGHT = (30, 41, 59)     # #1E293B
COBALT_BLUE = (59, 130, 246)  # #3B82F6
GOLD = (245, 158, 11)         # #F59E0B
WHITE = (255, 255, 255)
LIGHT_GRAY = (203, 213, 225) # #CBD5E1

def create_app_icon():
    width, height = 512, 512
    img = Image.new("RGBA", (width, height), NAVY_DARK)
    draw = ImageDraw.Draw(img)

    # Gradient background
    for y in range(height):
        r = int(NAVY_DARK[0] + (NAVY_LIGHT[0] - NAVY_DARK[0]) * (y / height))
        g = int(NAVY_DARK[1] + (NAVY_LIGHT[1] - NAVY_DARK[1]) * (y / height))
        b = int(NAVY_DARK[2] + (NAVY_LIGHT[2] - NAVY_DARK[2]) * (y / height))
        draw.line([(0, y), (width, y)], fill=(r, g, b, 255))

    # Outer Shield / Ring
    draw.rounded_rectangle([20, 20, width - 20, height - 20], radius=80, outline=COBALT_BLUE, width=8)

    # GPS Pin Background Circle
    center_x, center_y = 256, 220
    draw.ellipse([center_x - 120, center_y - 120, center_x + 120, center_y + 120], fill=(30, 58, 138, 255), outline=COBALT_BLUE, width=6)

    # Semi Truck Flatbed Silhouette Drawing
    # Truck Cab
    draw.rounded_rectangle([center_x - 90, center_y - 30, center_x - 20, center_y + 60], radius=10, fill=GOLD)
    draw.rectangle([center_x - 50, center_y - 20, center_x - 20, center_y + 10], fill=NAVY_DARK) # Cab Window
    # Flatbed Trailer
    draw.rectangle([center_x - 20, center_y + 10, center_x + 90, center_y + 60], fill=WHITE)
    # Tarp/Coil Load
    draw.ellipse([center_x + 10, center_y - 15, center_x + 60, center_y + 15], fill=GOLD)

    # Wheels
    draw.ellipse([center_x - 75, center_y + 50, center_x - 45, center_y + 80], fill=NAVY_DARK, outline=WHITE, width=4)
    draw.ellipse([center_x + 35, center_y + 50, center_x + 65, center_y + 80], fill=NAVY_DARK, outline=WHITE, width=4)
    draw.ellipse([center_x + 65, center_y + 50, center_x + 95, center_y + 80], fill=NAVY_DARK, outline=WHITE, width=4)

    # Text "LOAD TRACKER PRO" at bottom
    try:
        font_large = ImageFont.truetype("arialbd.ttf", 38)
        font_sub = ImageFont.truetype("arialbd.ttf", 24)
    except:
        font_large = ImageFont.load_default()
        font_sub = ImageFont.load_default()

    draw.text((256, 390), "LOAD TRACKER", fill=WHITE, font=font_large, anchor="mm")
    draw.text((256, 435), "PRO", fill=GOLD, font=font_sub, anchor="mm")

    path = os.path.join(output_dir, "app_icon_512x512.png")
    img.save(path, "PNG")
    print(f"Generated App Icon: {path}")

def create_feature_graphic():
    width, height = 1024, 500
    img = Image.new("RGBA", (width, height), NAVY_DARK)
    draw = ImageDraw.Draw(img)

    # Gradient background
    for y in range(height):
        r = int(NAVY_DARK[0] + (NAVY_LIGHT[0] - NAVY_DARK[0]) * (y / height))
        g = int(NAVY_DARK[1] + (NAVY_LIGHT[1] - NAVY_DARK[1]) * (y / height))
        b = int(NAVY_DARK[2] + (NAVY_LIGHT[2] - NAVY_DARK[2]) * (y / height))
        draw.line([(0, y), (width, y)], fill=(r, g, b, 255))

    # Decorative GPS Route Wave Line
    points = [(50, 400), (200, 320), (350, 380), (500, 280), (650, 340), (800, 220), (970, 300)]
    draw.line(points, fill=COBALT_BLUE, width=10)
    for p in points:
        draw.ellipse([p[0] - 12, p[1] - 12, p[0] + 12, p[1] + 12], fill=GOLD, outline=WHITE, width=3)

    # Title Typography
    try:
        font_title = ImageFont.truetype("arialbd.ttf", 56)
        font_subtitle = ImageFont.truetype("arialbd.ttf", 28)
        font_badge = ImageFont.truetype("arialbd.ttf", 22)
    except:
        font_title = ImageFont.load_default()
        font_subtitle = ImageFont.load_default()
        font_badge = ImageFont.load_default()

    draw.text((512, 120), "LOAD TRACKER PRO", fill=WHITE, font=font_title, anchor="mm")
    draw.text((512, 185), "GPS Trip Auditing  •  IFTA Mileage  •  Flatbed Payroll", fill=GOLD, font=font_subtitle, anchor="mm")

    # Feature Badges
    badges = ["🛰️ Real-Time GPS Tracking", "💵 Exact Payroll Math", "🗺️ Offline IFTA Audit", "📷 OCR Rate Scanner"]
    badge_x_starts = [100, 330, 560, 780]

    for i, b in enumerate(badges):
        bx = badge_x_starts[i]
        draw.rounded_rectangle([bx, 420, bx + 190, 465], radius=20, fill=(30, 58, 138, 255), outline=COBALT_BLUE, width=2)
        draw.text((bx + 95, 442), b, fill=WHITE, font=font_badge, anchor="mm")

    path = os.path.join(output_dir, "feature_graphic_1024x500.png")
    img.save(path, "PNG")
    print(f"Generated Feature Graphic: {path}")

if __name__ == "__main__":
    create_app_icon()
    create_feature_graphic()

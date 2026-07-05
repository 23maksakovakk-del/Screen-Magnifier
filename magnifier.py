# magnifier.py
#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import sys
import argparse
from PIL import Image
import math

# ANSI-цвета (24-битные)
COLORS = {
    'reset': '\033[0m',
}

def rgb_to_ansi(r, g, b):
    """Преобразует RGB в 24-битный ANSI-код для фона."""
    return f'\033[48;2;{r};{g};{b}m'

def clamp(val, min_val, max_val):
    return max(min_val, min(val, max_val))

class Magnifier:
    def __init__(self, image_path):
        self.img = Image.open(image_path).convert('RGB')
        self.width, self.height = self.img.size

    def get_region(self, x, y, w, h):
        """Извлекает область из изображения."""
        x = clamp(x, 0, self.width - 1)
        y = clamp(y, 0, self.height - 1)
        w = clamp(w, 1, self.width - x)
        h = clamp(h, 1, self.height - y)
        return self.img.crop((x, y, x + w, y + h))

    def resize(self, region, scale, method=Image.BILINEAR):
        """Увеличивает область."""
        new_size = (region.width * scale, region.height * scale)
        return region.resize(new_size, method)

    def display(self, region, scale, color=True):
        """Выводит увеличенную область в терминал."""
        pix = region.load()
        w, h = region.size
        print(colorize(f"🔍 Увеличенная область: {w//scale}×{h//scale} пикселей, масштаб: {scale}x", 'bold'))
        for y in range(h):
            line = ''
            for x in range(w):
                r, g, b = pix[x, y]
                if color:
                    line += rgb_to_ansi(r, g, b) + '  '  # два пробела для квадрата
                else:
                    # без цвета, просто символ
                    brightness = int(0.299*r + 0.587*g + 0.114*b)
                    if brightness > 200:
                        line += '█'
                    elif brightness > 150:
                        line += '▓'
                    elif brightness > 100:
                        line += '▒'
                    elif brightness > 50:
                        line += '░'
                    else:
                        line += ' '
            if color:
                line += COLORS['reset']
            print(line)

def colorize(text, color):
    # Для простоты используем ANSI bold
    return f'\033[1m{text}\033[0m'

def main():
    parser = argparse.ArgumentParser(description="Screen Magnifier – экранная лупа")
    parser.add_argument('file', help='Файл изображения')
    parser.add_argument('-x', type=int, default=0, help='Координата X')
    parser.add_argument('-y', type=int, default=0, help='Координата Y')
    parser.add_argument('-w', type=int, default=40, help='Ширина области (пиксели)')
    parser.add_argument('-h', type=int, default=20, help='Высота области (пиксели)')
    parser.add_argument('-s', '--scale', type=int, default=4, help='Коэффициент увеличения')
    parser.add_argument('--no-color', action='store_true', help='Отключить цвет')
    parser.add_argument('--output', help='Сохранить увеличенное изображение в файл')
    args = parser.parse_args()

    magnifier = Magnifier(args.file)
    region = magnifier.get_region(args.x, args.y, args.w, args.h)
    scaled = magnifier.resize(region, args.scale)

    if args.output:
        scaled.save(args.output)
        print(f"Увеличенное изображение сохранено в {args.output}")

    magnifier.display(scaled, args.scale, color=not args.no_color)

if __name__ == '__main__':
    try:
        main()
    except KeyboardInterrupt:
        print("\nВыход.")
        sys.exit(0)

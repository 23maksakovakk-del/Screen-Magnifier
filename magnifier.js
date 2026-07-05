// magnifier.js
#!/usr/bin/env node
'use strict';

const sharp = require('sharp');
const fs = require('fs');
const path = require('path');

const COLORS = {
    reset: '\x1b[0m',
    bold: '\x1b[1m'
};

function colorize(text, color) {
    return COLORS[color] + text + COLORS.reset;
}

function rgbToAnsi(r, g, b) {
    return `\x1b[48;2;${r};${g};${b}m`;
}

function clamp(val, min, max) {
    return Math.max(min, Math.min(val, max));
}

async function main() {
    const args = process.argv.slice(2);
    let file = null;
    let x = 0, y = 0, w = 40, h = 20, scale = 4;
    let noColor = false, output = null;

    for (let i = 0; i < args.length; i++) {
        const arg = args[i];
        if (arg === '-x' && i+1 < args.length) x = parseInt(args[++i]);
        else if (arg === '-y' && i+1 < args.length) y = parseInt(args[++i]);
        else if (arg === '-w' && i+1 < args.length) w = parseInt(args[++i]);
        else if (arg === '-h' && i+1 < args.length) h = parseInt(args[++i]);
        else if (arg === '-s' || arg === '--scale') scale = parseInt(args[++i]);
        else if (arg === '--no-color') noColor = true;
        else if (arg === '--output' && i+1 < args.length) output = args[++i];
        else if (arg === '-h' || arg === '--help') {
            console.log('Usage: node magnifier.js <file> [-x X] [-y Y] [-w W] [-h H] [-s SCALE] [--no-color] [--output FILE]');
            process.exit(0);
        } else if (!file) file = arg;
    }
    if (!file) {
        console.error('Укажите файл изображения.');
        process.exit(1);
    }

    try {
        const metadata = await sharp(file).metadata();
        const maxX = metadata.width - 1;
        const maxY = metadata.height - 1;
        x = clamp(x, 0, maxX);
        y = clamp(y, 0, maxY);
        w = clamp(w, 1, maxX - x + 1);
        h = clamp(h, 1, maxY - y + 1);

        // Извлечение области
        const regionBuffer = await sharp(file)
            .extract({ left: x, top: y, width: w, height: h })
            .raw()
            .toBuffer({ resolveWithObject: true });

        // Масштабирование (с помощью sharp)
        const scaledBuffer = await sharp(regionBuffer.data, {
            raw: { width: w, height: h, channels: 3 }
        })
            .resize(w * scale, h * scale, { kernel: sharp.kernel.lanczos2 })
            .raw()
            .toBuffer({ resolveWithObject: true });

        if (output) {
            await sharp(scaledBuffer.data, {
                raw: { width: w * scale, height: h * scale, channels: 3 }
            })
                .toFile(output);
            console.log(`Увеличенное изображение сохранено в ${output}`);
        }

        console.log(colorize(`🔍 Увеличенная область: ${w}×${h} пикселей, масштаб: ${scale}x`, 'bold'));

        // Вывод в терминал
        const data = scaledBuffer.data;
        const sw = scaledBuffer.info.width;
        const sh = scaledBuffer.info.height;
        for (let row = 0; row < sh; row++) {
            let line = '';
            for (let col = 0; col < sw; col++) {
                const idx = (row * sw + col) * 3;
                const r = data[idx];
                const g = data[idx+1];
                const b = data[idx+2];
                if (!noColor) {
                    line += rgbToAnsi(r, g, b) + '  ';
                } else {
                    const brightness = 0.299 * r + 0.587 * g + 0.114 * b;
                    if (brightness > 200) line += '██';
                    else if (brightness > 150) line += '▓▓';
                    else if (brightness > 100) line += '▒▒';
                    else if (brightness > 50) line += '░░';
                    else line += '  ';
                }
            }
            if (!noColor) line += COLORS.reset;
            console.log(line);
        }
    } catch (err) {
        console.error('Ошибка:', err.message);
        process.exit(1);
    }
}

main();

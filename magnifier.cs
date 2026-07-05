// magnifier.cs
using System;
using System.Collections.Generic;
using System.IO;
using SixLabors.ImageSharp;
using SixLabors.ImageSharp.Processing;
using SixLabors.ImageSharp.PixelFormats;

class Magnifier
{
    static string Colorize(string text, string color)
    {
        string col = color switch
        {
            "bold" => "\x1b[1m",
            _ => "\x1b[0m"
        };
        return col + text + "\x1b[0m";
    }

    static string RgbToAnsi(byte r, byte g, byte b)
    {
        return $"\x1b[48;2;{r};{g};{b}m";
    }

    static int Clamp(int val, int min, int max)
    {
        return Math.Max(min, Math.Min(val, max));
    }

    static void Main(string[] args)
    {
        string file = null;
        int x = 0, y = 0, w = 40, h = 20, scale = 4;
        bool noColor = false;
        string output = null;

        for (int i = 0; i < args.Length; i++)
        {
            string arg = args[i];
            if (arg == "-x" && i+1 < args.Length) x = int.Parse(args[++i]);
            else if (arg == "-y" && i+1 < args.Length) y = int.Parse(args[++i]);
            else if (arg == "-w" && i+1 < args.Length) w = int.Parse(args[++i]);
            else if (arg == "-h" && i+1 < args.Length) h = int.Parse(args[++i]);
            else if (arg == "-s" || arg == "--scale") scale = int.Parse(args[++i]);
            else if (arg == "--no-color") noColor = true;
            else if (arg == "--output" && i+1 < args.Length) output = args[++i];
            else if (arg == "-h" || arg == "--help")
            {
                Console.WriteLine("Usage: magnifier <file> [-x X] [-y Y] [-w W] [-h H] [-s SCALE] [--no-color] [--output FILE]");
                return;
            }
            else if (file == null) file = arg;
        }
        if (file == null)
        {
            Console.WriteLine("Укажите файл изображения.");
            return;
        }

        try
        {
            using var img = Image.Load<Rgb24>(file);
            int maxX = img.Width - 1;
            int maxY = img.Height - 1;
            x = Clamp(x, 0, maxX);
            y = Clamp(y, 0, maxY);
            w = Clamp(w, 1, maxX - x + 1);
            h = Clamp(h, 1, maxY - y + 1);

            var region = img.Clone(ctx => ctx.Crop(new Rectangle(x, y, w, h)));
            var scaled = region.Clone(ctx => ctx.Resize(w * scale, h * scale));

            if (output != null)
            {
                scaled.Save(output);
                Console.WriteLine($"Увеличенное изображение сохранено в {output}");
            }

            Console.WriteLine(Colorize($"🔍 Увеличенная область: {w}×{h} пикселей, масштаб: {scale}x", "bold"));

            for (int row = 0; row < scaled.Height; row++)
            {
                string line = "";
                for (int col = 0; col < scaled.Width; col++)
                {
                    var pixel = scaled[col, row];
                    byte r = pixel.R, g = pixel.G, b = pixel.B;
                    if (!noColor)
                    {
                        line += RgbToAnsi(r, g, b) + "  ";
                    }
                    else
                    {
                        double brightness = 0.299 * r + 0.587 * g + 0.114 * b;
                        if (brightness > 200) line += "██";
                        else if (brightness > 150) line += "▓▓";
                        else if (brightness > 100) line += "▒▒";
                        else if (brightness > 50) line += "░░";
                        else line += "  ";
                    }
                }
                if (!noColor) line += "\x1b[0m";
                Console.WriteLine(line);
            }
        }
        catch (Exception e)
        {
            Console.WriteLine($"Ошибка: {e.Message}");
        }
    }
}

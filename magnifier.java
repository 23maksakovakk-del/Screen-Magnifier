// magnifier.java
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.*;
import java.util.*;

public class magnifier {
    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";

    private static String colorize(String text, String color) {
        return color + text + RESET;
    }

    private static String rgbToAnsi(int r, int g, int b) {
        return String.format("\u001B[48;2;%d;%d;%dm", r, g, b);
    }

    private static int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(val, max));
    }

    public static void main(String[] args) throws IOException {
        String file = null;
        int x = 0, y = 0, w = 40, h = 20, scale = 4;
        boolean noColor = false;
        String output = null;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-x") && i+1 < args.length) x = Integer.parseInt(args[++i]);
            else if (arg.equals("-y") && i+1 < args.length) y = Integer.parseInt(args[++i]);
            else if (arg.equals("-w") && i+1 < args.length) w = Integer.parseInt(args[++i]);
            else if (arg.equals("-h") && i+1 < args.length) h = Integer.parseInt(args[++i]);
            else if (arg.equals("-s") || arg.equals("--scale")) scale = Integer.parseInt(args[++i]);
            else if (arg.equals("--no-color")) noColor = true;
            else if (arg.equals("--output") && i+1 < args.length) output = args[++i];
            else if (arg.equals("-h") || arg.equals("--help")) {
                System.out.println("Usage: java magnifier <file> [-x X] [-y Y] [-w W] [-h H] [-s SCALE] [--no-color] [--output FILE]");
                return;
            } else if (file == null) file = arg;
        }
        if (file == null) {
            System.err.println("Укажите файл изображения.");
            System.exit(1);
        }

        try {
            BufferedImage img = ImageIO.read(new File(file));
            int maxX = img.getWidth() - 1;
            int maxY = img.getHeight() - 1;
            x = clamp(x, 0, maxX);
            y = clamp(y, 0, maxY);
            w = clamp(w, 1, maxX - x + 1);
            h = clamp(h, 1, maxY - y + 1);

            // Извлечение области
            BufferedImage region = img.getSubimage(x, y, w, h);
            // Масштабирование
            BufferedImage scaled = new BufferedImage(w * scale, h * scale, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = scaled.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(region, 0, 0, w * scale, h * scale, null);
            g2d.dispose();

            if (output != null) {
                ImageIO.write(scaled, "png", new File(output));
                System.out.println("Увеличенное изображение сохранено в " + output);
            }

            System.out.println(colorize("🔍 Увеличенная область: " + w + "×" + h + " пикселей, масштаб: " + scale + "x", BOLD));

            for (int row = 0; row < scaled.getHeight(); row++) {
                StringBuilder line = new StringBuilder();
                for (int col = 0; col < scaled.getWidth(); col++) {
                    int rgb = scaled.getRGB(col, row);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    if (!noColor) {
                        line.append(rgbToAnsi(r, g, b)).append("  ");
                    } else {
                        double brightness = 0.299 * r + 0.587 * g + 0.114 * b;
                        if (brightness > 200) line.append("██");
                        else if (brightness > 150) line.append("▓▓");
                        else if (brightness > 100) line.append("▒▒");
                        else if (brightness > 50) line.append("░░");
                        else line.append("  ");
                    }
                }
                if (!noColor) line.append(RESET);
                System.out.println(line.toString());
            }
        } catch (Exception e) {
            System.err.println("Ошибка: " + e.getMessage());
            System.exit(1);
        }
    }
}

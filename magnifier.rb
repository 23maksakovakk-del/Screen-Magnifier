#!/usr/bin/env ruby
# magnifier.rb
# encoding: UTF-8

require 'rmagick'
require 'optparse'

COLORS = {
  reset: "\e[0m",
  bold: "\e[1m"
}

def colorize(text, color)
  "#{COLORS[color]}#{text}#{COLORS[:reset]}"
end

def rgb_to_ansi(r, g, b)
  "\e[48;2;#{r};#{g};#{b}m"
end

def clamp(val, min, max)
  [[val, min].max, max].min
end

def main
  options = { x: 0, y: 0, w: 40, h: 20, scale: 4, no_color: false, output: nil }
  file = nil

  OptionParser.new do |opts|
    opts.banner = "Usage: ruby magnifier.rb <file> [options]"
    opts.on("-x X", Integer, "Координата X") { |v| options[:x] = v }
    opts.on("-y Y", Integer, "Координата Y") { |v| options[:y] = v }
    opts.on("-w W", Integer, "Ширина области") { |v| options[:w] = v }
    opts.on("-h H", Integer, "Высота области") { |v| options[:h] = v }
    opts.on("-s SCALE", Integer, "Коэффициент увеличения") { |v| options[:scale] = v }
    opts.on("--no-color", "Отключить цвет") { options[:no_color] = true }
    opts.on("--output FILE", "Сохранить в файл") { |v| options[:output] = v }
    opts.on("-h", "--help", "Справка") { puts opts; exit }
  end.parse!

  file = ARGV[0] || file
  unless file
    puts "Укажите файл изображения."
    exit 1
  end

  begin
    img = Magick::Image.read(file).first
    max_x = img.columns - 1
    max_y = img.rows - 1

    x = clamp(options[:x], 0, max_x)
    y = clamp(options[:y], 0, max_y)
    w = clamp(options[:w], 1, max_x - x + 1)
    h = clamp(options[:h], 1, max_y - y + 1)

    region = img.crop(x, y, w, h)
    scaled = region.scale(w * options[:scale], h * options[:scale])

    if options[:output]
      scaled.write(options[:output])
      puts "Увеличенное изображение сохранено в #{options[:output]}"
    end

    puts colorize("🔍 Увеличенная область: #{w}×#{h} пикселей, масштаб: #{options[:scale]}x", :bold)

    (0...scaled.rows).each do |row|
      line = ""
      (0...scaled.columns).each do |col|
        pixel = scaled.pixel_color(col, row)
        r = pixel.red / 257  # Magick использует 16-битные значения
        g = pixel.green / 257
        b = pixel.blue / 257
        if !options[:no_color]
          line += rgb_to_ansi(r, g, b) + "  "
        else
          brightness = 0.299 * r + 0.587 * g + 0.114 * b
          if brightness > 200
            line += "██"
          elsif brightness > 150
            line += "▓▓"
          elsif brightness > 100
            line += "▒▒"
          elsif brightness > 50
            line += "░░"
          else
            line += "  "
          end
        end
      end
      line += COLORS[:reset] unless options[:no_color]
      puts line
    end
  rescue => e
    puts "Ошибка: #{e.message}"
    exit 1
  end
end

main if __FILE__ == $0

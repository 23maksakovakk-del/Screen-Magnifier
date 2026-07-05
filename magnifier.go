// magnifier.go
package main

import (
	"flag"
	"fmt"
	"image"
	"image/color"
	_ "image/jpeg"
	_ "image/png"
	"os"
	"strings"

	"github.com/disintegration/imaging"
)

const (
	reset = "\033[0m"
	bold  = "\033[1m"
)

func colorize(text, color string) string {
	return color + text + reset
}

func rgbToAnsi(r, g, b uint8) string {
	return fmt.Sprintf("\033[48;2;%d;%d;%dm", r, g, b)
}

func clamp(val, minVal, maxVal int) int {
	if val < minVal {
		return minVal
	}
	if val > maxVal {
		return maxVal
	}
	return val
}

func main() {
	var (
		file     string
		x, y     int
		w, h     int
		scale    int
		noColor  bool
		output   string
	)
	flag.StringVar(&file, "file", "", "Файл изображения")
	flag.IntVar(&x, "x", 0, "Координата X")
	flag.IntVar(&y, "y", 0, "Координата Y")
	flag.IntVar(&w, "w", 40, "Ширина области")
	flag.IntVar(&h, "h", 20, "Высота области")
	flag.IntVar(&scale, "s", 4, "Коэффициент увеличения")
	flag.BoolVar(&noColor, "no-color", false, "Отключить цвет")
	flag.StringVar(&output, "output", "", "Сохранить в файл")
	flag.Parse()

	if file == "" && flag.NArg() > 0 {
		file = flag.Arg(0)
	}
	if file == "" {
		fmt.Println("Укажите файл изображения.")
		flag.Usage()
		os.Exit(1)
	}

	// Чтение изображения
	src, err := imaging.Open(file)
	if err != nil {
		fmt.Printf("Ошибка загрузки: %v\n", err)
		os.Exit(1)
	}
	bounds := src.Bounds()
	maxX := bounds.Dx() - 1
	maxY := bounds.Dy() - 1

	x = clamp(x, 0, maxX)
	y = clamp(y, 0, maxY)
	w = clamp(w, 1, maxX-x+1)
	h = clamp(h, 1, maxY-y+1)

	// Извлечение области
	region := imaging.Crop(src, image.Rect(x, y, x+w, y+h))
	// Масштабирование
	scaled := imaging.Resize(region, w*scale, h*scale, imaging.Lanczos)

	if output != "" {
		err = imaging.Save(scaled, output)
		if err != nil {
			fmt.Printf("Ошибка сохранения: %v\n", err)
		} else {
			fmt.Printf("Увеличенное изображение сохранено в %s\n", output)
		}
	}

	fmt.Printf("%s\n", colorize(fmt.Sprintf("🔍 Увеличенная область: %d×%d пикселей, масштаб: %dx", w, h, scale), bold))

	bounds2 := scaled.Bounds()
	for row := bounds2.Min.Y; row < bounds2.Max.Y; row++ {
		var line strings.Builder
		for col := bounds2.Min.X; col < bounds2.Max.X; col++ {
			c := scaled.At(col, row)
			r, g, b, _ := c.RGBA()
			r8 := uint8(r >> 8)
			g8 := uint8(g >> 8)
			b8 := uint8(b >> 8)
			if !noColor {
				line.WriteString(rgbToAnsi(r8, g8, b8) + "  ")
			} else {
				brightness := int(0.299*float64(r8) + 0.587*float64(g8) + 0.114*float64(b8))
				if brightness > 200 {
					line.WriteString("██")
				} else if brightness > 150 {
					line.WriteString("▓▓")
				} else if brightness > 100 {
					line.WriteString("▒▒")
				} else if brightness > 50 {
					line.WriteString("░░")
				} else {
					line.WriteString("  ")
				}
			}
		}
		if !noColor {
			line.WriteString(reset)
		}
		fmt.Println(line.String())
	}
}

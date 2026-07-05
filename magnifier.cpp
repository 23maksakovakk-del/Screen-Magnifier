// magnifier.cpp
#include <iostream>
#include <string>
#include <vector>
#include <opencv2/opencv.hpp>
#include <opencv2/imgproc.hpp>

using namespace std;
using namespace cv;

// ANSI-коды
const string RESET = "\033[0m";
const string BOLD = "\033[1m";

string colorize(const string& text, const string& color) {
    return color + text + RESET;
}

string rgbToAnsi(int r, int g, int b) {
    return "\033[48;2;" + to_string(r) + ";" + to_string(g) + ";" + to_string(b) + "m";
}

int clamp(int val, int minVal, int maxVal) {
    return max(minVal, min(val, maxVal));
}

int main(int argc, char* argv[]) {
    string file;
    int x = 0, y = 0, w = 40, h = 20, scale = 4;
    bool noColor = false;
    string outputFile;

    for (int i = 1; i < argc; ++i) {
        string arg = argv[i];
        if (arg == "-x" && i+1 < argc) x = stoi(argv[++i]);
        else if (arg == "-y" && i+1 < argc) y = stoi(argv[++i]);
        else if (arg == "-w" && i+1 < argc) w = stoi(argv[++i]);
        else if (arg == "-h" && i+1 < argc) h = stoi(argv[++i]);
        else if (arg == "-s" || arg == "--scale") scale = stoi(argv[++i]);
        else if (arg == "--no-color") noColor = true;
        else if (arg == "--output" && i+1 < argc) outputFile = argv[++i];
        else if (arg == "-h" || arg == "--help") {
            cout << "Usage: magnifier <file> [-x X] [-y Y] [-w W] [-h H] [-s SCALE] [--no-color] [--output FILE]" << endl;
            return 0;
        } else if (file.empty()) file = arg;
    }
    if (file.empty()) {
        cerr << "Укажите файл изображения." << endl;
        return 1;
    }

    Mat img = imread(file, IMREAD_COLOR);
    if (img.empty()) {
        cerr << "Не удалось загрузить изображение." << endl;
        return 1;
    }

    // Ограничения
    x = clamp(x, 0, img.cols - 1);
    y = clamp(y, 0, img.rows - 1);
    w = clamp(w, 1, img.cols - x);
    h = clamp(h, 1, img.rows - y);

    Rect region(x, y, w, h);
    Mat roi = img(region);
    Mat scaled;
    resize(roi, scaled, Size(w * scale, h * scale), 0, 0, INTER_LINEAR);

    if (!outputFile.empty()) {
        imwrite(outputFile, scaled);
        cout << "Увеличенное изображение сохранено в " << outputFile << endl;
    }

    cout << colorize("🔍 Увеличенная область: " + to_string(w) + "×" + to_string(h) + " пикселей, масштаб: " + to_string(scale) + "x", BOLD) << endl;

    for (int row = 0; row < scaled.rows; ++row) {
        string line;
        for (int col = 0; col < scaled.cols; ++col) {
            Vec3b pixel = scaled.at<Vec3b>(row, col);
            int b = pixel[0], g = pixel[1], r = pixel[2];
            if (!noColor) {
                line += rgbToAnsi(r, g, b) + "  "; // два пробела
            } else {
                int brightness = (int)(0.299*r + 0.587*g + 0.114*b);
                if (brightness > 200) line += "██";
                else if (brightness > 150) line += "▓▓";
                else if (brightness > 100) line += "▒▒";
                else if (brightness > 50) line += "░░";
                else line += "  ";
            }
        }
        if (!noColor) line += RESET;
        cout << line << endl;
    }
    return 0;
}

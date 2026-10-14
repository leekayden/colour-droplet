import java.awt.Color;

/** Colour conversions shared by the app and the picker overlay. */
final class ColorMath {

    private ColorMath() {
    }

    static String toHex(Color c) {
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }

    /** Returns {hue 0-360, saturation 0-100, lightness 0-100} in HSL space. */
    static float[] toHsl(Color c) {
        float r = c.getRed() / 255f, g = c.getGreen() / 255f, b = c.getBlue() / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float l = (max + min) / 2f;
        float h, s;
        if (max == min) {
            h = 0f;
            s = 0f;
        } else {
            float d = max - min;
            s = l > 0.5f ? d / (2f - max - min) : d / (max + min);
            if (max == r) {
                h = ((g - b) / d) + (g < b ? 6 : 0);
            } else if (max == g) {
                h = ((b - r) / d) + 2;
            } else {
                h = ((r - g) / d) + 4;
            }
            h *= 60f;
        }
        return new float[]{h, s * 100f, l * 100f};
    }

    static String rgbString(Color c) {
        return String.format("R %d   G %d   B %d", c.getRed(), c.getGreen(), c.getBlue());
    }

    static String hslString(Color c) {
        float[] h = toHsl(c);
        return String.format("H %d\u00B0   S %d%%   L %d%%",
                Math.round(h[0]), Math.round(h[1]), Math.round(h[2]));
    }

    static String rgbCss(Color c) {
        return String.format("rgb(%d, %d, %d)", c.getRed(), c.getGreen(), c.getBlue());
    }

    static String hslCss(Color c) {
        float[] h = toHsl(c);
        return String.format("hsl(%d, %d%%, %d%%)",
                Math.round(h[0]), Math.round(h[1]), Math.round(h[2]));
    }
}

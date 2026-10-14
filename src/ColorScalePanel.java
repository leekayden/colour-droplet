import javax.swing.JPanel;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;

/**
 * Shows the picked colour on two scales: a full hue bar (0-360) with a marker
 * at the colour's hue, and a white-to-colour-to-black ramp with a marker at
 * the colour's lightness, so the colour visibly "sits" on its own scale.
 */
class ColorScalePanel extends JPanel {

    private static final int BAR_H = 24;
    private static final int LABEL_GAP = 6;
    private static final int ROW_GAP = 18;

    private Color color = new Color(0x3B82F6);
    private float[] hsl = {217f, 91f, 60f};

    ColorScalePanel() {
        setOpaque(false);
        setPreferredSize(new Dimension(120, 15 + LABEL_GAP + BAR_H + ROW_GAP + 15 + LABEL_GAP + BAR_H + 6));
    }

    void setColor(Color c) {
        this.color = c;
        this.hsl = ColorMath.toHsl(c);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int y = 0;

        y += drawLabel(g, "Hue \u2014 " + Math.round(hsl[0]) + "\u00B0", 0, y);
        y += LABEL_GAP;
        paintHueBar(g, 0, y, w, BAR_H);
        y += BAR_H + ROW_GAP;

        y += drawLabel(g, "Tint \u2194 shade \u2014 L " + Math.round(hsl[2]) + "%", 0, y);
        y += LABEL_GAP;
        paintRampBar(g, 0, y, w, BAR_H);

        g.dispose();
    }

    private int drawLabel(Graphics2D g, String text, int x, int y) {
        g.setFont(g.getFont().deriveFont(Font.PLAIN, 11.5f));
        Color c = UIManager.getColor("Label.disabledForeground");
        g.setColor(c != null ? c : new Color(0x6B7280));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, x, y + fm.getAscent());
        return fm.getHeight();
    }

    private void paintHueBar(Graphics2D g, int x, int y, int w, int h) {
        Shape clip = g.getClip();
        g.clip(new RoundRectangle2D.Float(x, y, w - 1, h, h, h));
        for (int i = 0; i < w; i++) {
            g.setColor(Color.getHSBColor(i / (float) w, 1f, 1f));
            g.drawLine(x + i, y, x + i, y + h);
        }
        g.setClip(clip);
        g.setColor(new Color(128, 128, 128, 150));
        g.draw(new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, w - 2, h - 1, h - 1, h - 1));
        drawMarker(g, x, y, w, h, hsl[0] / 360f);
    }

    private void paintRampBar(Graphics2D g, int x, int y, int w, int h) {
        float l = hsl[2] / 100f;
        Shape clip = g.getClip();
        g.clip(new RoundRectangle2D.Float(x, y, w - 1, h, h, h));
        for (int i = 0; i < w; i++) {
            float t = w <= 1 ? 0f : i / (float) (w - 1);
            if (t <= l) {
                g.setColor(lerp(Color.WHITE, color, l <= 0f ? 0f : t / l));
            } else {
                g.setColor(lerp(color, Color.BLACK, t >= 1f ? 1f : (t - l) / (1f - l)));
            }
            g.drawLine(x + i, y, x + i, y + h);
        }
        g.setClip(clip);
        g.setColor(new Color(128, 128, 128, 150));
        g.draw(new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, w - 2, h - 1, h - 1, h - 1));
        drawMarker(g, x, y, w, h, l);
    }

    /** Dark pill with a white ring, readable on any bar colour. */
    private void drawMarker(Graphics2D g, int x, int y, int w, int h, float t) {
        int mx = x + Math.round(t * (w - 1));
        g.setColor(new Color(0, 0, 0, 55));
        g.fill(new RoundRectangle2D.Float(mx - 3f, y - 5f, 8f, h + 12f, 8f, 8f));
        g.setColor(Color.WHITE);
        g.fill(new RoundRectangle2D.Float(mx - 4.5f, y - 6.5f, 9f, h + 13f, 9f, 9f));
        g.setColor(new Color(0x111827));
        g.fill(new RoundRectangle2D.Float(mx - 2.5f, y - 4.5f, 5f, h + 9f, 5f, 5f));
    }

    private static Color lerp(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int r = Math.round(a.getRed() + (b.getRed() - a.getRed()) * t);
        int g = Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl = Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t);
        return new Color(r, g, bl);
    }
}

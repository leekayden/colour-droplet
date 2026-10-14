import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/**
 * Full-screen overlay used while picking a colour. Shows a magnifier with the
 * pixel grid around the cursor; a left click captures the colour under the
 * cursor, right click or Escape cancels.
 *
 * Click capture needs the window to be hit-testable at the cursor, which means
 * its pixels there need alpha > 0 &mdash; but those are the same screen pixels
 * Robot reads back. So the overlay paints an invisible veil in the colour it
 * last sampled (plus one per channel): blended over the screen, a veil that
 * matches the underlying colour is an identity operation no matter how low its
 * alpha, so the reading stays exact instead of being tinted by the veil. Each
 * tick nudges the veil colour onto the freshly sampled one, so it converges
 * within a frame or two and stays exact while the cursor rests.
 */
class PickerOverlay extends JFrame {

    interface Listener {
        void onPick(Color color, Point screenPoint);

        void onCancel();
    }

    private static final int ZOOM = 16;      // magnifier pixels per screen pixel
    private static final int RADIUS = 7;     // screen pixels shown around the cursor
    private static final int BOX_GAP = 26;   // gap between cursor and magnifier
    private static final int INFO_H = 34;    // magnifier info strip height
    private static final int VEIL_ALPHA = 20;
    private static final int TICK_MS = 25;

    private final Robot robot;
    private final Listener listener;
    private final Timer timer;

    private Point cursor = new Point();
    private Color sampled = Color.WHITE;
    private BufferedImage zoom;      // last capture around the cursor

    PickerOverlay(Robot robot, Listener listener) {
        super("Colour Droplet picker");
        this.robot = robot;
        this.listener = listener;

        GraphicsDevice gd = getGraphicsConfiguration().getDevice();
        if (!gd.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT)) {
            throw new IllegalStateException(
                    "This desktop does not support the per-pixel transparency the picker needs.");
        }

        setUndecorated(true);
        setType(Window.Type.UTILITY);
        setAlwaysOnTop(true);
        setBounds(unionOfAllScreens());
        setBackground(new Color(0, 0, 0, 1));   // per-pixel translucency, near-invisible
        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

        JPanel canvas = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.translate(-getX(), -getY());   // draw in screen coordinates
                paintOverlay(g2);
                g2.dispose();
            }
        };
        canvas.setOpaque(false);
        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    pick();
                } else if (e.getButton() == MouseEvent.BUTTON3) {
                    cancel();
                }
            }
        });
        setContentPane(canvas);

        JRootPane root = getRootPane();
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("ESCAPE"), "cancel");
        root.getActionMap().put("cancel", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });

        timer = new Timer(TICK_MS, e -> tick());
    }

    /** Show the overlay and start sampling. Must run on the EDT. */
    void begin() {
        timer.stop();
        zoom = null;
        sample();                 // seed the veil colour before the window appears
        setVisible(true);
        toFront();
        requestFocus();
        repaint();                // paint the veil across the whole virtual screen once
        SwingUtilities.invokeLater(() -> {
            toFront();
            requestFocus();
        });
        timer.start();
    }

    private void sample() {
        PointerInfo info = MouseInfo.getPointerInfo();
        if (info == null) {
            return;
        }
        cursor = info.getLocation();
        try {
            sampled = robot.getPixelColor(cursor.x, cursor.y);
            zoom = robot.createScreenCapture(new Rectangle(cursor.x - RADIUS, cursor.y - RADIUS,
                    RADIUS * 2 + 1, RADIUS * 2 + 1));
        } catch (RuntimeException ignored) {
            // secure desktop or locked session — keep the last good reading
        }
    }

    private void tick() {
        sample();
        // Repaint the whole window every tick: the layered window is submitted
        // as one buffer, so repainting only the moved region leaves stale-frame
        // trails behind the magnifier.
        repaint();
    }

    private void pick() {
        timer.stop();
        Color c = sampled;
        try {
            c = robot.getPixelColor(cursor.x, cursor.y);
        } catch (RuntimeException ignored) {
        }
        setVisible(false);
        listener.onPick(c, new Point(cursor));
    }

    private void cancel() {
        timer.stop();
        setVisible(false);
        listener.onCancel();
    }

    private void paintOverlay(Graphics2D g) {
        Graphics2D veil = (Graphics2D) g.create();
        veil.setComposite(AlphaComposite.Src);
        veil.setColor(veilColor());
        veil.fillRect(getX(), getY(), getWidth(), getHeight());
        veil.dispose();

        if (zoom != null) {
            paintMagnifier(g);
        }
    }

    private Color veilColor() {
        return new Color(Math.min(255, sampled.getRed() + 1),
                Math.min(255, sampled.getGreen() + 1),
                Math.min(255, sampled.getBlue() + 1), VEIL_ALPHA);
    }

    private void paintMagnifier(Graphics2D g) {
        Rectangle box = magnifierBounds();
        int src = RADIUS * 2 + 1;
        int img = src * ZOOM;
        int bx = box.x;
        int by = box.y;

        g.setColor(new Color(15, 16, 20, 242));
        g.fill(new java.awt.geom.RoundRectangle2D.Float(bx - 6, by - 6, box.width, box.height, 12, 12));

        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(zoom, bx, by, img, img, null);

        g.setColor(new Color(255, 255, 255, 30));
        for (int i = 1; i < src; i++) {
            g.drawLine(bx + i * ZOOM, by, bx + i * ZOOM, by + img - 1);
            g.drawLine(bx, by + i * ZOOM, bx + img - 1, by + i * ZOOM);
        }

        g.setColor(new Color(0, 0, 0, 170));
        g.drawRect(bx + RADIUS * ZOOM - 2, by + RADIUS * ZOOM - 2, ZOOM + 3, ZOOM + 3);
        g.setColor(Color.WHITE);
        g.drawRect(bx + RADIUS * ZOOM - 1, by + RADIUS * ZOOM - 1, ZOOM + 1, ZOOM + 1);

        int iy = by + img + 7;
        g.setColor(sampled);
        g.fillRect(bx + 4, iy + 1, 18, 20);
        g.setColor(new Color(255, 255, 255, 70));
        g.drawRect(bx + 4, iy + 1, 18, 20);

        g.setColor(new Color(232, 234, 240));
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
        g.drawString(ColorMath.toHex(sampled), bx + 30, iy + 12);
        g.setColor(new Color(150, 155, 168));
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        g.drawString(sampled.getRed() + ", " + sampled.getGreen() + ", " + sampled.getBlue()
                + "   \u2022   " + cursor.x + ", " + cursor.y, bx + 30, iy + 24);
    }

    /** Magnifier box in screen coords, flipped/clamped so it never covers the cursor. */
    private Rectangle magnifierBounds() {
        int img = (RADIUS * 2 + 1) * ZOOM;
        int w = img + 12;
        int h = img + 12 + INFO_H;
        Rectangle screen = screenContaining(cursor);
        int bx = cursor.x + BOX_GAP;
        int by = cursor.y + BOX_GAP;
        if (bx + w > screen.x + screen.width - 8) {
            bx = cursor.x - BOX_GAP - w;
        }
        if (by + h > screen.y + screen.height - 8) {
            by = cursor.y - BOX_GAP - h;
        }
        if (bx < screen.x + 8) {
            bx = screen.x + 8;
        }
        if (by < screen.y + 8) {
            by = screen.y + 8;
        }
        if (bx - 6 < cursor.x && bx + w + 6 > cursor.x && by - 6 < cursor.y && by + h + 6 > cursor.y) {
            bx = screen.x + 8;
            by = screen.y + 8;
        }
        return new Rectangle(bx, by, w, h);
    }

    private Rectangle screenContaining(Point p) {
        for (GraphicsDevice device : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
            for (GraphicsConfiguration gc : device.getConfigurations()) {
                if (gc.getBounds().contains(p)) {
                    return gc.getBounds();
                }
            }
        }
        return getGraphicsConfiguration().getBounds();
    }

    private static Rectangle unionOfAllScreens() {
        Rectangle bounds = null;
        for (GraphicsDevice device : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
            for (GraphicsConfiguration gc : device.getConfigurations()) {
                Rectangle b = gc.getBounds();   // shared instance — copy it
                bounds = bounds == null ? new Rectangle(b) : bounds.union(b);
            }
        }
        return bounds != null ? bounds : new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
    }
}

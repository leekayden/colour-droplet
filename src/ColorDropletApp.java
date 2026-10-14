import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;

/**
 * Colour Droplet — a tiny desktop colour picker.
 * Click the droplet, then click anywhere on screen to capture the pixel colour
 * underneath; the hex/RGB/HSL values and the colour scales update, and the hex
 * can land on the clipboard automatically.
 */
public class ColorDropletApp extends JFrame {

    private static final Color ACCENT = new Color(0x2563EB);
    private static final Font MONO = monoFont();

    private final Robot robot;
    private PickerOverlay overlay;

    private Color current = new Color(0x3B82F6);
    private final Deque<Color> history = new ArrayDeque<>();
    private Timer statusTimer;

    private SwatchPanel swatch;
    private JTextField hexValue;
    private JTextField rgbValue;
    private JTextField hslValue;
    private ColorScalePanel scales;
    private JPanel historyStrip;
    private JCheckBox autoCopy;
    private JLabel status;

    public ColorDropletApp(Robot robot) {
        super("Colour Droplet");
        this.robot = robot;
        setIconImage(makeWindowIcon());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setContentPane(buildContent());
        pack();
        if (getWidth() < 520) {
            setSize(520, getHeight());
        }
        setMinimumSize(new Dimension(460, getHeight()));
        setLocationRelativeTo(null);
        applyColor(current);
        rebuildHistory();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // FlatLaf dark theme; its window decorations make the title bar dark too.
            try {
                UIManager.setLookAndFeel(new FlatDarkLaf());
            } catch (Exception ex) {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {
                }
            }
            if (GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(null, "Colour Droplet needs a graphical desktop session.");
                return;
            }
            try {
                new ColorDropletApp(new Robot()).setVisible(true);
            } catch (AWTException ex) {
                JOptionPane.showMessageDialog(null,
                        "Could not initialise screen access:\n" + ex.getMessage(),
                        "Colour Droplet", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    // ------------------------------------------------------------------ UI

    private JPanel buildContent() {
        JPanel root = new JPanel(new BorderLayout());

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        body.add(buildPickRow());
        body.add(Box.createVerticalStrut(10));
        body.add(buildCurrentColourGroup());
        body.add(Box.createVerticalStrut(10));
        body.add(buildScalesGroup());
        body.add(Box.createVerticalStrut(10));
        body.add(buildRecentGroup());
        body.add(Box.createVerticalStrut(6));
        body.add(buildOptionsRow());

        root.add(body, BorderLayout.CENTER);
        root.add(buildStatusBar(), BorderLayout.SOUTH);
        return root;
    }

    private JComponent buildPickRow() {
        JPanel row = new JPanel(new BorderLayout());
        JButton pick = new JButton("Pick a colour", new DropletIcon(16, ACCENT));
        pick.setMnemonic('P');
        pick.addActionListener(e -> startPicking());
        getRootPane().setDefaultButton(pick);
        row.add(pick, BorderLayout.WEST);
        return row;
    }

    private JComponent buildCurrentColourGroup() {
        swatch = new SwatchPanel();
        hexValue = valueField(9, true);
        rgbValue = valueField(20, false);
        hslValue = valueField(20, false);

        JPanel grid = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.gridheight = 3;
        gc.anchor = GridBagConstraints.NORTH;
        gc.insets = new Insets(4, 0, 2, 14);
        grid.add(swatch, gc);

        addValueRow(grid, 0, "Hex:", hexValue, () -> ColorMath.toHex(current));
        addValueRow(grid, 1, "RGB:", rgbValue, () -> ColorMath.rgbCss(current));
        addValueRow(grid, 2, "HSL:", hslValue, () -> ColorMath.hslCss(current));

        return titledGroup("Current colour", grid);
    }

    private void addValueRow(JPanel grid, int row, String name, JTextField field, Supplier<String> copyText) {
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 1;
        gc.gridy = row;
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(2, 0, 2, 8);
        grid.add(new JLabel(name), gc);

        gc = new GridBagConstraints();
        gc.gridx = 2;
        gc.gridy = row;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(2, 0, 2, 8);
        grid.add(field, gc);

        gc = new GridBagConstraints();
        gc.gridx = 3;
        gc.gridy = row;
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(2, 0, 2, 0);
        grid.add(copyButton(copyText), gc);
    }

    private JComponent buildScalesGroup() {
        scales = new ColorScalePanel();
        return titledGroup("Colour scales", scales);
    }

    private JComponent buildRecentGroup() {
        historyStrip = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        return titledGroup("Recent", historyStrip);
    }

    private JComponent buildOptionsRow() {
        autoCopy = new JCheckBox("Copy hex to clipboard when picking", true);
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.add(autoCopy);
        return row;
    }

    private JComponent buildStatusBar() {
        status = new JLabel(" ");
        JPanel bar = new JPanel(new BorderLayout());
        Color line = UIManager.getColor("controlShadow");
        if (line == null) {
            line = Color.GRAY;
        }
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, line),
                BorderFactory.createEmptyBorder(3, 10, 3, 10)));
        bar.add(status, BorderLayout.WEST);
        return bar;
    }

    private JPanel titledGroup(String title, JComponent content) {
        JPanel group = new JPanel(new BorderLayout());
        group.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(title),
                BorderFactory.createEmptyBorder(2, 8, 6, 8)));
        group.add(content, BorderLayout.CENTER);
        return group;
    }

    private JTextField valueField(int columns, boolean bold) {
        JTextField f = new JTextField();
        f.setEditable(false);
        f.setColumns(columns);
        f.setFont(bold ? MONO.deriveFont(Font.BOLD) : MONO);
        return f;
    }

    private JButton copyButton(Supplier<String> text) {
        JButton b = new JButton("Copy");
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> {
            String s = text.get();
            copyToClipboard(s);
            flashStatus("Copied " + s);
        });
        return b;
    }

    // ------------------------------------------------------------- actions

    private void startPicking() {
        if (overlay == null) {
            try {
                overlay = new PickerOverlay(robot, new PickerOverlay.Listener() {
                    @Override
                    public void onPick(Color color, Point p) {
                        finishPick(color, p);
                    }

                    @Override
                    public void onCancel() {
                        flashStatus("Cancelled \u2014 no colour picked");
                        setVisible(true);
                    }
                });
            } catch (IllegalStateException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(),
                        "Colour Droplet", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        setVisible(false);
        overlay.begin();
    }

    private void finishPick(Color c, Point p) {
        applyColor(c);
        if (history.isEmpty() || !history.peekFirst().equals(c)) {
            history.addFirst(c);
            while (history.size() > 14) {
                history.removeLast();
            }
            rebuildHistory();
        }
        String hex = ColorMath.toHex(c);
        String msg = "Picked " + hex + " at (" + p.x + ", " + p.y + ")";
        if (autoCopy.isSelected()) {
            copyToClipboard(hex);
            msg += " \u2014 copied to clipboard";
        }
        flashStatus(msg);
        setVisible(true);
        toFront();
    }

    private void applyColor(Color c) {
        current = c;
        swatch.color = c;
        swatch.repaint();
        hexValue.setText(ColorMath.toHex(c));
        rgbValue.setText(ColorMath.rgbString(c));
        hslValue.setText(ColorMath.hslString(c));
        scales.setColor(c);
    }

    private void rebuildHistory() {
        historyStrip.removeAll();
        if (history.isEmpty()) {
            historyStrip.add(new JLabel("Nothing picked yet"));
        } else {
            for (Color c : history) {
                historyStrip.add(new HistorySwatch(c));
            }
        }
        historyStrip.revalidate();
        historyStrip.repaint();
    }

    private void copyToClipboard(String s) {
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(s), null);
        } catch (IllegalStateException ex) {
            flashStatus("Clipboard unavailable");
        }
    }

    private void flashStatus(String text) {
        status.setText(text);
        if (statusTimer == null) {
            statusTimer = new Timer(4000, e -> status.setText(" "));
            statusTimer.setRepeats(false);
        }
        statusTimer.restart();
    }

    private Image makeWindowIcon() {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        new DropletIcon(56, ACCENT).paintIcon(null, g, 4, 4);
        g.dispose();
        return img;
    }

    /** Consolas when available (any Windows box), otherwise the generic mono font. */
    private static Font monoFont() {
        Font consolas = new Font("Consolas", Font.PLAIN, 12);
        return consolas.getFamily().equals("Dialog")
                ? new Font(Font.MONOSPACED, Font.PLAIN, 12)
                : consolas;
    }

    // ------------------------------------------------------ inner classes

    private static class DropletIcon implements Icon {
        private final int size;
        private final Color fill;

        DropletIcon(int size, Color fill) {
            this.size = size;
            this.fill = fill;
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }

        @Override
        public void paintIcon(Component c, Graphics g0, int x, int y) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            float w = size, h = size;
            float cx = x + w / 2f;
            float r = w * 0.30f;
            float cy = y + h - r - h * 0.08f;
            float tipY = y + h * 0.10f;
            float k = 0.5523f * r;
            float shoulder = tipY + (cy - r - tipY) * 0.85f;

            Path2D p = new Path2D.Float();
            p.moveTo(cx, tipY);
            p.curveTo(cx + r * 0.55f, shoulder, cx + r, cy - r * 0.9f, cx + r, cy);
            p.curveTo(cx + r, cy + k, cx + k, cy + r, cx, cy + r);
            p.curveTo(cx - k, cy + r, cx - r, cy + k, cx - r, cy);
            p.curveTo(cx - r, cy - r * 0.9f, cx - r * 0.55f, shoulder, cx, tipY);
            p.closePath();
            g.setColor(fill);
            g.fill(p);

            g.setColor(new Color(255, 255, 255, 90));
            g.fill(new Ellipse2D.Float(cx - r * 0.45f, cy - r * 0.25f, r * 0.38f, r * 0.55f));
            g.dispose();
        }
    }

    private class SwatchPanel extends JPanel {
        private Color color = current;

        SwatchPanel() {
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Click to copy the hex value");
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    String hex = ColorMath.toHex(SwatchPanel.this.color);
                    copyToClipboard(hex);
                    flashStatus("Copied " + hex);
                }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(72, 72);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(color);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(new Color(128, 128, 128, 160));
            g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            g.dispose();
        }
    }

    private class HistorySwatch extends JComponent {
        private final Color color;

        HistorySwatch(Color color) {
            this.color = color;
            setPreferredSize(new Dimension(24, 24));
            setToolTipText(ColorMath.toHex(color));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    applyColor(HistorySwatch.this.color);
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setColor(color);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(new Color(128, 128, 128, 160));
            g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            g.dispose();
        }
    }
}

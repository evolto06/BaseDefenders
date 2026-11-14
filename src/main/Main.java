package main;

import game.Game;
import game.InputEvent;
import game.Layout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferStrategy;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/** Opens the window and runs the frame loop. All game logic lives behind {@link Game}. */
public final class Main {
    private static final long FRAME_NANOS = 1_000_000_000L / 60;
    /** A sleep can overrun by a millisecond or more, so the last stretch before a frame is waited out. */
    private static final long SPIN_NANOS = 1_500_000;

    private final Game game = new Game();
    private final Canvas canvas = new Canvas();
    // How the logical screen is fitted into the canvas; read by the UI thread to map the mouse.
    private volatile double scale = 1, offsetX, offsetY;
    private boolean handShown;

    public static void main(String[] args) throws Exception {
        Main main = new Main();
        SwingUtilities.invokeAndWait(main::openWindow);
        main.run();
    }

    private void openWindow() {
        // Start at full size, or smaller if the screen cannot fit it.
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        double fit = Math.min(1, Math.min((screen.width - 20) / (double) Layout.WIDTH,
                (screen.height - 50) / (double) Layout.HEIGHT));
        canvas.setPreferredSize(new Dimension((int) (Layout.WIDTH * fit), (int) (Layout.HEIGHT * fit)));
        canvas.setBackground(Color.BLACK);
        canvas.setIgnoreRepaint(true);
        canvas.setFocusTraversalKeysEnabled(false);

        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) { post(InputEvent.Kind.MOVE, e, 0); }
            @Override public void mouseDragged(MouseEvent e) { post(InputEvent.Kind.MOVE, e, 0); }
            @Override public void mousePressed(MouseEvent e) { post(InputEvent.Kind.PRESS, e, e.getButton()); }
            @Override public void mouseReleased(MouseEvent e) { post(InputEvent.Kind.RELEASE, e, e.getButton()); }
        };
        canvas.addMouseListener(mouse);
        canvas.addMouseMotionListener(mouse);
        canvas.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                game.input.add(new InputEvent(InputEvent.Kind.KEY, 0, 0, e.getKeyCode()));
            }
        });

        JFrame frame = new JFrame("Base Defenders");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(canvas);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        canvas.createBufferStrategy(2);
        canvas.requestFocusInWindow();
    }

    private void post(InputEvent.Kind kind, MouseEvent e, int button) {
        int x = (int) Math.floor((e.getX() - offsetX) / scale);
        int y = (int) Math.floor((e.getY() - offsetY) / scale);
        game.input.add(new InputEvent(kind, x, y, button));
    }

    private void run() throws InterruptedException {
        long previous = System.nanoTime();
        long due = previous;
        long fpsWindowStart = previous;
        int frames = 0;
        while (!game.quit) {
            long now = System.nanoTime();
            game.frame((now - previous) / 1e9f);
            previous = now;
            render();
            if (game.cursorHand != handShown) {
                handShown = game.cursorHand;
                Cursor cursor = Cursor.getPredefinedCursor(handShown ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR);
                SwingUtilities.invokeLater(() -> canvas.setCursor(cursor));
            }

            frames++;
            if (now - fpsWindowStart >= 1_000_000_000L) {
                game.fps = frames;
                frames = 0;
                fpsWindowStart = now;
            }
            // Frames fall due on a steady beat, not a fixed pause after each one, so a slow frame
            // or an overlong sleep is made up on the next instead of dragging the rate down.
            due += FRAME_NANOS;
            if (due < System.nanoTime() - FRAME_NANOS) due = System.nanoTime();
            long wait;
            while ((wait = due - System.nanoTime()) > SPIN_NANOS) {
                long nap = wait - SPIN_NANOS;
                Thread.sleep(nap / 1_000_000, (int) (nap % 1_000_000));
            }
            while (due - System.nanoTime() > 0) Thread.yield();
        }
        System.exit(0);
    }

    private void render() {
        int width = canvas.getWidth(), height = canvas.getHeight();
        BufferStrategy strategy = canvas.getBufferStrategy();
        if (strategy == null || width <= 0 || height <= 0) return;
        scale = Math.min(width / (double) Layout.WIDTH, height / (double) Layout.HEIGHT);
        offsetX = (width - Layout.WIDTH * scale) / 2;
        offsetY = (height - Layout.HEIGHT * scale) / 2;
        do {
            do {
                Graphics2D g = (Graphics2D) strategy.getDrawGraphics();
                try {
                    g.setColor(Color.BLACK);
                    g.fillRect(0, 0, width, height);
                    g.translate(offsetX, offsetY);
                    g.scale(scale, scale);
                    g.clipRect(0, 0, Layout.WIDTH, Layout.HEIGHT);
                    // Pixel art stays crisp at whole-number scales and is smoothed otherwise.
                    double total = g.getTransform().getScaleX();
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, Math.abs(total - Math.rint(total)) < 0.01
                            ? RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
                            : RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    game.render(g);
                } finally {
                    g.dispose();
                }
            } while (strategy.contentsRestored());
            strategy.show();
        } while (strategy.contentsLost());
    }
}

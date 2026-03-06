package wordgame;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.Closeable;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class GameGUI extends JFrame {
    private static final int LEN = 5;
    private static final Color GREEN = new Color(34, 177, 76), YELLOW = new Color(255, 193, 7), GRAY = new Color(120, 120, 120);
    private static final String[] WORD_FILES = {"src/test.txt", "test.txt", "../../src/test.txt", "../test.txt", "src/full.txt", "full.txt", "../../src/full.txt", "../full.txt"};

    private Word secret;
    private final JTextField[] boxes = new JTextField[LEN];
    private final Map<Character, JButton> keys = new HashMap<>();
    private final Map<Character, Character> bestStatus = new HashMap<>();
    private JTextPane feedbackArea;
    private JButton submit;
    private JButton newGame;

    private boolean networkMode;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public GameGUI(Game game) {
        super("Wordle - Guess the Word");
        secret = resolveSecret(game);
        for (char c = 'a'; c <= 'z'; c++) bestStatus.put(c, null);
        setJMenuBar(buildMenuBar());
        setContentPane(buildRoot());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() { public void windowClosing(java.awt.event.WindowEvent e) { disconnect(); } });
        setSize(800, 600);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel buildRoot() {
        JLabel title = new JLabel("WORDLE", JLabel.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 24));

        JPanel top = new JPanel(new BorderLayout());
        top.add(title, BorderLayout.NORTH);
        top.add(buildInputRow(), BorderLayout.SOUTH);

        feedbackArea = new JTextPane();
        feedbackArea.setEditable(false);
        feedbackArea.setFont(new Font("Monospaced", Font.PLAIN, 16));
        feedbackArea.setBackground(new Color(240, 240, 240));

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(top, BorderLayout.NORTH);
        root.add(new JScrollPane(feedbackArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
        root.add(buildKeyboard(), BorderLayout.SOUTH);
        return root;
    }

    private JPanel buildInputRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        row.add(new JLabel("Enter guess:"));
        for (int i = 0; i < LEN; i++) {
            JTextField box = new JTextField(1);
            box.setFont(new Font("Arial", Font.BOLD, 28));
            box.setHorizontalAlignment(JTextField.CENTER);
            box.setPreferredSize(new Dimension(50, 50));
            box.setMaximumSize(new Dimension(50, 50));
            box.setBorder(new LineBorder(Color.BLACK, 2));
            ((AbstractDocument) box.getDocument()).setDocumentFilter(new LetterFilter(i));
            int index = i;
            box.addKeyListener(new KeyAdapter() {
                @Override public void keyPressed(KeyEvent e) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_BACK_SPACE -> { if (boxes[index].getText().isEmpty() && index > 0) { boxes[index - 1].requestFocus(); boxes[index - 1].selectAll(); } }
                        case KeyEvent.VK_RIGHT -> { if (index < LEN - 1 && !boxes[index].getText().isEmpty()) boxes[index + 1].requestFocus(); }
                        case KeyEvent.VK_LEFT -> { if (index > 0) boxes[index - 1].requestFocus(); }
                        case KeyEvent.VK_ENTER -> handleGuess();
                    }
                }
            });
            boxes[i] = box;
            row.add(box);
        }
        submit = new JButton("Submit");
        submit.addActionListener(e -> handleGuess());
        row.add(submit);

        newGame = new JButton("New Game");
        newGame.addActionListener(e -> resetGame());
        row.add(newGame);
        return row;
    }

    private JPanel buildKeyboard() {
        JPanel keyboard = new JPanel();
        keyboard.setLayout(new BoxLayout(keyboard, BoxLayout.Y_AXIS));
        keyboard.setBorder(BorderFactory.createTitledBorder("Alphabet"));
        keyboard.add(buildKeyRow("qwertyuiop")); keyboard.add(Box.createVerticalStrut(5));
        keyboard.add(buildKeyRow("asdfghjkl")); keyboard.add(Box.createVerticalStrut(5));
        keyboard.add(buildKeyRow("zxcvbnm"));
        return keyboard;
    }

    private JPanel buildKeyRow(String letters) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 3));
        for (char ch : letters.toCharArray()) {
            JButton button = new JButton(String.valueOf(Character.toUpperCase(ch)));
            button.setFont(new Font("Arial", Font.BOLD, 12));
            button.setPreferredSize(new Dimension(35, 35));
            button.setBackground(new Color(200, 200, 200));
            button.setOpaque(true);
            button.setBorder(new LineBorder(Color.BLACK, 1));
            button.setEnabled(false);
            keys.put(ch, button);
            row.add(button);
        }
        return row;
    }

    private class LetterFilter extends DocumentFilter {
        private final int index;
        private LetterFilter(int index) { this.index = index; }
        @Override public void insertString(FilterBypass fb, int offset, String text, AttributeSet attrs) throws BadLocationException { replace(fb, offset, 0, text, attrs); }
        @Override public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            String normalized = text == null ? "" : text.replaceAll("[^a-zA-Z]", "").toLowerCase();
            if (normalized.isEmpty()) { fb.replace(offset, length, "", attrs); return; }
            fb.remove(0, fb.getDocument().getLength());
            fb.insertString(0, String.valueOf(normalized.charAt(0)), attrs);
            if (index < LEN - 1) SwingUtilities.invokeLater(() -> boxes[index + 1].requestFocus());
        }
    }

    private void handleGuess() {
        String guess = readGuess();
        if (guess == null) return;
        Feedback fb = makeGuess(guess);
        String status = normalizeStatus(fb.colors);
        appendColoredGuess(guess, status);
        updateKeyboard(guess, status);
        if (fb.isCorrect()) {
            JOptionPane.showMessageDialog(this, "🎉 YOU WIN! The word was " + (secret == null ? "(unknown)" : secret.toString().toUpperCase()), "Congratulations", JOptionPane.INFORMATION_MESSAGE);
            for (JTextField box : boxes) box.setEditable(false);
            submit.setEnabled(false);
            return;
        }
        for (JTextField box : boxes) box.setText("");
        boxes[0].requestFocus();
    }

    private String readGuess() {
        StringBuilder guess = new StringBuilder(LEN);
        for (JTextField box : boxes) {
            String value = box.getText().trim().toLowerCase();
            if (value.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill all 5 letter boxes.", "Incomplete Guess", JOptionPane.WARNING_MESSAGE);
                return null;
            }
            guess.append(value.charAt(0));
        }
        return guess.toString();
    }

    private Feedback makeGuess(String guessText) {
        if (networkMode && out != null && in != null) {
            try {
                out.writeObject(new Word(guessText));
                out.flush();
                Object response = in.readObject();
                if (response instanceof Feedback) return (Feedback) response;
            } catch (Exception exception) {
                disconnect();
                JOptionPane.showMessageDialog(this, "Network guess failed: " + exception.getMessage(), "Network Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        if (secret == null) secret = resolveSecret(new Game());
        return secret.compareTo(new Word(guessText));
    }

    private void appendColoredGuess(String guess, String status) {
        try {
            for (int i = 0; i < guess.length(); i++) {
                char code = status.charAt(i);
                SimpleAttributeSet attrs = new SimpleAttributeSet();
                StyleConstants.setBackground(attrs, code == 'G' ? GREEN : code == 'Y' ? YELLOW : GRAY);
                StyleConstants.setForeground(attrs, code == 'Y' ? Color.BLACK : Color.WHITE);
                StyleConstants.setFontFamily(attrs, Font.MONOSPACED);
                StyleConstants.setFontSize(attrs, 20);
                StyleConstants.setBold(attrs, true);
                feedbackArea.getDocument().insertString(feedbackArea.getDocument().getLength(), " " + Character.toUpperCase(guess.charAt(i)) + " ", attrs);
            }
            feedbackArea.getDocument().insertString(feedbackArea.getDocument().getLength(), "\n", null);
        } catch (BadLocationException exception) {
            JOptionPane.showMessageDialog(this, "Unable to render feedback: " + exception.getMessage(), "Render Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void resetGame() {
        secret = resolveSecret(new Game());
        feedbackArea.setText("");

        for (JTextField box : boxes) {
            box.setText("");
            box.setEditable(true);
        }

        for (char c = 'a'; c <= 'z'; c++) {
            bestStatus.put(c, null);
            JButton key = keys.get(c);
            if (key != null) {
                key.setBackground(new Color(200, 200, 200));
                key.setForeground(Color.BLACK);
            }
        }

        submit.setEnabled(true);
        boxes[0].requestFocus();
    }

    private void updateKeyboard(String guess, String status) {
        for (int i = 0; i < guess.length(); i++) {
            char letter = guess.charAt(i), next = status.charAt(i);
            Character current = bestStatus.get(letter);
            if (current == null || next == 'G' || (next == 'Y' && current != 'G')) {
                bestStatus.put(letter, next);
                JButton key = keys.get(letter);
                if (key != null) {
                    key.setBackground(next == 'G' ? GREEN : next == 'Y' ? YELLOW : GRAY);
                    key.setForeground(next == 'Y' ? Color.BLACK : Color.WHITE);
                }
            }
        }
    }

    private String normalizeStatus(char[] colors) {
        StringBuilder status = new StringBuilder(colors.length);
        for (char c : colors) status.append((c == 'G' || c == 'Y' || c == 'B') ? c : 'B');
        return status.toString();
    }

    private JMenuBar buildMenuBar() {
        JMenuItem host = new JMenuItem("Host Game");
        host.addActionListener(e -> {
            new Thread(() -> {
                try {
                    Method serverStart = GameServer.class.getDeclaredMethod("serverStart");
                    serverStart.setAccessible(true);
                    serverStart.invoke(new GameServer());
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Failed to start server: " + ex.getMessage(), "Network Error", JOptionPane.ERROR_MESSAGE));
                }
            }, "game-server-thread").start();
            JOptionPane.showMessageDialog(this, "Server started (console output).", "Network", JOptionPane.INFORMATION_MESSAGE);
        });

        JMenuItem join = new JMenuItem("Join Game...");
        join.addActionListener(e -> {
            String hostInput = JOptionPane.showInputDialog(this, "Enter server host (default localhost):", "localhost");
            if (hostInput == null) return;
            String hostName = hostInput.trim().isEmpty() ? "localhost" : hostInput.trim();
            disconnect();
            try {
                socket = new Socket(hostName, 5000);
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());
                networkMode = true;
                JOptionPane.showMessageDialog(this, "Connected to server. Type guesses as usual.", "Network", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                disconnect();
                JOptionPane.showMessageDialog(this, "Failed to connect: " + ex.getMessage(), "Network Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JMenu menu = new JMenu("Network");
        menu.add(host);
        menu.add(join);
        JMenuBar bar = new JMenuBar();
        bar.add(menu);
        return bar;
    }

    private Word resolveSecret(Game game) {
        try {
            Field field = game.getClass().getDeclaredField("secret");
            field.setAccessible(true);
            Object value = field.get(game);
            if (value instanceof Word) return (Word) value;
        } catch (Exception ignored) { }

        for (String file : WORD_FILES) {
            try {
                if (Files.exists(Path.of(file))) return new WordList(file).getRandom();
            } catch (Exception ignored) { }
        }
        return new Word("apple");
    }

    private void disconnect() {
        networkMode = false;
        closeQuietly(in);
        closeQuietly(out);
        closeQuietly(socket);
        in = null;
        out = null;
        socket = null;
    }

    private void closeQuietly(Closeable closeable) {
        if (closeable == null) return;
        try { closeable.close(); } catch (Exception ignored) { }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GameGUI(new Game()));
    }
}

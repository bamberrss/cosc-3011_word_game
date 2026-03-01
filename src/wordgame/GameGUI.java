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
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashMap;
import java.util.Map;

/**
 * Wordle-style GUI with on-screen keyboard showing letter states.
 * Green = correct position, Yellow = wrong position, Gray = not in word
 */
public class GameGUI extends JFrame {
    private Word secret;
    // no wordList field; logic lives in Game

    // reference to game logic object (injected)
    private Game game;

    private JTextField[] guessBoxes;  // 5 boxes for 5-letter word
    private JTextPane feedbackArea;
    private JButton guessButton;
    private Map<Character, JButton> keyboardButtons;
    private Map<Character, String> letterStatus;  // 'G', 'Y', 'X', or null

    // network support
    private GameClient networkClient;
    private boolean networkMode = false;

    public GameGUI(Game game) {
        super("Wordle - Guess the Word");
        this.game = game;
        this.secret = game.getSecret();
        guessBoxes = new JTextField[5];
        keyboardButtons = new HashMap<>();
        letterStatus = new HashMap<>();

        // Initialize all letters as unused
        for (char c = 'a'; c <= 'z'; c++) {
            letterStatus.put(c, null);
        }

        initComponents();
        createMenuBar();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void initComponents() {
        // Title
        JLabel titleLabel = new JLabel("WORDLE");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setHorizontalAlignment(JLabel.CENTER);

        // Input panel with 5 boxes
        JPanel inputPanel = createInputPanel();

        // Feedback area
        feedbackArea = new JTextPane();
        feedbackArea.setEditable(false);
        feedbackArea.setFont(new Font("Monospaced", Font.PLAIN, 16));
        feedbackArea.setBackground(new Color(240, 240, 240));
        JScrollPane scroll = new JScrollPane(feedbackArea,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // Keyboard panel
        JPanel keyboardPanel = createKeyboardPanel();

        // Layout
        getContentPane().setLayout(new BorderLayout(10, 10));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(titleLabel, BorderLayout.NORTH);
        topPanel.add(inputPanel, BorderLayout.SOUTH);

        getContentPane().add(topPanel, BorderLayout.NORTH);
        getContentPane().add(scroll, BorderLayout.CENTER);
        getContentPane().add(keyboardPanel, BorderLayout.SOUTH);
    }

    private JPanel createInputPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
        panel.add(new JLabel("Enter guess:"));

        for (int i = 0; i < 5; i++) {
            guessBoxes[i] = new JTextField(1);
            guessBoxes[i].setFont(new Font("Arial", Font.BOLD, 28));
            guessBoxes[i].setHorizontalAlignment(JTextField.CENTER);
            guessBoxes[i].setPreferredSize(new Dimension(50, 50));
            guessBoxes[i].setMaximumSize(new Dimension(50, 50));
            guessBoxes[i].setBorder(new LineBorder(Color.BLACK, 2));

            // Attach a document filter to allow only single letters
            AbstractDocument doc = (AbstractDocument) guessBoxes[i].getDocument();
            doc.setDocumentFilter(new SingleLetterFilter(i));

            // Add key listener for navigation and Enter submission
            final int boxIndex = i;
            guessBoxes[i].addKeyListener(new KeyListener() {
                @Override
                public void keyTyped(KeyEvent e) {}

                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                        if (guessBoxes[boxIndex].getText().isEmpty() && boxIndex > 0) {
                            guessBoxes[boxIndex - 1].requestFocus();
                            guessBoxes[boxIndex - 1].selectAll();
                        }
                    } else if (e.getKeyCode() == KeyEvent.VK_RIGHT && boxIndex < 4) {
                        if (!guessBoxes[boxIndex].getText().isEmpty()) {
                            guessBoxes[boxIndex + 1].requestFocus();
                        }
                    } else if (e.getKeyCode() == KeyEvent.VK_LEFT && boxIndex > 0) {
                        guessBoxes[boxIndex - 1].requestFocus();
                    } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        handleGuess();
                    }
                }

                @Override
                public void keyReleased(KeyEvent e) {}
            });

            panel.add(guessBoxes[i]);
        }

        guessButton = new JButton("Submit");
        guessButton.addActionListener(e -> handleGuess());
        panel.add(guessButton);

        // network controls to show usage of GameServer/GameClient

        return panel;
    }

    private JPanel createKeyboardPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Alphabet"));

        // Three rows for QWERTY keyboard layout
        String[] row1 = {"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"};
        String[] row2 = {"a", "s", "d", "f", "g", "h", "j", "k", "l"};
        String[] row3 = {"z", "x", "c", "v", "b", "n", "m"};

        panel.add(createKeyboardRow(row1));
        panel.add(Box.createVerticalStrut(5));
        panel.add(createKeyboardRow(row2));
        panel.add(Box.createVerticalStrut(5));
        panel.add(createKeyboardRow(row3));

        return panel;
    }

    private JPanel createKeyboardRow(String[] letters) {
        JPanel row = new JPanel();
        row.setLayout(new FlowLayout(FlowLayout.CENTER, 3, 3));

        for (String letterStr : letters) {
            char letter = letterStr.charAt(0);
            JButton btn = new JButton(letterStr.toUpperCase());
            btn.setFont(new Font("Arial", Font.BOLD, 12));
            btn.setPreferredSize(new Dimension(35, 35));
            btn.setBackground(new Color(200, 200, 200));
            btn.setOpaque(true);
            btn.setBorder(new LineBorder(Color.BLACK, 1));
            btn.setEnabled(false);

            keyboardButtons.put(letter, btn);
            row.add(btn);
        }

        return row;
    }

    /**
     * Document filter that allows only a single letter per box
     * and auto-advances to the next box.
     */
    private class SingleLetterFilter extends DocumentFilter {
        private int boxIndex;

        SingleLetterFilter(int boxIndex) {
            this.boxIndex = boxIndex;
        }

        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                throws BadLocationException {
            if (string == null || string.isEmpty()) return;

            // Only allow letters
            string = string.replaceAll("[^a-zA-Z]", "").toLowerCase();
            if (string.isEmpty()) return;

            // Reject if box already has a character
            if (fb.getDocument().getLength() >= 1) {
                return;
            }

            // Only take the first character
            char c = string.charAt(0);
            super.insertString(fb, offset, String.valueOf(c), attr);

            // Auto-advance to next box if not the last
            if (boxIndex < 4) {
                SwingUtilities.invokeLater(() -> guessBoxes[boxIndex + 1].requestFocus());
            }
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String string, AttributeSet attr)
                throws BadLocationException {
            if (string == null) string = "";

            // Only allow letters
            string = string.replaceAll("[^a-zA-Z]", "").toLowerCase();
            if (string.isEmpty()) {
                // Allow deletion
                super.replace(fb, offset, length, string, attr);
                return;
            }

            // Only take the first character
            char c = string.charAt(0);
            
            // Replace entire content with single character
            fb.remove(0, fb.getDocument().getLength());
            fb.insertString(0, String.valueOf(c), attr);

            // Auto-advance to next box if not the last
            if (boxIndex < 4) {
                SwingUtilities.invokeLater(() -> guessBoxes[boxIndex + 1].requestFocus());
            }
        }
    }

    private void handleGuess() {
        // Combine all 5 boxes into one word
        StringBuilder guessText = new StringBuilder();
        for (JTextField box : guessBoxes) {
            String text = box.getText().toLowerCase();
            if (text.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Please fill all 5 letter boxes.",
                        "Incomplete Guess", JOptionPane.WARNING_MESSAGE);
                return;
            }
            guessText.append(text);
        }

        String guess = guessText.toString();
        // delegate to game object for logic
        Feedback fb = game.makeGuess(guess);
        String status = fb.getLetterStatus();

        // Update feedback display with colored letters
        appendColoredGuess(guess, status);

        // Update keyboard letter states
        for (int i = 0; i < guess.length(); i++) {
            char letter = guess.charAt(i);
            char statusChar = status.charAt(i);

            // Update letter state if this status is better than previous
            String currentStatus = letterStatus.get(letter);
            if (shouldUpdateStatus(currentStatus, statusChar)) {
                letterStatus.put(letter, String.valueOf(statusChar));
                updateKeyboardButtonColor(letter, statusChar);
            }
        }

        if (fb.isCorrect()) {
            JOptionPane.showMessageDialog(this, "🎉 YOU WIN! The word was " + secret.toString().toUpperCase(),
                    "Congratulations", JOptionPane.INFORMATION_MESSAGE);
            for (JTextField box : guessBoxes) {
                box.setEditable(false);
            }
            guessButton.setEnabled(false);
        } else {
            // Clear boxes for next guess
            for (JTextField box : guessBoxes) {
                box.setText("");
            }
            guessBoxes[0].requestFocus();
        }
    }

    /**
     * Append a colored guess to the feedback area.
     * Each letter is colored based on its status.
     */
    private void appendColoredGuess(String guess, String status) {
        try {
            int docLength = feedbackArea.getDocument().getLength();
            
            for (int i = 0; i < guess.length(); i++) {
                char letter = Character.toUpperCase(guess.charAt(i));
                char statusChar = status.charAt(i);

                SimpleAttributeSet attrs = new SimpleAttributeSet();
                Color bgColor;
                Color fgColor;

                if (statusChar == 'G') {
                    bgColor = new Color(34, 177, 76);  // Green
                    fgColor = Color.WHITE;
                } else if (statusChar == 'Y') {
                    bgColor = new Color(255, 193, 7);  // Yellow
                    fgColor = Color.BLACK;
                } else {  // 'X'
                    bgColor = new Color(120, 120, 120);  // Gray
                    fgColor = Color.WHITE;
                }

                StyleConstants.setBackground(attrs, bgColor);
                StyleConstants.setForeground(attrs, fgColor);
                StyleConstants.setFontSize(attrs, 20);
                StyleConstants.setBold(attrs, true);

                feedbackArea.getDocument().insertString(docLength + i, String.valueOf(letter), attrs);
            }
            
            // Add newline after the guess
            feedbackArea.getDocument().insertString(feedbackArea.getDocument().getLength(), "\n", null);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    /**
     * Determine if we should update the letter state.
     * Green > Yellow > Gray
     */
    private boolean shouldUpdateStatus(String current, char newStatus) {
        if (current == null) return true;
        char currentChar = current.charAt(0);
        if (newStatus == 'G') return true;  // Green always wins
        if (newStatus == 'Y' && currentChar != 'G') return true;  // Yellow wins over gray
        return false;
    }

    private void updateKeyboardButtonColor(char letter, char status) {
        JButton btn = keyboardButtons.get(letter);
        if (btn == null) return;

        Color color;
        if (status == 'G') {
            color = new Color(34, 177, 76);  // Green
        } else if (status == 'Y') {
            color = new Color(255, 193, 7);  // Yellow
        } else {
            color = new Color(120, 120, 120);  // Gray
        }
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Arial", Font.BOLD, 12));
    }

    /**
     * Build a simple menu bar with network options.
     */
    private void createMenuBar() {
        JMenuBar menubar = new JMenuBar();
        JMenu networkMenu = new JMenu("Network");

        JMenuItem hostItem = new JMenuItem("Host Game");
        hostItem.addActionListener(e -> {
            // start server in background thread
            new Thread(() -> new GameServer().serverStart()).start();
            JOptionPane.showMessageDialog(this, "Server started (console output).", "Network", JOptionPane.INFORMATION_MESSAGE);
        });

        JMenuItem joinItem = new JMenuItem("Join Game...");
        joinItem.addActionListener(e -> {
            String host = JOptionPane.showInputDialog(this, "Enter server host (default localhost):", "localhost");
            if (host == null || host.isEmpty()) return;
            try {
                networkClient = new GameClient(host, 5000);
                networkMode = true;
                JOptionPane.showMessageDialog(this, "Connected to server. Type guesses as usual.", "Network", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to connect: " + ex.getMessage(), "Network Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        networkMenu.add(hostItem);
        networkMenu.add(joinItem);
        menubar.add(networkMenu);
        setJMenuBar(menubar);
    }

    public static void main(String[] args) {
        // set up game logic outside of GUI
        Game game = new Game();
        game.init("test.txt");
        SwingUtilities.invokeLater(() -> new GameGUI(game));
    }
}

package wordgame;

import java.io.Serializable;

/**
 * Feedback for each guess with per-letter status tracking.
 * letterStatus: String where each char is 'G' (green), 'Y' (yellow), or 'X' (gray)
 */
class Feedback implements Serializable {

    private String letterStatus;  // e.g., "GYXGY"
    private int wordLength;

    /**
     * Create feedback with per-letter status.
     * @param letterStatus string of G/Y/X for each position
     * @param wordLength the word length
     */
    Feedback(String letterStatus, int wordLength) {
        this.letterStatus = letterStatus;
        this.wordLength = wordLength;
    }

    /**
     * Legacy constructor for backward compatibility.
     */
    Feedback(int correctLetters, int correctPositions, int wordLength) {
        this.wordLength = wordLength;
        this.letterStatus = "X".repeat(wordLength);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (char c : letterStatus.toCharArray()) {
            if (c == 'G') sb.append("🟩");
            else if (c == 'Y') sb.append("🟨");
            else sb.append("⬜");
        }
        return sb.toString();
    }

    public String getLetterStatus() {
        return letterStatus;
    }

    public boolean isCorrect() {
        return letterStatus.equals("G".repeat(wordLength));
    }
}

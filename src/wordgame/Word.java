package wordgame;

import java.util.ArrayList;
import java.io.Serializable;

class Word implements Serializable {

    private ArrayList<Character> letters;
    private String value;

    Word(String str) {
        value = str.toLowerCase();
        letters = new ArrayList<>();

        for (char c : str.toLowerCase().toCharArray()) {
            letters.add(c);
        }
    }

    /**
     * Compares this word (the secret) to the guessed word.
     * Returns per-letter feedback: G=green, Y=yellow, X=gray
     * Handles duplicate letters like Wordle.
     */
    public Feedback compareTo(Word guess) {
        StringBuilder letterStatus = new StringBuilder();
        boolean[] secretUsed = new boolean[this.length()];
        char[] feedbackChars = new char[guess.length()];

        // First pass: mark exact position matches (GREEN)
        for (int i = 0; i < guess.length(); i++) {
            if (this.letters.get(i).equals(guess.letters.get(i))) {
                feedbackChars[i] = 'G';
                secretUsed[i] = true;
            } else {
                feedbackChars[i] = '?';
            }
        }

        // Second pass: mark wrong position and non-matches
        for (int i = 0; i < guess.length(); i++) {
            if (feedbackChars[i] == 'G') continue;

            char guessLetter = guess.letters.get(i);
            boolean found = false;
            for (int j = 0; j < this.letters.size(); j++) {
                if (!secretUsed[j] && this.letters.get(j).equals(guessLetter)) {
                    feedbackChars[i] = 'Y';
                    secretUsed[j] = true;
                    found = true;
                    break;
                }
            }
            if (!found) {
                feedbackChars[i] = 'X';
            }
        }

        for (char c : feedbackChars) {
            letterStatus.append(c);
        }

        return new Feedback(letterStatus.toString(), this.length());
    }

    @Override
    public String toString() {
        return value;
    }

    public int length() {
        return letters.size();
    }
}

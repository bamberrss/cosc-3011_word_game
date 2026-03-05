package wordgame;

import java.io.Serializable;

class Feedback implements Serializable {

    char[] colors;
    int correct;
    private int wordLength;

    Feedback(char[] colors, int correct, int wordLength) {
        this.colors = colors;
        this.correct = correct;
        this.wordLength = wordLength;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (char c : colors) {
            sb.append(c).append(' ');
        }
        return sb.toString().trim();
    }

    public boolean isCorrect() {
        return correct == wordLength;
    }
}

package wordgame;

import java.util.Scanner;

class Game {

    private Word secret;

    /**
     * Initialize the game by loading a word list and picking a secret word.
     * Can be called by GUI code.
     */
    public void init(String filename) {
        WordList wordList = new WordList(filename);
        secret = wordList.getRandom();
    }

    /**
     * Return the length of the secret word (for GUI display).
     */
    public int getSecretLength() {
        return secret == null ? 0 : secret.length();
    }

    /**
     * Return the secret word object itself.
     */
    public Word getSecret() {
        return secret;
    }

    /**
     * Make a guess and return the feedback.  Public so GUI can call it.
     */
    public Feedback makeGuess(String guess) {
        return new Word(guess).compareTo(secret);
    }

    private void start() {
        // Change to "full.txt" or "test.txt" as needed.  WordList will
        // resolve the correct path whether we run from the project root
        // or from the src directory.
        WordList wordList = new WordList("full.txt");

        secret = wordList.getRandom();
        Feedback fb;

        Scanner scanner = new Scanner(System.in);

        do {
            System.out.print("Enter your guess: ");
            String guess = scanner.nextLine();

            fb = makeGuess(guess);
            printFeedback(fb);
        } while (!fb.isCorrect());

        System.out.println("YOU WIN!");
        scanner.close();
    }

    private static void printFeedback(Feedback fb) {
        System.out.println(fb);
    }

    public static void main(String[] args) {
        new Game().start();
    }
}

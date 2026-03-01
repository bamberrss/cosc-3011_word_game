package wordgame;

import java.util.Scanner;

class Game {

    private Word secret;

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

    private Feedback makeGuess(String guess) {
        return new Word(guess).compareTo(secret);
    }

    private static void printFeedback(Feedback fb) {
        System.out.println(fb);
    }

    public static void main(String[] args) {
        new Game().start();
    }
}

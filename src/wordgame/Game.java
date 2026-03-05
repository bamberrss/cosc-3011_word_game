package wordgame;

import java.util.Scanner;

class Game {

    private Word secret;

    private void start() {
        // Change to src/full.txt for full game. test.txt is a placeholder for testing only.
        WordList wordList = new WordList("src/test.txt");

        secret = wordList.getRandom();
        Feedback fb;

        Scanner scanner = new Scanner(System.in);

        do {
            System.out.print("Enter your guess: ");
            String guessStr = scanner.nextLine();
            Word guess = new Word(guessStr);

            fb = makeGuess(guess);
            printFeedback(fb);
        } while (!fb.isCorrect());

        System.out.println("YOU WIN!");
        scanner.close();
    }

    private Feedback makeGuess(Word guess) {
        return secret.compareTo(guess);
    }

    private static void printFeedback(Feedback fb) {
        System.out.println(fb);
    }

    public static void main(String[] args) {
        new Game().start();
    }
}

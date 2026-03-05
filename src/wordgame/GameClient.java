// GameClient

package wordgame;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class GameClient {
  public static void main(String[] args) {

    try (Socket socket = new Socket("localhost", 5000);
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        Scanner scanner = new Scanner(System.in)) {

      System.out.println("Connected to the server!");

      Feedback fb;

      do {
        // Read guess from user
        System.out.print("Enter your guess: ");
        String guessStr = scanner.nextLine();
        Word guess = new Word(guessStr);

        // Send to server
        out.writeObject(guess);
        out.flush();

        // Receive the feedback from the server
        fb = (Feedback) in.readObject();
        printFeedback(fb);
      } while (!fb.isCorrect());

      System.out.println("YOU WIN!");
      
    } catch (IOException | ClassNotFoundException e) {
      System.out.println("Connection Error: " + e.getMessage());
    }
  }

  private static void printFeedback(Feedback fb) {
      System.out.println(fb);
  }
}

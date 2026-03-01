// GameClient

package wordgame;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class GameClient implements AutoCloseable {
  private Socket socket;
  private ObjectOutputStream out;
  private ObjectInputStream in;

  /**
   * Create a client and connect to the given host/port.
   */
  public GameClient(String host, int port) throws IOException {
    socket = new Socket(host, port);
    out = new ObjectOutputStream(socket.getOutputStream());
    in = new ObjectInputStream(socket.getInputStream());
  }

  /**
   * Send a guess and wait for feedback from server.
   */
  public Feedback sendGuess(String guess) throws IOException, ClassNotFoundException {
    Word w = new Word(guess);
    out.writeObject(w);
    out.flush();
    return (Feedback) in.readObject();
  }

  /**
   * Close the connection gracefully.
   */
  @Override
  public void close() {
    try {
      if (out != null) out.close();
      if (in != null) in.close();
      if (socket != null) socket.close();
    } catch (IOException ignored) {}
  }

  public static void main(String[] args) {

    try (GameClient client = new GameClient("localhost", 5000);
         Scanner scanner = new Scanner(System.in)) {

      System.out.println("Connected to the server!");

      Feedback fb;

      do {
        // Read guess from user
        System.out.print("Enter your guess: ");
        String guessStr = scanner.nextLine();

        // Send to server and receive feedback
        fb = client.sendGuess(guessStr);
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

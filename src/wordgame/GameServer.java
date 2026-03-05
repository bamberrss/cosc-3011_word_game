// GameServer

package wordgame;

import java.net.*;
import java.io.*;

public class GameServer {

  private Word secret;
  private WordList wordList;


  private void serverStart() {
    // Change to src/full.txt for full game. There are a few wordlist options currently
    wordList = new WordList("src/test.txt");

    secret = wordList.getRandom();
    Feedback fb;

    try (ServerSocket server = new ServerSocket(5000)) {
      System.out.println("Server started!");
      
  
      // Socket connection
      try (Socket client = server.accept();
          ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
          ObjectInputStream in = new ObjectInputStream(client.getInputStream())) {

        do {
          // wait for client guess 
          Word guess = (Word) in.readObject();

          // Create feedback object
          fb = makeGuess(guess);

          // Send feedback to client
          out.writeObject(fb);
          out.flush();

        } while (!fb.isCorrect());
      } catch (ClassNotFoundException e) {
        System.err.println("Received unknown object from client: " + e.getMessage());
      }
    } catch (IOException e) {
      System.err.println("Failed to start server: " + e.getMessage());
    } 
}

  private Feedback makeGuess(Word guess) {
    return secret.compareTo(guess);
  }

  public static void main(String[] args) {
      new GameServer().serverStart();
  }
}

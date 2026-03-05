package wordgame;

import java.util.ArrayList;
import java.util.Random;
import java.nio.file.*;
import java.io.IOException;

class WordList {

    private ArrayList<Word> words;

    WordList(String filename) {
        this.words = new ArrayList<>();
        try {
          for (String line : Files.readAllLines(Path.of(filename))) {
              if (!line.isBlank()) {
                Word w = new Word(line.trim());
                this.words.add(w);
              }
          }
        } catch (IOException e){
          System.err.println("Error reading file: " + filename);
          e.printStackTrace();
        }
    }

    public Word getRandom() {
        Random randomizer = new Random();

        int randIndex = randomizer.nextInt(words.size());

        return words.get(randIndex);
    }
}

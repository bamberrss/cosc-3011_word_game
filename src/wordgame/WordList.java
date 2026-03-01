package wordgame;

import java.util.ArrayList;
import java.util.Random;
import java.nio.file.*;
import java.io.IOException;

class WordList {

    private ArrayList<Word> words;

    WordList(String filename) {
        this.words = new ArrayList<>();

        // try a few plausible locations so the user can run the
        // program from either the project root or the src directory.
        Path path = Path.of(filename);
        if (!Files.exists(path)) {
            Path alt = Path.of("src").resolve(filename);
            if (Files.exists(alt)) {
                path = alt;
            } else {
                Path alt2 = Path.of("..", filename);
                if (Files.exists(alt2)) {
                    path = alt2;
                }
            }
        }

        try {
          for (String line : Files.readAllLines(path)) {
              if (!line.isBlank()) {
                Word w = new Word(line.trim());
                this.words.add(w);
              }
          }
        } catch (IOException e){
          System.err.println("Error reading file: " + path);
          e.printStackTrace();
        }
    }

    public Word getRandom() {
        Random randomizer = new Random();

        int randIndex = randomizer.nextInt(words.size());

        return words.get(randIndex);
    }
}

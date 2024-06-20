import java.util.Collections;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;

/**
 * Defines a Wordle word as scraped from: https://github.com/steve-kasica/wordle-words
 */
public class Word implements Comparable<Word> {
    private String _word;
    private Double _occurrence;
    private Integer _day;

    public Word(String csvLine) {
        // each line is expected to contain 3 fields!
        String[] csvParts = csvLine.split(",");
        _word = csvParts[0].toUpperCase();
        _occurrence = Doubles.tryParse(csvParts[1]);
        _day = csvParts.length > 2 ? Ints.tryParse(csvParts[2]) : null;
    }

    public String getWord() {
        return _word;
    }

    public String getHints(String word) {
        if (word.length() != _word.length()) {
            return String.join("", Collections.nCopies(_word.length(), "?"));
        }
        char[] hints = new char[_word.length()];
        String unmatched = "";

        // find first the perfect matches, hold on the missed secret characters
        for (int i = 0; i < hints.length; i++) {
            char sC = _word.charAt(i);
            if (sC == word.charAt(i)) {
                hints[i] = sC;
            } else {
                unmatched += sC;
            }
        }

        // go through missed places and mark with * if the given character is still in the unmatched set
        for (int i = 0; i < hints.length; i++) {
            if (hints[i] != 0) {
                continue;
            }
            hints[i] = unmatched.indexOf(word.charAt(i)) >= 0 ? '*' : '-';
        }

        return String.valueOf(hints);
    }

    @Override
    public int compareTo(Word o) {
        return -(int)Math.signum(_occurrence - o._occurrence);
    }

    @Override
    public String toString() {
        return String.format("[%s, %.6e, %d]", _word, _occurrence, _day);
    }
}

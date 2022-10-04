/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * Text processing utilities
 */
public class TextUtils {

  /**
   * Wraps the words of the given (long) text to several lines of maximum given length
   */
  public static String wrapText(String text, int len) {

    // return text if less than length
    if (text.length() <= len)
      return text;

    StringBuffer result = new StringBuffer();
    StringBuffer line = new StringBuffer();
    StringBuffer word = new StringBuffer();

    char[] chars = text.toCharArray();

    for (int i = 0; i < chars.length; i++) {
      word.append(chars[i]);

      if (chars[i] == ' ') {
        if ((line.length() + word.length()) > len) {
          if (result.length() != 0)
            result.append("\n");
          result.append(line.toString());
          line.delete(0, line.length());
        }

        line.append(word);
        word.delete(0, word.length());
      }
    }

    // handle any extra chars in current word
    if (word.length() > 0) {
      if ((line.length() + word.length()) > len) {
        if (result.length() != 0)
          result.append("\n");
        result.append(line.toString());
        line.delete(0, line.length());
      }
      line.append(word);
    }

    // handle extra line
    if (line.length() > 0) {
      result.append("\n");
      result.append(line.toString());
    }

    return result.toString();
  }

  /**
   * Reads a line of text from a given input stream or null if the end of the stream is reached.
   */
  public static String readLineFromStream(InputStream in) throws IOException {
    byte buf[] = new byte[1024];
    int pos = 0;
    while (true) {
      int ch = in.read();
      if ((ch == '\n') || (ch < 0))
        break;
      buf[pos++] = (byte) ch;
      if (pos == buf.length)
        buf = Arrays.copyOf(buf, pos * 2);
    }
    if (pos == 0)
      return null;

    return new String(Arrays.copyOf(buf, pos), "UTF-8");
  }

  /**
   * Generates a regular expression from a string that contains asterisks (*) as wild cards.
   * Basically, it replaces all * with .*
   */
  public static String createRegexFromWildcards(String text) {
    final StringBuilder regex = new StringBuilder("^");
    String sections[] = text.split("\\*", -1);
    for (int i = 0; i < sections.length; i++) {
      if (i > 0)
        regex.append(".*");
      regex.append(Pattern.quote(sections[i]));
    }
    regex.append("$");
    return regex.toString();
  }

}

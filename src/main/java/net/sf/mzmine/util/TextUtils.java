/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * Text processing utilities
 */
public class TextUtils {

    /**
     * Wraps the words of the given (long) text to several lines of maximum
     * given length
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
     * Reads a line of text from a given input stream or null if the end of the
     * stream is reached.
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
     * Generates a regular expression from a string that contains asterisks (*)
     * as wild cards. Basically, it replaces all * with .*
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

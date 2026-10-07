/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.util.spectraldb.parser.gnps;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import io.github.mzmine.util.io.JsonUtils;
import java.io.IOException;
import java.util.Arrays;
import org.jetbrains.annotations.NotNull;

/**
 * The GNPS peaks_json field nests the signals as a list of (mz, intensity) pairs inside a json
 * string. Deserializes it into mz values in [0] and intensities in [1].
 * <p>
 * Classic GNPS exports write real json, {@code [[mz,intensity],...]}. The cleaned GNPS libraries
 * write the python repr of a list of tuples instead, {@code [(mz, intensity),...]}, which is not
 * json. Both are accepted.
 */
class SpectrumDeserializer extends JsonDeserializer<double[][]> {

  private static final int INITIAL_SIGNALS = 128;

  @Override
  public double[][] deserialize(@NotNull final JsonParser p,
      @NotNull final DeserializationContext ctx) throws IOException {
    double[] mzs = new double[INITIAL_SIGNALS];
    double[] intensities = new double[INITIAL_SIGNALS];
    int n = 0;

    // decision: parse the nested json straight from the outer parser's character buffer into the
    // two primitive arrays. Avoids materializing the peaks string, the double[] per signal of the
    // databind route, and the ObjectMapper that used to be created for every single spectrum.
    char[] text = p.getTextCharacters();
    int offset = p.getTextOffset();
    int length = p.getTextLength();
    if (usesTuples(text, offset, length)) {
      text = tuplesToJsonArrays(text, offset, length);
      offset = 0;
      length = text.length;
    }

    try (JsonParser peaks = JsonUtils.FACTORY.createParser(text, offset, length)) {
      if (peaks.nextToken() != JsonToken.START_ARRAY) {
        throw new IOException("peaks_json does not contain a json array");
      }
      while (peaks.nextToken() == JsonToken.START_ARRAY) {
        if (n == mzs.length) {
          mzs = Arrays.copyOf(mzs, n * 2);
          intensities = Arrays.copyOf(intensities, n * 2);
        }
        peaks.nextToken();
        mzs[n] = peaks.getDoubleValue();
        peaks.nextToken();
        intensities[n] = peaks.getDoubleValue();
        n++;

        // tolerate additional values in a pair, only mz and intensity are used
        for (JsonToken t = peaks.nextToken(); t != JsonToken.END_ARRAY; t = peaks.nextToken()) {
          if (t == null) {
            throw new IOException("peaks_json ended inside a signal");
          }
        }
      }
    }

    return new double[][]{Arrays.copyOf(mzs, n), Arrays.copyOf(intensities, n)};
  }

  /**
   * @return true if the signals are written as python tuples instead of json arrays
   */
  private static boolean usesTuples(final char[] text, final int offset, final int length) {
    for (int i = offset; i < offset + length; i++) {
      final char c = text[i];
      // the first character that opens a signal decides, everything before is the outer list
      if (c == '(') {
        return true;
      }
      if (c == '[' && i > offset) {
        return false;
      }
      if (c != '[' && !Character.isWhitespace(c)) {
        return false;
      }
    }
    return false;
  }

  /**
   * Rewrites the python tuple brackets into json array brackets. peaks_json only ever holds
   * numbers, so there are no strings whose content could be changed by this.
   */
  private static char[] tuplesToJsonArrays(final char[] text, final int offset, final int length) {
    final char[] json = new char[length];
    for (int i = 0; i < length; i++) {
      final char c = text[offset + i];
      json[i] = switch (c) {
        case '(' -> '[';
        case ')' -> ']';
        default -> c;
      };
    }
    return json;
  }
}

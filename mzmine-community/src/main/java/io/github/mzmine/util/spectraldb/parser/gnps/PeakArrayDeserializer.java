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
import java.io.IOException;
import java.util.Arrays;
import org.jetbrains.annotations.NotNull;

/**
 * Reads a plain json array of [mz, intensity] pairs into mz values in [0] and intensities in [1].
 * Unlike {@link SpectrumDeserializer} the signals are a real json array here, not json nested in a
 * string, so they are streamed straight out of the parser.
 */
class PeakArrayDeserializer extends JsonDeserializer<double[][]> {

  private static final int INITIAL_SIGNALS = 128;

  @Override
  public double[][] deserialize(@NotNull final JsonParser p,
      @NotNull final DeserializationContext ctx) throws IOException {
    if (p.currentToken() != JsonToken.START_ARRAY) {
      throw new IOException("peaks is no json array");
    }

    double[] mzs = new double[INITIAL_SIGNALS];
    double[] intensities = new double[INITIAL_SIGNALS];
    int n = 0;

    while (p.nextToken() == JsonToken.START_ARRAY) {
      if (n == mzs.length) {
        mzs = Arrays.copyOf(mzs, n * 2);
        intensities = Arrays.copyOf(intensities, n * 2);
      }
      p.nextToken();
      mzs[n] = p.getDoubleValue();
      p.nextToken();
      intensities[n] = p.getDoubleValue();
      n++;

      // tolerate additional values in a signal, only mz and intensity are used
      for (JsonToken t = p.nextToken(); t != JsonToken.END_ARRAY; t = p.nextToken()) {
        if (t == null) {
          throw new IOException("peaks ended inside a signal");
        }
      }
    }

    return new double[][]{Arrays.copyOf(mzs, n), Arrays.copyOf(intensities, n)};
  }
}

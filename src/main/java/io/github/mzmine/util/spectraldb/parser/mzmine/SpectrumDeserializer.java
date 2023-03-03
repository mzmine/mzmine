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

package io.github.mzmine.util.spectraldb.parser.mzmine;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.io.IOException;

class SpectrumDeserializer extends JsonDeserializer<double[][]> {

  @Override
  public double[][] deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
    DoubleArrayList mzs = new DoubleArrayList();
    DoubleArrayList intensities = new DoubleArrayList();
    while (true) {
      JsonToken token = p.nextToken();
      if (token == null) {
        break;
      }
      if (token == JsonToken.START_ARRAY) {
        // read xy
        p.nextToken();
        mzs.add(p.getDoubleValue());
        p.nextToken();
        intensities.add(p.getDoubleValue());
      }
    }
    return new double[][]{mzs.toDoubleArray(), intensities.toDoubleArray()};
  }
}

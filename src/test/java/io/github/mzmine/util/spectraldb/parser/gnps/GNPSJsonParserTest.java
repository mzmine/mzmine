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

package io.github.mzmine.util.spectraldb.parser.gnps;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonParseException;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GNPSJsonParserTest {


  @Test
  void testParse() throws IOException {
    File file = new File(
        GNPSJsonParserTest.class.getClassLoader().getResource("json/gnps.json").getFile());

    List<SpectralLibraryEntry> list = new ArrayList<>();
    new GNPSJsonParser(0, (newList, alreadyProcessed) -> {
      list.addAll(newList);
    }).parse(null, file, null);

    assert list.size() == 4;
  }

  @Test
  public void testObjectMapper() throws JsonParseException, IOException {
    File file = new File(
        GNPSJsonParserTest.class.getClassLoader().getResource("json/gnps.json").getFile());

    ObjectMapper mapper = new ObjectMapper();
    List<GnpsLibraryEntry> list = mapper.readValue(file, new TypeReference<>() {
    });

    assert list.size() == 4;
  }

  @Test
  public void testJacksonStream() throws JsonParseException, IOException {
    File file = new File(
        GNPSJsonParserTest.class.getClassLoader().getResource("json/gnps.json").getFile());

    ObjectMapper mapper = new ObjectMapper();
    List<SpectralLibraryEntry> list = new ArrayList<>();
    // Create a JsonParser instance
    try (JsonParser jsonParser = mapper.getFactory().createParser(file)) {

      // Check the first token
      if (jsonParser.nextToken() != JsonToken.START_ARRAY) {
        throw new IllegalStateException("Expected content to be an array");
      }

      // Iterate over the tokens until the end of the array
      while (jsonParser.nextToken() != JsonToken.END_ARRAY) {

        // Read a contact instance using ObjectMapper and do something with it
        SpectralLibraryEntry entry = mapper.readValue(jsonParser, GnpsLibraryEntry.class)
            .toSpectralLibraryEntry(null);
        list.add(entry);
      }
    }

    assert list.size() == 4;
  }
}
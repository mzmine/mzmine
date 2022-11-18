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

import java.io.File;

class MZmineJsonParserTest {

  File file = new File(
      MZmineJsonParserTest.class.getClassLoader().getResource("json/mzmine.json").getFile());

//  @Test
//  void testParse() throws IOException {
//    List<SpectralLibraryEntry> list = new ArrayList<>();
//    new GNPSJsonParser(0, (newList, alreadyProcessed) -> {
//      list.addAll(newList);
//    }).parse(null, file, null);
//
//    assert list.size() == 4;
//  }
//
//  @Test
//  public void testObjectMapper() throws JsonParseException, IOException {
//    ObjectMapper mapper = new ObjectMapper();
//    List<MZmineJsonLibraryEntry> list = mapper.readValue(file, new TypeReference<>() {
//    });
//
//    assert list.size() == 4;
//  }
//
//  @Test
//  public void testJacksonStream() throws JsonParseException, IOException {
//    ObjectMapper mapper = new ObjectMapper();
//    List<MZmineJsonLibraryEntry> list = new ArrayList<>();
//    // Create a JsonParser instance
//    try (JsonParser jsonParser = mapper.getFactory().createParser(file)) {
//
//      // Check the first token
////      if (jsonParser.nextToken() != JsonToken.START_ARRAY) {
////        throw new IllegalStateException("Expected content to be an array");
////      }
//
//      // Iterate over the tokens until the end of the array
//
//      while (true) {
//        JsonToken token = jsonParser.nextToken();
//        if (token == null || token == JsonToken.END_ARRAY) {
//          break;
//        } else if (token == JsonToken.START_OBJECT) {
//          // Read a contact instance using ObjectMapper and do something with it
//          MZmineJsonLibraryEntry entry = mapper.readValue(jsonParser, MZmineJsonLibraryEntry.class);
//          list.add(entry);
//        }
//      }
//    }
//
//    assert list.size() == 4;
//  }
}
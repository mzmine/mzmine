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

package io.github.mzmine.datamodel.features.types.annotations;

import datamodel.DataTypeTestUtils;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.util.io.JsonUtils;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import io.github.mzmine.util.spectraldb.parser.AutoLibraryParser;
import io.github.mzmine.util.spectraldb.parser.mzmine.MZmineJsonLibraryEntry;
import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLInputFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SynonymsTypeTest {

  @Test
  void parsesQuotedNamesAndPreservesChemicalPunctuation() {
    final var type = new SynonymsType();
    Assertions.assertEquals(List.of("1,2-dichlorobenzene", "name;with;semicolons", "a \"quote\""),
        type.getMapper().apply("1,2-dichlorobenzene;\"name;with;semicolons\";\"a \"\"quote\"\"\""));
    Assertions.assertEquals(List.of("[13C]caffeine"), SynonymsType.parse("[13C]caffeine"));
    Assertions.assertEquals(List.of(), SynonymsType.parse("  "));
    Assertions.assertNull(SynonymsType.parse(null));
  }

  @Test
  void mapsDatabaseFieldAndRoundTripsJsonAndMgfValues() {
    final List<String> names = List.of("1,2-dichlorobenzene", "name;with;semicolons",
        "a \"quote\"");
    Assertions.assertEquals(List.class, DBEntryField.SYNONYMS.getObjectClass());
    Assertions.assertEquals(SynonymsType.class, DBEntryField.SYNONYMS.getDataType());
    Assertions.assertEquals(DBEntryField.SYNONYMS, DBEntryField.fromDataType(new SynonymsType()));
    Assertions.assertEquals(names,
        DBEntryField.SYNONYMS.convertValue(DBEntryField.SYNONYMS.formatForMgf(names)));
    Assertions.assertEquals(names,
        SynonymsType.parse(new SynonymsType().getFormattedExportString(names)));
    final var jsonEntry = new MZmineJsonLibraryEntry();
    jsonEntry.synonyms.addAll(names);
    jsonEntry.signals = new double[][]{{100.0, 200.0}, {10.0, 20.0}};
    final var restored = JsonUtils.readValueOrThrow(JsonUtils.writeStringOrThrow(jsonEntry),
        MZmineJsonLibraryEntry.class).toSpectralLibraryEntry(null);
    Assertions.assertEquals(names, restored.getField(DBEntryField.SYNONYMS).orElseThrow());
    Assertions.assertEquals(names,
        MZmineJsonLibraryEntry.fromSpectralLibraryEntry(restored).synonyms);
  }

  @Test
  void roundTripsProjectXmlAndLoadsLegacyText() throws Exception {
    final var type = new SynonymsType();
    final var project = new MZmineProjectImpl();
    final var featureList = new ModularFeatureList("test", null);
    final var row = new ModularFeatureListRow(featureList, 1);
    DataTypeTestUtils.testSaveLoad(type, List.of("name;with;semicolons", "1,2-name"), project,
        featureList, row, null, null);
    DataTypeTestUtils.testSaveLoad(type, List.of(), project, featureList, row, null, null);
    DataTypeTestUtils.testSaveLoad(type, null, project, featureList, row, null, null);
    final var reader = XMLInputFactory.newFactory()
        .createXMLStreamReader(new StringReader("<datatype>1,2-dichlorobenzene</datatype>"));
    try {
      reader.nextTag();
      Assertions.assertEquals(List.of("1,2-dichlorobenzene"),
          type.loadFromXML(reader, project, featureList, row, null, null));
    } finally {
      reader.close();
    }
  }

  @Test
  void retainsRepeatedSynonymsFromExistingMassbankLibrary() throws Exception {
    final File file = new File(getClass().getClassLoader()
        .getResource("spectral_libraries/integration_tests/massbank_nist_for_tests.msp").toURI());
    final List<SpectralLibraryEntry> entries = new ArrayList<>();
    final var parser = new AutoLibraryParser(100,
        (parsed, alreadyProcessed) -> entries.addAll(parsed));
    Assertions.assertTrue(parser.parse(null, file, new SpectralLibrary(null, file)));
    Assertions.assertEquals(List.of("TBEP"),
        entries.getFirst().getField(DBEntryField.SYNONYMS).orElseThrow());
    Assertions.assertEquals(List.of("Triisobutyl phosphate", "tris(2-methylpropyl) phosphate"),
        entries.get(1).getField(DBEntryField.SYNONYMS).orElseThrow());
  }
}

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

package datamodel;

import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.modifiers.NullColumnType;
import io.github.mzmine.datamodel.features.types.numbers.embeddings.AbstractMLEmbeddingType;
import io.github.mzmine.datamodel.features.types.numbers.embeddings.DreaMSEmbeddingType_1_0;
import io.github.mzmine.datamodel.features.types.numbers.embeddings.MS2DeepscoreEmbeddingType_2_0;
import io.github.mzmine.datamodel.features.types.numbers.scores.MLModelId;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralDBEntry;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the runtime-only ML embedding cache wiring (DataType ↔ DBEntryField ↔ MLModelId).
 * No DJL required.
 */
public class MLEmbeddingTypesTest {

  @Test
  public void mlModelId_to_DBEntryField_roundTrip() {
    for (final MLModelId id : MLModelId.values()) {
      final DBEntryField field = id.getEmbeddingField();
      Assertions.assertTrue(field.isRuntimeOnly(), "embedding field must be runtime-only: " + id);
      final Class<? extends DataType> dataType = field.getDataType();
      final AbstractMLEmbeddingType instance = (AbstractMLEmbeddingType) DataTypes.get(dataType);
      Assertions.assertNotNull(instance, "DataType not registered: " + dataType);
      Assertions.assertEquals(id, instance.getMLModelId(),
          "MLModelId round-trip failed: " + id + " -> " + field + " -> " + dataType);
    }
  }

  @Test
  public void mlEmbeddingTypes_areHiddenColumns() {
    Assertions.assertTrue(
        DataTypes.get(MS2DeepscoreEmbeddingType_2_0.class) instanceof NullColumnType,
        "MS2DeepscoreEmbeddingType must implement NullColumnType");
    Assertions.assertTrue(DataTypes.get(DreaMSEmbeddingType_1_0.class) instanceof NullColumnType,
        "DreaMSEmbeddingType must implement NullColumnType");
  }

  @Test
  public void saveToXML_skipsRuntimeOnlyFields() throws Exception {
    final SpectralDBEntry entry = new SpectralDBEntry(null, new double[]{100.0, 200.0},
        new double[]{1.0, 2.0});
    entry.putAll(
        Map.of(DBEntryField.NAME, "test compound", DBEntryField.ML_EMBEDDING_MS2DEEPSCORE_2_0,
            new float[]{0.1f, 0.2f, 0.3f}));

    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    final XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(baos);
    writer.writeStartDocument();
    entry.saveToXML(writer);
    writer.writeEndDocument();
    writer.close();

    final String xml = baos.toString();
    Assertions.assertFalse(xml.contains("ML_EMBEDDING"),
        "runtime-only field leaked into XML: " + xml);
    Assertions.assertTrue(xml.contains("test compound"), "regular field missing from XML: " + xml);
  }

  @Test
  public void equalsAndHashCode_ignoreRuntimeOnlyFields() {
    final SpectralDBEntry a = new SpectralDBEntry(null, new double[]{100.0}, new double[]{1.0});
    final SpectralDBEntry b = new SpectralDBEntry(null, new double[]{100.0}, new double[]{1.0});
    a.putAll(Map.of(DBEntryField.NAME, "compound"));
    b.putAll(Map.of(DBEntryField.NAME, "compound"));
    Assertions.assertEquals(a, b);
    Assertions.assertEquals(a.hashCode(), b.hashCode());

    // float[] references differ even when contents are equal — must not break equality
    a.putAll(Map.of(DBEntryField.ML_EMBEDDING_DREAMS_1_0, new float[]{0.1f, 0.2f}));
    b.putAll(Map.of(DBEntryField.ML_EMBEDDING_DREAMS_1_0, new float[]{0.9f, 0.8f}));
    Assertions.assertEquals(a, b, "runtime-only fields must not affect equals");
    Assertions.assertEquals(a.hashCode(), b.hashCode(),
        "runtime-only fields must not affect hashCode");
  }
}

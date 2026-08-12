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

package io.github.mzmine.datamodel.features.types.numbers.embeddings;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.modifiers.NullColumnType;
import io.github.mzmine.datamodel.features.types.numbers.scores.MLModelId;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Runtime-only cache slot for an ML embedding vector of one library entry. Carries the
 * {@link MLModelId} so a given {@link io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry}
 * can hold embeddings from several models in parallel. Implements {@link NullColumnType} because
 * the vector has no useful tabular representation. saveToXML/loadFromXML are intentional no-ops —
 * embeddings are recomputed when the library is reloaded; persistence can be wired later without
 * changing the data-model shape.
 */
public abstract class AbstractMLEmbeddingType extends DataType<float[]> implements NullColumnType {

  public abstract @NotNull MLModelId getMLModelId();

  @Override
  public Class<float[]> getValueClass() {
    return float[].class;
  }

  @Override
  public Property<float[]> createProperty() {
    return new SimpleObjectProperty<>();
  }

  @Override
  public boolean getDefaultVisibility() {
    return false;
  }

  @Override
  public void saveToXML(@NotNull XMLStreamWriter writer, @Nullable Object value,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) {
    // runtime-only: never persist embedding vectors
  }

  @Override
  public Object loadFromXML(@NotNull XMLStreamReader reader, @NotNull MZmineProject project,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) {
    return null;
  }
}

/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.datamodel.features.types.analysis;

import io.github.mzmine.datamodel.features.SimpleModularDataModel;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.abstr.SimpleSubColumnsType;
import io.github.mzmine.datamodel.features.types.modifiers.SubColumnsFactory;
import io.github.mzmine.modules.dataprocessing.id_ion_type.IonTypeAnalysisResults;
import java.util.List;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import org.jetbrains.annotations.NotNull;

/**
 * Type of {@link IonTypeAnalysisResults}
 */
public class IonTypeAnalysisType extends SimpleSubColumnsType<IonTypeAnalysisResults> implements
    SubColumnsFactory {

  @NotNull
  @Override
  public String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "ion_type_analysis";
  }

  @NotNull
  @Override
  public String getHeaderString() {
    return "Ion type analysis";
  }

  @Override
  public @NotNull List<DataType> getSubDataTypes() {
    return IonTypeAnalysisResults.getSubTypes();
  }

  @Override
  public Property<IonTypeAnalysisResults> createProperty() {
    return new SimpleObjectProperty<>();
  }

  @Override
  protected IonTypeAnalysisResults createRecord(final SimpleModularDataModel model) {
    return IonTypeAnalysisResults.create(model);
  }

  @Override
  public Class<IonTypeAnalysisResults> getValueClass() {
    return IonTypeAnalysisResults.class;
  }

  @Override
  public boolean getDefaultVisibility() {
    return false;
  }
}
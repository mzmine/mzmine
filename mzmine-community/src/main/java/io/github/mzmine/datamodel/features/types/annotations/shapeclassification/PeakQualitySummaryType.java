/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.datamodel.features.types.annotations.shapeclassification;

import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.RowBinding;
import io.github.mzmine.datamodel.features.SimpleModularDataModel;
import io.github.mzmine.datamodel.features.SimpleRowBinding;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.abstr.SimpleSubColumnsType;
import io.github.mzmine.datamodel.features.types.exceptions.UndefinedRowBindingException;
import io.github.mzmine.datamodel.features.types.modifiers.BindingsType;
import io.github.mzmine.modules.dataprocessing.filter_featurefilter.peak_fitter.PeakDimension;
import io.github.mzmine.modules.dataprocessing.filter_featurefilter.peak_fitter.PeakQualitySummary;
import io.github.mzmine.modules.dataprocessing.filter_featurefilter.peak_fitter.PeakShapeClassification;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import org.jetbrains.annotations.NotNull;

public abstract class PeakQualitySummaryType extends SimpleSubColumnsType<PeakQualitySummary> {

  @Override
  public @NotNull List<DataType> getSubDataTypes() {
    return List.of(DataTypes.get(PeakDimensionType.class),
        DataTypes.get(PeakShapeClassificationType.class),
        DataTypes.get(ShapeClassificationScoreType.class));
  }

  @Override
  protected PeakQualitySummary createRecord(SimpleModularDataModel model) {
    return new PeakQualitySummary(model.get(PeakDimensionType.class),
        model.get(PeakShapeClassificationType.class),
        model.get(ShapeClassificationScoreType.class));
  }

  @Override
  public Property<PeakQualitySummary> createProperty() {
    return new SimpleObjectProperty<>();
  }

  @Override
  public Class<PeakQualitySummary> getValueClass() {
    return PeakQualitySummary.class;
  }

  @Override
  public @NotNull List<RowBinding> createDefaultRowBindings() {
    return List.of(new SimpleRowBinding(this, BindingsType.CONSENSUS));
  }

  @Override
  public Object evaluateBindings(@NotNull BindingsType bindingType,
      @NotNull List<? extends ModularDataModel> models) {
    return switch (bindingType) {
      case AVERAGE, SUM, MIN, MAX, DIFFERENCE, COUNT, RANGE, LIST ->
          new UndefinedRowBindingException(this, bindingType);
      case CONSENSUS -> {

        final PeakDimension dimension = models.stream().map(model -> model.get(this))
            .filter(Objects::nonNull).map(PeakQualitySummary::dimension).findFirst().orElse(null);

        final PeakShapeClassification consensusClassification = models.stream()
            .map(model -> model.get(this)).filter(Objects::nonNull)
            .map(PeakQualitySummary::classification)
            // group by occurrences of the same classification
            .collect(Collectors.groupingBy(s -> s, Collectors.counting())).entrySet().stream()
            // get the classification that occurs the most
            .max(Comparator.comparingLong((Entry<PeakShapeClassification, Long> o) -> o.getValue())
                .thenComparingInt(o -> o.getKey().ordinal())).map(Entry::getKey).orElse(null);

        final float averageScore = (float) models.stream().map(model -> model.get(this))
            .filter(Objects::nonNull).mapToDouble(PeakQualitySummary::shapeClassificationScore)
            .average().orElse(0d);

        if (dimension == null || consensusClassification == null || averageScore == 0f) {
          yield null;
        }
        yield new PeakQualitySummary(dimension, consensusClassification, averageScore);
      }
    };
  }
}

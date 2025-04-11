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

package io.github.mzmine.datamodel.features.types.numbers;

import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.RowBinding;
import io.github.mzmine.datamodel.features.SimpleRowBinding;
import io.github.mzmine.datamodel.features.types.modifiers.BindingsType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.IntegerType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * This type describes the negative or positive charge of an ion (feature)
 */
public class ChargeType extends IntegerType {

  @NotNull
  @Override
  public final String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "charge";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Charge";
  }

  @NotNull
  @Override
  public List<RowBinding> createDefaultRowBindings() {
    return List.of(new SimpleRowBinding(this, BindingsType.CONSENSUS));
  }

  /**
   * The consensus charge will be the lowest charge with the maximum count in the feature list row
   *
   * @param bindingType
   * @param models
   * @return
   */
  @Override
  public Object evaluateBindings(@NotNull BindingsType bindingType,
      @NotNull List<? extends ModularDataModel> models) {
    switch (bindingType) {
      case CONSENSUS: {
        Map<Integer, Integer> max = new HashMap<>();
        for (ModularDataModel model : models) {
          if (model != null) {
            Integer charge = model.get(this);
            if (charge != null) {
              // only count non-null
              max.merge(charge, 1, Integer::sum);
            }
          }
        }
        Integer maxCharge = null; // default is null
        Integer maxCount = 0;
        for (var entry : max.entrySet()) {
          if (entry.getValue() > maxCount) {
            maxCount = entry.getValue();
            maxCharge = entry.getKey();
          }
        }
        return maxCharge;
      }
      default:
        return super.evaluateBindings(bindingType, models);
    }
  }
}

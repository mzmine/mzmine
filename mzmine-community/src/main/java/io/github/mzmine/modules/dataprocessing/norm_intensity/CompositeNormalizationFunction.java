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

package io.github.mzmine.modules.dataprocessing.norm_intensity;

import io.github.mzmine.util.XMLUtils;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;

/**
 *
 * @param functions a list of all functions to be applied. Each factor is multiplied to get the
 *                  total factor.
 */
record CompositeNormalizationFunction(
    @NotNull List<@NotNull NormalizationFunction> functions) implements NormalizationFunction {

  public static final String XML_TYPE = "composite_list_normalization";

  CompositeNormalizationFunction(@NotNull List<@NotNull NormalizationFunction> functions) {
    this.functions = mergeAllFactorFunctions(functions);
  }

  /**
   * @return composite function or a simpler normalization function if the list of functions can be
   * simplified by merging functions
   */
  public static @NotNull NormalizationFunction createComposite(
      @NotNull List<@NotNull NormalizationFunction> functions) {
    final CompositeNormalizationFunction composite = new CompositeNormalizationFunction(functions);
    return composite.size() == 1 ? composite.functions.getFirst() : composite;
  }

  private int size() {
    return functions.size();
  }

  /**
   * Merges all functions of type {@link FactorNormalizationFunction} to simplify list. The factor
   * function will then be the last function in the list. Currently all functions apply factors and
   * therefore the order is irrelevant. Other functions that are feature specific cannot be merged
   * into existing functions.
   *
   * @return a new list of functions
   */
  public static @NotNull List<@NotNull NormalizationFunction> mergeAllFactorFunctions(
      @NotNull List<@NotNull NormalizationFunction> functions) {
    if (functions.size() <= 1) {
      return functions;
    }

    final ArrayList<@NotNull NormalizationFunction> merged = new ArrayList<>(4);
    double accumulatedFactor = 1.0;
    boolean hasFactorFunction = false;

    for (NormalizationFunction function : functions) {
      if (function instanceof FactorNormalizationFunction(double factor)) {
        accumulatedFactor *= factor;
        hasFactorFunction = true;
      } else {
        merged.add(function);
      }
    }

    if (hasFactorFunction) {
      merged.add(new FactorNormalizationFunction(accumulatedFactor));
    }

    // immutable copy
    return List.copyOf(merged);
  }

  @Override
  public double getNormalizationFactor(@NotNull Double mz, @NotNull Float rt) {
    double f = 1;
    for (NormalizationFunction function : functions) {
      f *= function.getNormalizationFactor(mz, rt);
    }
    return f;
  }

  @Override
  public @NotNull String getUniqueID() {
    return XML_TYPE;
  }


  @Override
  public void saveToXML(final @NotNull Element functionElement) {
    functionElement.setAttribute(XML_FUNCTION_TYPE_ATTR, getUniqueID());

    final Element subFunctions = functionElement.getOwnerDocument().createElement("subfunctions");

    for (NormalizationFunction function : functions) {
      NormalizationFunction.appendFunctionElement(subFunctions, function);
    }

    functionElement.appendChild(subFunctions);
  }

  public static @NotNull CompositeNormalizationFunction loadFromXML(
      final @NotNull Element functionElement) {

    final Element subfunctions = XMLUtils.findChildElement(functionElement, "subfunctions");

    final List<@NotNull NormalizationFunction> functions = XMLUtils.streamChildElementsByTagName(
        subfunctions, XML_FUNCTION_ELEMENT).map(NormalizationFunction::loadFromXML).toList();

    return new CompositeNormalizationFunction(functions);
  }

}

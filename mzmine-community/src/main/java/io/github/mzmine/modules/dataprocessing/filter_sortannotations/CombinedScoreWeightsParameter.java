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

package io.github.mzmine.modules.dataprocessing.filter_sortannotations;

import io.github.mzmine.parameters.AbstractParameter;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.util.StringUtils;
import java.util.Collection;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

public class CombinedScoreWeightsParameter extends
    AbstractParameter<CombinedScoreWeights, CombinedScoreWeightsComponent> {

  @Nullable
  private CombinedScoreWeights weights = CombinedScoreWeights.DEFAULT_WEIGHTS;

  public CombinedScoreWeightsParameter() {
    this("Combined score weights", """
            Defines the weights on how to combine all scores into a meta score.
            Each score has a tooltip describing how the score is calculated.""",
        CombinedScoreWeights.DEFAULT_WEIGHTS);
  }

  public CombinedScoreWeightsParameter(String name, String description,
      @Nullable CombinedScoreWeights defaultVal) {
    super(name, description, defaultVal);
  }

  @Override
  public CombinedScoreWeightsComponent createEditingComponent() {
    return new CombinedScoreWeightsComponent(getValue());
  }

  @Override
  public void setValueFromComponent(CombinedScoreWeightsComponent comp) {
    setValue(comp.getValue());
  }

  @Override
  public void setValueToComponent(CombinedScoreWeightsComponent comp,
      @Nullable CombinedScoreWeights newValue) {
    comp.setValue(newValue);
  }

  @Override
  public CombinedScoreWeights getValue() {
    return weights;
  }

  @Override
  public void setValue(CombinedScoreWeights newValue) {
    weights = newValue;
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if (weights == null) {
      errorMessages.add(name + " is undefined");
      return false;
    }
    return true;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    String mzStr = xmlElement.getAttribute("mz");
    if (StringUtils.isBlank(mzStr)) {
      setValue(null);
      return;
    }

    String isotopesStr = xmlElement.getAttribute("isotope_pattern");
    String ms2Str = xmlElement.getAttribute("ms2");
    String rtStr = xmlElement.getAttribute("rt");
    String riStr = xmlElement.getAttribute("ri");
    String ccsStr = xmlElement.getAttribute("ccs");

    weights = new CombinedScoreWeights(Double.parseDouble(mzStr), Double.parseDouble(rtStr),
        Double.parseDouble(riStr), Double.parseDouble(ccsStr), Double.parseDouble(ms2Str),
        Double.parseDouble(isotopesStr));
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (weights == null) {
      return;
    }

    xmlElement.setAttribute("isotope_pattern", String.valueOf(weights.isotopes()));
    xmlElement.setAttribute("ms2", String.valueOf(weights.ms2()));
    xmlElement.setAttribute("mz", String.valueOf(weights.mz()));
    xmlElement.setAttribute("rt", String.valueOf(weights.rt()));
    xmlElement.setAttribute("ri", String.valueOf(weights.ri()));
    xmlElement.setAttribute("ccs", String.valueOf(weights.ccs()));
  }

  @Override
  public UserParameter<CombinedScoreWeights, CombinedScoreWeightsComponent> cloneParameter() {
    return new CombinedScoreWeightsParameter(name, description, weights);
  }
}

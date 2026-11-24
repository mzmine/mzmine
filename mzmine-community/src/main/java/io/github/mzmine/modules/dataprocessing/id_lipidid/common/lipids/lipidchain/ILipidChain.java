/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain;

import java.util.List;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.jetbrains.annotations.NotNull;

public interface ILipidChain {

  String getChainAnnotation();

  int getNumberOfCarbons();

  int getNumberOfDBEs();

  IMolecularFormula getChainMolecularFormula();

  LipidChainType getLipidChainType();

  int getNumberOfOxygens();

  /** Returns localized double-bond positions, or an empty list for a composition-only chain. */
  default @NotNull List<DoubleBondPosition> getDoubleBondPositions() {
    return List.of();
  }

  /**
   * Returns {@code true} only when every DBE has one distinct localized position. Partial position
   * lists are intentionally not considered localized because {@code 18:2(9)} is ambiguous.
   */
  default boolean hasFullyLocalizedDoubleBonds() {
    return getNumberOfDBEs() > 0 && getDoubleBondPositions().size() == getNumberOfDBEs()
        && getDoubleBondPositions().stream().map(DoubleBondPosition::position).distinct().count()
        == getNumberOfDBEs();
  }

  /** Returns the same chain without positional detail for molecular-species-level rendering. */
  default @NotNull ILipidChain withoutDoubleBondPositions() {
    return this;
  }

  void saveToXML(XMLStreamWriter writer) throws XMLStreamException;

}

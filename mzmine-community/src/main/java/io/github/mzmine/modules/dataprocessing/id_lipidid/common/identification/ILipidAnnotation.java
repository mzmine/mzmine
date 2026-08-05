/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification;

import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.ILipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidAnnotationLevel;
import io.github.mzmine.modules.dataprocessing.id_lipidid.utils.LipidFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.openscience.cdk.interfaces.IMolecularFormula;

public sealed interface ILipidAnnotation permits SpeciesLevelAnnotation,
    MolecularSpeciesLevelAnnotation {

  ILipidClass getLipidClass();

  String getAnnotation();

  default String getSpeciesLevelAnnotation() {
    return LipidFactory.buildAnnotation(getLipidClass(), getChainsCarbonCount(),
        getChainsDoubleBondCount(), getSpeciesLevelOxygens());
  }

  /**
   *
   * @param level The annotation level.
   * @return A string defining this lipid annotation. If molecular species is requested and this
   * annotation is only a species level, the species level annotation is returned.
   */
  default String getAnnotation(LipidAnnotationLevel level) {
    return switch (level) {
      case SPECIES_LEVEL -> getSpeciesLevelAnnotation();
      case MOLECULAR_SPECIES_LEVEL -> getAnnotation();
    };
  }

  LipidAnnotationLevel getLipidAnnotationLevel();

  IMolecularFormula getMolecularFormula();

  /**
   *
   * @return Number of carbon atoms in all chains.
   */
  int getChainsCarbonCount();

  /**
   * @return Number of double bonds in all chains.
   */
  int getChainsDoubleBondCount();

  int getSpeciesLevelOxygens();

  void saveToXML(XMLStreamWriter writer) throws XMLStreamException;

}

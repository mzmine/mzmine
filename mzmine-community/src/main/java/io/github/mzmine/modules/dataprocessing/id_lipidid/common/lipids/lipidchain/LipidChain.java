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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.interfaces.IMolecularFormula;

public class LipidChain implements ILipidChain {

  private final String chainAnnotation;
  private final IMolecularFormula molecularFormula;
  private final int numberOfCarbons;
  private final int numberOfDBEs;
  private final LipidChainType lipidChainType;
  private final int numberOfOxygens;

  public LipidChain(String chainAnnotation, IMolecularFormula molecularFormula, int numberOfCarbons,
      int numberOfDBEs, LipidChainType lipidChainType) {
    this.chainAnnotation = chainAnnotation;
    this.molecularFormula = molecularFormula;
    this.numberOfCarbons = numberOfCarbons;
    this.numberOfDBEs = numberOfDBEs;
    this.lipidChainType = lipidChainType;
    this.numberOfOxygens = lipidChainType.getFixNumberOfOxygens();
  }

  public LipidChain(String chainAnnotation, IMolecularFormula molecularFormula, int numberOfCarbons,
      int numberOfDBEs, LipidChainType lipidChainType, int numberOfAdditionalOxygens) {
    this.chainAnnotation = chainAnnotation;
    this.molecularFormula = molecularFormula;
    this.numberOfCarbons = numberOfCarbons;
    this.numberOfDBEs = numberOfDBEs;
    this.lipidChainType = lipidChainType;
    this.numberOfOxygens = lipidChainType.getFixNumberOfOxygens() + numberOfAdditionalOxygens;
  }

  public String getChainAnnotation() {
    return chainAnnotation;
  }


  public IMolecularFormula getChainMolecularFormula() {
    return molecularFormula;
  }


  public int getNumberOfCarbons() {
    return numberOfCarbons;
  }


  public int getNumberOfDBEs() {
    return numberOfDBEs;
  }


  public LipidChainType getLipidChainType() {
    return lipidChainType;
  }


  public int getNumberOfOxygens() {
    return numberOfOxygens;
  }

  public void saveToXML(final @NotNull XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement(LipidChainFactory.XML_CHAIN_ELEMENT);
    LipidChainFactory.writeChainBody(writer, this);
    writer.writeEndElement();
  }
}

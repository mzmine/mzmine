/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.util.structure;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.inchi.InChIGenerator;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

public class StructureParser {

  private static final Logger logger = Logger.getLogger(StructureParser.class.getName());
  private final InChIGeneratorFactory inchifactory;
  private SmilesParser smilesparser;

  public StructureParser() throws CDKException {
    // Parse the SMILES and create an IAtomContainer
    IChemObjectBuilder builder = DefaultChemObjectBuilder.getInstance();
    this.smilesparser = new SmilesParser(builder);
    inchifactory = InChIGeneratorFactory.getInstance();
  }

  @Nullable
  public String getInChIKey(String structure, StructureInputType inputType) {
    var molecule = getAtomContainer(structure, inputType);
    if (molecule == null) {
      return null;
    }
    InChIGenerator generator = null;
    try {
      generator = inchifactory.getInChIGenerator(molecule);
      // Generate the InChI Key
      return generator.getInchiKey();
    } catch (CDKException e) {
      logger.log(Level.WARNING, "Cannot parse 'structure' %s as %s".formatted(structure, inputType),
          e);
      return null;
    }
  }

  @Nullable
  public IAtomContainer getAtomContainer(String structure, StructureInputType inputType) {
    // Define a SMILES representation
    // String smiles = "OC(=O)CC(O)(C(=O)O)CC(=O)O";

    try {
      return switch (inputType) {
        case SMILES -> smilesparser.parseSmiles(structure);
        case INCHI ->
            inchifactory.getInChIToStructure(structure, DefaultChemObjectBuilder.getInstance())
                .getAtomContainer();
      };
    } catch (CDKException e) {
      logger.log(Level.WARNING, "Cannot parse 'structure' %s as %s".formatted(structure, inputType),
          e);
    }
    return null;
  }
}

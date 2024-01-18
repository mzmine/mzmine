/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

package io.github.mzmine.datamodel.structures;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

public class StructureParser {

  private static final Logger logger = Logger.getLogger(StructureParser.class.getName());
  private final InChIGeneratorFactory inchiFactory;
  private final boolean verbose;
  private SmilesParser smilesParser;

  public StructureParser(boolean verbose) throws CDKException {
    this.verbose = verbose;
    // Parse the SMILES and create an IAtomContainer
    IChemObjectBuilder builder = DefaultChemObjectBuilder.getInstance();
    this.smilesParser = new SmilesParser(builder);
    inchiFactory = InChIGeneratorFactory.getInstance();
  }

  @Nullable
  public SimpleMolecularStructure parseStructure(@Nullable String structure,
      @NotNull StructureInputType inputType) {
    if (structure == null) {
      return null;
    }
    try {
      IAtomContainer molecule = switch (inputType) {
        case SMILES -> smilesParser.parseSmiles(structure);
        case INCHI ->
            inchiFactory.getInChIToStructure(structure, DefaultChemObjectBuilder.getInstance())
                .getAtomContainer();
      };

      return molecule == null || molecule.getAtomCount() == 0 ? null
          : new SimpleMolecularStructure(molecule);
    } catch (CDKException e) {
      String message = "Cannot parse 'structure' %s as %s".formatted(structure, inputType);
      if (verbose) {
        logger.log(Level.WARNING, message, e);
      } else {
        logger.log(Level.WARNING, message);
      }
      return null;
    }
  }

  public InChIGeneratorFactory getInchiFactory() {
    return inchiFactory;
  }

  public boolean isVerbose() {
    return verbose;
  }

  @Nullable
  public MolecularStructure parseStructure(@Nullable String smiles, @Nullable String inchi) {
    MolecularStructure struc = null;

    struc = parseStructure(smiles, StructureInputType.SMILES);

    if (struc == null) {
      struc = parseStructure(inchi, StructureInputType.INCHI);
    }

    return struc;
  }
}

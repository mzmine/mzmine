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
import org.openscience.cdk.smarts.SmartsPattern;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

/**
 * Parsing of inchi and smiles structures harmonized
 */
public class StructureParser {

  private static final Logger logger = Logger.getLogger(StructureParser.class.getName());
  private final InChIGeneratorFactory inchiFactory;
  private final boolean verbose;
  private final SmilesParser smilesParser;

  private static final StructureParser SILENT_INSTANCE = new StructureParser(false);


  public StructureParser() {
    this(false);
  }

  public StructureParser(boolean verbose) {
    this.verbose = verbose;
    // Parse the SMILES and create an IAtomContainer
    IChemObjectBuilder builder = DefaultChemObjectBuilder.getInstance();
    this.smilesParser = new SmilesParser(builder);
    InChIGeneratorFactory inchiFactory = null;
    try {
      inchiFactory = InChIGeneratorFactory.getInstance();
    } catch (Exception e) {
      logger.warning(
          "Failed to load InChI generator factory in structure parser. SMILES will work");
    }
    this.inchiFactory = inchiFactory;
  }

  /**
   * Default silent instance of structure parser
   */
  public static StructureParser silent() {
    return SILENT_INSTANCE;
  }

  @Nullable
  public MolecularStructure parseStructure(@Nullable String structure,
      @NotNull StructureInputType inputType) {
    if (structure == null || structure.isBlank() || structure.equalsIgnoreCase("n/a")
        || structure.equalsIgnoreCase("na")) {
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

  @Nullable
  public SmartsMolecularStructure parseSmarts(@Nullable String smarts) {
    if (smarts == null || smarts.isBlank() || smarts.equalsIgnoreCase("n/a")
        || smarts.equalsIgnoreCase("na")) {
      return null;
    }
    try {
      final SmartsPattern smartsPattern = SmartsPattern.create(smarts,
          DefaultChemObjectBuilder.getInstance());
      smartsPattern.setPrepare(true);
      return new SmartsMolecularStructure(smartsPattern, smarts);
    } catch (Exception e) {
      String message = "Cannot parse 'smarts' %s as SMARTS".formatted(smarts);
      if (verbose) {
        logger.log(Level.WARNING, message, e);
      } else {
        logger.log(Level.WARNING, message);
      }
    }
    return null;
  }


  public InChIGeneratorFactory getInchiFactory() {
    return inchiFactory;
  }

  public boolean isVerbose() {
    return verbose;
  }

  @Nullable
  public MolecularStructure parseStructure(@Nullable String smiles, @Nullable String inchi) {
    MolecularStructure mol = parseStructure(smiles, StructureInputType.SMILES);

    if (mol == null) {
      mol = parseStructure(inchi, StructureInputType.INCHI);
    }

    if (mol != null) {
      // Suppress the hydrogens
      AtomContainerManipulator.suppressHydrogens(mol.structure());
    }

    return mol;
  }
}

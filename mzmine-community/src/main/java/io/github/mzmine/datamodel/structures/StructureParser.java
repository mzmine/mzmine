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
  public SimpleMolecularStructure parseStructure(@Nullable String structure,
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

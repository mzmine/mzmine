package io.github.mzmine.datamodel.structures;


import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

/**
 * All values are computed on demand. So if accessed often use
 * {@link #precomputeValues(StructureParser)} to create a {@link ComplexMolecularStructure} with
 * direct value access.
 *
 * @param structure
 */
public record SimpleMolecularStructure(@NotNull IAtomContainer structure) implements
    MolecularStructure {

  private static final Logger logger = Logger.getLogger(SimpleMolecularStructure.class.getName());

  @Nullable
  public IMolecularFormula formula() {
    AtomContainerManipulator.getMass(structure(), AtomContainerManipulator.MonoIsotopic);
    return MolecularFormulaManipulator.getMolecularFormula(structure());
  }

  public double monoIsotopicMass() {
    return AtomContainerManipulator.getMass(structure(), AtomContainerManipulator.MonoIsotopic);
  }

  public double mostAbundantMass() {
    return AtomContainerManipulator.getMass(structure(), AtomContainerManipulator.MostAbundant);
  }

  public int totalFormalCharge() {
    return AtomContainerManipulator.getTotalFormalCharge(structure());
  }

  @Nullable
  public String inChIKey(@NotNull StructureParser parser) {
    try {
      // Generate the InChI Key
      return parser.getInchiFactory().getInChIGenerator(structure()).getInchiKey();
    } catch (CDKException e) {
      String message = "Cannot parse 'structure' %s".formatted(structure());
      if (parser.isVerbose()) {
        logger.log(Level.WARNING, message, e);
      } else {
        logger.log(Level.WARNING, message);
      }
      return null;
    }
  }

  /**
   * Precompute values in case they are access more often
   *
   * @param parser structure parser, can be the same that was used to create this structure
   * @return a structure with precomputed values
   */
  public ComplexMolecularStructure precomputeValues(StructureParser parser) {
    return new ComplexMolecularStructure(structure, formula(), inChIKey(parser), monoIsotopicMass(),
        mostAbundantMass(), totalFormalCharge());
  }
}
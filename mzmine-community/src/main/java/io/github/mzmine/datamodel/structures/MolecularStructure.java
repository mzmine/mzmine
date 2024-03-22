package io.github.mzmine.datamodel.structures;


import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IMolecularFormula;

public interface MolecularStructure {

  Logger logger = Logger.getLogger(MolecularStructure.class.getName());

  @NotNull IAtomContainer structure();

  @Nullable IMolecularFormula formula();

  double monoIsotopicMass();

  double mostAbundantMass();

  int totalFormalCharge();

  @Nullable String inChIKey(@NotNull StructureParser parser);
}
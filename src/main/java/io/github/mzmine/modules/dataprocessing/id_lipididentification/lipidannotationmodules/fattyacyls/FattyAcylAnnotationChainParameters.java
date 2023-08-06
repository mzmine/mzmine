package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidannotationmodules.fattyacyls;

import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;

public class FattyAcylAnnotationChainParameters extends SimpleParameterSet {

  public static final IntegerParameter minChainLength = new IntegerParameter("Minimum chain length",
      "Enter the shortest possible chain length.", 12, true, 1, 60);

  public static final IntegerParameter maxChainLength = new IntegerParameter("Maximum chain length",
      "Enter the longest possible chain length.", 26, true, 1, 60);

  public static final IntegerParameter minDBEs = new IntegerParameter("Minimum number of DBEs",
      "Enter the minimum number of double bond equivalents.", 0, true, 0, 30);

  public static final IntegerParameter maxDBEs = new IntegerParameter("Maximum number of DBEs",
      "Enter the maximum number of double bond equivalents.", 6, true, 0, 30);

  public static final BooleanParameter onlySearchForEvenChainLength = new BooleanParameter(
      "Only search for even chain length", "Only search for even chain length.");

  public FattyAcylAnnotationChainParameters() {
    super(minChainLength, maxChainLength, minDBEs, maxDBEs, onlySearchForEvenChainLength);
  }

}

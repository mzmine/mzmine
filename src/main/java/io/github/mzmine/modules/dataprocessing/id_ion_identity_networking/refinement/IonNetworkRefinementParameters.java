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

package io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.refinement;


import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import org.jetbrains.annotations.NotNull;

/**
 * Refinement to MS annotation
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class IonNetworkRefinementParameters extends SimpleParameterSet {

  // NOT INCLUDED in sub
  // General parameters
  public static final FeatureListsParameter PEAK_LISTS = new FeatureListsParameter();


  public static final OptionalParameter<IntegerParameter> MIN_NETWORK_SIZE =
      new OptionalParameter<>(new IntegerParameter("Minimum size", "Minimum network size", 2));
  public static final OptionalParameter<IntegerParameter> TRUE_THRESHOLD = // todo - better description
      new OptionalParameter<>(new IntegerParameter("Delete smaller networks: Link threshold",
          "links>=true threshold, then delete all other occurance in annotation networks", 4));

  public static final BooleanParameter DELETE_SMALL_NO_MAJOR = new BooleanParameter(
      "Delete small networks without major ion",
      "Delete small networks without H+, Na+, NH4+, H-, Cl-, formic acid- adduct. \nMaximum allowed in-source modifications: H+(3, e.g., -H2O), Na(0), NH4(1), H-(2), Cl(0), FA(0).",
      false);
  public static final BooleanParameter DELETE_ROWS_WITHOUT_ID =
      new BooleanParameter("Delete rows witout ion id", "Keeps only rows with ion ids", false);
  public static final BooleanParameter DELETE_WITHOUT_MONOMER =
      new BooleanParameter("Delete networks without monomer",
          "Deletes all networks without monomer or with 1 monomer and >=3 multimers", true);
  // public static final BooleanParameter DELETE_XMERS_ON_MSMS = new BooleanParameter(
  // "Use MS/MS xmer verification to exclude",
  // "If an xmer was identified by MS/MS annotation of fragment xmers, then use this identification
  // and delete rest",
  // true);


  // Constructor
  public IonNetworkRefinementParameters() {
    this(false);
  }

  public IonNetworkRefinementParameters(boolean isSub) {
    super(isSub ? // no peak list and rt tolerance
        new Parameter[] {MIN_NETWORK_SIZE, DELETE_SMALL_NO_MAJOR, TRUE_THRESHOLD,
            DELETE_WITHOUT_MONOMER, DELETE_ROWS_WITHOUT_ID}
        : new Parameter[] {PEAK_LISTS, MIN_NETWORK_SIZE, DELETE_SMALL_NO_MAJOR, TRUE_THRESHOLD,
            DELETE_WITHOUT_MONOMER, DELETE_ROWS_WITHOUT_ID});
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}

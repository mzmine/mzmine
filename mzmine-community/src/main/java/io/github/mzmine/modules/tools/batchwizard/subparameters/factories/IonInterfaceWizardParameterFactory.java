/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.tools.batchwizard.subparameters.factories;

import com.google.common.collect.Range;
import io.github.mzmine.modules.tools.batchwizard.subparameters.IonInterfaceDirectAndFlowInjectWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.IonInterfaceGcElectronImpactWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.IonInterfaceHplcWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.IonInterfaceImagingWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance.Unit;
import org.jetbrains.annotations.NotNull;

/**
 * the defaults should not change the name of enum values. if strings are needed, override the
 * toString method
 */
public enum IonInterfaceWizardParameterFactory implements WizardParameterFactory {
  /**
   * Soft ionization in LC-MS
   */
  HPLC, UHPLC, HILIC,
  /**
   * Chemical ionization uses LC workflow
   */
  GC_CI,
  /**
   * GC-EI is a different workflow, GC uses the LC workflow
   */
  GC_EI,
  /**
   * imaging workflows
   */
  MALDI, LDI, DESI, SIMS,
  /**
   * Direct infusion
   */
  DIRECT_INFUSION,
  /**
   * FLOW_INJECTION
   */
  FLOW_INJECT;


  @Override
  public String toString() {
    return switch (this) {
      case HPLC, UHPLC, HILIC, MALDI, LDI, DESI, SIMS -> super.toString();
      case GC_EI -> "GC-EI";
      case GC_CI -> "GC-CI";
      case DIRECT_INFUSION -> "Direct";
      case FLOW_INJECT -> "Flow inject";
    };
  }

  @Override
  public @NotNull String getUniqueID() {
    return name();
  }

  /**
   * Create parameters from defaults
   */
  public WizardStepParameters create() {
    // override defaults
    return switch (this) {
      case HPLC -> new IonInterfaceHplcWizardParameters(this, true, 15, 4, Range.closed(0.5, 60d),
          new RTTolerance(0.1f, Unit.MINUTES), new RTTolerance(0.08f, Unit.MINUTES),
          new RTTolerance(0.4f, Unit.MINUTES));
      case UHPLC -> new IonInterfaceHplcWizardParameters(this, true, 15, 4, Range.closed(0.3, 30d),
          new RTTolerance(0.05f, Unit.MINUTES), new RTTolerance(0.04f, Unit.MINUTES),
          new RTTolerance(0.1f, Unit.MINUTES));
      case HILIC -> new IonInterfaceHplcWizardParameters(this, true, 15, 5, Range.closed(0.3, 30d),
          new RTTolerance(0.15f, Unit.MINUTES), new RTTolerance(3, Unit.SECONDS),
          new RTTolerance(6, Unit.SECONDS));
      case GC_CI -> new IonInterfaceHplcWizardParameters(this, true, 30, 6, Range.closed(0.3, 30d),
          new RTTolerance(0.05f, Unit.MINUTES), new RTTolerance(0.04f, Unit.MINUTES),
          new RTTolerance(0.1f, Unit.MINUTES));
      // different workflow for GC-EI
      case GC_EI -> new IonInterfaceGcElectronImpactWizardParameters(this, false, true,
          Range.closed(0.3, 30d), new RTTolerance(0.05f, Unit.MINUTES),
          new RTTolerance(0.04f, Unit.MINUTES), new RTTolerance(0.1f, Unit.MINUTES), 4,
          Range.closed(0.001, 0.06));
      // parameters for imaging
      case MALDI, LDI, DESI, SIMS -> new IonInterfaceImagingWizardParameters(this, 25, false);
      //
      case DIRECT_INFUSION, FLOW_INJECT ->
          new IonInterfaceDirectAndFlowInjectWizardParameters(this, 5);
    };
  }

  public boolean isImaging() {
    return switch (this) {
      case MALDI, LDI, DESI, SIMS -> true;
      case HPLC, UHPLC, HILIC, GC_CI, GC_EI, DIRECT_INFUSION, FLOW_INJECT -> false;
    };
  }

  /**
   * Group values for easier decisions
   */
  public IonIterfaceGroup group() {
    return switch (this) {
      case MALDI, LDI, DESI, SIMS -> IonIterfaceGroup.SPATIAL_IMAGING;
      case HPLC, UHPLC, HILIC, GC_CI -> IonIterfaceGroup.CHROMATOGRAPHY_SOFT;
      case DIRECT_INFUSION, FLOW_INJECT -> IonIterfaceGroup.DIRECT_AND_FLOW;
      case GC_EI -> IonIterfaceGroup.CHROMATOGRAPHY_HARD;
    };
  }

  /**
   * Not all combinations work.
   *
   * @return supported combinations
   */
  public IonMobilityWizardParameterFactory[] getMatchingImsPresets() {
    return switch (this) {
      case DIRECT_INFUSION, FLOW_INJECT, HPLC, UHPLC, HILIC, GC_CI, MALDI, LDI, DESI, SIMS ->
          IonMobilityWizardParameterFactory.values();
      case GC_EI ->
          new IonMobilityWizardParameterFactory[]{IonMobilityWizardParameterFactory.NO_IMS};
    };
  }

}

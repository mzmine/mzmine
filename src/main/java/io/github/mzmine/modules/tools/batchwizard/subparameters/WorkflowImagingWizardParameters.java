package io.github.mzmine.modules.tools.batchwizard.subparameters;

import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.WorkflowWizardParameterFactory;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;

public final class WorkflowImagingWizardParameters extends WorkflowWizardParameters {

  public static final BooleanParameter CORRELATE_IMAGES = new BooleanParameter("Co-localize images",
      """
          Co-localize images to group ions and isotopes of the same molecule, as well as co-located molecules. 
          """, true);

  public static final BooleanParameter USE_MEDIAN_FILTER = new BooleanParameter("Use median filter",
      """
          Smooth over pixels to reduce noise and remove outliers. 
          """, true);

  public static final BooleanParameter USE_QUANTILE_FILTER = new BooleanParameter(
      "Filter out low signals", """
      Only consider intensities above a selected percentile. 
      """, true);

  public static final BooleanParameter REMOVE_HOTSPOTS = new BooleanParameter("Remove hotspot", """
      Ignore very high intensity outliers. 
      """, true);

  public WorkflowImagingWizardParameters() {
    super(WorkflowWizardParameterFactory.IMAGING,
        // actual parameters
        CORRELATE_IMAGES, USE_MEDIAN_FILTER, USE_QUANTILE_FILTER, REMOVE_HOTSPOTS);
  }

  public WorkflowImagingWizardParameters(final boolean correlateImagesActive,
      final boolean useMedianFilter, final boolean useQuantileFilter,
      final boolean removeHotspots) {
    this();
    setParameter(CORRELATE_IMAGES, correlateImagesActive);
    setParameter(USE_MEDIAN_FILTER, useMedianFilter);
    setParameter(USE_QUANTILE_FILTER, useQuantileFilter);
    setParameter(REMOVE_HOTSPOTS, removeHotspots);
  }

}

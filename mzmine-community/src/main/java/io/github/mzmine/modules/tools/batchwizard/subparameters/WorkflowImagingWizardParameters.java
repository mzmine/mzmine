package io.github.mzmine.modules.tools.batchwizard.subparameters;

import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.workflows.WorkflowImaging;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;

public final class WorkflowImagingWizardParameters extends WorkflowWizardParameters {

  public static final BooleanParameter CORRELATE_IMAGES = new BooleanParameter("Co-localize images",
      """
          Co-localize images to group ions and isotopes of the same molecule, as well as co-located molecules. 
          """, true);


  public WorkflowImagingWizardParameters() {
    super(new WorkflowImaging(),
        // actual parameters
        CORRELATE_IMAGES);
  }

  public WorkflowImagingWizardParameters(final boolean correlateImagesActive) {
    this();
    setParameter(CORRELATE_IMAGES, correlateImagesActive);

  }

}

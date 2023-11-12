package io.github.mzmine.modules.tools.batchwizard.subparameters;

import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.WorkflowWizardParameterFactory;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;

public final class WorkflowImagingWizardParameters extends WorkflowWizardParameters {

  public static final BooleanParameter correlateImages = new BooleanParameter("Correlate images",
      """
          Correlate images to group ions and isotopes of the same molecule, as well as co-located molecules. 
          """, true);

  public static final BooleanParameter applyIIMNetworking = new BooleanParameter(
      "Apply ion identity molecular networking (IIMN)", """
      Applies ion identity molecular networking. Adds graphml export and enables use of visualizer. 
      """, true);


  public WorkflowImagingWizardParameters() {
    super(WorkflowWizardParameterFactory.IMAGING,
        // actual parameters
        correlateImages, applyIIMNetworking);
  }

  public WorkflowImagingWizardParameters(final boolean correlateImagesActive,
      final boolean applyIIMNetworkingActive) {
    this();
    setParameter(applyIIMNetworking, applyIIMNetworkingActive);
    setParameter(correlateImages, correlateImagesActive);
  }

}

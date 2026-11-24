package io.github.mzmine.modules.tools.batchwizard.subparameters;

import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;

/**
 * "Annotate lipids" in the wizard with its options as sub parameters.
 * <p>
 * Earlier versions stored this parameter as a plain checkbox with the value as text content. Such
 * presets and generated wizard files are still loaded with their selection and default options.
 */
public class LipidAnnotationWizardParameter extends
    OptionalModuleParameter<LipidAnnotationWizardSubParameters> {

  private static final String SELECTED_ATTRIBUTE = "selected";

  /**
   * @param name               parameter name
   * @param description        parameter description
   * @param embeddedParameters the lipid annotation options
   * @param defaultValue       true if lipid annotation is selected by default
   */
  public LipidAnnotationWizardParameter(final @NotNull String name,
      final @NotNull String description,
      final @NotNull LipidAnnotationWizardSubParameters embeddedParameters,
      final boolean defaultValue) {
    super(name, description, embeddedParameters, defaultValue);
  }

  @Override
  public void loadValueFromXML(final Element xmlElement) {
    if (xmlElement.hasAttribute(SELECTED_ATTRIBUTE)) {
      super.loadValueFromXML(xmlElement);
      return;
    }
    // assumption: no selected attribute means the legacy checkbox format with true/false as text
    final String legacyValue = xmlElement.getTextContent().trim();
    if (!legacyValue.isEmpty()) {
      setValue(Boolean.valueOf(legacyValue));
    }
  }

  @Override
  public @NotNull LipidAnnotationWizardParameter cloneParameter() {
    return new LipidAnnotationWizardParameter(getName(), getDescription(),
        (LipidAnnotationWizardSubParameters) getEmbeddedParameters().cloneParameterSet(),
        getValue());
  }
}

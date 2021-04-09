package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.customlipidclass;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.LipidChainType;
import io.github.mzmine.parameters.UserParameter;

public class CustomLipidChainChoiceParameter
		implements UserParameter<LipidChainType[], CustomLipidChainChoiceComponent> {

	private final String name;
	private final String description;
	private LipidChainType[] choices;
	private LipidChainType[] values;

	/**
   * Create the parameter.
   *
   * @param name name of the parameter.
   * @param description description of the parameter.
   */
	public CustomLipidChainChoiceParameter(String name,
			String description, LipidChainType[] choices) {
    this.name = name;
    this.description = description;
    this.choices = choices;
    this.values = choices;
  }

	@Override
	public CustomLipidChainChoiceComponent createEditingComponent() {
		return new CustomLipidChainChoiceComponent(choices);
	}

	@Override
	public void setValueFromComponent(final CustomLipidChainChoiceComponent component) {
		values = component.getValue().toArray(new LipidChainType[component.getValue().size()]);
		choices = component.getChoices().toArray(new LipidChainType[component.getChoices().size()]);
	}

	@Override
	public void setValueToComponent(CustomLipidChainChoiceComponent component,
			LipidChainType[] newValue) {
		component.setValue(Arrays.asList(newValue));
	}

	@Override
	public CustomLipidChainChoiceParameter cloneParameter() {

		final CustomLipidChainChoiceParameter copy = new CustomLipidChainChoiceParameter(
				name, description, choices);
		copy.setValue(values);
		return copy;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public LipidChainType[] getValue() {
		return values;
	}

	public LipidChainType[] getChoices() {
		return choices;
	}

	@Override
	public void setValue(LipidChainType[] newValue) {
		this.values = newValue;
	}

	@Override
	public boolean checkValue(Collection<String> errorMessages) {
		return true;
	}

	@Override
	public void loadValueFromXML(Element xmlElement) {
		NodeList items = xmlElement.getElementsByTagName("item");
		ArrayList<LipidChainType> newValues = new ArrayList<>();
		for (int i = 0; i < items.getLength(); i++) {
			String itemString = items.item(i).getTextContent();
			for (int j = 0; j < choices.length; j++) {
				if (choices[j].toString().equals(itemString)) {
					newValues.add(choices[j]);
				}
			}
		}
		this.values = newValues.toArray(new LipidChainType[0]);
	}

	@Override
	public void saveValueToXML(Element xmlElement) {
		if (values == null)
			return;
		Document parentDocument = xmlElement.getOwnerDocument();
		for (LipidChainType item : values) {
			Element newElement = parentDocument.createElement("item");
			newElement.setTextContent(item.toString());
			xmlElement.appendChild(newElement);
		}
	}
}

package io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.params;

import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.util.collections.CollectionUtils;
import javafx.scene.layout.Priority;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Parameters for the mobile phases of the samples.
 * The main logic was derived form similar classes in other modules, and the checkboxes were done with the help of ChatGPT.
 * @param <ValueType> The type of the value associated.
 */
public class MobilePhaseParameter<ValueType> implements UserParameter<ValueType[], MobilePhaseComponent>  {

    /**
     * Parameter name.
     */
    private final String name;
    /**
     * Parameter description.
     */
    private final String description;
    /**
     * Array of possible choices, in this case mobile phases.
     */
    private ValueType[] choices;
    /**
     * Array of values, in this case mobile phases.
     */
    private ValueType[] values;
    /**
     * Minimum number of needed mobile phases.
     */
    private final int minNumber;

    /**
     * Creates a new MobilePhaseParameter with the specified info.
     * This constructor is called when creating the task.
     * @param name The parameter name.
     * @param description The parameter description.
     * @param choices The parameter options.
     */
    public MobilePhaseParameter(String name, String description, ValueType[] choices) {
        this(name, description, choices, null, 0);
    }

    /**
     * Creates a new MobilePhaseParameter with the specified info.
     * This constructor is used in one of the methods of this class.
     * @param name The parameter name.
     * @param description The parameter description.
     * @param choices The parameter options.
     * @param values The parameter values.
     */
    public MobilePhaseParameter(String name, String description, ValueType[] choices,
                               ValueType[] values) {
        this(name, description, choices, values, 0);
    }

    /**
     * Creates a new MobilePhaseParameter with the specified info.
     * This constructor is a general one called in the other constructors, and its the one that assigns values to the attributes.
     * @param name
     * @param description
     * @param choices
     * @param values
     * @param minNumber
     */
    public MobilePhaseParameter(String name, String description, ValueType[] choices, ValueType[] values, int minNumber) {

        assert choices != null;

        this.name = name;
        this.description = description;
        this.choices = choices;
        this.values = values;
        this.minNumber = minNumber;
    }

    /**
     * Gets the name of the parameter.
     * @return The parameter name.
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Sets the choices of the parameters to the input value.
     * @param choices Array of possible options.
     */
    public void setChoices(ValueType[] choices) {
        this.choices = choices;
    }

    /**
     * Gets the choices in the parameter.
     * @return The parameter choices.
     */
    public ValueType[] getChoices() {
        return choices;
    }

    /**
     * Gets the description.
     * @return The parameter description.
     */
    @Override
    public String getDescription() {
        return description;
    }

    /**
     * Creates the MobilePhaseComponent that will be used in the setup dialog.
     * @return A MobilePhaseComponent.
     */
    @Override
    public MobilePhaseComponent createEditingComponent() {
        MobilePhaseComponent component = new MobilePhaseComponent(choices);

        //Adjust size of the box to number of mobile phases
        int rows = Math.min(choices.length, 6);  // cap at 6 rows for readability
        component.setPrefHeight(rows * 28);      // ~28px per row, tweak if needed

        return component;
    }


    /**
     * Sets the values to the ones chosen in the setup dialog.
     * @param component MobilePhaseComponent where the user chooses the options.
     */
    @Override
    public void setValueFromComponent(MobilePhaseComponent component) {
        Object[] componentValue = component.getValue();
        Class<ValueType> arrayType = (Class<ValueType>) this.choices.getClass().getComponentType();
        this.values = CollectionUtils.changeArrayType(componentValue, arrayType);
    }

    /**
     * Sets the values in the MobilePhaseComponent.
     * @param mobilePhaseComponent MobilePhaseComponent where the user chooses.
     * @param newValue The values to be assigned to the component.
     */
    @Override
    public void setValueToComponent(MobilePhaseComponent mobilePhaseComponent, ValueType @Nullable [] newValue) {
        mobilePhaseComponent.setValue(newValue);
    }

    /**
     * Gets the values.
     * @return The parameter values.
     */
    @Override
    public ValueType[] getValue() {
        return values;
    }

    /**
     * Sets the values.
     * @param values The parameter values.
     */
    @Override
    public void setValue(ValueType[] values) {
        this.values = values;
    }

    /**
     * Clones a MobilePhaseParameter object.
     * @return A copy of the MobilePhaseParameter.
     */
    @Override
    public MobilePhaseParameter<ValueType> cloneParameter() {
        MobilePhaseParameter<ValueType> copy = new MobilePhaseParameter<>(name, description,
                choices, values);
        copy.setValue(this.getValue());
        return copy;
    }

    /**
     * Retruns the priority of the component.
     * @return Priority object.
     */
    @Override
    public Priority getComponentVgrowPriority() {
        return UserParameter.super.getComponentVgrowPriority();
    }


    @SuppressWarnings("unchecked")
    @Override
    public void loadValueFromXML(Element xmlElement) {
        NodeList items = xmlElement.getElementsByTagName("item");
        ArrayList<ValueType> newValues = new ArrayList<ValueType>();
        for (int i = 0; i < items.getLength(); i++) {
            String itemString = items.item(i).getTextContent();
            for (int j = 0; j < choices.length; j++) {
                if (choices[j].toString().equals(itemString)) {
                    newValues.add(choices[j]);
                    break;
                }
            }
        }
        Class<ValueType> arrayType = (Class<ValueType>) this.choices.getClass().getComponentType();
        Object[] newArray = newValues.toArray();
        this.values = CollectionUtils.changeArrayType(newArray, arrayType);
    }

    @Override
    public void saveValueToXML(Element xmlElement) {
        if (values == null) {
            return;
        }
        Document parentDocument = xmlElement.getOwnerDocument();
        for (ValueType item : values) {
            Element newElement = parentDocument.createElement("item");
            newElement.setTextContent(item.toString());
            xmlElement.appendChild(newElement);
        }
    }

    @Override
    public boolean checkValue(Collection<String> errorMessages) {
        if (values == null) {
            errorMessages.add(name + " is not set properly");
            return false;
        } else if (values.length < minNumber) {
            errorMessages.add("Select at least one mobile phase");
            return false;
        }
        return true;
    }
}

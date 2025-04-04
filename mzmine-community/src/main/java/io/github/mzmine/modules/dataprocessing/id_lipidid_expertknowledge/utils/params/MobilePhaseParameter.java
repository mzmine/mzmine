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

public class MobilePhaseParameter<ValueType> implements UserParameter<ValueType[], MobilePhaseComponent>  {

    private final String name;
    private final String description;
    private ValueType[] choices;
    private ValueType[] values;
    private final int minNumber;

    public MobilePhaseParameter(String name, String description, ValueType[] choices) {
        this(name, description, choices, null, 0);
    }

    public MobilePhaseParameter(String name, String description, ValueType[] choices,
                               ValueType[] values) {
        this(name, description, choices, values, 0);
    }

    public MobilePhaseParameter(String name, String description, ValueType[] choices, ValueType[] values, int minNumber) {

        assert choices != null;

        this.name = name;
        this.description = description;
        this.choices = choices;
        this.values = values;
        this.minNumber = minNumber;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setChoices(ValueType[] choices) {
        this.choices = choices;
    }

    public ValueType[] getChoices() {
        return choices;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public MobilePhaseComponent createEditingComponent() {
        return new MobilePhaseComponent(choices);
    }

    @Override
    public void setValueFromComponent(MobilePhaseComponent component) {
        Object[] componentValue = component.getValue();
        Class<ValueType> arrayType = (Class<ValueType>) this.choices.getClass().getComponentType();
        this.values = CollectionUtils.changeArrayType(componentValue, arrayType);
    }

    @Override
    public void setValueToComponent(MobilePhaseComponent mobilePhaseComponent, ValueType @Nullable [] newValue) {
        mobilePhaseComponent.setValue(newValue);
    }

    @Override
    public ValueType[] getValue() {
        return values;
    }


    @Override
    public void setValue(ValueType[] values) {
        this.values = values;
    }

    @Override
    public MobilePhaseParameter<ValueType> cloneParameter() {
        MobilePhaseParameter<ValueType> copy = new MobilePhaseParameter<>(name, description,
                choices, values);
        copy.setValue(this.getValue());
        return copy;
    }

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

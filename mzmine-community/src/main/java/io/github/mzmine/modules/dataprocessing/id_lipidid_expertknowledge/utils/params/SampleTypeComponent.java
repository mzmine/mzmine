package io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.params;

import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Class that represents the SampleTypes in the setup dialog for the user to choose.
 * The user has to choose only one option.
 * The main logic was derived form similar classes in other modules, and the radio buttons were done with the help of ChatGPT.
 */
public class SampleTypeComponent extends BorderPane {

    /**
     * Group of buttons in the window.
     * There is one for each option.
     */
    private final ToggleGroup toggleGroup = new ToggleGroup();
    /**
     * Relationship between SampleTypes and a button.
     */
    private final Map<SampleTypes, RadioButton> classToItemMap = new HashMap<>();

    /**
     * Creates a new SampleTypeComponent with the specified info.
     * Specifies how it appears and creates a "Clear" button to clear the selected option.
     * @param choices The options the user can choose from.
     */
    public SampleTypeComponent(final Object[] choices) {
        VBox mobilePhasesBox = new VBox();
        mobilePhasesBox.setSpacing(10);

        // Add MobilePhases enum items
        for (SampleTypes phase : SampleTypes.values()) {
            RadioButton phaseRadioButton = new RadioButton(phase.getName());
            phaseRadioButton.setToggleGroup(toggleGroup);
            classToItemMap.put(phase, phaseRadioButton);
            mobilePhasesBox.getChildren().add(phaseRadioButton);
        }

        setCenter(mobilePhasesBox);

        // Add buttons
        Button selectNoneButton = new Button("Clear");
        FlowPane buttonsPanel = new FlowPane(Orientation.HORIZONTAL);
        buttonsPanel.getChildren().add(selectNoneButton);
        setTop(buttonsPanel);

        // Add tooltip and action for the clear button
        selectNoneButton.setTooltip(new Tooltip("Clear selection"));
        selectNoneButton.setOnAction(e -> toggleGroup.selectToggle(null));
    }

    /**
     * Get the users selections.
     * @return The selected choices.
     */
    public Object[] getValue() {
        RadioButton selectedRadioButton = (RadioButton) toggleGroup.getSelectedToggle();
        if (selectedRadioButton != null) {
            String label = selectedRadioButton.getText();
            for (SampleTypes type : SampleTypes.values()) {
                if (type.getName().equalsIgnoreCase(label.trim())) {
                    return new SampleTypes[]{type};
                }
            }
            throw new IllegalArgumentException("Unknown sample type label: " + label);
        }
        return null;
    }

    /**
     * Set the selections.
     * @param value The selected objects.
     */
    public void setValue(@Nullable final Object[] value) {
        toggleGroup.selectToggle(null); // Clear current selection

        if (value != null) {
            RadioButton radioButton = classToItemMap.get(value);
            if (radioButton != null) {
                radioButton.setSelected(true);
            }
        }
    }


}

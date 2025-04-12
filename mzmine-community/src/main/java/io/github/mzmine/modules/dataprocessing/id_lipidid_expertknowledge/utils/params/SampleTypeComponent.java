package io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.params;

import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Component for the sample types
 */
public class SampleTypeComponent extends BorderPane {

    private final ToggleGroup toggleGroup = new ToggleGroup();
    private final Map<SampleTypes, RadioButton> classToItemMap = new HashMap<>();

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

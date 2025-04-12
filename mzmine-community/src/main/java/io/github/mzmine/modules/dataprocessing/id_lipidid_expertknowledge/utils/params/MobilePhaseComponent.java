package io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.params;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidClasses;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.controlsfx.control.CheckTreeView;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Component for the choosing of the movile phases of the samples
 */
public class MobilePhaseComponent extends BorderPane {

    private final CheckBoxTreeItem<Object> rootItem = new CheckBoxTreeItem<>("Root");
    private final CheckTreeView<Object> mobilePhases = new CheckTreeView<>(rootItem);
    private final Map<MobilePhases, CheckBoxTreeItem<Object>> classToItemMap = new HashMap<>();

    public MobilePhaseComponent(final Object[] choices) {
        mobilePhases.setShowRoot(false);
        mobilePhases.setMinWidth(500);
        mobilePhases.setMinHeight(200);

        // Add MobilePhases enum items
        for (MobilePhases phase : MobilePhases.values()) {
            CheckBoxTreeItem<Object> phaseItem = new CheckBoxTreeItem<>(phase);
            classToItemMap.put(phase, phaseItem);
            rootItem.getChildren().add(phaseItem);
        }

        setCenter(mobilePhases);

        // Add buttons
        Button selectAllButton = new Button("All");
        Button selectNoneButton = new Button("Clear");
        FlowPane buttonsPanel = new FlowPane(Orientation.HORIZONTAL);
        buttonsPanel.getChildren().addAll(selectAllButton, selectNoneButton);
        setTop(buttonsPanel);

        // Add tooltips and actions for buttons
        selectAllButton.setTooltip(new Tooltip("Select all choices"));
        selectAllButton.setOnAction(e -> {
            for (CheckBoxTreeItem<Object> item : classToItemMap.values()) {
                item.setSelected(true);
            }
        });

        selectNoneButton.setTooltip(new Tooltip("Clear all selections"));
        selectNoneButton.setOnAction(e -> {
            for (CheckBoxTreeItem<Object> item : classToItemMap.values()) {
                item.setSelected(false);
            }
        });
    }

    /**
     * Get the users selections.
     *
     * @return the selected choices.
     */
    public Object[] getValue() {

        var checkedItems = mobilePhases.getCheckModel().getCheckedItems();
        return checkedItems.stream()
                .filter(item -> item != null && item.getValue() instanceof MobilePhases)
                .map(TreeItem::getValue).toList().toArray();

    }

    /**
     * Set the selections.
     *
     * @param values the selected objects.
     */
    public void setValue(@Nullable final Object[] values) {
        mobilePhases.getCheckModel().clearChecks();
        if (values == null) {
            return;
        }

        for (Object mobilePhase : values) {
            CheckBoxTreeItem<Object> item = classToItemMap.get(mobilePhase);
            mobilePhases.getCheckModel().check(item);
        }
    }
}
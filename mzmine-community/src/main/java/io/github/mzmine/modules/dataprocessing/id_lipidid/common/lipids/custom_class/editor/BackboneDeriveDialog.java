package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.editor;

import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.factories.FxTextFields;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.simulation.LipidBackboneDeriver;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.simulation.LipidBackboneDeriver.BackboneResult;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain.LipidChainType;
import java.util.Optional;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.stage.Window;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Dialog that derives the backbone formula from one known lipid of the class.
 */
final class BackboneDeriveDialog {

  private BackboneDeriveDialog() {
  }

  /**
   * @param owner      window of the editor
   * @param chainTypes chains of the class in the editor
   * @return the backbone formula if the user confirmed a valid result
   */
  static @NotNull Optional<String> show(@Nullable Window owner,
      @NotNull LipidChainType[] chainTypes) {
    final StringProperty formula = new SimpleStringProperty("");
    final StringProperty carbons = new SimpleStringProperty("");
    final StringProperty dbes = new SimpleStringProperty("");

    final ObjectBinding<BackboneResult> result = Bindings.createObjectBinding(() -> {
      if (formula.get().isBlank() || carbons.get().isBlank()) {
        return new BackboneResult(null, "Enter the formula and the chain composition.");
      }
      try {
        final int c = Integer.parseInt(carbons.get().strip());
        final int d = dbes.get().isBlank() ? 0 : Integer.parseInt(dbes.get().strip());
        return LipidBackboneDeriver.derive(chainTypes, formula.get(), c, d);
      } catch (NumberFormatException e) {
        return new BackboneResult(null, "Carbons and double bonds must be whole numbers.");
      }
    }, formula, carbons, dbes);

    final Label resultLabel = FxLabels.newBoldLabel(Bindings.createStringBinding(
        () -> result.get().isOk() ? "Backbone formula: " + result.get().formula()
            : result.get().error(), result));
    resultLabel.setWrapText(true);

    final Label intro = FxLabels.newLabel("""
        Enter the neutral formula of one lipid of this class that you know, \
        and the total carbons and double bonds of all its chains. \
        Example: PE 34:1 is C39H76NO8P with 34 carbons and 1 double bond.""");
    intro.setWrapText(true);
    final Node grid = FxLayout.newGrid2Col(FxLabels.newLabel("Lipid formula"),
        FxTextFields.newTextField(formula, "Neutral molecular formula, e.g., C39H76NO8P"),
        FxLabels.newLabel("Chain carbons"),
        FxTextFields.newTextField(carbons, "Total carbons in all chains, e.g., 34"),
        FxLabels.newLabel("Chain double bonds"),
        FxTextFields.newTextField(dbes, "Total double bonds in all chains, e.g., 1"));

    final Dialog<String> dialog = new Dialog<>();
    dialog.setTitle("Derive backbone formula");
    dialog.initOwner(owner);
    dialog.getDialogPane().setContent(
        FxLayout.newVBox(Pos.TOP_LEFT, Insets.EMPTY, true, intro, grid, resultLabel));
    dialog.getDialogPane().setPrefWidth(480);
    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    dialog.getDialogPane().lookupButton(ButtonType.OK).disableProperty()
        .bind(Bindings.createBooleanBinding(() -> !result.get().isOk(), result));
    dialog.setResultConverter(button -> button == ButtonType.OK ? result.get().formula() : null);
    return dialog.showAndWait();
  }
}

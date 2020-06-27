package io.github.mzmine.modules.visualization.chromatogramandspectra;

import io.github.mzmine.modules.visualization.chromatogram.TICPlotType;
import io.github.mzmine.parameters.parametertypes.ranges.MZRangeComponent;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

public class ChromatogramPlotControlPane extends VBox {

  protected final ChoiceBox<TICPlotType> cbPlotType;

  protected final MZRangeComponent mzRange;

  protected final CheckBox cbXIC;

  public ChromatogramPlotControlPane() {
    super(5);
    setAlignment(Pos.CENTER);
    cbPlotType = new ChoiceBox<>();
    cbPlotType.setItems(FXCollections.observableArrayList(TICPlotType.values()));
    cbPlotType.setValue(TICPlotType.BASEPEAK);
    cbXIC = new CheckBox("XIC: ");
    mzRange = new MZRangeComponent();
    mzRange.disableProperty().bind(cbXIC.selectedProperty().not());
    FlowPane xicWrap = new FlowPane(5, 0);
    xicWrap.getChildren().addAll(cbXIC, mzRange);
    getChildren().addAll(cbPlotType, xicWrap);
  }

  public ChoiceBox<TICPlotType> getCbPlotType() {
    return cbPlotType;
  }

  public MZRangeComponent getMzRange() {
    return mzRange;
  }

  public CheckBox getCbXIC() {
    return cbXIC;
  }
}

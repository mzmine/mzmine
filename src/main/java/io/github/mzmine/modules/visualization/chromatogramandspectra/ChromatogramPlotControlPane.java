package io.github.mzmine.modules.visualization.chromatogramandspectra;

import com.google.common.collect.Range;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.chromatogram.TICPlotType;
import io.github.mzmine.parameters.parametertypes.ranges.MZRangeComponent;
import java.text.NumberFormat;
import java.text.ParseException;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class ChromatogramPlotControlPane extends VBox {

  protected final ChoiceBox<TICPlotType> cbPlotType;
  protected final MZRangeComponent mzRangeNode;
  protected final CheckBox cbXIC;
  protected final Button btnUpdateXIC;

  protected final ObjectProperty<Range<Double>> mzRange;
  protected NumberFormat mzFormat;
  protected Number min;
  protected Number max;


  public ChromatogramPlotControlPane() {
    super(5);

    setPadding(new Insets(5));

    getStyleClass().add("region-match-chart-bg");

//    setAlignment(Pos.CENTER);
    cbPlotType = new ChoiceBox<>();
    cbXIC = new CheckBox("Show XIC");
    btnUpdateXIC = new Button("Update chromatogram(s)");
    btnUpdateXIC.setTooltip(new Tooltip("Applies the current m/z range to the TIC/XIC plot."));
    mzRangeNode = new MZRangeComponent();

    cbPlotType.setItems(FXCollections.observableArrayList(TICPlotType.values()));

    // disable mz range and button if xic is not selected
//    btnUpdateXIC.disableProperty().bind(cbXIC.selectedProperty().not());
    // also remove the mz range node if xic is not selected
    cbXIC.selectedProperty().addListener((obs, old, val) -> {
      if (val && !getChildren().contains(mzRangeNode)) {
        getChildren().add(mzRangeNode);
      } else {
        getChildren().remove(mzRangeNode);
      }
    });

    HBox controlsWrap = new HBox(5, cbPlotType, cbXIC, btnUpdateXIC);
    controlsWrap.setAlignment(Pos.CENTER);
    mzRangeNode.setAlignment(Pos.CENTER);
    getChildren().addAll(controlsWrap);

    // set here, so all the listeners trigger and disable the other components.
    cbPlotType.setValue(TICPlotType.BASEPEAK);

    addListenersToMzRangeNode();
    mzRange = new SimpleObjectProperty<>();
    mzFormat = MZmineCore.getConfiguration().getMZFormat();
    min = null;
    max = null;
  }

  public ChoiceBox<TICPlotType> getCbPlotType() {
    return cbPlotType;
  }

  public CheckBox getCbXIC() {
    return cbXIC;
  }

  public Range<Double> getMzRange() {
    return mzRange.get();
  }

  public ObjectProperty<Range<Double>> mzRangeProperty() {
    return mzRange;
  }

  public void setMzRange(Range<Double> mzRange) {
    this.mzRange.set(mzRange);
    if (mzRange != null) {
      mzRangeNode.getMinTxtField().setText(mzFormat.format(mzRange.lowerEndpoint()));
      mzRangeNode.getMaxTxtField().setText(mzFormat.format(mzRange.upperEndpoint()));
    } else {
      mzRangeNode.getMinTxtField().setText("");
      mzRangeNode.getMaxTxtField().setText("");
    }
  }

  public Button getBtnUpdateXIC() {
    return btnUpdateXIC;
  }

  private void addListenersToMzRangeNode() {
    Range<Double> range;
    mzRangeNode.getMinTxtField().textProperty().addListener(((observable, oldValue, newValue) -> {
      if (newValue == null) {
        return;
      }
      try {
        min = mzFormat.parse(newValue.trim());
      } catch (ParseException e) {
//        e.printStackTrace();
        min = null;
      }
      if (min != null && max != null && min.doubleValue() < max.doubleValue()
          && min.doubleValue() >= 0.0) {
        mzRange.set(Range.closed(min.doubleValue(), max.doubleValue()));
      } else {
        mzRange.set(null);
      }
    }));
    mzRangeNode.getMaxTxtField().textProperty().addListener(((observable, oldValue, newValue) -> {
      if (newValue == null) {
        return;
      }
      try {
        max = mzFormat.parse(newValue.trim());
      } catch (ParseException e) {
//        e.printStackTrace();
        max = null;
      }
      if (min != null && max != null && min.doubleValue() < max.doubleValue()
          && min.doubleValue() >= 0.0) {
        mzRange.set(Range.closed(min.doubleValue(), max.doubleValue()));
      } else {
        mzRange.set(null);
      }
    }));
  }
}

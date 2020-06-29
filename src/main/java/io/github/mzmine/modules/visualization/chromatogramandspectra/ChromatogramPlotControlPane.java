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
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
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

    getStyleClass().add("white-region");

//    setAlignment(Pos.CENTER);
    cbPlotType = new ChoiceBox<>();
    cbXIC = new CheckBox("Show XIC");
    btnUpdateXIC = new Button("Update chromatogram(s)");
    btnUpdateXIC.setTooltip(new Tooltip("Applies the current m/z range to the TIC/XIC plot."));

    cbPlotType.setItems(FXCollections.observableArrayList(TICPlotType.values()));
    cbPlotType.valueProperty().addListener(((observable, oldValue, newValue) -> {
      if (newValue == TICPlotType.TIC) {
        cbXIC.disableProperty().set(false);
      } else {
        cbXIC.disableProperty().set(true);
      }
    }));

    mzRangeNode = new MZRangeComponent();
    // disable mz range and button if xic is not selected
    btnUpdateXIC.disableProperty().bind(cbXIC.disableProperty());
    cbXIC.disableProperty().addListener((obs, old, val) ->
        mzRangeNode.setDisable(!(!val && cbXIC.isSelected())));
    // also disable the mz range node if xic is not selected
    cbXIC.selectedProperty().addListener((obs, old, val) -> mzRangeNode.setDisable(!val));

    // hide components if they're not needed
    btnUpdateXIC.visibleProperty().bind(btnUpdateXIC.disableProperty().not());
    mzRangeNode.visibleProperty().bind(mzRangeNode.disableProperty().not());
    cbXIC.visibleProperty().bind(cbXIC.disableProperty().not());

    HBox controlsWrap = new HBox(5, cbPlotType, cbXIC, btnUpdateXIC);
//    controlsWrap.setAlignment(Pos.CENTER);
//    mzRangeNode.setAlignment(Pos.CENTER);
    getChildren().addAll(controlsWrap, mzRangeNode);

    // set here, so all the listeners trigger and disable the other components.
    cbPlotType.setValue(TICPlotType.BASEPEAK);

    addListenersToMzRange();
    mzRange = new SimpleObjectProperty<>();
    mzFormat = MZmineCore.getConfiguration().getMZFormat();
    min = null;
    max = null;
    mzRangeProperty().addListener(((observable, oldValue, newValue) -> {
      if (newValue != null) {
        System.out.println(newValue.toString());
      }
    }));
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
  }

  public Button getBtnUpdateXIC() {
    return btnUpdateXIC;
  }

  private void addListenersToMzRange() {
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

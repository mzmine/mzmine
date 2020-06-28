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
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

public class ChromatogramPlotControlPane extends VBox {

  protected final ChoiceBox<TICPlotType> cbPlotType;
  protected final MZRangeComponent mzRangeNode;
  protected final CheckBox cbXIC;

  protected final ObjectProperty<Range<Double>> mzRange;
  protected NumberFormat mzFormat;
  protected Number min;
  protected Number max;


  public ChromatogramPlotControlPane() {
    super(5);
    setAlignment(Pos.CENTER);
    cbPlotType = new ChoiceBox<>();
    cbXIC = new CheckBox("XIC: ");

    cbPlotType.setItems(FXCollections.observableArrayList(TICPlotType.values()));
    cbPlotType.valueProperty().addListener(((observable, oldValue, newValue) -> {
      if (newValue == TICPlotType.TIC) {
        cbXIC.disableProperty().set(false);
      } else {
        cbXIC.disableProperty().set(true);
      }
    }));
    cbPlotType.setValue(TICPlotType.BASEPEAK);

    mzRangeNode = new MZRangeComponent();
    mzRangeNode.disableProperty().bind(cbXIC.disableProperty());
    mzRangeNode.disableProperty().bind(cbXIC.selectedProperty().not());

    FlowPane xicWrap = new FlowPane(5, 0);
    xicWrap.getChildren().addAll(cbXIC, mzRangeNode);
    getChildren().addAll(cbPlotType, xicWrap);

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

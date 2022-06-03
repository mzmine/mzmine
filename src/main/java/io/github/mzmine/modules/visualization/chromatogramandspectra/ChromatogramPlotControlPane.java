/*
 * Copyright 2006-2022 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.visualization.chromatogramandspectra;

import com.google.common.collect.Range;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.ranges.MZRangeComponent;
import io.github.mzmine.util.ExitCode;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.function.Consumer;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class ChromatogramPlotControlPane extends VBox {

  protected final MZRangeComponent mzRangeNode;
  protected final CheckBox cbXIC;
  protected final Button btnUpdateXIC;

  protected final ObjectProperty<Range<Double>> mzRange;
  private final Button btnParam;
  protected NumberFormat mzFormat;
  protected Number min;
  protected Number max;

  protected Consumer<ParameterSet> parameterListener;


  public ChromatogramPlotControlPane() {
    super(5);

    setPadding(new Insets(5));

    getStyleClass().add("region-match-chart-bg");

//    setAlignment(Pos.CENTER);
    cbXIC = new CheckBox("Show XIC");
    btnUpdateXIC = new Button("Update chromatogram(s)");
    btnUpdateXIC.setTooltip(new Tooltip("Applies the current m/z range to the TIC/XIC plot."));
    btnParam = new Button("Setup");
    btnParam.setTooltip(new Tooltip("Setup parameters"));
    btnParam.setOnAction(event -> showChromParameterSetup());
    mzRangeNode = new MZRangeComponent();

    // disable mz range and button if xic is not selected
    btnUpdateXIC.disableProperty().bind(cbXIC.selectedProperty().not());
    mzRangeNode.disableProperty().bind(cbXIC.selectedProperty().not());

    HBox controlsWrap = new HBox(5, cbXIC, btnUpdateXIC, btnParam);
    controlsWrap.setAlignment(Pos.CENTER);
    mzRangeNode.setAlignment(Pos.CENTER);
    getChildren().addAll(controlsWrap, mzRangeNode);

    addListenersToMzRangeNode();
    mzRange = new SimpleObjectProperty<>();
    mzFormat = MZmineCore.getConfiguration().getMZFormat();
    min = null;
    max = null;
  }

  public void setParameterListener(Consumer<ParameterSet> parameterListener) {
    this.parameterListener = parameterListener;
  }

  public void showChromParameterSetup() {
    showChromParameterSetup(MZmineCore.getConfiguration()
        .getModuleParameters(ChromatogramAndSpectraVisualizerModule.class));
  }

  public void showChromParameterSetup(ParameterSet parameterSet) {
    ExitCode code = parameterSet.showSetupDialog(true);
    if (code == ExitCode.OK) {
      if (parameterListener != null) {
        parameterListener.accept(parameterSet);
      }
    }
  }

  public CheckBox getCbXIC() {
    return cbXIC;
  }

  public Range<Double> getMzRange() {
    return mzRange.get();
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

  public ObjectProperty<Range<Double>> mzRangeProperty() {
    return mzRange;
  }

  public Button getBtnUpdateXIC() {
    return btnUpdateXIC;
  }

  private void addListenersToMzRangeNode() {
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

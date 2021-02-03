package io.github.mzmine.gui.mainwindow;

import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.parametertypes.ParameterSetParameter;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javax.annotation.Nullable;

public class FeatureListSummaryController {

  @FXML
  public TextField tfNumRows;
  @FXML
  public TextField tfCreated;
  @FXML
  public ListView<FeatureListAppliedMethod> lvAppliedMethods;
  @FXML
  public TextArea tvParameterValues;
  @FXML
  public Label lbFeatureListName;

  @FXML
  public void initialize() {

    lvAppliedMethods.getSelectionModel().selectedItemProperty()
        .addListener((observable, oldValue, newValue) -> {
          tvParameterValues.clear();

          if (newValue == null) {
            return;
          }

          tvParameterValues.appendText(newValue.getDescription());
          tvParameterValues.appendText("\n");
          for (Parameter<?> parameter : newValue.getParameters().getParameters()) {
            tvParameterValues.appendText(parameterToString(parameter));
            tvParameterValues.appendText("\n");
          }
        });
  }

  public void setFeatureList(@Nullable ModularFeatureList featureList) {
    clear();

    if (featureList == null) {
      return;
    }

    lbFeatureListName.setText(featureList.getName());
    tfNumRows.setText(String.valueOf(featureList.getNumberOfRows()));
    tfCreated.setText(featureList.getDateCreated());
    lvAppliedMethods.setItems(featureList.getAppliedMethods());
  }

  public void clear() {
    lbFeatureListName.setText("None selected");
    tfNumRows.setText("");
    tfCreated.setText("");
    lvAppliedMethods.getItems().clear();
    tvParameterValues.setText("");
  }

  private String parameterToString(Parameter<?> parameter) {
    String name = parameter.getName();
    Object value = parameter.getValue();
    StringBuilder sb = new StringBuilder(name);
    sb.append(":\t");
    sb.append(value.toString());
    if(parameter instanceof ParameterSetParameter) {
      for (Parameter<?> parameter1 : ((ParameterSetParameter) parameter).getValue()
          .getParameters()) {
        sb.append("\n\t");
        sb.append(parameterToString(parameter1));
      }
    }
    return sb.toString();
  }
}

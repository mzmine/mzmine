/*
 * Copyright (c) 2004-2025 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.parameters.dialogs;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.GroupedParameterSetupPane.GroupView;
import io.github.mzmine.parameters.dialogs.GroupedParameterSetupPane.ParameterGroup;
import java.util.List;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class represents the parameter setup dialog to set the values of SimpleParameterSet. Each
 * Parameter is represented by a component. The component can be obtained by calling
 * getComponentForParameter(). Type of component depends on a parameter type:
 * <p>
 * TODO: parameter setup dialog should show the name of the module in the title
 */
public class GroupedParameterSetupDialog extends EmptyParameterSetupDialogBase {

  private final GroupedParameterSetupPane groupedParamPane;

  public GroupedParameterSetupDialog(boolean valueCheckRequired, ParameterSet parameters,
      boolean showSummary, @Nullable List<? extends Parameter<?>> fixedParameters,
      @NotNull List<ParameterGroup> groups, @NotNull GroupView viewType) {
    this(valueCheckRequired, parameters, null, showSummary, fixedParameters, groups, viewType);
  }

  /**
   * Method to display setup dialog with a html-formatted footer message at the bottom.
   *
   * @param message     html-formatted text
   * @param showSummary a summary just under the filter field
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  public GroupedParameterSetupDialog(boolean valueCheckRequired, @NotNull ParameterSet parameters,
      @Nullable Region message, boolean showSummary,
      @Nullable List<? extends Parameter<?>> fixedParameters, @NotNull List<ParameterGroup> groups,
      @NotNull GroupView viewType) {
    super(valueCheckRequired, parameters, message);

    groupedParamPane = new GroupedParameterSetupPane(fixedParameters, groups, getParamPane(),
        viewType, false);
    groupedParamPane.setShowSummary(showSummary);
    centerPane.setCenter(groupedParamPane);

    setMinWidth(750);
    setMinHeight(600);

    centerOnScreen();
  }

  /**
   * Set the filter at the top of the property pane
   *
   * @param filter filters for the parameter titles
   */
  public void setFilterText(@Nullable String filter) {
    groupedParamPane.setSearchFilter(filter);
  }

}

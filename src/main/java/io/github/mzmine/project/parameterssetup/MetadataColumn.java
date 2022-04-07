package io.github.mzmine.project.parameterssetup;

import io.github.mzmine.parameters.UserParameter;

/**
 * Parameter column in a project metadata table.
 *
 * @param <T> Type of the parameter (e.g. String, Date or Double)
 */
public class MetadataColumn<T> {

  private final UserParameter<T, ?> parameter;

  public MetadataColumn(UserParameter<T, ?> parameter) {
    this.parameter = parameter;
  }

  /**
   * Get project parameter which is represented by this cell of the project metadata table.
   *
   * @return project parameter
   */
  public UserParameter<T, ?> getParameter() {
    return parameter;
  }
}

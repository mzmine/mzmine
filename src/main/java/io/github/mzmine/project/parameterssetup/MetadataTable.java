package io.github.mzmine.project.parameterssetup;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.parameters.UserParameter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Holds metadata of a project and represents it as a table (parameters are columns).
 */
public class MetadataTable {

  private Map<MetadataColumn<?>, Map<RawDataFile, ?>> data = new HashMap<>();

  public Map<MetadataColumn<?>, Map<RawDataFile, ?>> getData() {
    return data;
  }

  public void setData(Map<MetadataColumn<?>, Map<RawDataFile, ?>> data) {
    this.data = data;
  }

  /**
   * Add new parameter column to the metadata table.
   *
   * @param parameterColumn new parameter column
   */
  public void addParameterColumn(MetadataColumn<?> parameterColumn) {
    if (!data.containsKey(parameterColumn)) {
      data.put(parameterColumn, new HashMap<>());
    }
  }

  /**
   * Remove parameter column from the metadata table.
   *
   * @param parameterColumn parameter column
   */
  public void removeParameterColumn(MetadataColumn<?> parameterColumn) {
    data.remove(parameterColumn);
  }

  /**
   * Is the specified parameter obtained in the metadata table?
   *
   * @param parameter project parameter
   * @return true if it's contained, false otherwise
   */
  public boolean hasParameter(UserParameter<?, ?> parameter) {
    return getParameterColumnByName(parameter.getName()) != null;
  }

  /**
   * Return parameters columns of the metadata table.
   *
   * @return set with the parameters columns
   */
  public Set<MetadataColumn<?>> getParametersColumns() {
    return data.keySet();
  }

  /**
   * Return parameter column with the corresponding parameter name.
   *
   * @param parameterName name of the parameter
   * @return parameterColumn or null in case if the parameter with the passed name isn't obtained in
   * the metadata table
   */
  public MetadataColumn<?> getParameterColumnByName(String parameterName) {
    for (MetadataColumn<?> parameterColumn : getParametersColumns()) {
      if (parameterColumn.getParameter().getName().equals(parameterName)) {
        return parameterColumn;
      }
    }

    return null;
  }

  /**
   * Return parameter value of the corresponding RawData file.
   *
   * @param parameter   project parameter
   * @param rawDataFile RawData file
   * @param <T>         type of the project parameter
   * @return parameter value
   */
  public <T> T getParameterValue(UserParameter<T, ?> parameter, RawDataFile rawDataFile) {
    if (hasParameter(parameter)) {
      return (T) data.get(getParameterColumnByName(parameter.getName())).get(rawDataFile);
    }

    return null;
  }

  /**
   * Try to set particular value of the parameter of the RawData file. The parameter column will be
   * added in case if it wasn't previously obtained in the table.
   *
   * @param column parameter
   * @param file   RawData file
   * @param value  value to be set
   * @param <T>    type of the parameter
   */
  public <T> void setParameterValue(MetadataColumn<T> column, RawDataFile file, T value) {
    if (!data.containsKey(column)) {
      addParameterColumn(column);
    }
    data.put(column, Map.of(file, value));
  }
}

package io.github.mzmine.datamodel.data.types;


/**
 * Class of data types: Provides formatters
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 * @param <T>
 */
public abstract class DataType<T> {

  protected T value;

  public T getValue() {
    return value;
  }

  public void setValue(T value) {
    this.value = value;
  }

  /**
   * A formatted string representation of the value
   * 
   * @return
   */
  public abstract String getFormattedString();
}

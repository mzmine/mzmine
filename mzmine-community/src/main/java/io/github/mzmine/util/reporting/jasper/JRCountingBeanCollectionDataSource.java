package io.github.mzmine.util.reporting.jasper;

import java.util.Collection;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

/**
 * Helper class that extends {@link JRBeanCollectionDataSource} and tracks the progress of the
 * iterator.
 */
public class JRCountingBeanCollectionDataSource extends JRBeanCollectionDataSource {

  int currentItem = 0;

  public JRCountingBeanCollectionDataSource(Collection<?> beanCollection) {
    super(beanCollection);
  }

  @Override
  public boolean next() {
    currentItem++;
    return super.next();
  }

  public int getCurrentItemIndex() {
    return currentItem;
  }

  public int getSize() {
    return getData().size();
  }

  public double getProgress() {
    return (double) getCurrentItemIndex() / getSize();
  }
}

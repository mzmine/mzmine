package io.github.mzmine.util.reporting.jasper;

import static java.io.IO.println;

import java.util.Collection;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

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

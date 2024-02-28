/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.util.components;

import java.util.Enumeration;
import java.util.Vector;

import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

/**
 * GroupableTableHeader
 * 
 * @version 1.0 10/20/98
 * @author Nobuo Tamemasa
 */

public class GroupableTableHeader extends JTableHeader {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  protected Vector<ColumnGroup> columnGroups = null;

  public GroupableTableHeader() {
    super();
    setUI(new GroupableTableHeaderUI());
    setReorderingAllowed(false);
  }

  public GroupableTableHeader(TableColumnModel model) {
    super(model);
    setUI(new GroupableTableHeaderUI());
    setReorderingAllowed(false);
  }

  public void setReorderingAllowed(boolean b) {
    reorderingAllowed = false;
  }

  public void addColumnGroup(ColumnGroup g) {
    if (columnGroups == null) {
      columnGroups = new Vector<ColumnGroup>();
    }
    columnGroups.addElement(g);
  }

  public void removeColumnGroup(ColumnGroup g) {
    if (columnGroups == null)
      return;
    columnGroups.remove(g);
  }

  public ColumnGroup[] getColumnGroups() {
    if (columnGroups == null)
      return null;
    return columnGroups.toArray(new ColumnGroup[0]);
  }

  public Enumeration<?> getColumnGroups(TableColumn col) {
    if (columnGroups == null)
      return null;
    Enumeration<ColumnGroup> en = columnGroups.elements();
    while (en.hasMoreElements()) {
      ColumnGroup cGroup = (ColumnGroup) en.nextElement();
      Vector<?> v_ret = (Vector<?>) cGroup.getColumnGroups(col, new Vector<ColumnGroup>());
      if (v_ret != null) {
        return v_ret.elements();
      }
    }
    return null;
  }

  public void setColumnMargin() {
    if (columnGroups == null)
      return;
    int columnMargin = getColumnModel().getColumnMargin();
    Enumeration<ColumnGroup> en = columnGroups.elements();
    while (en.hasMoreElements()) {
      ColumnGroup cGroup = (ColumnGroup) en.nextElement();
      cGroup.setColumnMargin(columnMargin);
    }
  }
}

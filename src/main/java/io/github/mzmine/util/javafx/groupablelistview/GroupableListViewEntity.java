/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.util.javafx.groupablelistview;

/**
 * Class designed to be used as an item of {@link GroupableListView}.
 * @param <T> type of the list view item value
 */
public class GroupableListViewEntity<T> {

  // Value

  private T value;
  private String group;

  public GroupableListViewEntity(T value) {
    this.value = value;
  }

  public T getValue() {
    assertVariables(false);
    return value;
  }

  public void setValue(T value) {
    assertVariables(false);
    this.value = value;
  }

  public void setGroup(String group) {
    assertVariables(false);
    this.group = group;
  }

  public String getGroup() {
    assertVariables(false);
    return group;
  }

  public boolean isGrouped() {
    assertVariables(false);
    return getGroup() != null;
  }

  // Group header

  private String groupHeader;
  private boolean isExpanded = true;

  public GroupableListViewEntity(String groupHeader) {
    this.groupHeader = groupHeader;
  }

  public String getGroupHeader() {
    assertVariables(true);
    return groupHeader;
  }

  public void setGroupHeader(String groupHeader) {
    assertVariables(true);
    this.groupHeader = groupHeader;
  }

  public void invertState() {
    assertVariables(true);
    isExpanded = !isExpanded;
  }

  public boolean isExpanded() {
    assertVariables(true);
    return isExpanded;
  }

  public boolean isHidden() {
    assertVariables(true);
    return !isExpanded;
  }

  // Common

  public boolean isValue() {
    assertVariables(false);
    return value != null;
  }

  public boolean isGroupHeader() {
    assertVariables(true);
    return groupHeader != null;
  }

  private void assertVariables(boolean isGroupHeader) {
    if (isGroupHeader) {
      assert groupHeader != null && value == null : "Invalid state of the group header " + toString();
    } else {
      assert groupHeader == null && value != null : "Invalid state of the value " + toString();
    }
  }

  @Override
  public String toString() {
    return isGroupHeader() ? groupHeader : value.toString();
  }

}

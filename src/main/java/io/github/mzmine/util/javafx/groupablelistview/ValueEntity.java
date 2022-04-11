/*
 * Copyright 2006-2021 The MZmine Development Team
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

package io.github.mzmine.util.javafx.groupablelistview;

/**
 * Class designed to be used as a value item of {@link GroupableListView}.
 *
 * @param <T> type of the value
 */
public class ValueEntity<T> implements GroupableListViewEntity {

  private T value;
  private GroupEntity group;

  public ValueEntity(T value) {
    this.value = value;
  }

  public T getValue() {
    return value;
  }

  public void setValue(T value) {
    this.value = value;
  }

  public void setGroup(GroupEntity group) {
    this.group = group;
  }

  public GroupEntity getGroup() {
    return group;
  }

  public boolean isGrouped() {
    return getGroup() != null;
  }

  @Override
  public String toString() {
    return value.toString();
  }

}

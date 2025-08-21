package io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.correlation.RowGroup;

import java.util.ArrayList;
import java.util.List;

public class VirtualRowGroup implements RowGroup {

    private final List<FeatureListRow> rows;
    private int groupId;      // ID del RowGroup original
    private final int subgroupId;   // Índice del subgrupo (0, 1, 2, ...)

    public VirtualRowGroup(List<FeatureListRow> rows, int groupId, int subgroupId) {
        this.rows = new ArrayList<>(rows);
        this.groupId = groupId;
        this.subgroupId = subgroupId;
        for (FeatureListRow row : this.rows) {
            row.setGroup(this);
            //System.out.println("Assigning to row " + row.getID() + " → groupID=" + groupId + ", subgroupID=" + subgroupId);
        }
    }

    @Override
    public List<FeatureListRow> getRows() {
        return rows;
    }

    @Override
    public boolean add(FeatureListRow row) {
        if (!rows.contains(row)) {
            rows.add(row);
            row.setGroup(this);
            return true;
        }
        return false;
    }

    @Override
    public int getGroupID() {
        return groupId;
    }

    @Override
    public void setGroupID(int groupID) {
        this.groupId = groupID;
    }

    public int getSubgroupId() {
        return subgroupId;
    }

    @Override
    public boolean isCorrelated(int i, int j) { return i != -1 && j != -1 && i < size() && j < size();
    }

    @Override
    public boolean isCorrelated(FeatureListRow a, FeatureListRow b) {
        int ia = indexOf(a);
        int ib = indexOf(b);
        if (ia == -1 || ib == -1) {
            return false;
        }
        return isCorrelated(ia, ib);
    }

    @Override
    public int indexOf(FeatureListRow row) {
        return rows.indexOf(row);
    }

    public int getSubgroupID() {
        return subgroupId;
    }

    @Override
    public String toString() {
        return "" +groupId;
    }
}

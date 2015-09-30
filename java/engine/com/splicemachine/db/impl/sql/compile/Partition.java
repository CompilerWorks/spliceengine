package com.splicemachine.db.impl.sql.compile;

import com.splicemachine.db.iapi.error.StandardException;

/**
 * Re purposing GroupByList as Partition.
 *
 * @author Jeff Cunningham
 *         Date: 6/9/14
 */
public class Partition extends GroupByList {

    // not overriding, just re-labeling

    public Partition(GroupByList groupByList) {
        super();
        setContextManager(groupByList.getContextManager());
        setNodeType(groupByList.getNodeType());
        if (groupByList.isRollup()) {
            setRollup();
        }
        for (int i=0; i<groupByList.size(); i++) {
            addGroupByColumn(groupByList.getGroupByColumn(i));
        }
    }

    public boolean isEquivalent(Partition other) throws StandardException {
        if (this == other) return true;
        if (other == null) return false;
        if (this.isRollup() != other.isRollup()) return false;
        if (this.size() != other.size()) return false;

        for (int i=0; i<size(); i++) {
            if (! this.getGroupByColumn(i).getColumnExpression().isEquivalent(other.getGroupByColumn(i).getColumnExpression()))
                return false;
        }
        return true;
    }

    @Override
    public boolean isRollup() {
        // Window partitions differ from GroupBy in that we don't have rollups
        return false;
    }

    @Override
    public int hashCode() {
        int result = 31;
        for (int i=0; i<size(); i++) {
            result = 31 * result +
                (this.getGroupByColumn(i).getColumnExpression().getColumnName() == null ? 0 :
                    this.getGroupByColumn(i).getColumnExpression().getColumnName().hashCode());
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        try {
            return isEquivalent((Partition) o);
        } catch (StandardException e) {
            // ignore
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("\n");
        for (int i=0; i<size(); ++i) {
            GroupByColumn col = getGroupByColumn(i);
            buf.append("column_name: ").append(col.getColumnName()).append("\n");
            // Lang col indexes are 1-based, storage col indexes are zero-based
            buf.append("columnid: ").append(col.getColumnPosition()-1).append("\n");
        }
//        if (buf.length() > 0) { buf.setLength(buf.length()-1); }
        return buf.toString();
    }

    @Override
    public String toHTMLString() {
        StringBuilder buf = new StringBuilder();
        for (int i=0; i<size(); ++i) {
            GroupByColumn col = getGroupByColumn(i);
            buf.append(col.getColumnName()).append('(');
            buf.append(col.getColumnPosition()-1).append("),");
        }
        if (buf.length() > 0) { buf.setLength(buf.length()-1); }
        return buf.toString();
    }
}

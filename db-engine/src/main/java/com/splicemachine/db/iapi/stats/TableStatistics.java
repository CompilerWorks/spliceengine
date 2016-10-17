/*
 * Apache Derby is a subproject of the Apache DB project, and is licensed under
 * the Apache License, Version 2.0 (the "License"); you may not use these files
 * except in compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * Splice Machine, Inc. has modified this file.
 *
 * All Splice Machine modifications are Copyright 2012 - 2016 Splice Machine, Inc.,
 * and are licensed to you under the License; you may not use this file except in
 * compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 */

package com.splicemachine.db.iapi.stats;

import java.util.BitSet;
import java.util.Comparator;
import java.util.List;

/**
 * Representation of Table-level statistics.
 *
 * Table statistics are statistics about the overall table itself, accumulated over all partitions.
 *
 * @author Scott Fines
 *         Date: 2/23/15
 */
public interface TableStatistics {
    /**
     *
     * The unique identifier for the table
     *
     * @return
     */
    public String tableId();

    /**
     * @return the total number of entries in the table across all partitions
     */
    long rowCount();

    /**
     * @return the total size of the table across all partitions
     */
    long totalSize();

    /**
     * @return the average size of a single partition across all partitions.
     */
    long avgPartitionSize();

    /**
     * @return the average width of a single row (in bytes) across all partitions. This includes
     * the row key and cell contents.
     */
    int avgRowWidth();

    /**
     * @return Detailed statistics for each partition.
     */
    List<? extends PartitionStatistics> getPartitionStatistics();

    PartitionStatistics getEffectivePartitionStatistics();

    <T extends Comparator<T>> T minValue(int positionNumber);

    <T extends Comparator<T>> T maxValue(int positionNumber);

    long nullCount(int positionNumber);

    long notNullCount(int positionNumber);

    long cardinality(int positionNumber);

    int numPartitions();

    /**
     * @param element the element to match
     * @return the number of entries which are <em>equal</em> to the specified element.
     */
    <T extends Comparator<T>> double selectivity(T element,int positionNumber);

    /**
     * @param start the start of the range to estimate. If {@code null}, then scan everything before {@code stop}.
     *              If {@code stop} is also {@code null}, then this will return an estimate to the number of entries
     *              in the entire data set.
     * @param stop the end of the range to estimate. If {@code null}, then scan everything after {@code start}.
     *             If {@code start} is also {@code null}, then this will return an estimate of the number of entries
     *             in the entire data set.
     * @param includeStart if {@code true}, then include entries which are equal to {@code start}
     * @param includeStop if {@code true}, then include entries which are <em>equal</em> to {@code stop}
     * @return the number of rows which fall in the range {@code start},{@code stop}, with
     * inclusion determined by {@code includeStart} and {@code includeStop}
     */
    <T extends Comparator<T>> double rangeSelectivity(T start,T stop, boolean includeStart,boolean includeStop,int positionNumber);

    double columnSizeFactor(BitSet validColumns);

}


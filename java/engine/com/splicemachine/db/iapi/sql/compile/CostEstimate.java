/*

   Derby - Class com.splicemachine.db.iapi.sql.compile.CostEstimate

   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to you under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */

package com.splicemachine.db.iapi.sql.compile;

import com.splicemachine.db.iapi.store.access.StoreCostResult;

/**
 * A CostEstimate represents the cost of getting a ResultSet, along with the
 * ordering of rows in the ResultSet, and the estimated number of rows in
 * this ResultSet.
 *
 */

public interface CostEstimate extends StoreCostResult {
    /**
     * Set the cost for this cost estimate.
     */
    void setCost(double cost, double rowCount, double singleScanRowCount);

    void setCost(double cost, double rowCount, double singleScanRowCount,int numPartitions);

    void setRemoteCost(double remoteCost);

    void setLocalCost(double remoteCost);

    boolean isOuterJoin();

    void setOuterJoin(boolean isOuterJoin);

    /**
     * Copy the values from the given cost estimate into this one.
     */
    void setCost(CostEstimate other);

    /**
     * Set the single scan row count.
     */
    void setSingleScanRowCount(double singleRowScanCount);

    void setNumPartitions(int numPartitions);

    /**
     * Compare this cost estimate with the given cost estimate.
     *
     * @param other		The cost estimate to compare this one with
     *
     * @return	< 0 if this < other, 0 if this == other, > 0 if this > other
     */
    double compare(CostEstimate other);

    /**
     * Add this cost estimate to another one.  This presumes that any row
     * ordering is destroyed.
     *
     * @param addend	This cost estimate to add this one to.
     * @param retval	If non-null, put the result here.
     *
     * @return  this + other.
     */
    CostEstimate add(CostEstimate addend, CostEstimate retval);

    /**
     * Multiply this cost estimate by a scalar, non-dimensional number.  This
     * presumes that any row ordering is destroyed.
     *
     * @param multiplicand	The value to multiply this CostEstimate by.
     * @param retval	If non-null, put the result here.
     *
     * @return	this * multiplicand
     */
    CostEstimate multiply(double multiplicand, CostEstimate retval);

    /**
     * Divide this cost estimate by a scalar, non-dimensional number.
     *
     * @param divisor	The value to divide this CostEstimate by.
     * @param retval	If non-null, put the result here.
     *
     * @return	this / divisor
     */
    CostEstimate divide(double divisor, CostEstimate retval);

    /**
     * Get the estimated number of rows returned by the ResultSet that this
     * CostEstimate models.
     */
    double rowCount();

    /**
     * Get the estimated number of rows returned by a single scan of
     * the ResultSet that this CostEstimate models.
     */
    double singleScanRowCount();

    /**
     * @return the number of partitions which must be visited.
     */
    int partitionCount();

    double remoteCost();

    double localCost();

    void setEstimatedHeapSize(long estHeapBytes);

    long getEstimatedHeapSize();

    /** Get a copy of this CostEstimate */
    CostEstimate cloneMe();

    /**
     * Return whether or not this CostEstimate is uninitialized.
     *
     * @return Whether or not this CostEstimate is uninitialized.
     */
    boolean isUninitialized();

    RowOrdering getRowOrdering();

    void setRowOrdering(RowOrdering rowOrdering);

    CostEstimate getBase();

    void setBase(CostEstimate baseCost);

    /**
     * @return true if this is a "real" cost--that is, a cost which was generated
     * using real statistics, rather than from arbitrary scaling factors
     */
    boolean isRealCost();

    void setIsRealCost(boolean isRealCost);

    /**
     * @return the cost to open a scan and begin reading data
     */
    double getOpenCost();

    void setOpenCost(double openCost);

    /**
     * @return the cost to close a scan after completely reading data
     */
    double getCloseCost();

    void setCloseCost(double closeCost);

    void setRowCount(double outerRows);

    /**
     * @return a well-formatted display string
     */
    String prettyString();
}

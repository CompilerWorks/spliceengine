/*

   Derby - Class com.splicemachine.db.iapi.store.access.RowCountable

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

package com.splicemachine.db.iapi.store.access;

import com.splicemachine.db.iapi.error.StandardException;

/**
 * Allows clients to read and write row count estimates for conglomerates.
 *
 * @see ScanController
 * @see StoreCostController
 */
public interface RowCountable
{
    /**
     * Get the total estimated number of rows in the container.
     * <p>
     * The number is a rough estimate and may be grossly off.  In general
     * the server will cache the row count and then occasionally write
     * the count unlogged to a backing store.  If the system happens to 
     * shutdown before the store gets a chance to update the row count it
     * may wander from reality.
     * <p>
     * For btree conglomerates this call will return the count of both
     * user rows and internal implementaation rows.  The "BTREE" implementation
     * generates 1 internal implementation row for each page in the btree, and 
     * it generates 1 internal implementation row for each branch row.  For
     * this reason it is recommended that clients if possible use the count
     * of rows in the heap table to estimate the number of rows in the index
     * rather than use the index estimated row count.
     *
     * @return The total estimated number of rows in the conglomerate.
     *
     * @throws  StandardException  Standard exception policy.
     */
    public long getEstimatedRowCount()
        throws StandardException;

}

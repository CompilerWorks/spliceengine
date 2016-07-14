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

package com.splicemachine.db.impl.sql.compile;

import com.splicemachine.db.impl.sql.execute.IndexColumnOrder;

import java.util.HashSet;
import java.util.Set;

/**
 * List of OrderedColumns
 */
public abstract class OrderedColumnList extends QueryTreeNodeVector<OrderedColumn>{
    /**
     * Get an array of ColumnOrderings to pass to the store
     */
    public IndexColumnOrder[] getColumnOrdering(){
        IndexColumnOrder[] ordering;
        int numCols=size();
        int actualCols;

        ordering=new IndexColumnOrder[numCols];

		/*
            order by is fun, in that we need to ensure
			there are no duplicates in the list.  later copies
			of an earlier entry are considered purely redundant,
			they won't affect the result, so we can drop them.
			We don't know how many columns are in the source,
			so we use a hashtable for lookup of the positions
		*/
        Set<Integer> hashColumns=new HashSet<>();

        actualCols=0;

        for(int i=0;i<numCols;i++){
            OrderedColumn oc=elementAt(i);

            // order by (lang) positions are 1-based,
            // order items (store) are 0-based.
            int position=oc.getColumnPosition()-1;

            Integer posInt=position;

            if(hashColumns.add(posInt)){
                ordering[i]=new IndexColumnOrder(position, oc.isAscending(), oc.isNullsOrderedLow());
                actualCols++;
            }
        }

		/*
			If there were duplicates removed, we need
			to shrink the array down to what we used.
		*/
        if(actualCols<numCols){
            IndexColumnOrder[] newOrdering=new IndexColumnOrder[actualCols];
            System.arraycopy(ordering,0,newOrdering,0,actualCols);
            ordering=newOrdering;
        }

        return ordering;
    }
}

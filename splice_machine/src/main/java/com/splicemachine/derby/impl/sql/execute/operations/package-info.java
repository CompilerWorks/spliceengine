/*
 * Copyright 2012 - 2016 Splice Machine, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

/**
 * 
<p>  
  This package encapsulates the Splice Machine's parallel operations for generating Derby Result Sets.  Here are the
  list of operations to implement...  

	John:
		Done:
			TableScanResultSet
			BulkTableScanResultSet
			NestedLoopJoinResultSet
      		HashJoinResultSet  
			ScrollInsensitiveResultSet 
    		NestedLoopLeftOuterJoinResultSet
    		HashLeftOuterJoinResultSet
			HashScanResultSet
			ProjectRestrictResultSet
		Working On:
			HashScanResultSet (Bugs)			
	Scott:
		Working On:
			IndexRowToBaseRowResultSet
			RowResultSet
			OnceResultSet
			InsertResultSet
			NormalizeResultSet 
		Done:
			ScalarAggregateResultSet
			GroupedAggregateResultSet
			SortResultSet
	Gene:
		Working On:
			RowCountResultSet
	Jessie:
		Done:
			UnionResultSet
			DistinctScalarAggregateResultSet
			DistinctScanResultSet
			DistinctGroupedAggregateResultSet
		Working On:
			MiscResultSet
			NoRowsResultSet
			
			

	Open To Do Items:
	MiscResultSet
	SetTransactionResultSet 
	InsertVTIResultSet
	DeleteVTIResultSet
	DeleteResultSet
	DeleteCascadeResultSet
	UpdateResultSet
	UpdateVTIResultSet
	DeleteCascadeUpdateResultSet 
	CallStatementResultSet
	HashTableResultSet
	AnyResultSet
	VTIResultSet
	MultiProbeTableScanResultSet
	WindowResultSet
	MaterializedResultSet
	CurrentOfResultSet
    SetOpResultSet
	LastIndexKeyResultSet
	getRaDependentTableScanResultSet
	

 @see com.splicemachine.db.iapi.sql.execute.ResultSetFactory
   
 */

package com.splicemachine.derby.impl.sql.execute.operations;


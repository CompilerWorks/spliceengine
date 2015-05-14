/*

   Derby - Class org.apache.derby.impl.sql.execute.GenericResultSetFactory

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

package com.splicemachine.db.impl.sql.execute;

import java.util.List;

import com.splicemachine.db.iapi.error.StandardException;
import com.splicemachine.db.iapi.services.loader.GeneratedMethod;
import com.splicemachine.db.iapi.services.sanity.SanityManager;
import com.splicemachine.db.iapi.sql.Activation;
import com.splicemachine.db.iapi.sql.ResultSet;
import com.splicemachine.db.iapi.sql.conn.Authorizer;
import com.splicemachine.db.iapi.sql.conn.LanguageConnectionContext;
import com.splicemachine.db.iapi.sql.execute.ExecRow;
import com.splicemachine.db.iapi.sql.execute.NoPutResultSet;
import com.splicemachine.db.iapi.sql.execute.ResultSetFactory;
import com.splicemachine.db.iapi.store.access.StaticCompiledOpenConglomInfo;

/**
 * ResultSetFactory provides a wrapper around all of
 * the result sets used in this execution implementation.
 * This removes the need of generated classes to do a new
 * and of the generator to know about all of the result
 * sets.  Both simply know about this interface to getting
 * them.
 * <p>
 * In terms of modularizing, we can create just an interface
 * to this class and invoke the interface.  Different implementations
 * would get the same information provided but could potentially
 * massage/ignore it in different ways to satisfy their
 * implementations.  The practicality of this is to be seen.
 * <p>
 * The cost of this type of factory is that once you touch it,
 * you touch *all* of the possible result sets, not just
 * the ones you need.  So the first time you touch it could
 * be painful ... that might be a problem for execution.
 *
 */
public abstract class GenericResultSetFactory implements ResultSetFactory 
{
	//
	// ResultSetFactory interface
	//
	public GenericResultSetFactory()
	{
	}

    @Override
	public abstract ResultSet getInsertResultSet(NoPutResultSet source, GeneratedMethod generationClauses, GeneratedMethod checkGM) throws StandardException;

	/**
		@see ResultSetFactory#getInsertVTIResultSet
		@exception StandardException thrown on error
	 */
	public ResultSet getInsertVTIResultSet(NoPutResultSet source, 
										NoPutResultSet vtiRS
										)
		throws StandardException
	{
		Activation activation = source.getActivation();
		getAuthorizer(activation).authorize(activation, Authorizer.SQL_WRITE_OP);
		return new InsertVTIResultSet(source, vtiRS, activation );
	}

	/**
		@see ResultSetFactory#getDeleteVTIResultSet
		@exception StandardException thrown on error
	 */
	public ResultSet getDeleteVTIResultSet(NoPutResultSet source)
		throws StandardException
	{
		Activation activation = source.getActivation();
		getAuthorizer(activation).authorize(activation, Authorizer.SQL_WRITE_OP);
		return new DeleteVTIResultSet(source, activation);
	}

    @Override
	public abstract ResultSet getDeleteResultSet(NoPutResultSet source) throws StandardException;

    @Override
	public abstract ResultSet getDeleteCascadeResultSet(NoPutResultSet source,
											   int constantActionItem,
											   ResultSet[] dependentResultSets,
											   String resultSetId) throws StandardException;


    @Override
	public abstract ResultSet getUpdateResultSet(NoPutResultSet source, GeneratedMethod generationClauses, GeneratedMethod checkGM) throws StandardException;

	/**
		@see ResultSetFactory#getUpdateVTIResultSet
		@exception StandardException thrown on error
	 */
	public ResultSet getUpdateVTIResultSet(NoPutResultSet source)
			throws StandardException
	{
		Activation activation = source.getActivation();
		getAuthorizer(activation).authorize(activation, Authorizer.SQL_WRITE_OP);
		return new UpdateVTIResultSet(source, activation);
	}



    @Override
	public abstract ResultSet getDeleteCascadeUpdateResultSet(NoPutResultSet source,
                                                     GeneratedMethod generationClauses,
													 GeneratedMethod checkGM,
													 int constantActionItem,
													 int rsdItem) throws StandardException;

	/**
		@see ResultSetFactory#getCallStatementResultSet
		@exception StandardException thrown on error
	 */
	public ResultSet getCallStatementResultSet(GeneratedMethod methodCall,
				Activation activation)
			throws StandardException
	{
		getAuthorizer(activation).authorize(activation, Authorizer.SQL_CALL_OP);
		return new CallStatementResultSet(methodCall, activation);
	}

	/**
		@see ResultSetFactory#getProjectRestrictResultSet
		@exception StandardException thrown on error
	 */
	public NoPutResultSet getProjectRestrictResultSet(NoPutResultSet source,
		GeneratedMethod restriction, 
		GeneratedMethod projection, int resultSetNumber,
		GeneratedMethod constantRestriction,
		int mapRefItem,
        int cloneMapItem,
		boolean reuseResult,
		boolean doesProjection,
		double optimizerEstimatedRowCount,
		double optimizerEstimatedCost)
			throws StandardException
	{
		return new ProjectRestrictResultSet(source, source.getActivation(), 
			restriction, projection, resultSetNumber, 
            constantRestriction, mapRefItem, cloneMapItem,
			reuseResult,
			doesProjection,
		    optimizerEstimatedRowCount,
			optimizerEstimatedCost);
	}

	/**
		@see ResultSetFactory#getSortResultSet
		@exception StandardException thrown on error
	 */
	public NoPutResultSet getSortResultSet(NoPutResultSet source,
		boolean distinct, 
		boolean isInSortedOrder,
		int orderItem,
		GeneratedMethod rowAllocator, 
		int maxRowSize,
		int resultSetNumber, 
		double optimizerEstimatedRowCount,
		double optimizerEstimatedCost)
			throws StandardException
	{
		return new SortResultSet(source, 
			distinct, 
			isInSortedOrder,
			orderItem,
			source.getActivation(), 
			rowAllocator, 
			maxRowSize,
			resultSetNumber, 
		    optimizerEstimatedRowCount,
			optimizerEstimatedCost);
	}

	/**
		@see ResultSetFactory#getScalarAggregateResultSet
		@exception StandardException thrown on error
	 */
	public NoPutResultSet getScalarAggregateResultSet(NoPutResultSet source,
		boolean isInSortedOrder,
		int aggregateItem,
		int orderItem,
		GeneratedMethod rowAllocator, 
		int maxRowSize,
		int resultSetNumber, 
		boolean singleInputRow,
		double optimizerEstimatedRowCount,
		double optimizerEstimatedCost) 
			throws StandardException
	{
		return new ScalarAggregateResultSet(
						source, isInSortedOrder, aggregateItem, source.getActivation(),
						rowAllocator, resultSetNumber, singleInputRow,
						optimizerEstimatedRowCount,
						optimizerEstimatedCost);
	}

	/**
		@see ResultSetFactory#getDistinctScalarAggregateResultSet
		@exception StandardException thrown on error
	 */
	public NoPutResultSet getDistinctScalarAggregateResultSet(NoPutResultSet source,
		boolean isInSortedOrder,
		int aggregateItem,
		int orderItem,
		GeneratedMethod rowAllocator, 
		int maxRowSize,
		int resultSetNumber, 
		boolean singleInputRow,
		double optimizerEstimatedRowCount,
		double optimizerEstimatedCost) 
			throws StandardException
	{
		return new DistinctScalarAggregateResultSet(
						source, isInSortedOrder, aggregateItem, orderItem, source.getActivation(),
						rowAllocator, maxRowSize, resultSetNumber, singleInputRow,
						optimizerEstimatedRowCount,
						optimizerEstimatedCost);
	}

	/**
		@see ResultSetFactory#getGroupedAggregateResultSet
		@exception StandardException thrown on error
	 */
	public NoPutResultSet getGroupedAggregateResultSet(NoPutResultSet source,
		boolean isInSortedOrder,
		int aggregateItem,
		int orderItem,
		GeneratedMethod rowAllocator, 
		int maxRowSize,
		int resultSetNumber, 
		double optimizerEstimatedRowCount,
		double optimizerEstimatedCost,
		boolean isRollup) 
			throws StandardException
	{
		return new GroupedAggregateResultSet(
						source, isInSortedOrder, aggregateItem, orderItem, source.getActivation(),
						rowAllocator, maxRowSize, resultSetNumber, optimizerEstimatedRowCount,
						optimizerEstimatedCost, isRollup);
	}

	/**
		@see ResultSetFactory#getDistinctGroupedAggregateResultSet
		@exception StandardException thrown on error
	 */
	public NoPutResultSet getDistinctGroupedAggregateResultSet(NoPutResultSet source,
		boolean isInSortedOrder,
		int aggregateItem,
		int orderItem,
		GeneratedMethod rowAllocator, 
		int maxRowSize,
		int resultSetNumber, 
		double optimizerEstimatedRowCount,
		double optimizerEstimatedCost,
		boolean isRollup) 
			throws StandardException
	{
		return new DistinctGroupedAggregateResultSet(
						source, isInSortedOrder, aggregateItem, orderItem, source.getActivation(),
						rowAllocator, maxRowSize, resultSetNumber, optimizerEstimatedRowCount,
						optimizerEstimatedCost, isRollup);
	}
											

	/**
		@see ResultSetFactory#getAnyResultSet
		@exception StandardException thrown on error
	 */
	public NoPutResultSet getAnyResultSet(NoPutResultSet source,
		GeneratedMethod emptyRowFun, int resultSetNumber,
		int subqueryNumber, int pointOfAttachment,
		double optimizerEstimatedRowCount,
		double optimizerEstimatedCost)
			throws StandardException
	{
		return new AnyResultSet(source,
					 source.getActivation(), emptyRowFun, resultSetNumber,
					 subqueryNumber, pointOfAttachment,
					 optimizerEstimatedRowCount,
					 optimizerEstimatedCost);
	}

	/**
		@see ResultSetFactory#getOnceResultSet
		@exception StandardException thrown on error
	 */
	public NoPutResultSet getOnceResultSet(NoPutResultSet source,
	 GeneratedMethod emptyRowFun,
		int cardinalityCheck, int resultSetNumber,
		int subqueryNumber, int pointOfAttachment,
		double optimizerEstimatedRowCount,
		double optimizerEstimatedCost)
			throws StandardException
	{
		return new OnceResultSet(source,
					 source.getActivation(), emptyRowFun, 
					 cardinalityCheck, resultSetNumber,
					 subqueryNumber, pointOfAttachment,
				     optimizerEstimatedRowCount,
					 optimizerEstimatedCost);
	}

	/**
		@see ResultSetFactory#getRowResultSet
	 */
	public NoPutResultSet getRowResultSet(Activation activation, GeneratedMethod row,
									 boolean canCacheRow,
									 int resultSetNumber,
									 double optimizerEstimatedRowCount,
									 double optimizerEstimatedCost)
	{
		return new RowResultSet(activation, row, canCacheRow, resultSetNumber,
							    optimizerEstimatedRowCount,
								optimizerEstimatedCost);
	}

    public NoPutResultSet getCachedResultSet(final Activation activation,
                                             final List rows,
                                             final int resultSetNumber)
            throws StandardException
    {
        final int numRows = rows.size();
        if (numRows == 0) {
            return new RowResultSet(activation, (ExecRow) null, true, resultSetNumber, 0, 0);
        }
        NoPutResultSet[] rrs = new NoPutResultSet[numRows];
        NoPutResultSet[] urs = new NoPutResultSet[numRows - 1];

        for (int i = 0; i < numRows; i++) {
            rrs[i] = new RowResultSet(activation, (ExecRow) rows.get(i), true, resultSetNumber, 1, 0);
            if (i > 0) {
                urs[i - 1] = new UnionResultSet((i > 1) ? (NoPutResultSet) urs[i - 2] : (NoPutResultSet) rrs[0],
                                                       rrs[i],
                                                       activation,
                                                       resultSetNumber,
                                                       i + 1,
                                                       0);
            }
        }

        return (numRows == 1) ? rrs[0] : urs[urs.length -1];

    }


	/**
    	a distinct scan generator, for ease of use at present.
		@see ResultSetFactory#getHashScanResultSet
		@exception StandardException thrown on error
	 */
	public NoPutResultSet getDistinctScanResultSet(
                         			Activation activation,
									long conglomId,
									int scociItem,
									GeneratedMethod resultRowAllocator,
									int resultSetNumber,
									int hashKeyColumn,
									String tableName,
									String userSuppliedOptimizerOverrides,
									String indexName,
									boolean isConstraint,
									int colRefItem,
									int lockMode,
									boolean tableLocked,
									int isolationLevel,
									double optimizerEstimatedRowCount,
									double optimizerEstimatedCost)
			throws StandardException
	{
        StaticCompiledOpenConglomInfo scoci = (StaticCompiledOpenConglomInfo)(activation.getPreparedStatement().
						getSavedObject(scociItem));
		return new DistinctScanResultSet(
								conglomId,
								scoci,
								activation,
								resultRowAllocator,
								resultSetNumber,
								hashKeyColumn,
								tableName,
								userSuppliedOptimizerOverrides,
								indexName,
								isConstraint,
								colRefItem,
								lockMode,
								tableLocked,
								isolationLevel,
								optimizerEstimatedRowCount,
								optimizerEstimatedCost);
	}

	/**
		@see ResultSetFactory#getIndexRowToBaseRowResultSet
		@exception StandardException	Thrown on error
	 */
	public NoPutResultSet getIndexRowToBaseRowResultSet(
								long conglomId,
								int scociItem,
								NoPutResultSet source,
								GeneratedMethod resultRowAllocator,
								int resultSetNumber,
								String indexName,
								int heapColRefItem,
								int allColRefItem,
								int heapOnlyColRefItem,
								int indexColMapItem,
								GeneratedMethod restriction,
								boolean forUpdate,
								double optimizerEstimatedRowCount,
								double optimizerEstimatedCost)
			throws StandardException
	{
		return new IndexRowToBaseRowResultSet(
								conglomId,
								scociItem,
								source.getActivation(),
								source,
								resultRowAllocator,
								resultSetNumber,
								indexName,
								heapColRefItem,
								allColRefItem,
								heapOnlyColRefItem,
								indexColMapItem,
								restriction,
								forUpdate,
							    optimizerEstimatedRowCount,
								optimizerEstimatedCost);
	}

    /**
     @see ResultSetFactory#getWindowResultSet
     @exception StandardException	Thrown on error
     */
    public NoPutResultSet getWindowResultSet(NoPutResultSet source,
                                             boolean isInSortedOrder,
                                             int aggregateItem,
                                             GeneratedMethod rowAllocator,
                                             int rowSize,
                                             int resultSetNumber,
                                             double optimizerEstimatedRowCount,
                                             double optimizerEstimatedCost)
        throws StandardException
    {
        return new WindowResultSet(source,
        isInSortedOrder,
        aggregateItem,
        source.getActivation(),
        rowAllocator,
        rowSize,
        resultSetNumber,
        optimizerEstimatedRowCount,
        optimizerEstimatedCost);
    }


    /**
		@see ResultSetFactory#getNestedLoopJoinResultSet
		@exception StandardException thrown on error
	 */

    public NoPutResultSet getNestedLoopJoinResultSet(NoPutResultSet leftResultSet,
								   int leftNumCols,
								   NoPutResultSet rightResultSet,
								   int rightNumCols,
								   GeneratedMethod joinClause,
								   int resultSetNumber,
								   boolean oneRowRightSide,
								   boolean notExistsRightSide,
								   double optimizerEstimatedRowCount,
								   double optimizerEstimatedCost,
								   String userSuppliedOptimizerOverrides)
			throws StandardException
	{
		return new NestedLoopJoinResultSet(leftResultSet, leftNumCols,
										   rightResultSet, rightNumCols,
										   leftResultSet.getActivation(), joinClause,
										   resultSetNumber, 
										   oneRowRightSide, 
										   notExistsRightSide, 
										   optimizerEstimatedRowCount,
										   optimizerEstimatedCost,
										   userSuppliedOptimizerOverrides);
	}

	/**
		@see ResultSetFactory#getHashJoinResultSet
		@exception StandardException thrown on error
	 */

    public NoPutResultSet getHashJoinResultSet(NoPutResultSet leftResultSet,
								   int leftNumCols,
								   NoPutResultSet rightResultSet,
								   int rightNumCols,
								   GeneratedMethod joinClause,
								   int resultSetNumber,
								   boolean oneRowRightSide,
								   boolean notExistsRightSide,
								   double optimizerEstimatedRowCount,
								   double optimizerEstimatedCost,
								   String userSuppliedOptimizerOverrides)
			throws StandardException
	{
		return new HashJoinResultSet(leftResultSet, leftNumCols,
										   rightResultSet, rightNumCols,
										   leftResultSet.getActivation(), joinClause,
										   resultSetNumber, 
										   oneRowRightSide, 
										   notExistsRightSide, 
										   optimizerEstimatedRowCount,
										   optimizerEstimatedCost,
										   userSuppliedOptimizerOverrides);
	}

	/**
		@see ResultSetFactory#getNestedLoopLeftOuterJoinResultSet
		@exception StandardException thrown on error
	 */

    public NoPutResultSet getNestedLoopLeftOuterJoinResultSet(NoPutResultSet leftResultSet,
								   int leftNumCols,
								   NoPutResultSet rightResultSet,
								   int rightNumCols,
								   GeneratedMethod joinClause,
								   int resultSetNumber,
								   GeneratedMethod emptyRowFun,
								   boolean wasRightOuterJoin,
								   boolean oneRowRightSide,
								   boolean notExistsRightSide,
								   double optimizerEstimatedRowCount,
								   double optimizerEstimatedCost,
								   String userSuppliedOptimizerOverrides)
			throws StandardException
	{
		return new NestedLoopLeftOuterJoinResultSet(leftResultSet, leftNumCols,
										   rightResultSet, rightNumCols,
										   leftResultSet.getActivation(), joinClause,
										   resultSetNumber, 
										   emptyRowFun, 
										   wasRightOuterJoin,
										   oneRowRightSide,
										   notExistsRightSide,
										   optimizerEstimatedRowCount,
										   optimizerEstimatedCost,
										   userSuppliedOptimizerOverrides);
	}

	/**
		@see ResultSetFactory#getHashLeftOuterJoinResultSet
		@exception StandardException thrown on error
	 */

    public NoPutResultSet getHashLeftOuterJoinResultSet(NoPutResultSet leftResultSet,
								   int leftNumCols,
								   NoPutResultSet rightResultSet,
								   int rightNumCols,
								   GeneratedMethod joinClause,
								   int resultSetNumber,
								   GeneratedMethod emptyRowFun,
								   boolean wasRightOuterJoin,
								   boolean oneRowRightSide,
								   boolean notExistsRightSide,
								   double optimizerEstimatedRowCount,
								   double optimizerEstimatedCost,
								   String userSuppliedOptimizerOverrides)
			throws StandardException
	{
		return new HashLeftOuterJoinResultSet(leftResultSet, leftNumCols,
										   rightResultSet, rightNumCols,
										   leftResultSet.getActivation(), joinClause,
										   resultSetNumber, 
										   emptyRowFun, 
										   wasRightOuterJoin,
										   oneRowRightSide,
										   notExistsRightSide,
										   optimizerEstimatedRowCount,
										   optimizerEstimatedCost,
										   userSuppliedOptimizerOverrides);
	}

	/**
		@see ResultSetFactory#getSetTransactionResultSet
		@exception StandardException thrown when unable to create the
			result set
	 */
	public ResultSet getSetTransactionResultSet(Activation activation) 
		throws StandardException
	{
		getAuthorizer(activation).authorize(activation, Authorizer.SQL_ARBITARY_OP);		
		return new SetTransactionResultSet(activation);
	}

	/**
		@see ResultSetFactory#getMaterializedResultSet
		@exception StandardException thrown on error
	 */
	public NoPutResultSet getMaterializedResultSet(NoPutResultSet source,
							int resultSetNumber,
						    double optimizerEstimatedRowCount,
							double optimizerEstimatedCost)
		throws StandardException
	{
		return new MaterializedResultSet(source, source.getActivation(), 
									  resultSetNumber, 
									  optimizerEstimatedRowCount,
									  optimizerEstimatedCost);
	}

	/**
		@see ResultSetFactory#getScrollInsensitiveResultSet
		@exception StandardException thrown on error
	 */
	public NoPutResultSet getScrollInsensitiveResultSet(NoPutResultSet source,
							Activation activation, int resultSetNumber,
							int sourceRowWidth,
							boolean scrollable,
						    double optimizerEstimatedRowCount,
							double optimizerEstimatedCost)
		throws StandardException
	{
		/* ResultSet tree is dependent on whether or not this is
		 * for a scroll insensitive cursor.
		 */

		if (scrollable)
		{
			return new ScrollInsensitiveResultSet(source, activation, 
									  resultSetNumber, 
									  sourceRowWidth,
									  optimizerEstimatedRowCount,
									  optimizerEstimatedCost);
		}
		else
		{
			return source;
		}
	}

	/**
		@see ResultSetFactory#getNormalizeResultSet
		@exception StandardException thrown on error
	 */
	public NoPutResultSet getNormalizeResultSet(NoPutResultSet source,
							int resultSetNumber, 
							int erdNumber,
						    double optimizerEstimatedRowCount,
							double optimizerEstimatedCost,
							boolean forUpdate)
		throws StandardException
	{
		return new NormalizeResultSet(source, source.getActivation(), 
									  resultSetNumber, erdNumber, 
									  optimizerEstimatedRowCount,
									  optimizerEstimatedCost, forUpdate);
	}

	/**
		@see ResultSetFactory#getCurrentOfResultSet
	 */
	public NoPutResultSet getCurrentOfResultSet(String cursorName, 
	    Activation activation, int resultSetNumber)
	{
		return new CurrentOfResultSet(cursorName, activation, resultSetNumber);
	}

	/**
		@see ResultSetFactory#getDDLResultSet
		@exception StandardException thrown on error
	 */
	public ResultSet getDDLResultSet(Activation activation)
					throws StandardException
	{
		getAuthorizer(activation).authorize(activation, Authorizer.SQL_DDL_OP);
		return getMiscResultSet( activation);
	}

	/**
		@see ResultSetFactory#getMiscResultSet
		@exception StandardException thrown on error
	 */
	public ResultSet getMiscResultSet(Activation activation)
					throws StandardException
	{
		getAuthorizer(activation).authorize(activation, Authorizer.SQL_ARBITARY_OP);
		return new MiscResultSet(activation);
	}

	/**
    	a minimal union scan generator, for ease of use at present.
		@see ResultSetFactory#getUnionResultSet
		@exception StandardException thrown on error
	 */
    public NoPutResultSet getUnionResultSet(NoPutResultSet leftResultSet,
								   NoPutResultSet rightResultSet,
								   int resultSetNumber,
								   double optimizerEstimatedRowCount,
								   double optimizerEstimatedCost)
			throws StandardException
	{
		return new UnionResultSet(leftResultSet, rightResultSet, 
				                  leftResultSet.getActivation(),
								  resultSetNumber, 
								  optimizerEstimatedRowCount,
								  optimizerEstimatedCost);
	}

    public NoPutResultSet getSetOpResultSet( NoPutResultSet leftSource,
                                             NoPutResultSet rightSource,
                                             Activation activation, 
                                             int resultSetNumber,
                                             long optimizerEstimatedRowCount,
                                             double optimizerEstimatedCost,
                                             int opType,
                                             boolean all,
                                            int intermediateOrderByColumnsSavedObject,
                                             int intermediateOrderByDirectionSavedObject,
                                             int intermediateOrderByNullsLowSavedObject)
        throws StandardException
    {
        return new SetOpResultSet( leftSource,
                                   rightSource,
                                   activation,
                                   resultSetNumber,
                                   optimizerEstimatedRowCount,
                                   optimizerEstimatedCost,
                                   opType,
                                   all,
                                   intermediateOrderByColumnsSavedObject,
                                   intermediateOrderByDirectionSavedObject,
                                   intermediateOrderByNullsLowSavedObject);
    }

	/**
	 * A last index key sresult set returns the last row from
	 * the index in question.  It is used as an ajunct to max().
	 *
	 * @param activation 		the activation for this result set,
	 *		which provides the context for the row allocation operation.
	 * @param resultSetNumber	The resultSetNumber for the ResultSet
	 * @param resultRowAllocator a reference to a method in the activation
	 * 						that creates a holder for the result row of the scan.  May
	 *						be a partial row.  <verbatim>
	 *		ExecRow rowAllocator() throws StandardException; </verbatim>
	 * @param conglomId 		the conglomerate of the table to be scanned.
	 * @param tableName			The full name of the table
	 * @param userSuppliedOptimizerOverrides		Overrides specified by the user on the sql
	 * @param indexName			The name of the index, if one used to access table.
	 * @param colRefItem		An saved item for a bitSet of columns that
	 *							are referenced in the underlying table.  -1 if
	 *							no item.
	 * @param lockMode			The lock granularity to use (see
	 *							TransactionController in access)
	 * @param tableLocked		Whether or not the table is marked as using table locking
	 *							(in sys.systables)
	 * @param isolationLevel	Isolation level (specified or not) to use on scans
	 * @param optimizerEstimatedRowCount	Estimated total # of rows by
	 * 										optimizer
	 * @param optimizerEstimatedCost		Estimated total cost by optimizer
	 *
	 * @return the scan operation as a result set.
 	 *
	 * @exception StandardException thrown when unable to create the
	 * 				result set
	 */
	public NoPutResultSet getLastIndexKeyResultSet
	(
		Activation 			activation,
		int 				resultSetNumber,
		GeneratedMethod 	resultRowAllocator,
		long 				conglomId,
		String 				tableName,
		String 				userSuppliedOptimizerOverrides,
		String 				indexName,
		int 				colRefItem,
		int 				lockMode,
		boolean				tableLocked,
		int					isolationLevel,
		double				optimizerEstimatedRowCount,
		double 				optimizerEstimatedCost
	) throws StandardException
	{
		return new LastIndexKeyResultSet(
					activation,
					resultSetNumber,
					resultRowAllocator,
					conglomId,
					tableName,
					userSuppliedOptimizerOverrides,
					indexName,
					colRefItem,
					lockMode,
					tableLocked,
					isolationLevel,
					optimizerEstimatedRowCount,
					optimizerEstimatedCost);
	}



	
	/**
	 * @see ResultSetFactory#getRowCountResultSet
	 */
	public NoPutResultSet getRowCountResultSet(
		NoPutResultSet source,
		Activation activation,
		int resultSetNumber,
		GeneratedMethod offsetMethod,
		GeneratedMethod fetchFirstMethod,
        boolean hasJDBClimitClause,
		double optimizerEstimatedRowCount,
		double optimizerEstimatedCost)
		throws StandardException
	{
		return new RowCountResultSet(source,
									 activation,
									 resultSetNumber,
									 offsetMethod,
									 fetchFirstMethod,
									 hasJDBClimitClause,
									 optimizerEstimatedRowCount,
									 optimizerEstimatedCost);
	}


	static private Authorizer getAuthorizer(Activation activation)
	{
		LanguageConnectionContext lcc = activation.getLanguageConnectionContext();
		return lcc.getAuthorizer();
	}


   /////////////////////////////////////////////////////////////////
   //
   //	PUBLIC MINIONS
   //
   /////////////////////////////////////////////////////////////////

}

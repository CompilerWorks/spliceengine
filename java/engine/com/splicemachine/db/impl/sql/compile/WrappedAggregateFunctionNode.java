package com.splicemachine.db.impl.sql.compile;

import java.util.ArrayList;
import java.util.List;

import com.splicemachine.db.iapi.error.StandardException;
import com.splicemachine.db.iapi.services.compiler.MethodBuilder;
import com.splicemachine.db.iapi.services.sanity.SanityManager;
import com.splicemachine.db.iapi.sql.compile.AggregateDefinition;
import com.splicemachine.db.iapi.sql.compile.C_NodeTypes;
import com.splicemachine.db.iapi.sql.dictionary.DataDictionary;
import com.splicemachine.db.iapi.types.DataTypeDescriptor;

/**
 * @author Jeff Cunningham
 *         Date: 10/2/14
 */
public class WrappedAggregateFunctionNode extends WindowFunctionNode {
    private AggregateNode aggregateFunction;
    /**
     * Initializer. QueryTreeNode override.
     *
     * @param arg1 the window definition
     * @param arg2 the wrapped aggregate function
     * @throws com.splicemachine.db.iapi.error.StandardException
     */
    public void init(Object arg1, Object arg2) throws StandardException {
        super.init(arg1, null);
        aggregateFunction = (AggregateNode) arg2;
        this.aggregateName = aggregateFunction.aggregateName;
        this.operator = aggregateFunction.operator;
    }

    @Override
    public String getName() {
        return aggregateFunction.getAggregateName();
    }

    @Override
    protected void setOperands(ValueNode[] operands) {
        if (operands != null && operands.length > 0) {
            this.operand = operands[0];
        }
    }

    @Override
     public ValueNode getNewNullResultExpression() throws StandardException {
         return aggregateFunction.getNewNullResultExpression();
     }

    @Override
    public DataTypeDescriptor getTypeServices() {
        return aggregateFunction.getTypeServices();
    }

    @Override
    public ValueNode[] getOperands() {
        return new ValueNode[] {aggregateFunction.operand};
    }

    /**
     * Overridden to redirect the call to the wrapped aggregate node.
     * @param rc the result column to which to add the new ref, only used if not null.
     * @param tableNumber The tableNumber for the new ColumnReference
     * @param nestingLevel this node's nesting level
     * @param newResultColumn the source RC for the new CR
     * @return the new CR
     * @throws StandardException
     */
    @Override
    public ValueNode replaceCallWithColumnReference(ResultColumn rc, int tableNumber, int nestingLevel, ResultColumn
        newResultColumn) throws StandardException {
        ColumnReference node = (ColumnReference) aggregateFunction.replaceAggregatesWithColumnReferences(
            (ResultColumnList) getNodeFactory().getNode(
                C_NodeTypes.RESULT_COLUMN_LIST,
                getContextManager()),
            tableNumber);

        // Mark the ColumnReference as being generated to replace a call to
        // a window function
        node.markGeneratedToReplaceWindowFunctionCall();

        node.setSource(newResultColumn);
        if (rc != null) {
            rc.setExpression(node);
        }
        return node;
    }

    /**
      * QueryTreeNode override. Prints the sub-nodes of this object.
      *
      * @param depth The depth of this node in the tree
      * @see QueryTreeNode#printSubNodes
      */
     public void printSubNodes(int depth) {
         if (SanityManager.DEBUG) {
             super.printSubNodes(depth);

             printLabel(depth, "aggregate: ");
             aggregateFunction.treePrint(depth + 1);
         }
     }

     @Override
      public ValueNode replaceAggregatesWithColumnReferences(ResultColumnList rcl,
                                                             int tableNumber) throws StandardException {
          return aggregateFunction.replaceAggregatesWithColumnReferences(rcl, tableNumber);
      }

    @Override
    public ValueNode bindExpression(FromList fromList,
                                    SubqueryList subqueryList,
                                    List<AggregateNode> aggregateVector) throws StandardException {
        // DB-2086 - Vector.remove() calls node1.isEquivalent(node2), not node1.equals(node2), which
        // returns true for identical aggregate nodes removing the first aggregate, not necessarily
        // this one. We need to create a tmp Vector and add all Agg nodes found but this delegate by
        // checking for object identity (==)
        List<AggregateNode> tmp = new ArrayList<AggregateNode>();
        aggregateFunction.bindExpression(fromList,subqueryList,tmp);

        // We don't want to be in this aggregateVector - we add all aggs found during bind except
        // this delegate.
        // We want to bind the wrapped aggregate (and operand, etc) but we don't want to show up
        // in this list as an aggregate. The list will be handed to GroupByNode, which we don't
        // want doing the work.  Window function code will handle the window function aggregates
        for (AggregateNode aggFn : tmp) {
            if (aggregateFunction != aggFn) {
                aggregateVector.add(aggFn);
            }
        }

        // Now that delegate is bound, set some required fields on this
        // TODO: What all is required?
        this.operator = aggregateFunction.operator;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        WrappedAggregateFunctionNode that = (WrappedAggregateFunctionNode) o;

        return aggregateFunction.equals(that.aggregateFunction);

    }

    @Override
    public int hashCode() {
        return aggregateFunction.hashCode();
    }

    /**
     * Get the generated ResultColumn where this
     * aggregate now resides after a call to
     * replaceAggregatesWithColumnReference().
     *
     * @return the result column
     */
     @Override
     AggregateDefinition getAggregateDefinition() {
         return aggregateFunction.getAggregateDefinition();
     }
     @Override
     public boolean isDistinct() {
         return aggregateFunction.isDistinct();
     }

     @Override
     public String getAggregatorClassName() {
         return aggregateFunction.getAggregatorClassName();
     }

     @Override
     public String getAggregateName() {
         return aggregateFunction.getAggregateName();
     }

     @Override
     public ResultColumn getNewAggregatorResultColumn(DataDictionary dd) throws StandardException {
         return aggregateFunction.getNewAggregatorResultColumn(dd);
     }

     @Override
     public ResultColumn getNewExpressionResultColumn(DataDictionary dd) throws StandardException {
         return aggregateFunction.getNewExpressionResultColumn(dd);
     }

     @Override
     public void generateExpression(ExpressionClassBuilder acb, MethodBuilder mb) throws StandardException {
         aggregateFunction.generateExpression(acb, mb);
     }

     @Override
     public String getSQLName() {
         return aggregateFunction.getSQLName();
     }

     @Override
     public ColumnReference getGeneratedRef() {
         return aggregateFunction.getGeneratedRef();
     }

     @Override
     public ResultColumn getGeneratedRC() {
         return aggregateFunction.getGeneratedRC();
     }

}

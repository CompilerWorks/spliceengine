package com.splicemachine.derby.stream.iapi;

import com.google.common.base.Optional;
import com.splicemachine.db.iapi.error.StandardException;
import com.splicemachine.derby.iapi.sql.execute.SpliceOperation;
import com.splicemachine.derby.stream.function.SpliceFlatMapFunction;
import com.splicemachine.derby.stream.function.SpliceFunction;
import com.splicemachine.derby.stream.function.SpliceFunction2;
import com.splicemachine.derby.stream.index.HTableWriterBuilder;
import com.splicemachine.derby.stream.output.delete.DeleteTableWriterBuilder;
import com.splicemachine.derby.stream.output.insert.InsertTableWriterBuilder;
import com.splicemachine.derby.stream.output.update.UpdateTableWriterBuilder;
import scala.Tuple2;
import java.util.Comparator;
import java.util.Iterator;

/**
 * Stream of data acting on a key/values.
 */
public interface PairDataSet<K,V> {
    public DataSet<V> values();
    public DataSet<V> values(String name);
    public DataSet<V> values(boolean isLast);
    public DataSet<K> keys();
    public <Op extends SpliceOperation> PairDataSet<K,V> reduceByKey(SpliceFunction2<Op,V,V,V> function2);
    public <Op extends SpliceOperation> PairDataSet<K,V> reduceByKey(SpliceFunction2<Op,V,V,V> function2, boolean isLast);
    public <Op extends SpliceOperation, U> DataSet<U> map(SpliceFunction<Op,Tuple2<K,V>,U> function);
    public <Op extends SpliceOperation, U> DataSet<U> flatmap(SpliceFlatMapFunction<Op,Tuple2<K,V>,U> function);
    public <Op extends SpliceOperation, U> DataSet<U> flatmap(SpliceFlatMapFunction<Op,Tuple2<K,V>,U> function, boolean isLast);
    public PairDataSet<K,V> sortByKey(Comparator<K> comparator);
    public PairDataSet<K,V> sortByKey(Comparator<K> comparator, String name);
    public PairDataSet<K, Iterable<V>> groupByKey();
    public PairDataSet<K, Iterable<V>> groupByKey(String name);
    public <W> PairDataSet<K,Tuple2<V,Optional<W>>> hashLeftOuterJoin(PairDataSet<K,W> rightDataSet);
    public <W> PairDataSet<K,Tuple2<Optional<V>,W>> hashRightOuterJoin(PairDataSet<K,W> rightDataSet);
    public <W> PairDataSet<K,Tuple2<V,W>> hashJoin(PairDataSet<K,W> rightDataSet);
    public <W> PairDataSet<K,Tuple2<V,W>> hashJoin(PairDataSet<K,W> rightDataSet, String name);
    public <W> PairDataSet<K,V> subtractByKey(PairDataSet<K,W> rightDataSet);
    public <W> PairDataSet<K,V> subtractByKey(PairDataSet<K,W> rightDataSet, String name);
    public <W> PairDataSet<K,Tuple2<Iterable<V>, Iterable<W>>> cogroup(PairDataSet<K,W> rightDataSet);
    public <W> PairDataSet<K,Tuple2<Iterable<V>, Iterable<W>>> cogroup(PairDataSet<K,W> rightDataSet, String name);
    public PairDataSet<K,V> union (PairDataSet<K,V> dataSet);
    public DataSet<V> insertData(InsertTableWriterBuilder builder, OperationContext operationContext) throws StandardException;
    public DataSet<V> updateData(UpdateTableWriterBuilder builder, OperationContext operationContext) throws StandardException;
    public DataSet<V> deleteData(DeleteTableWriterBuilder builder, OperationContext operationContext) throws StandardException;
    public DataSet<V> writeKVPair(HTableWriterBuilder builder);
    public String toString();
    public <Op extends SpliceOperation, U> DataSet<U> mapPartitions(SpliceFlatMapFunction<Op,Iterator<Tuple2<K,V>>, U> f);

}
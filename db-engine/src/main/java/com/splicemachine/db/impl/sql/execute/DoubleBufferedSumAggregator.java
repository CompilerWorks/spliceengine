package com.splicemachine.db.impl.sql.execute;

import com.splicemachine.db.iapi.error.StandardException;
import com.splicemachine.db.iapi.sql.execute.ExecAggregator;
import com.splicemachine.db.iapi.types.DataValueDescriptor;
import com.splicemachine.db.iapi.types.NumberDataType;
import com.splicemachine.db.iapi.types.SQLDouble;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.math.BigDecimal;
import java.util.Arrays;

/**
 * @author Scott Fines
 *         Date: 5/15/14
 */
public class DoubleBufferedSumAggregator extends SumAggregator{

		private final double[] buffer;
		private final int length;
		private int position;

		private double sum = 0d;
		private boolean isNull = true;

		public DoubleBufferedSumAggregator(int bufferSize) {
				int s = 1;
				while(s<bufferSize){
						s<<=1;
				}
				buffer = new double[s];
				this.length = s-1;
				position = 0;
		}

		@Override
		protected void accumulate(DataValueDescriptor addend) throws StandardException {
				buffer[position] = addend.getDouble();
				incrementPosition();
		}

		public void addDirect(double l) throws StandardException {
				buffer[position] = l;
				incrementPosition();
		}

		@Override
		public void merge(ExecAggregator addend) throws StandardException {
				if(addend==null) return; //treat null entries as zero
				//In Splice, we should never see a different type of an ExecAggregator
				DoubleBufferedSumAggregator other = (DoubleBufferedSumAggregator)addend;
            if (other.isNull){
               return;
            }

            if (other.sum != 0d) {
                buffer[position] = other.sum;
                incrementPosition();
            }
            for (int i = 0; i< other.position;i++) {
                buffer[position] = other.buffer[i];
                incrementPosition();
            }
		}

		@Override
		public void writeExternal(ObjectOutput out) throws IOException {
				//Need to sum up all the intermediate values before serializing
				if(position!=0){
						try {
								sum(position);
						} catch (StandardException e) {
								throw new IOException(e);
						}
						position=0;
				}
				out.writeBoolean(eliminatedNulls);
				out.writeBoolean(isNull);
				out.writeDouble(sum);
		}

		@Override
		public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
				this.eliminatedNulls = in.readBoolean();
				this.isNull = in.readBoolean();
				this.sum = in.readDouble();
		}

		@Override
		public DataValueDescriptor getResult() throws StandardException {
				if (value == null) {
						value = new SQLDouble();
				}
				if(isNull){
						value.setToNull();
						return value;
				}
				if(position!=0){
						sum(position);
						position=0;
				}
				value.setValue(sum);
				return value;
		}

		/**
		 * Can only be safely called after first calling getResult();
		 * e.g. after GenericAggregator.finish() has been called
		 * @return the current sum;
		 */
		public double getSum(){
				assert position==0: "There are entries still to be buffered!";
				return sum;
		}

		public void init(double sum,boolean eliminatedNulls){
				this.sum = sum;
				this.eliminatedNulls = eliminatedNulls;
				this.isNull = false;
		}

		@Override
		public ExecAggregator newAggregator() {
				return new DoubleBufferedSumAggregator(buffer.length);
		}

		private void sum(int bufferLength) throws StandardException {
				double newSum = sum;
				for (int i=0;i<bufferLength;i++) {
						double l = buffer[i];
						newSum += l;
				}
				//normalize the sum to ensure it remains valid
				sum = NumberDataType.normalizeDOUBLE(newSum);
		}

		private void incrementPosition() throws StandardException {
				isNull = false;
				position = (position+1) & length;
				if(position==0){
						sum(buffer.length);
				}
		}

		public SumAggregator upgrade() throws StandardException {
				DecimalBufferedSumAggregator agg = new DecimalBufferedSumAggregator(buffer.length);
				agg.init(BigDecimal.valueOf(sum),eliminatedNulls);
				for(int i=0;i<position;i++){
						agg.addDirect(BigDecimal.valueOf(buffer[i]));
				}
				return agg;
		}

      public String toString() {
         String bufferInfo = isNull ? null : (position < 25 && position > 0 ? 
                                                Arrays.toString(Arrays.copyOfRange(buffer, 0, position))
                                                : String.format("%s buffered", position));
         return "DoubleBufferedSumAggregator: " + (isNull ? "NULL" : 
                                                   String.format("{ sum=%s buffer=%s }", sum, bufferInfo));
      }

}

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

package com.splicemachine.stats;

import com.splicemachine.encoding.Encoder;
import com.splicemachine.stats.cardinality.CardinalityEstimator;
import com.splicemachine.stats.cardinality.CardinalityEstimators;
import com.splicemachine.stats.cardinality.DoubleCardinalityEstimator;
import com.splicemachine.stats.estimate.Distribution;
import com.splicemachine.stats.estimate.UniformDoubleDistribution;
import com.splicemachine.stats.frequency.DoubleFrequentElements;
import com.splicemachine.stats.frequency.FrequencyCounters;
import com.splicemachine.stats.frequency.FrequentElements;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * @author Scott Fines
 *         Date: 2/23/15
 */
public class DoubleColumnStatistics extends BaseColumnStatistics<Double> {
    private DoubleCardinalityEstimator cardinalityEstimator;
    private DoubleFrequentElements frequentElements;
    private double min;
    private double max;
    private Distribution<Double> distribution;

    public DoubleColumnStatistics(int columnId,
                                  DoubleCardinalityEstimator cardinalityEstimator,
                                  DoubleFrequentElements frequentElements,
                                  double min,
                                  double max,
                                  long totalBytes,
                                  long totalCount,
                                  long nullCount,
                                  long minCount) {
        super(columnId, totalBytes, totalCount, nullCount,minCount);
        this.cardinalityEstimator = cardinalityEstimator;
        this.frequentElements = frequentElements;
        this.min = min;
        this.max = max;
        this.distribution = new UniformDoubleDistribution(this);
    }


    @Override public long cardinality() { return cardinalityEstimator.getEstimate(); }
    @Override public FrequentElements<Double> topK() { return frequentElements; }
    @Override public Double minValue() { return min; }
    @Override public Double maxValue() { return max; }
    public double min(){ return min; }
    public double max(){ return max; }

    @Override
    public Distribution<Double> getDistribution() {
        return distribution;
    }

    @Override
    public ColumnStatistics<Double> getClone() {
        return new DoubleColumnStatistics(columnId,cardinalityEstimator.newCopy(),
                frequentElements.newCopy(),
                min,
                max,
                totalBytes,
                totalCount,
                nullCount,
                minCount);
    }

    @Override
    public CardinalityEstimator getCardinalityEstimator() {
        return cardinalityEstimator;
    }

    @Override
    public ColumnStatistics<Double> merge(ColumnStatistics<Double> other) {
        assert other.getCardinalityEstimator() instanceof DoubleCardinalityEstimator: "Cannot merge statistics of type "+ other.getClass();
        cardinalityEstimator = cardinalityEstimator.merge((DoubleCardinalityEstimator)other.getCardinalityEstimator());
        frequentElements = (DoubleFrequentElements)frequentElements.merge(other.topK());
        if(other.minValue()<min)
            min = other.minValue();
        if(other.maxValue()>max)
            max = other.maxValue();
        totalBytes+=other.totalBytes();
        totalCount+=other.nullCount()+nonNullCount();
        nullCount+=other.nullCount();
        return this;
    }

    public static Encoder<DoubleColumnStatistics> encoder(){
        return EncDec.INSTANCE;
    }

    static class EncDec implements Encoder<DoubleColumnStatistics> {
        public static final EncDec INSTANCE = new EncDec();

        @Override
        public void encode(DoubleColumnStatistics item,DataOutput encoder) throws IOException {
            BaseColumnStatistics.write(item,encoder);
            encoder.writeDouble(item.min);
            encoder.writeDouble(item.max);
            CardinalityEstimators.doubleEncoder().encode(item.cardinalityEstimator, encoder);
            FrequencyCounters.doubleEncoder().encode(item.frequentElements,encoder);
        }

        @Override
        public DoubleColumnStatistics decode(DataInput decoder) throws IOException {
            int columnId = decoder.readInt();
            long totalBytes = decoder.readLong();
            long totalCount = decoder.readLong();
            long nullCount = decoder.readLong();
            long minCount = decoder.readLong();
            double min = decoder.readDouble();
            double max = decoder.readDouble();
            DoubleCardinalityEstimator cardinalityEstimator = CardinalityEstimators.doubleEncoder().decode(decoder);
            DoubleFrequentElements frequentElements = FrequencyCounters.doubleEncoder().decode(decoder);
            return new DoubleColumnStatistics(columnId,cardinalityEstimator,frequentElements,min,max,totalBytes,totalCount,nullCount,minCount);
        }
    }
}


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
import com.splicemachine.stats.cardinality.ShortCardinalityEstimator;
import com.splicemachine.stats.estimate.Distribution;
import com.splicemachine.stats.estimate.UniformShortDistribution;
import com.splicemachine.stats.frequency.FrequencyCounters;
import com.splicemachine.stats.frequency.FrequentElements;
import com.splicemachine.stats.frequency.ShortFrequentElements;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * @author Scott Fines
 *         Date: 2/23/15
 */
public class CombinedShortColumnStatistics extends BaseColumnStatistics<Short> implements ShortColumnStatistics {

    private ShortCardinalityEstimator cardinalityEstimator;
    private ShortFrequentElements frequentElements;
    private short min;
    private short max;
    private Distribution<Short> distribution;


    public CombinedShortColumnStatistics(int columnId,
                                         ShortCardinalityEstimator cardinalityEstimator,
                                         ShortFrequentElements frequentElements,
                                         short min,
                                         short max,
                                         long totalBytes,
                                         long totalCount,
                                         long nullCount,
                                         long minCount) {
        super(columnId, totalBytes, totalCount, nullCount,minCount);
        this.cardinalityEstimator = cardinalityEstimator;
        this.frequentElements = frequentElements;
        this.min = min;
        this.max = max;
        this.distribution = new UniformShortDistribution(this);
    }

    @Override public long cardinality() { return cardinalityEstimator.getEstimate(); }
    @Override public FrequentElements<Short> topK() { return frequentElements; }
    @Override public ShortFrequentElements frequentElements() { return frequentElements; }

    @Override public Short minValue() { return min; }
    @Override public Short maxValue() { return max; }
    @Override
    public short min(){ return min; }
    @Override
    public short max(){ return max; }

    @Override
    public ColumnStatistics<Short> getClone() {
        return new CombinedShortColumnStatistics(columnId,cardinalityEstimator.newCopy(),
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
    public ColumnStatistics<Short> merge(ColumnStatistics<Short> other) {
        assert other.getCardinalityEstimator() instanceof ShortCardinalityEstimator : "Cannot merge statistics of type "+ other.getClass();

        cardinalityEstimator = cardinalityEstimator.merge((ShortCardinalityEstimator)other.getCardinalityEstimator());
        frequentElements = (ShortFrequentElements)frequentElements.merge(other.topK());
        if(other.minValue()<min)
            min = other.minValue();
        if(other.maxValue()>max)
            max = other.maxValue();
        totalBytes+=other.totalBytes();
        totalCount+=other.nullCount()+other.nonNullCount();
        nullCount+=other.nullCount();
        return this;
    }

    @Override
    public Distribution<Short> getDistribution() {
        return distribution;
    }

    @SuppressWarnings("unchecked")
    public static <T extends ShortColumnStatistics> Encoder<T> encoder(){
        return (Encoder<T>) EncDec.INSTANCE;
    }

    static class EncDec implements Encoder<CombinedShortColumnStatistics> {
        public static final EncDec INSTANCE = new EncDec();

        @Override
        public void encode(CombinedShortColumnStatistics item,DataOutput encoder) throws IOException {
            BaseColumnStatistics.write(item, encoder);
            encoder.writeShort(item.min);
            encoder.writeShort(item.max);
            CardinalityEstimators.shortEncoder().encode(item.cardinalityEstimator, encoder);
            FrequencyCounters.shortEncoder().encode(item.frequentElements,encoder);
        }

        @Override
        public CombinedShortColumnStatistics decode(DataInput decoder) throws IOException {
            int colId = decoder.readInt();
            long totalBytes = decoder.readLong();
            long totalCount = decoder.readLong();
            long nullCount = decoder.readLong();
            long minCount = decoder.readLong();
            short min = decoder.readShort();
            short max = decoder.readShort();
            ShortCardinalityEstimator cardinalityEstimator = CardinalityEstimators.shortEncoder().decode(decoder);
            ShortFrequentElements frequentElements = FrequencyCounters.shortEncoder().decode(decoder);
            return new CombinedShortColumnStatistics(colId,cardinalityEstimator,frequentElements,min,max,totalBytes,totalCount,nullCount,minCount);
        }
    }
}

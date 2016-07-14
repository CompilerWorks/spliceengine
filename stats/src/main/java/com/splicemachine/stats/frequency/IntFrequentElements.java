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

package com.splicemachine.stats.frequency;

import org.sparkproject.guava.primitives.Ints;
import org.sparkproject.guava.primitives.Longs;
import com.splicemachine.encoding.Encoder;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;

/**
 * @author Scott Fines
 *         Date: 12/5/14
 */
public abstract class IntFrequentElements implements FrequentElements<Integer> {
    private static final Comparator<IntFrequencyEstimate> naturalComparator = new Comparator<IntFrequencyEstimate>() {
        @Override
        public int compare(IntFrequencyEstimate o1, IntFrequencyEstimate o2) {
            int compare = Ints.compare(o1.value(), o2.value());
            if(compare!=0) return compare;
            return Long.signum(o1.count()-o2.count()); //should never happen, but just in case
        }
    };

    private NavigableSet<IntFrequencyEstimate> elements;
    private long totalCount;

    private IntFrequentElements(long totalCount,Collection<IntFrequencyEstimate> elements) {
        TreeSet<IntFrequencyEstimate> elems = new TreeSet<>(naturalComparator);
        elems.addAll(elements);
        this.elements = elems;
        this.totalCount = totalCount;
    }

    public static IntFrequentElements topK(int k, long totalCount,Collection<IntFrequencyEstimate> elements){
        return new TopK(k,totalCount,elements);
    }

    public static IntFrequentElements heavyHitters(float support, long totalCount,Collection<IntFrequencyEstimate> elements){
        return new HeavyItems(support,totalCount,elements);
    }

    public IntFrequencyEstimate countEqual(int item){
        for(IntFrequencyEstimate est:elements){
            if(est.value()==item) return est;
        }
        return new ZeroFreq(item);
    }

    @Override
    public Set<? extends FrequencyEstimate<Integer>> allFrequentElements() {
        return Collections.unmodifiableSet(elements);
    }

    public Set<IntFrequencyEstimate> frequentBetween(int start, int stop, boolean includeStart, boolean includeStop){
        IntFrequencyEstimate first = elements.first();
        if(first.value()>stop) return Collections.emptySet();
        else if(first.value()==stop){
            if(includeStop) return Collections.singleton(first);
            else return Collections.emptySet();
        }else if(first.value()>start || (includeStart && first.value()==start))
            return frequentBefore(stop,includeStop);

        IntFrequencyEstimate last = elements.last();
        if(last.value()<start) return Collections.emptySet();
        else if(last.value()==start){
            if(includeStart) return Collections.singleton(last);
            else return Collections.emptySet();
        }else if(last.value()<stop || (includeStop && last.value()==stop)) return frequentAfter(start,includeStart);

        ZeroFreq est = new ZeroFreq(start);
        IntFrequencyEstimate rangeStart = elements.ceiling(est);
        est.item = stop;
        IntFrequencyEstimate rangeStop = elements.ceiling(est);
        return Collections.unmodifiableSet(elements.subSet(rangeStart,includeStart,rangeStop,includeStop));
    }

    public Set<IntFrequencyEstimate> frequentAfter(int start, boolean includeStart){
        IntFrequencyEstimate first = elements.first();
        if(first.value()>start) return elements;
        else if(first.value()==start){
            if(includeStart) return elements;
            else return elements.tailSet(first,false);
        }
        IntFrequencyEstimate last = elements.last();
        if(last.value()<start) return Collections.emptySet();
        else if(last.value()==start){
            if(includeStart) return Collections.singleton(last);
            else return Collections.emptySet();
        }

        //find the starting value
        IntFrequencyEstimate rangeStart = elements.ceiling(new ZeroFreq(start));
        return Collections.unmodifiableSet(elements.tailSet(rangeStart,includeStart));
    }

    public Set<IntFrequencyEstimate> frequentBefore(int stop, boolean includeStop){
        IntFrequencyEstimate first = elements.first();
        if(first.value()>stop)return Collections.emptySet();
        else if(first.value()==stop){
            if(!includeStop) return Collections.emptySet();
            else return Collections.singleton(first);
        }
        IntFrequencyEstimate last = elements.last();
        if(last.value()<stop) return elements;
        else if(last.value()==stop){
            if(includeStop) return elements;
            else return elements.headSet(last);
        }

        IntFrequencyEstimate rangeStop = elements.higher(new ZeroFreq(stop));
        return Collections.unmodifiableSet(elements.headSet(rangeStop,includeStop));
    }

    @Override
    public FrequencyEstimate<? extends Integer> equal(Integer item) {
        assert item!=null: "Cannot estimate null matches!";
        return countEqual(item);
    }

    @Override
    public Set<? extends FrequencyEstimate<Integer>> frequentElementsBetween(Integer start, Integer stop, boolean includeMin, boolean includeStop) {
        if(start==null){
            if(stop==null){
                return elements;
            }else
                return frequentBefore(stop,includeStop);
        }else if(stop==null){
            return frequentAfter(start,includeMin);
        }else{
            if(start.equals(stop)){
                if(includeMin||includeStop) return Collections.singleton(countEqual(start));
                else return Collections.emptySet();
            }else
                return frequentBetween(start,stop,includeMin,includeStop);
        }
    }

    @Override
    public FrequentElements<Integer> merge(FrequentElements<Integer> other) {
        assert other instanceof IntFrequentElements: "Cannot merge instance of type "+ other.getClass();
        return merge((IntFrequentElements)other);
    }

//    @Override
    public IntFrequentElements merge(IntFrequentElements other) {
        NavigableSet<IntFrequencyEstimate> merged = this.elements;
        NavigableSet<IntFrequencyEstimate> otherEstimates = other.elements;
        /*
         * We need to pick out *maxSize* most common elements from both sets. Since this
         * is relatively small data, we will maintain a set in occurrance order, and add everything
         * from both sets, merging as we go. In order to effectively do that, we use a simple array,
         * add everything in, then add back in the number of elements that we need.
         */
        int totalSize = merged.size()+otherEstimates.size();
        IntFrequencyEstimate[] topK = new IntFrequencyEstimate[totalSize]; //assume no intersection
        int size =0;
        for(IntFrequencyEstimate estimate:merged){
            topK[size] = estimate;
            size++;
        }
        for(IntFrequencyEstimate otherEst:otherEstimates){
            boolean found = false;
            for(int i=0;i<size;i++){
                IntFrequencyEstimate existingEst = topK[i];
                if(existingEst.equals(otherEst)){
                    topK[i] = (IntFrequencyEstimate)existingEst.merge(otherEst);
                    found=true;
                    break;
                }
            }
            if(!found) {
                topK[size] = new IntValueEstimate(otherEst.value(),otherEst.count(),otherEst.error());
                size++;
            }
        }
        this.totalCount+=other.totalCount;
        this.elements = rebuild(totalCount,topK,size);

        return this;
    }

    public Encoder<IntFrequentElements> encoder(){
        return EncoderDecoder.INSTANCE;
    }

    @Override public FrequentElements<Integer> getClone() { return newCopy(); }

    public IntFrequentElements newCopy() {
        Collection<IntFrequencyEstimate> ests = new TreeSet<>(naturalComparator);
        for(IntFrequencyEstimate est:elements){
            ests.add(new IntValueEstimate(est.value(),est.count(),est.error()));
        }
        return getNew(totalCount,ests);
    }

    @Override
    public long totalFrequentElements() {
        long total = 0l;
        for(FrequencyEstimate<Integer> est:elements)
            total+=est.count();

        return total;
    }

    protected abstract IntFrequentElements getNew(long totalCount, Collection<IntFrequencyEstimate> ests);

    protected abstract NavigableSet<IntFrequencyEstimate> rebuild(long mergedCount,IntFrequencyEstimate[] topK,int topKSize);

    /* ****************************************************************************************************************/
    /*private helper methods*/
    private static class TopK extends IntFrequentElements{
        private final int k;
        private static final Comparator<? super IntFrequencyEstimate> frequencyComparator = new Comparator<IntFrequencyEstimate>() {
            @Override
            public int compare(IntFrequencyEstimate o1, IntFrequencyEstimate o2) {
                if(o1==null){
                    if(o2==null) return 0;
                    return 1; //sort nulls last
                }else if(o2==null) return -1; //sort nulls last

                int compare = Longs.compare(o1.count(), o2.count());
                return -1*compare;
            }
        };

        private TopK(int k,long totalCount, Collection<IntFrequencyEstimate> elements) {
            super(totalCount, elements);
            this.k = k;
        }


        @Override
        protected NavigableSet<IntFrequencyEstimate> rebuild(long mergedCount, IntFrequencyEstimate[] topK,int topKSize) {
            Arrays.sort(topK,0,topKSize,frequencyComparator);
            int k = Math.min(this.k,topKSize);
            NavigableSet<IntFrequencyEstimate> newElements = new TreeSet<>(naturalComparator);
            //noinspection ManualArrayToCollectionCopy
            for(int i=0;i<k;i++){
                newElements.add(topK[i]);
            }
            return newElements;
        }

        @Override
        protected IntFrequentElements getNew(long totalCount, Collection<IntFrequencyEstimate> ests) {
            return new TopK(k,totalCount,ests);
        }
    }

    private static class HeavyItems extends IntFrequentElements{
        private final float support;
        private HeavyItems(float support,long totalCount, Collection<IntFrequencyEstimate> elements) {
            super(totalCount, elements);
            this.support = support;
        }

        @Override
        protected NavigableSet<IntFrequencyEstimate> rebuild(long mergedCount, IntFrequencyEstimate[] topK,int topKSize) {
            NavigableSet<IntFrequencyEstimate> result = new TreeSet<>(naturalComparator);
            long threshold = (long)(mergedCount*support);
            //noinspection ForLoopReplaceableByForEach
            for(int i=0;i< topKSize;i++){
                IntFrequencyEstimate est = topK[i];
                if(est.count()>threshold)
                    result.add(est);
            }
            return result;
        }

        @Override
        protected IntFrequentElements getNew(long totalCount, Collection<IntFrequencyEstimate> ests) {
            return new HeavyItems(support,totalCount,ests);
        }
    }

    private class ZeroFreq implements IntFrequencyEstimate {
        private int item;

        public ZeroFreq(int item) {
            this.item = item;
        }

        @Override public int value() { return item; }
        @SuppressWarnings("NullableProblems")
        @Override public int compareTo(IntFrequencyEstimate o) { return Ints.compare(item, o.value()); }
        @Override public Integer getValue() { return item; }
        @Override public long count() { return 0; }
        @Override public long error() { return 0; }

        @Override public FrequencyEstimate<Integer> merge(FrequencyEstimate<Integer> other) { return other; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof IntFrequencyEstimate)) return false;

            IntFrequencyEstimate zeroFreq = (IntFrequencyEstimate) o;

            return item == zeroFreq.value();
        }

        @Override public int hashCode() { return item; }

        @Override
        public String toString() {
            return "("+item+",0,0)";
        }
    }

    static class EncoderDecoder implements Encoder<IntFrequentElements> {
        public static final EncoderDecoder INSTANCE = new EncoderDecoder(); //singleton pattern

        @Override
        public void encode(IntFrequentElements item, DataOutput dataInput) throws IOException {
            dataInput.writeLong(item.totalCount);
            encodeSet(item.elements,dataInput);
            if(item instanceof TopK) {
                dataInput.writeBoolean(true);
                dataInput.writeInt(((TopK)item).k);
            }else {
                dataInput.writeBoolean(false);
                dataInput.writeFloat(((HeavyItems)item).support);
            }

        }

        @Override
        public IntFrequentElements decode(DataInput input) throws IOException {
            long totalCount = input.readLong();
            Set<IntFrequencyEstimate> estimates = decodeSet(input);
            if(input.readBoolean()){
                int k = input.readInt();
                return new TopK(k,totalCount,estimates);
            }else{
                float support = input.readFloat();
                return new HeavyItems(support,totalCount,estimates);
            }
        }

        private void encodeSet(NavigableSet<IntFrequencyEstimate> elements, DataOutput dataInput) throws IOException {
            dataInput.writeInt(elements.size());
            for(IntFrequencyEstimate element:elements){
                dataInput.writeInt(element.value());
                dataInput.writeLong(element.count());
                dataInput.writeLong(element.error());
            }
        }

        private Set<IntFrequencyEstimate> decodeSet(DataInput input) throws IOException{
            int size = input.readInt();
            Set<IntFrequencyEstimate> set = new TreeSet<>(naturalComparator);
            for(int i=0;i<size;i++){
                int v = input.readInt();
                long c = input.readLong();
                long eps = input.readLong();
                set.add(new IntValueEstimate(v,c,eps));
            }
            return set;
        }
    }
}

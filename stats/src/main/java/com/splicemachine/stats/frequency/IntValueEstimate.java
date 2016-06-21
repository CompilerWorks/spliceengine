package com.splicemachine.stats.frequency;

import org.sparkproject.guava.primitives.Ints;
import org.sparkproject.guava.primitives.Longs;

/**
 * @author Scott Fines
 *         Date: 2/24/15
 */
class IntValueEstimate implements IntFrequencyEstimate {
    private int value;
    private long count;
    private long epsilon;

    public IntValueEstimate(int v, long c, long eps) {
        this.value = v;
        this.count = c;
        this.epsilon = eps;
    }

    @Override public int value() { return value; }

    @Override
    public int compareTo(IntFrequencyEstimate o) {
        int compare = Ints.compare(value, o.value());
        if(compare!=0) return compare;
        compare = Longs.compare(count,o.count());
        if(compare!=0) return compare;
        return Longs.compare(epsilon,o.error());
    }

    @Override public Integer getValue() { return value; }
    @Override public long count() { return count; }
    @Override public long error() { return epsilon; }

    @Override
    public FrequencyEstimate<Integer> merge(FrequencyEstimate<Integer> other) {
        this.count+=other.count();
        this.epsilon+=other.error();
        return this;
    }

    @Override public String toString() { return "("+value+","+count+","+epsilon+")"; }

    @Override public int hashCode() { return value; }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof FrequencyEstimate)) return false;
        if(obj instanceof IntFrequencyEstimate)
            return ((IntFrequencyEstimate)obj).value()==value;
        Object ob = ((FrequencyEstimate) obj).getValue();
        return ob instanceof Long && (Long)ob == value;
    }
}

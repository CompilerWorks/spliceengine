package com.splicemachine.stats.histogram;

import com.carrotsearch.hppc.IntIntOpenHashMap;
import com.carrotsearch.hppc.cursors.IntIntCursor;
import com.splicemachine.stats.random.RandomGenerator;

import java.util.Iterator;
import java.util.Random;

/**
 * @author Scott Fines
 * Date: 6/3/14
 */
public class IntWaveletQuerySolverDistortionMeasure {

		public static void main(String...args){
//				int max = 16;
//        int max = 8;
//        int max = 64;
//				int max = 128;
//				int max = 256;
//				int max = 512;
//				int max =1<<10;
//				int max =1<<11;
//				int max =1<<12;
//				int max =1<<13;
				int max =1<<14;
//				int max =1<<15;
//				int max = 65536;
//				int max = Integer.MAX_VALUE;
				int numElements = 1000000;

//				List<Integer> ints = Arrays.asList(2,2,0,2,3,5,4,4);
//        List<Integer> ints = Arrays.asList(1,3,5,11,12,13,0,1);
//				List<Integer> ints = Arrays.asList(0,0,2,2,2,2,2,3,3);
//				List<Integer> ints = Arrays.asList(-8,-7,-6,-5,-4,-3,-2,-1,0,1,2,3,4,5,6,7);
//				performAnalysis(new FixedGenerator(ints.iterator()),max);

//        performAnalysis(new EnergyGenerator(numElements,new Random(0l),10,new int[]{20,15,10,17,24,30,50,80,112,90,85,95,100,105,120,133,127,110,95,85,50,44,35,27}),max);
//				performAnalysis(new UniformGenerator(numElements,max,new Random()),max);
//        GaussianGenerator generator = new GaussianGenerator(numElements,max,new Random(0l));
//        while(generator.hasNext()){
//            System.out.println(generator.next().key);
//        }
        performAnalysis(new GaussianGenerator(numElements,max,new Random(0l)),max);
		}

		protected static void performAnalysis(Iterator<IntIntPair> dataGenerator,int maxInteger) {
				printFreeMemory();
				int max = 2*maxInteger;
        if(max<maxInteger){
            //integer overflow
            max = Integer.MAX_VALUE;
        }
				IntIntOpenHashMap actualDistribution = new IntIntOpenHashMap();

        IntHaarTransform builder = IntHaarTransform.newCounter(maxInteger, .1f, 3);
				printFreeMemory();
//				IntHaarTransform builder = IntHaarTransform.newCounter(maxInteger, 0.01f, 0.1f);
//				IntHaarTransform builder = new IntHaarTransform(maxInteger);

				while(dataGenerator.hasNext()){
						IntIntPair pair = dataGenerator.next();
						actualDistribution.addTo(pair.key,pair.count);
						builder.update(pair.key,pair.count);
				}

				System.out.print("Building query solver...");
				long nt = System.nanoTime();
				IntRangeQuerySolver querySolver = builder.build(1d);//build the full thingy
				nt = System.nanoTime()-nt;
				System.out.printf("done in %f sec%n",((double)nt)/1000d/1000d);
				printFreeMemory();

//				IntLongOpenHashMap estimated = new IntLongOpenHashMap(actualDistribution.size(),0.99f);
//				IntDoubleOpenHashMap relDiffs = new IntDoubleOpenHashMap(actualDistribution.size(),0.99f);

//				long[] estimated = new long[max];
//				long[] diffs = new long[max];
//				double[] relDiffs = new double[max];
//				for(IntIntCursor entry:actualDistribution){
//						int value = entry.key;
//						int count = entry.value;
//
//						long estimate = querySolver.equal(value);
//
//						long diff = Math.abs(estimate-count);
//						double relDiff = ((double)diff)/count;
////						diffs[value+maxInteger] = diff;
//						relDiffs.put(value,relDiff);
////						relDiffs[value+maxInteger] = relDiff;
//						estimated.put(value,estimate);
//				}

//				System.out.printf("%-10s\t%10s\t%10s\t%10s\t%10s\t%10s%s%n", "element", "correct", "estimated", "rawEstimate", "diff", "relDiff","diffHist");

				double maxRelError = 0.0d;
				int maxErrorElement = 0;
				double avgRelError = 0.0d;
				double relErrorVar = 0.0d;

				int numAccurate = 0;
				int numOff = 0;
				int i=1;
				for(IntIntCursor entry:actualDistribution){
						int actual = entry.value;
						long estimate = querySolver.equal(entry.key);
						double relDiff = Math.abs((double)actual-estimate)/actual;
						if(relDiff>0d){
								numOff++;
						}else{
								numAccurate++;
						}
//						if((i&127)==0)
//						String symbol = diff<0? "-": "+";
//								System.out.printf("%-10d\t%10d\t%10d\t%10f\t%10d%10f\t%s%n",pos,actual,estimate,rawEstimate,diff,relDiff, Strings.repeat(symbol, (int) diff));

//						if(r>maxAbsError){
//								maxAbsError = diff;
//								maxAbsErrorElement = pos;
//						}
						if(relDiff>maxRelError){
								maxRelError = relDiff;
								maxErrorElement = entry.key;
						}

						double oldAvg = avgRelError;
						avgRelError += (relDiff-avgRelError)/i;
						relErrorVar += (relDiff-oldAvg)*(relDiff-avgRelError);
						i++;
				}

//				System.out.println("---");
//				System.out.printf("Num Wavelet Coefficients: %d%n",querySolver.getNumWaveletCoefficients());

//				System.out.println("---");
//				System.out.printf("Max Absolute Error: %d%n",maxAbsError);
//				System.out.printf("Element with Max Abs error: %d\t%d\t%d%n",maxAbsErrorElement,actualDistribution.get(maxAbsErrorElement),estimated[maxAbsErrorElement+maxInteger]);

				System.out.println("---");
				System.out.printf("Max Relative Error: %f%n", maxRelError);
				System.out.printf("Element with Max Rel. error: %d\t%d\t%d%n",maxErrorElement,actualDistribution.get(maxErrorElement),querySolver.equal(maxErrorElement));
				System.out.printf("Avg Relative Error: %f%n",avgRelError);
				System.out.printf("Std. Dev: %f%n",Math.sqrt(relErrorVar/(2l*maxInteger-1)));
				System.out.printf("Num Accurate: %d%n",numAccurate);
				System.out.printf("Num Off: %d%n",numOff);
				System.out.printf("Accuracy Fraction: %f%n",(double)numAccurate/(numAccurate+numOff));
				System.out.println("---");
				printFreeMemory();

//				Arrays.sort(relDiffs);
//				System.out.printf("p25 Rel. Error:%f%n", relDiffs[relDiffs.length / 4]);
//				System.out.printf("p50 Rel. Error:%f%n", relDiffs[relDiffs.length / 2]);
//				System.out.printf("p75 Rel. Error:%f%n", relDiffs[3 * relDiffs.length / 4]);
//				System.out.printf("p95 Rel. Error:%f%n",relDiffs[95*relDiffs.length/100]);
//				System.out.printf("p99 Rel. Error:%f%n",relDiffs[99*relDiffs.length/100]);

		}

		private static void printFreeMemory() {
				System.out.printf("Free Memory(mb): %d%n", Runtime.getRuntime().freeMemory() / 1024 / 1024l);
		}

		private static class IntIntPair {
				private int key;
				private int count;
		}

    private static class GaussianGenerator implements Iterator<IntIntPair>{
        private final RandomGenerator random;
        private final int maxInt;
        private final int numIterations;
        private int iterationCount = 0;
        private final int boundary;

        private IntIntPair retPair = new IntIntPair();
        private GaussianGenerator(int numIterations, int maxInt, Random random) {
            this.random = new com.splicemachine.stats.random.GaussianGenerator(new com.splicemachine.stats.random.UniformGenerator(random));
            this.numIterations = numIterations;
            int s = 2*maxInt;
            if(s<maxInt){
                boundary = Integer.MAX_VALUE;
                maxInt = boundary>>1;
            }else
                boundary = s-1;
            this.maxInt = maxInt;

        }

        @Override
        public boolean hasNext() {
            return (iterationCount++)<=numIterations;
        }

        @Override
        public IntIntPair next() {
            /*
             * random.nextDouble() generates a gaussian which is centered on 0
             * and has unit std. deviation. We want to ensure that we NEVER
             * generate numbers outside of the range (-maxInt,maxInt), so
             * we first truncate any number which >=1 (forcing us to generate
             * within the bounds [0,1). Then we scale by 2*maxInt to get to
             * [0,2*maxInt); finally, we shift down by maxInt to get to [-maxInt,maxInt)
             */
            while(true){
                double d = random.nextDouble();
                if(Math.abs(d)>=1) continue;

                d = d*(boundary);
                if(d<0)
                    retPair.key = (int)(d)+maxInt-1;
                else
                    retPair.key = (int)(d)-maxInt+1;
                retPair.count=1;
                return retPair;
            }
        }

        @Override public void remove() { throw new UnsupportedOperationException(); }

    }
		private static class UniformGenerator implements Iterator<IntIntPair>{
				private final Random random;
				private final int maxInt;
				private final int numIterations;
				private int iterationCount = 0;
        private final int boundary;

				private IntIntPair retPair = new IntIntPair();
				private UniformGenerator(int numIterations, int maxInt, Random random) {
						this.random = random;
						this.numIterations = numIterations;
            int s = 2*maxInt;
            if(s<maxInt){
                boundary = Integer.MAX_VALUE;
                maxInt = boundary>>1;
            }else
                boundary = s;
            this.maxInt = maxInt;

				}

				@Override
				public boolean hasNext() {
						return (iterationCount++)<=numIterations;
				}

				@Override
				public IntIntPair next() {
						retPair.key = random.nextInt(2*maxInt)-maxInt;
						retPair.count = 1;
						return retPair;
				}

				@Override public void remove() { throw new UnsupportedOperationException(); }
		}

    private static class EnergyGenerator implements Iterator<IntIntPair>{
        private final int numIterations;
        private int iterationCount = 0;
        private final Random jitter;
        private final int magnitude;
        private final int[] baseValues;

        private IntIntPair retPair = new IntIntPair();
        private EnergyGenerator(int numIterations, Random jitter, int magnitude, int[] baseValues) {
            this.numIterations = numIterations;
            this.jitter = jitter;
            this.magnitude = magnitude;
            this.baseValues = baseValues;
        }

        @Override
        public boolean hasNext() {
            return (iterationCount++)<=numIterations;
        }

        @Override
        public IntIntPair next() {
            int v = baseValues[iterationCount%baseValues.length];
            if(jitter.nextBoolean()){
                v-=jitter.nextInt(magnitude);
            }else
                v+=jitter.nextInt(magnitude);
            retPair.key=v;
            retPair.count=1;
            return retPair;
        }

        @Override public void remove() { throw new UnsupportedOperationException(); }
    }

		private static class FixedGenerator implements Iterator<IntIntPair>{
				private final Iterator<Integer> valuesIterator;

				private final IntIntPair retPair = new IntIntPair();
				private FixedGenerator(Iterator<Integer> valuesIterator) {
						this.valuesIterator = valuesIterator;
						this.retPair.count=1;
				}

				@Override public boolean hasNext() { return valuesIterator.hasNext(); }

				@Override
				public IntIntPair next() {
						retPair.key = valuesIterator.next();
						return retPair;
				}

				@Override public void remove() { throw new UnsupportedOperationException(); }
		}
}

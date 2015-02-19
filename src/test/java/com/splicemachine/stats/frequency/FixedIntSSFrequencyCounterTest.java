package com.splicemachine.stats.frequency;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import com.splicemachine.hash.HashFunctions;
import com.splicemachine.stats.LongPair;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Nullable;
import java.util.*;

/**
 * @author Scott Fines
 *         Date: 3/26/14
 */
@SuppressWarnings("ConstantConditions")
public class FixedIntSSFrequencyCounterTest {


		@Test
		public void testWorksWithNoEviction() throws Exception {
				//insert 10 unique elements, then pull them out
				IntFrequencyCounter spaceSaver = FrequencyCounters.intCounter(20,10);

				Map<Integer,LongPair> correctEstimates = new HashMap<Integer, LongPair>();
				for(int i=0;i<10;i++){
						long count = 1;
						spaceSaver.update(i);
						if(i%2==0){
								spaceSaver.update(i);
								count++;
						}
						correctEstimates.put(i, new LongPair(count, 0l));
				}

        IntFrequentElements intFrequentElements = spaceSaver.heavyHitters(0f);
        Assert.fail("IMPLEMENT");
//        Set<? extends FrequencyEstimate<Integer>> estimates = spaceSaver.getFrequentElements(0f);
//				Assert.assertEquals("Incorrect number of rows!", correctEstimates.size(), estimates.size());
//
//				long totalCount = 0l;
//				for(FrequencyEstimate<Integer> estimate:estimates){
//						int val = estimate.getValue();
//						long count = estimate.count();
//						long error = estimate.error();
//						totalCount+=count;
//
//						LongPair correct = correctEstimates.get(val);
//						Assert.assertNotNull("Observed entry for "+val+" not found!",correct);
//						Assert.assertEquals("Incorrect count!",correct.getFirst(),count);
//						Assert.assertEquals("Incorrect error!",correct.getSecond(),error);
//				}
//				Assert.assertEquals("Total estimated count does not equal the number of elements!",15,totalCount);
		}

		@Test
		public void testEvictsEntry() throws Exception {
				IntFrequencyCounter spaceSaver = FrequencyCounters.intCounter(2, 10);

				int element = 1;
				spaceSaver.update(element);
				spaceSaver.update(element);
				element = 2;
				spaceSaver.update(element);
				element = 3;
				spaceSaver.update(element);

				//output should be (1,1,0),(3,2,1)

        IntFrequentElements estimates = spaceSaver.heavyHitters(0f);
        Assert.fail("IMPLEMENT");
//				List<FrequencyEstimate<Integer>> estimates = Lists.newArrayList(spaceSaver.getFrequentElements(0f));
//				Collections.sort(estimates,new Comparator<FrequencyEstimate<Integer>>(){
//						@Override
//						public int compare(FrequencyEstimate<Integer> o1, FrequencyEstimate<Integer> o2) {
//								return Ints.compare(o1.getValue(), o2.getValue());
//						}
//				});
//
//				List<Integer> values = Lists.transform(estimates,new Function<FrequencyEstimate<Integer>, Integer>() {
//						@Override
//						public Integer apply(@Nullable FrequencyEstimate<Integer> input) {
//								return input.getValue();
//						}
//				});
//
//				List<Integer> correctValues = Arrays.asList(1,3);
//				Assert.assertEquals("Incorrect reported values!",correctValues,values);
//
//				List<Long> counts = Lists.transform(estimates, new Function<FrequencyEstimate<Integer>, Long>() {
//						@Override
//						public Long apply(@Nullable FrequencyEstimate<Integer> input) {
//								return input.count();
//						}
//				});
//				List<Long> correctCounts = Arrays.asList(2l,2l);
//				Assert.assertEquals("Incorrect reported counts!",correctCounts,counts);
//
//				List<Long> errors = Lists.transform(estimates, new Function<FrequencyEstimate<Integer>, Long>() {
//						@Override
//						public Long apply(@Nullable FrequencyEstimate<Integer> input) {
//								return input.error();
//						}
//				});
//
//				List<Long> correctErrors = Arrays.asList(0l, 1l);
//				Assert.assertEquals("Incorrect reported errors!",correctErrors,errors);
		}
}

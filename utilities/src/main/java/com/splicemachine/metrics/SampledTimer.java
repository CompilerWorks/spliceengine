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

package com.splicemachine.metrics;

/**
 * @author Scott Fines
 *         Date: 7/21/14
 */
public class SampledTimer implements LatencyTimer,DistributionTimeView{
    private final SampleTimeMeasure wallTimer;
//    private final SampleTimeMeasure cpuTimer;
//    private final SampleTimeMeasure userTimer;

    private long numEvents = 0l;
    public SampledTimer(int sampleSize,
                        TimeMeasure wallTimer,
                        TimeMeasure cpuTimer,
                        TimeMeasure userTimer) {
        this.wallTimer = new SampleTimeMeasure(wallTimer,sampleSize);
//        this.cpuTimer = new SampleTimeMeasure(cpuTimer,sampleSize);
//        this.userTimer = new SampleTimeMeasure(userTimer,sampleSize);
    }

    @Override public long getWallClockTime() { return wallTimer.getElapsedTime(); }
//    @Override public long getCpuTime() { return cpuTimer.getElapsedTime(); }
//    @Override public long getUserTime() { return userTimer.getElapsedTime(); }

    @Override public long getCpuTime() { return 0l; }
    @Override public long getUserTime() { return 0l; }

    @Override public long getStopWallTimestamp() { return wallTimer.getStopTimestamp(); }
    @Override public long getStartWallTimestamp() { return wallTimer.getStartTimestamp(); }
    @Override public long getNumEvents() { return numEvents; }

    @Override
    public void startTiming() {
        wallTimer.startTime();
//        cpuTimer.startTime();
//        userTimer.startTime();
    }

    @Override
    public void stopTiming() {
//        userTimer.stopTime();
//        cpuTimer.stopTime();
        wallTimer.stopTime();
    }

    @Override
    public void tick(long numEvents) {
        stopTiming();
        this.numEvents+=numEvents;
    }

    @Override public LatencyView wallLatency() { return wallTimer; }
//    @Override public LatencyView cpuLatency() { return cpuTimer; }
//    @Override public LatencyView userLatency() { return userTimer; }
    @Override public LatencyView cpuLatency() { return Metrics.noOpLatencyView(); }
    @Override public LatencyView userLatency() { return Metrics.noOpLatencyView();}
    @Override public TimeView getTime() { return this; }

    public DistributionTimeView getDistribution() { return this; }
}

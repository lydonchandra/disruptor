/*
 * Copyright 2011 LMAX Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lmax.disruptor;

import java.util.concurrent.TimeUnit;

/**
 * DependencyBarrier handed out for gating {@link EventProcessor}s of the RingBuffer and dependent {@link com.lmax.disruptor.EventProcessor}(s)
 * by sequence.
 */
final class TrackingDependencyBarrier implements DependencyBarrier
{
    private final WaitStrategy waitStrategy;
    private final Sequence cursorSequence;
    private final Sequence[] dependentSequences;
    private volatile boolean alerted = false;

    public TrackingDependencyBarrier(final WaitStrategy waitStrategy,
                                     final Sequence cursorSequence,
                                     final Sequence[] dependentSequences)
    {
        this.waitStrategy = waitStrategy;
        this.cursorSequence = cursorSequence;
        this.dependentSequences = dependentSequences;
    }

    @Override
    public long waitFor(final long sequence)
        throws AlertException, InterruptedException
    {
        return waitStrategy.waitFor(dependentSequences, cursorSequence, this, sequence);
    }

    @Override
    public long waitFor(final long sequence, final long timeout, final TimeUnit units)
        throws AlertException, InterruptedException
    {
        return waitStrategy.waitFor(dependentSequences, cursorSequence, this, sequence, timeout, units);
    }

    @Override
    public long getCursor()
    {
        return cursorSequence.get();
    }

    @Override
    public boolean isAlerted()
    {
        return alerted;
    }

    @Override
    public void alert()
    {
        alerted = true;
        waitStrategy.signalAll();
    }

    @Override
    public void clearAlert()
    {
        alerted = false;
    }
}
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

/**
 * Convenience class for handling the batching semantics of consuming entries from a {@link RingBuffer}
 * and delegating the available {@link AbstractEvent}s to a {@link EventHandler}.
 *
 * If the {@link EventHandler} also implements {@link LifecycleAware} it will be notified just after the thread
 * is started and just before the thread is shutdown.
 *
 * @param <T> {@link AbstractEvent} implementation storing the data for sharing during exchange or parallel coordination of an event.
 */
public final class BatchEventProcessor<T extends AbstractEvent>
    implements EventProcessor
{
    private volatile boolean running = true;
    private ExceptionHandler exceptionHandler = new FatalExceptionHandler();
    private final RingBuffer<T> ringBuffer;
    private final DependencyBarrier dependencyBarrier;
    private final EventHandler<T> eventHandler;
    private final Sequence sequence = new Sequence(RingBuffer.INITIAL_CURSOR_VALUE);

    /**
     * Construct a {@link EventProcessor} that will automatically track the progress by updating its sequence when
     * the {@link EventHandler#onEvent(AbstractEvent, boolean)} method returns.
     *
     * @param ringBuffer to which events are published.
     * @param dependencyBarrier on which it is waiting.
     * @param eventHandler is the delegate to which {@link AbstractEvent}s are dispatched.
     */
    public BatchEventProcessor(final RingBuffer<T> ringBuffer,
                               final DependencyBarrier dependencyBarrier,
                               final EventHandler<T> eventHandler)
    {
        this.ringBuffer = ringBuffer;
        this.dependencyBarrier = dependencyBarrier;
        this.eventHandler = eventHandler;
    }

    /**
     * Construct a batch event processor that will allow a {@link SequenceReportingEventHandler}
     * to callback and update its sequence within a batch.  The Sequence will be updated at the end of
     * a batch regardless.
     *
     * @param ringBuffer to which events are published.
     * @param dependencyBarrier on which it is waiting.
     * @param eventHandler is the delegate to which {@link AbstractEvent}s are dispatched.
     */
    public BatchEventProcessor(final RingBuffer<T> ringBuffer,
                               final DependencyBarrier dependencyBarrier,
                               final SequenceReportingEventHandler<T> eventHandler)
    {
        this.ringBuffer = ringBuffer;
        this.dependencyBarrier = dependencyBarrier;
        this.eventHandler = eventHandler;
        eventHandler.setSequenceCallback(sequence);
    }

    @Override
    public Sequence getSequence()
    {
        return sequence;
    }

    @Override
    public void halt()
    {
        running = false;
        dependencyBarrier.alert();
    }

    /**
     * Set a new {@link ExceptionHandler} for handling exceptions propagated out of the {@link BatchEventProcessor}
     *
     * @param exceptionHandler to replace the existing exceptionHandler.
     */
    public void setExceptionHandler(final ExceptionHandler exceptionHandler)
    {
        if (null == exceptionHandler)
        {
            throw new NullPointerException();
        }

        this.exceptionHandler = exceptionHandler;
    }

    /**
     * Get the {@link DependencyBarrier} the {@link EventProcessor} is waiting on.
     *
      * @return the dependencyBarrier this {@link EventProcessor} is using.
     */
    public DependencyBarrier getDependencyBarrier()
    {
        return dependencyBarrier;
    }

    /**
     * It is ok to have another thread rerun this method after a halt().
     */
    @Override
    public void run()
    {
        running = true;
        if (LifecycleAware.class.isAssignableFrom(eventHandler.getClass()))
        {
            ((LifecycleAware) eventHandler).onStart();
        }

        T event = null;
        long nextSequence = sequence.get() + 1L;
        while (true)
        {
            try
            {
                final long availableSequence = dependencyBarrier.waitFor(nextSequence);
                while (nextSequence <= availableSequence)
                {
                    event = ringBuffer.getEvent(nextSequence);
                    eventHandler.onEvent(event, nextSequence == availableSequence);
                    nextSequence++;
                }

                sequence.set(event.getSequence());
            }
            catch (final AlertException ex)
            {
               if (!running)
               {
                   break;
               }
            }
            catch (final Exception ex)
            {
                exceptionHandler.handle(ex, event);
                sequence.set(event.getSequence());
                nextSequence = event.getSequence() + 1L;
            }
        }

        if (LifecycleAware.class.isAssignableFrom(eventHandler.getClass()))
        {
            ((LifecycleAware) eventHandler).onShutdown();
        }
    }
}
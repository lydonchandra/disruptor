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
package com.lmax.disruptor.support;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import junit.framework.Assert;

import org.junit.Test;

import com.lmax.disruptor.BatchEventProcessor;
import com.lmax.disruptor.ClaimStrategy;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.ClaimStrategy.Option;
import com.lmax.disruptor.support.ValueEvent;
import com.lmax.disruptor.support.ValuePublisher;


public final class ValueMutationEventHandlerTest
{
	
	@Test
    public void testConstructor() throws InterruptedException, BrokenBarrierException, ExecutionException, TimeoutException
    {
		int numPublisher = 1;
		int numConsumer = 1;
		int parties = numPublisher + numConsumer;
		CyclicBarrier barrier = new CyclicBarrier(parties);
		
		RingBuffer<ValueEvent> ringBuffer = new RingBuffer<ValueEvent>(
				ValueEvent.EVENT_FACTORY, 8192,
				ClaimStrategy.Option.MULTI_THREADED,
				WaitStrategy.Option.YIELDING
		);
		
		int iteration = 10;
		ValuePublisher valuePublisher = new ValuePublisher(
				barrier, ringBuffer, iteration
		);
		
		ExecutorService execService = Executors.newFixedThreadPool(2);
		Future future = execService.submit(valuePublisher);
		
		ValueMutationEventHandler eventHandler = new ValueMutationEventHandler(Operation.ADDITION);
		
		BatchEventProcessor<ValueEvent> eventProcessor = new BatchEventProcessor<ValueEvent>(ringBuffer, 
				ringBuffer.newDependencyBarrier(),
				eventHandler
		);
		
		barrier.await();
		Future future2 = execService.submit(eventProcessor);
		
		Thread.sleep(1000);
		
		Assert.assertEquals(eventHandler.getValue(), 45L );
		
    }
	
}

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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import junit.framework.Assert;

import org.junit.Test;


public final class ValueQueuePublisherTest
{
	
	@Test
    public void testConstructor() throws InterruptedException, BrokenBarrierException, ExecutionException, TimeoutException
    {
		CyclicBarrier cyclicBarrier = new CyclicBarrier(2);
		BlockingQueue<Long> blockingQueue = new ArrayBlockingQueue<Long>(10);
		ValueQueuePublisher valueQueuePublisher = new ValueQueuePublisher(cyclicBarrier, blockingQueue, 10 );
		
		ExecutorService execSvc = Executors.newFixedThreadPool(2);
		execSvc.submit(valueQueuePublisher);
		
		ValueAdditionQueueProcessor processor = new ValueAdditionQueueProcessor(blockingQueue);
		cyclicBarrier.await();

		execSvc.submit(processor);
		
		Assert.assertEquals(cyclicBarrier.getNumberWaiting(), 0);
		Assert.assertEquals(processor.getValue(), 45L );
		Assert.assertEquals(processor.getSequence(), 10L );
		
		processor.reset();
		Assert.assertEquals(processor.getValue(), 0L );
		Assert.assertEquals(processor.getSequence(), -1L );
		
		processor.halt();
		
    }
	
}

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
package com.lmax.disruptor.wizard;

import com.lmax.disruptor.AbstractEvent;
import com.lmax.disruptor.BatchEventProcessor;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.ExceptionHandler;

public class ExceptionHandlerSetting<T extends AbstractEvent>
{
    private final EventHandler<T> eventHandler;
    private final EventProcessorRepository<T> eventProcessorRepository;

    ExceptionHandlerSetting(final EventHandler<T> eventHandler, EventProcessorRepository<T> eventProcessorRepository)
    {
        this.eventHandler = eventHandler;
        this.eventProcessorRepository = eventProcessorRepository;
    }

    public void with(ExceptionHandler exceptionHandler)
    {
        ((BatchEventProcessor) eventProcessorRepository.getEventProcessorFor(eventHandler)).setExceptionHandler(exceptionHandler);
        eventProcessorRepository.getBarrierFor(eventHandler).alert();
    }
}

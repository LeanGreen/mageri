/*
 * Copyright 2014-2016 Mikhail Shugay
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.antigenomics.mageri.pipeline.analysis;

import cc.redberry.pipe.OutputPort;
import com.antigenomics.mageri.core.Mig;
import com.antigenomics.mageri.core.ReadSpecific;
import com.antigenomics.mageri.core.assemble.Consensus;
import com.antigenomics.mageri.core.input.MigOutputPort;
import com.antigenomics.mageri.core.input.MigSizeDistribution;
import com.antigenomics.mageri.misc.ProcessorResultWrapper;
import com.antigenomics.mageri.preprocessing.CheckoutProcessor;

import java.io.Serializable;

public interface Preprocessor<MigType extends Mig> extends ReadSpecific, Serializable {

    MigSizeDistribution getUmiHistogram(Sample sample);

    MigOutputPort<MigType> create(Sample sample);

    OutputPort<ProcessorResultWrapper<Consensus>> createRaw(Sample sample);

    int getOverSeq(String sampleName);

    SampleGroup getSampleGroup();

    CheckoutProcessor getCheckoutProcessor();

}

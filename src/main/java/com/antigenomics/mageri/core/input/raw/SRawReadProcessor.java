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

package com.antigenomics.mageri.core.input.raw;

import cc.redberry.pipe.Processor;
import com.antigenomics.mageri.core.input.PreprocessorParameters;
import com.antigenomics.mageri.core.input.SMigReader;
import com.antigenomics.mageri.core.input.index.*;
import com.antigenomics.mageri.preprocessing.CheckoutProcessor;
import com.antigenomics.mageri.preprocessing.CheckoutResult;
import com.milaboratory.core.sequencing.read.SequencingRead;

public class SRawReadProcessor implements Processor<SequencingRead, IndexedReadContainer> {
    private final CheckoutProcessor checkoutProcessor;
    private final PreprocessorParameters preprocessorParameters;

    public SRawReadProcessor(CheckoutProcessor checkoutProcessor,
                             PreprocessorParameters preprocessorParameters) {
        this.checkoutProcessor = checkoutProcessor;
        this.preprocessorParameters = preprocessorParameters;
    }

    @Override
    @SuppressWarnings("unchecked")
    public IndexedReadContainer process(SequencingRead milibRead) {
        if (milibRead == null) {
            return null;
        }

        CheckoutResult result = checkoutProcessor.checkout(milibRead);

        if (result != null) {
            String sampleName = result.getSampleName();

            Read read = SMigReader.groom(new RawRead(milibRead.getData(0)),
                    result, preprocessorParameters.trimAdapters());

            return new IndexedReadContainer(sampleName, new SingleReadContainer(read));
        }

        return new IndexedReadContainer(null, null);
    }
}

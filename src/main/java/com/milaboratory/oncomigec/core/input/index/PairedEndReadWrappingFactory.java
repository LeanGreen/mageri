/*
 * Copyright 2013-2015 Mikhail Shugay (mikhail.shugay@gmail.com)
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
 *
 * Last modified on 10.4.2015 by mikesh
 */

package com.milaboratory.oncomigec.core.input.index;

import com.milaboratory.core.sequence.NucleotideSQPair;
import com.milaboratory.core.sequencing.read.PSequencingRead;

public class PairedEndReadWrappingFactory extends ReadWrappingFactory<PSequencingRead> {
    public PairedEndReadWrappingFactory(QualityProvider qualityProvider) {
        super(qualityProvider);
    }

    @Override
    public ReadContainer wrap(PSequencingRead pSequencingRead) {
        NucleotideSQPair data1 = pSequencingRead.getData(0),
                data2 = pSequencingRead.getData(1);
        return new PairedReadContainer(
                new Read(data1.getSequence(),
                        qualityProvider.convert(data1.getQuality())),
                new Read(data2.getSequence(),
                        qualityProvider.convert(data2.getQuality()))
        );
    }
}

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
 * Last modified on 13.3.2015 by mikesh
 */

package com.milaboratory.oncomigec.pipeline.analysis;

import com.milaboratory.oncomigec.ReadSpecific;
import com.milaboratory.oncomigec.core.io.entity.Mig;
import com.milaboratory.oncomigec.core.io.misc.UmiHistogram;
import com.milaboratory.oncomigec.core.io.readers.MigOutputPort;
import com.milaboratory.oncomigec.core.io.readers.MigReader;
import com.milaboratory.oncomigec.core.io.readers.PMigReader;
import com.milaboratory.oncomigec.core.io.readers.SMigReader;
import com.milaboratory.oncomigec.pipeline.Presets;
import com.milaboratory.oncomigec.pipeline.input.InputChunk;

import java.io.IOException;

public class Preprocessor<MigType extends Mig> implements ReadSpecific {
    private final Presets presets;
    private final SampleGroup sampleGroup;
    private final MigReader migReader;

    public Preprocessor(Presets presets, SampleGroup sampleGroup) throws IOException, InterruptedException {
        this.presets = presets;
        this.sampleGroup = sampleGroup;
        InputChunk inputChunk = sampleGroup.getInputChunk();
        this.migReader = inputChunk.isPairedEnd() ?
                new PMigReader(inputChunk.getInputStream1(), inputChunk.getInputStream2(),
                        inputChunk.getCheckoutRule().getProcessor())
                :
                new SMigReader(inputChunk.getInputStream1(),
                        inputChunk.getCheckoutRule().getProcessor());

        if (presets.filterMismatchUmis())
            migReader.setMinMismatchRatio(presets.getUmiMismatchFilterRatio());
    }

    public UmiHistogram getUmiHistogram(Sample sample) {
        if (!sampleGroup.getSamples().contains(sample))
            throw new RuntimeException("Sample " + sample + " not found in sample group " + sampleGroup);

        return migReader.getUmiHistogram(sample.getName());
    }

    @SuppressWarnings("unchecked")
    public MigOutputPort<MigType> create(Sample sample) {
        if (!sampleGroup.getSamples().contains(sample))
            throw new RuntimeException("Sample " + sample + " not found in sample group " + sampleGroup);

        String sampleName = sample.getName();

        return new MigOutputPort<>(migReader, sampleName, getOverSeq(sampleName));
    }

    public int getOverSeq(String sampleName) {
        return presets.forceOverseq() ? presets.getDefaultOverseq() :
                migReader.getUmiHistogram(sampleName).getMigSizeThreshold();
    }

    @Override
    public boolean isPairedEnd() {
        return migReader.isPairedEnd();
    }
}

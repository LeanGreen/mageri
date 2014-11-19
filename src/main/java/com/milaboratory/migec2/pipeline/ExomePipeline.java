/*
 * Copyright 2014 Mikhail Shugay (mikhail.shugay@gmail.com)
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
package com.milaboratory.migec2.pipeline;

import com.milaboratory.migec2.core.align.processor.AlignerFactoryWithReference;
import com.milaboratory.migec2.core.align.processor.aligners.ExtendedExomeAlignerFactory;
import com.milaboratory.migec2.core.align.processor.aligners.SimpleExomeAlignerFactory;
import com.milaboratory.migec2.core.align.reference.ReferenceLibrary;
import com.milaboratory.migec2.core.assemble.misc.AssemblerFactory;
import com.milaboratory.migec2.core.assemble.misc.PAssemblerFactory;
import com.milaboratory.migec2.core.assemble.misc.SAssemblerFactory;
import com.milaboratory.migec2.core.consalign.misc.ConsensusAlignerFactory;
import com.milaboratory.migec2.core.consalign.misc.PConsensusAlignerFactory;
import com.milaboratory.migec2.core.consalign.misc.SConsensusAlignerFactory;
import com.milaboratory.migec2.core.io.misc.MigReaderParameters;
import com.milaboratory.migec2.core.io.readers.MigReader;
import com.milaboratory.migec2.core.io.readers.PMigReader;
import com.milaboratory.migec2.core.io.readers.SMigReader;
import com.milaboratory.migec2.preproc.demultiplex.config.BarcodeListParser;
import org.apache.commons.io.FileUtils;

import java.io.File;

// todo: barcode matcher options
public final class ExomePipeline extends MigecPipeline {
    private ExomePipeline(MigReader reader,
                          AssemblerFactory assemblerFactory,
                          ConsensusAlignerFactory consensusAlignerFactory,
                          MigecParameterSet migecParameterSet) {
        super(reader, assemblerFactory, consensusAlignerFactory, migecParameterSet);
    }

    public static ExomePipeline preprocess(File fastq1, File fastq2,
                                           String sampleName,
                                           File references,
                                           MigecParameterSet migecParameterSet) throws Exception {
        return new ExomePipeline(
                new PMigReader(fastq1, fastq2, sampleName,
                        MigReaderParameters.WITH_QUAL(migecParameterSet.getReaderUmiQualThreshold())),
                new PAssemblerFactory(migecParameterSet.getAssemblerParameters()),
                new PConsensusAlignerFactory(
                        new AlignerFactoryWithReference(new ReferenceLibrary(references),
                                new ExtendedExomeAlignerFactory())
                ),
                migecParameterSet
        );
    }

    public static ExomePipeline preprocess(File fastq1, File fastq2,
                                           File barcodes,
                                           File references,
                                           MigecParameterSet migecParameterSet) throws Exception {
        return new ExomePipeline(
                new PMigReader(fastq1, fastq2,
                        BarcodeListParser.generatePCheckoutProcessor(FileUtils.readLines(barcodes),
                                migecParameterSet.getDemultiplexParameters()),
                        MigReaderParameters.WITH_QUAL(migecParameterSet.getReaderUmiQualThreshold())
                ),
                new PAssemblerFactory(migecParameterSet.getAssemblerParameters()),
                new PConsensusAlignerFactory(
                        new AlignerFactoryWithReference(new ReferenceLibrary(references),
                                new ExtendedExomeAlignerFactory()),
                        migecParameterSet.getConsensusAlignerParameters()
                ),
                migecParameterSet
        );
    }

    public static ExomePipeline preprocess(File fastq1,
                                           String sampleName,
                                           File references,
                                           MigecParameterSet migecParameterSet) throws Exception {
        return new ExomePipeline(
                new SMigReader(fastq1, sampleName,
                        MigReaderParameters.WITH_QUAL(migecParameterSet.getReaderUmiQualThreshold())),
                new SAssemblerFactory(migecParameterSet.getAssemblerParameters()),
                new SConsensusAlignerFactory(
                        new AlignerFactoryWithReference(new ReferenceLibrary(references),
                                new ExtendedExomeAlignerFactory())
                ),
                migecParameterSet
        );
    }

    public static ExomePipeline preprocess(File fastq1,
                                           File barcodes,
                                           File references,
                                           MigecParameterSet migecParameterSet) throws Exception {
        return new ExomePipeline(
                new SMigReader(fastq1,
                        BarcodeListParser.generateSCheckoutProcessor(FileUtils.readLines(barcodes),
                                migecParameterSet.getDemultiplexParameters()),
                        MigReaderParameters.WITH_QUAL(migecParameterSet.getReaderUmiQualThreshold())
                ),
                new SAssemblerFactory(migecParameterSet.getAssemblerParameters()),
                new SConsensusAlignerFactory(
                        new AlignerFactoryWithReference(new ReferenceLibrary(references),
                                new ExtendedExomeAlignerFactory())
                ),
                migecParameterSet
        );
    }
}

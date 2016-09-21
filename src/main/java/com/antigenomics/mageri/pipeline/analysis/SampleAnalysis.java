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
import cc.redberry.pipe.blocks.Merger;
import cc.redberry.pipe.blocks.ParallelProcessor;
import cc.redberry.pipe.util.CountingOutputPort;
import com.antigenomics.mageri.core.Mig;
import com.antigenomics.mageri.core.ReadSpecific;
import com.antigenomics.mageri.core.assemble.Assembler;
import com.antigenomics.mageri.core.assemble.Consensus;
import com.antigenomics.mageri.core.input.MigOutputPort;
import com.antigenomics.mageri.core.input.MigSizeDistribution;
import com.antigenomics.mageri.core.mapping.AlignedConsensus;
import com.antigenomics.mageri.core.mapping.ConsensusAligner;
import com.antigenomics.mageri.core.variant.VariantCaller;
import com.antigenomics.mageri.pipeline.Speaker;
import com.antigenomics.mageri.misc.ProcessorResultWrapper;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

public class SampleAnalysis implements ReadSpecific, Serializable {
    protected final boolean paired;

    protected final ProjectAnalysis parent;
    protected final Sample sample;

    protected transient final MigOutputPort reader;

    protected final MigSizeDistribution migSizeDistribution;
    protected final Assembler assembler;
    protected final ConsensusAligner consensusAligner;
    protected VariantCaller variantCaller;

    protected boolean ran = false;

    private final List<AlignedConsensus> alignmentDataList = new LinkedList<>();

    @SuppressWarnings("unchecked")
    protected SampleAnalysis(ProjectAnalysis parent,
                             Sample sample,
                             MigSizeDistribution migSizeDistribution,
                             MigOutputPort reader,
                             Assembler assembler,
                             ConsensusAligner consensusAligner) {
        this.parent = parent;
        this.migSizeDistribution = migSizeDistribution;
        this.sample = sample;
        this.reader = reader;
        this.paired = reader.isPairedEnd();

        if (assembler.isPairedEnd() != paired ||
                consensusAligner.isPairedEnd() != paired)
            throw new RuntimeException("All read-specific pipeline steps should have the same paired-end property.");

        this.assembler = assembler;
        this.consensusAligner = consensusAligner;
    }

    private void sout(String message, int verbosityLevel) {
        Speaker.INSTANCE.sout("[" + sample.getFullName() + "] " +
                message, verbosityLevel);
    }

    @SuppressWarnings("unchecked")
    public void run() throws Exception {
        if (ran) {
            return;
        }

        String outputPrefix = getOutputPrefix();

        OutputPort<Mig> input = reader;

        final Merger<Mig> bufferedInput = new Merger<>(524288);
        bufferedInput.merge(input);
        bufferedInput.start();
        input = bufferedInput;

        final CountingOutputPort<Mig> countingInput = new CountingOutputPort<>(input);

        Thread reporter = new Thread(new Runnable() {
            long prevCount = -1;

            @Override
            public void run() {
                try {
                    while (!countingInput.isClosed()) {
                        long count = countingInput.getCount();
                        if (prevCount != count) {
                            sout("Assembling & aligning consensuses, " + count + " MIGs processed..", 2);
                            prevCount = count;
                        }
                        Thread.sleep(10000);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        reporter.setDaemon(true);
        reporter.start();

        // Assemble & align in parallel
        final OutputPort<ProcessorResultWrapper<Consensus>> assemblyResults =
                new ParallelProcessor<>(countingInput, assembler, parent.getRuntimeParameters().getNumberOfThreads());

        final OutputPort<ProcessorResultWrapper<AlignedConsensus>> alignerResults =
                new ParallelProcessor<>(assemblyResults, consensusAligner, parent.getRuntimeParameters().getNumberOfThreads());

        ProcessorResultWrapper<AlignedConsensus> alignmentDataWrapped;
        while ((alignmentDataWrapped = alignerResults.take()) != null) {
            if (alignmentDataWrapped.hasResult()) {
                alignmentDataList.add(alignmentDataWrapped.getResult());
            }
        }

        // Write plain-text and consensus FASTQ files
        // Write consensus aligner output now, as it will be cleared upon creation of VariantCaller
        if (outputPrefix != null) {
            migSizeDistribution.writePlainText(outputPrefix);
            assembler.writePlainText(outputPrefix);
            consensusAligner.writePlainText(outputPrefix);
        }

        assembler.clear();

        sout("Finished, " + countingInput.getCount() + " MIGs processed in total.", 1);

        sout("Calling variants.", 1);

        this.variantCaller = new VariantCaller(consensusAligner,
                parent.getPresets().getVariantCallerParameters());

        if (outputPrefix != null) {
            variantCaller.writePlainText(outputPrefix);
        }

        sout("Finished", 1);

        ran = true;
    }

    protected String getOutputPrefix() {
        String outputPath = parent.outputPath;

        if (outputPath == null) {
            return null;
        } else {
            return outputPath + sample.getFullName();
        }
    }

    public MigOutputPort getReader() {
        return reader;
    }

    public MigSizeDistribution getMigSizeDistribution() {
        return migSizeDistribution;
    }

    public Assembler getAssembler() {
        return assembler;
    }

    public ConsensusAligner getConsensusAligner() {
        return consensusAligner;
    }

    public VariantCaller getVariantCaller() {
        return variantCaller;
    }

    public List<AlignedConsensus> getAlignmentDataList() {
        return alignmentDataList;
    }

    public Sample getSample() {
        return sample;
    }

    public ProjectAnalysis getParent() {
        return parent;
    }

    public boolean wasRan() {
        return ran;
    }

    @Override
    public boolean isPairedEnd() {
        return paired;
    }
}
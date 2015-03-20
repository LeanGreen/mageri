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
 * Last modified on 12.3.2015 by mikesh
 */

package com.milaboratory.oncomigec.pipeline.analysis;

import com.milaboratory.oncomigec.core.PipelineBlock;
import com.milaboratory.oncomigec.core.align.processor.aligners.ExtendedExomeAlignerFactory;
import com.milaboratory.oncomigec.core.genomic.BasicGenomicInfoProvider;
import com.milaboratory.oncomigec.core.genomic.Reference;
import com.milaboratory.oncomigec.core.genomic.ReferenceLibrary;
import com.milaboratory.oncomigec.core.io.readers.MigOutputPort;
import com.milaboratory.oncomigec.model.classifier.BaseVariantClassifier;
import com.milaboratory.oncomigec.model.classifier.VariantClassifier;
import com.milaboratory.oncomigec.model.variant.Variant;
import com.milaboratory.oncomigec.model.variant.VariantContainer;
import com.milaboratory.oncomigec.pipeline.Presets;
import com.milaboratory.oncomigec.pipeline.RuntimeParameters;
import com.milaboratory.oncomigec.pipeline.SerializationUtils;
import com.milaboratory.oncomigec.pipeline.Speaker;
import com.milaboratory.oncomigec.pipeline.input.Input;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ProjectAnalysis implements Serializable {
    protected final ReferenceLibrary referenceLibrary;
    protected final Project project;
    protected final Presets presets;
    protected final RuntimeParameters runtimeParameters;

    private final PreprocessorFactory preprocessorFactory;
    private transient final PipelineAssemblerFactory pipelineAssemblerFactory;
    private transient final PipelineConsensusAlignerFactory pipelineConsensusAlignerFactory;
    protected transient final VariantClassifier classifier;

    private final Map<Sample, SampleAnalysis> analysisBySample = new TreeMap<>();

    public ProjectAnalysis(Input input) throws IOException {
        this(input, Presets.DEFAULT, RuntimeParameters.DEFAULT, BaseVariantClassifier.BUILT_IN);
    }

    public ProjectAnalysis(Input input,
                           Presets presets,
                           RuntimeParameters runtimeParameters,
                           VariantClassifier classifier) throws IOException {
        this.presets = presets;
        this.runtimeParameters = runtimeParameters;
        this.project = Project.fromInput(input);

        this.referenceLibrary = ReferenceLibrary.fromInput(input.getReferences(),
                new BasicGenomicInfoProvider()); // todo: implement genomic info

        this.preprocessorFactory = new PreprocessorFactory(presets.getDemultiplexParameters(),
                presets.getPreprocessorParameters());

        this.pipelineAssemblerFactory = new PipelineAssemblerFactory(presets.getAssemblerParameters());

        ExtendedExomeAlignerFactory alignerFactory = new ExtendedExomeAlignerFactory(referenceLibrary);

        this.pipelineConsensusAlignerFactory = new PipelineConsensusAlignerFactory(alignerFactory,
                presets.getConsensusAlignerParameters());

        this.classifier = classifier;
    }

    private void sout(String message, int verbosityLevel) {
        Speaker.INSTANCE.sout("[" + project.getName() + "] " +
                message, verbosityLevel);
    }

    public void run() throws Exception {
        sout("Started analysis.", 1);
        for (SampleGroup sampleGroup : project.getSampleGroups()) {
            sout("Pre-processing sample group " + sampleGroup.getName() + ".", 1);
            final Preprocessor preprocessor = preprocessorFactory.create(sampleGroup, runtimeParameters);

            sout("Running analysis for sample group " + sampleGroup.getName() + ".", 1);
            for (Sample sample : sampleGroup.getSamples()) {
                final MigOutputPort reader = preprocessor.create(sample);

                SampleAnalysis sampleAnalysis = new SampleAnalysis(
                        this, sample, preprocessor.getUmiHistogram(sample),
                        reader,
                        pipelineAssemblerFactory.create(sample),
                        pipelineConsensusAlignerFactory.create(sample)
                );

                sampleAnalysis.runFirstStage();

                if (!runtimeParameters.variantDumpModeOn())
                    sampleAnalysis.runSecondStage();

                // only for binary output mode
                analysisBySample.put(sample, sampleAnalysis);
            }
        }

        sout("Finished analysis.", 1);
    }

    public ReferenceLibrary getReferenceLibrary() {
        return referenceLibrary;
    }

    public Project getProject() {
        return project;
    }

    public Presets getPresets() {
        return presets;
    }

    public RuntimeParameters getRuntimeParameters() {
        return runtimeParameters;
    }

    public VariantClassifier getClassifier() {
        return classifier;
    }

    public SampleAnalysis getAnalysis(Sample sample) {
        return analysisBySample.get(sample);
    }

    public Preprocessor getPreprocessor(SampleGroup sampleGroup) {
        return preprocessorFactory.getPreprocessor(sampleGroup);
    }

    public List<SampleAnalysis> getAnalysis(SampleGroup sampleGroup) {
        List<SampleAnalysis> analysises = new ArrayList<>();
        for (Sample sample : sampleGroup.getSamples()) {
            analysises.add(getAnalysis(sample));
        }
        return analysises;
    }

    public void serialize(String path) throws IOException {
        serialize(path, false);
    }

    public void serialize(String path, boolean noBinary) throws IOException {
        sout("Writing output.", 1);
        String prefix = path + "/" + project.getName();

        if (runtimeParameters.variantDumpModeOn()) {
            File variantFile = new File(prefix + ".variants.txt");
            PrintWriter writer = new PrintWriter(variantFile);

            writer.println(Variant.HEADER);
            for (Reference reference : referenceLibrary.getReferences()) {
                for (SampleAnalysis sampleAnalysis : analysisBySample.values()) {
                    VariantContainer variantContainer = sampleAnalysis.dumpMinorVariants(reference);
                    if (variantContainer != null) {
                        for (Variant variant : variantContainer.getMinorVariants()) {
                            writer.println(variant.toString());
                        }
                    }
                }
            }
            writer.close();
            return;
        }

        preprocessorFactory.writePlainText(prefix);
        pipelineAssemblerFactory.writePlainText(prefix);
        pipelineConsensusAlignerFactory.writePlainText(prefix);

        for (SampleAnalysis sampleAnalysis : analysisBySample.values()) {
            for (PipelineBlock pipelineBlock : sampleAnalysis.getBlocks()) {
                pipelineBlock.writePlainText(prefix + "." + sampleAnalysis.getSample().getFullName());
            }
        }

        if (!noBinary) {
            SerializationUtils.writeObjectToFile(new File(prefix + ".mi"), this);
        }
        sout("Done.", 1);
    }
}

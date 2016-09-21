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

package com.antigenomics.mageri.core.variant;

import com.antigenomics.mageri.core.PipelineBlock;
import com.antigenomics.mageri.core.genomic.Reference;
import com.antigenomics.mageri.core.mapping.ConsensusAligner;
import com.antigenomics.mageri.core.mapping.MutationsTable;
import com.antigenomics.mageri.core.mutations.Substitution;
import com.antigenomics.mageri.core.variant.filter.QualFilter;
import com.antigenomics.mageri.core.variant.filter.SingletonFilter;
import com.antigenomics.mageri.core.variant.filter.VariantFilter;
import com.milaboratory.core.sequence.mutations.Mutations;
import com.milaboratory.core.sequence.nucleotide.NucleotideSequenceBuilder;
import com.antigenomics.mageri.core.genomic.ReferenceLibrary;
import com.antigenomics.mageri.core.mutations.Mutation;
import com.antigenomics.mageri.core.variant.filter.CoverageFilter;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.BinomialDistribution;
import org.apache.commons.math.distribution.BinomialDistributionImpl;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class VariantCaller extends PipelineBlock {
    protected final ReferenceLibrary referenceLibrary;
    protected final VariantFilter[] filters;
    protected final List<Variant> variants = new LinkedList<>();

    public VariantCaller(ConsensusAligner consensusAligner) {
        this(consensusAligner, VariantCallerParameters.DEFAULT);
    }

    public VariantCaller(ConsensusAligner consensusAligner,
                         VariantCallerParameters variantCallerParameters) {
        super("variant.caller");
        this.referenceLibrary = consensusAligner.getReferenceLibrary();
        filters = new VariantFilter[3];
        filters[0] = new QualFilter(variantCallerParameters.getQualityThreshold());
        filters[1] = new SingletonFilter(variantCallerParameters.getSingletonFrequencyThreshold());
        filters[2] = new CoverageFilter(variantCallerParameters.getCoverageThreshold());

        final ErrorModel errorModel = new ErrorModel(
                variantCallerParameters.getOrder(),
                variantCallerParameters.getModelCycles(),
                variantCallerParameters.getModelEfficiency() - 1.0);

        for (Reference reference : referenceLibrary.getReferences()) {
            final MutationsTable mutationsTable = consensusAligner.getAlignerTable(reference);
            final MinorMatrix minorMatrix = MinorMatrix.fromMutationsTable(mutationsTable);
            if (mutationsTable.wasUpdated()) {
                for (Mutation mutation : mutationsTable.getMutations()) {
                    if (mutation instanceof Substitution) {
                        int code = ((Substitution) mutation).getCode(),
                                pos = Mutations.getPosition(code),
                                from = Mutations.getFrom(code), to = Mutations.getTo(code);

                        int majorCount = mutationsTable.getMajorMigCount(pos, to);

                        assert majorCount > 0;

                        int coverage = mutationsTable.getMigCoverage(pos),
                                minorCount = mutationsTable.getMinorMigCount(pos, to);

                        double errorRate = errorModel.getErrorRate(minorCount, coverage,
                                from, to,
                                minorMatrix),
                                score = -10 * getLog10PValue(majorCount, coverage, errorRate);

                        NucleotideSequenceBuilder nsb = new NucleotideSequenceBuilder(1);
                        nsb.setCode(0, mutationsTable.getAncestralBase(pos));

                        Variant variant = new Variant(reference,
                                mutation, majorCount, minorCount,
                                mutationsTable.getMigCoverage(pos),
                                majorCount / (double) coverage,
                                score, mutationsTable.getMeanCqs(pos, to), errorRate,
                                nsb.create(), mutationsTable.hasReferenceBase(pos));

                        variant.filter(this);

                        variants.add(variant);
                    } else {
                        // TODO: IMPORTANT: INDELS
                    }
                }
            }
        }

        // This is quite important for memory usage
        // Of all objects, mig reader, consensus consensusAligner and variant caller
        // consume most memory. Consensus consensusAligner holds memory ~ number of references
        // Mig reader holds the entire read index, yet it gets immediately disposed
        // Variant caller data is needed to merge variant tables from different samples
        // Consensus consensusAligner is the only thing we can and should get rid from here
        consensusAligner.clear();

        // Variants should be sorted for GATK compatibility
        Collections.sort(variants);
    }

    private static double getLog10PValue(int majorCount, int total, double errorRate) {
        if (majorCount == 0) {
            return 0;
        }

        BinomialDistribution binomialDistribution = new BinomialDistributionImpl(total,
                errorRate);

        try {
            return Math.log10(1.0 - binomialDistribution.cumulativeProbability(majorCount) +
                    0.5 * binomialDistribution.probability(majorCount));
        } catch (MathException e) {
            e.printStackTrace();
            return Math.log10(binomialDistribution.probability(majorCount));
        }
    }

    public ReferenceLibrary getReferenceLibrary() {
        return referenceLibrary;
    }

    public int getFilterCount() {
        return filters.length;
    }

    public VariantFilter getFilter(int index) {
        return filters[index];
    }

    public List<Variant> getVariants() {
        return Collections.unmodifiableList(variants);
    }

    @Override
    public String getHeader() {
        return Variant.getHeader();
    }

    @Override
    public String getBody() {
        StringBuilder stringBuilder = new StringBuilder();

        for (Variant variant : variants) {
            stringBuilder.append(variant.toString()).append("\n");
        }

        return stringBuilder.toString();
    }
}
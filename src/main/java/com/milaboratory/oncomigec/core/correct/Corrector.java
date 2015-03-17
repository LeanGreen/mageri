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
package com.milaboratory.oncomigec.core.correct;

import com.milaboratory.core.sequence.Range;
import com.milaboratory.core.sequence.nucleotide.NucleotideAlphabet;
import com.milaboratory.oncomigec.core.PipelineBlock;
import com.milaboratory.oncomigec.core.consalign.entity.AlignedConsensus;
import com.milaboratory.oncomigec.core.consalign.entity.AlignerReferenceLibrary;
import com.milaboratory.oncomigec.core.genomic.Reference;
import com.milaboratory.oncomigec.core.mutations.MigecMutation;
import com.milaboratory.oncomigec.core.mutations.MigecMutationsCollection;
import com.milaboratory.oncomigec.model.classifier.BaseVariantClassifier;
import com.milaboratory.oncomigec.model.classifier.VariantClassifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public final class Corrector extends PipelineBlock {
    private final AtomicInteger goodConsensuses = new AtomicInteger(),
            totalConsensuses = new AtomicInteger();
    private final CorrectorReferenceLibrary correctorReferenceLibrary;

    public Corrector(AlignerReferenceLibrary referenceLibraryWithStatistics) {
        this(referenceLibraryWithStatistics, CorrectorParameters.DEFAULT, BaseVariantClassifier.BUILT_IN);
    }

    public Corrector(AlignerReferenceLibrary referenceLibraryWithStatistics,
                     CorrectorParameters parameters, VariantClassifier variantClassifier) {
        super("corrector");
        this.correctorReferenceLibrary = new CorrectorReferenceLibrary(referenceLibraryWithStatistics,
                parameters, variantClassifier);
    }

    public CorrectedConsensus correct(AlignedConsensus alignedConsensus) {
        totalConsensuses.incrementAndGet();

        Set<Integer> coverageMask = new HashSet<>();

        double maxPValue = 0;
        int offset = 0;
        Reference mainReference = alignedConsensus.getReference(0);
        MigecMutationsCollection totalMutations = new MigecMutationsCollection(mainReference);
        List<Range> ranges = new ArrayList<>();

        for (int i = 0; i < alignedConsensus.getNumberOfReferences(); i++) {
            Reference reference = alignedConsensus.getReference(i);

            if (!mainReference.equals(reference)) {
                throw new RuntimeException("Chimeric alignments not allowed in here.");
            }

            MigecMutationsCollection mutations = alignedConsensus.getMajorMutations(i);
            MutationFilter mutationFilter = correctorReferenceLibrary.getMutationFilter(reference);
            Range range = alignedConsensus.getRange(i);

            totalMutations.append(mutations);
            ranges.add(range);

            if (!mutationFilter.good())
                return null; // badly covered consensus

            // Update coverage mask
            for (int k = 0; k < reference.getSequence().size(); k++)
                if (!mutationFilter.passedFilter(k))
                    coverageMask.add(k + offset);

            // Filter substitutions and indels
            int mustHaveMutationsCount = 0;
            for (MigecMutation mutation : mutations) {
                // Check if that substitution passes coverage-quality filter 2nd step MIGEC
                if (mutation.isSubstitution()) {
                    if (mutationFilter.hasSubstitution(mutation.pos(), mutation.to())) {
                        if (!mutationFilter.hasReference(mutation.pos()))
                            mustHaveMutationsCount++; // covered a hole in reference with substitution
                    } else {
                        mutation.filter();
                    }
                    maxPValue = Math.max(maxPValue, correctorReferenceLibrary.getPValue(reference,
                            mutation.pos(),
                            mutation.to()));
                } else if (!mutationFilter.hasIndel(mutation.code())) {
                    mutation.filter();
                } else if (mutation.isDeletion()) {
                    if (!mutationFilter.hasReference(mutation.pos()))
                        mustHaveMutationsCount++; // covered a hole in reference with deletion

                    coverageMask.remove(offset + mutation.pos()); // no need to mask here
                }
            }

            // Check if we've covered all holes in the reference, discard otherwise
            if (mustHaveMutationsCount < mutationFilter.getMustHaveMutationsCount())
                return null;

            // Shift coverage mask
            offset += range.length();
        }

        goodConsensuses.incrementAndGet();

        return new CorrectedConsensus(mainReference, totalMutations.getMutationCodes(),
                coverageMask, maxPValue, alignedConsensus.getMigSize(), ranges);
    }

    public CorrectorReferenceLibrary getCorrectorReferenceLibrary() {
        return correctorReferenceLibrary;
    }

    @Override
    public String getHeader() {
        String subst = "", substP = "";
        for (byte i = 0; i < 4; i++) {
            char bp = NucleotideAlphabet.INSTANCE.symbolFromCode(i);
            subst += "\t" + bp;
            substP += "\t" + bp + ".prob";
        }
        return "reference\tpos\thas.reference\tgood.coverage\tgood.quality\t" +
                subst + substP;
    }

    @Override
    public String getBody() {
        StringBuilder stringBuilder = new StringBuilder();
        for (Reference reference : correctorReferenceLibrary.getReferenceLibrary().getReferences()) {
            MutationFilter mutationFilter = correctorReferenceLibrary.getMutationFilter(reference);
            for (int i = 0; i < reference.getSequence().size(); i++) {
                stringBuilder.append(reference.getFullName()).append("\t").
                        append(i).append("\t").
                        append(mutationFilter.hasReference(i)).append("\t").
                        append(mutationFilter.goodCoverage(i)).append("\t").
                        append(mutationFilter.goodQuality(i)).append("\t");

                for (byte j = 0; j < 4; j++) {
                    stringBuilder.append("\t").append(correctorReferenceLibrary.getMajorCount(reference, i, j));
                }
                for (byte j = 0; j < 4; j++) {
                    stringBuilder.append("\t").append(1.0 - correctorReferenceLibrary.getPValue(reference, i, j));
                }

                stringBuilder.append("\n");
            }
        }
        return stringBuilder.toString();
    }
}

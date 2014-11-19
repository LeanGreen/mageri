package com.milaboratory.migec2.core.consalign.mutations;

import com.milaboratory.core.sequence.NucleotideSQPair;
import com.milaboratory.core.sequence.alignment.KAligner;
import com.milaboratory.core.sequence.alignment.KAlignerParameters;
import com.milaboratory.core.sequence.alignment.KAlignmentHit;
import com.milaboratory.core.sequence.alignment.LocalAlignment;
import com.milaboratory.core.sequence.mutations.Mutations;
import com.milaboratory.core.sequence.nucleotide.NucleotideSequence;
import com.milaboratory.core.sequence.quality.SequenceQualityPhred;
import com.milaboratory.migec2.core.align.reference.Reference;
import com.milaboratory.migec2.core.assemble.entity.SConsensus;
import com.milaboratory.migec2.core.consalign.entity.VariantSizeLibrary;
import com.milaboratory.migec2.core.consalign.misc.ConsensusAlignerParameters;
import com.milaboratory.migec2.core.mutations.MigecMutationsCollection;

import java.util.*;

public final class MutationsExtractor {
    private final LocalAlignment consensusAlignment;
    private final Reference reference;
    private final SConsensus consensus;
    private final byte readQualThreshold, consQualThreshold;
    private final boolean backAlignDroppedReads;
    private final int[] consensusMutations, invertedConsensusMutations;
    private final KAligner aligner;

    private Map<Integer, Integer> minorMutations = null, minorMutationsUnmodifyable = null;
    private final Collection<MutationQualityPair> passedMajorMQPairs = new LinkedList<>(); // updated on creation

    public MutationsExtractor(LocalAlignment consensusAlignment, Reference reference,
                              SConsensus consensus,
                              ConsensusAlignerParameters parameters) {
        this.consensusAlignment = consensusAlignment;
        this.reference = reference;
        this.consensus = consensus;
        this.readQualThreshold = parameters.getReadQualityThreshold();
        this.consQualThreshold = parameters.getConsensusQualityThreshold();
        this.backAlignDroppedReads = parameters.backAlignDroppedReads();

        // ======================================================================
        // Extract ref->cons mutations (MAJORS)
        // Filter possible spurious mutations that arose due to inexact alignment
        // Then filter by CQS quality threshold
        // ======================================================================
        int[] mutations = computeFilteredRelativeMutations(
                reference.getSequence(),
                consensus.getConsensusSQPair().getQuality(),
                consensusAlignment, true);

        // ====================================================================
        // Move ref->cons mutations (MAJORS) to absolute reference coordinates
        // ====================================================================
        this.consensusMutations = Mutations.move(mutations, consensusAlignment.getSequence1Range().getFrom());

        // For read back-alignment
        this.aligner = new KAligner(KAlignerParameters.getByName("strict"));
        aligner.addReference(consensus.getConsensusSQPair().getSequence());

        // For converting read mutations to reference coordinates
        this.invertedConsensusMutations = Mutations.invertMutations(consensusMutations);
    }

    public MigecMutationsCollection calculateMajorMutations() {
        // Does all the stuff
        return new MigecMutationsCollection(reference, consensusMutations);
    }

    private Map<Integer, Integer> prebuildMinorMutations() {
        if (minorMutations == null) {
            // Also include dropped reads if required
            List<NucleotideSQPair> reads = consensus.getAssembledReads();
            if (backAlignDroppedReads) {
                reads = new LinkedList<>(reads); // copy
                reads.addAll(consensus.getDroppedReads());
            }

            // Align back the reads in Mig
            minorMutations = new HashMap<>();
            for (NucleotideSQPair read : reads) {
                KAlignmentHit hit = aligner.align(read.getSequence()).getBestHit();
                if (hit != null) {
                    LocalAlignment readAlignment = hit.getAlignment();

                    // ======================================================================
                    // Extract cons->read mutations (MINORS)
                    // Filter possible spurious mutations that arose due to inexact alignment
                    // Then filter by Phred quality threshold
                    // ======================================================================
                    int[] readMutations = computeFilteredRelativeMutations(
                            consensus.getConsensusSQPair().getSequence(),
                            read.getQuality(), readAlignment,
                            false);

                    // ====================================================================
                    // Move cons->read mutations (MINORS) to absolute reference coordinates
                    // ====================================================================
                    for (int code : readMutations) {
                        int pos = Mutations.getPosition(code), // pos in cons in cons <-> read frame
                                absPos = pos + readAlignment.getSequence1Range().getFrom(); // abs pos in cons

                        // if overlaps with ref<->cons frame, filter otherwise
                        if (consensusAlignment.getSequence2Range().contains(absPos)) {
                            // pos in cons in ref<->cons frame
                            int posInRefConsFrame = absPos - consensusAlignment.getSequence2Range().getFrom(),
                                    // pos in ref in ref<->cons frame
                                    relPosInRef = Mutations.convertPosition(invertedConsensusMutations, posInRefConsFrame);

                            // abs pos in ref
                            int newPos = relPosInRef +
                                    consensusAlignment.getSequence1Range().getFrom();

                            // not falls out of ref<->const frame
                            if (consensusAlignment.getSequence1Range().contains(newPos)) {
                                int newCode = Mutations.move(code, newPos - pos); // MOVE TO REF COORDS HERE
                                Integer count = minorMutations.get(newCode);
                                if (count == null)
                                    count = 0;
                                minorMutations.put(newCode, count + 1);
                            }
                        }
                    }
                }
            }
            minorMutationsUnmodifyable = Collections.unmodifiableMap(minorMutations);
        }
        return minorMutationsUnmodifyable;
    }

    public Map<Integer, Integer> calculateMinorMutations() {
        prebuildMinorMutations();
        return new HashMap<>(minorMutations);
    }

    public void updateVariantSizeStatistics(VariantSizeLibrary variantSizeLibrary) {
        variantSizeLibrary.update(reference,
                // this should return MAJOR substitutions, already moved to ref coords
                passedMajorMQPairs,
                // this should return MINOR mutations in reference coords
                // however they're not filtered from indels
                prebuildMinorMutations(),
                backAlignDroppedReads ? consensus.fullSize() : consensus.size());
    }

    private int[] computeFilteredRelativeMutations(NucleotideSequence referenceSequence, SequenceQualityPhred queryQuality,
                                                   LocalAlignment alignment, boolean major) {
        int[] mutationCodes = getRealMutations(referenceSequence, alignment);

        return filterSubstitutionsByQual(queryQuality,
                mutationCodes, alignment,
                major ? consQualThreshold : readQualThreshold,
                major ? passedMajorMQPairs : null);
    }

    private int[] getRealMutations(NucleotideSequence reference, LocalAlignment alignment) {
        int[] mutations = alignment.getMutations();
        NucleotideSequence subSequence = reference.getRange(alignment.getSequence1Range());
        Mutations.shiftIndelsAtHomopolymers(subSequence, mutations);
        mutations = Mutations.filterMutations(subSequence, mutations);
        return mutations;
    }

    private static int[] filterSubstitutionsByQual(SequenceQualityPhred queryQuality,
                                                   int[] mutationCodes,
                                                   LocalAlignment alignment,
                                                   byte qualityThreshold,
                                                   Collection<MutationQualityPair> passedQualityValues) {
        final boolean[] filter = new boolean[mutationCodes.length];
        int nFiltered = 0;
        for (int i = 0; i < mutationCodes.length; i++) {
            int code = mutationCodes[i];
            if (Mutations.isSubstitution(code)) {
                // This tells us precise position in sequence2,
                // that is, consensus for ref->cons alignment and
                // read for cons->read alignment
                int position = alignment.getSequence2Range().getFrom() + Mutations.convertPosition(mutationCodes,
                        Mutations.getPosition(code));
                byte qual = queryQuality.value(position);
                if (qual < qualityThreshold) {
                    filter[i] = true;
                    nFiltered++;
                } else if (passedQualityValues != null) {
                    passedQualityValues.add(new MutationQualityPair(
                            Mutations.move(code, alignment.getSequence1Range().getFrom()), // MOVE TO REF COORDS HERE
                            qual));
                }
            }
        }

        final int[] filteredMutationCodes = new int[mutationCodes.length - nFiltered];
        for (int i = 0, j = 0; i < mutationCodes.length; i++) {
            if (!filter[i]) {
                filteredMutationCodes[j] = mutationCodes[i];
                j++;
            }
        }

        return filteredMutationCodes;
    }

    // for testing purposes
    static int[] filterSubstitutionsByQual(SequenceQualityPhred queryQuality,
                                           LocalAlignment alignment,
                                           byte qualityThreshold) {
        return filterSubstitutionsByQual(queryQuality, alignment.getMutations(),
                alignment, qualityThreshold,
                null);
    }
}

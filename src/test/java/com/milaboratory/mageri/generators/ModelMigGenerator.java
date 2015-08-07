/*
 * Copyright (c) 2014-2015, Bolotin Dmitry, Chudakov Dmitry, Shugay Mikhail
 * (here and after addressed as Inventors)
 * All Rights Reserved
 *
 * Permission to use, copy, modify and distribute any part of this program for
 * educational, research and non-profit purposes, by non-profit institutions
 * only, without fee, and without a written agreement is hereby granted,
 * provided that the above copyright notice, this paragraph and the following
 * three paragraphs appear in all copies.
 *
 * Those desiring to incorporate this work into commercial products or use for
 * commercial purposes should contact the Inventors using one of the following
 * email addresses: chudakovdm@mail.ru, chudakovdm@gmail.com
 *
 * IN NO EVENT SHALL THE INVENTORS BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT,
 * SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS,
 * ARISING OUT OF THE USE OF THIS SOFTWARE, EVEN IF THE INVENTORS HAS BEEN
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * THE SOFTWARE PROVIDED HEREIN IS ON AN "AS IS" BASIS, AND THE INVENTORS HAS
 * NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR
 * MODIFICATIONS. THE INVENTORS MAKES NO REPRESENTATIONS AND EXTENDS NO
 * WARRANTIES OF ANY KIND, EITHER IMPLIED OR EXPRESS, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY OR FITNESS FOR A
 * PARTICULAR PURPOSE, OR THAT THE USE OF THE SOFTWARE WILL NOT INFRINGE ANY
 * PATENT, TRADEMARK OR OTHER RIGHTS.
 */

package com.milaboratory.mageri.generators;

import com.milaboratory.core.sequence.NucleotideSQPair;
import com.milaboratory.core.sequence.mutations.Mutations;
import com.milaboratory.core.sequence.nucleotide.NucleotideSequence;
import com.milaboratory.core.sequence.quality.SequenceQualityPhred;
import com.milaboratory.mageri.core.input.SMig;
import com.milaboratory.mageri.core.input.index.Read;
import com.milaboratory.mageri.core.variant.ErrorModel;
import com.milaboratory.mageri.pipeline.analysis.Sample;

import java.util.*;

import static com.milaboratory.mageri.generators.RandomUtil.randomSequence;

public class ModelMigGenerator {
    private static final Random rnd = new Random(480011L);
    private final double somaticMutationFreq;
    private final ErrorModel errorModel;
    private final MutationGenerator readErrorGenerator, pcrErrorGenerator, pcrHotSpotErrorGenerator;

    private int[] somaticMutations;

    private final NucleotideSequence reference;
    private final Set<Integer> hotSpotPositions = new HashSet<>(),
            pcrPositions = new HashSet<>();
    private final Map<Integer, Integer> somaticMutationCounters = new HashMap<>(),
            hotSpotMutationCounters = new HashMap<>(),
            totalMutationCounters = new HashMap<>();

    public ModelMigGenerator(double hotSpotPositionRatio, double pcrPositionRatio, double somaticMutationRatio,
                             double somaticMutationFreq,
                             ErrorModel errorModel,
                             MutationGenerator readErrorGenerator, MutationGenerator pcrErrorGenerator,
                             MutationGenerator pcrHotSpotErrorGenerator,
                             NucleotideSequence reference) {
        this.somaticMutationFreq = somaticMutationFreq;
        this.errorModel = errorModel;
        this.readErrorGenerator = readErrorGenerator;
        this.pcrErrorGenerator = pcrErrorGenerator;
        this.pcrHotSpotErrorGenerator = pcrHotSpotErrorGenerator;
        this.reference = reference;

        for (int i = 0; i < reference.size(); i++) {
            double p = rnd.nextDouble();
            if (p < hotSpotPositionRatio) {
                hotSpotPositions.add(i);
            }
            if (p < pcrPositionRatio) {
                pcrPositions.add(i);
            }
        }

        int[] somaticMutations = new int[reference.size()];
        int j = 0;

        for (int i = 0; i < reference.size(); i++) {
            double p = rnd.nextDouble();
            if (p < somaticMutationRatio) {
                int from = reference.codeAt(i);
                int to;
                do {
                    to = rnd.nextInt(3);
                } while (to == from);
                somaticMutations[j++] = Mutations.createSubstitution(i, from, to);
            }
        }

        this.somaticMutations = Arrays.copyOf(somaticMutations, j);
    }

    private static int[] generateAndFilterMutations(MutationGenerator mutationGenerator,
                                                    NucleotideSequence sequence,
                                                    Set<Integer> positions) {
        int l = 0;
        int[] mutations = mutationGenerator.nextMutations(sequence);
        for (int j = 0; j < mutations.length; j++) {
            int code = mutations[j];
            if (positions.contains(Mutations.getPosition(code))) {
                mutations[l++] = code;
            }
        }
        return Arrays.copyOf(mutations, l);
    }

    private static int[] selectMutations(int[] mutations, double p) {
        int l = 0;
        int[] _mutations = new int[mutations.length];
        for (int code : mutations) {
            if (rnd.nextDouble() < p) {
                _mutations[l++] = code;
            }
        }
        return Arrays.copyOf(_mutations, l);
    }

    public SMig nextMig() {
        int[] somaticMutations = selectMutations(this.somaticMutations, somaticMutationFreq);

        Integer counter;
        for (int code : somaticMutations) {
            somaticMutationCounters.put(code,
                    (((counter = somaticMutationCounters.get(code)) == null) ? 0 : counter) + 1
            );
            totalMutationCounters.put(code,
                    (((counter = totalMutationCounters.get(code)) == null) ? 0 : counter) + 1
            );
        }

        NucleotideSequence sequence1 = Mutations.mutate(reference, somaticMutations);

        //

        int[] hotSpotMutations = generateAndFilterMutations(pcrHotSpotErrorGenerator,
                sequence1, hotSpotPositions);

        for (int code : hotSpotMutations) {
            hotSpotMutationCounters.put(code,
                    (((counter = hotSpotMutationCounters.get(code)) == null) ? 0 : counter) + 1
            );
            totalMutationCounters.put(code,
                    (((counter = totalMutationCounters.get(code)) == null) ? 0 : counter) + 1
            );
        }

        NucleotideSequence sequence2 = Mutations.mutate(sequence1, hotSpotMutations);

        //

        List<Read> reads = new ArrayList<>();

        for (int i = 0; i < Math.pow(errorModel.getCycles(), 0.5 + 1.5 * rnd.nextDouble()); i++) {
            int[] pcrMutations = generateAndFilterMutations(pcrErrorGenerator,
                    sequence2, pcrPositions);
            NucleotideSequence sequence3 = Mutations.mutate(sequence2, pcrMutations);

            int[] readMutations = readErrorGenerator.nextMutations(sequence3);
            NucleotideSequence sequence4 = Mutations.mutate(sequence3, readMutations);
            byte[] quality = new byte[sequence4.size()];
            Arrays.fill(quality, (byte) 40);

            for (int mutation : readMutations) {
                int pos = Mutations.getPosition(mutation),
                        from = Mutations.getFrom(mutation),
                        to = Mutations.getTo(mutation);

                byte qual = (byte) Math.max(2, Math.min(40,
                        -10 * Math.log10(readErrorGenerator.getSubstitutionModel().getValue(from, to)
                        )));
                quality[pos] = qual;
            }

            reads.add(new Read(new NucleotideSQPair(sequence4, new SequenceQualityPhred(quality))));
        }

        return new SMig(Sample.create("dummy", false), randomSequence(12), reads);
    }

    public int getSomaticCount(int mutationCode) {
        Integer count = somaticMutationCounters.get(mutationCode);
        return count != null ? count : 0;
    }

    public int getHotSpotCount(int mutationCode) {
        Integer count = hotSpotMutationCounters.get(mutationCode);
        return count != null ? count : 0;
    }

    public int getVariantCount(int mutationCode) {
        Integer count = totalMutationCounters.get(mutationCode);
        return count != null ? count : 0;
    }

    public int somaticSize() {
        return somaticMutationCounters.size();
    }

    public int hotSpotSize() {
        return hotSpotMutationCounters.size();
    }

    public int totalSize() {
        return totalMutationCounters.size();
    }

    public int totalCount() {
        int totalCount = 0;
        for (int count : totalMutationCounters.values()) {
            totalCount += count;
        }
        return totalCount;
    }
}

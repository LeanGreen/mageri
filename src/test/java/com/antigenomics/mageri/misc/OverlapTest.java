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
package com.antigenomics.mageri.misc;

import com.antigenomics.mageri.generators.MutationGenerator;
import com.milaboratory.core.sequence.NucleotideSQPair;
import com.milaboratory.core.sequence.mutations.Mutations;
import com.milaboratory.core.sequence.nucleotide.NucleotideSequence;
import com.milaboratory.core.sequence.quality.QualityFormat;
import com.milaboratory.core.sequence.quality.SequenceQualityPhred;
import com.antigenomics.mageri.FastTests;
import com.antigenomics.mageri.PercentRangeAssertion;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Arrays;

import static com.antigenomics.mageri.generators.RandomUtil.randomSequence;


public class OverlapTest {
    private final int seqSize = 75;
    private final int totalRuns = 10000;

    @Test
    @Category(FastTests.class)
    public void positiveTestWithErrors() {
        positiveTestWithErrors(MutationGenerator.DEFAULT,
                PercentRangeAssertion.createLowerBound("Correct overlap rate", "Overlap with indels", 75),
                PercentRangeAssertion.createUpperBound("Incorrect overlap rate", "Overlap with indels", 5));
        positiveTestWithErrors(MutationGenerator.NO_INDEL,
                PercentRangeAssertion.createLowerBound("Correct overlap rate", "Overlap without indels", 90),
                PercentRangeAssertion.createUpperBound("Incorrect overlap rate", "Overlap without indels", 5));
    }

    public void positiveTestWithErrors(MutationGenerator mutationModel,
                                       PercentRangeAssertion correctOverlapRate,
                                       PercentRangeAssertion incorrectOverlapRate) {
        Overlapper readOverlapper = new Overlapper();
        int overlapped = 0, overlappedCorrectly = 0, total = totalRuns;
        for (int i = 0; i < total; i++) {
            int totalOverlapSize = 10;
            NucleotideSequence s1orig = randomSequence(seqSize - totalOverlapSize),
                    s2 = randomSequence(seqSize);

            NucleotideSequence s1 = s1orig.concatenate(s2.getRange(0, totalOverlapSize));

            int[] mutations1 = mutationModel.nextMutations(s1),
                    mutations2 = mutationModel.nextMutations(s2);

            s1 = Mutations.mutate(s1, mutations1);
            s2 = Mutations.mutate(s2, mutations2);

            NucleotideSequence s12 = Mutations.mutate(s1orig,
                    Mutations.extractMutationsForRange(mutations1, 0,
                            seqSize - totalOverlapSize)
            ).concatenate(s2);

            byte[] q1 = new byte[s1.size()], q2 = new byte[s2.size()];
            Arrays.fill(q1, (byte) (40 + 33));
            Arrays.fill(q2, (byte) (2 + 33));

            NucleotideSQPair r1 = new NucleotideSQPair(s1, SequenceQualityPhred.create(QualityFormat.Phred33, q1, true)),
                    r2 = new NucleotideSQPair(s2, SequenceQualityPhred.create(QualityFormat.Phred33, q2, true));

            Overlapper.OverlapResult result = readOverlapper.overlap(r1, r2);

            if (result.overlapped()) {
                overlapped++;
                NucleotideSequence overlappedSequence = result.getSQPair().getSequence();

                if (overlappedSequence.equals(s12))
                    overlappedCorrectly++;
            }
        }

        correctOverlapRate.assertInRange(overlappedCorrectly, total);
        incorrectOverlapRate.assertInRange(overlapped - overlappedCorrectly, overlapped);
    }

    @Test
    @Category(FastTests.class)
    public void readThroughTest() {
        Overlapper readOverlapper = new Overlapper(), readOverlapper2 = new Overlapper();
        int correctOverlap = 0, correctOverlapSwap = 0, correctOffset = 0,
                readThroughIdentified = 0, overlappedCount = 0,
                total = totalRuns;
        int overhangSize = (int) (0.5 * seqSize);

        for (int i = 0; i < total; i++) {
            NucleotideSequence fragment = randomSequence(2 * seqSize),
                    overhang5 = randomSequence(1, overhangSize), overhang3 = randomSequence(1, overhangSize);

            NucleotideSequence fullSeq = overhang5.concatenate(fragment).concatenate(overhang3);

            NucleotideSQPair r1 = new NucleotideSQPair(fragment),
                    r2 = new NucleotideSQPair(fullSeq);

            Overlapper.OverlapResult result = readOverlapper.overlap(r1, r2);

            if (result.overlapped()) {
                overlappedCount++;
                
                if (result.readThrough())
                    readThroughIdentified++;

                if (result.getSQPair().getSequence().equals(fullSeq))
                    correctOverlap++;

                if (result.getOffset1() == overhang5.size())
                    correctOffset++;
            }

            result = readOverlapper2.overlap(r2, r1);

            if (result.overlapped()) {
                if (result.getSQPair().getSequence().equals(fullSeq))
                    correctOverlapSwap++;
            }
        }

        PercentRangeAssertion.createLowerBound("Correct overlap", "Read-through", 95).
                assertInRange(correctOverlap, total);

        PercentRangeAssertion.createLowerBound("Correct overlap swapped", "Read-through", 95).
                assertInRange(correctOverlapSwap, total);

        Assert.assertEquals("Correct offset identified", correctOverlap, correctOffset);
        Assert.assertEquals("Read-through identified", correctOverlap, readThroughIdentified);

        // check stats
        Assert.assertEquals(readThroughIdentified, readOverlapper.getReadThroughCount());
        Assert.assertEquals(total, readOverlapper.getTotalCount());
        Assert.assertEquals(overlappedCount, readOverlapper.getOverlappedCount());
    }

    @Test
    @Category(FastTests.class)
    public void negativeTest() throws Exception {
        Overlapper readOverlapper = new Overlapper();
        int overlapped = totalRuns, total = overlapped;
        for (int i = 0; i < total; i++) {
            NucleotideSequence s1 = randomSequence(seqSize), s2 = randomSequence(seqSize);

            byte[] q1 = new byte[s1.size()], q2 = new byte[s2.size()];
            Arrays.fill(q1, (byte) (40 + 33));
            Arrays.fill(q2, (byte) (2 + 33));

            NucleotideSQPair r1 = new NucleotideSQPair(s1, SequenceQualityPhred.create(QualityFormat.Phred33, q1, true)),
                    r2 = new NucleotideSQPair(s2, SequenceQualityPhred.create(QualityFormat.Phred33, q2, true));

            Overlapper.OverlapResult result = readOverlapper.overlap(r1, r2);

            if (!result.overlapped()) {
                overlapped--;
            }
        }

        PercentRangeAssertion.createUpperBound("Overlap identified", "Negative test", 5).
                assertInRange(overlapped, total);
    }
}

package com.milaboratory.oncomigec.core.io.misc;

import com.milaboratory.core.sequence.nucleotide.NucleotideSequence;
import com.milaboratory.oncomigec.util.Util;
import com.milaboratory.oncomigec.util.testing.TestUtil;
import org.apache.commons.math.random.MersenneTwister;
import org.apache.commons.math.random.RandomGenerator;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;


public class UmiHistogramTest {
    private static RandomGenerator randomGenerator = new MersenneTwister(51102);

    private static UmiHistogram prepareHistogram() {
        UmiHistogram histogram = new UmiHistogram();

        // correct
        for (int i = 0; i < 1000; i++) {
            int migSize = (int) Math.pow(2, randomGenerator.nextGaussian() + 5);
            NucleotideSequence umi = Util.randomSequence(12);
            for (int k = 0; k < migSize; k++)
                histogram.update(umi);
        }

        // errors
        for (int i = 0; i < 10000; i++) {
            int migSize = (int) Math.pow(2, randomGenerator.nextGaussian() * 0.5);
            NucleotideSequence umi = Util.randomSequence(12);
            for (int k = 0; k < migSize; k++)
                histogram.update(umi);
        }

        histogram.calculateHistogram();

        return histogram;
    }

    @Test
    public void test() {
        System.out.println("Running performance test for UmiHistogram");
        UmiHistogram histogram = prepareHistogram();

        System.out.println(histogram);

        int overseq = histogram.getMigSizeThreshold();
        System.out.println("Overseq = " + overseq);

        System.out.println("Reads dropped count with overseq = " +
                histogram.calculateReadsDropped(overseq) +
                " of total = " + histogram.getReadTotal());
        System.out.println("MIGs dropped count with overseq = " +
                histogram.calculateMigsDropped(overseq) +
                " of total = " + histogram.getMigsTotal());

        Assert.assertEquals("Correct overseq", 4, overseq);
    }

    @Test
    public void serializationTest() throws IOException {
        System.out.println("Testing serialization test for UmiHistogram");
        UmiHistogram histogram = prepareHistogram();
        TestUtil.serializationCheck(histogram);
    }
}

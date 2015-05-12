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
package com.milaboratory.oncomigec.preprocessing;

import com.milaboratory.core.sequence.NucleotideSQPair;
import com.milaboratory.core.sequence.quality.QualityFormat;
import com.milaboratory.core.sequencing.io.fastq.PFastqReader;
import com.milaboratory.core.sequencing.io.fastq.SFastqReader;
import com.milaboratory.core.sequencing.read.PSequencingRead;
import com.milaboratory.core.sequencing.read.SSequencingRead;
import com.milaboratory.core.sequencing.read.SequencingRead;
import com.milaboratory.oncomigec.PercentRangeAssertion;
import com.milaboratory.oncomigec.TestUtil;
import com.milaboratory.oncomigec.preprocessing.barcode.BarcodeListParser;
import com.milaboratory.oncomigec.preprocessing.barcode.BarcodeSearcherResult;
import com.milaboratory.util.CompressionType;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static com.milaboratory.oncomigec.TestDataset.*;

public class CheckoutProcessorTest {
    private static void assertResult(SequencingRead read, CheckoutResult checkoutResult) {
        if (checkoutResult != null) {
            assertResult(checkoutResult);
            BarcodeSearcherResult masterResult = checkoutResult.getMasterResult();

            if (checkoutResult instanceof SimpleCheckoutResult) {
                Assert.assertTrue("UMI is specified in simple checkout result", checkoutResult.getUmi().size() > 0);
                Assert.assertTrue("No adapter match for simple checkout result", !checkoutResult.getMasterResult().hasAdapterMatch());
                return;
            }

            assertResult("Master",
                    checkoutResult.getOrientation() ? read.getData(0) : read.getData(1),
                    masterResult);

            if (checkoutResult instanceof PCheckoutResult) {
                PCheckoutResult pCheckoutResult = ((PCheckoutResult) checkoutResult);

                if (pCheckoutResult.slaveFound()) {
                    BarcodeSearcherResult slaveResult = pCheckoutResult.getSlaveResult();
                    assertResult("Slave",
                            checkoutResult.getOrientation() ? read.getData(1) : read.getData(0),
                            slaveResult);
                    Assert.assertEquals(checkoutResult.getUmi(), masterResult.getUmi().
                            concatenate(slaveResult.getUmi()));
                }
            } else {
                Assert.assertEquals(checkoutResult.getUmi(), masterResult.getUmi());
            }
        }
    }

    private static void assertResult(CheckoutResult checkoutResult) {
        Assert.assertNotNull(checkoutResult.getSampleName());
        Assert.assertNotNull(checkoutResult.getMasterResult());
        Assert.assertNotNull(checkoutResult.getUmi());
    }

    private static void assertResult(String resultType,
                                     NucleotideSQPair read, BarcodeSearcherResult result) {
        if (result.hasAdapterMatch()) {
            Assert.assertTrue(resultType + " result in bounds (from=" + result.getFrom() + ",read_sz=" + read.size() + ")",
                    result.getFrom() < read.size() && result.getFrom() >= 0);
            Assert.assertTrue(resultType + " result in bounds (to=" + result.getTo() + ",read_sz=" + read.size() + ")",
                    result.getTo() <= read.size() && result.getTo() > 0);
        } else {
            Assert.assertEquals("Blank result is used when no adapter match present and no UMI is specified",
                    BarcodeSearcherResult.BLANK_RESULT, result);
        }
    }

    private static void assertProcessor(CheckoutProcessor processor) {
        System.out.println(processor);
        double extractionRatio = processor.extractionRatio();

        System.out.println("Extraction ratio = " + extractionRatio);
        Assert.assertTrue("Extraction ratio in expected bounds", extractionRatio >= 0.99);
    }

    private static CheckoutProcessor runOnSampleData1Positional() throws IOException {
        return runOnSampleData1Positional("NNNNNNNNNNNNNN");
    }

    private static CheckoutProcessor runOnSampleData1Positional(String mask) throws IOException {
        SPositionalExtractor processor = new SPositionalExtractor(SAMPLE_NAME, mask);

        SFastqReader reader = new SFastqReader(getR1(),
                CompressionType.None);

        SSequencingRead read;
        while ((read = reader.take()) != null) {
            assertResult(read, processor.checkout(read));
        }

        return processor;
    }

    private static CheckoutProcessor runOnSampleData2Positional() throws IOException {
        return runOnSampleData2Positional("NNNNNNNNNNNNNN", null, null);
    }

    private static CheckoutProcessor runOnSampleData2Positional(String mask1,
                                                                String mask2,
                                                                String seed) throws IOException {
        PPositionalExtractor processor = mask2 == null ? new PPositionalExtractor(SAMPLE_NAME, mask1) :
                new PPositionalExtractor(SAMPLE_NAME, mask1, mask2);

        PFastqReader reader = new PFastqReader(getR1(), getR2(),
                QualityFormat.Phred33, CompressionType.None,
                null, false, false);

        PSequencingRead read;

        int nReads = 0, seedFound = 0;

        while ((read = reader.take()) != null) {
            PCheckoutResult checkoutResult = processor.checkout(read);

            assertResult(read, checkoutResult);

            if (checkoutResult != null &&
                    checkoutResult.slaveFound() &&
                    seed != null &&
                    checkoutResult.getSlaveResult().getUmi().toString().equals(seed)) {
                seedFound++;
            }
            nReads++;
        }

        if (mask2 != null && seed != null) {
            PercentRangeAssertion.createLowerBound("CorrectUMIExtracted", "SlaveSlidingSearcher", 95).
                    assertInRange(seedFound, nReads);
        }

        return processor;
    }

    @Test
    public void headerParserTest() throws Exception {
        System.out.println("Running performance test for Header parser");
        HeaderExtractor processor = new HeaderExtractor(SAMPLE_NAME);

        SFastqReader reader = new SFastqReader(getR1(),
                CompressionType.None);

        SSequencingRead read;
        while ((read = reader.take()) != null) {
            assertResult(read, processor.checkout(read));
        }

        double extractionRatio = processor.extractionRatio();

        System.out.println("Extraction ratio = " + extractionRatio);
        Assert.assertTrue("Extraction ratio in expected bounds", extractionRatio == 1);
    }

    @Test
    public void positionalSingleEnd() throws Exception {
        System.out.println("Running performance test for positional Checkout processor (single)");

        CheckoutProcessor processor = runOnSampleData1Positional("nnnnnnNNNNNNNNNNNtNNNNNtNNNNNtgta");

        assertProcessor(processor);
    }

    @Test
    public void positionalPairedEnd() throws Exception {
        System.out.println("Running performance test for positional Checkout processor (paired-end)");

        CheckoutProcessor processor = runOnSampleData2Positional(
                "nnnnnnNNNNNNNNNNNtNNNNNtNNNNNtgta",
                "aggactgNNNNNNNaagtcgggnnnnnn",
                "CTTAAAG");

        assertProcessor(processor);

        runOnSampleData2Positional(
                "NNNNNNNNNNNNNNtgatcttgacgttgtagatgag",
                "aggactgcttaaagaagtcggg", null);

        runOnSampleData2Positional(
                "NNNNNNNNNNNNNNtgatcttgacgttgtagatgag",
                "a", null);
    }

    private static CheckoutProcessor runOnSampleData1Adapter() throws IOException {
        SAdapterExtractor processor = BarcodeListParser.generateSCheckoutProcessor(getBarcodesMix(),
                DemultiplexParameters.DEFAULT);

        SFastqReader reader = new SFastqReader(getR1(),
                CompressionType.None);

        SSequencingRead read;
        while ((read = reader.take()) != null) {
            assertResult(read, processor.checkout(read));
        }

        return processor;
    }

    private static CheckoutProcessor runOnSampleData2Adapter() throws IOException {
        return runOnSampleData2Adapter(getBarcodesMix());
    }

    private static CheckoutProcessor runOnSampleData2Adapter(List<String> barcodes) throws IOException {
        PAdapterExtractor processor = BarcodeListParser.generatePCheckoutProcessor(barcodes,
                DemultiplexParameters.DEFAULT);

        PFastqReader reader = new PFastqReader(getR1(), getR2(),
                QualityFormat.Phred33, CompressionType.None,
                null, false, false);

        PSequencingRead read;
        while ((read = reader.take()) != null) {
            assertResult(read, processor.checkout(read));
        }

        return processor;
    }

    @Test
    public void adapterSingleEnd() throws Exception {
        System.out.println("Running performance test for vanilla Checkout processor (single)");

        CheckoutProcessor processor = runOnSampleData1Adapter();

        assertProcessor(processor);
    }

    @Test
    public void adapterPairedEnd() throws Exception {
        System.out.println("Running performance test for vanilla Checkout processor (paired-end)");

        CheckoutProcessor processor = runOnSampleData2Adapter();

        assertProcessor(processor);

        runOnSampleData2Adapter(getBarcodesNoSlave());
    }

    @Test
    public void adapterPairedNoSlave() throws Exception {
        System.out.println("Running performance test for vanilla Checkout processor (paired-end) without slave");

        CheckoutProcessor processor = runOnSampleData2Adapter(getBarcodesNoSlave());

        assertProcessor(processor);
    }

    @Test
    public void adapterBadSlave() throws Exception {
        System.out.println("Running performance test for vanilla Checkout processor (paired-end), bad slave barcode");

        CheckoutProcessor processor = runOnSampleData2Adapter(getBarcodesBadSlave());

        System.out.println(processor);
        double extractionRatio = processor.extractionRatio();

        System.out.println("Extraction ratio = " + extractionRatio);
        Assert.assertTrue("Extraction ratio in expected bounds", extractionRatio <= 0.01);
    }

    @Test
    public void serializationTest() throws IOException {
        System.out.println("Running serialization test for Checkout processors");

        CheckoutProcessor processor = runOnSampleData1Positional();

        TestUtil.serializationCheck(processor);

        processor = runOnSampleData2Positional();

        TestUtil.serializationCheck(processor);

        processor = runOnSampleData1Adapter();

        TestUtil.serializationCheck(processor);

        processor = runOnSampleData2Adapter();

        TestUtil.serializationCheck(processor);
    }
}

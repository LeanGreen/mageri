package com.milaboratory.oncomigec.preproc.demultiplex.entity;

import com.milaboratory.core.sequence.NucleotideSQPair;
import com.milaboratory.oncomigec.preproc.demultiplex.barcode.BarcodeSearcherResult;

public final class SimpleCheckoutResult extends CheckoutResult {
    public SimpleCheckoutResult(String sampleName, NucleotideSQPair umiSQPair) {
        super(0, sampleName, new BarcodeSearcherResult(umiSQPair));
    }
}

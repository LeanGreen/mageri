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
package com.antigenomics.mageri.core.mapping.alignment;

import com.milaboratory.core.sequence.alignment.LocalAlignment;
import com.milaboratory.core.sequence.nucleotide.NucleotideSequence;
import com.antigenomics.mageri.core.genomic.Reference;

import java.io.Serializable;

public class AlignmentResult implements Serializable {
    private final NucleotideSequence query;
    private final LocalAlignment alignment;
    private final Reference reference;
    private final boolean reverseComplement, good;
    private final byte score;

    public AlignmentResult(NucleotideSequence query,
                           Reference reference,
                           LocalAlignment alignment,
                           boolean reverseComplement,
                           byte score,
                           boolean good) {
        this.query = query;
        this.alignment = alignment;
        this.reference = reference;
        this.reverseComplement = reverseComplement;
        this.score = score;
        this.good = good;
    }

    public NucleotideSequence getQuery() {
        return query;
    }

    public Reference getReference() {
        return reference;
    }

    public LocalAlignment getAlignment() {
        return alignment;
    }

    public byte getScore() {
        return score;
    }

    public boolean isReverseComplement() {
        return reverseComplement;
    }

    public boolean isGood() {
        return good;
    }
}
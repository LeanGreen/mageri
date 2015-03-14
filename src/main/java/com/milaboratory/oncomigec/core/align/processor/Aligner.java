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
package com.milaboratory.oncomigec.core.align.processor;

import com.milaboratory.core.sequence.nucleotide.NucleotideSequence;
import com.milaboratory.oncomigec.core.align.entity.PAlignmentResult;
import com.milaboratory.oncomigec.core.align.entity.SAlignmentResult;
import com.milaboratory.oncomigec.core.genomic.ReferenceLibrary;

public interface Aligner {
    /**
     * This thread-safe method should perform an alignment and return a set of LocalAlignment blocks.
     * @param sequence nucleotide sequence to align
     * @return alignment result (blocks of local alignments)
     */
    public SAlignmentResult align(NucleotideSequence sequence);

    public PAlignmentResult align(NucleotideSequence sequence1, NucleotideSequence sequence2);

    /**
     * Gets reference library
     *
     * @return returns a map of references built in the aligner with corresponding IDs
     */
    public ReferenceLibrary getReferenceLibrary();
}

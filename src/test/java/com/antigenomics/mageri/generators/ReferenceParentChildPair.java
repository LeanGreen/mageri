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

package com.antigenomics.mageri.generators;

import com.antigenomics.mageri.core.genomic.Reference;
import com.milaboratory.core.sequence.nucleotide.NucleotideSequence;

public class ReferenceParentChildPair {
    private final int[] mutations;
    private final NucleotideSequence parentSequence, childSequence;
    private final Reference parentReference;

    public ReferenceParentChildPair(int[] mutations, Reference parentReference,
                           NucleotideSequence parentSequence, NucleotideSequence childSequence) {
        this.mutations = mutations;
        this.parentReference = parentReference;
        this.parentSequence = parentSequence;
        this.childSequence = childSequence;
    }

    public int[] getMutations() {
        return mutations;
    }

    public Reference getParentReference() {
        return parentReference;
    }

    public NucleotideSequence getParentSequence() {
        return parentSequence;
    }

    public NucleotideSequence getChildSequence() {
        return childSequence;
    }
}

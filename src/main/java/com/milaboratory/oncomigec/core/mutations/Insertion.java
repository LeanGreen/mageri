/*
 * Copyright 2013-2015 Mikhail Shugay (mikhail.shugay@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Last modified on 11.4.2015 by mikesh
 */

package com.milaboratory.oncomigec.core.mutations;

import com.milaboratory.core.sequence.mutations.Mutations;
import com.milaboratory.core.sequence.nucleotide.NucleotideSequence;
import com.milaboratory.core.sequence.nucleotide.NucleotideSequenceBuilder;

public class Insertion extends Indel {
    public Insertion(MutationArray parent, int[] codes) {
        super(parent, codes);
    }

    @Override
    public char getType() {
        return 'I';
    }

    @Override
    public int getStart() {
        return Mutations.getPosition(codes[0]);
    }

    @Override
    public int getEnd() {
        return getStart();
    }

    @Override
    public NucleotideSequence getRef() {
        return NucleotideSequence.EMPTY_NUCLEOTIDE_SEUQUENCE;
    }

    @Override
    public NucleotideSequence getAlt() {
        NucleotideSequenceBuilder nsb = new NucleotideSequenceBuilder(getLength());
        for (int i = 0; i < getLength(); i++) {
            nsb.setCode(i, (byte) Mutations.getTo(codes[i]));
        }
        return nsb.create();
    }

    @Override
    public String toString() {
        return getType() + "" + getStart() + ":" + getRef().toString() + ">" + getAlt().toString();
    }
}

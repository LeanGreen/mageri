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
import com.milaboratory.oncomigec.core.genomic.Reference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MutationArray {
    protected final Reference reference;
    protected final List<Mutation> mutations;
    protected int length, numberOfFiltered = 0;

    public MutationArray(Reference reference, int[] codes) {
        this.reference = reference;
        this.mutations = new ArrayList<>();
        this.length = codes.length;

        IndelAccumulator insertionAccumulaor = new InsertionAccumulator(),
                deletionAccumulator = new DeletionAccumulator();

        for (int code : codes) {
            switch (Mutations.getType(code)) {
                case Insertion:
                    insertionAccumulaor.append(code);
                    break;
                case Deletion:
                    deletionAccumulator.append(code);
                    break;
                case Substitution:
                    insertionAccumulaor.safeFlush();
                    deletionAccumulator.safeFlush();
                    mutations.add(new Substitution(this, code));
                    break;
            }
        }

        insertionAccumulaor.safeFlush();
        deletionAccumulator.safeFlush();
    }

    public void append(MutationArray other) {
        mutations.addAll(other.mutations);
        length += other.length;
    }

    public Reference getReference() {
        return reference;
    }

    public int getLength() {
        return length;
    }

    public int getNumberOfFiltered() {
        return numberOfFiltered;
    }

    public List<Mutation> getMutations() {
        return Collections.unmodifiableList(mutations);
    }

    public List<Mutation> getMutations(boolean filtered) {
        List<Mutation> mutations = new ArrayList<>(length - numberOfFiltered);
        for (Mutation mutation : this.mutations) {
            if (!(filtered && mutation.filtered)) {
                mutations.add(mutation);
            }
        }
        return mutations;
    }

    public int[] getMutationCodes(boolean filtered) {
        int[] codes = new int[length - (filtered ? numberOfFiltered : 0)];
        int i = 0;
        for (Mutation mutation : mutations) {
            if (!(filtered && mutation.filtered)) {
                if (mutation instanceof Indel) {
                    for (int code : ((Indel) mutation).codes) {
                        codes[i++] = code;
                    }
                } else {
                    codes[i++] = ((Substitution) mutation).code;
                }
            }
        }
        return codes;
    }

    private abstract class IndelAccumulator {
        protected final int[] codes;
        protected int counter = 0;

        public IndelAccumulator() {
            this.codes = new int[length];
        }

        protected int getLastPos() {
            return Mutations.getPosition(codes[counter - 1]);
        }

        protected abstract boolean canExtend(int pos);

        public void append(int code) {
            if (counter == 0 || canExtend(Mutations.getPosition(code)) || flush())
                codes[counter++] = code;
        }

        public boolean flush() {
            mutations.add(new Insertion(MutationArray.this,
                    Arrays.copyOf(codes, counter)));
            counter = 0;
            return true;
        }

        public void safeFlush() {
            if (counter > 0)
                flush();
        }
    }

    private class InsertionAccumulator extends IndelAccumulator {
        @Override
        protected boolean canExtend(int pos) {
            return getLastPos() == pos;
        }
    }


    private class DeletionAccumulator extends IndelAccumulator {
        @Override
        protected boolean canExtend(int pos) {
            return getLastPos() + 1 == pos;
        }
    }
}

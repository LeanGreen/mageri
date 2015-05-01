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

import com.milaboratory.core.sequence.nucleotide.NucleotideSequence;

public abstract class Mutation {
    protected final MutationArray parent;
    protected boolean filtered = false;

    protected Mutation(MutationArray parent) {
        this.parent = parent;
    }

    public MutationArray getParent() {
        return parent;
    }

    public void filter() {
        filtered = true;
        parent.numberOfFiltered++;
    }

    public boolean isFiltered() {
        return filtered;
    }

    public abstract char getType();

    public abstract int getStart();

    public abstract int getEnd();

    public abstract NucleotideSequence getRef();

    public abstract NucleotideSequence getAlt();

    public abstract int getLength();

    @Override
    public String toString() {
        return getType() + "" + getStart() + "-" + getEnd() + ":" + getRef().toString() + ">" + getAlt().toString();
    }
}

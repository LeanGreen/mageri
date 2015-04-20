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
package com.milaboratory.oncomigec.core.mapping;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicIntegerArray;

/**
 * Singapore-style atomic coverage container.
 */
public final class NucleotideCoverage implements Serializable {
    final AtomicIntegerArray coverage;
    final int size;

    /**
     * Creates nucleotide coverage container with given length.
     *
     * @param size size of container
     */
    public NucleotideCoverage(int size) {
        this.size = size;
        this.coverage = new AtomicIntegerArray(size * 4);
    }

    /**
     * Increments coverage value for a given letter in a given position.
     *
     * @param position position in sequence
     * @param letter   letter
     * @return value after increment
     */
    public int incrementCoverage(int position, int letter) {
        return coverage.incrementAndGet(4 * position + letter);
    }

    public int incrementCoverage(int position, int letter, int count) {
        return coverage.addAndGet(4 * position + letter, count);
    }

    /**
     * Decrements coverage value for a given letter in a given position.
     *
     * @param position position in sequence
     * @param letter   letter
     * @return value after decrement
     */
    public int decrementCoverage(int position, int letter) {
        return coverage.decrementAndGet(4 * position + letter);
    }

    public int decrementCoverage(int position, int letter, int count) {
        return coverage.addAndGet(4 * position + letter, -count);
    }


    /**
     * Adds given delta to a given letter in a given position.
     *
     * @param position position in sequence
     * @param letter   letter
     * @param delta    delta value
     * @return value after addition
     */
    public int addCoverage(int position, int letter, int delta) {
        return coverage.addAndGet(4 * position + letter, delta);
    }

    /**
     * Returns the coverage value
     *
     * @param position
     * @param letter
     * @return
     */
    public int getCoverage(int position, int letter) {
        return coverage.get(4 * position + letter);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NucleotideCoverage)) return false;

        NucleotideCoverage that = (NucleotideCoverage) o;

        if (size != that.size) return false;
        if (!coverage.equals(that.coverage)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = coverage.hashCode();
        result = 31 * result + size;
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder().append("NucleotideCoverage\n");
        for (int l = 0; l < 4; ++l) {
            for (int i = 0; i < size; ++i)
                sb.append(getCoverage(i, l)).append("\t");
            sb.deleteCharAt(sb.length() - 1); //Removing last "\t"
            sb.append("\n");
        }
        sb.deleteCharAt(sb.length() - 1); //Removing last "\n"
        return sb.toString();
    }
}

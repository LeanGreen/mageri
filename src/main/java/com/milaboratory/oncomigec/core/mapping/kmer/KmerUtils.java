/*
 * Copyright (c) 2014-2015, Bolotin Dmitry, Chudakov Dmitry, Shugay Mikhail
 * (here and after addressed as Inventors)
 * All Rights Reserved
 *
 * Permission to use, copy, modify and distribute any part of this program for
 * educational, research and non-profit purposes, by non-profit institutions
 * only, without fee, and without a written agreement is hereby granted,
 * provided that the above copyright notice, this paragraph and the following
 * three paragraphs appear in all copies.
 *
 * Those desiring to incorporate this work into commercial products or use for
 * commercial purposes should contact the Inventors using one of the following
 * email addresses: chudakovdm@mail.ru, chudakovdm@gmail.com
 *
 * IN NO EVENT SHALL THE INVENTORS BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT,
 * SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS,
 * ARISING OUT OF THE USE OF THIS SOFTWARE, EVEN IF THE INVENTORS HAS BEEN
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * THE SOFTWARE PROVIDED HEREIN IS ON AN "AS IS" BASIS, AND THE INVENTORS HAS
 * NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR
 * MODIFICATIONS. THE INVENTORS MAKES NO REPRESENTATIONS AND EXTENDS NO
 * WARRANTIES OF ANY KIND, EITHER IMPLIED OR EXPRESS, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY OR FITNESS FOR A
 * PARTICULAR PURPOSE, OR THAT THE USE OF THE SOFTWARE WILL NOT INFRINGE ANY
 * PATENT, TRADEMARK OR OTHER RIGHTS.
 */

package com.milaboratory.oncomigec.core.mapping.kmer;

import com.milaboratory.core.sequence.nucleotide.NucleotideSequence;
import com.milaboratory.oncomigec.core.genomic.Reference;
import com.milaboratory.oncomigec.core.genomic.ReferenceLibrary;

public class KmerUtils {
    private final int k;

    public KmerUtils(int k) {
        if (k < 0 || k > 31)
            throw new IllegalArgumentException("K-mer length should be in [0, 31] (64bit)");
        this.k = k;
    }

    private int nKmers(NucleotideSequence sequence) {
        int nKmers = sequence.size() - k + 1;
        if (nKmers < 0)
            throw new IllegalArgumentException("Sequence size less than k-mer size");
        return nKmers;
    }

    public long[] extractKmers(NucleotideSequence sequence) {
        final int n = nKmers(sequence);
        final long[] kmers = new long[n];
        for (int i = 0; i < n; ++i) {
            long kmer = 0;
            for (int j = i; j < i + k; ++j)
                kmer = kmer << 2 | sequence.codeAt(j);
            kmers[i] = kmer;
        }
        return kmers;
    }

    public void countKmers(Reference reference, KmerMap kmerMap, boolean rc) {
        final NucleotideSequence sequence = rc ?
                reference.getSequence().getReverseComplement() :
                reference.getSequence();
        final int index = rc ?
                -(reference.getIndex() + 1) :
                (reference.getIndex() + 1);
        final int n = nKmers(sequence);
        for (int i = 0; i < n; ++i) {
            long kmer = 0;
            for (int j = i; j < i + k; ++j)
                kmer = kmer << 2 | sequence.codeAt(j);
            kmerMap.increment(kmer, index);
        }
    }

    public KmerMap buildKmerMap(ReferenceLibrary referenceLibrary) {
        final KmerMap kmerMap = new KmerMap();
        for (Reference reference : referenceLibrary.getReferences()) {
            countKmers(reference, kmerMap, true);
            countKmers(reference, kmerMap, false);
        }
        return kmerMap;
    }

    public long kmerByPosition(NucleotideSequence sequence, int pos) {
        long kmer = 0;
        for (int i = pos; i < pos + k; ++i)
            kmer = kmer << 2 | sequence.codeAt(i);
        return kmer;
    }

    public int getK() {
        return k;
    }
}

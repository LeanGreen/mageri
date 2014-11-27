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
package com.milaboratory.migec2.core.haplotype.misc;

import com.milaboratory.migec2.core.align.reference.Reference;
import com.milaboratory.migec2.core.mutations.wrappers.MutationWrapper;
import org.apache.commons.math.MathException;

// todo: change for VariantLibrary
@Deprecated
public interface HaplotypeErrorStatistics {
    public double calculatePValue(Reference reference,
                                  int mutation,
                                  int parentCount, int childCount) throws MathException;

    public int totalCountForMutation(Reference reference, int mutation);
}

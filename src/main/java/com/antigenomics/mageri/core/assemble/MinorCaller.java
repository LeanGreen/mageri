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

package com.antigenomics.mageri.core.assemble;

import com.antigenomics.mageri.core.PipelineBlock;

public abstract class MinorCaller<M extends MinorCaller> extends PipelineBlock {
    MinorCaller(String name) {
        super(name);
    }

    public abstract double computeFdr(int from, int to);

    abstract boolean callAndUpdate(int from, int to, int k, int n, int n0);

    abstract M combine(M other);

    public abstract double getReadFractionForCalledMinors(int from, int to);

    public abstract double getFilteredReadFraction(int from, int to);

    public abstract double getGlobalMinorRate(int from, int to);

    public abstract int getTotalMigs();

    public abstract double getGeometricMeanMigSize();
}

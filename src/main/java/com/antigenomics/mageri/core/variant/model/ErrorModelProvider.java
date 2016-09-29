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

package com.antigenomics.mageri.core.variant.model;

import com.antigenomics.mageri.core.mapping.MutationsTable;
import com.antigenomics.mageri.core.variant.VariantCallerParameters;

public class ErrorModelProvider {
    public static ErrorModel create(VariantCallerParameters parameters, MutationsTable mutationsTable) {
        switch (parameters.getErrorModelType()) {
            case MinorBased:
                return new MinorBasedErrorModel(parameters.getModelOrder(),
                        parameters.getModelCycles(), parameters.getModelEfficiency(),
                        mutationsTable);
            case RawData:
                return new RawDataErrorModel(mutationsTable);
            case Custom:
                return parameters.getParsedSubstitutionErrorRateMatrix();
            default:
                throw new IllegalArgumentException("Unknown error model " + parameters.getErrorModelType().toString());
        }
    }
}

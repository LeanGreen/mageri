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
 * Last modified on 17.3.2015 by mikesh
 */

package com.milaboratory.oncomigec.core.genomic;

import com.milaboratory.oncomigec.pipeline.MigecCli;

import java.util.Date;

public class Vcf {
    private final ReferenceLibrary referenceLibrary;

    public Vcf(ReferenceLibrary referenceLibrary) {
        this.referenceLibrary = referenceLibrary;
    }

    public String getHeader() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("##fileformat=VCFv4.2").append("\n").
                append("##fileDate=").append(new Date().toString()).append("\n").
                append("##source=oncomigec").append(MigecCli.MY_VERSION).append("\n").
                append("##reference=").append(referenceLibrary.getPath()).append("\n");
        for (Contig contig : referenceLibrary.getGenomicInfoProvider().getContigs()) {
            stringBuilder.append("##contig=<ID=").append(contig.getID()).
                    append(",assembly=").append(contig.getAssembly()).append(">\n");
        }
        stringBuilder.append("##phasing=none\n");
        return stringBuilder.toString();
    }

}

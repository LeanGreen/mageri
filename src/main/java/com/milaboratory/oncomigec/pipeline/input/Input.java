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
 * Last modified on 12.3.2015 by mikesh
 */

package com.milaboratory.oncomigec.pipeline.input;

import java.io.InputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * An object representing input metadata that is provided in json + tab delimited format
 */
public class Input implements Serializable {
    protected final String projectName;
    protected transient final InputStream references;
    protected final List<InputChunk> inputChunks;


    public Input(String projectName, InputStream references, List<InputChunk> inputChunks) {
        this.projectName = projectName;
        this.references = references;
        this.inputChunks = inputChunks;
    }

    public String getProjectName() {
        return projectName;
    }

    public InputStream getReferences() {
        return references;
    }

    public List<InputChunk> getInputChunks() {
        return Collections.unmodifiableList(inputChunks);
    }
}


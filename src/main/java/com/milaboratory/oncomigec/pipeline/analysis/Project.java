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

package com.milaboratory.oncomigec.pipeline.analysis;

import com.milaboratory.oncomigec.pipeline.input.Input;
import com.milaboratory.oncomigec.pipeline.input.InputChunk;
import com.milaboratory.oncomigec.pipeline.input.SubMultiplexRule;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Project implements Serializable {
    private final List<SampleGroup> sampleGroups = new ArrayList<>();
    private final List<Sample> samples = new ArrayList<>();
    private final Input input;

    public static Project fromInput(Input input) {
        Project project = new Project(input);

        for (InputChunk inputChunk : input.getInputChunks()) {
            SampleGroup group = new SampleGroup(inputChunk, project);
            if (inputChunk.hasSubMultiplexing()) {
                for (SubMultiplexRule subMultiplexRule : inputChunk.getSubMultiplexRules()) {
                    Sample sample = new Sample(subMultiplexRule, group);
                    project.samples.add(sample);
                }
            } else {
                Sample sample = new Sample(null, group);
                group.addSample(sample);
                project.samples.add(sample);
            }
        }

        return project;
    }

    private Project(Input input) {
        this.input = input;
    }

    public String getName() {
        return input.getProjectName();
    }

    public List<Sample> getSamples() {
        return Collections.unmodifiableList(samples);
    }

    public List<SampleGroup> getSampleGroups() {
        return sampleGroups;
    }
}

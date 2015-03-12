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

package com.milaboratory.oncomigec.pipeline;

import com.sun.istack.internal.NotNull;

public class Sample implements Comparable<Sample> {
    private final String name;
    private final Project parentProject;

    public Sample(@NotNull String name, @NotNull Project parentProject) {
        this.name = name;
        this.parentProject = parentProject;
        parentProject.add(this);
    }

    public String getName() {
        return name;
    }

    public Project getParentProject() {
        return parentProject;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Sample sample = (Sample) o;

        if (!name.equals(sample.name)) return false;
        if (!parentProject.equals(sample.parentProject)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + parentProject.hashCode();
        return result;
    }

    @Override
    public int compareTo(Sample o) {
        return this.getName().compareTo(o.getName());
    }
}

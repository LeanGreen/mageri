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
package com.milaboratory.oncomigec.util.testing;

import org.junit.Assume;

import java.io.File;

public class TestResources {
    public static File getResource(String name) {
        String path = System.getProperty("testResources");

        Assume.assumeTrue(path != null);

        if (!path.endsWith(File.separator))
            path += File.separator;

        path += name;

        return new File(path);
    }
}

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

package com.antigenomics.mageri;

import org.junit.Assert;

public class DoubleRangeAssertion {
    private final String paramName, condition;
    private final double from, to;
    private final boolean lowerBound, upperBound;

    public static DoubleRangeAssertion createLowerBound(String paramName, String condition, double from) {
        return new DoubleRangeAssertion(paramName, condition, from, Double.MAX_VALUE, true, false);
    }

    public static DoubleRangeAssertion createUpperBound(String paramName, String condition, double to) {
        return new DoubleRangeAssertion(paramName, condition, Double.MIN_VALUE, to, false, true);
    }

    public static DoubleRangeAssertion createRange(String paramName, String condition, double from, double to) {
        return new DoubleRangeAssertion(paramName, condition, from, to, false, false);
    }

    public static DoubleRangeAssertion createDummy(String paramName, String condition) {
        return new DoubleRangeAssertion(paramName, condition, Double.MIN_VALUE, Double.MAX_VALUE, true, true);
    }

    private DoubleRangeAssertion(String paramName, String condition, double from, double to, boolean lowerBound, boolean upperBound) {
        if (from > to)
            throw new IllegalArgumentException("From > to in range");
        this.paramName = paramName;
        this.condition = condition;
        this.from = from;
        this.to = to;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    private boolean contains(double value) {
        return (upperBound || value >= from) && (lowerBound || value <= to);
    }

    public void assertInRange(double value) {
        System.out.println(condition + " : " + paramName + " = " + value);
        Assert.assertTrue(paramName + " is in range " + toString() + " for " + condition, contains(value));
    }

    @Override
    public String toString() {
        if (lowerBound)
            return "[" + from + ", +inf)";
        else if (upperBound)
            return "(-inf, " + to + "]";
        else
            return "[" + from + ", " + to + "]";
    }
}

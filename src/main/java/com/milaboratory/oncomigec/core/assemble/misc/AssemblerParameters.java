package com.milaboratory.oncomigec.core.assemble.misc;

import com.milaboratory.oncomigec.util.ParameterSet;
import org.jdom.Element;

public final class AssemblerParameters implements ParameterSet {
    private final boolean qualityWeightedMode;
    private final int offsetRange, anchorRegion, maxMMs, maxConsequentMMs;
    private final int minReadSize;
    private final double maxDroppedReadsRatio;
    private final boolean cqsRescue;

    public static AssemblerParameters DEFAULT = new AssemblerParameters(
            4, 8, 4, 3,
            true,
            0.3,
            false);

    public static AssemblerParameters TORRENT454 = new AssemblerParameters(
            4, 8, 4, 3,
            true,
            0.6,
            true);

    public AssemblerParameters(int offsetRange, int anchorRegion, int maxMMs, int maxConsequentMMs,
                               boolean qualityWeightedMode, double maxDroppedReadsRatio,
                               boolean cqsRescue) {
        this.offsetRange = offsetRange;
        this.anchorRegion = anchorRegion;
        this.maxMMs = maxMMs;
        this.maxConsequentMMs = maxConsequentMMs;
        this.qualityWeightedMode = qualityWeightedMode;
        this.minReadSize = 2 * offsetRange + 2 * anchorRegion;
        this.maxDroppedReadsRatio = maxDroppedReadsRatio;
        this.cqsRescue = cqsRescue;
    }

    public boolean qualityWeightedMode() {
        return qualityWeightedMode;
    }

    public int getOffsetRange() {
        return offsetRange;
    }

    public int getAnchorRegion() {
        return anchorRegion;
    }

    public int getMaxMMs() {
        return maxMMs;
    }

    public int getMaxConsequentMMs() {
        return maxConsequentMMs;
    }

    public int getMinReadSize() {
        return minReadSize;
    }

    public double getMaxDroppedReadsRatio() {
        return maxDroppedReadsRatio;
    }

    public boolean doCqsRescue() {
        return cqsRescue;
    }

    @Override
    public Element toXml() {
        Element e = new Element("AssemblerParameters");
        e.addContent(new Element("offsetRange").setText(Integer.toString(offsetRange)));
        e.addContent(new Element("anchorRegion").setText(Integer.toString(anchorRegion)));
        e.addContent(new Element("maxMMs").setText(Integer.toString(maxMMs)));
        e.addContent(new Element("maxConsequentMMs").setText(Integer.toString(maxConsequentMMs)));
        e.addContent(new Element("qualityWeightedMode").setText(Boolean.toString(qualityWeightedMode)));
        e.addContent(new Element("maxDroppedReadsRatio").setText(Double.toString(maxDroppedReadsRatio)));
        e.addContent(new Element("cqsRescue").setText(Boolean.toString(cqsRescue)));
        return e;
    }

    public static AssemblerParameters fromXml(Element parent) {
        Element e = parent.getChild("AssemblerParameters");
        return new AssemblerParameters(
                Integer.parseInt(e.getChildTextTrim("offsetRange")),
                Integer.parseInt(e.getChildTextTrim("anchorRegion")),
                Integer.parseInt(e.getChildTextTrim("maxMMs")),
                Integer.parseInt(e.getChildTextTrim("maxConsequentMMs")),
                Boolean.parseBoolean(e.getChildTextTrim("qualityWeightedMode")),
                Double.parseDouble(e.getChildTextTrim("maxDroppedReadsRatio")),
                Boolean.parseBoolean(e.getChildTextTrim("cqsRescue"))
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AssemblerParameters that = (AssemblerParameters) o;

        if (anchorRegion != that.anchorRegion) return false;
        if (cqsRescue != that.cqsRescue) return false;
        if (maxConsequentMMs != that.maxConsequentMMs) return false;
        if (Double.compare(that.maxDroppedReadsRatio, maxDroppedReadsRatio) != 0) return false;
        if (maxMMs != that.maxMMs) return false;
        if (minReadSize != that.minReadSize) return false;
        if (offsetRange != that.offsetRange) return false;
        if (qualityWeightedMode != that.qualityWeightedMode) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = (qualityWeightedMode ? 1 : 0);
        result = 31 * result + offsetRange;
        result = 31 * result + anchorRegion;
        result = 31 * result + maxMMs;
        result = 31 * result + maxConsequentMMs;
        result = 31 * result + minReadSize;
        temp = Double.doubleToLongBits(maxDroppedReadsRatio);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (cqsRescue ? 1 : 0);
        return result;
    }
}

/*
 * Copyright (c) 2014-2015, Bolotin Dmitry, Chudakov Dmitry, Shugay Mikhail
 * (here and after addressed as Inventors)
 * All Rights Reserved
 *
 * Permission to use, copy, modify and distribute any part of this program for
 * educational, research and non-profit purposes, by non-profit institutions
 * only, without fee, and without a written agreement is hereby granted,
 * provided that the above copyright notice, this paragraph and the following
 * three paragraphs appear in all copies.
 *
 * Those desiring to incorporate this work into commercial products or use for
 * commercial purposes should contact the Inventors using one of the following
 * email addresses: chudakovdm@mail.ru, chudakovdm@gmail.com
 *
 * IN NO EVENT SHALL THE INVENTORS BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT,
 * SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS,
 * ARISING OUT OF THE USE OF THIS SOFTWARE, EVEN IF THE INVENTORS HAS BEEN
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * THE SOFTWARE PROVIDED HEREIN IS ON AN "AS IS" BASIS, AND THE INVENTORS HAS
 * NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR
 * MODIFICATIONS. THE INVENTORS MAKES NO REPRESENTATIONS AND EXTENDS NO
 * WARRANTIES OF ANY KIND, EITHER IMPLIED OR EXPRESS, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY OR FITNESS FOR A
 * PARTICULAR PURPOSE, OR THAT THE USE OF THE SOFTWARE WILL NOT INFRINGE ANY
 * PATENT, TRADEMARK OR OTHER RIGHTS.
 */

package com.milaboratory.oncomigec.pipeline;

import com.milaboratory.oncomigec.core.assemble.AssemblerParameters;
import com.milaboratory.oncomigec.core.input.PreprocessorParameters;
import com.milaboratory.oncomigec.core.mapping.ConsensusAlignerParameters;
import com.milaboratory.oncomigec.core.variant.VariantCallerParameters;
import com.milaboratory.oncomigec.misc.ParameterSet;
import com.milaboratory.oncomigec.preprocessing.DemultiplexParameters;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.*;

public class Presets implements ParameterSet {
    public static final Presets DEFAULT = new Presets();

    private final PreprocessorParameters preprocessorParameters;
    private final AssemblerParameters assemblerParameters;
    private final ConsensusAlignerParameters consensusAlignerParameters;
    private final VariantCallerParameters variantCallerParameters;
    private final DemultiplexParameters demultiplexParameters;

    private static String DEDUCE_VERSION() {
        return Presets.class.getPackage().getImplementationVersion();
    }

    private final static boolean TEST_VERSION;
    private final static String VERSION = (TEST_VERSION = (DEDUCE_VERSION() == null)) ? "TEST" : DEDUCE_VERSION();

    private Presets() {
        this(AssemblerParameters.DEFAULT);
    }

    public Presets(AssemblerParameters assemblerParameters) {
        this(DemultiplexParameters.DEFAULT,
                PreprocessorParameters.DEFAULT,
                assemblerParameters,
                ConsensusAlignerParameters.DEFAULT,
                VariantCallerParameters.DEFAULT);
    }

    public Presets(DemultiplexParameters demultiplexParameters,
                   PreprocessorParameters preprocessorParameters,
                   AssemblerParameters assemblerParameters,
                   ConsensusAlignerParameters consensusAlignerParameters,
                   VariantCallerParameters variantCallerParameters) {
        this.preprocessorParameters = preprocessorParameters;
        this.assemblerParameters = assemblerParameters;
        this.consensusAlignerParameters = consensusAlignerParameters;
        this.variantCallerParameters = variantCallerParameters;
        this.demultiplexParameters = demultiplexParameters;
    }


    public DemultiplexParameters getDemultiplexParameters() {
        return demultiplexParameters;
    }

    public PreprocessorParameters getPreprocessorParameters() {
        return preprocessorParameters;
    }

    public AssemblerParameters getAssemblerParameters() {
        return assemblerParameters;
    }

    public ConsensusAlignerParameters getConsensusAlignerParameters() {
        return consensusAlignerParameters;
    }

    public VariantCallerParameters getVariantCallerParameters() {
        return variantCallerParameters;
    }

    public static Presets loadFromFile(File xmlFile) throws JDOMException, IOException {
        return readFromStream(new FileInputStream(xmlFile));
    }

    public void writeToStream(OutputStream oStream) throws IOException {
        Element e = this.toXml();
        Document document = new Document(e);
        new XMLOutputter(Format.getPrettyFormat()).output(document, oStream);
    }

    public void writeToFile(File file) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        writeToStream(fileOutputStream);
        fileOutputStream.close();
    }

    public static Presets readFromStream(InputStream iStream) throws JDOMException, IOException {
        SAXBuilder builder = new SAXBuilder();
        Document document = builder.build(iStream);
        return fromXml(document.getRootElement());
    }

    public static Presets create(String instrument, String libraryType) {
        // todo: extend

        AssemblerParameters assemblerParameters;
        switch (instrument.toUpperCase()) {
            case "ILLUMINA":
                assemblerParameters = AssemblerParameters.DEFAULT;
                break;
            case "454":
            case "IONTORRENT":
                assemblerParameters = AssemblerParameters.TORRENT454;
                break;
            default:
                throw new IllegalArgumentException("Unknown instrument: " + instrument);
        }

        switch (libraryType.toUpperCase()) {
            case "MULTIPLEX":
                break;
            case "TRAPPING":
                break;
            case "WALKING":
                break;
            default:
                throw new IllegalArgumentException("Unknown library type preset: " + libraryType);
        }

        return new Presets(assemblerParameters);
    }

    @Override
    public Element toXml() {
        Element e = new Element("OncomigecPresets");
        e.addContent(new Element("version").setText(VERSION));
        e.addContent(demultiplexParameters.toXml());
        e.addContent(preprocessorParameters.toXml());
        e.addContent(assemblerParameters.toXml());
        e.addContent(consensusAlignerParameters.toXml());
        e.addContent(variantCallerParameters.toXml());
        return e;
    }

    public static Presets fromXml(Element e) {
        //Extracting format information
        String format = e.getChildTextTrim("version");

        //Checking for compatibility
        if (!TEST_VERSION && !format.equals(VERSION))
            throw new RuntimeException("Unsupported parameters format version.");

        return new Presets(
                DemultiplexParameters.fromXml(e),
                PreprocessorParameters.fromXml(e),
                AssemblerParameters.fromXml(e),
                ConsensusAlignerParameters.fromXml(e),
                VariantCallerParameters.fromXml(e)
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Presets presets = (Presets) o;

        if (!assemblerParameters.equals(presets.assemblerParameters)) return false;
        if (!consensusAlignerParameters.equals(presets.consensusAlignerParameters)) return false;
        if (!variantCallerParameters.equals(presets.variantCallerParameters)) return false;
        if (!demultiplexParameters.equals(presets.demultiplexParameters)) return false;
        if (!preprocessorParameters.equals(presets.preprocessorParameters)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = preprocessorParameters.hashCode();
        result = 31 * result + assemblerParameters.hashCode();
        result = 31 * result + consensusAlignerParameters.hashCode();
        result = 31 * result + variantCallerParameters.hashCode();
        result = 31 * result + demultiplexParameters.hashCode();
        return result;
    }
}

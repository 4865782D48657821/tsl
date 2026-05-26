package de.tsl.ingester.trustedlist;

import de.tsl.ingester.trustedlist.xml.TrustedListXml;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import java.io.StringReader;

/**
 * JAXB-based parser for the subset of the trusted list XML used by the synchronization pipeline.
 */
public class TrustedListXmlParser {

    private static final JAXBContext JAXB_CONTEXT = createContext();

    /**
     * Creates a new trusted-list XML parser.
     */
    public TrustedListXmlParser() {
    }

    /**
     * Parses the trusted list XML into the internal JAXB model.
     *
     * @param xml the raw trusted list document
     * @return the parsed trusted list representation
     * @throws TrustedListFormatException when the XML cannot be unmarshalled
     */
    public TrustedListXml parseTrustedList(String xml) {
        try {
            Unmarshaller unmarshaller = JAXB_CONTEXT.createUnmarshaller();
            return (TrustedListXml) unmarshaller.unmarshal(new StringReader(xml));
        } catch (JAXBException exception) {
            throw new TrustedListFormatException("Unable to parse trusted list XML", exception);
        }
    }

    private static JAXBContext createContext() {
        try {
            return JAXBContext.newInstance(TrustedListXml.class);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to initialize trusted list JAXB context", exception);
        }
    }
}

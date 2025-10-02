package no.imr.tools.xml;

import no.imr.tools.ResourceUtils;
import no.imr.tools.upgrade.UpgradeException;
import no.imr.tools.upgrade.Upgrader;
import no.imr.tools.upgrade.UpgraderFactory;
import org.dom4j.Element;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;

/**
 * Creates upgraders from xsl-files.
 */
public final class XslUpgraderFactory implements UpgraderFactory<Element> {
   private final String resourceDirectory;

   /**
    * Creates an upgrader factory that uses xsl-files.
    * Naming convention:
    * <blockquote>{@code
    * {resource directory}/FromVersion{version}.xsl
    * }</blockquote>
    *
    * @param resourceDirectory the resource directory
    */
   public XslUpgraderFactory(String resourceDirectory) {
      this.resourceDirectory = resourceDirectory;
   }

   @Override
   public Upgrader<Element> createUpgrader(String fromVersion) throws UpgradeException {
      String resourceName = resourceDirectory + "/FromVersion" + fromVersion + ".xsl";
      try (InputStream inputStream = ResourceUtils.getUrl(resourceName).openStream()) {
         XslTransformer xslTransformer = new XslTransformer(new StreamSource(inputStream));
         return element -> upgrade(xslTransformer, element);
      } catch (IOException | TransformerConfigurationException e) {
         throw new UpgradeException(resourceName, e);
      }
   }

   private static Element upgrade(XslTransformer xslTransformer, Element element) throws UpgradeException {
      try {
         return xslTransformer.transform(element);
      } catch (TransformerException e) {
         throw new UpgradeException(e);
      }
   }
}

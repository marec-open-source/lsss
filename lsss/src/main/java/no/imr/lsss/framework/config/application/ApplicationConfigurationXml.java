package no.imr.lsss.framework.config.application;

import org.dom4j.Document;
import org.dom4j.Node;
import org.jspecify.annotations.Nullable;

public final class ApplicationConfigurationXml {
   private final Document document;

   public ApplicationConfigurationXml(Document document) {
      this.document = document;
   }

   public @Nullable Node lsssServerPortNode() {
      return document.selectSingleNode("/unit[@name='ApplicationConfiguration']/unit[@name='AppMiscConf']/unit[@name='LsssServerConf']/configuration/parameters/parameter[@name='ServerPort']");
   }

   public @Nullable Node lsssServerActiveNode() {
      return document.selectSingleNode("/unit[@name='ApplicationConfiguration']/unit[@name='AppMiscConf']/unit[@name='LsssServerConf']/configuration/parameters/parameter[@name='ServerActive']");
   }
}

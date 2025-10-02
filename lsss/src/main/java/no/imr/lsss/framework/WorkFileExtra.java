package no.imr.lsss.framework;

import no.imr.korona.data.datamanager.DataFile;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

public abstract class WorkFileExtra {
   final String id;
   final Object lock = new Object();

   protected WorkFileExtra(String id) {
      this.id = id;
   }

   public abstract void toXml(DataFile dataFile, Element element);

   public abstract void beginFromXml();

   public abstract void fromXml(DataFile dataFile, @Nullable Element element, int originalVersion);

   public abstract void endFromXml();
}

package no.imr.korona.region;

import no.imr.tools.parameter.IntParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ParameterCollection;
import no.imr.tools.parameter.Unit;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.util.List;

public final class WorkData {
   private final IntParameter objectNumber = new IntParameter(new Name("ObjectNumber"),
         0, Unit.NONE);

   public WorkData() {
   }

   public void possiblyAdjustObjectNumber(int maxObjectNumber) {
      synchronized (objectNumber) {
         objectNumber.setIntValue(Math.max(maxObjectNumber, objectNumber.getIntValue()));
      }
   }

   public int nextObjectNumber() {
      synchronized (objectNumber) {
         objectNumber.add(1);
         return objectNumber.getIntValue();
      }
   }

   public Element toXml() {
      Element workData = DocumentHelper.createElement("workData");
      workData.add(parameterCollection().toXml());
      return workData;
   }

   public void fromXml(Element element) {
      Element parameters = element.element(ParameterCollection.XML_PARAMETERS);
      if (parameters != null) {
         parameterCollection().fromXml(parameters);
      }
   }

   private ParameterCollection parameterCollection() {
      return new ParameterCollection(List.of(objectNumber));
   }
}

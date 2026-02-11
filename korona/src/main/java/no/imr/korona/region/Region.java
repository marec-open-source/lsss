package no.imr.korona.region;

import com.google.common.collect.ImmutableSet;
import no.imr.korona.data.datamanager.PingContainer;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.tools.ImmutableUtils;
import no.imr.tools.Utils;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.FloatRangeSet;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.util.Collection;
import java.util.Set;

/**
 * Base class for regions.
 */
public abstract sealed class Region implements InterpretationContainer, no.marec.lsss.api.regions.Region permits Layer, School {
   private static final String XML_VISITED = "hasBeenVisisted";
   private static final String XML_OBJECT_NUMBER = "objectNumber";
   private static final String XML_LABELS = "labels";
   private static final String XML_LABEL = "label";

   private RegionManager regionManager;
   private volatile int objectNumber = -1;
   private final Interpretation interpretation;
   private ImmutableSet<String> labels = ImmutableSet.of();

   private boolean selectedAtLeastOnce = false;
   private boolean selected = false;

   Region(RegionManager regionManager) {
      this.regionManager = regionManager;
      interpretation = new Interpretation(regionManager.getPingContainer().getTransducerCount());
   }

   void setRegionManager(RegionManager regionManager) {
      this.regionManager = regionManager;
   }

   PingContainer getPingContainer() {
      return regionManager.getPingContainer();
   }

   public RegionManager getRegionManager() {
      return regionManager;
   }

   @Override
   public String toString() {
      PingRange pingRange = getPingRange();
      FloatRange depthRange = getPingContainer().getPingIndexStream(pingRange)
            .map(this::getDepthRanges)
            .reduce(FloatRangeSet.of(), FloatRangeSet::add)
            .getBoundingRange();
      return "objectNumber: " + objectNumber
            + ", pingCount: " + pingRange.getPingCount()
            + ", pingNumber: [" + pingRange.begin().getPingNumber() + ", " + pingRange.end().getPingNumber() + ")"
            + ", depthRange: " + depthRange;
   }

   @Override
   public int getId() {
      return getObjectNumber();
   }

   public boolean hasObjectNumber() {
      return objectNumber != -1;
   }

   public void removeObjectNumber() {
      objectNumber = -1;
   }

   public int getObjectNumber() {
      int result = objectNumber;
      if (result == -1) {
         synchronized (this) { // Double-checked locking with volatile field.
            result = objectNumber;
            if (result == -1) {
               result = regionManager.getRegionConfiguration().nextObjectNumber();
               objectNumber = result;
            }
         }
      }
      return result;
   }

   void setObjectNumber(int objectNumber) {
      this.objectNumber = objectNumber;
   }

   public boolean isSelected() {
      return selected;
   }

   public void setSelected(boolean selected) {
      this.selected = selected;
      if (selected) {
         selectedAtLeastOnce = true;
      }
   }

   public boolean isSelectedAtLeastOnce() {
      return selectedAtLeastOnce;
   }

   void setSelectedAtLeastOnce(boolean selectedAtLeastOnce) {
      this.selectedAtLeastOnce = selectedAtLeastOnce;
   }

   public ChannelInterpretation getChannelInterpretation(int channel) {
      return interpretation.getChannelInterpretation(channel);
   }

   @Override
   public Interpretation getInterpretation() {
      return interpretation;
   }

   @Override
   public ImmutableSet<String> getLabels() {
      return labels;
   }

   public void setLabels(ImmutableSet<String> labels) {
      this.labels = labels;
      regionManager.getLabelsChangeManager().notifyListeners(this);
   }

   @Override
   public void setLabels(Set<String> labels) {
      setLabels(ImmutableSet.copyOf(labels));
   }

   public boolean hasLabel(String label) {
      return labels.contains(label);
   }

   public void addLabel(String label) {
      setLabels(ImmutableUtils.add(labels, label));
   }

   public void addLabels(Collection<String> moreLabels) {
      setLabels(ImmutableUtils.addAll(labels, moreLabels));
   }

   public void removeLabel(String label) {
      setLabels(ImmutableUtils.remove(labels, label));
   }

   Element baseToXml(String elementName) {
      Element element = DocumentHelper.createElement(elementName)
            .addAttribute(XML_VISITED, Boolean.toString(isSelectedAtLeastOnce()))
            .addAttribute(XML_OBJECT_NUMBER, Integer.toString(getObjectNumber()));
      interpretation.toXml(element, getPingContainer());
      labelsToXml(element);
      return element;
   }

   void baseFromXml(Element element) {
      setSelectedAtLeastOnce(Boolean.parseBoolean(element.attributeValue(XML_VISITED)));
      String objectNumberAttribute = element.attributeValue(XML_OBJECT_NUMBER);
      if (objectNumberAttribute != null) {
         objectNumber = Integer.parseInt(objectNumberAttribute);
      }
      interpretation.fromXml(element, getPingContainer());
      labelsFromXml(element);
   }

   private void labelsToXml(Element element) {
      if (labels.isEmpty()) {
         return;
      }
      Element labelsElement = element.addElement(XML_LABELS);
      labels.forEach(label -> labelsElement.addElement(XML_LABEL).addText(label));
   }

   private void labelsFromXml(Element element) {
      Element labelsElement = element.element(XML_LABELS);
      if (labelsElement != null) {
         ImmutableSet.Builder<String> builder = ImmutableSet.builder();
         labelsElement.elements().forEach(labelElement -> {
            builder.add(Utils.intern(labelElement.getText()));
         });
         setLabels(builder.build());
      }
   }

   public boolean contains(EchogramPoint point) {
      return getDepthRanges(point.pingIndex()).contains(point.depth());
   }

   public abstract boolean contains(PingIndex pingIndex);

   public abstract boolean intersectsPingRange(PingRange pingRange);

   public abstract PingRange getPingRange();

   abstract float getRepresentativeMinDepth(PingIndex pingIndex);

   abstract float getRepresentativeMaxDepth(PingIndex pingIndex);

   abstract FloatRangeSet getDepthRanges(PingIndex pingIndex);

   public boolean hasEqualInterpretationTo(Region region) {
      return interpretation.isEqualTo(region.interpretation);
   }
}

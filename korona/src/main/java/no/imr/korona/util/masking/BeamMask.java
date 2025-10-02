package no.imr.korona.util.masking;

import no.imr.tools.range.IntRangeSet;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Mask for one beam.
 */
public final class BeamMask {
   private static final String XML_BEAM_MASK = "beam";
   public static final String XML_CHANNEL = "channel";
   private static final String XML_BEAM_LENGTH = "beamLength";
   private static final String XML_BEAM_RANGE = "range";

   public static final BeamMask EMPTY = new BeamMask(0);

   private final int size;
   private @Nullable FullBeamMask fullBeamMask;
   private @Nullable IntRangeSet compactBeamMask;

   public BeamMask(int size) {
      this.size = size;
      compactBeamMask = new IntRangeSet();
   }

   public BeamMask(Element element) {
      this(element, Integer.parseInt(element.attributeValue(XML_BEAM_LENGTH)));
   }

   public BeamMask(Element element, int size) {
      this.size = size;

      String s = element.attributeValue(XML_BEAM_RANGE).trim();

      if (s.isEmpty()) {
         compactBeamMask = new IntRangeSet();
         return;
      }

      int index = 0;
      List<Integer> indexes = new ArrayList<>();
      for (String token : s.split("\\s+")) {
         index += Integer.parseInt(token);
         indexes.add(index);
      }

      compactBeamMask = new IntRangeSet(indexes, size);
   }

   public Element toXml(int channel, boolean addLengthAttribute) {
      Element beamMask = DocumentHelper.createElement(XML_BEAM_MASK)
            .addAttribute(XML_CHANNEL, Integer.toString(channel));
      if (addLengthAttribute) {
         beamMask.addAttribute(XML_BEAM_LENGTH, Integer.toString(size()));
      }
      beamMask.addAttribute(XML_BEAM_RANGE, getCompactBeamMask().getDeltaString());
      return beamMask;
   }

   public BeamMask createCopy() {
      applyFullBeamMask();
      BeamMask copy = new BeamMask(size);
      copy.compactBeamMask = getCompactBeamMask().createCopy();
      return copy;
   }

   @Override
   public String toString() {
      return getCompactBeamMask().toString();
   }

   public FullBeamMask getFullBeamMask() {
      FullBeamMask fullBeamMask = this.fullBeamMask;
      if (fullBeamMask == null) {
         fullBeamMask = new FullBeamMask(size, getCompactBeamMask());
         this.fullBeamMask = fullBeamMask;
         compactBeamMask = null;
      }
      return fullBeamMask;
   }

   public void applyFullBeamMask() {
      FullBeamMask fullBeamMask = this.fullBeamMask;
      if (fullBeamMask != null) {
         compactBeamMask = fullBeamMask.toCompactBeamMask();
         this.fullBeamMask = null;
      }
   }

   public IntRangeSet getCompactBeamMask() {
      IntRangeSet compactBeamMask = this.compactBeamMask;
      if (compactBeamMask == null) {
         throw new IllegalStateException("No compact beam mask");
      }
      return compactBeamMask;
   }

   public int size() {
      return size;
   }

   public boolean contains(int index) {
      return getCompactBeamMask().contains(index);
   }

   public boolean isEmpty() {
      return getCompactBeamMask().isEmpty();
   }

   public void clear() {
      getCompactBeamMask().clear();
   }

   public void add(int index) {
      add(index, index + 1);
   }

   public void add(int beginIndex, int endIndex) {
      getCompactBeamMask().add(beginIndex, endIndex);
   }

   public void add(BeamMask beamMask) {
      getCompactBeamMask().add(beamMask.getCompactBeamMask());
   }

   public boolean remove(int beginIndex, int endIndex) {
      return getCompactBeamMask().remove(beginIndex, endIndex);
   }

   public boolean remove(BeamMask beamMask) {
      return getCompactBeamMask().remove(beamMask.getCompactBeamMask());
   }

   public boolean intersects(BeamMask beamMask) {
      return getCompactBeamMask().intersects(beamMask.getCompactBeamMask());
   }
}

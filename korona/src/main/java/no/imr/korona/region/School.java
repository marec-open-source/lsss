package no.imr.korona.region;

import com.google.common.collect.ImmutableMap;
import no.imr.korona.data.datamanager.PingContainer;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.korona.data.util.geometry.depth.DepthTransform;
import no.imr.korona.data.util.mask.MaskOutlineTracer;
import no.imr.korona.data.util.mask.MaskUtils;
import no.imr.korona.region.schooledit.BoundaryDrawEditor;
import no.imr.korona.region.schooledit.BoxBoundaryMoveEditor;
import no.imr.korona.region.schooledit.MoveEditor;
import no.imr.korona.region.schooledit.ScaleEditor;
import no.imr.korona.region.schooledit.SchoolEditor;
import no.imr.korona.util.KoronaUtils;
import no.imr.korona.util.echogram.EchogramPingSettings;
import no.imr.korona.util.echogram.EchogramZSettings;
import no.imr.tools.Utils;
import no.imr.tools.range.ArrayRangeSet;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.FloatRangeSet;
import no.imr.tools.range.RangeSet;
import no.imr.tools.range.RangeUtils;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public final class School extends Region {
   static final String XML_SCHOOL_MASK_REP = "schoolMaskRep";
   private static final String XML_PING_MASK = "pingMask";
   private static final String XML_REL_PING_NUMBER = "relativePingNumber";
   private static final String XML_PARAMETERS = "parameters";
   private static final String XML_PARAMETER = "parameter";
   private static final String XML_PARAMETER_CHANNEL_PARAMETERS = "channelParameters";
   private static final String XML_PARAMETER_FREQUENCY = "frequency";
   private static final String XML_PARAMETER_DATA_PROCESSED = "dataProcessed";
   private static final String XML_PARAMETER_NAME = "name";

   private SchoolMaskRepresentation schoolMaskRepresentation;
   private List<SchoolBoundaryObject> boundaryObjects;
   private @Nullable EchogramPoint centerPoint;
   private SchoolParameters parameters = SchoolParameters.EMPTY;
   private @Nullable SchoolEditor editor;

   private School(RegionManager regionManager, NavigableMap<PingIndex, FloatRangeSet> mask) {
      super(regionManager);

      schoolMaskRepresentation = new SchoolMaskRepresentation(mask, getPingContainer());
      boundaryObjects = schoolMaskRepresentation.computeBoundaryObjects(getPingContainer());
      centerPoint = schoolMaskRepresentation.computeCenterPoint(getPingContainer());
   }

   static School createFromXml(RegionManager regionManager, PingIndex referencePingIndex, Element element) {
      NavigableMap<PingIndex, FloatRangeSet> mask = xmlToSchoolMask(referencePingIndex, element, regionManager.getPingContainer());
      School school = createUnconstrained(regionManager, mask);
      school.baseFromXml(element);
      school.parametersFromXml(element);
      return school;
   }

   static School create(RegionManager regionManager, NavigableMap<PingIndex, FloatRangeSet> mask) {
      mask = constrainMask(mask, null, regionManager);
      return new School(regionManager, mask);
   }

   static School createUnconstrained(RegionManager regionManager, NavigableMap<PingIndex, FloatRangeSet> mask) {
      return new School(regionManager, mask);
   }

   void setMask(NavigableMap<PingIndex, FloatRangeSet> mask) {
      mask = constrainMask(mask, this, getRegionManager());
      setSchoolMaskRepresentation(mask);
   }

   private void setSchoolMaskRepresentation(NavigableMap<PingIndex, FloatRangeSet> schoolMask) {
      schoolMaskRepresentation = new SchoolMaskRepresentation(schoolMask, getPingContainer());
      boundaryObjects = schoolMaskRepresentation.computeBoundaryObjects(getPingContainer());
      centerPoint = schoolMaskRepresentation.computeCenterPoint(getPingContainer());
   }

   @Override
   public boolean contains(PingIndex pingIndex) {
      return getCurrentMask().contains(pingIndex);
   }

   @Override
   public boolean intersectsPingRange(PingRange pingRange) {
      return getCurrentMask().intersectsPingRange(pingRange);
   }

   @Override
   public boolean isReadOnly() {
      return getCurrentMask().isReadOnly(getRegionManager());
   }

   public SchoolParameters getParameters() {
      return parameters;
   }

   public void setParameters(SchoolParameters parameters) {
      this.parameters = parameters;
   }

   public void invalidateParameters() {
      parameters = SchoolParameters.EMPTY;
   }

   private SchoolManager getSchoolManager() {
      return getRegionManager().getSchoolManager();
   }

   private static NavigableMap<PingIndex, FloatRangeSet> xmlToSchoolMask(PingIndex referencePingIndex, Element element, PingContainer pingContainer) {
      NavigableMap<PingIndex, FloatRangeSet> schoolMask = new TreeMap<>();
      for (Element pingMaskElement : element.elements(XML_PING_MASK)) {
         long relIndex = Long.parseLong(pingMaskElement.attributeValue(XML_REL_PING_NUMBER));
         PingIndex pingIndex = pingContainer.getPingIndex(relIndex + referencePingIndex.getPingNumber());
         String s = pingMaskElement.getText().trim();
         String[] tokens = s.split("\\s+");
         if (tokens.length % 2 != 0) {
            throw new IllegalArgumentException("Mask definition must have an even number of values." +
                  "An odd number was detected for relative index " + relIndex);
         }
         List<FloatRange> depthRanges = new ArrayList<>(tokens.length / 2);
         for (int i = 0; i < tokens.length; i += 2) {
            depthRanges.add(FloatRange.of(Float.parseFloat(tokens[i]), Float.parseFloat(tokens[i + 1])));
         }
         schoolMask.put(pingIndex, FloatRangeSet.of(depthRanges));
      }
      return schoolMask;
   }

   Element toXml(PingRange pingRange) {
      Element rep = baseToXml(XML_SCHOOL_MASK_REP);
      parametersToXml(rep);
      PingIndex referencePingIndex = pingRange.begin();
      for (Map.Entry<PingIndex, FloatRangeSet> entry : schoolMaskRepresentation.getSchoolMask().entrySet()) {
         PingIndex index = entry.getKey();
         if (!pingRange.contains(index)) {
            continue;
         }
         long relIndex = index.getPingNumber() - referencePingIndex.getPingNumber();
         List<FloatRange> floatRanges = entry.getValue().getFloatRanges();
         boolean firstTime = true;
         StringBuilder sb = new StringBuilder(2 * 10 * floatRanges.size());
         for (FloatRange floatRange : floatRanges) {
            if (firstTime) {
               firstTime = false;
            } else {
               sb.append(' ');
            }
            sb.append(floatRange.min()).append(' ').append(floatRange.max());
         }
         rep.addElement(XML_PING_MASK)
               .addAttribute(XML_REL_PING_NUMBER, Long.toString(relIndex))
               .addText(sb.toString());
      }
      return rep;
   }

   private void parametersToXml(Element element) {
      if (!parameters.isUpToDate()) {
         return;
      }
      Element parameterElement = element.addElement(XML_PARAMETERS);
      addParameterValues(parameterElement, parameters.values());
      PingContainer pingContainer = getPingContainer();
      parameters.perChannelValues().forEach((channel, channelValues) -> {
         int kHz = KoronaUtils.hzToKHz(pingContainer.getFrequency(channel));
         Element channelParameter = parameterElement.addElement(XML_PARAMETER_CHANNEL_PARAMETERS)
               .addAttribute(XML_PARAMETER_FREQUENCY, Integer.toString(kHz))
               .addAttribute(XML_PARAMETER_DATA_PROCESSED, Boolean.toString(parameters.dataProcessed()));
         addParameterValues(channelParameter, channelValues);
      });
   }

   private static void addParameterValues(Element element, ImmutableMap<String, Float> channelValues) {
      channelValues.forEach((key, value) -> {
         element.addElement(XML_PARAMETER)
               .addAttribute(XML_PARAMETER_NAME, key)
               .addText(Float.toString(value));
      });
   }

   private void parametersFromXml(Element element) {
      Element parametersElement = element.element(XML_PARAMETERS);
      if (parametersElement != null) {
         boolean dataProcessed = Boolean.parseBoolean(parametersElement.attributeValue(XML_PARAMETER_DATA_PROCESSED));
         ImmutableMap<String, Float> values = xmlToParameterValues(parametersElement);
         ImmutableMap.Builder<Integer, ImmutableMap<String, Float>> perChannelValues = ImmutableMap.builder();
         PingContainer pingContainer = getPingContainer();
         parametersElement.elements(XML_PARAMETER_CHANNEL_PARAMETERS).forEach(channelParameterElement -> {
            if (channelParameterElement.attributeValue(XML_PARAMETER_FREQUENCY) != null) {
               int kHz = Integer.parseInt(channelParameterElement.attributeValue(XML_PARAMETER_FREQUENCY));
               int channel = pingContainer.firstChannelClosestTo(kHz * 1000);
               if (channel > 0) {
                  ImmutableMap<String, Float> channelValues = xmlToParameterValues(channelParameterElement);
                  perChannelValues.put(channel, channelValues);
               }
            }
         });
         setParameters(new SchoolParameters(dataProcessed, values, perChannelValues.build()));
      }
   }

   private static ImmutableMap<String, Float> xmlToParameterValues(Element element) {
      ImmutableMap.Builder<String, Float> values = ImmutableMap.builder();
      element.elements(XML_PARAMETER).forEach(parameterElement -> {
         String key = Utils.intern(parameterElement.attributeValue(XML_PARAMETER_NAME));
         float value = Float.parseFloat(parameterElement.getText());
         values.put(key, value);
      });
      return values.build();
   }

   boolean constrainToLayers() {
      var mask = schoolMaskRepresentation.getSchoolMask();
      LayerManager layerManager = getRegionManager().getLayerManager();
      if (MaskUtils.isContainedIn(mask, layerManager::getBoundaryDepthRange)) {
         return false;
      }
      var newMask = MaskUtils.intersection(mask, layerManager::getBoundaryDepthRange);
      setSchoolMaskRepresentation(newMask);
      return true;
   }

   void constrainAfterUndoRedo() {
      NavigableMap<PingIndex, FloatRangeSet> newMask = constrainMaskToOtherRegions(schoolMaskRepresentation.getSchoolMask());
      setSchoolMaskRepresentation(newMask);
   }

   private static NavigableMap<PingIndex, FloatRangeSet> constrainMask(NavigableMap<PingIndex, FloatRangeSet> mask,
                                                                       @Nullable School thisSchool, RegionManager regionManager) {
      mask = constrainMaskToWritablePings(mask, regionManager);
      mask = constrainMaskToOtherRegions(mask, thisSchool, regionManager);
      return mask;
   }

   private NavigableMap<PingIndex, FloatRangeSet> constrainMaskToOtherRegions(NavigableMap<PingIndex, FloatRangeSet> schoolMask) {
      return constrainMaskToOtherRegions(schoolMask, this, getRegionManager());
   }

   private static NavigableMap<PingIndex, FloatRangeSet> constrainMaskToOtherRegions(NavigableMap<PingIndex, FloatRangeSet> mask,
                                                                                     @Nullable School thisSchool, RegionManager regionManager) {
      PingRange pingRange = PingRange.from(mask, regionManager.getPingContainer());
      List<School> schools = regionManager.getSchoolManager().regionsIntersectingPingRange(pingRange)
            .filter(school -> school != thisSchool)
            .toList();
      LayerManager layerManager = regionManager.getLayerManager();
      NavigableMap<PingIndex, FloatRangeSet> result = new TreeMap<>();
      mask.forEach((pingIndex, value) -> {
         for (School school : schools) {
            value = value.subtract(school.getDepthRanges(pingIndex));
         }
         value = value.intersection(layerManager.getBoundaryDepthRange(pingIndex));
         if (!value.isEmpty()) {
            result.put(pingIndex, value);
         }
      });
      return result;
   }

   private static NavigableMap<PingIndex, FloatRangeSet> constrainMaskToWritablePings(NavigableMap<PingIndex, FloatRangeSet> mask, RegionManager regionManager) {
      NavigableMap<PingIndex, FloatRangeSet> result = new TreeMap<>(mask);
      RangeSet<PingIndex> readOnlyPings = regionManager.getRegionConfiguration().getReadOnlyPings();
      result.keySet().removeIf(readOnlyPings::contains);
      return result;
   }

   public boolean isEmpty() {
      return getCurrentMask().isEmpty();
   }

   @Override
   public PingRange getPingRange() {
      return getCurrentMask().getPingRange();
   }

   NavigableMap<PingIndex, FloatRangeSet> getSchoolMask() {
      return getCurrentMask().getSchoolMask();
   }

   private SchoolMaskRepresentation getCurrentMask() {
      SchoolEditor editor = this.editor;
      return editor != null ? editor.getEditedSchoolMaskRepresentation() : schoolMaskRepresentation;
   }

   public void notifySchoolMaskUpdated(PingRange pingRange) {
      getRegionManager().notifyRegionBoundaryChanged(pingRange, this);
      getRegionManager().notifyLayersInPingRange(pingRange);
   }

   @Override
   float getRepresentativeMinDepth(PingIndex pingIndex) {
      FloatRangeSet rangeSet = getRepresentativeMaskRangeSet(pingIndex);
      return rangeSet != null ? rangeSet.getFloatRanges().getFirst().min() : 0;
   }

   @Override
   float getRepresentativeMaxDepth(PingIndex pingIndex) {
      FloatRangeSet rangeSet = getRepresentativeMaskRangeSet(pingIndex);
      return rangeSet != null ? rangeSet.getFloatRanges().getLast().max() : 0;
   }

   private @Nullable FloatRangeSet getRepresentativeMaskRangeSet(PingIndex pingIndex) {
      NavigableMap<PingIndex, FloatRangeSet> schoolMask = getCurrentMask().getSchoolMask();

      Map.Entry<PingIndex, FloatRangeSet> entry = schoolMask.floorEntry(pingIndex);
      if (entry != null) {
         return entry.getValue();
      }
      entry = schoolMask.ceilingEntry(pingIndex);
      if (entry != null) {
         return entry.getValue();
      }
      return null;
   }

   @Override
   public FloatRangeSet getDepthRanges(PingIndex pingIndex) {
      NavigableMap<PingIndex, FloatRangeSet> mask = getCurrentMask().getSchoolMask();
      FloatRangeSet rangeSet = mask.get(pingIndex);
      return rangeSet != null ? rangeSet : FloatRangeSet.of();
   }

   public List<SchoolBoundaryObject> getBoundaryObjects() {
      return boundaryObjects;
   }

   public @Nullable SchoolEditor getEditor() {
      return editor;
   }

   private SchoolEditor setEditor(SchoolEditor editor) {
      if (this.editor != null) {
         throw new IllegalStateException();
      }
      this.editor = editor;
      return editor;
   }

   public SchoolEditor boundaryDrawStart(EchogramPoint point, EchogramPingSettings pingSettings, EchogramZSettings zSettings) {
      return setEditor(new BoundaryDrawEditor(this, getSchoolManager()::boundaryDrawConfirm, point, pingSettings, zSettings));
   }

   PingRange boundaryDrawConfirm() {
      if (!(editor instanceof BoundaryDrawEditor boundaryDrawEditor)) {
         throw new IllegalStateException();
      }
      editor = null;

      PingRange originalPingRange = schoolMaskRepresentation.getPingRange();
      NavigableMap<PingIndex, FloatRangeSet> originalBoundaryMask = MaskOutlineTracer.createMaskForSingleBoundary(boundaryDrawEditor.getOriginalBoundary());
      NavigableMap<PingIndex, FloatRangeSet> drawnBoundaryMask = MaskOutlineTracer.createMaskForSingleBoundary(boundaryDrawEditor.getDrawnBoundary());
      NavigableMap<PingIndex, FloatRangeSet> drawnInsideMask = MaskUtils.intersection(drawnBoundaryMask, originalBoundaryMask);
      NavigableMap<PingIndex, FloatRangeSet> drawnOutsideMask = MaskUtils.subtract(drawnBoundaryMask, originalBoundaryMask);

      List<NavigableMap<PingIndex, FloatRangeSet>> allDisjointMasks = MaskUtils.toDisjointMasks(schoolMaskRepresentation.getSchoolMask(), getPingContainer());
      Set<NavigableMap<PingIndex, FloatRangeSet>> allFilledDisjointMasks = allDisjointMasks.stream()
            .map(mask -> MaskUtils.fillHoles(mask, getPingContainer()))
            .collect(Collectors.toSet());

      List<NavigableMap<PingIndex, FloatRangeSet>> boundaryDisjointMasks = MaskUtils.toDisjointMasks(originalBoundaryMask, getPingContainer());
      List<NavigableMap<PingIndex, FloatRangeSet>> boundaryFilledDisjointMasks = boundaryDisjointMasks.stream()
            .map(mask -> MaskUtils.fillHoles(mask, getPingContainer()))
            .toList();

      boolean isHole = !allFilledDisjointMasks.containsAll(boundaryFilledDisjointMasks);

      NavigableMap<PingIndex, FloatRangeSet> schoolMask = schoolMaskRepresentation.getSchoolMask();
      if (isHole) {
         schoolMask = MaskUtils.add(schoolMask, drawnInsideMask);
         schoolMask = MaskUtils.subtract(schoolMask, drawnOutsideMask);
      } else {
         schoolMask = MaskUtils.subtract(schoolMask, drawnInsideMask);
         schoolMask = MaskUtils.add(schoolMask, drawnOutsideMask);
      }
      setMask(schoolMask);
      return originalPingRange.union(getPingRange());
   }

   public SchoolEditor boundarySplitStart(EchogramPoint point, EchogramPingSettings pingSettings, EchogramZSettings zSettings) {
      return setEditor(new BoundaryDrawEditor(this, getSchoolManager()::boundarySplitConfirm, point, pingSettings, zSettings));
   }

   List<School> boundarySplitConfirm() {
      if (!(editor instanceof BoundaryDrawEditor boundaryDrawEditor)) {
         throw new IllegalStateException();
      }
      editor = null;

      List<EchogramPoint> drawnBoundary = boundaryDrawEditor.getDrawnBoundary();
      NavigableMap<PingIndex, FloatRangeSet> additionalMask = MaskOutlineTracer.createMaskForSingleBoundary(drawnBoundary);
      if (additionalMask.isEmpty()) {
         return List.of();
      }
      NavigableMap<PingIndex, FloatRangeSet> intersectionMask = MaskUtils.intersection(additionalMask, schoolMaskRepresentation.getSchoolMask());
      NavigableMap<PingIndex, FloatRangeSet> remainingMask = MaskUtils.subtract(schoolMaskRepresentation.getSchoolMask(), intersectionMask);
      setSchoolMaskRepresentation(remainingMask);

      List<NavigableMap<PingIndex, FloatRangeSet>> masks = MaskUtils.toDisjointMasks(intersectionMask, getPingContainer());
      return masks.stream()
            .map(mask -> {
               School newSchool = create(getRegionManager(), mask);
               newSchool.getInterpretation().copyFrom(getInterpretation());
               newSchool.setLabels(getLabels());
               return newSchool;
            })
            .toList();
   }

   public SchoolEditor boxBoundaryMoveStart(EchogramPoint point, EchogramPingSettings pingSettings, EchogramZSettings zSettings) {
      if (!isBoxMode(zSettings.getDepthTransform())) {
         throw new IllegalStateException();
      }
      return setEditor(new BoxBoundaryMoveEditor(this, getSchoolManager()::boxBoundaryMoveConfirm, point, pingSettings, zSettings));
   }

   PingRange boxBoundaryMoveConfirm() {
      if (!(editor instanceof BoxBoundaryMoveEditor boxBoundaryMoveEditor)) {
         throw new IllegalStateException();
      }
      editor = null;

      NavigableMap<PingIndex, FloatRangeSet> schoolMask = boxBoundaryMoveEditor.getEditedSchoolMaskRepresentation().getSchoolMask();
      setMask(schoolMask);
      return getPingRange();
   }

   public SchoolEditor moveStart(EchogramPoint point, EchogramPingSettings pingSettings, EchogramZSettings zSettings) {
      return setEditor(new MoveEditor(this, getSchoolManager()::moveConfirm, point, pingSettings, zSettings));
   }

   PingRange moveConfirm() {
      if (!(editor instanceof MoveEditor moveEditor)) {
         throw new IllegalStateException();
      }
      editor = null;

      NavigableMap<PingIndex, FloatRangeSet> schoolMask = moveEditor.getEditedSchoolMaskRepresentation().getSchoolMask();
      setMask(schoolMask);
      return getPingRange();
   }

   public SchoolEditor scaleStart(EchogramPoint point, EchogramPingSettings pingSettings, EchogramZSettings zSettings) {
      return setEditor(new ScaleEditor(this, getSchoolManager()::scaleConfirm, point, pingSettings, zSettings));
   }

   PingRange scaleConfirm(PingRange clampingPingRange) {
      if (!(editor instanceof ScaleEditor scaleEditor)) {
         throw new IllegalStateException();
      }
      editor = null;

      NavigableMap<PingIndex, FloatRangeSet> schoolMask = scaleEditor.getEditedSchoolMaskRepresentation().getSchoolMask();
      schoolMask = schoolMask.subMap(clampingPingRange.begin(), true, clampingPingRange.end(), false);
      setMask(schoolMask);
      return getPingRange();
   }

   void cancelEdit() {
      SchoolEditor editor = this.editor;
      if (editor == null) {
         return;
      }
      editor.cancel();
      this.editor = null;
      PingRange originalPingRange = schoolMaskRepresentation.getPingRange();
      PingRange editPingRange = editor.getEditedSchoolMaskRepresentation().getPingRange();
      notifySchoolMaskUpdated(originalPingRange.union(editPingRange));
   }

   public boolean isBoxMode(DepthTransform depthTransform) {
      NavigableMap<PingIndex, FloatRangeSet> mask = schoolMaskRepresentation.getSchoolMask();
      if (mask.isEmpty() || mask.lastKey().getPingNumber() - mask.firstKey().getPingNumber() != mask.size() - 1) {
         return false;
      }
      Map.Entry<PingIndex, FloatRangeSet> firstEntry = mask.firstEntry();
      FloatRange firstZRange = depthTransform.depthToZ(firstEntry.getValue().getFloatRanges().getFirst(), firstEntry.getKey());
      for (Map.Entry<PingIndex, FloatRangeSet> entry : mask.entrySet()) {
         List<FloatRange> depthRanges = entry.getValue().getFloatRanges();
         if (depthRanges.size() != 1) {
            return false;
         }
         FloatRange zRange = depthTransform.depthToZ(entry.getValue().getFloatRanges().getFirst(), entry.getKey());
         if (!firstZRange.equals(zRange)) {
            return false;
         }
      }
      return true;
   }

   public @Nullable SchoolBoundaryIntersectionInfo distanceFrom(EchogramPoint point, EchogramPingSettings pingSettings, EchogramZSettings zSettings,
                                                                double closestDistSq) {
      SchoolBoundaryIntersectionInfo bestIntersectionInfo = null;
      for (SchoolBoundaryObject boundaryObject : boundaryObjects) {
         SchoolBoundaryIntersectionInfo intersectionInfo = boundaryObject.getClosestIntersection(this, point, pingSettings, zSettings);
         if (intersectionInfo.distanceSquared() < closestDistSq) {
            closestDistSq = intersectionInfo.distanceSquared();
            bestIntersectionInfo = intersectionInfo;
         }
      }
      return bestIntersectionInfo;
   }

   public SchoolMaskRepresentation getUneditedSchoolMaskRepresentation() {
      return schoolMaskRepresentation;
   }

   NavigableMap<PingIndex, FloatRangeSet> getSchoolMaskRepresentation() {
      return schoolMaskRepresentation.getSchoolMask();
   }

   public @Nullable EchogramPoint getCenterPoint() {
      return centerPoint;
   }

   public static final class SchoolMaskRepresentation {
      private final NavigableMap<PingIndex, FloatRangeSet> schoolMask;
      private final PingRange pingRange;
      private final @Nullable RangeSet<PingIndex> pingRangeSetWithGaps; // null if no gaps.

      public SchoolMaskRepresentation(NavigableMap<PingIndex, FloatRangeSet> schoolMask, PingContainer pingContainer) {
         this.schoolMask = schoolMask;
         pingRange = PingRange.from(schoolMask, pingContainer);
         pingRangeSetWithGaps = schoolMask.size() == pingRange.getPingCount()
               ? null
               : toPingRangeSet(schoolMask, pingContainer);
      }

      private static RangeSet<PingIndex> toPingRangeSet(NavigableMap<PingIndex, FloatRangeSet> schoolMask, PingContainer pingContainer) {
         if (schoolMask.isEmpty()) {
            return RangeUtils.emptyRangeSet();
         }
         RangeSet<PingIndex> rangeSet = new ArrayRangeSet<>();
         PingIndex begin = schoolMask.firstKey();
         PingIndex end = begin;
         for (PingIndex pingIndex : schoolMask.keySet()) {
            if (pingIndex.getPingNumber() > end.getPingNumber() + 1) {
               rangeSet.add(begin, pingContainer.nextOrSame(end));
               begin = pingIndex;
            }
            end = pingIndex;
         }
         rangeSet.add(begin, pingContainer.nextOrSame(end));
         return rangeSet;
      }

      public NavigableMap<PingIndex, FloatRangeSet> getSchoolMask() {
         return schoolMask;
      }

      public PingRange getPingRange() {
         return pingRange;
      }

      private boolean isEmpty() {
         return schoolMask.isEmpty();
      }

      private boolean contains(PingIndex pingIndex) {
         if (pingRangeSetWithGaps == null) {
            return pingRange.contains(pingIndex);
         }
         return pingRangeSetWithGaps.contains(pingIndex);
      }

      private boolean intersectsPingRange(PingRange range) {
         if (pingRangeSetWithGaps == null) {
            return range.intersects(pingRange);
         }
         return pingRangeSetWithGaps.containsAny(range);
      }

      private boolean isReadOnly(RegionManager regionManager) {
         if (pingRangeSetWithGaps == null) {
            return regionManager.isReadOnly(pingRange);
         }
         return pingRangeSetWithGaps.stream().anyMatch(regionManager::isReadOnly);
      }

      private @Nullable EchogramPoint computeCenterPoint(PingContainer pingContainer) {
         if (pingRange.isEmpty()) {
            return null;
         }
         long midPingNumber = (pingRange.end().getPingNumber() + pingRange.begin().getPingNumber()) / 2;
         PingIndex midPingIndex = pingContainer.getPingIndex(midPingNumber);
         Map.Entry<PingIndex, FloatRangeSet> entry = schoolMask.floorEntry(midPingIndex);
         if (entry == null) {
            return null;
         }
         FloatRange largestDepthRange = entry.getValue().stream()
               .max(Comparator.comparing(FloatRange::getSize))
               .orElseThrow();
         return new EchogramPoint(entry.getKey(), largestDepthRange.getCenter());
      }

      private List<SchoolBoundaryObject> computeBoundaryObjects(PingContainer pingContainer) {
         return MaskOutlineTracer.createBoundary(schoolMask, pingContainer).stream()
               .map(boundary -> new SchoolBoundaryObject(boundary, pingContainer))
               .toList();
      }
   }
}

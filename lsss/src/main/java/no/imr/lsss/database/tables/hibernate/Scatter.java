package no.imr.lsss.database.tables.hibernate;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import no.imr.tools.database.ColumnOrder;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Set;

@Entity
@ColumnOrder({
      "nation",
      "platform",
      "survey",
      "object",
      "observationDate",
      "observationTime",
      "frequency",
      "transceiver",
      "scatterType",

      // Properties:
      "observationType",
      "duration",
      "distanceInterval",
      "minBottomDepth",
      "maxBottomDepth",
      "threshold",
      "bubbleCorrection",
      "channelThickness",
      "upperDepth",
      "lowerDepth",
      "sa",
      "quality",
      "pctSa",
      "upperInterpretationDepth",
      "lowerInterpretationDepth",
      "bottomActive",
})
public class Scatter implements BaseSurveyObject<ScatterPK> {
   private ScatterPK compId;

   // Properties:
   private short observationType;
   private int duration;
   private float distanceInterval;
   private float minBottomDepth;
   private float maxBottomDepth;
   private float threshold;
   private float bubbleCorrection;
   private float channelThickness;
   private float upperDepth;
   private float lowerDepth;
   private float sa;
   private float quality;
   private float pctSa;
   private float upperInterpretationDepth;
   private float lowerInterpretationDepth;
   private short bottomActive;

   // Referenced tables:
   private ScatterType scatterType;
   private Observation observation;
   private Set<ScatterData> scatterDatas;
   private ScatterObject scatterObject;

   public Scatter() {
   }

   public Scatter(ScatterPK compId) {
      this.compId = compId;
   }

   public Scatter(ScatterPK compId, short observationType, int duration, float distanceInterval, float minBottomDepth, float maxBottomDepth, float threshold, float bubbleCorrection, float channelThickness, float upperDepth, float lowerDepth, float sa, float quality, float pctSa, float upperInterpretationDepth, float lowerInterpretationDepth, short bottomActive) {
      this.compId = compId;
      this.observationType = observationType;
      this.duration = duration;
      this.distanceInterval = distanceInterval;
      this.minBottomDepth = minBottomDepth;
      this.maxBottomDepth = maxBottomDepth;
      this.threshold = threshold;
      this.bubbleCorrection = bubbleCorrection;
      this.channelThickness = channelThickness;
      this.upperDepth = upperDepth;
      this.lowerDepth = lowerDepth;
      this.sa = sa;
      this.quality = quality;
      this.pctSa = pctSa;
      this.upperInterpretationDepth = upperInterpretationDepth;
      this.lowerInterpretationDepth = lowerInterpretationDepth;
      this.bottomActive = bottomActive;
   }

   @EmbeddedId
   @Override
   public ScatterPK getCompId() {
      return compId;
   }

   @Override
   public void setCompId(ScatterPK compId) {
      this.compId = compId;
   }

   public short getObservationType() {
      return observationType;
   }

   public void setObservationType(short observationType) {
      this.observationType = observationType;
   }

   public int getDuration() {
      return duration;
   }

   public void setDuration(int duration) {
      this.duration = duration;
   }

   public float getDistanceInterval() {
      return distanceInterval;
   }

   public void setDistanceInterval(float distanceInterval) {
      this.distanceInterval = distanceInterval;
   }

   public float getMinBottomDepth() {
      return minBottomDepth;
   }

   public void setMinBottomDepth(float minBottomDepth) {
      this.minBottomDepth = minBottomDepth;
   }

   public float getMaxBottomDepth() {
      return maxBottomDepth;
   }

   public void setMaxBottomDepth(float maxBottomDepth) {
      this.maxBottomDepth = maxBottomDepth;
   }

   public float getThreshold() {
      return threshold;
   }

   public void setThreshold(float threshold) {
      this.threshold = threshold;
   }

   public float getBubbleCorrection() {
      return bubbleCorrection;
   }

   public void setBubbleCorrection(float bubbleCorrection) {
      this.bubbleCorrection = bubbleCorrection;
   }

   public float getChannelThickness() {
      return channelThickness;
   }

   public void setChannelThickness(float channelThickness) {
      this.channelThickness = channelThickness;
   }

   public float getUpperDepth() {
      return upperDepth;
   }

   public void setUpperDepth(float upperDepth) {
      this.upperDepth = upperDepth;
   }

   public float getLowerDepth() {
      return lowerDepth;
   }

   public void setLowerDepth(float lowerDepth) {
      this.lowerDepth = lowerDepth;
   }

   public float getSa() {
      return sa;
   }

   public void setSa(float sa) {
      this.sa = sa;
   }

   public float getQuality() {
      return quality;
   }

   public void setQuality(float quality) {
      this.quality = quality;
   }

   public float getPctSa() {
      return pctSa;
   }

   public void setPctSa(float pctSa) {
      this.pctSa = pctSa;
   }

   public float getUpperInterpretationDepth() {
      return upperInterpretationDepth;
   }

   public void setUpperInterpretationDepth(float upperInterpretationDepth) {
      this.upperInterpretationDepth = upperInterpretationDepth;
   }

   public float getLowerInterpretationDepth() {
      return lowerInterpretationDepth;
   }

   public void setLowerInterpretationDepth(float lowerInterpretationDepth) {
      this.lowerInterpretationDepth = lowerInterpretationDepth;
   }

   public short getBottomActive() {
      return bottomActive;
   }

   public void setBottomActive(short bottomActive) {
      this.bottomActive = bottomActive;
   }

   @ManyToOne(fetch = FetchType.LAZY)
   @MapsId("compId")
   @JoinColumns({
         @JoinColumn(name = "scatterType", referencedColumnName = "scatterType")
   })
   public ScatterType getScatterType() {
      return scatterType;
   }

   public void setScatterType(ScatterType scatterType) {
      this.scatterType = scatterType;
   }

   @ManyToOne(fetch = FetchType.LAZY)
   @MapsId("compId")
   @JoinColumns({
         @JoinColumn(name = "nation", referencedColumnName = "nation"),
         @JoinColumn(name = "platform", referencedColumnName = "platform"),
         @JoinColumn(name = "survey", referencedColumnName = "survey"),
         @JoinColumn(name = "observationDate", referencedColumnName = "observationDate"),
         @JoinColumn(name = "observationTime", referencedColumnName = "observationTime"),
         @JoinColumn(name = "observationType", referencedColumnName = "observationType")
   })
   public Observation getObservation() {
      return observation;
   }

   public void setObservation(Observation observation) {
      this.observation = observation;
   }

   @OneToMany(mappedBy = "scatter")
   public Set<ScatterData> getScatterDatas() {
      return scatterDatas;
   }

   public void setScatterDatas(Set<ScatterData> scatterDatas) {
      this.scatterDatas = scatterDatas;
   }

   @ManyToOne(fetch = FetchType.LAZY)
   @MapsId("compId")
   @JoinColumns({
         @JoinColumn(name = "nation", referencedColumnName = "nation"),
         @JoinColumn(name = "platform", referencedColumnName = "platform"),
         @JoinColumn(name = "survey", referencedColumnName = "survey"),
         @JoinColumn(name = "object", referencedColumnName = "object")
   })
   public ScatterObject getScatterObject() {
      return scatterObject;
   }

   public void setScatterObject(ScatterObject scatterObject) {
      this.scatterObject = scatterObject;
   }

   @Override
   public String toString() {
      return "Scatter{" +
            "compId=" + compId +
            ", observationType=" + observationType +
            ", duration=" + duration +
            ", distanceInterval=" + distanceInterval +
            ", minBottomDepth=" + minBottomDepth +
            ", maxBottomDepth=" + maxBottomDepth +
            ", threshold=" + threshold +
            ", bubbleCorrection=" + bubbleCorrection +
            ", channelThickness=" + channelThickness +
            ", upperDepth=" + upperDepth +
            ", lowerDepth=" + lowerDepth +
            ", sa=" + sa +
            ", quality=" + quality +
            ", pctSa=" + pctSa +
            ", upperInterpretationDepth=" + upperInterpretationDepth +
            ", lowerInterpretationDepth=" + lowerInterpretationDepth +
            ", bottomActive=" + bottomActive +
            '}';
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof Scatter that
            && Objects.equals(compId, that.compId)
            && observationType == that.observationType
            && duration == that.duration
            && Float.floatToIntBits(distanceInterval) == Float.floatToIntBits(that.distanceInterval)
            && Float.floatToIntBits(minBottomDepth) == Float.floatToIntBits(that.minBottomDepth)
            && Float.floatToIntBits(maxBottomDepth) == Float.floatToIntBits(that.maxBottomDepth)
            && Float.floatToIntBits(threshold) == Float.floatToIntBits(that.threshold)
            && Float.floatToIntBits(bubbleCorrection) == Float.floatToIntBits(that.bubbleCorrection)
            && Float.floatToIntBits(channelThickness) == Float.floatToIntBits(that.channelThickness)
            && Float.floatToIntBits(upperDepth) == Float.floatToIntBits(that.upperDepth)
            && Float.floatToIntBits(lowerDepth) == Float.floatToIntBits(that.lowerDepth)
            && Float.floatToIntBits(sa) == Float.floatToIntBits(that.sa)
            && Float.floatToIntBits(quality) == Float.floatToIntBits(that.quality)
            && Float.floatToIntBits(pctSa) == Float.floatToIntBits(that.pctSa)
            && Float.floatToIntBits(upperInterpretationDepth) == Float.floatToIntBits(that.upperInterpretationDepth)
            && Float.floatToIntBits(lowerInterpretationDepth) == Float.floatToIntBits(that.lowerInterpretationDepth)
            && bottomActive == that.bottomActive;
   }

   @Override
   public int hashCode() {
      int result = Objects.hashCode(compId);
      result = 31 * result + observationType;
      result = 31 * result + duration;
      result = 31 * result + Float.floatToIntBits(distanceInterval);
      result = 31 * result + Float.floatToIntBits(minBottomDepth);
      result = 31 * result + Float.floatToIntBits(maxBottomDepth);
      result = 31 * result + Float.floatToIntBits(threshold);
      result = 31 * result + Float.floatToIntBits(bubbleCorrection);
      result = 31 * result + Float.floatToIntBits(channelThickness);
      result = 31 * result + Float.floatToIntBits(upperDepth);
      result = 31 * result + Float.floatToIntBits(lowerDepth);
      result = 31 * result + Float.floatToIntBits(sa);
      result = 31 * result + Float.floatToIntBits(quality);
      result = 31 * result + Float.floatToIntBits(pctSa);
      result = 31 * result + Float.floatToIntBits(upperInterpretationDepth);
      result = 31 * result + Float.floatToIntBits(lowerInterpretationDepth);
      result = 31 * result + bottomActive;
      return result;
   }
}

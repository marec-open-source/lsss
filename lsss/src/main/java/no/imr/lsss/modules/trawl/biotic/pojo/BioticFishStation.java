package no.imr.lsss.modules.trawl.biotic.pojo;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("SpellCheckingInspection")
public final class BioticFishStation {
   @XmlAttribute
   public int serialnumber;
   public int station;
   public @Nullable String stationstartdate;
   public @Nullable String stationstarttime;
   public @Nullable String stationstopdate;
   public @Nullable String stationstoptime;
   public double latitudestart = Double.NaN;
   public double longitudestart = Double.NaN;
   public float fishingdepthmax; // = 0
   public float fishingdepthmin; // = 0
   public @Nullable String gear;
   public @Nullable String gearno;
   public double logstart = Double.NaN;
   public double distance = Double.NaN;
   public @Nullable String gearcondition;
   public @Nullable String samplequality;

   @XmlElement(name = "catchsample")
   public List<BioticCatchSample> catchsamples = new ArrayList<>();

   public BioticFishStation() {
   }

   @Override
   public String toString() {
      return serialnumber + ", " + station + ", " + stationstartdate + ", " + stationstarttime;
   }
}

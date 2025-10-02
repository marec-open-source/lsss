package no.imr.lsss.modules.trawl.biotic.pojo;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("SpellCheckingInspection")
@XmlRootElement(name = "mission")
public final class BioticMission {
   @XmlElement(name = "fishstation")
   public List<BioticFishStation> fishstations = new ArrayList<>();

   public BioticMission() {
   }
}

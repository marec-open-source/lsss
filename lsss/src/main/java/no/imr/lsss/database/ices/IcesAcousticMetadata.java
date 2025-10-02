package no.imr.lsss.database.ices;

import no.imr.tools.parameter.Configurable;
import no.imr.tools.parameter.ListConfigurable;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.StringParameter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class IcesAcousticMetadata extends Configurable {
   public final StringParameter survey = new StringParameter(new Name("Survey"));
   public final StringParameter platform = new StringParameter(new Name("Platform"));
   public final StringParameter organisation = new StringParameter(new Name("Organisation"));
   public final List<IcesInstrument> instruments = new ArrayList<>();
   public final List<IcesCalibration> calibrations = new ArrayList<>();
   public final List<IcesDataAcquisition> dataAcquisitions = new ArrayList<>();
   public final List<IcesDataProcessing> dataProcessings = new ArrayList<>();

   public IcesAcousticMetadata() {
      super(new Name("IcesAcousticMetadata"));
   }

   @Override
   public Collection<? extends Configurable> getSubConfigurables() {
      List<Configurable> configurables = new ArrayList<>();
      configurables.add(survey);
      configurables.add(platform);
      configurables.add(organisation);
      configurables.addAll(getListConfigurables());
      return configurables;
   }

   public Collection<ListConfigurable<? extends IcesGroup>> getListConfigurables() {
      return List.of(
            new ListConfigurable<>(new Name("Instruments"), IcesInstrument::new, instruments),
            new ListConfigurable<>(new Name("Calibrations"), IcesCalibration::new, calibrations),
            new ListConfigurable<>(new Name("DataAcquisitions"), IcesDataAcquisition::new, dataAcquisitions),
            new ListConfigurable<>(new Name("DataProcessings"), IcesDataProcessing::new, dataProcessings)
      );
   }
}

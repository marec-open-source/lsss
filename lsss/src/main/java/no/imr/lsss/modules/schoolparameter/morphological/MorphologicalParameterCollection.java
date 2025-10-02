package no.imr.lsss.modules.schoolparameter.morphological;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.region.RegionManager;
import no.imr.korona.region.School;
import no.imr.lsss.modules.schoolparameter.SchoolParameterCollection;

import java.util.Map;

public interface MorphologicalParameterCollection extends SchoolParameterCollection {
   Map<String, Float> computeValues(DataFileSet dataFileSet, RegionManager regionManager, School school);
}

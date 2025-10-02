package no.imr.lsss.modules.schoolparameter.perchannel;

import no.imr.lsss.modules.schoolparameter.SchoolParameterCollection;

public interface PerChannelParameterCollection extends SchoolParameterCollection {
   PerChannelComputer createComputer();
}

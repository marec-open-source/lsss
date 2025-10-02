package no.imr.lsss.framework.extensions;

import no.imr.lsss.modules.BaseViewModule;
import no.marec.lsss.api.modules.ViewModuleAccess;

final class ViewModuleAccessImpl extends LsssModuleAccessImpl<BaseViewModule> implements ViewModuleAccess {
   ViewModuleAccessImpl(BaseViewModule module) {
      super(module);
   }
}

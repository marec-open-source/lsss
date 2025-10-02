package no.imr.lsss.framework.config.survey.data.remote;

import no.imr.lsss.framework.config.ConfigurationUnit;
import no.imr.lsss.resources.LsssHelp;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.ViewHolder;

import javax.swing.JComponent;
import javax.swing.JTextPane;

final class RemoteDataConfView implements ViewHolder.View {
   private final JComponent mainComponent;

   RemoteDataConfView(RemoteDataConf remoteDataConf) {
      JTextPane info = ConfigurationUnit.createInfoComponent("""
            <h2>Remote directories</h2>
            <p>
               These directories are used when
               <a href="copyRemote">copying remote survey data</a>.
            </p>
            """);
      GuiUtils.addHrefListener(info, href -> {
         switch (href) {
            case "copyRemote" -> LsssHelp.COPY_REMOTE.show();
            default -> {
            }
         }
      });
      mainComponent = GuiUtils.createScrollPane(info, remoteDataConf.createParameterEditor().getEditorComponent());
   }

   @Override
   public JComponent getComponent() {
      return mainComponent;
   }
}

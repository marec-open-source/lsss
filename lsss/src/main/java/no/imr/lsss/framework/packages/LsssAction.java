package no.imr.lsss.framework.packages;

import com.google.common.html.HtmlEscapers;
import no.imr.tools.NoCanDoException;
import no.imr.tools.listening.ChangeManager;
import no.imr.tools.swing.svg.SvgIcon;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

public abstract class LsssAction implements ActionExecutor {
   private LsssPackage lsssPackage = LsssPackage.NO_PACKAGE;
   private final String id;
   private final String label;
   private String toolTipText;
   private @Nullable SvgIcon icon;
   private volatile boolean enabled = true;
   private final ChangeManager changeManager = new ChangeManager();

   LsssAction(String id, String label) {
      this.id = id;
      this.label = label;
      toolTipText = label;
   }

   @Override
   public String toString() {
      return id;
   }

   public LsssPackage getLsssPackage() {
      return lsssPackage;
   }

   void setLsssPackage(LsssPackage lsssPackage) {
      this.lsssPackage = lsssPackage;
   }

   public String getId() {
      return id;
   }

   public String getLabel() {
      return label;
   }

   public String getFullHtmlLabel() {
      return "<html>" + HtmlEscapers.htmlEscaper().escape(label)
            + " <span style='color: gray;'>(" + HtmlEscapers.htmlEscaper().escape(lsssPackage.getLabel()) + ")</span>";
   }

   public String getToolTipText() {
      return toolTipText;
   }

   public LsssAction setToolTipText(String toolTipText) {
      this.toolTipText = toolTipText;
      return this;
   }

   public Optional<SvgIcon> getIcon() {
      return Optional.ofNullable(icon);
   }

   public LsssAction setIcon(@Nullable SvgIcon icon) {
      this.icon = icon;
      return this;
   }

   public void setEnabled(boolean enabled) {
      if (this.enabled != enabled) {
         this.enabled = enabled;
         changeManager.notifyListeners();
      }
   }

   @Override
   public boolean isEnabled() {
      return enabled;
   }

   public ChangeManager getChangeManager() {
      return changeManager;
   }

   @Override
   public void run(ActionArgument argument) {
      if (!enabled) {
         throw new NoCanDoException("Action " + id + " is not enabled");
      }
      doRun(argument);
   }

   protected abstract void doRun(ActionArgument argument);
}

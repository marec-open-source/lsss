package no.imr.lsss.framework.config.application.packages;

import no.imr.lsss.framework.config.application.packages.pojo.ActionReference;
import no.imr.lsss.framework.packages.LsssAction;
import no.imr.lsss.framework.packages.PackageManager;
import no.imr.tools.Utils;
import no.imr.tools.misc.TextFilter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ObjectParameter;
import no.imr.tools.parameter.StringParameter;
import no.imr.tools.parameter.gui.ParameterEditor;
import no.imr.tools.swing.SimpleDocumentListener;
import no.imr.tools.swing.SwingDelayer;
import no.imr.tools.swing.icons.MiscIcons;
import no.imr.tools.swing.svg.SvgIcon;
import org.jspecify.annotations.Nullable;

import javax.swing.JTextField;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

final class ActionParameter extends ObjectParameter<Optional<LsssAction>> {
   private final List<Optional<LsssAction>> allActions;

   ActionParameter(UserDefinedPackage userDefinedPackage, ActionReference actionReference) {
      LsssAction initialAction = userDefinedPackage.uiInfoToLsssAction(actionReference);
      List<Optional<LsssAction>> allActions = toAllActions(userDefinedPackage.getLSSS().getPackageManager(), initialAction);
      this.allActions = allActions;

      super(new Name("Action"), Optional.ofNullable(initialAction), allActions);
   }

   @Override
   public String toString(Optional<LsssAction> value) {
      return value.map(LsssAction::getFullHtmlLabel).orElse("");
   }

   @Override
   public SvgIcon toIcon(Optional<LsssAction> value) {
      return value.flatMap(LsssAction::getIcon).orElse(MiscIcons.EMPTY);
   }

   void init(ParameterEditor parameterEditor, StringParameter filter) {
      JTextField filterComponent = (JTextField) parameterEditor.getInputComponent(filter);
      filterComponent.getDocument().addDocumentListener(new SimpleDocumentListener(_ -> {
         SwingDelayer.invokeLater(filterComponent, () -> {
            String text = filterComponent.getText();
            filter.setValue(text);
            List<Optional<LsssAction>> filteredActions = toFilteredActions(allActions, getValue(), text);
            setAllowedValuesAndValue(filteredActions, getValue());
         });
      }));
   }

   private static List<Optional<LsssAction>> toAllActions(PackageManager packageManager, @Nullable LsssAction initialAction) {
      Stream<Optional<LsssAction>> firstAction = initialAction != null ? Stream.empty() : Stream.of(Optional.empty());
      Stream<Optional<LsssAction>> actions = packageManager.getPackages().stream()
            .flatMap(pack -> pack.getActions().stream())
            .sorted(Utils.comparingIgnoringCase(LsssAction::getLabel))
            .map(Optional::of);
      return Stream.concat(firstAction, actions).toList();
   }

   private static List<Optional<LsssAction>> toFilteredActions(List<Optional<LsssAction>> actions, Optional<LsssAction> currentAction, String text) {
      TextFilter textFilter = new TextFilter(text);
      return actions.stream()
            .filter(value -> {
               if (value.isEmpty() || value.equals(currentAction)) {
                  return true;
               }
               LsssAction action = value.get();
               return textFilter.test(List.of(action.getLsssPackage().getLabel(), action.getLabel()));
            })
            .toList();
   }
}

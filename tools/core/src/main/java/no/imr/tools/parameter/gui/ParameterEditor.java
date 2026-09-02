package no.imr.tools.parameter.gui;

import no.imr.tools.listening.ChangeManager;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.gui.input.GUIConfig;
import no.imr.tools.parameter.gui.input.ParameterGUI;
import no.imr.tools.swing.CurrentInputComponent;
import no.imr.tools.swing.GridBag;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.PopupMenuAdapter;
import no.imr.tools.swing.PopupMenuMouseListener;
import no.imr.tools.swing.ScrollablePanel;
import no.imr.tools.swing.icons.MiscIcons;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An editor for editing the parameters in a ParameterCollection.
 */
public final class ParameterEditor {
   private final List<? extends BaseParameter<?>> parameters;
   private final GUIConfig guiConfig;
   private final ScrollablePanel panel = new ScrollablePanel(new BorderLayout());
   private ParameterEditorData parameterEditorData;
   private final ChangeManager parameterChangeManager = new ChangeManager();

   public ParameterEditor(List<? extends BaseParameter<?>> parameters, GUIConfig guiConfig) {
      this.parameters = parameters;
      this.guiConfig = guiConfig;
      addPopupMenuMouseListener(panel);
      parameterEditorData = createParameterEditorData();
   }

   public ParameterEditor(List<? extends BaseParameter<?>> parameters) {
      this(parameters, new GUIConfig());
   }

   public void addPopupMenuMouseListener(JComponent component) {
      component.addMouseListener(new PopupMenuMouseListener(_ -> makePopupMenu()));
   }

   public ChangeManager getParameterChangeManager() {
      return parameterChangeManager;
   }

   public ParameterEditorData getParameterEditorData() {
      return parameterEditorData;
   }

   public ScrollablePanel getEditorComponent() {
      return panel;
   }

   public JComponent getInputComponent(BaseParameter<?> parameter) {
      return getParameterEditorData().getInputComponent(parameter);
   }

   private void update() {
      parameterEditorData = createParameterEditorData();
   }

   private ParameterEditorData createParameterEditorData() {
      guiConfig.setDoRelayout(this::update);

      AtomicBoolean horizontalFillHasBeenActivated = new AtomicBoolean();
      GridBag gridBag = new GridBag() {
         @Override
         public GridBag activateHorizontalFill() {
            horizontalFillHasBeenActivated.set(true);
            return super.activateHorizontalFill();
         }
      };
      GridBagConstraints constraints = gridBag.getConstraints();
      constraints.insets = new Insets(1, 3, 1, 3);

      ParameterEditorData editorData = new ParameterEditorData(gridBag.getPanel(), guiConfig, parameters);

      for (BaseParameter<?> parameter : parameters) {
         constraints.gridwidth = 1;
         constraints.anchor = GridBagConstraints.WEST;
         gridBag.deactivateFill();

         ParameterGUI<?> parameterGUI = editorData.getParameterGUIs().get(parameter);
         parameterGUI.installGUI(gridBag);
      }

      if (!someParameterHasVerticalFill()) {
         gridBag.addVerticalFiller();
      }

      editorData.getParameterChangeManager().addListener(parameterChangeManager);

      panel.setLayout(horizontalFillHasBeenActivated.get() ? new BorderLayout() : new FlowLayout(FlowLayout.LEFT, 0, 0));
      panel.removeAll();
      panel.add(gridBag.getPanel());
      GuiUtils.validateAndRepaintTopmostParent(panel);

      return editorData;
   }

   public boolean someParameterHasVerticalFill() {
      return parameters.stream().anyMatch(parameter -> parameter.getProperty(BaseParameter.KEY_VERTICAL_FILL));
   }

   public @Nullable JPopupMenu makePopupMenu() {
      if (!CurrentInputComponent.commitEdit()) {
         return null;
      }
      ParameterClipboard parameterClipboard = ParameterClipboard.make(parameters, getParameterEditorData().getParameterGUIs());

      JPopupMenu menu = new JPopupMenu();

      JMenuItem copy = MiscIcons.COPY.on(menu.add("Copy parameters"));
      copy.setToolTipText("Copy parameters to clipboard");
      copy.addActionListener(_ -> ParameterClipboard.doCopy(parameters));

      JMenuItem paste = MiscIcons.PASTE.on(menu.add("Paste parameters"));
      paste.setToolTipText("""
            <html>
            Paste parameters from clipboard
            <br>Parameters with same value are marked <span style='background-color: #c0c0c0'>gray</span>
            <br>Parameters with different value are marked <span style='background-color: #98fb98'>green</span>
            <br>Parameters with invalid value are marked <span style='background-color: #ff6347'>red</span>
            """);
      if (parameterClipboard == null) {
         paste.setEnabled(false);
      } else {
         paste.addActionListener(_ -> {
            parameterClipboard.doPaste();
            update();
         });
         paste.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
               parameterClipboard.highlight();
            }

            @Override
            public void mouseExited(MouseEvent e) {
               parameterClipboard.removeHighlight();
            }
         });
         menu.addPopupMenuListener(new PopupMenuAdapter() {
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
               parameterClipboard.removeHighlight();
            }
         });
      }

      return menu;
   }
}

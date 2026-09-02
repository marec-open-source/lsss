package no.imr.korona.viewer;

import no.imr.korona.data.buffer.PingAnimator;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.icons.MiscIcons;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import java.awt.FlowLayout;

/**
 * Buttons for navigating a {@link PingAnimator}.
 */
public final class EchogramNavigationView {
   private static final int PINGS_PER_SECOND_FAST = 200;
   private static final int PINGS_PER_SECOND_SLOW = 50;

   private final PingAnimator pingAnimator;

   private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));

   private final JToggleButton fastBackward = MiscIcons.FAST_REWIND.withSize(24).on(new JToggleButton());
   private final JToggleButton backward = MiscIcons.PLAY_BACKWARD.withSize(24).on(new JToggleButton());
   private final JToggleButton pause = MiscIcons.PAUSE.withSize(24).on(new JToggleButton());
   private final JToggleButton forward = MiscIcons.PLAY.withSize(24).on(new JToggleButton());
   private final JToggleButton fastForward = MiscIcons.FAST_FORWARD.withSize(24).on(new JToggleButton());
   private final JToggleButton home = MiscIcons.HOME.withSize(24).on(new JToggleButton((String) null, true));

   public EchogramNavigationView(PingAnimator pingAnimator) {
      this.pingAnimator = pingAnimator;
      pingAnimator.getStateChangeManager().addListener(this::updateState);

      fastBackward.addActionListener(_ -> pingAnimator.setPingsPerSecond(-PINGS_PER_SECOND_FAST));
      backward.addActionListener(_ -> pingAnimator.setPingsPerSecond(-PINGS_PER_SECOND_SLOW));
      pause.addActionListener(_ -> pingAnimator.setPingsPerSecond(0));
      forward.addActionListener(_ -> pingAnimator.setPingsPerSecond(PINGS_PER_SECOND_SLOW));
      fastForward.addActionListener(_ -> pingAnimator.setPingsPerSecond(PINGS_PER_SECOND_FAST));
      home.addActionListener(_ -> pingAnimator.setState(PingAnimator.State.Home));

      GuiUtils.createButtonGroup(fastBackward, backward, pause, forward, fastForward, home);

      panel.add(fastBackward);
      panel.add(backward);
      panel.add(pause);
      panel.add(forward);
      panel.add(fastForward);
      panel.add(home);
   }

   public JComponent getComponent() {
      return panel;
   }

   private void updateState(PingAnimator.State state) {
      getButton(state).setSelected(true);
   }

   private JToggleButton getButton(PingAnimator.State state) {
      return switch (state) {
         case Home -> home;
         case Pause -> pause;
         case Backward -> pingAnimator.getPingsPerSecond() == PINGS_PER_SECOND_SLOW ? backward : fastBackward;
         case Forward -> pingAnimator.getPingsPerSecond() == PINGS_PER_SECOND_SLOW ? forward : fastForward;
      };
   }
}

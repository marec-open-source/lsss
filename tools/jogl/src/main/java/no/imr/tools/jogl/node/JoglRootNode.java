package no.imr.tools.jogl.node;

final class JoglRootNode extends JoglGroupNode {
   private final JoglModule joglModule;

   JoglRootNode(JoglModule joglModule) {
      this.joglModule = joglModule;
   }

   @Override
   JoglModule getJoglModule() {
      return joglModule;
   }

   @Override
   public void repaint() {
      joglModule.repaint();
   }
}

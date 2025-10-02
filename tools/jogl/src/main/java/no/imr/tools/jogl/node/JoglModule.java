package no.imr.tools.jogl.node;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.awt.GLJPanel;
import no.imr.tools.math.linalg.Ray;
import no.imr.tools.swing.ViewHolder;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import java.awt.geom.Point2D;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.jogamp.opengl.GL2.*;

public abstract sealed class JoglModule implements GLEventListener permits JoglOrthographicModule, JoglPerspectiveModule {
   private final @Nullable GLContext glContext;
   private final ViewHolder<View> viewHolder = new ViewHolder<>(() -> new View(this));
   private final JoglRootNode rootNode = new JoglRootNode(this);
   private int width;
   private int height;
   private float clearRed = 1;
   private float clearGreen = 1;
   private float clearBlue = 1;
   private boolean needProjection;
   private boolean needInit;
   private final Queue<JoglNode> needDispose = new ConcurrentLinkedQueue<>();

   JoglModule(@Nullable GLContext glContext) {
      this.glContext = glContext;
   }

   public JComponent getComponent() {
      return viewHolder.getComponent();
   }

   public JoglGroupNode getRootNode() {
      return rootNode;
   }

   void nodeAdded() {
      needInit = true;
   }

   void nodeRemoved(JoglNode node) {
      needDispose.add(node);
      if (node instanceof JoglGroupNode groupNode) {
         groupNode.getChildren().forEach(this::nodeRemoved);
      }
   }

   public void setClearColor(float red, float green, float blue) {
      clearRed = red;
      clearGreen = green;
      clearBlue = blue;
   }

   void triggerProjectionUpdate() {
      needProjection = true;
      repaint();
   }

   abstract void setProjection(GL2 gl);

   @Override
   public void init(GLAutoDrawable drawable) {
      init(drawable.getGL().getGL2());
   }

   public void init(GL2 gl) {
      needProjection = true;
      needInit = true;
      gl.glEnable(GL_DEPTH_TEST);
   }

   @Override
   public void display(GLAutoDrawable drawable) {
      GL2 gl = drawable.getGL().getGL2();

      if (needProjection) {
         needProjection = false;
         gl.glMatrixMode(GL_PROJECTION);
         gl.glLoadIdentity();
         setProjection(gl);
         gl.glMatrixMode(GL_MODELVIEW);
      }

      gl.glClearColor(clearRed, clearGreen, clearBlue, 0);
      gl.glClearDepth(1);
      gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

      if (!needDispose.isEmpty()) {
         handleNeedDispose(gl);
      }
      if (needInit) {
         needInit = false;
         rootNode.initTraversal(gl);
      }

      rootNode.drawOpaque(gl);
      rootNode.drawTransparent(gl);
   }

   @Override
   public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
      this.width = width;
      this.height = height;
      needProjection = true;
   }

   @Override
   public void dispose(GLAutoDrawable drawable) {
      GL2 gl = drawable.getGL().getGL2();
      rootNode.disposeTraversal(gl);
      handleNeedDispose(gl);
   }

   private void handleNeedDispose(GL2 gl) {
      while (true) {
         JoglNode node = needDispose.poll();
         if (node == null) {
            return;
         }
         node.disposeTraversal(gl);
      }
   }

   public void repaint() {
      viewHolder.ifView(View::repaint);
   }

   public int getWidth() {
      return width;
   }

   public int getHeight() {
      return height;
   }

   /**
    * Computes a ray in view coordinates.
    *
    * @param pixPos pixel position
    * @return the view ray
    */
   public abstract Ray pixPosToViewRay(Point2D pixPos);

   private static final class View implements ViewHolder.View {
      private final GLJPanel panel = new GLJPanel(null, null);

      private View(JoglModule joglModule) {
         if (joglModule.glContext != null) {
            panel.setSharedContext(joglModule.glContext);
         }
         panel.setFocusable(true);
         panel.addGLEventListener(joglModule);
      }

      @Override
      public JComponent getComponent() {
         return panel;
      }

      private void repaint() {
         panel.repaint();
      }
   }
}

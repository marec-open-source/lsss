package no.imr.lsss.resources;

import no.imr.tools.swing.GuiUtils;

import java.awt.Cursor;
import java.awt.Point;

public final class LsssCursors {
   public static final Cursor FORWARD = GuiUtils.createCursor("no/imr/lsss/resources/images/cursors", "Forward", new Point(21, 15));
   public static final Cursor BACK = GuiUtils.createCursor("no/imr/lsss/resources/images/cursors", "Back", new Point(9, 15));
   public static final Cursor UP = GuiUtils.createCursor("no/imr/lsss/resources/images/cursors", "Up", new Point(15, 9));
   public static final Cursor DOWN = GuiUtils.createCursor("no/imr/lsss/resources/images/cursors", "Down", new Point(15, 21));

   public static final Cursor ZOOM = GuiUtils.createCursor("no/imr/lsss/resources/images/cursors", "Zoom", new Point(9, 9));

   public static final Cursor ADD_HORIZONTAL = GuiUtils.createCursor("no/imr/lsss/resources/images/cursors", "AddHorizontal", new Point(15, 15));
   public static final Cursor ADD_HORIZONTAL_MAX = GuiUtils.createCursor("no/imr/lsss/resources/images/cursors", "AddHorizontalMax", new Point(15, 15));
   public static final Cursor ADD_VERTICAL = GuiUtils.createCursor("no/imr/lsss/resources/images/cursors", "AddVertical", new Point(15, 15));
   public static final Cursor ADD_VERTICAL_MAX = GuiUtils.createCursor("no/imr/lsss/resources/images/cursors", "AddVerticalMax", new Point(15, 15));
   public static final Cursor ADD_BOX = GuiUtils.createCursor("no/imr/lsss/resources/images/cursors", "AddBox", new Point(15, 15));

   public static final Cursor EDIT = GuiUtils.createCursor("no/imr/lsss/resources/images/cursors", "Pencil", new Point(1, 30));
   public static final Cursor ERASE = GuiUtils.createCursor("no/imr/lsss/resources/images/cursors", "Eraser", new Point(6, 24));
   public static final Cursor SPLIT = GuiUtils.createCursor("no/imr/lsss/resources/images/cursors", "Split", new Point(1, 30));
   public static final Cursor SCALE = GuiUtils.createCursor("no/imr/lsss/resources/images/cursors", "Scale", new Point(15, 15));

   public static final Cursor EMPTY = GuiUtils.createCursor("no/imr/lsss/resources/images/cursors", "Empty", new Point(0, 0));

   private LsssCursors() {
   }
}

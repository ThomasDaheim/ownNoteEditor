package tf.ownnote.ui.helper;

import java.util.HashSet;
import java.util.Set;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import tf.ownnote.ui.main.OwnNoteEditor;

/**
 * A draggable tab that can optionally be detached from its tab pane and shown
 * in a separate window. This can be added to any normal TabPane, however a
 * TabPane with draggable tabs must *only* have DraggableTabs, normal tabs and
 * DrragableTabs mixed will cause issues!
 * <p>
 * @author Michael Berry
 */
public class OwnNoteTab extends Tab {

    // callback to OwnNoteEditor required for e.g. delete & rename
    private OwnNoteEditor myEditor= null;
            
    private static final Set<TabPane> tabPanes = new HashSet<>();
    private Label nameLabel = new Label();
    private Text dragText = new Text();
    private static final Stage markerStage;
    private Stage dragStage;
    private boolean detachable;
    
    private String tabName;
    private String tabCount;
    private String tabColor;

    // can this tab be a drop target for notes?
    private boolean droptarget;
    
    private boolean protectedTab = false;
    
    static {
        markerStage = new Stage();
        markerStage.initStyle(StageStyle.UNDECORATED);
        Rectangle dummy = new Rectangle(3, 20, Color.web("#555555"));
        StackPane markerStack = new StackPane();
        markerStack.getChildren().add(dummy);
        markerStage.setScene(new Scene(markerStack));
    }

    /**
     * Create a new draggable tab. This can be added to any normal TabPane,
     * however a TabPane with draggable tabs must *only* have DraggableTabs,
     * normal tabs and DragableTabs mixed will cause issues!
     * <p>
     * @param text the text to appear on the tag label.
     * @param count number of th3 tab in the tab pane
     * @param editor reference to the OwnNoteEditor
     */
    public OwnNoteTab(String text, String count, final OwnNoteEditor editor) {
        nameLabel.setPadding(new Insets(5));
        
        myEditor = editor;
        tabName = text;
        tabCount = count;
        
        setLabelText(text);

        detachable = true;
        dragStage = new Stage();
        dragStage.initStyle(StageStyle.UNDECORATED);
        StackPane dragStagePane = new StackPane();
        dragStagePane.setStyle("-fx-background-color:#DDDDDD;");
        StackPane.setAlignment(dragText, Pos.CENTER);
        dragStagePane.getChildren().add(dragText);
        dragStage.setScene(new Scene(dragStagePane));

        // enabled drag and drop of tabs - but not of all
        nameLabel.setOnMouseDragged((MouseEvent t) -> {
            if (!isProtectedTab()) {
                dragStage.setWidth(nameLabel.getWidth() + 10);
                dragStage.setHeight(nameLabel.getHeight() + 10);
                dragStage.setX(t.getScreenX());
                dragStage.setY(t.getScreenY());
                dragStage.show();
                Point2D screenPoint = new Point2D(t.getScreenX(), t.getScreenY());
                tabPanes.add(getTabPane());
                InsertData data = getInsertData(screenPoint);
                if(data == null || data.getInsertPane().getTabs().isEmpty()) {
                    markerStage.hide();
                } else {
                    int index = data.getIndex();
                    boolean end = false;
                    if(index == data.getInsertPane().getTabs().size()) {
                        end = true;
                        index--;
                    }
                    Rectangle2D rect = getAbsoluteRect(data.getInsertPane().getTabs().get(index));
                    if(end) {
                        markerStage.setX(rect.getMaxX() + 14);
                    }
                    else {
                        markerStage.setX(rect.getMinX() - 4);
                    }
                    markerStage.setY(rect.getMaxY() + 10);
                    markerStage.show();
                }
            }
        });
        nameLabel.setOnMouseReleased((MouseEvent t) -> {
            if (!isProtectedTab()) {
                markerStage.hide();
                dragStage.hide();
                if(!t.isStillSincePress()) {
                    Point2D screenPoint = new Point2D(t.getScreenX(), t.getScreenY());
                    TabPane oldTabPane = getTabPane();
                    int oldIndex = oldTabPane.getTabs().indexOf(OwnNoteTab.this);
                    tabPanes.add(oldTabPane);
                    InsertData insertData = getInsertData(screenPoint);
                    if(insertData != null) {
                        int addIndex = insertData.getIndex();
                        if(oldTabPane == insertData.getInsertPane() && oldTabPane.getTabs().size() == 1) {
                            return;
                        }
                        oldTabPane.getTabs().remove(OwnNoteTab.this);
                        if(oldIndex < addIndex && oldTabPane == insertData.getInsertPane()) {
                            addIndex--;
                        }
                        if(addIndex > insertData.getInsertPane().getTabs().size()) {
                            addIndex = insertData.getInsertPane().getTabs().size();
                        }
                        insertData.getInsertPane().getTabs().add(addIndex, OwnNoteTab.this);
                        insertData.getInsertPane().selectionModelProperty().get().select(addIndex);
                        return;
                    }
                    if(!detachable) {
                        return;
                    }
                    final Stage newStage = new Stage();
                    final TabPane pane = new TabPane();
                    tabPanes.add(pane);
                    newStage.setOnHiding((WindowEvent t1) -> {
                        tabPanes.remove(pane);
                    });
                    getTabPane().getTabs().remove(OwnNoteTab.this);
                    pane.getTabs().add(OwnNoteTab.this);
                    pane.getTabs().addListener((ListChangeListener.Change<? extends Tab> change) -> {
                        if(pane.getTabs().isEmpty()) {
                            newStage.hide();
                        }
                    });
                    newStage.setScene(new Scene(pane));
                    newStage.initStyle(StageStyle.UTILITY);
                    newStage.setX(t.getScreenX());
                    newStage.setY(t.getScreenY());
                    newStage.show();
                    pane.requestLayout();
                    pane.requestFocus();
                }
            }
        });
        
        // act as drag target for notesTable entries
        nameLabel.setOnDragOver((DragEvent event) -> {
            // accept only if dragged from a notesTable row
            // and if not the "ALL" group...
            if (event.getDragboard().hasHtml() &&
                    event.getDragboard().getHtml().equals("notesTable") &&
                    droptarget) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            
            event.consume();
        });

        nameLabel.setOnDragEntered((DragEvent event) -> {
            if (event.getDragboard().hasHtml() &&
                    event.getDragboard().getHtml().equals("notesTable")) {
                //nameLabel.setFill(Color.GREEN);
            }
            
            event.consume();
        });

        nameLabel.setOnDragExited((DragEvent event) -> {
            if (event.getDragboard().hasHtml() &&
                    event.getDragboard().getHtml().equals("notesTable")) {
                //nameLabel.setFill(Color.GREEN);
            }
            
            event.consume();
        });
        
        nameLabel.setOnDragDropped((DragEvent event) -> {
            assert (myEditor != null);
        
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasString()) {
                final NoteData dragNote = NoteData.fromString(db.getString());
                // 1. rename note to new group name
                if (myEditor.moveNoteWrapper(dragNote, getTabName())) {
                    // 2. focus on this tab
                    getTabPane().getSelectionModel().select(this);
                    
                    // TF, 20161105: update tab count on both tabs
                    myEditor.initFromDirectory(true);
                    
                    success = true;
                }
            }
            event.setDropCompleted(success);
            
            event.consume();
        });
    }

    /**
     * Set whether it's possible to detach the tab from its pane and move it to
     * another pane or another window. Defaults to true.
     * <p>
     * @param detachable true if the tab should be detachable, false otherwise.
     */
    public void setDetachable(boolean detachable) {
        this.detachable = detachable;
    }

    public String getTabName() {
        return this.tabName;
    }
    
    public String getTabCount() {
        return tabCount;
    }

    public void setTabCount(final String tabCount) {
        this.tabCount = tabCount;
        
        // and now update label
        setLabelText(tabName);
    }
    
    public String getTabColor() {
        return tabColor;
    }

    public void setTabColor(final String newTabColor) {
        tabColor = newTabColor;
        setStyle("tab-color: " + newTabColor);
    }

    /**
     * Set the label text on this draggable tab. This must be used instead of
     * setText() to set the label, otherwise weird side effects will result!
     * <p>
     * @param text the label text for this tab.
     */
    public void setLabelText(final String text) {
        tabName = text;
        
        if (tabCount != null) {
            nameLabel.setText(text + " ("  + tabCount + ")");
        } else {
            nameLabel.setText(text);
        }
        dragText.setText(text);
        setGraphic(nameLabel);
    }

    public boolean isProtectedTab() {
        return protectedTab;
    }

    public void setProtectedTab(final boolean protectedTab) {
        this.protectedTab = protectedTab;
    }

    public boolean isDroptarget() {
        return droptarget;
    }

    public void setDroptarget(final boolean inDroptarget) {
        droptarget = inDroptarget;
    }

    public Label getLabel() {
        return nameLabel;
    }

    private InsertData getInsertData(Point2D screenPoint) {
        // TF, 20160618
        // in our case we have the 
        // ALL_GROUPS, "Not grouped" tabs to the left 
        // "+" to the right
        // and those shouldn't change their places and you shouldn't be able to move your tab outside of those
        for(TabPane tabPane : tabPanes) {
            Rectangle2D tabAbsolute = getAbsoluteRect(tabPane);
            if(tabAbsolute.contains(screenPoint)) {
                int tabInsertIndex = 0;
                if(!tabPane.getTabs().isEmpty()) {
                    // ALL_GROUPS, "Not grouped" tabs to the left => start with third tab
                    Rectangle2D firstTabRect = getAbsoluteRect(tabPane.getTabs().get(2));
                    if(firstTabRect.getMaxY()+60 < screenPoint.getY() || firstTabRect.getMinY() > screenPoint.getY()) {
                        return null;
                    }
                    // "+" to the right => end with second last tab
                    Rectangle2D lastTabRect = getAbsoluteRect(tabPane.getTabs().get(tabPane.getTabs().size() - 2));
                    if(screenPoint.getX() < (firstTabRect.getMinX() + firstTabRect.getWidth() / 2)) {
                        // we have skipped the first two tabs
                        tabInsertIndex = 2;
                    }
                    else if(screenPoint.getX() > (lastTabRect.getMaxX() - lastTabRect.getWidth() / 2)) {
                        // we have ignored the last tab
                        tabInsertIndex = tabPane.getTabs().size() - 1;
                    }
                    else {
                        // ALL_GROUPS, "Not grouped" tabs to the left => start with third tab
                        // "+" to the right => end with second last tab
                        for(int i = 2; i < tabPane.getTabs().size() - 2; i++) {
                            Tab leftTab = tabPane.getTabs().get(i);
                            Tab rightTab = tabPane.getTabs().get(i + 1);
                            if(leftTab instanceof OwnNoteTab && rightTab instanceof OwnNoteTab) {
                                Rectangle2D leftTabRect = getAbsoluteRect(leftTab);
                                Rectangle2D rightTabRect = getAbsoluteRect(rightTab);
                                if(betweenX(leftTabRect, rightTabRect, screenPoint.getX())) {
                                    tabInsertIndex = i + 1;
                                    break;
                                }
                            }
                        }
                    }
                }
                return new InsertData(tabInsertIndex, tabPane);
            }
        }
        return null;
    }

    private Rectangle2D getAbsoluteRect(Node node) {
        return new Rectangle2D(node.localToScene(node.getLayoutBounds().getMinX(), node.getLayoutBounds().getMinY()).getX() + node.getScene().getWindow().getX(),
                node.localToScene(node.getLayoutBounds().getMinX(), node.getLayoutBounds().getMinY()).getY() + node.getScene().getWindow().getY(),
                node.getLayoutBounds().getWidth(),
                node.getLayoutBounds().getHeight());
    }

    public Rectangle2D getAbsoluteRect(Tab tab) {
        Node node = ((OwnNoteTab) tab).getLabel();
        // loop 2 upwards to TabPaneSkin
        node = node.getParent().getParent();
        return getAbsoluteRect(node);
    }

    private boolean betweenX(Rectangle2D r1, Rectangle2D r2, double xPoint) {
        double lowerBound = r1.getMinX() + r1.getWidth() / 2;
        double upperBound = r2.getMaxX() - r2.getWidth() / 2;
        return xPoint >= lowerBound && xPoint <= upperBound;
    }

    private static class InsertData {

        private final int index;
        private final TabPane insertPane;

        public InsertData(int index, TabPane insertPane) {
            this.index = index;
            this.insertPane = insertPane;
        }

        public int getIndex() {
            return index;
        }

        public TabPane getInsertPane() {
            return insertPane;
        }

    }
}

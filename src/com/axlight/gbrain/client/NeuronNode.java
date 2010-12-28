/* Copyright 2010 Daishi Kato
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.axlight.gbrain.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.vaadin.gwtgraphics.client.DrawingArea;
import org.vaadin.gwtgraphics.client.Group;
import org.vaadin.gwtgraphics.client.Line;
import org.vaadin.gwtgraphics.client.shape.Ellipse;
import org.vaadin.gwtgraphics.client.shape.Rectangle;
import org.vaadin.gwtgraphics.client.shape.Text;

import com.axlight.gbrain.shared.NeuronData;

/**
 * Representation of a node
 * 
 * Used both for logical representation and graphical representation.
 * 
 */
public class NeuronNode extends Group {

	private static final String TEXT_FONT_FAMILY = "Arial Black";
	private static final int TEXT_FONT_SIZE = 12;
	private static final int TEXT_MARGIN_TOP = 1;
	private static final int TEXT_MARGIN_BOTTOM = 5;
	private static final int TEXT_MARGIN_LEFT = 5;
	private static final int TEXT_MARGIN_RIGHT = 6;
	private static final int FOCUS_STROKE_WIDTH = 1;
	private static final int HIGHLIGHT_STROKE_WIDTH = 3;
	private static final int OVERLAP_MARGIN_TOP = 15;
	private static final int OVERLAP_MARGIN_BOTTOM = 15;
	private static final int OVERLAP_MARGIN_LEFT = 15;
	private static final int OVERLAP_MARGIN_RIGHT = 15;

	private static final List<String> COLORS = new ArrayList<String>(Arrays.asList("#aa0000","#550088","#007777","#448800"));
	
	private final long id;
	private Long parentId;
	private final String content;
	private int children;
	private int posX;
	private int posY;
	private int colorIndex;

	private int saveX;
	private int saveY;
	private int viewX;
	private int viewY;

	private final Text text;
	private final int textWidth;
	private final int textHeight;
	private final Rectangle rect;
	private Line parentLine = null;
	private final Map<Long, Line> childLines = new HashMap<Long, Line>();
	private Rectangle focusRect = null;
	private Ellipse highlightEllipse = null;

	public NeuronNode(NeuronData nd, int viewX, int viewY) {
		id = nd.getId();
		parentId = nd.getParentId();
		content = nd.getContent();
		children = nd.getChildren();
		posX = nd.getX();
		posY = nd.getY();
		colorIndex = COLORS.indexOf(nd.getColor());

		saveX = posX;
		saveY = posY;
		this.viewX = viewX;
		this.viewY = viewY;

		text = new Text(0, 0, "");
		updateText();
		text.setFillColor("#ffffff");
		text.setStrokeWidth(0);
		text.setFontFamily(TEXT_FONT_FAMILY);
		text.setFontSize(TEXT_FONT_SIZE);
		textWidth = text.getTextWidth();
		textHeight = text.getTextHeight();

		rect = new Rectangle(0, 0, textWidth + TEXT_MARGIN_LEFT
				+ TEXT_MARGIN_RIGHT, textHeight + TEXT_MARGIN_TOP
				+ TEXT_MARGIN_BOTTOM);
		rect.setFillColor(getColor());
		rect.setStrokeWidth(0);

		setPosition(posX, posY);

		add(rect);
		add(text);
	}

	public long getId() {
		return id;
	}

	public Long getParentId() {
		return parentId;
	}

	public void setParentId(Long parentId) {
		this.parentId = parentId;
	}

	public String getContent() {
		return content;
	}

	public int getChildren() {
		return children;
	}

	public void setChildren(int children) {
		this.children = children;
		updateText();
	}

	private void updateText() {
		text.setText(content + " (" + children + ")");
	}

	public void increaseChildren() {
		children++;
		updateText();
	}

	public void decreaseChildren() {
		children--;
		updateText();
	}

	public String getColor(){
		if(colorIndex == -1){
			return "#888888";
		}else{
			return COLORS.get(colorIndex);
		}
	}
	
	public void setNextColor(){
		colorIndex++;
		if(colorIndex >= COLORS.size()){
			colorIndex = 0;
		}
		rect.setFillColor(getColor());
	}
	
	public void bringToFront() {
		DrawingArea parent = (DrawingArea) getParent();
		parent.bringToFront(this);
	}

	/**
	 * remove this node from the view (graphic model only)
	 */
	public void removeFromParent() {
		DrawingArea parent = (DrawingArea) getParent();
		parent.remove(this);
	}

	/**
	 * call this when clicking this node
	 */
	public void setFocus() {
		if(focusRect != null) {
			return;
		}
		focusRect = new Rectangle(rect.getX() - 1 - FOCUS_STROKE_WIDTH, rect.getY() - 1 - FOCUS_STROKE_WIDTH, textWidth
				+ TEXT_MARGIN_LEFT + TEXT_MARGIN_RIGHT + 2 + FOCUS_STROKE_WIDTH * 2, textHeight
				+ TEXT_MARGIN_TOP + TEXT_MARGIN_BOTTOM + 2 + FOCUS_STROKE_WIDTH * 2);
		focusRect.setFillOpacity(0);
		focusRect.setStrokeColor("#cccccc");
		focusRect.setStrokeWidth(FOCUS_STROKE_WIDTH);
		add(focusRect);
	}

	/**
	 * the opposite of setFocus
	 */
	public void unsetFocus() {
		if (focusRect != null) {
			remove(focusRect);
			focusRect = null;
		}
	}

	/**
	 * call this when mouse-overing this node
	 */
	public void setHighlight() {
		if(highlightEllipse != null){
			return;
		}
		int width = rect.getWidth();
		int height = rect.getHeight();
		highlightEllipse = new Ellipse(rect.getX() + width / 2, rect.getY() + height / 2, width * 5 / 6 + HIGHLIGHT_STROKE_WIDTH * 2, height * 5 / 6 + HIGHLIGHT_STROKE_WIDTH * 2);
		highlightEllipse.setFillOpacity(0);
		highlightEllipse.setStrokeColor("#cccccc");
		highlightEllipse.setStrokeWidth(HIGHLIGHT_STROKE_WIDTH);
		add(highlightEllipse);
	}

	/**
	 * the opposite of setHighlight
	 */
	public void unsetHighlight() {
		if (highlightEllipse != null) {
			remove(highlightEllipse);
			highlightEllipse = null;
		}
	}

	public int getPosX() {
		return posX;
	}

	public int getPosY() {
		return posY;
	}

	public void setPosition(int posX, int posY) {
		this.posX = posX;
		this.posY = posY;
		int x = this.posX - viewX;
		text.setX(x - textWidth / 2);
		rect.setX(x - textWidth / 2 - TEXT_MARGIN_LEFT);
		int y = this.posY - viewY;
		text.setY(y + textHeight / 2);
		rect.setY(y - textHeight / 2 - TEXT_MARGIN_TOP);
		if (focusRect != null) {
			focusRect.setX(rect.getX() - 1 - FOCUS_STROKE_WIDTH);
			focusRect.setY(rect.getY() - 1 - FOCUS_STROKE_WIDTH);
		}
		for (Line l : childLines.values()) {
			l.setX1(x);
			l.setY1(y);
		}
		if (parentLine != null) {
			parentLine.setX2(x);
			parentLine.setY2(y);
		}
	}

	/**
	 * call this when sliding/scrolling the view
	 * 
	 * @param viewX
	 *            x-axis view offset
	 * @param viewY
	 *            y-axis view offset
	 */
	public void updateView(int viewX, int viewY) {
		this.viewX = viewX;
		this.viewY = viewY;
		setPosition(posX, posY);
	}

	/**
	 * call this before dragging
	 */
	public void savePosition() {
		saveX = posX;
		saveY = posY;
	}

	/**
	 * call this while dragging
	 * 
	 * @param offsetX
	 *            x-axis offset from the start of the dragging
	 * @param offsetY
	 *            y-axis offset from the start of the dragging
	 */
	public void dragPosition(int offsetX, int offsetY) {
		setPosition(saveX + offsetX, saveY + offsetY);
	}

	/**
	 * reset dragging
	 */
	public void revertPosition() {
		setPosition(saveX, saveY);
	}

	/**
	 * check if the position is changed from the saved point
	 */
	public boolean isPositionUpdated() {
		return saveX != posX || saveY != posY;
	}

	/**
	 * check if the point is covered by this node
	 * 
	 * @param targetX
	 *            x-axis of screen position
	 * @param targetY
	 *            y-axis of screen position
	 * @return
	 */
	public boolean containsPoint(int targetX, int targetY) {
		int x = this.posX - viewX;
		int y = this.posY - viewY;
		return x - textWidth / 2 - TEXT_MARGIN_LEFT <= targetX
				&& targetX <= x + textWidth / 2 + TEXT_MARGIN_RIGHT
				&& y - textHeight / 2 - TEXT_MARGIN_TOP <= targetY
				&& targetY <= y + textHeight / 2 + TEXT_MARGIN_BOTTOM;
	}
	
	/**
	 * check if the node with new position is covered by this node
	 * 
	 * @param target the target node
	 * @param newPosX the new posX of the node
	 * @param newPosY the new posY of the node
	 * @return
	 */
	public boolean isNewPositionOverlap(NeuronNode target, int newPosX, int newPosY) {
		int x1 = this.posX - this.textWidth / 2 - TEXT_MARGIN_LEFT;
		int x2 = this.posX + this.textWidth / 2 + TEXT_MARGIN_RIGHT;
		int y1 = this.posY - this.textHeight / 2 - TEXT_MARGIN_TOP;
		int y2 = this.posY + this.textHeight / 2 + TEXT_MARGIN_BOTTOM;
		int tx1 = newPosX - target.textWidth / 2 - TEXT_MARGIN_LEFT - OVERLAP_MARGIN_LEFT;
		int tx2 = newPosX + target.textWidth / 2 + TEXT_MARGIN_RIGHT + OVERLAP_MARGIN_RIGHT;
		int ty1 = newPosY - target.textHeight / 2 - TEXT_MARGIN_TOP - OVERLAP_MARGIN_TOP;
		int ty2 = newPosY + target.textHeight / 2 + TEXT_MARGIN_BOTTOM +OVERLAP_MARGIN_BOTTOM;
		return (tx1 <= x2 && x1 <= tx2) &&
		       (ty1 <= y2 && y1 <= ty2);
	}

	public Line getParentLine() {
		return parentLine;
	}

	public void setParentLine(Line line) {
		int x = this.posX - viewX;
		int y = this.posY - viewY;
		line.setX2(x);
		line.setY2(y);
		parentLine = line;
	}

	public void addChildLine(long childId, Line line) {
		int x = this.posX - viewX;
		int y = this.posY - viewY;
		line.setX1(x);
		line.setY1(y);
		childLines.put(childId, line);
	}

	public void removeParentLine() {
		if (parentLine != null) {
			DrawingArea parent = (DrawingArea) getParent();
			if (parent != null) {
				parent.remove(parentLine);
			}
			parentLine = null;
		}
	}

	public void removeChildLine(long childId) {
		Line line = childLines.get(childId);
		if (line != null) {
			DrawingArea parent = (DrawingArea) getParent();
			if (parent != null) {
				parent.remove(line);
			}
			childLines.remove(childId);
		}
	}

}

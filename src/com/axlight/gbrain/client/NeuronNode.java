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

import java.util.HashMap;
import java.util.Map;

import org.vaadin.gwtgraphics.client.DrawingArea;
import org.vaadin.gwtgraphics.client.Group;
import org.vaadin.gwtgraphics.client.Line;
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

	private static final int TEXT_FONT_SIZE = 11;
	private static final int TEXT_MARGIN_TOP = 1;
	private static final int TEXT_MARGIN_BOTTOM = 5;
	private static final int TEXT_MARGIN_LEFT = 4;
	private static final int TEXT_MARGIN_RIGHT = 7;

	private final long id;
	private Long parentId;
	private final String content;
	private int children;
	private int posX;
	private int posY;
	private String color;

	private int saveX;
	private int saveY;
	private int viewX;
	private int viewY;

	private final Text text;
	private final int textWidth;
	private final int textHeight;
	private final Rectangle rect;
	private Line parentLine = null;
	private Rectangle highlight = null;
	private final Map<Long, Line> childLines = new HashMap<Long, Line>();

	public NeuronNode(NeuronData nd, int viewX, int viewY) {
		id = nd.getId();
		parentId = nd.getParentId();
		content = nd.getContent();
		children = nd.getChildren();
		posX = nd.getX();
		posY = nd.getY();
		color = null;
		if (color == null) {
			color = "#888888";
		}

		saveX = posX;
		saveY = posY;
		this.viewX = viewX;
		this.viewY = viewY;

		text = new Text(0, 0, "");
		updateText();
		text.setFillColor("#dddddd");
		text.setStrokeWidth(0);
		text.setFontFamily("Arial");
		text.setFontSize(TEXT_FONT_SIZE);
		textWidth = text.getTextWidth();
		textHeight = text.getTextHeight();

		rect = new Rectangle(0, 0, textWidth + TEXT_MARGIN_LEFT
				+ TEXT_MARGIN_RIGHT, textHeight + TEXT_MARGIN_TOP
				+ TEXT_MARGIN_BOTTOM);
		rect.setFillColor(color);
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
		text.setFillColor("#ffffff");
	}

	/**
	 * the opposite of setFocus
	 */
	public void unsetFocus() {
		text.setFillColor("#dddddd");
	}

	/**
	 * call this when mouse-overing this node
	 */
	public void setHighlight() {
		highlight = new Rectangle(rect.getX() - 2, rect.getY() - 2, textWidth
				+ TEXT_MARGIN_LEFT + TEXT_MARGIN_RIGHT + 4, textHeight
				+ TEXT_MARGIN_TOP + TEXT_MARGIN_BOTTOM + 4);
		highlight.setFillOpacity(0);
		highlight.setStrokeColor("#eeeeee");
		add(highlight);
	}

	/**
	 * the opposite of setHighlight
	 */
	public void unsetHighlight() {
		removeRect();
	}

	private void removeRect() {
		if (highlight != null) {
			remove(highlight);
			highlight = null;
		}
	}

	public int getPosX() {
		return posX;
	}

	public int getPosY() {
		return posY;
	}

	public void setPosition(int posX, int posY) {
		removeRect();
		this.posX = posX;
		this.posY = posY;
		int x = this.posX - viewX;
		text.setX(x - textWidth / 2);
		rect.setX(x - textWidth / 2 - TEXT_MARGIN_LEFT);
		int y = this.posY - viewY;
		text.setY(y + textHeight / 2);
		rect.setY(y - textHeight / 2 - TEXT_MARGIN_TOP);
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

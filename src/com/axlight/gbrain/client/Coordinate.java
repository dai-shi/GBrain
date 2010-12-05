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
import java.util.List;

import org.vaadin.gwtgraphics.client.DrawingArea;
import org.vaadin.gwtgraphics.client.Group;
import org.vaadin.gwtgraphics.client.shape.Circle;

public class Coordinate extends Group {

	private final DrawingArea drawArea;
	private int viewX;
	private int viewY;

	private List<Circle> circles = new ArrayList<Circle>();

	public Coordinate(DrawingArea drawArea, int viewX, int viewY) {
		this.drawArea = drawArea;
		this.viewX = viewX;
		this.viewY = viewY;
		updateCoordinate();
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
		updateCoordinate();
	}

	private static final int RADIUS_STEP = 100;

	private void updateCoordinate() {
		int width = drawArea.getWidth();
		int height = drawArea.getHeight();

		int x1 = viewX * viewX;
		int x2 = (viewX + width) * (viewX + width);
		int y1 = viewY * viewY;
		int y2 = (viewY + height) * (viewY + height);
		final int minX;
		final int minY;
		final int maxX;
		final int maxY;
		if (x1 < x2) {
			minX = x1;
			maxX = x2;
		} else {
			minX = x2;
			maxX = x1;
		}
		if (y1 < y2) {
			minY = y1;
			maxY = y2;
		} else {
			minY = y2;
			maxY = y1;
		}
		int minDist = RADIUS_STEP;
		if (viewX > 0 || viewY > 0 || viewX + width < 0 || viewY + height < 0) {
			if (viewX * (viewX + width) < 0 && viewY * (viewY+width) > 0){
				minDist = (int) Math.sqrt(minY) + RADIUS_STEP;
			}else if (viewX * (viewX + width) > 0 && viewY * (viewY+width) < 0){
				minDist = (int) Math.sqrt(minX) + RADIUS_STEP;
			}else{
				minDist = (int) Math.sqrt(minX + minY) + RADIUS_STEP;
			}
		}
		int maxDist = (int) Math.sqrt(maxX + maxY);

		int index = 0;
		for (int r = (minDist / RADIUS_STEP) * RADIUS_STEP; r < maxDist; r += RADIUS_STEP) {
			Circle circle;
			try {
				circle = circles.get(index);
			} catch (IndexOutOfBoundsException e) {
				circle = newCircle();
				circles.add(circle);
			}
			circle.setX(-viewX);
			circle.setY(-viewY);
			circle.setRadius(r);
			index++;
		}
		for (int i = circles.size() - 1; i >= index; i--) {
			Circle circle = circles.get(i);
			circles.remove(i);
			remove(circle);
		}

	}

	private Circle newCircle() {
		Circle circle = new Circle(0, 0, 0);
		circle.setStrokeWidth(1);
		circle.setStrokeColor("#333333");
		circle.setFillOpacity(0);
		add(circle);
		return circle;
	}

}

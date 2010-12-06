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
import org.vaadin.gwtgraphics.client.Line;

public class Coordinate extends Group {

	private final DrawingArea drawArea;
	private int viewX;
	private int viewY;

	private List<Line> lines = new ArrayList<Line>();

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

	private static final double DEGREE_STEP = Math.PI / 16.0;

	private void updateCoordinate() {
		int width = drawArea.getWidth();
		int height = drawArea.getHeight();

		int x1 = viewX * viewX;
		int x2 = (viewX + width) * (viewX + width);
		int y1 = viewY * viewY;
		int y2 = (viewY + height) * (viewY + height);
		int maxX = x1 > x2 ? x1 : x2;
		int maxY = y1 > y2 ? y1 : y2;
		double radius = Math.sqrt(maxX + maxY);

		int index = 0;
		for (double d = 0; d < Math.PI * 2; d += DEGREE_STEP) {
			Line line;
			try {
				line = lines.get(index);
			} catch (IndexOutOfBoundsException e) {
				line = newLine();
				lines.add(line);
			}
			line.setX1(-viewX);
			line.setY1(-viewY);
			line.setX2(-viewX + (int) (radius * Math.sin(d)));
			line.setY2(-viewY + (int) (radius * Math.cos(d)));
			index++;
		}
		for (int i = lines.size() - 1; i >= index; i--) {
			Line line = lines.get(i);
			lines.remove(i);
			remove(line);
		}

	}

	private Line newLine() {
		Line line = new Line(0, 0, 0, 0);
		line.setStrokeWidth(1);
		line.setStrokeColor("#333333");
		add(line);
		return line;
	}

}

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
import org.vaadin.gwtgraphics.client.shape.Text;

public class Coordinate extends Group{

	private final DrawingArea drawArea;
	private int drawAreaWidth = 0;
	private int drawAreaHeight = 0;
	private int viewX;
	private int viewY;

	//private Text text;
	private List<Line> verticalLines = new ArrayList<Line>();
	private List<Line> horizontalLines = new ArrayList<Line>();

	public Coordinate(DrawingArea drawArea, int viewX, int viewY) {
		this.drawArea = drawArea;
		this.viewX = viewX;
		this.viewY = viewY;

		/*
		text = new Text(0, 0, "");
		text.setFillColor("#555555");
		text.setStrokeWidth(0);
		text.setFontFamily("Arial");
		text.setFontSize(9);
		add(text);
		*/

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

	private static final int LINE_MARGIN = 100;

	private void updateCoordinate() {
		int width = drawArea.getWidth();
		int height = drawArea.getHeight();

		/*
		if (drawAreaWidth != width) {
			text.setX(width / 2);
		}
		if (drawAreaHeight != height) {
			text.setY(height / 2);
		}
		text.setText("(" + (viewX + width / 2) + "," + (viewY + height / 2)
				+ ")");
		*/
		
		int indX=0;
		for (int x = (width / 2 - viewX) % LINE_MARGIN; x < width; x += LINE_MARGIN) {
			Line line;
			try{
				line = verticalLines.get(indX);
			}catch(IndexOutOfBoundsException e){
				line = newLine();
				verticalLines.add(line);
			}
			line.setX1(x);
			line.setY1(0);
			line.setX2(x);
			line.setY2(height);
			indX++;
		}
		for(int i = verticalLines.size() - 1; i >= indX; i--){
			Line line = verticalLines.get(i);
			verticalLines.remove(i);
		    remove(line);
		}

		int indY=0;
		for (int y = (height / 2 - viewY) % LINE_MARGIN; y < height; y += LINE_MARGIN) {
			Line line;
			try{
				line = horizontalLines.get(indY);
			}catch(IndexOutOfBoundsException e){
				line = newLine();
				horizontalLines.add(line);
			}
			line.setX1(0);
			line.setY1(y);
			line.setX2(width);
			line.setY2(y);
			line.setVisible(true);
			indY++;
		}
		for(int i = horizontalLines.size() - 1; i >= indY; i--){
			Line line = horizontalLines.get(i);
			horizontalLines.remove(i);
		    remove(line);
		}

		drawAreaWidth = width;
		drawAreaHeight = height;
	}

	private Line newLine(){
		Line line = new Line(0,0,0,0);
		line.setStrokeWidth(1);
		line.setStrokeColor("#333333");
		insert(line, 0);
		return line;
	}
	

}

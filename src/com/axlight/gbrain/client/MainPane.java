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

import java.util.ConcurrentModificationException;
import java.util.Iterator;

import org.vaadin.gwtgraphics.client.DrawingArea;
import org.vaadin.gwtgraphics.client.Line;
import org.vaadin.gwtgraphics.client.VectorObject;
import org.vaadin.gwtgraphics.client.animation.Animate;
import org.vaadin.gwtgraphics.client.shape.Circle;

import com.axlight.gbrain.shared.FieldVerifier;
import com.axlight.gbrain.shared.NeuronData;
import com.google.gwt.animation.client.Animation;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.ScrollEvent;
import com.google.gwt.user.client.Window.ScrollHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class MainPane extends AbsolutePanel implements ProvidesResize,
		RequiresResize, ScrollHandler {

	private final GBrainServiceAsync gbrainService = GWT
			.create(GBrainService.class);

	private final NodeManager nodeManager;
	private final AsyncCallback<Void> nullCallback;

	private final HorizontalPanel buttonPanel;
	private final DrawingArea drawArea;
	private final Coordinate coordinate;
	private final Circle animationCircle;

	private static final int BUTTON_SIZE = 28;
	private static final int SCREEN_SCALE = 5;

	private int viewX;
	private int viewY;

	public MainPane() {
		nodeManager = new NodeManager();

		nullCallback = new AsyncCallback<Void>() {
			public void onFailure(Throwable caught) {
				GWT.log("Network error!", caught);
				showAlertDialog("Network error!");
			}

			public void onSuccess(Void ignored) {
				// nothing
			}
		};

		Image image;
		if (GBrain.isIPhone) {
			image = new Image("images/create_button.svg");
		} else {
			image = new Image("images/create_button.png");
		}
		PushButton createButton = new PushButton(image, new ClickHandler() {
			public void onClick(ClickEvent event) {
				showCreateDialog();
			}
		});
		createButton.setPixelSize(BUTTON_SIZE, BUTTON_SIZE);
		createButton.setTitle("Create a new text");

		if (GBrain.isIPhone) {
			image = new Image("images/delete_button.svg");
		} else {
			image = new Image("images/delete_button.png");
		}
		PushButton deleteButton = new PushButton(image, new ClickHandler() {
			public void onClick(ClickEvent event) {
				if (selectNode == null) {
					showAlertDialog("Nothing is selected.");
					return;
				}
				if (selectNode.getChildren() > 0) {
					showAlertDialog("You can't delete it with children.");
					return;
				}
				final NeuronNode tmpSelectNode = selectNode;
				String content = tmpSelectNode.getContent();
				final long id = tmpSelectNode.getId();
				showConfirmDialog("Are you sure you want to delete\n'"
						+ content + "' ?", new ClickHandler() {
					public void onClick(ClickEvent event) {
						gbrainService.deleteNeuron(id,
								new AsyncCallback<Void>() {
									public void onFailure(Throwable caught) {
										GWT.log("Network error!", caught);
										showAlertDialog("Network error!");
									}

									public void onSuccess(Void ignored) {
										Long parentId = tmpSelectNode
												.getParentId();
										if (parentId != null) {
											NeuronNode parentNode = nodeManager
													.getNode(parentId);
											if (parentNode != null) {
												parentNode.decreaseChildren();
											}
										}
										removeNode(tmpSelectNode);
									}
								});
					}
				});
			}
		});
		deleteButton.setPixelSize(BUTTON_SIZE, BUTTON_SIZE);
		deleteButton.setTitle("Delete text");

		if (GBrain.isIPhone) {
			image = new Image("images/open_button.svg");
		} else {
			image = new Image("images/open_button.png");
		}
		PushButton openButton = new PushButton(image, new ClickHandler() {
			public void onClick(ClickEvent event) {
				if (selectNode == null) {
					showAlertDialog("Nothing is selected.");
					return;
				}
				refreshChildNeurons(selectNode.getId());
			}
		});
		openButton.setPixelSize(BUTTON_SIZE, BUTTON_SIZE);
		openButton.setTitle("Open children");

		if (GBrain.isIPhone) {
			image = new Image("images/close_button.svg");
		} else {
			image = new Image("images/close_button.png");
		}
		PushButton closeButton = new PushButton(image, new ClickHandler() {
			public void onClick(ClickEvent event) {
				if (selectNode == null) {
					showAlertDialog("Nothing is selected.");
					return;
				}
				removeChildNodes(selectNode);
			}
		});
		closeButton.setPixelSize(BUTTON_SIZE, BUTTON_SIZE);
		closeButton.setTitle("Close children");

		if (GBrain.isIPhone) {
			image = new Image("images/up_button.svg");
		} else {
			image = new Image("images/up_button.png");
		}
		PushButton upButton = new PushButton(image, new ClickHandler() {
			public void onClick(ClickEvent event) {
				Window.alert("to be supported");
			}
		});
		upButton.setPixelSize(BUTTON_SIZE, BUTTON_SIZE);
		upButton.setTitle("Jump to parent");

		if (GBrain.isIPhone) {
			image = new Image("images/down_button.svg");
		} else {
			image = new Image("images/down_button.png");
		}
		PushButton downButton = new PushButton(image, new ClickHandler() {
			public void onClick(ClickEvent event) {
				Window.alert("to be supported");
			}
		});
		downButton.setPixelSize(BUTTON_SIZE, BUTTON_SIZE);
		downButton.setTitle("Jump to a child");

		if (GBrain.isIPhone) {
			image = new Image("images/next_button.svg");
		} else {
			image = new Image("images/next_button.png");
		}
		PushButton nextButton = new PushButton(image, new ClickHandler() {
			public void onClick(ClickEvent event) {
				Window.alert("to be supported");
			}
		});
		nextButton.setPixelSize(BUTTON_SIZE, BUTTON_SIZE);
		nextButton.setTitle("Jump to the next sibling");

		if (GBrain.isIPhone) {
			image = new Image("images/jump_button.svg");
		} else {
			image = new Image("images/jump_button.png");
		}
		PushButton jumpButton = new PushButton(image, new ClickHandler() {
			public void onClick(ClickEvent event) {
				if (selectNode == null) {
					showAlertDialog("Nothing is selected.");
					return;
				}
				jumpToUrl(selectNode);
			}
		});
		jumpButton.setPixelSize(BUTTON_SIZE, BUTTON_SIZE);
		jumpButton.setTitle("Jump to URL");

		if (GBrain.isIPhone) {
			image = new Image("images/color_button.svg");
		} else {
			image = new Image("images/color_button.png");
		}
		PushButton colorButton = new PushButton(image, new ClickHandler() {
			public void onClick(ClickEvent event) {
				Window.alert("to be supported");
			}
		});
		colorButton.setPixelSize(BUTTON_SIZE, BUTTON_SIZE);
		colorButton.setTitle("Change color");

		buttonPanel = new HorizontalPanel();
		buttonPanel.add(createButton);
		buttonPanel.add(deleteButton);
		buttonPanel.add(openButton);
		buttonPanel.add(closeButton);
		buttonPanel.add(upButton);
		buttonPanel.add(downButton);
		buttonPanel.add(nextButton);
		buttonPanel.add(jumpButton);
		buttonPanel.add(colorButton);

		int clientWidth = Window.getClientWidth();
		int clientHeight = Window.getClientHeight();
		int screenWidth = clientWidth * (SCREEN_SCALE * 2 + 1);
		int screenHeight = clientHeight * (SCREEN_SCALE * 2 + 1);

		RootLayoutPanel.get().getElement().getStyle().setRight(
				-clientWidth * SCREEN_SCALE * 2, Unit.PX);
		RootLayoutPanel.get().getElement().getStyle().setBottom(
				-clientHeight * SCREEN_SCALE * 2, Unit.PX);

		drawArea = new DrawingArea(screenWidth, screenHeight);
		drawArea.getElement().setId("gbrain-svgpanel");
		drawArea.getElement().getStyle().setBackgroundColor("#000000");
		viewX = -drawArea.getWidth() / 2;
		viewY = -drawArea.getHeight() / 2;
		this.add(drawArea, 0, 0);
		this.add(buttonPanel, -viewX, -viewY);

		coordinate = new Coordinate(drawArea, viewX, viewY);
		drawArea.add(coordinate);

		animationCircle = new Circle(-10, -10, 3);
		animationCircle.setFillColor("#cccccc");
		animationCircle.setVisible(false);
		drawArea.add(animationCircle);

		Window
				.scrollTo(clientWidth * SCREEN_SCALE, clientHeight
						* SCREEN_SCALE);
		Element welcome = Document.get().getElementById("gbrain-welcome");
		welcome.getStyle().setLeft(clientWidth * SCREEN_SCALE + 20, Unit.PX);
		welcome.getStyle().setTop(clientHeight * SCREEN_SCALE + 50, Unit.PX);

		supportDragAndDrop();
		new LineAnimation();
		refreshTopNeurons();
		// Window.addWindowScrollHandler(this);
	}

	public void onResize() {
		int prevCenterX = drawArea.getWidth() / 2;
		int prevCenterY = drawArea.getHeight() / 2;
		int clientWidth = Window.getClientWidth();
		int clientHeight = Window.getClientHeight();
		int screenWidth = clientWidth * (SCREEN_SCALE * 2 + 1);
		int screenHeight = clientHeight * (SCREEN_SCALE * 2 + 1);
		RootLayoutPanel.get().getElement().getStyle().setRight(
				-clientWidth * SCREEN_SCALE * 2, Unit.PX);
		RootLayoutPanel.get().getElement().getStyle().setBottom(
				-clientHeight * SCREEN_SCALE * 2, Unit.PX);
		viewX += prevCenterX - screenWidth / 2;
		viewY += prevCenterY - screenHeight / 2;
		drawArea.setWidth(screenWidth);
		drawArea.setHeight(screenHeight);
		nodeManager.updateView(viewX, viewY);
		coordinate.updateView(viewX, viewY);
		Window
				.scrollTo(clientWidth * SCREEN_SCALE, clientHeight
						* SCREEN_SCALE);
	}

	// XXX unused
	public void onWindowScroll(ScrollEvent event) {
		Window.alert("onWindowScroll: left=" + event.getScrollLeft() + ",top="
				+ event.getScrollTop());
	}

	private int previousScrollLeft = -1;
	private int previousScrollTop = -1;
	private int secondPreviousScrollLeft = -1;
	private int secondPreviousScrollTop = -1;

	public void onScrollForGBrain(int left, int top) {
		int clientWidth = Window.getClientWidth();
		int clientHeight = Window.getClientHeight();
		if (left <= 0
				|| top <= 0
				|| left >= clientWidth * SCREEN_SCALE * 2
				|| top >= clientHeight * SCREEN_SCALE * 2
				|| (left == previousScrollLeft
						&& left == secondPreviousScrollLeft
						&& top == previousScrollTop && top == secondPreviousScrollTop)) {

			// TODO make some kind of glass effect
			viewX += left - clientWidth * SCREEN_SCALE;
			viewY += top - clientHeight * SCREEN_SCALE;
			nodeManager.updateView(viewX, viewY);
			coordinate.updateView(viewX, viewY);
			Window.scrollTo(clientWidth * SCREEN_SCALE, clientHeight
					* SCREEN_SCALE);
		}
		secondPreviousScrollLeft = previousScrollLeft;
		secondPreviousScrollTop = previousScrollTop;
		previousScrollLeft = left;
		previousScrollTop = top;
	}

	private void showAlertDialog(String message) {
		if (GBrain.isIPhone) {
			Window.alert(message);
		} else {
			final DialogBox dialog = new DialogBox();
			dialog.setModal(true);
			dialog.setGlassEnabled(true);
			dialog.setText("Alert");
			Label label = new Label(message);
			Button close = new Button("Close");
			VerticalPanel basePanel = new VerticalPanel();
			basePanel.setSpacing(10);
			basePanel.add(label);
			basePanel.add(close);
			dialog.add(basePanel);
			close.addClickHandler(new ClickHandler() {
				public void onClick(ClickEvent event) {
					dialog.hide();
				}
			});
			dialog.center();
			close.setFocus(true);
		}
	}

	private void showConfirmDialog(String message, final ClickHandler okHandler) {
		if (GBrain.isIPhone) {
			if (Window.confirm(message)) {
				okHandler.onClick(null);
			}
		} else {
			final DialogBox dialog = new DialogBox();
			dialog.setModal(true);
			dialog.setGlassEnabled(true);
			dialog.setText("Confirm");
			Label label = new Label(message);
			HorizontalPanel buttonPanel = new HorizontalPanel();
			buttonPanel.setSpacing(5);
			Button ok = new Button("OK");
			buttonPanel.add(ok);
			Button cancel = new Button("Cancel");
			buttonPanel.add(cancel);
			VerticalPanel basePanel = new VerticalPanel();
			basePanel.setSpacing(10);
			basePanel.add(label);
			basePanel.add(buttonPanel);
			dialog.add(basePanel);
			ok.addClickHandler(new ClickHandler() {
				public void onClick(ClickEvent event) {
					okHandler.onClick(event);
					dialog.hide();
				}
			});
			cancel.addClickHandler(new ClickHandler() {
				public void onClick(ClickEvent event) {
					dialog.hide();
				}
			});
			dialog.center();
		}
	}

	private interface PromptHandler {
		public void handleResult(String input);
	}

	private void showPromptDialog(String message,
			final PromptHandler promptHandler) {
		if (GBrain.isIPhone) {
			String input = Window.prompt(message, "");
			if (input != null) {
				promptHandler.handleResult(input);
			}
		} else {
			final DialogBox dialog = new DialogBox();
			dialog.setModal(true);
			dialog.setGlassEnabled(true);
			dialog.setText("Prompt");
			Label label = new Label(message);
			final TextBox textBox = new TextBox();
			HorizontalPanel buttonPanel = new HorizontalPanel();
			buttonPanel.setSpacing(5);
			Button ok = new Button("OK");
			buttonPanel.add(ok);
			Button cancel = new Button("Cancel");
			buttonPanel.add(cancel);
			VerticalPanel basePanel = new VerticalPanel();
			basePanel.setSpacing(10);
			basePanel.add(label);
			basePanel.add(textBox);
			basePanel.add(buttonPanel);
			dialog.add(basePanel);
			ok.addClickHandler(new ClickHandler() {
				public void onClick(ClickEvent event) {
					promptHandler.handleResult(textBox.getText());
					dialog.hide();
				}
			});
			cancel.addClickHandler(new ClickHandler() {
				public void onClick(ClickEvent event) {
					dialog.hide();
				}
			});
			dialog.center();
		}
	}

	private void showCreateDialog() {
		String message = "[New ";
		if (selectNode == null) {
			message += "Top";
		} else {
			message += "Child";
		}
		message += "] Enter text in 4~64 chars:";
		showPromptDialog(message, new PromptHandler() {
			public void handleResult(String text) {
				if (!FieldVerifier.isValidContent(text)) {
					showAlertDialog("Must be between 4 chars and 64 chars.\n(currently "
							+ text.length() + " chars)");
					return;
				}
				createNewNeuron(text, selectNode);
			}
		});
	}

	public void createNewNeuron(String content, final NeuronNode parentNode) {
		AsyncCallback<Void> cb = new AsyncCallback<Void>() {
			public void onFailure(Throwable caught) {
				GWT.log("Network error!", caught);
				showAlertDialog("Network error!");
			}

			public void onSuccess(Void ignored) {
				if (parentNode == null) {
					refreshTopNeurons();
				} else {
					parentNode.increaseChildren();
					refreshChildNeurons(parentNode.getId());
				}
			}
		};

		if (parentNode == null) {
			int x = viewX + buttonPanel.getAbsoluteLeft();
			int y = viewY + buttonPanel.getAbsoluteTop() + 50;
			gbrainService.addNeuron(content, x, y, cb);
		} else {
			int x = parentNode.getPosX() + 80;
			int y = parentNode.getPosY() + 40 + 40 * parentNode.getChildren();
			gbrainService.addNeuron(parentNode.getId(), content, x, y, cb);
		}
	}

	private void refreshTopNeurons() {
		gbrainService.getTopNeurons(new AsyncCallback<NeuronData[]>() {
			public void onFailure(Throwable caught) {
				GWT.log("Network error!", caught);
				showAlertDialog("Network error!");
			}

			public void onSuccess(NeuronData[] neurons) {
				for (NeuronData n : neurons) {
					addNode(n);
				}
			}
		});
	}

	private void refreshChildNeurons(long parent) {
		gbrainService.getChildNeurons(parent,
				new AsyncCallback<NeuronData[]>() {
					public void onFailure(Throwable caught) {
						GWT.log("Network error!", caught);
						showAlertDialog("Network error!");
					}

					public void onSuccess(NeuronData[] neurons) {
						for (NeuronData n : neurons) {
							addNode(n);
						}
					}
				});
	}

	private NeuronNode selectNode = null;
	private NeuronNode dragNode = null;
	private int dragStartX = 0;
	private int dragStartY = 0;
	private NeuronNode dragOverNode = null;

	private boolean sliding = false;
	private int slideStartX = 0;
	private int slideStartY = 0;

	private void startDrag(NeuronNode n, int eventX, int eventY) {
		dragNode = n;
		dragStartX = eventX;
		dragStartY = eventY;
		savePositionNodeAndChildNodes(dragNode);
	}

	private void stopDrag() {
		if (dragNode != null) {
			if (dragOverNode != null) {
				long newparent = dragOverNode.getId();
				if (dragNode.getParentId() == null
						|| dragNode.getParentId() != newparent) {
					revertPositionNodeAndChildNodes(dragNode);
					replaceParent(dragNode, newparent);
					gbrainService.updateParent(dragNode.getId(), newparent,
							nullCallback);
				}
				dragOverNode.unsetHighlight();
				dragOverNode = null;
			} else {
				if (dragNode.isPositionUpdated()) {
					updatePositionNodeAndChildNodes(dragNode);
				}
			}
		}
		dragNode = null;
	}

	public void updateDrag(int eventX, int eventY) {
		if (dragNode != null) {
			int offsetX = eventX - dragStartX;
			int offsetY = eventY - dragStartY;
			dragPositionNodeAndChildNodes(dragNode, offsetX, offsetY);
			checkDragOver(eventX, eventY);
		}
	}

	private void supportDragAndDrop() {
		drawArea.addMouseDownHandler(new MouseDownHandler() {
			public void onMouseDown(MouseDownEvent event) {
				cacnelRelocateButtonPannel();
				int eventX = event.getX();
				int eventY = event.getY();
				if (selectNode != null
						&& selectNode.containsPoint(eventX, eventY)) {
					startDrag(selectNode, eventX, eventY);
				} else {
					sliding = true;
					slideStartX = eventX;
					slideStartY = eventY;
				}
			}
		});
		drawArea.addMouseUpHandler(new MouseUpHandler() {
			public void onMouseUp(MouseUpEvent event) {
				stopDrag();
				sliding = false;
			}
		});
		drawArea.addMouseMoveHandler(new MouseMoveHandler() {
			public void onMouseMove(MouseMoveEvent event) {
				int eventX = event.getX();
				int eventY = event.getY();
				updateDrag(eventX, eventY);
				if (sliding) {
					int left = Window.getScrollLeft();
					int top = Window.getScrollTop();
					left -= eventX - slideStartX;
					top -= eventY - slideStartY;
					Window.scrollTo(left, top);
				}
			}
		});
		drawArea.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				int eventX = event.getX();
				int eventY = event.getY();
				if (selectNode == null
						|| !selectNode.containsPoint(eventX, eventY)) {
					handleNodeClick(null);
				}
				handleDrawAreaClick(eventX, eventY);
			}
		});
	}

	public boolean onTouchStartForGBrain(int eventX, int eventY) {
		cacnelRelocateButtonPannel();
		if (selectNode != null && selectNode.containsPoint(eventX, eventY)) {
			startDrag(selectNode, eventX, eventY);
			return false;
		} else {
			return true;
		}
	}

	public boolean onTouchMoveForGBrain(int eventX, int eventY) {
		if (dragNode != null) {
			updateDrag(eventX, eventY);
			return false;
		} else {
			return true;
		}
	}

	public boolean onTouchEndForGBrain() {
		if (dragNode != null) {
			stopDrag();
			return false;
		} else {
			return true;
		}
	}

	private Timer relocateButtonPannelTimer = null;

	private void handleDrawAreaClick(final int eventX, final int eventY) {
		relocateButtonPannelTimer = new Timer() {
			public void run() {
				setWidgetPosition(buttonPanel, eventX + 10, eventY - 43);
			}
		};
		relocateButtonPannelTimer.schedule(1000);
	}

	private void cacnelRelocateButtonPannel() {
		if (relocateButtonPannelTimer != null) {
			relocateButtonPannelTimer.cancel();
			relocateButtonPannelTimer = null;
		}
	}

	private void handleNodeClick(NeuronNode n) {
		if (selectNode != n) {
			if (selectNode != null) {
				selectNode.unsetFocus();
			}
			selectNode = n;
			if (selectNode != null) {
				selectNode.setFocus();
				selectNode.bringToFront();
			}
		}
	}

	private void handleNodeDoubleClick(NeuronNode n) {
		if (nodeManager.hasAnyChildNodes(n.getId())) {
			removeChildNodes(n);
		} else {
			refreshChildNeurons(n.getId());
		}
	}

	private void addNode(NeuronData nd) {
		final long myid = nd.getId();
		NeuronNode n = nodeManager.getNode(myid);
		if (n != null) {
			n.setChildren(nd.getChildren());
			n.setPosition(nd.getX(), nd.getY());
			if (n.getParentId() != nd.getParentId()) {
				replaceParent(n, nd.getParentId());
			}
			return;
		}

		final NeuronNode node = new NeuronNode(nd, viewX, viewY);
		node.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				handleNodeClick(node);
			}
		});
		node.addDoubleClickHandler(new DoubleClickHandler() {
			public void onDoubleClick(DoubleClickEvent event) {
				handleNodeDoubleClick(node);
			}
		});

		nodeManager.addNode(node);
		drawArea.add(node);

		addParentLine(node);
	}

	private void addParentLine(NeuronNode n) {
		Long parentId = n.getParentId();
		if (parentId == null) {
			return;
		}
		NeuronNode parentNode = nodeManager.getNode(parentId);
		if (parentNode == null) {
			return;
		}
		Line line = new Line(0, 0, 0, 0);
		line.setStrokeColor("#ffffff");
		parentNode.addChildLine(n.getId(), line);
		n.setParentLine(line);
		drawArea.insert(line, 1);
		new Animate(line, "strokewidth", 4, 1, 1000).start();
	}

	private void removeParentLine(NeuronNode n) {
		Long parentId = n.getParentId();
		if (parentId == null) {
			return;
		}
		NeuronNode parentNode = nodeManager.getNode(parentId);
		if (parentNode == null) {
			return;
		}
		n.removeParentLine();
		parentNode.removeChildLine(n.getId());
	}

	private void removeNode(NeuronNode n) {
		removeParentLine(n);
		n.removeFromParent();

		// remove child lines
		for (NeuronNode tmp : nodeManager.getChildNodes(n.getId())) {
			tmp.setParentId(null);
			tmp.removeParentLine();
			n.removeChildLine(tmp.getId());
		}

		nodeManager.removeNode(n.getId());
	}

	private void removeChildNodes(NeuronNode n) {
		for (NeuronNode tmp : nodeManager.getChildNodes(n.getId())) {
			removeChildNodes(tmp);
			removeNode(tmp);
		}
	}

	private void savePositionNodeAndChildNodes(NeuronNode n) {
		n.savePosition();
		for (NeuronNode tmp : nodeManager.getChildNodes(n.getId())) {
			savePositionNodeAndChildNodes(tmp);
		}
	}

	private void dragPositionNodeAndChildNodes(NeuronNode n, int offsetX,
			int offsetY) {
		n.dragPosition(offsetX, offsetY);
		for (NeuronNode tmp : nodeManager.getChildNodes(n.getId())) {
			dragPositionNodeAndChildNodes(tmp, offsetX, offsetY);
		}
	}

	private void revertPositionNodeAndChildNodes(NeuronNode n) {
		n.revertPosition();
		for (NeuronNode tmp : nodeManager.getChildNodes(n.getId())) {
			revertPositionNodeAndChildNodes(tmp);
		}
	}

	private void updatePositionNodeAndChildNodes(NeuronNode n) {
		long myid = n.getId();
		int x = n.getPosX();
		int y = n.getPosY();
		gbrainService.updatePosition(myid, x, y, nullCallback);

		for (NeuronNode tmp : nodeManager.getChildNodes(n.getId())) {
			updatePositionNodeAndChildNodes(tmp);
		}
	}

	private static final RegExp URL_REGEX = RegExp
			.compile("(https?|ftp)://(www\\.)?(((([a-zA-Z0-9.-]+\\.){1,}[a-zA-Z]{2,4}|localhost))|((\\d{1,3}\\.){3}(\\d{1,3})))(:(\\d+))?(/([a-zA-Z0-9-._~!$&'()*+,;=:@/]|%[0-9A-F]{2})*)?(\\?([a-zA-Z0-9-._~!$&'()*+,;=:/?@]|%[0-9A-F]{2})*)?(#([a-zA-Z0-9._-]|%[0-9A-F]{2})*)?");

	private void jumpToUrl(NeuronNode n) {
		String text = n.getContent();
		MatchResult match = URL_REGEX.exec(text);
		if (match.getGroupCount() > 0) {
			Window.open(match.getGroup(0), "_blank", "");
		}
	}

	private void replaceParent(NeuronNode n, Long newParentId) {
		Long parentId = n.getParentId();
		if (parentId != null) {
			NeuronNode parentNode = nodeManager.getNode(parentId);
			parentNode.decreaseChildren();
		}
		removeParentLine(n);

		n.setParentId(newParentId);
		if (newParentId != null) {
			NeuronNode newParentNode = nodeManager.getNode(newParentId);
			if (newParentNode != null) {
				newParentNode.increaseChildren();
				addParentLine(n);
			}
		}
	}

	private void checkDragOver(int x, int y) {
		NeuronNode n = findNodeByPosition(x, y, dragNode);
		if (n != null) {
			if (dragOverNode != n) {
				if (dragOverNode != null) {
					dragOverNode.unsetHighlight();
				}
				dragOverNode = n;
				dragOverNode.setHighlight();
			}
		} else if (dragOverNode != null) {
			dragOverNode.unsetHighlight();
			dragOverNode = null;
		}
	}

	public NeuronNode findNodeByPosition(int x, int y, NeuronNode excludeNode) {
		// XXX this could be slow with the number of nodes.
		for (int i = drawArea.getVectorObjectCount() - 1; i >= 0; i--) {
			VectorObject vo = drawArea.getVectorObject(i);
			if (vo instanceof NeuronNode) {
				NeuronNode n = (NeuronNode) vo;
				if (n != excludeNode && n.containsPoint(x, y)) {
					return n;
				}
			}
		}
		return null;
	}

	private Iterator<NeuronNode> animationIte = null;

	private class LineAnimation extends Animation {

		private Line line = null;

		public LineAnimation() {
			try {
				if (animationIte == null || !animationIte.hasNext()) {
					animationIte = nodeManager.getAllNodes().iterator();
				}
				while (animationIte.hasNext()) {
					line = animationIte.next().getParentLine();
					if (line != null) {
						break;
					}
				}
			} catch (ConcurrentModificationException e) {
				animationIte = null;
				line = null;
			}
			if (line == null) {
				run(500);
			} else {
				run(3000);
			}
		}

		protected void onStart() {
			if (line != null && dragNode == null && sliding == false) {
				animationCircle.setX(line.getX1());
				animationCircle.setY(line.getY1());
				animationCircle.setVisible(true);
			}
		}

		protected void onUpdate(double progress) {
			if (line != null) {
				if (line.getParent() == null || dragNode != null || sliding) {
					animationCircle.setVisible(false);
					line = null;
				} else {
					progress = interpolate(progress);
					animationCircle.setX((int) (line.getX1() + progress
							* (line.getX2() - line.getX1())));
					animationCircle.setY((int) (line.getY1() + progress
							* (line.getY2() - line.getY1())));
				}
			}
		}

		protected void onComplete() {
			if (line != null) {
				animationCircle.setVisible(false);
			}
			new LineAnimation();
		}

	}

	// TODO (Middle) color selection (update color)
	// TODO (Middle) NodeManager child list w/ parent-index
	// TODO (Middle) jump child/parent button, auto-scroll to a child position
	// TODO (Middle) jump siblings button (top siblings)
	// TODO (Middle) slide animation
	// TODO (Low) open all children
	// TODO (Low) Re-position child nodes
	// TODO (Low) auto-scroll to a certain position
	// TODO (Low) search text and auto-scroll
	// TODO (Low) channel to update immediately
	// TODO (Low) Move to trash rather than delete
	// TODO (Low) progress indicator
	// TODO (Low) unlink parent button (make it as top)
	// TODO (Idea) Land
	// TODO (Idea) submit from twitter
	// TODO (Future) separated DragAndDropSupport.java (reusable version)
	// http://developer.apple.com/library/safari/#documentation/AppleApplications/Reference/SafariWebContent/HandlingEvents/HandlingEvents.html#//apple_ref/doc/uid/TP40006511-SW1

}

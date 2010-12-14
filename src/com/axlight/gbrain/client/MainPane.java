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
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Style.Visibility;
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
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class MainPane extends AbsolutePanel implements ProvidesResize,
		RequiresResize, ScrollHandler {

	private final GBrainServiceAsync gbrainService = GWT
			.create(GBrainService.class);

	private final NodeManager nodeManager;
	private final AsyncCallback<Void> nullCallback;

	private final SimplePanel borderNorth;
	private final SimplePanel borderEast;
	private final SimplePanel borderSouth;
	private final SimplePanel borderWest;
	private final FlowPanel buttonPanel;
	private final DrawingArea drawArea;
	private final Coordinate coordinate;

	private static final int BUTTON_PANEL_MARGIN = 20;
	private static final int BUTTON_SIZE = 28;
	private static final int VIEW_SCREEN_SCALE = 11;

	private int viewX = 0; // means viewOffsetX
	private int viewY = 0; // means viewOffsetY
	private int viewWidth = 0;
	private int viewHeight = 0;

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

		ClickHandler borderClickHandler = new ClickHandler() {
			public void onClick(ClickEvent event) {
				int left = getWindowScrollLeft();
				int top = getWindowScrollTop();
				int screenWidth = getWindowScreenWidth();
				int screenHeight = getWindowScreenHeight();
				int prevCenterPosX = viewX + left + screenWidth / 2;
				int prevCenterPosY = viewY + top + screenHeight / 2;
				relocateCenter(prevCenterPosX, prevCenterPosY);
			}
		};

		borderNorth = new SimplePanel();
		borderNorth.addDomHandler(borderClickHandler, ClickEvent.getType());
		borderNorth.getElement().getStyle().setBackgroundColor("#445566");
		borderEast = new SimplePanel();
		borderEast.addDomHandler(borderClickHandler, ClickEvent.getType());
		borderEast.getElement().getStyle().setBackgroundColor("#445566");
		borderSouth = new SimplePanel();
		borderSouth.addDomHandler(borderClickHandler, ClickEvent.getType());
		borderSouth.getElement().getStyle().setBackgroundColor("#445566");
		borderWest = new SimplePanel();
		borderWest.addDomHandler(borderClickHandler, ClickEvent.getType());
		borderWest.getElement().getStyle().setBackgroundColor("#445566");

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
				if (selectNode == null) {
					showAlertDialog("Nothing is selected.");
					return;
				}
				NeuronNode n = nodeManager.getParentNode(selectNode.getId());
				if (n == null) {
					return;
				}
				handleNodeClick(n);
				slideToPosition(n.getPosX(), n.getPosY());
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
				if (selectNode == null) {
					showAlertDialog("Nothing is selected.");
					return;
				}
				NeuronNode n = nodeManager
						.getFirstChildNode(selectNode.getId());
				if (n == null) {
					return;
				}
				handleNodeClick(n);
				slideToPosition(n.getPosX(), n.getPosY());
			}
		});
		downButton.setPixelSize(BUTTON_SIZE, BUTTON_SIZE);
		downButton.setTitle("Jump to a child");

		if (GBrain.isIPhone) {
			image = new Image("images/prev_button.svg");
		} else {
			image = new Image("images/prev_button.png");
		}
		PushButton prevButton = new PushButton(image, new ClickHandler() {
			public void onClick(ClickEvent event) {
				if (selectNode == null) {
					showAlertDialog("Nothing is selected.");
					return;
				}
				NeuronNode n = nodeManager.getPreviousSiblingNode(selectNode
						.getId());
				if (n == null) {
					return;
				}
				handleNodeClick(n);
				slideToPosition(n.getPosX(), n.getPosY());
			}
		});
		prevButton.setPixelSize(BUTTON_SIZE, BUTTON_SIZE);
		prevButton.setTitle("Jump to previous sibling");

		if (GBrain.isIPhone) {
			image = new Image("images/next_button.svg");
		} else {
			image = new Image("images/next_button.png");
		}
		PushButton nextButton = new PushButton(image, new ClickHandler() {
			public void onClick(ClickEvent event) {
				if (selectNode == null) {
					showAlertDialog("Nothing is selected.");
					return;
				}
				NeuronNode n = nodeManager.getNextSiblingNode(selectNode
						.getId());
				if (n == null) {
					return;
				}
				handleNodeClick(n);
				slideToPosition(n.getPosX(), n.getPosY());
			}
		});
		nextButton.setPixelSize(BUTTON_SIZE, BUTTON_SIZE);
		nextButton.setTitle("Jump to next sibling");

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
				if (selectNode == null) {
					showAlertDialog("Nothing is selected.");
					return;
				}
				final NeuronNode tmpSelectNode = selectNode;
				tmpSelectNode.setNextColor();
				final String color = tmpSelectNode.getColor();
				final long id = tmpSelectNode.getId();
				gbrainService.updateColor(id, color, nullCallback);
			}
		});
		colorButton.setPixelSize(BUTTON_SIZE, BUTTON_SIZE);
		colorButton.setTitle("Change color");

		buttonPanel = new FlowPanel();
		buttonPanel.add(createButton);
		buttonPanel.add(deleteButton);
		buttonPanel.add(openButton);
		buttonPanel.add(closeButton);
		buttonPanel.add(upButton);
		buttonPanel.add(downButton);
		buttonPanel.add(prevButton);
		buttonPanel.add(nextButton);
		buttonPanel.add(jumpButton);
		buttonPanel.add(colorButton);

		drawArea = new DrawingArea(0, 0);
		drawArea.getElement().setId("gbrain-svgpanel");
		drawArea.getElement().getStyle().setBackgroundColor("#000000");

		this.add(drawArea, 0, 0);
		this.add(borderNorth, -100, -100); // initially not visible
		this.add(borderEast, -100, -100); // initially not visible
		this.add(borderSouth, -100, -100); // initially not visible
		this.add(borderWest, -100, -100); // initially not visible
		this.add(buttonPanel, -100, -100); // initially not visible

		coordinate = new Coordinate(drawArea);
		drawArea.add(coordinate);

		supportDragAndDrop();
		refreshTopNeurons();

		onResize();
		Element welcome = Document.get().getElementById("gbrain-welcome");
		welcome.getStyle().setLeft(
				viewWidth / 2 - welcome.getClientWidth() / 2, Unit.PX);
		welcome.getStyle().setTop(
				viewHeight / 2 - welcome.getClientHeight() / 2, Unit.PX);
		Window.addWindowScrollHandler(this);
	}

	public void onResize() {
		int prevCenterPosX = viewX + viewWidth / 2;
		int prevCenterPosY = viewY + viewHeight / 2;
		int screenWidth = getWindowScreenWidth();
		int screenHeight = getWindowScreenHeight();
		viewWidth = screenWidth * VIEW_SCREEN_SCALE;
		viewHeight = screenHeight * VIEW_SCREEN_SCALE;
		Element root = RootLayoutPanel.get().getElement();
		root.getStyle().setRight(root.getClientWidth() - viewWidth, Unit.PX);
		root.getStyle().setBottom(root.getClientHeight() - viewHeight, Unit.PX);
		buttonPanel.setWidth(""
				+ (screenWidth - BUTTON_PANEL_MARGIN - BUTTON_SIZE) + "px");
		drawArea.setWidth(viewWidth);
		drawArea.setHeight(viewHeight);

		borderNorth.setPixelSize(viewWidth, BUTTON_PANEL_MARGIN);
		setWidgetPosition(borderNorth, 0, 0);
		borderEast.setPixelSize(BUTTON_PANEL_MARGIN, viewHeight
				- BUTTON_PANEL_MARGIN * 2);
		setWidgetPosition(borderEast, viewWidth - BUTTON_PANEL_MARGIN,
				BUTTON_PANEL_MARGIN);
		borderSouth.setPixelSize(viewWidth, BUTTON_PANEL_MARGIN);
		setWidgetPosition(borderSouth, 0, viewHeight - BUTTON_PANEL_MARGIN);
		borderWest.setPixelSize(BUTTON_PANEL_MARGIN, viewHeight
				- BUTTON_PANEL_MARGIN * 2);
		setWidgetPosition(borderWest, 0, BUTTON_PANEL_MARGIN);

		relocateCenter(prevCenterPosX, prevCenterPosY);
	}

	private void relocateCenter(int posX, int posY) {
		final Style glassStyle = Document.get().getElementById("gbrain-glass")
				.getStyle();
		glassStyle.setWidth(viewWidth, Unit.PX);
		glassStyle.setHeight(viewHeight, Unit.PX);
		glassStyle.setVisibility(Visibility.VISIBLE);
		viewX = posX - viewWidth / 2;
		viewY = posY - viewHeight / 2;
		nodeManager.updateView(viewX, viewY);
		coordinate.updateView(viewX, viewY);
		new Timer() {
			public void run() {
				int screenWidth = getWindowScreenWidth();
				int screenHeight = getWindowScreenHeight();
				int left = viewWidth / 2 - screenWidth / 2;
				int top = viewHeight / 2 - screenHeight / 2;
				Window.scrollTo(left, top);
				setWidgetPosition(buttonPanel, left + BUTTON_PANEL_MARGIN, top
						+ BUTTON_PANEL_MARGIN);
				glassStyle.setVisibility(Visibility.HIDDEN);
			}
		}.schedule(500);
	}

	private void slideToPosition(int posX, int posY) {
		final int lastLeft = getWindowScrollLeft();
		final int lastTop = getWindowScrollTop();
		int screenWidth = getWindowScreenWidth();
		int screenHeight = getWindowScreenHeight();
		if(posX < viewX + screenWidth / 2 || posX > viewX + viewWidth - screenWidth / 2 ||
				posY < viewY + screenHeight / 2 || posY > viewY + viewHeight - screenHeight / 2){
			// outside of the view, can't scroll.
			relocateCenter(posX, posY);
			return;
		}
		int prevCenterPosX = viewX + lastLeft + screenWidth / 2;
		int prevCenterPosY = viewY + lastTop + screenHeight / 2;
		final int diffX = posX - prevCenterPosX;
		final int diffY = posY - prevCenterPosY;
		new Animation() {
			protected void onUpdate(double progress) {
				progress = interpolate(progress);
				Window.scrollTo((int) (lastLeft + diffX * progress),
						(int) (lastTop + diffY * progress));
			}

			protected void onComplete() {
				Window.scrollTo(lastLeft + diffX, lastTop + diffY);
				setWidgetPosition(buttonPanel, lastLeft + diffX
						+ BUTTON_PANEL_MARGIN, lastTop + diffY
						+ BUTTON_PANEL_MARGIN);
			}
		}.run(2000);
	}

	public void onWindowScroll(ScrollEvent event) {
		int left = getWindowScrollLeft();
		int top = getWindowScrollTop();
		setWidgetPosition(buttonPanel, left + BUTTON_PANEL_MARGIN, top
				+ BUTTON_PANEL_MARGIN);
	}

	private static native int getWindowScreenWidth() /*-{
		return $wnd.innerWidth || $doc.body.clientWidth;
	}-*/;

	private static native int getWindowScreenHeight() /*-{
		return $wnd.innerHeight || $doc.body.clientHeight;
	}-*/;

	private static native int getWindowScrollLeft() /*-{
		return $wnd.pageXOffset || $doc.body.scrollLeft || 0
	}-*/;

	private static native int getWindowScrollTop() /*-{
		return $wnd.pageYOffset || $doc.body.scrollTop || 0
	}-*/;

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

	private boolean touchHandlerEnabled = false;

	private void supportDragAndDrop() {
		drawArea.addMouseDownHandler(new MouseDownHandler() {
			public void onMouseDown(MouseDownEvent event) {
				// XXX workaround for iPad, which fires mouse events even with
				// preventDefault.
				if (touchHandlerEnabled) {
					return;
				}

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
	}

	public boolean onTouchStartForGBrain(int eventX, int eventY) {
		touchHandlerEnabled = true;
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
			onWindowScroll(null);
			return true;
		}
	}

	public boolean onGestureEndForGBrain() {
		onResize();
		return true;
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
		Circle circle = new Circle(-10, -10, 3);
		circle.setFillColor("#cccccc");
		circle.setVisible(false);
		drawArea.insert(circle, 1);
		Line line = new Line(0, 0, 0, 0);
		line.setStrokeColor("#ffffff");
		parentNode.addChildLine(n.getId(), line);
		n.setParentLine(line);
		drawArea.insert(line, 1);
		new Animate(line, "strokewidth", 4, 1, 1000).start();
		new LineAnimation(line, circle);
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

	private class LineAnimation extends Animation {

		private final Line line;
		private final Circle circle;

		public LineAnimation(Line line, Circle circle) {
			this.line = line;
			this.circle = circle;
			run(3000);
		}

		protected void onStart() {
			if (line.getParent() != null && dragNode == null
					&& sliding == false) {
				circle.setX(line.getX1());
				circle.setY(line.getY1());
				circle.setVisible(true);
			}
		}

		protected void onUpdate(double progress) {
			if (line.getParent() != null && dragNode == null
					&& sliding == false) {
				progress = interpolate(progress);
				circle.setX((int) (line.getX1() + progress
						* (line.getX2() - line.getX1())));
				circle.setY((int) (line.getY1() + progress
						* (line.getY2() - line.getY1())));
			} else {
				circle.setVisible(false);
			}
		}

		protected void onComplete() {
			circle.setVisible(false);
			if (line.getParent() != null) {
				new LineAnimation(line, circle);
			} else {
				drawArea.remove(circle);
			}
		}

	}

	// TODO (Low) open all children
	// TODO (Low) Re-position child nodes
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

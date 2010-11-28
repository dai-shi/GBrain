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
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.user.client.Random;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class MainPane extends AbsolutePanel implements ProvidesResize,
		RequiresResize {

	private final GBrainServiceAsync gbrainService = GWT
			.create(GBrainService.class);

	private final NodeManager nodeManager;
	private final AsyncCallback<Void> nullCallback;

	private final HorizontalPanel buttonPanel;
	private final DrawingArea drawArea;
	private final Coordinate coordinate;

	public static final int BUTTON_SIZE = 28;
	public static final int IPHONE_EXTRA_HEIGHT = 60;

	private int viewX;
	private int viewY;

	public MainPane() {
		nodeManager = new NodeManager();

		nullCallback = new AsyncCallback<Void>() {
			public void onFailure(Throwable caught) {
				GWT.log("Network error!", caught);
				new AlertDialog("Network error!");
			}

			public void onSuccess(Void ignored) {
				// nothing
			}
		};

		PushButton createButton = new PushButton(new Image(
				"images/create_button.png"), new ClickHandler() {
			public void onClick(ClickEvent event) {
				new CreateDialog();
			}
		});
		createButton.setPixelSize(BUTTON_SIZE, BUTTON_SIZE);
		createButton.setTitle("Create a new text");

		PushButton deleteButton = new PushButton(new Image(
				"images/delete_button.png"), new ClickHandler() {
			public void onClick(ClickEvent event) {
				if (selectNode == null) {
					new AlertDialog("Nothing is selected.");
					return;
				}
				final NeuronNode tmpSelectNode = selectNode;
				String content = tmpSelectNode.getContent();
				final long id = tmpSelectNode.getId();
				new ConfirmDialog("Are you sure you want to delete\n'"
						+ content + "' ?", new ClickHandler() {
					public void onClick(ClickEvent event) {
						gbrainService.deleteNeuron(id,
								new AsyncCallback<Void>() {
									public void onFailure(Throwable caught) {
										GWT.log("Network error!", caught);
										new AlertDialog("Network error!");
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

		PushButton openButton = new PushButton(new Image(
				"images/open_button.png"), new ClickHandler() {
			public void onClick(ClickEvent event) {
				if (selectNode == null) {
					new AlertDialog("Nothing is selected.");
					return;
				}
				refreshChildNeurons(selectNode.getId());
			}
		});
		openButton.setPixelSize(BUTTON_SIZE, BUTTON_SIZE);
		openButton.setTitle("Open children");

		PushButton closeButton = new PushButton(new Image(
				"images/close_button.png"), new ClickHandler() {
			public void onClick(ClickEvent event) {
				if (selectNode == null) {
					new AlertDialog("Nothing is selected.");
					return;
				}
				removeChildNodes(selectNode);
			}
		});
		closeButton.setPixelSize(BUTTON_SIZE, BUTTON_SIZE);
		closeButton.setTitle("Close children");

		PushButton jumpButton = new PushButton(new Image(
				"images/jump_button.png"), new ClickHandler() {
			public void onClick(ClickEvent event) {
				if (selectNode == null) {
					new AlertDialog("Nothing is selected.");
					return;
				}
				jumpToUrl(selectNode);
			}
		});
		jumpButton.setPixelSize(BUTTON_SIZE, BUTTON_SIZE);
		jumpButton.setTitle("Jump to URL");

		buttonPanel = new HorizontalPanel();
		buttonPanel.add(createButton);
		buttonPanel.add(deleteButton);
		buttonPanel.add(openButton);
		buttonPanel.add(closeButton);
		buttonPanel.add(jumpButton);

		int screenWidth = Window.getClientWidth();
		int screenHeight = Window.getClientHeight() + IPHONE_EXTRA_HEIGHT;
		drawArea = new DrawingArea(screenWidth, screenHeight);
		drawArea.getElement().setId("gbrain-svgpanel");
		this.add(drawArea, 0, 0);
		this.add(buttonPanel, 0, 0);
		viewX = -drawArea.getWidth() / 2;
		viewY = -drawArea.getHeight() / 2;
		coordinate = new Coordinate(drawArea, viewX, viewY);
		Window.scrollTo(0, 1);

		supportDragAndDrop();
		new LineAnimation().start();
		refreshTopNeurons();
	}

	public void onResize() {
		int screenWidth = Window.getClientWidth();
		int screenHeight = Window.getClientHeight() + IPHONE_EXTRA_HEIGHT;
		drawArea.setWidth(screenWidth);
		drawArea.setHeight(screenHeight);
		coordinate.updateView(viewX, viewY);
		Window.scrollTo(0, 1);
	}

	private class AlertDialog extends DialogBox {
		public AlertDialog(String message) {
			setModal(true);
			//setGlassEnabled(true);
			setText("Alert");

			Label label = new Label(message);
			Button close = new Button("Close");

			VerticalPanel basePanel = new VerticalPanel();
			basePanel.setSpacing(10);
			basePanel.add(label);
			basePanel.add(close);
			add(basePanel);

			close.addClickHandler(new ClickHandler() {
				public void onClick(ClickEvent event) {
					hide();
				}
			});

			center();
		}
	}

	private class ConfirmDialog extends DialogBox {
		public ConfirmDialog(String message, final ClickHandler okHandler) {
			setModal(true);
			//setGlassEnabled(true);
			setText("Confirm");

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
			add(basePanel);

			ok.addClickHandler(new ClickHandler() {
				public void onClick(ClickEvent event) {
					okHandler.onClick(event);
					hide();
				}
			});
			cancel.addClickHandler(new ClickHandler() {
				public void onClick(ClickEvent event) {
					hide();
				}
			});

			center();
		}
	}

	private class CreateDialog extends DialogBox {
		private final TextBox content;
		private final RadioButton top;
		private final RadioButton child;

		public CreateDialog() {
			setModal(true);
			//setGlassEnabled(true);
			setText("Create New");

			Label label = new Label("Enter Text (4~64 chars)");

			content = new TextBox();

			HorizontalPanel radioPanel = new HorizontalPanel();
			radioPanel.setSpacing(5);
			top = new RadioButton("toporchild", "Top");
			top.setValue(true);
			radioPanel.add(top);
			child = new RadioButton("toporchild", "Child");
			radioPanel.add(child);

			HorizontalPanel buttonPanel = new HorizontalPanel();
			buttonPanel.setSpacing(5);
			Button ok = new Button("OK");
			buttonPanel.add(ok);
			Button cancel = new Button("Cancel");
			buttonPanel.add(cancel);

			VerticalPanel basePanel = new VerticalPanel();
			basePanel.setSpacing(10);
			basePanel.add(label);
			basePanel.add(content);
			basePanel.add(radioPanel);
			basePanel.add(buttonPanel);
			add(basePanel);

			content.addKeyPressHandler(new KeyPressHandler() {
				public void onKeyPress(KeyPressEvent event) {
					if (event.getCharCode() == KeyCodes.KEY_ENTER) {
						handleEnter();
					}
				}
			});
			ok.addClickHandler(new ClickHandler() {
				public void onClick(ClickEvent event) {
					handleEnter();
				}
			});
			cancel.addClickHandler(new ClickHandler() {
				public void onClick(ClickEvent event) {
					hide();
				}
			});

			center();
		}

		private void handleEnter() {
			String text = content.getText();
			if (!FieldVerifier.isValidContent(text)) {
				new AlertDialog(
						"Must be between 4 chars and 64 chars.\n(currently "
								+ text.length() + " chars)");
				return;
			}
			if (top.getValue()) {
				createNewNeuron(text, null);
			} else if (child.getValue()) {
				if (selectNode == null) {
					new AlertDialog("No parent is selected!");
					return;
				}
				createNewNeuron(text, selectNode);
			}
			hide();
		}
	}

	public void createNewNeuron(String content, final NeuronNode parentNode) {
		AsyncCallback<Void> cb = new AsyncCallback<Void>() {
			public void onFailure(Throwable caught) {
				GWT.log("Network error!", caught);
				new AlertDialog("Network error!");
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
			int x = viewX + Random.nextInt(100) + 100;
			int y = viewY + Random.nextInt(100) + 100;
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
				new AlertDialog("Network error!");
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
						new AlertDialog("Network error!");
					}

					public void onSuccess(NeuronData[] neurons) {
						for (NeuronData n : neurons) {
							addNode(n);
						}
					}
				});
	}

	private NeuronNode dragNode = null;
	private int dragStartX = 0;
	private int dragStartY = 0;
	private NeuronNode mouseOverNode = null;

	private NeuronNode selectNode = null;
	private long lastMouseDownTime = 0;
	private int lastMouseDownX = 0;
	private int lastMouseDownY = 0;
	private long secondLastMouseDownTime = 0;
	private int secondLastMouseDownX = 0;
	private int secondLastMouseDownY = 0;
	private static long CLICK_TIMEOUT = 600;
	private static int CLICK_ALLOWANCE = 9;

	private boolean sliding = false;
	private int slideStartX = 0;
	private int slideStartY = 0;
	private long slideStartTime = 0;
	private static double MAX_SLIDE_SPEED = 5.0;

	private void supportDragAndDrop() {
		drawArea.addMouseDownHandler(new MouseDownHandler() {
			public void onMouseDown(MouseDownEvent event) {
				long now = System.currentTimeMillis();
				int eventX = event.getX();
				int eventY = event.getY();
				if (selectNode != null
						&& selectNode.containsPoint(eventX, eventY)) {
					dragNode = selectNode;
					dragStartX = eventX;
					dragStartY = eventY;
					savePositionNodeAndChildNodes(dragNode);
				} else {
					sliding = true;
					slideStartX = eventX;
					slideStartY = eventY;
					slideStartTime = now;
				}
				secondLastMouseDownTime = lastMouseDownTime;
				secondLastMouseDownX = lastMouseDownX;
				secondLastMouseDownY = lastMouseDownY;
				lastMouseDownTime = now;
				lastMouseDownX = eventX;
				lastMouseDownY = eventY;
			}
		});
		drawArea.addMouseUpHandler(new MouseUpHandler() {
			public void onMouseUp(MouseUpEvent event) {
				sliding = false;
				int eventX = event.getX();
				int eventY = event.getY();
				long now = System.currentTimeMillis();
				if (now < lastMouseDownTime + CLICK_TIMEOUT
						&& Math.abs(eventX - lastMouseDownX) < CLICK_ALLOWANCE
						&& Math.abs(eventY - lastMouseDownY) < CLICK_ALLOWANCE) {
					if (now < secondLastMouseDownTime + CLICK_TIMEOUT
							&& Math.abs(eventX - secondLastMouseDownX) < CLICK_ALLOWANCE
							&& Math.abs(eventY - secondLastMouseDownY) < CLICK_ALLOWANCE) {
						if (nodeManager.hasAnyChildNodes(selectNode.getId())) {
							removeChildNodes(selectNode);
						} else {
							refreshChildNeurons(selectNode.getId());
						}
						lastMouseDownTime = 0;
					} else {
						selectNodeByPosition(lastMouseDownX, lastMouseDownY);
					}
					dragNode = null;
				} else if (dragNode != null) {
					if (mouseOverNode != null) {
						long newparent = mouseOverNode.getId();
						if (dragNode.getParentId() == null
								|| dragNode.getParentId() != newparent) {
							revertPositionNodeAndChildNodes(dragNode);
							replaceParent(dragNode, newparent);
							gbrainService.updateParent(dragNode.getId(),
									newparent, nullCallback);
						}
						mouseOverNode.unsetHighlight();
						mouseOverNode = null;
					} else {
						if (dragNode.isPositionUpdated()) {
							updatePositionNodeAndChildNodes(dragNode);
						}
					}
					dragNode = null;
				}
			}
		});
		drawArea.addMouseMoveHandler(new MouseMoveHandler() {
			public void onMouseMove(MouseMoveEvent event) {
				if (dragNode != null) {
					int offsetX = event.getX() - dragStartX;
					int offsetY = event.getY() - dragStartY;
					dragPositionNodeAndChildNodes(dragNode, offsetX, offsetY);
					checkMouseOver(event.getX(), event.getY());
				} else if (sliding) {
					long now = System.currentTimeMillis();
					int eventX = event.getX();
					int eventY = event.getY();
					float speedX = (float) Math.abs(eventX - slideStartX)
							/ (float) (now - slideStartTime);
					float speedY = (float) Math.abs(eventY - slideStartY)
							/ (float) (now - slideStartTime);
					if (speedX < MAX_SLIDE_SPEED && speedY < MAX_SLIDE_SPEED) {
						viewX -= eventX - slideStartX;
						viewY -= eventY - slideStartY;
						nodeManager.updateView(viewX, viewY);
						coordinate.updateView(viewX, viewY);
						slideStartX = eventX;
						slideStartY = eventY;
						slideStartTime = now;
					} else {
						sliding = false;
					}
				}
			}
		});
	}

	private void selectNodeByPosition(int x, int y) {
		NeuronNode n = findNodeByPosition(x, y, null);
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
		parentNode.addChildLine(n.getId(), line);
		n.setParentLine(line);
		drawArea.insert(line, drawArea.getVectorObjectCount()
				- nodeManager.count());
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

	private void checkMouseOver(int x, int y) {
		NeuronNode n = findNodeByPosition(x, y, dragNode);
		if (n != null) {
			if (mouseOverNode != n) {
				if (mouseOverNode != null) {
					mouseOverNode.unsetHighlight();
				}
				mouseOverNode = n;
				mouseOverNode.setHighlight();
			}
		} else if (mouseOverNode != null) {
			mouseOverNode.unsetHighlight();
			mouseOverNode = null;
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
		Window.alert("DEBUG: no node found out of "+drawArea.getVectorObjectCount()+" objects.");
		return null;
	}

	private Circle animationCircle = null;
	private Iterator<NeuronNode> animationIte = null;

	private class LineAnimation extends Animation {

		private Line line = null;

		public LineAnimation() {
			if (animationCircle == null) {
				animationCircle = new Circle(-10, -10, 3);
				animationCircle.setFillColor("black");
				animationCircle.setVisible(false);
				drawArea.insert(animationCircle, drawArea
						.getVectorObjectCount()
						- nodeManager.count());
			}
		}

		public void start() {
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
			new LineAnimation().start();
		}

	}

	// TODO (High) concept -> redesign
	// TODO (Middle) open all children
	// TODO (Middle) Re-position child nodes
	// TODO (Low) slide like iphone (speed detection)
	// TODO (Low) auto-scroll to a certain position (to a child node?)
	// TODO (Low) search text and auto-scroll
	// TODO (Low) textbox focus in dialogs, textbox enter to OK
	// TODO (App) Land
	// TODO (App) from twitter
	// TODO separated DragAndDropSupport.java (reusable version)
	// TODO progress indicator
	// TODO unlink parent button (make it as top)

}

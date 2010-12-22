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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NodeManager {

	private Map<Long, NeuronNode> nodeMap = new HashMap<Long, NeuronNode>();
	private Map<Long, List<NeuronNode>> childrenMap = new HashMap<Long, List<NeuronNode>>();

	public NodeManager() {

	}

	public void addNode(NeuronNode n) {
		nodeMap.put(n.getId(), n);
		Long parent = n.getParentId();
		if (childrenMap.containsKey(parent)) {
			List<NeuronNode> list = childrenMap.get(parent);
			if (!list.contains(n)) {
				list.add(n);
			}
		} else {
			childrenMap
					.put(parent, new ArrayList<NeuronNode>(Arrays.asList(n)));
		}
	}

	public NeuronNode getNode(long id) {
		return nodeMap.get(id);
	}

	public void removeNode(long id) {
		NeuronNode n = nodeMap.remove(id);
		List<NeuronNode> list = childrenMap.get(n.getParentId());
		list.remove(n);
		if (list.isEmpty()) {
			childrenMap.remove(n.getParentId());
		}
	}

	public void replaceParentNode(long id, Long newParentId) {
		NeuronNode n = getNode(id);
		Long parentId = n.getParentId();
		if (parentId != null) {
			NeuronNode parentNode = getNode(parentId);
			parentNode.decreaseChildren();
			List<NeuronNode> list = childrenMap.get(parentId);
			list.remove(n);
			if (list.isEmpty()) {
				childrenMap.remove(parentId);
			}
		}
		n.setParentId(newParentId);
		if (newParentId != null) {
			NeuronNode newParentNode = getNode(newParentId);
			if (newParentNode != null) {
				newParentNode.increaseChildren();
				if (childrenMap.containsKey(newParentId)) {
					List<NeuronNode> list = childrenMap.get(newParentId);
					if (!list.contains(n)) {
						list.add(n);
					}
				} else {
					childrenMap.put(newParentId, new ArrayList<NeuronNode>(
							Arrays.asList(n)));
				}
			} else {
				n.setParentId(null);
			}
		}
	}

	public int count() {
		return nodeMap.size();
	}

	public boolean hasAnyChildNodes(long id) {
		return childrenMap.containsKey(id);
	}

	public Collection<NeuronNode> getAllNodes() {
		return nodeMap.values();
	}

	public Collection<NeuronNode> getChildNodes(long id) {
		List<NeuronNode> l = childrenMap.get(id);
		if (l != null) {
			l = new ArrayList<NeuronNode>(l);
		} else {
			l = new ArrayList<NeuronNode>();
		}
		return l;
	}

	public NeuronNode getFirstChildNode(long id) {
		List<NeuronNode> l = childrenMap.get(id);
		if (l != null) {
			return l.get(0);
		} else {
			return null;
		}
	}

	public NeuronNode getParentNode(long id) {
		NeuronNode n = nodeMap.get(id);
		if (n != null) {
			return nodeMap.get(n.getParentId());
		} else {
			return null;
		}
	}

	public NeuronNode getNextSiblingNode(long id) {
		NeuronNode n = nodeMap.get(id);
		if (n == null) {
			return null;
		}
		List<NeuronNode> l = childrenMap.get(n.getParentId());
		if (l == null) {
			return null;
		}
		int size = l.size();
		for (int i = 0; i < size; i++) {
			if (l.get(i).getId() == id) {
				if (i + 1 < size) {
					return l.get(i + 1);
				} else {
					return l.get(0);
				}
			}
		}
		return null;
	}

	public NeuronNode getPreviousSiblingNode(long id) {
		NeuronNode n = nodeMap.get(id);
		if (n == null) {
			return null;
		}
		List<NeuronNode> l = childrenMap.get(n.getParentId());
		if (l == null) {
			return null;
		}
		int size = l.size();
		for (int i = size - 1; i >= 0; i--) {
			if (l.get(i).getId() == id) {
				if (i - 1 >= 0) {
					return l.get(i - 1);
				} else {
					return l.get(size - 1);
				}
			}
		}
		return null;
	}

	public void arrangeChildNodes(long id) {
		NeuronNode n = nodeMap.get(id);
		if (n == null) {
			return;
		}
		int posX = n.getPosX();
		int posY = n.getPosY();
		int rX = 250; // TODO change based on the number of all children
		int rY = 250; // TODO change based on the number of all children
		if (posX < 0) {
			rX *= -1;
		}
		if (posY < 0) {
			rY *= -1;
		}
		Collection<NeuronNode> childNodes = getChildNodes(id);
		int children = childNodes.size();
		int i = 1;
		for (NeuronNode tmp : childNodes) {
			int x = posX
					+ (int) (rX * Math.sin(Math.PI * i / (2 * (children + 1))));
			int y = posY
					+ (int) (rY * Math.cos(Math.PI * i / (2 * (children + 1))));
			tmp.setPosition(x, y);
			i++;
		}
	}

	public void updateView(int viewX, int viewY) {
		// XXX this could be slow with the number of nodes.
		for (NeuronNode n : nodeMap.values()) {
			n.updateView(viewX, viewY);
		}
	}

}

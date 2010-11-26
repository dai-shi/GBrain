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

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class NodeManager {

	private Map<Long, NeuronNode> nodeMap = new HashMap<Long, NeuronNode>();

	public NodeManager() {

	}

	public void addNode(NeuronNode n) {
		nodeMap.put(n.getId(), n);
	}

	public NeuronNode getNode(long id) {
		return nodeMap.get(id);
	}

	public void removeNode(long id) {
		nodeMap.remove(id);
	}

	public int count(){
		return nodeMap.size();
	}
	
	public boolean hasAnyChildNodes(long id) {
		// FIXME use parent index to speedup.
		for (NeuronNode tmp : nodeMap.values()) {
			if (tmp.getParentId() != null && tmp.getParentId() == id) {
				return true;
			}
		}
		return false;
	}

	public Collection<NeuronNode> getAllNodes(){
		return nodeMap.values();
	}

	public Collection<NeuronNode> getChildNodes(long id) {
		List<NeuronNode> l = new LinkedList<NeuronNode>();
		// FIXME use parent index to speedup.
		for (NeuronNode tmp : nodeMap.values()) {
			if (tmp.getParentId() != null && tmp.getParentId() == id) {
				l.add(tmp);
			}
		}
		return l;
	}
	
	public void updateView(int viewX, int viewY) {
		// XXX this could be slow with the number of nodes.
		for (NeuronNode n : nodeMap.values()) {
			n.updateView(viewX, viewY);
		}
	}
	
}

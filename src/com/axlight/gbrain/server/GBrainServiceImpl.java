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
package com.axlight.gbrain.server;

import java.util.ArrayList;
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import com.axlight.gbrain.client.GBrainService;
import com.axlight.gbrain.shared.FieldVerifier;
import com.axlight.gbrain.shared.NeuronData;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class GBrainServiceImpl extends RemoteServiceServlet implements
		GBrainService {

	public void addNeuron(String content, int x, int y)
			throws IllegalArgumentException {
		if (!FieldVerifier.isValidContent(content)) {
			throw new IllegalArgumentException("Invalid content");
		}
		PersistenceManager pm = PMF.get().getPersistenceManager();
		try {
			Neuron neuron = new Neuron(content, x, y);
			pm.makePersistent(neuron);
		} finally {
			pm.close();
		}
	}

	public void addNeuron(long parentid, String content, int x, int y)
			throws IllegalArgumentException {
		if (!FieldVerifier.isValidContent(content)) {
			throw new IllegalArgumentException("Invalid content");
		}
		PersistenceManager pm = PMF.get().getPersistenceManager();
		try {
			Neuron n = pm.getObjectById(Neuron.class, parentid);
			if (n == null) {
				throw new IllegalArgumentException("No such parent neuron");
			}
			n.setChildren(n.getChildren() + 1);
			Neuron neuron = new Neuron(parentid, content, x, y);
			pm.makePersistent(neuron);
		} finally {
			pm.close();
		}
	}

	public void updatePosition(long id, int x, int y)
			throws IllegalArgumentException {
		PersistenceManager pm = PMF.get().getPersistenceManager();
		try {
			Neuron n = pm.getObjectById(Neuron.class, id);
			if (n == null) {
				throw new IllegalArgumentException("No such neuron");
			}
			n.setX(x);
			n.setY(y);
		} finally {
			pm.close();
		}
	}

	public void updateColor(long id, String color)
			throws IllegalArgumentException {
		PersistenceManager pm = PMF.get().getPersistenceManager();
		try {
			Neuron n = pm.getObjectById(Neuron.class, id);
			if (n == null) {
				throw new IllegalArgumentException("No such neuron");
			}
			n.setColor(color);
		} finally {
			pm.close();
		}
	}

	public void updateParent(long id, long parentid)
			throws IllegalArgumentException {
		PersistenceManager pm = PMF.get().getPersistenceManager();
		try {
			Neuron parent = pm.getObjectById(Neuron.class, parentid);
			if (parent == null) {
				throw new IllegalArgumentException("No such new parent neuron");
			}
			Neuron me = pm.getObjectById(Neuron.class, id);
			if (me == null) {
				throw new IllegalArgumentException("No such neuron");
			}
			Long oldParent = me.getParentId();
			if (oldParent != null) {
				Neuron n = pm.getObjectById(Neuron.class, oldParent);
				if (n == null) {
					throw new IllegalArgumentException(
							"No such old parent neuron");
				}
				n.setChildren(n.getChildren() - 1);
			}
			parent.setChildren(parent.getChildren() + 1);
			me.setParentId(parentid);
		} finally {
			pm.close();
		}
	}

	public void removeParent(long id) throws IllegalArgumentException {
		PersistenceManager pm = PMF.get().getPersistenceManager();
		try {
			Neuron me = pm.getObjectById(Neuron.class, id);
			if (me == null) {
				throw new IllegalArgumentException("No such neuron");
			}
			Long oldParent = me.getParentId();
			if (oldParent != null) {
				Neuron n = pm.getObjectById(Neuron.class, oldParent);
				if (n == null) {
					throw new IllegalArgumentException(
							"No such old parent neuron");
				}
				n.setChildren(n.getChildren() - 1);
			}
			me.setParentId(null);
		} finally {
			pm.close();
		}
	}

	@SuppressWarnings("unchecked")
	public void deleteNeuron(long id) throws IllegalArgumentException {
		PersistenceManager pm = PMF.get().getPersistenceManager();
		try {
			Neuron me = pm.getObjectById(Neuron.class, id);
			if (me == null) {
				throw new IllegalArgumentException("No such neuron");
			}
			Long oldParent = me.getParentId();
			if (oldParent != null) {
				Neuron n = pm.getObjectById(Neuron.class, oldParent);
				if (n == null) {
					throw new IllegalArgumentException(
							"No such old parent neuron");
				}
				n.setChildren(n.getChildren() - 1);
			}
			pm.deletePersistent(me);

			Query query = pm.newQuery(Neuron.class);
			query.setOrdering("children desc");
			query.setFilter("parentId == parent");
			query.declareParameters("long parent");
			List<Neuron> list = (List<Neuron>) query.execute(id);
			for (Neuron n : list) {
				n.setParentId(null);
			}
		} finally {
			pm.close();
		}
	}

	@SuppressWarnings("unchecked")
	public NeuronData[] getTopNeurons() throws IllegalArgumentException {
		List<NeuronData> result = new ArrayList<NeuronData>();
		PersistenceManager pm = PMF.get().getPersistenceManager();
		try {
			Query query = pm.newQuery(Neuron.class);
			query.setOrdering("children desc");
			query.setFilter("parentId == null");
			List<Neuron> list = (List<Neuron>) query.execute();
			for (Neuron n : list) {
				result.add(n.toNeuronData());
			}
		} finally {
			pm.close();
		}
		return result.toArray(new NeuronData[0]);
	}

	@SuppressWarnings("unchecked")
	public NeuronData[] getChildNeurons(long parent)
			throws IllegalArgumentException {
		List<NeuronData> result = new ArrayList<NeuronData>();
		PersistenceManager pm = PMF.get().getPersistenceManager();
		try {
			Query query = pm.newQuery(Neuron.class);
			query.setOrdering("children desc");
			query.setFilter("parentId == parent");
			query.declareParameters("long parent");
			List<Neuron> list = (List<Neuron>) query.execute(parent);
			for (Neuron n : list) {
				result.add(n.toNeuronData());
			}
		} finally {
			pm.close();
		}
		return result.toArray(new NeuronData[0]);
	}

}

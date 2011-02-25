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

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import com.axlight.gbrain.client.GBrainService;
import com.axlight.gbrain.shared.FieldVerifier;
import com.axlight.gbrain.shared.NeuronData;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class GBrainServiceImpl extends RemoteServiceServlet implements
		GBrainService {

	private static GBrainServiceImpl instance = null;

	public static GBrainServiceImpl getInstance() {
		return instance;
	}

	public GBrainServiceImpl() {
		super();
		instance = this;
	}

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

	@SuppressWarnings("unchecked")
	private void queryAllChildNeurons(Query query, long parent,
			List<NeuronData> result) {
		List<Neuron> list = (List<Neuron>) query.execute(parent);
		for (Neuron n : list) {
			result.add(n.toNeuronData());
		}
		for (Neuron n : list) {
			queryAllChildNeurons(query, n.getId(), result);
		}
	}

	public NeuronData[] getAllChildNeurons(long parent)
			throws IllegalArgumentException {
		List<NeuronData> result = new ArrayList<NeuronData>();
		PersistenceManager pm = PMF.get().getPersistenceManager();
		try {
			Query query = pm.newQuery(Neuron.class);
			query.setOrdering("children desc");
			query.setFilter("parentId == parent");
			query.declareParameters("long parent");
			queryAllChildNeurons(query, parent, result);
		} finally {
			pm.close();
		}
		return result.toArray(new NeuronData[0]);
	}

	@SuppressWarnings("unchecked")
	private boolean alreadyExists(String content) {
		PersistenceManager pm = PMF.get().getPersistenceManager();
		try {
			Query query = pm.newQuery(Neuron.class);
			query.setFilter("content == c");
			query.declareParameters("String c");
			List<Neuron> list = (List<Neuron>) query.execute(content);
			if (list.isEmpty()) {
				return false;
			} else {
				return true;
			}
		} finally {
			pm.close();
		}
	}

	private static final double PARAM_NEW_X = 50.0;
	private static final double PARAM_NEW_Y = 300.0;

	public void fetchNeuron() throws IOException {
		URLFetchService ufs = URLFetchServiceFactory.getURLFetchService();
		JsonParser parser = new JsonParser();

		NeuronData[] targets = getTopNeurons();
		for (NeuronData n : targets) {
			String q = URLEncoder.encode(n.getContent(), "UTF-8");
			long parent = n.getId();
			int x = n.getX();
			int y = n.getY();
			int children = n.getChildren();

			HTTPResponse res = ufs.fetch(new URL(
					"http://search.twitter.com/search.json?q=" + q
							+ "&result_type=recent"));
			JsonElement top = parser
					.parse(new String(res.getContent(), "UTF-8"));
			for (JsonElement ele : top.getAsJsonObject().get("results")
					.getAsJsonArray()) {
				String content = ele.getAsJsonObject().get("text")
						.getAsString();
				if (alreadyExists(content)) {
					continue;
				}
				try {
					double deg = Math.random() * Math.PI * 2;
					int xx = x
							+ (int) ((children + 1) * Math.random()
									* PARAM_NEW_X * Math.cos(deg));
					int yy = y
							+ (int) ((children + 1) * Math.random()
									* PARAM_NEW_Y * Math.sin(deg));
					addNeuron(parent, content, xx, yy);
					children++;
				} catch (IllegalArgumentException e) {
					// ignored
				}
			}
		}
	}

}

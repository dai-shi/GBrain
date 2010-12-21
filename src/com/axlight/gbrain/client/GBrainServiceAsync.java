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

import com.axlight.gbrain.shared.NeuronData;
import com.google.gwt.user.client.rpc.AsyncCallback;

public interface GBrainServiceAsync {
	void addNeuron(String content, int x, int y, AsyncCallback<Void> callback);
	void addNeuron(long parent, String content, int x, int y, AsyncCallback<Void> callback);
	void updatePosition(long id, int x, int y, AsyncCallback<Void> callback);
	void updateColor(long id, String color, AsyncCallback<Void> callback);
	void updateParent(long id, long parent, AsyncCallback<Void> callback);
	void removeParent(long id, AsyncCallback<Void> callback);
	void deleteNeuron(long id, AsyncCallback<Void> callback);
	void getTopNeurons(AsyncCallback<NeuronData[]> callback);
	void getChildNeurons(long parent, AsyncCallback<NeuronData[]> callback);
}

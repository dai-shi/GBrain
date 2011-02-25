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
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * The client side stub for the RPC service.
 */
@RemoteServiceRelativePath("service")
public interface GBrainService extends RemoteService {
	void addNeuron(String content, int x, int y) throws IllegalArgumentException;
	void addNeuron(long parent, String content, int x, int y) throws IllegalArgumentException;
	void updatePosition(long id, int x, int y) throws IllegalArgumentException;
	void updateColor(long id, String color) throws IllegalArgumentException;
	void updateParent(long id, long parent) throws IllegalArgumentException;
	void removeParent(long id) throws IllegalArgumentException;
	void deleteNeuron(long id) throws IllegalArgumentException;
	NeuronData[] getTopNeurons() throws IllegalArgumentException;
	NeuronData[] getChildNeurons(long parent) throws IllegalArgumentException;
	NeuronData[] getAllChildNeurons(long parent) throws IllegalArgumentException;
}

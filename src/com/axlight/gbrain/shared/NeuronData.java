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
package com.axlight.gbrain.shared;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

public class NeuronData implements IsSerializable {
	private long id;
	private String content;
	private Date created;
	private Long parentId;
	private int children;
	private int x;
	private int y;
	
	NeuronData(){
		this.id = 0;
		this.content = null;
		this.created = null;
		this.children = 0;
		this.parentId = null;
		this.x = 0;
		this.y = 0;
	}
	
	public NeuronData(long id, String content, Date created, int children, Long parentId, int x, int y){
		this.id = id;
		this.content = content;
		this.created = created;
		this.children = children;
		this.parentId = parentId;
		this.x = x;
		this.y = y;
	}
	
	public long getId() {
		return id;
	}
	
	public String getContent() {
		return content;
	}

	public Date getCreated() {
		return created;
	}

	public int getChildren() {
		return children;
	}
	
	public Long getParentId() {
		return parentId;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}
	
}

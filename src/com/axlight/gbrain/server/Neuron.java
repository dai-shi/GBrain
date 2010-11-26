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

import java.util.Date;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.axlight.gbrain.shared.NeuronData;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class Neuron {

	@PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private Long id;

    @Persistent
    private User author;

    @Persistent
    private String content;

    @Persistent
    private Date created;
    
    @Persistent
    private Long parentId;

    @Persistent
    private int children;
    
    @Persistent
    private int x;
    
    @Persistent
    private int y;
    
    public Neuron(String content, int x, int y) {
    	UserService userService = UserServiceFactory.getUserService();
        this.author = userService.getCurrentUser();
        this.content = content;
        this.created = new Date();
        this.parentId = null;
        this.children = 0;
        this.x = x;
        this.y = y;
    }

    public Neuron(Long parentId, String content, int x, int y) {
    	this(content, x, y);
        this.parentId = parentId;
    }

    public long getId() {
        return id;
    }

    public User getAuthor() {
        return author;
    }

    public String getContent() {
        return content;
    }

    public Date getCreated() {
        return created;
    }

	public Long getParentId() {
		return parentId;
	}


	public int getChildren() {
		return children;
	}
	
	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}
	
	public void setContent(String content) {
        this.content = content;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public void setChildren(int children){
    	this.children = children;
    }
    
    public void setParentId(Long parentId){
    	this.parentId = parentId;
    }

	public void setX(int x) {
		this.x = x;
	}

	public void setY(int y) {
		this.y = y;
	}
	
    public NeuronData toNeuronData(){
    	return new NeuronData(id, content, created, children, parentId, x, y);
    }
    
}
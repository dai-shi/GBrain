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

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootLayoutPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class GBrain implements EntryPoint {

	public static boolean isIPhone = false;

	/**
	 * This is the entry point method.
	 */
	public void onModuleLoad() {
		String userAgent = Window.Navigator.getUserAgent();
		if (userAgent.indexOf("iPhone") >= 0) {
			isIPhone = true;
		}
		MainPane mainPane = new MainPane();
		RootLayoutPanel.get().add(mainPane);
	}

}

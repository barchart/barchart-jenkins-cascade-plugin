/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

/**
 * Attach action to the layout build.
 * 
 * @author Andrei Pozolotin
 */
public class LayoutArgumentsAction extends AbstractAction {

	private final String configAction;

	public LayoutArgumentsAction(final String configAction) {
		this.configAction = configAction;
	}

	public ProjectAction getConfigAction() {
		return ProjectAction.form(configAction);
	}

	@Override
	public String toString() {
		return "/action=" + configAction;
	}

}

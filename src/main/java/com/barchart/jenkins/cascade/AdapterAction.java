/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import hudson.model.Action;

/**
 * Empty base action.
 * 
 * @author Andrei Pozolotin
 */
public abstract class AdapterAction implements Action {

	public String getIconFileName() {
		/** Show no icon. */
		return null;
	}

	public String getDisplayName() {
		/** Show no name. */
		return null;
	}

	public String getUrlName() {
		/** Show no link. */
		return null;
	}

}

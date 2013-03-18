/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

/**
 * Show project identity on project page.
 * 
 * @author Andrei Pozolotin
 */
public class ProjectPageIdentity extends AbstractAction {

	private final ProjectIdentity identity;

	public ProjectPageIdentity(final ProjectIdentity identity) {
		this.identity = identity;
	}

	public ProjectIdentity identity() {
		return identity;
	}

}

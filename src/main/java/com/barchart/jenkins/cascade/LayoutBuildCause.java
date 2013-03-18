/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import hudson.model.AbstractBuild;
import hudson.model.Cause.UserIdCause;

/**
 * User action cause to signifying a layout build type.
 * 
 * @author Andrei Pozolotin
 */
public class LayoutBuildCause extends UserIdCause {

	/**
	 * Build originated by layout action.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static boolean hasCause(final AbstractBuild build) {
		return build.getCause(LayoutBuildCause.class) != null;
	}

}

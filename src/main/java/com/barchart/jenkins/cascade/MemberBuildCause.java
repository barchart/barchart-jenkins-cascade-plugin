/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import hudson.model.Cause;
import hudson.model.Cause.UserIdCause;

import java.util.List;

/**
 * User action cause to signifying a cascade build type.
 * 
 * @author Andrei Pozolotin
 */
public class MemberBuildCause extends UserIdCause {

	public static boolean hasCause(final List<Cause> causeList) {
		if (causeList == null || causeList.isEmpty()) {
			return false;
		}
		for (final Cause cause : causeList) {
			if (cause instanceof MemberBuildCause) {
				return true;
			}
		}
		return false;
	}

}

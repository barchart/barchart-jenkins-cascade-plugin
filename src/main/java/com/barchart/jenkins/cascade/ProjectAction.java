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
 * Project life cycle action.
 * 
 * @author Andrei Pozolotin
 */
public enum ProjectAction {

	UNKNOWN("unknown"), //

	CREATE("create"), //
	DELETE("delete"), //
	UPDATE("update"), //

	;

	public Action badge() {
		switch (this) {
		case CREATE:
			return new DoCreateBadge();
		case DELETE:
			return new DoDeleteBadge();
		case UPDATE:
			return new DoUpdateBadge();
		default:
			return new DoUnknownBadge();
		}
	}

	public final String name;

	ProjectAction(final String name) {
		this.name = name;
	}

	public static ProjectAction form(final String name) {
		for (final ProjectAction type : ProjectAction.values()) {
			if (type.name.equalsIgnoreCase(name)) {
				return type;
			}
		}
		return UNKNOWN;
	}

}

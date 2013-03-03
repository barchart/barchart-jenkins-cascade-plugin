/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

/**
 * Attach action to the build to record arguments.
 */
public class LayoutArgumentsAction extends AdapterAction {

	public enum ConfigAction {

		BUILD("build"), //

		CREATE("create"), //
		DELETE("delete"), //
		UPDATE("update"), //

		;
		public final String name;

		ConfigAction(final String name) {
			this.name = name;
		}

		public static ConfigAction form(final String name) {
			for (final ConfigAction type : ConfigAction.values()) {
				if (type.name.equalsIgnoreCase(name)) {
					return type;
				}
			}
			return BUILD;
		}

	}

	private final String configAction;

	public LayoutArgumentsAction(final String configAction) {
		this.configAction = configAction;
	}

	public ConfigAction getConfigAction() {
		return ConfigAction.form(configAction);
	}

}

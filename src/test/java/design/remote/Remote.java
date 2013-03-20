package design.remote;

import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.remoting.Callable;

import java.io.IOException;

public class Remote {

	public void test(//
			final AbstractBuild build, //
			final Launcher launcher, //
			final BuildListener listener //
	) {

		final Callable<String, IOException> task = new Callable<String, IOException>() {

			private static final long serialVersionUID = 3386611994549331353L;

			public String call() throws IOException {
				return "";
			}
		};

		try {
			launcher.getChannel().call(task);
		} catch (final Exception e) {
			e.printStackTrace();
		}

	}

}

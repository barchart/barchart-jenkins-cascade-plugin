package design;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Resource;
import hudson.model.ResourceActivity;
import hudson.model.ResourceList;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;

import java.io.IOException;

import org.kohsuke.stapler.DataBoundConstructor;

import com.barchart.jenkins.cascade.ProjectIdentity;

/**
 * 
 */
@Extension
public class ResourceBuildWrapper extends BuildWrapper implements
		ResourceActivity {

	public static class TheDescriptor extends BuildWrapperDescriptor {

		@Override
		public String getDisplayName() {
			return "Resource Wrapper";
		}

		@Override
		public boolean isApplicable(final AbstractProject<?, ?> project) {
			final ProjectIdentity identity = ProjectIdentity.identity(project);
			return identity != null;
		}

	}

	@Extension
	public static final TheDescriptor META = new TheDescriptor();

	@DataBoundConstructor
	public ResourceBuildWrapper() {

	}

	@Override
	public Environment setUp(final AbstractBuild abstractBuild,
			final Launcher launcher, final BuildListener buildListener)
			throws IOException, InterruptedException {
		return new Environment() {
		};
	}

	public ResourceList getResourceList() {
		final ResourceList list = new ResourceList();
		final Resource resource = new Resource("ZZZ");
		list.w(resource);
		return list;
	}

	public String getDisplayName() {
		return "AAA";
	}

}

package com.hp.octane.plugins.jenkins.model.pipeline;

import com.hp.octane.plugins.jenkins.model.scm.SCMData;
import com.hp.octane.plugins.jenkins.model.scm.SCMDataFactory;
import com.tikal.jenkins.plugins.multijob.MultiJobBuilder;
import com.tikal.jenkins.plugins.multijob.MultiJobProject;
import com.tikal.jenkins.plugins.multijob.PhaseJobsConfig;
import hudson.matrix.MatrixProject;
import hudson.maven.MavenModuleSet;
import hudson.model.AbstractProject;
import hudson.model.Project;
import hudson.plugins.parameterizedtrigger.BlockableBuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.TriggerBuilder;
import hudson.tasks.Builder;
import jenkins.model.Jenkins;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: gullery
 * Date: 20/11/14
 * Time: 09:15
 * To change this template use File | Settings | File Templates.
 */
public final class FlowItem {
	private String name;
	private int order;
	private String phase;
	private boolean async;
	private SCMData scmData;
	private ArrayList<FlowItem> steps;

	public FlowItem(AbstractProject project, String phase, boolean async, int order) {
		this.name = project.getName();
		this.order = order;
		this.phase = phase;
		this.async = async;
		this.scmData = SCMDataFactory.create(project);
		this.steps = new ArrayList<FlowItem>();

		if (project instanceof MultiJobProject) {
			appendSteps((MultiJobProject) project);
		} else if (project instanceof hudson.model.Project) {
			appendSteps((Project) project);
		} else if (project instanceof MatrixProject) {
			appendSteps((MatrixProject) project);
		} else if (project instanceof MavenModuleSet) {
			appendSteps((MavenModuleSet) project);
		}
	}

	private void appendSteps(Project project) {
		processPhases(project, project.getBuilders(), "build");
		processProjects(project.getDownstreamProjects(), "downstream", true);
	}

	private void appendSteps(MultiJobProject multiJobProject) {
		processPhases(multiJobProject, multiJobProject.getBuilders(), null);
		//  TODO: this is HACK to handle the bug in the MultiJob downstream projects definition (all of the jobs are taken as downstream as well)
		//  remove all of the multi job projects from the downstreamList and the rest append as downstream
	}

	private void appendSteps(MatrixProject matrixProject) {
		processPhases(matrixProject, matrixProject.getBuilders(), "build");
		processProjects(matrixProject.getDownstreamProjects(), "downstream", true);
	}

	private void appendSteps(MavenModuleSet mavenProject) {
		processPhases(mavenProject, mavenProject.getPrebuilders(), "pre-build");
		processPhases(mavenProject, mavenProject.getPostbuilders(), "post-build");
		processProjects(mavenProject.getDownstreamProjects(), "downstream", true);
	}

	private void processPhases(AbstractProject project, List<Builder> builders, String phaseName) {
		for (Builder builder : builders) {
			if (builder instanceof TriggerBuilder) {
				TriggerBuilder tb = (TriggerBuilder) builder;
				for (BlockableBuildTriggerConfig config : tb.getConfigs()) {
					processProjects(config.getProjectList(project.getParent(), null), phaseName, config.getBlock() == null);
				}
			} else if (builder instanceof MultiJobBuilder) {
				MultiJobBuilder mjb = (MultiJobBuilder) builder;
				ArrayList<AbstractProject> subSteps = new ArrayList<AbstractProject>();
				for (PhaseJobsConfig config : mjb.getPhaseJobs()) {
					subSteps.add((AbstractProject) Jenkins.getInstance().getItem(config.getJobName()));
				}
				processProjects(subSteps, mjb.getPhaseName(), false);
			} else {
				System.out.println("not yet supported build action: " + builder.getClass().getName());
				//  TODO: probably we need to add the support for more stuff like:
				//      org.jenkinsci.plugins.conditionalbuildstep.singlestep.SingleConditionalBuilder
				//      org.jenkinsci.plugins.conditionalbuildstep.ConditionalBuilder
			}
		}
	}

	private void processProjects(List<AbstractProject> projects, String phase, boolean async) {
		FlowItem tmpItem;
		if (projects != null && projects.size() > 0) {
			for (AbstractProject project : projects) {

				//  get the phase name, async and order from the project internal data

				tmpItem = new FlowItem(project, phase, async, 0);
				steps.add(tmpItem);
			}
		}
	}

	public JSONObject toJSON() {
		return null;
	}

	public void fromJSON(JSONObject jsonObject) {
	}
}
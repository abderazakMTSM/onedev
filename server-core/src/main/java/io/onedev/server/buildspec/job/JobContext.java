package io.onedev.server.buildspec.job;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jgit.lib.ObjectId;

import io.onedev.k8shelper.Action;
import io.onedev.k8shelper.CloneInfo;
import io.onedev.server.buildspec.Service;
import io.onedev.server.util.SimpleLogger;

public abstract class JobContext {
	
	private final String projectName;
	
	private final Long buildNumber;
	
	private final File projectGitDir;
	
	private final List<Action> actions;
	
	private final boolean retrieveSource;
	
	private final Integer cloneDepth;
	
	private final CloneInfo cloneInfo;
	
	private final String cpuRequirement;
	
	private final String memoryRequirement;
	
	private final ObjectId commitId;
	
	private final Collection<CacheSpec> cacheSpecs; 
	
	private final List<Service> services;
	
	private final int cacheTTL;
	
	private final int retried;
	
	private final SimpleLogger logger;	
	
	private final Collection<String> allocatedCaches = new HashSet<>();
	
	private final Map<String, Integer> cacheCounts = new ConcurrentHashMap<>();
	
	public JobContext(String projectName, Long buildNumber, File projectGitDir, 
			List<Action> actions, boolean retrieveSource, Integer cloneDepth, 
			CloneInfo cloneInfo, String cpuRequirement, String memoryRequirement, 
			ObjectId commitId, Collection<CacheSpec> caches, int cacheTTL, 
			int retried, List<Service> services, SimpleLogger logger) {
		this.projectName = projectName;
		this.buildNumber = buildNumber;
		this.projectGitDir = projectGitDir;
		this.actions = actions;
		this.retrieveSource = retrieveSource;
		this.cloneDepth = cloneDepth;
		this.cloneInfo = cloneInfo;
		this.cpuRequirement = cpuRequirement;
		this.memoryRequirement = memoryRequirement;
		this.commitId = commitId;
		this.cacheSpecs = caches;
		this.cacheTTL = cacheTTL;
		this.retried = retried;
		this.services = services;
		this.logger = logger;
	}
	
	public String getProjectName() {
		return projectName;
	}

	public Long getBuildNumber() {
		return buildNumber;
	}

	public File getProjectGitDir() {
		return projectGitDir;
	}

	public List<Action> getActions() {
		return actions;
	}

	public ObjectId getCommitId() {
		return commitId;
	}

	public boolean isRetrieveSource() {
		return retrieveSource;
	}

	public Integer getCloneDepth() {
		return cloneDepth;
	}

	public CloneInfo getCloneInfo() {
		return cloneInfo;
	}

	public String getCpuRequirement() {
		return cpuRequirement;
	}

	public String getMemoryRequirement() {
		return memoryRequirement;
	}

	public Collection<CacheSpec> getCacheSpecs() {
		return cacheSpecs;
	}

	public SimpleLogger getLogger() {
		return logger;
	}
	
	public int getCacheTTL() {
		return cacheTTL;
	}

	public int getRetried() {
		return retried;
	}

	public Collection<String> getAllocatedCaches() {
		return allocatedCaches;
	}

	public List<Service> getServices() {
		return services;
	}

	public Map<String, Integer> getCacheCounts() {
		return cacheCounts;
	}

	public abstract void notifyJobRunning();
	
	public abstract void reportJobWorkspace(String jobWorkspace);
	
	public abstract void runServerStep(List<Integer> stepPosition, File filesDir, SimpleLogger logger);
	
	public abstract void copyDependencies(File targetDir);
	
}

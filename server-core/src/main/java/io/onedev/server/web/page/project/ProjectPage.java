package io.onedev.server.web.page.project;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.jgit.lib.ObjectId;

import com.google.common.collect.Lists;

import io.onedev.server.OneDev;
import io.onedev.server.entitymanager.BuildManager;
import io.onedev.server.entitymanager.ProjectManager;
import io.onedev.server.entitymanager.SettingManager;
import io.onedev.server.model.Build;
import io.onedev.server.model.Project;
import io.onedev.server.security.SecurityUtils;
import io.onedev.server.web.avatar.AvatarManager;
import io.onedev.server.web.component.floating.FloatingPanel;
import io.onedev.server.web.component.project.info.ProjectInfoPanel;
import io.onedev.server.web.component.project.path.ProjectPathPanel;
import io.onedev.server.web.editable.EditableUtils;
import io.onedev.server.web.page.layout.LayoutPage;
import io.onedev.server.web.page.layout.SidebarMenu;
import io.onedev.server.web.page.layout.SidebarMenuItem;
import io.onedev.server.web.page.project.blob.ProjectBlobPage;
import io.onedev.server.web.page.project.branches.ProjectBranchesPage;
import io.onedev.server.web.page.project.builds.ProjectBuildsPage;
import io.onedev.server.web.page.project.builds.detail.BuildDetailPage;
import io.onedev.server.web.page.project.builds.detail.InvalidBuildPage;
import io.onedev.server.web.page.project.children.ProjectChildrenPage;
import io.onedev.server.web.page.project.codecomments.ProjectCodeCommentsPage;
import io.onedev.server.web.page.project.commits.CommitDetailPage;
import io.onedev.server.web.page.project.commits.ProjectCommitsPage;
import io.onedev.server.web.page.project.compare.RevisionComparePage;
import io.onedev.server.web.page.project.issues.boards.IssueBoardsPage;
import io.onedev.server.web.page.project.issues.create.NewIssuePage;
import io.onedev.server.web.page.project.issues.detail.IssueDetailPage;
import io.onedev.server.web.page.project.issues.list.ProjectIssueListPage;
import io.onedev.server.web.page.project.issues.milestones.MilestoneDetailPage;
import io.onedev.server.web.page.project.issues.milestones.MilestoneEditPage;
import io.onedev.server.web.page.project.issues.milestones.MilestoneListPage;
import io.onedev.server.web.page.project.issues.milestones.NewMilestonePage;
import io.onedev.server.web.page.project.pullrequests.InvalidPullRequestPage;
import io.onedev.server.web.page.project.pullrequests.ProjectPullRequestsPage;
import io.onedev.server.web.page.project.pullrequests.create.NewPullRequestPage;
import io.onedev.server.web.page.project.pullrequests.detail.PullRequestDetailPage;
import io.onedev.server.web.page.project.setting.ContributedProjectSetting;
import io.onedev.server.web.page.project.setting.ProjectSettingContribution;
import io.onedev.server.web.page.project.setting.authorization.ProjectAuthorizationsPage;
import io.onedev.server.web.page.project.setting.avatar.AvatarEditPage;
import io.onedev.server.web.page.project.setting.branchprotection.BranchProtectionsPage;
import io.onedev.server.web.page.project.setting.build.ActionAuthorizationsPage;
import io.onedev.server.web.page.project.setting.build.BuildPreservationsPage;
import io.onedev.server.web.page.project.setting.build.DefaultFixedIssueFiltersPage;
import io.onedev.server.web.page.project.setting.build.JobSecretsPage;
import io.onedev.server.web.page.project.setting.general.GeneralProjectSettingPage;
import io.onedev.server.web.page.project.setting.pluginsettings.ContributedProjectSettingPage;
import io.onedev.server.web.page.project.setting.servicedesk.ProjectServiceDeskSettingPage;
import io.onedev.server.web.page.project.setting.tagprotection.TagProtectionsPage;
import io.onedev.server.web.page.project.setting.webhook.WebHooksPage;
import io.onedev.server.web.page.project.stats.ProjectContribsPage;
import io.onedev.server.web.page.project.stats.SourceLinesPage;
import io.onedev.server.web.page.project.tags.ProjectTagsPage;
import io.onedev.server.web.util.ProjectAware;

@SuppressWarnings("serial")
public abstract class ProjectPage extends LayoutPage implements ProjectAware {

	protected static final String PARAM_PROJECT = "project";
	
	protected final IModel<Project> projectModel;
	
	private transient Map<ObjectId, Collection<Build>> buildsCache;
	
	public static PageParameters paramsOf(Project project) {
		PageParameters params = new PageParameters();
		params.add(PARAM_PROJECT, project.getId());
		return params;
	}
	
	public ProjectPage(PageParameters params) {
		super(params);
		
		Long projectId = params.get(PARAM_PROJECT).toOptionalLong();
		if (projectId == null)
			throw new RestartResponseException(ProjectListPage.class);
		
		Project project = OneDev.getInstance(ProjectManager.class).load(projectId);
		projectModel = new LoadableDetachableModel<Project>() {

			@Override
			protected Project load() {
				Project project = OneDev.getInstance(ProjectManager.class).load(projectId);
				
				/*
				 * Give child page a chance to cache object id of known revisions upon
				 * loading the project object 
				 */
				for (Map.Entry<String, ObjectId> entry: getObjectIdCache().entrySet()) {
					project.cacheObjectId(entry.getKey(), entry.getValue());
				}
				return project;
			}
			
		};
		projectModel.setObject(project);
	}
	
	protected Map<String, ObjectId> getObjectIdCache() {
		return new HashMap<>();
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
	}

	@Override
	protected boolean isPermitted() {
		return SecurityUtils.canAccess(getProject());
	}
	
	@Override
	public Project getProject() {
		return projectModel.getObject();
	}
	
	@Override
	protected List<SidebarMenu> getSidebarMenus() {
		List<SidebarMenu> menus = super.getSidebarMenus();
		
		List<SidebarMenuItem> menuItems = new ArrayList<>();
		
		if (getProject().isCodeManagement() && SecurityUtils.canReadCode(getProject())) {
			List<SidebarMenuItem> codeMenuItems = new ArrayList<>();
	
			codeMenuItems.add(new SidebarMenuItem.Page(null, "Files", 
					ProjectBlobPage.class, ProjectBlobPage.paramsOf(getProject())));
			codeMenuItems.add(new SidebarMenuItem.Page(null, "Commits", 
					ProjectCommitsPage.class, ProjectCommitsPage.paramsOf(getProject(), null), 
					Lists.newArrayList(CommitDetailPage.class)));
			codeMenuItems.add(new SidebarMenuItem.Page(null, "Branches", 
					ProjectBranchesPage.class, ProjectBranchesPage.paramsOf(getProject())));
			codeMenuItems.add(new SidebarMenuItem.Page(null, "Tags", 
					ProjectTagsPage.class, ProjectTagsPage.paramsOf(getProject())));
			codeMenuItems.add(new SidebarMenuItem.Page(null, "Pull Requests", 
					ProjectPullRequestsPage.class, ProjectPullRequestsPage.paramsOf(getProject(), 0), 
					Lists.newArrayList(NewPullRequestPage.class, PullRequestDetailPage.class, InvalidPullRequestPage.class)));
			codeMenuItems.add(new SidebarMenuItem.Page(null, "Code Comments", 
					ProjectCodeCommentsPage.class, ProjectCodeCommentsPage.paramsOf(getProject(), 0)));
			codeMenuItems.add(new SidebarMenuItem.Page(null, "Code Compare", 
					RevisionComparePage.class, RevisionComparePage.paramsOf(getProject())));
			
			menuItems.add(new SidebarMenuItem.SubMenu("git", "Code", codeMenuItems));
		}		
		if (getProject().isIssueManagement()) {
			List<SidebarMenuItem> issueMenuItems = new ArrayList<>();
			
			issueMenuItems.add(new SidebarMenuItem.Page(null, "List", 
					ProjectIssueListPage.class, ProjectIssueListPage.paramsOf(getProject(), 0), 
					Lists.newArrayList(NewIssuePage.class, IssueDetailPage.class)));
			issueMenuItems.add(new SidebarMenuItem.Page(null, "Boards", 
					IssueBoardsPage.class, IssueBoardsPage.paramsOf(getProject())));
			issueMenuItems.add(new SidebarMenuItem.Page(null, "Milestones", 
					MilestoneListPage.class, MilestoneListPage.paramsOf(getProject(), false, null), 
					Lists.newArrayList(NewMilestonePage.class, MilestoneDetailPage.class, MilestoneEditPage.class)));
			
			menuItems.add(new SidebarMenuItem.SubMenu("bug", "Issues", issueMenuItems));
		}
		
		if (getProject().isCodeManagement()) {
			menuItems.add(new SidebarMenuItem.Page("play-circle", "Builds", 
					ProjectBuildsPage.class, ProjectBuildsPage.paramsOf(getProject(), 0), 
					Lists.newArrayList(BuildDetailPage.class, InvalidBuildPage.class)));
		}
		
		List<SidebarMenuItem> statsMenuItems = new ArrayList<>();
		
		if (getProject().isCodeManagement() && SecurityUtils.canReadCode(getProject())) {
			statsMenuItems.add(new SidebarMenuItem.Page(null, "Contributions", 
					ProjectContribsPage.class, ProjectContribsPage.paramsOf(getProject())));
			statsMenuItems.add(new SidebarMenuItem.Page(null, "Source Lines", 
					SourceLinesPage.class, SourceLinesPage.paramsOf(getProject())));
		}
		
		List<StatisticsMenuContribution> contributions = new ArrayList<>(OneDev.getExtensions(StatisticsMenuContribution.class));
		contributions.sort(Comparator.comparing(StatisticsMenuContribution::getOrder));
		
		for (StatisticsMenuContribution contribution: contributions)
			statsMenuItems.addAll(contribution.getMenuItems(getProject()));
		
		if (!statsMenuItems.isEmpty())
			menuItems.add(new SidebarMenuItem.SubMenu("statistics", "Statistics", statsMenuItems));
		
		menuItems.add(new SidebarMenuItem.Page("tree", "Child Projects", 
				ProjectChildrenPage.class, ProjectChildrenPage.paramsOf(getProject(), null, 0)));
		
		if (SecurityUtils.canManage(getProject())) {
			List<SidebarMenuItem> settingMenuItems = new ArrayList<>();
			settingMenuItems.add(new SidebarMenuItem.Page(null, "General Setting", 
					GeneralProjectSettingPage.class, GeneralProjectSettingPage.paramsOf(getProject())));
			settingMenuItems.add(new SidebarMenuItem.Page(null, "Edit Avatar", 
					AvatarEditPage.class, AvatarEditPage.paramsOf(getProject())));
			settingMenuItems.add(new SidebarMenuItem.Page(null, "Authorizations", 
					ProjectAuthorizationsPage.class, ProjectAuthorizationsPage.paramsOf(getProject())));
			
			settingMenuItems.add(new SidebarMenuItem.Page(null, "Branch Protection", 
					BranchProtectionsPage.class, BranchProtectionsPage.paramsOf(getProject())));
			settingMenuItems.add(new SidebarMenuItem.Page(null, "Tag Protection", 
					TagProtectionsPage.class, TagProtectionsPage.paramsOf(getProject())));
			
			List<SidebarMenuItem> buildSettingMenuItems = new ArrayList<>();
			
			buildSettingMenuItems.add(new SidebarMenuItem.Page(null, "Job Secrets", 
					JobSecretsPage.class, JobSecretsPage.paramsOf(getProject())));
			buildSettingMenuItems.add(new SidebarMenuItem.Page(null, "Action Authorizations", 
					ActionAuthorizationsPage.class, ActionAuthorizationsPage.paramsOf(getProject())));
			buildSettingMenuItems.add(new SidebarMenuItem.Page(null, "Build Preserve Rules", 
					BuildPreservationsPage.class, BuildPreservationsPage.paramsOf(getProject())));
			buildSettingMenuItems.add(new SidebarMenuItem.Page(null, "Default Fixed Issue Filters", 
					DefaultFixedIssueFiltersPage.class, DefaultFixedIssueFiltersPage.paramsOf(getProject())));
			
			settingMenuItems.add(new SidebarMenuItem.SubMenu(null, "Build Setting", buildSettingMenuItems));

			if (OneDev.getInstance(SettingManager.class).getServiceDeskSetting() != null && getProject().isIssueManagement()) {
				settingMenuItems.add(new SidebarMenuItem.Page(null, "Service Desk Setting", 
						ProjectServiceDeskSettingPage.class, ProjectServiceDeskSettingPage.paramsOf(getProject())));
			}
			
			settingMenuItems.add(new SidebarMenuItem.Page(null, "Web Hooks", 
					WebHooksPage.class, WebHooksPage.paramsOf(getProject())));
			
			for (ProjectSettingContribution contribution:OneDev.getExtensions(ProjectSettingContribution.class)) {
				for (Class<? extends ContributedProjectSetting> settingClass: contribution.getSettingClasses()) {
					settingMenuItems.add(new SidebarMenuItem.Page(
							null, 
							EditableUtils.getDisplayName(settingClass), 
							ContributedProjectSettingPage.class, 
							ContributedProjectSettingPage.paramsOf(getProject(), settingClass)));
				}
			}
			
			menuItems.add(new SidebarMenuItem.SubMenu("sliders", "Settings", settingMenuItems));
		}
		
		String avatarUrl = OneDev.getInstance(AvatarManager.class).getAvatarUrl(getProject());
		SidebarMenu.Header menuHeader = new SidebarMenu.Header(avatarUrl, getProject().getName()) {
			
			@Override
			protected Component newMoreInfo(String componentId, FloatingPanel dropdown) {
				return new ProjectInfoPanel(componentId, projectModel) {

					@Override
					protected void onPromptForkOption(AjaxRequestTarget target) {
						dropdown.close();
					}
					
				};
			}
			
		};
		menus.add(new SidebarMenu(menuHeader, menuItems));
		return menus;
	}

	@Override
	protected void onDetach() {
		projectModel.detach();
		super.onDetach();
	}

	@Override
	protected Component newTopbarTitle(String componentId) {
		Fragment fragment = new Fragment(componentId, "topbarTitleFrag", this);
		fragment.add(new BookmarkablePageLink<Void>("projects", ProjectListPage.class));
		
		fragment.add(new ProjectPathPanel("projectPath", projectModel) {

			@Override
			protected Component newSeparator(String componentId) {
				return new Label(componentId).add(AttributeAppender.append("class", "dot"));
			}
			
		});
		
		fragment.add(newProjectTitle("projectTitle"));
		return fragment;
	}

	@Override
	protected String getPageTitle() {
		return getProject().getPath();
	}
	
	protected abstract Component newProjectTitle(String componentId);
	
	protected Collection<Build> getBuilds(ObjectId commitId) {
		if (buildsCache == null)
			buildsCache = new HashMap<>();
		Collection<Build> builds = buildsCache.get(commitId);
		if (builds == null) {
			BuildManager buildManager = OneDev.getInstance(BuildManager.class);
			builds = buildManager.query(getProject(), commitId, null, null, null, new HashMap<>(), null);
			buildsCache.put(commitId, builds);
		}
		return builds;
	}

}

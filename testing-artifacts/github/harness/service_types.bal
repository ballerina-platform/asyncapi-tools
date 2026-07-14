public type DeleteService service object {
    remote function onDelete(DeletePayload event) returns error?;
};

public type MetaService service object {
    remote function onMeta(MetaPayload event) returns error?;
};

public type WorkflowDispatchService service object {
    remote function onWorkflowDispatch(WorkflowDispatchPayload event) returns error?;
};

public type SecurityAndAnalysisService service object {
    remote function onSecurityAndAnalysis(SecurityAndAnalysisPayload event) returns error?;
};

public type DeployKeyService service object {
    remote function onDeployKeyCreated(DeployKeyPayload event) returns error?;
    remote function onDeployKeyDeleted(DeployKeyPayload event) returns error?;
};

public type ProjectColumnService service object {
    remote function onProjectColumnMoved(ProjectColumnPayload event) returns error?;
    remote function onProjectColumnEdited(ProjectColumnPayload event) returns error?;
    remote function onProjectColumnDeleted(ProjectColumnPayload event) returns error?;
    remote function onProjectColumnCreated(ProjectColumnPayload event) returns error?;
};

public type MarketplacePurchaseService service object {
    remote function onMarketplacePurchasePurchased(MarketplacePurchasePayload event) returns error?;
    remote function onMarketplacePurchaseCancelled(MarketplacePurchasePayload event) returns error?;
    remote function onMarketplacePurchasePendingChangeCancelled(MarketplacePurchasePayload event) returns error?;
    remote function onMarketplacePurchasePendingChange(MarketplacePurchasePayload event) returns error?;
    remote function onMarketplacePurchaseChanged(MarketplacePurchasePayload event) returns error?;
};

public type BranchProtectionConfigurationService service object {
    remote function onBranchProtectionConfigurationEnabled(BranchProtectionConfigurationPayload event) returns error?;
    remote function onBranchProtectionConfigurationDisabled(BranchProtectionConfigurationPayload event) returns error?;
};

public type PullRequestService service object {
    remote function onPullRequestEnqueued(PullRequestPayload event) returns error?;
    remote function onPullRequestReviewRequestRemoved(PullRequestPayload event) returns error?;
    remote function onPullRequestOpened(PullRequestPayload event) returns error?;
    remote function onPullRequestReadyForReview(PullRequestPayload event) returns error?;
    remote function onPullRequestLabeled(PullRequestPayload event) returns error?;
    remote function onPullRequestUnassigned(PullRequestPayload event) returns error?;
    remote function onPullRequestEdited(PullRequestPayload event) returns error?;
    remote function onPullRequestSynchronize(PullRequestPayload event) returns error?;
    remote function onPullRequestReviewRequested(PullRequestPayload event) returns error?;
    remote function onPullRequestReopened(PullRequestPayload event) returns error?;
    remote function onPullRequestAutoMergeDisabled(PullRequestPayload event) returns error?;
    remote function onPullRequestLocked(PullRequestPayload event) returns error?;
    remote function onPullRequestAutoMergeEnabled(PullRequestPayload event) returns error?;
    remote function onPullRequestMilestoned(PullRequestPayload event) returns error?;
    remote function onPullRequestDequeued(PullRequestPayload event) returns error?;
    remote function onPullRequestUnlabeled(PullRequestPayload event) returns error?;
    remote function onPullRequestClosed(PullRequestPayload event) returns error?;
    remote function onPullRequestUnlocked(PullRequestPayload event) returns error?;
    remote function onPullRequestAssigned(PullRequestPayload event) returns error?;
    remote function onPullRequestConvertedToDraft(PullRequestPayload event) returns error?;
    remote function onPullRequestDemilestoned(PullRequestPayload event) returns error?;
};

public type LabelService service object {
    remote function onLabelEdited(LabelPayload event) returns error?;
    remote function onLabelCreated(LabelPayload event) returns error?;
    remote function onLabelDeleted(LabelPayload event) returns error?;
};

public type DeploymentService service object {
    remote function onDeployment(DeploymentPayload event) returns error?;
};

public type TeamAddService service object {
    remote function onTeamAdd(TeamAddPayload event) returns error?;
};

public type CodeScanningAlertService service object {
    remote function onCodeScanningAlertAppearedInBranch(CodeScanningAlertPayload event) returns error?;
    remote function onCodeScanningAlertClosedByUser(CodeScanningAlertPayload event) returns error?;
    remote function onCodeScanningAlertCreated(CodeScanningAlertPayload event) returns error?;
    remote function onCodeScanningAlertFixed(CodeScanningAlertPayload event) returns error?;
    remote function onCodeScanningAlertReopened(CodeScanningAlertPayload event) returns error?;
    remote function onCodeScanningAlertReopenedByUser(CodeScanningAlertPayload event) returns error?;
    remote function onCodeScanningAlertUpdatedAssignment(CodeScanningAlertPayload event) returns error?;
};

public type MembershipService service object {
    remote function onMembershipAdded(MembershipPayload event) returns error?;
    remote function onMembershipRemoved(MembershipPayload event) returns error?;
};

public type SecretScanningAlertService service object {
    remote function onSecretScanningAlertAssigned(SecretScanningAlertPayload event) returns error?;
    remote function onSecretScanningAlertReopened(SecretScanningAlertPayload event) returns error?;
    remote function onSecretScanningAlertUnassigned(SecretScanningAlertPayload event) returns error?;
    remote function onSecretScanningAlertCreated(SecretScanningAlertPayload event) returns error?;
    remote function onSecretScanningAlertPubliclyLeaked(SecretScanningAlertPayload event) returns error?;
    remote function onSecretScanningAlertValidated(SecretScanningAlertPayload event) returns error?;
    remote function onSecretScanningAlertResolved(SecretScanningAlertPayload event) returns error?;
};

public type PushService service object {
    remote function onPush(PushPayload event) returns error?;
};

public type MemberService service object {
    remote function onMemberEdited(MemberPayload event) returns error?;
    remote function onMemberAdded(MemberPayload event) returns error?;
    remote function onMemberRemoved(MemberPayload event) returns error?;
};

public type RepositoryDispatchService service object {
    remote function onRepositoryDispatch(RepositoryDispatchPayload event) returns error?;
};

public type StatusService service object {
    remote function onStatus(StatusPayload event) returns error?;
};

public type RepositoryImportService service object {
    remote function onRepositoryImport(RepositoryImportPayload event) returns error?;
};

public type PersonalAccessTokenRequestService service object {
    remote function onPersonalAccessTokenRequestCreated(PersonalAccessTokenRequestPayload event) returns error?;
    remote function onPersonalAccessTokenRequestApproved(PersonalAccessTokenRequestPayload event) returns error?;
    remote function onPersonalAccessTokenRequestDenied(PersonalAccessTokenRequestPayload event) returns error?;
    remote function onPersonalAccessTokenRequestCancelled(PersonalAccessTokenRequestPayload event) returns error?;
};

public type SubIssuesService service object {
    remote function onSubIssuesSubIssueAdded(SubIssuesPayload event) returns error?;
    remote function onSubIssuesParentIssueAdded(SubIssuesPayload event) returns error?;
    remote function onSubIssuesSubIssueRemoved(SubIssuesPayload event) returns error?;
    remote function onSubIssuesParentIssueRemoved(SubIssuesPayload event) returns error?;
};

public type RepositoryRulesetService service object {
    remote function onRepositoryRulesetCreated(RepositoryRulesetPayload event) returns error?;
    remote function onRepositoryRulesetEdited(RepositoryRulesetPayload event) returns error?;
    remote function onRepositoryRulesetDeleted(RepositoryRulesetPayload event) returns error?;
};

public type MilestoneService service object {
    remote function onMilestoneCreated(MilestonePayload event) returns error?;
    remote function onMilestoneEdited(MilestonePayload event) returns error?;
    remote function onMilestoneOpened(MilestonePayload event) returns error?;
    remote function onMilestoneDeleted(MilestonePayload event) returns error?;
    remote function onMilestoneClosed(MilestonePayload event) returns error?;
};

public type PublicService service object {
    remote function onPublic(PublicPayload event) returns error?;
};

public type WorkflowRunService service object {
    remote function onWorkflowRunInProgress(WorkflowRunPayload event) returns error?;
    remote function onWorkflowRunCompleted(WorkflowRunPayload event) returns error?;
    remote function onWorkflowRunRequested(WorkflowRunPayload event) returns error?;
};

public type ProjectsV2statusUpdateService service object {
    remote function onProjectsV2StatusUpdateEdited('ProjectsV2StatusUpdatePayload event) returns error?;
    remote function onProjectsV2StatusUpdateDeleted('ProjectsV2StatusUpdatePayload event) returns error?;
    remote function onProjectsV2StatusUpdateCreated('ProjectsV2StatusUpdatePayload event) returns error?;
};

public type ProjectsV2itemService service object {
    remote function onProjectsV2ItemEdited('ProjectsV2ItemPayload event) returns error?;
    remote function onProjectsV2ItemCreated('ProjectsV2ItemPayload event) returns error?;
    remote function onProjectsV2ItemArchived('ProjectsV2ItemPayload event) returns error?;
    remote function onProjectsV2ItemDeleted('ProjectsV2ItemPayload event) returns error?;
    remote function onProjectsV2ItemRestored('ProjectsV2ItemPayload event) returns error?;
    remote function onProjectsV2ItemReordered('ProjectsV2ItemPayload event) returns error?;
    remote function onProjectsV2ItemConverted('ProjectsV2ItemPayload event) returns error?;
};

public type SponsorshipService service object {
    remote function onSponsorshipCancelled(SponsorshipPayload event) returns error?;
    remote function onSponsorshipEdited(SponsorshipPayload event) returns error?;
    remote function onSponsorshipTierChanged(SponsorshipPayload event) returns error?;
    remote function onSponsorshipPendingCancellation(SponsorshipPayload event) returns error?;
    remote function onSponsorshipCreated(SponsorshipPayload event) returns error?;
    remote function onSponsorshipPendingTierChange(SponsorshipPayload event) returns error?;
};

public type MergeGroupService service object {
    remote function onMergeGroupDestroyed(MergeGroupPayload event) returns error?;
    remote function onMergeGroupChecksRequested(MergeGroupPayload event) returns error?;
};

public type ProjectService service object {
    remote function onProjectDeleted(ProjectPayload event) returns error?;
    remote function onProjectCreated(ProjectPayload event) returns error?;
    remote function onProjectClosed(ProjectPayload event) returns error?;
    remote function onProjectReopened(ProjectPayload event) returns error?;
    remote function onProjectEdited(ProjectPayload event) returns error?;
};

public type OrgBlockService service object {
    remote function onOrgBlockBlocked(OrgBlockPayload event) returns error?;
    remote function onOrgBlockUnblocked(OrgBlockPayload event) returns error?;
};

public type SecretScanningAlertLocationService service object {
    remote function onSecretScanningAlertLocation(SecretScanningAlertLocationPayload event) returns error?;
};

public type InstallationTargetService service object {
    remote function onInstallationTarget(InstallationTargetPayload event) returns error?;
};

public type CheckSuiteService service object {
    remote function onCheckSuiteCompleted(CheckSuitePayload event) returns error?;
    remote function onCheckSuiteRequested(CheckSuitePayload event) returns error?;
    remote function onCheckSuiteRerequested(CheckSuitePayload event) returns error?;
};

public type PingService service object {
    remote function onPing(PingPayload event) returns error?;
};

public type IssueCommentService service object {
    remote function onIssueCommentEdited(IssueCommentPayload event) returns error?;
    remote function onIssueCommentPinned(IssueCommentPayload event) returns error?;
    remote function onIssueCommentDeleted(IssueCommentPayload event) returns error?;
    remote function onIssueCommentCreated(IssueCommentPayload event) returns error?;
    remote function onIssueCommentUnpinned(IssueCommentPayload event) returns error?;
};

public type SecurityAdvisoryService service object {
    remote function onSecurityAdvisoryWithdrawn(SecurityAdvisoryPayload event) returns error?;
    remote function onSecurityAdvisoryPublished(SecurityAdvisoryPayload event) returns error?;
    remote function onSecurityAdvisoryUpdated(SecurityAdvisoryPayload event) returns error?;
};

public type PackageService service object {
    remote function onPackagePublished(PackagePayload event) returns error?;
    remote function onPackageUpdated(PackagePayload event) returns error?;
};

public type DiscussionService service object {
    remote function onDiscussionUnanswered(DiscussionPayload event) returns error?;
    remote function onDiscussionCreated(DiscussionPayload event) returns error?;
    remote function onDiscussionTransferred(DiscussionPayload event) returns error?;
    remote function onDiscussionCategoryChanged(DiscussionPayload event) returns error?;
    remote function onDiscussionDeleted(DiscussionPayload event) returns error?;
    remote function onDiscussionUnlocked(DiscussionPayload event) returns error?;
    remote function onDiscussionPinned(DiscussionPayload event) returns error?;
    remote function onDiscussionEdited(DiscussionPayload event) returns error?;
    remote function onDiscussionReopened(DiscussionPayload event) returns error?;
    remote function onDiscussionAnswered(DiscussionPayload event) returns error?;
    remote function onDiscussionClosed(DiscussionPayload event) returns error?;
    remote function onDiscussionUnlabeled(DiscussionPayload event) returns error?;
    remote function onDiscussionLabeled(DiscussionPayload event) returns error?;
    remote function onDiscussionUnpinned(DiscussionPayload event) returns error?;
    remote function onDiscussionLocked(DiscussionPayload event) returns error?;
};

public type ForkService service object {
    remote function onFork(ForkPayload event) returns error?;
};

public type PullRequestReviewService service object {
    remote function onPullRequestReviewSubmitted(PullRequestReviewPayload event) returns error?;
    remote function onPullRequestReviewEdited(PullRequestReviewPayload event) returns error?;
    remote function onPullRequestReviewDismissed(PullRequestReviewPayload event) returns error?;
};

public type OrganizationService service object {
    remote function onOrganizationMemberAdded(OrganizationPayload event) returns error?;
    remote function onOrganizationMemberRemoved(OrganizationPayload event) returns error?;
    remote function onOrganizationDeleted(OrganizationPayload event) returns error?;
    remote function onOrganizationRenamed(OrganizationPayload event) returns error?;
    remote function onOrganizationMemberInvited(OrganizationPayload event) returns error?;
};

public type IssuesService service object {
    remote function onIssuesReopened(IssuesPayload event) returns error?;
    remote function onIssuesTransferred(IssuesPayload event) returns error?;
    remote function onIssuesUnpinned(IssuesPayload event) returns error?;
    remote function onIssuesAssigned(IssuesPayload event) returns error?;
    remote function onIssuesMilestoned(IssuesPayload event) returns error?;
    remote function onIssuesLabeled(IssuesPayload event) returns error?;
    remote function onIssuesOpened(IssuesPayload event) returns error?;
    remote function onIssuesPinned(IssuesPayload event) returns error?;
    remote function onIssuesTyped(IssuesPayload event) returns error?;
    remote function onIssuesEdited(IssuesPayload event) returns error?;
    remote function onIssuesUntyped(IssuesPayload event) returns error?;
    remote function onIssuesDemilestoned(IssuesPayload event) returns error?;
    remote function onIssuesLocked(IssuesPayload event) returns error?;
    remote function onIssuesUnassigned(IssuesPayload event) returns error?;
    remote function onIssuesUnlocked(IssuesPayload event) returns error?;
    remote function onIssuesUnlabeled(IssuesPayload event) returns error?;
    remote function onIssuesClosed(IssuesPayload event) returns error?;
    remote function onIssuesDeleted(IssuesPayload event) returns error?;
};

public type RegistryPackageService service object {
    remote function onRegistryPackageUpdated(RegistryPackagePayload event) returns error?;
    remote function onRegistryPackagePublished(RegistryPackagePayload event) returns error?;
};

public type ProjectsV2Service service object {
    remote function onProjectsV2Created('ProjectsV2Payload event) returns error?;
    remote function onProjectsV2Edited('ProjectsV2Payload event) returns error?;
    remote function onProjectsV2Closed('ProjectsV2Payload event) returns error?;
    remote function onProjectsV2Reopened('ProjectsV2Payload event) returns error?;
    remote function onProjectsV2Deleted('ProjectsV2Payload event) returns error?;
};

public type RepositoryVulnerabilityAlertService service object {
    remote function onRepositoryVulnerabilityAlertResolve(RepositoryVulnerabilityAlertPayload event) returns error?;
    remote function onRepositoryVulnerabilityAlertReopen(RepositoryVulnerabilityAlertPayload event) returns error?;
    remote function onRepositoryVulnerabilityAlertDismiss(RepositoryVulnerabilityAlertPayload event) returns error?;
    remote function onRepositoryVulnerabilityAlertCreate(RepositoryVulnerabilityAlertPayload event) returns error?;
};

public type StarService service object {
    remote function onStarCreated(StarPayload event) returns error?;
    remote function onStarDeleted(StarPayload event) returns error?;
};

public type CreateService service object {
    remote function onCreate(CreatePayload event) returns error?;
};

public type DeploymentReviewService service object {
    remote function onDeploymentReviewRequested(DeploymentReviewPayload event) returns error?;
    remote function onDeploymentReviewRejected(DeploymentReviewPayload event) returns error?;
    remote function onDeploymentReviewApproved(DeploymentReviewPayload event) returns error?;
};

public type GollumService service object {
    remote function onGollum(GollumPayload event) returns error?;
};

public type GithubAppAuthorizationService service object {
    remote function onGithubAppAuthorization(GithubAppAuthorizationPayload event) returns error?;
};

public type WatchService service object {
    remote function onWatch(WatchPayload event) returns error?;
};

public type TeamService service object {
    remote function onTeamCreated(TeamPayload event) returns error?;
    remote function onTeamDeleted(TeamPayload event) returns error?;
    remote function onTeamEdited(TeamPayload event) returns error?;
    remote function onTeamAddedToRepository(TeamPayload event) returns error?;
    remote function onTeamRemovedFromRepository(TeamPayload event) returns error?;
};

public type WorkflowJobService service object {
    remote function onWorkflowJobQueued(WorkflowJobPayload event) returns error?;
    remote function onWorkflowJobWaiting(WorkflowJobPayload event) returns error?;
    remote function onWorkflowJobCompleted(WorkflowJobPayload event) returns error?;
    remote function onWorkflowJobInProgress(WorkflowJobPayload event) returns error?;
};

public type ReleaseService service object {
    remote function onReleaseCreated(ReleasePayload event) returns error?;
    remote function onReleasePublished(ReleasePayload event) returns error?;
    remote function onReleaseReleased(ReleasePayload event) returns error?;
    remote function onReleasePrereleased(ReleasePayload event) returns error?;
    remote function onReleaseUnpublished(ReleasePayload event) returns error?;
    remote function onReleaseDeleted(ReleasePayload event) returns error?;
    remote function onReleaseEdited(ReleasePayload event) returns error?;
};

public type InstallationService service object {
    remote function onInstallationNewPermissionsAccepted(InstallationPayload event) returns error?;
    remote function onInstallationSuspend(InstallationPayload event) returns error?;
    remote function onInstallationCreated(InstallationPayload event) returns error?;
    remote function onInstallationDeleted(InstallationPayload event) returns error?;
    remote function onInstallationUnsuspend(InstallationPayload event) returns error?;
};

public type CommitCommentService service object {
    remote function onCommitComment(CommitCommentPayload event) returns error?;
};

public type DiscussionCommentService service object {
    remote function onDiscussionCommentDeleted(DiscussionCommentPayload event) returns error?;
    remote function onDiscussionCommentCreated(DiscussionCommentPayload event) returns error?;
    remote function onDiscussionCommentEdited(DiscussionCommentPayload event) returns error?;
};

public type BranchProtectionRuleService service object {
    remote function onBranchProtectionRuleDeleted(BranchProtectionRulePayload event) returns error?;
    remote function onBranchProtectionRuleEdited(BranchProtectionRulePayload event) returns error?;
    remote function onBranchProtectionRuleCreated(BranchProtectionRulePayload event) returns error?;
};

public type IssueDependenciesService service object {
    remote function onIssueDependenciesBlockingRemoved(IssueDependenciesPayload event) returns error?;
    remote function onIssueDependenciesBlockedByRemoved(IssueDependenciesPayload event) returns error?;
    remote function onIssueDependenciesBlockingAdded(IssueDependenciesPayload event) returns error?;
    remote function onIssueDependenciesBlockedByAdded(IssueDependenciesPayload event) returns error?;
};

public type RepositoryService service object {
    remote function onRepositoryPrivatized(RepositoryPayload event) returns error?;
    remote function onRepositoryCreated(RepositoryPayload event) returns error?;
    remote function onRepositoryRenamed(RepositoryPayload event) returns error?;
    remote function onRepositoryTransferred(RepositoryPayload event) returns error?;
    remote function onRepositoryEdited(RepositoryPayload event) returns error?;
    remote function onRepositoryDeleted(RepositoryPayload event) returns error?;
    remote function onRepositoryArchived(RepositoryPayload event) returns error?;
    remote function onRepositoryPublicized(RepositoryPayload event) returns error?;
    remote function onRepositoryUnarchived(RepositoryPayload event) returns error?;
};

public type PullRequestReviewCommentService service object {
    remote function onPullRequestReviewCommentCreated(PullRequestReviewCommentPayload event) returns error?;
    remote function onPullRequestReviewCommentDeleted(PullRequestReviewCommentPayload event) returns error?;
    remote function onPullRequestReviewCommentEdited(PullRequestReviewCommentPayload event) returns error?;
};

public type DeploymentProtectionRuleService service object {
    remote function onDeploymentProtectionRule(DeploymentProtectionRulePayload event) returns error?;
};

public type CustomPropertyValuesService service object {
    remote function onCustomPropertyValues(CustomPropertyValuesPayload event) returns error?;
};

public type InstallationRepositoriesService service object {
    remote function onInstallationRepositoriesRemoved(InstallationRepositoriesPayload event) returns error?;
    remote function onInstallationRepositoriesAdded(InstallationRepositoriesPayload event) returns error?;
};

public type SecretScanningScanService service object {
    remote function onSecretScanningScan(SecretScanningScanPayload event) returns error?;
};

public type ProjectCardService service object {
    remote function onProjectCardEdited(ProjectCardPayload event) returns error?;
    remote function onProjectCardDeleted(ProjectCardPayload event) returns error?;
    remote function onProjectCardMoved(ProjectCardPayload event) returns error?;
    remote function onProjectCardConverted(ProjectCardPayload event) returns error?;
    remote function onProjectCardCreated(ProjectCardPayload event) returns error?;
};

public type CheckRunService service object {
    remote function onCheckRunCreated(CheckRunPayload event) returns error?;
    remote function onCheckRunCompleted(CheckRunPayload event) returns error?;
    remote function onCheckRunRequestedAction(CheckRunPayload event) returns error?;
    remote function onCheckRunRerequested(CheckRunPayload event) returns error?;
};

public type PageBuildService service object {
    remote function onPageBuild(PageBuildPayload event) returns error?;
};

public type CustomPropertyService service object {
    remote function onCustomPropertyUpdated(CustomPropertyPayload event) returns error?;
    remote function onCustomPropertyDeleted(CustomPropertyPayload event) returns error?;
    remote function onCustomPropertyPromoteToEnterprise(CustomPropertyPayload event) returns error?;
    remote function onCustomPropertyCreated(CustomPropertyPayload event) returns error?;
};

public type DependabotAlertService service object {
    remote function onDependabotAlertAutoDismissed(DependabotAlertPayload event) returns error?;
    remote function onDependabotAlertAutoReopened(DependabotAlertPayload event) returns error?;
    remote function onDependabotAlertCreated(DependabotAlertPayload event) returns error?;
    remote function onDependabotAlertDismissed(DependabotAlertPayload event) returns error?;
    remote function onDependabotAlertReopened(DependabotAlertPayload event) returns error?;
    remote function onDependabotAlertReintroduced(DependabotAlertPayload event) returns error?;
    remote function onDependabotAlertAssigneesChanged(DependabotAlertPayload event) returns error?;
    remote function onDependabotAlertFixed(DependabotAlertPayload event) returns error?;
};

public type DeploymentStatusService service object {
    remote function onDeploymentStatus(DeploymentStatusPayload event) returns error?;
};

public type RepositoryAdvisoryService service object {
    remote function onRepositoryAdvisoryReported(RepositoryAdvisoryPayload event) returns error?;
    remote function onRepositoryAdvisoryPublished(RepositoryAdvisoryPayload event) returns error?;
};

public type PullRequestReviewThreadService service object {
    remote function onPullRequestReviewThreadUnresolved(PullRequestReviewThreadPayload event) returns error?;
    remote function onPullRequestReviewThreadResolved(PullRequestReviewThreadPayload event) returns error?;
};

public type GenericServiceType DeleteService|MetaService|WorkflowDispatchService|SecurityAndAnalysisService|DeployKeyService|ProjectColumnService|MarketplacePurchaseService|BranchProtectionConfigurationService|PullRequestService|LabelService|DeploymentService|TeamAddService|CodeScanningAlertService|MembershipService|SecretScanningAlertService|PushService|MemberService|RepositoryDispatchService|StatusService|RepositoryImportService|PersonalAccessTokenRequestService|SubIssuesService|RepositoryRulesetService|MilestoneService|PublicService|WorkflowRunService|ProjectsV2statusUpdateService|ProjectsV2itemService|SponsorshipService|MergeGroupService|ProjectService|OrgBlockService|SecretScanningAlertLocationService|InstallationTargetService|CheckSuiteService|PingService|IssueCommentService|SecurityAdvisoryService|PackageService|DiscussionService|ForkService|PullRequestReviewService|OrganizationService|IssuesService|RegistryPackageService|ProjectsV2Service|RepositoryVulnerabilityAlertService|StarService|CreateService|DeploymentReviewService|GollumService|GithubAppAuthorizationService|WatchService|TeamService|WorkflowJobService|ReleaseService|InstallationService|CommitCommentService|DiscussionCommentService|BranchProtectionRuleService|IssueDependenciesService|RepositoryService|PullRequestReviewCommentService|DeploymentProtectionRuleService|CustomPropertyValuesService|InstallationRepositoriesService|SecretScanningScanService|ProjectCardService|CheckRunService|PageBuildService|CustomPropertyService|DependabotAlertService|DeploymentStatusService|RepositoryAdvisoryService|PullRequestReviewThreadService;


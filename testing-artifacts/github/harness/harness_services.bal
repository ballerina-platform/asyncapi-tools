import ballerina/io;

listener Listener githubHarnessListener = check new (8090, webhookSecret = "harness-secret");

service DeleteService on githubHarnessListener {
    remote function onDelete(DeletePayload event) returns error? {
        io:println("FIRED::DeleteService::onDelete");
    }
}

service MetaService on githubHarnessListener {
    remote function onMeta(MetaPayload event) returns error? {
        io:println("FIRED::MetaService::onMeta");
    }
}

service WorkflowDispatchService on githubHarnessListener {
    remote function onWorkflowDispatch(WorkflowDispatchPayload event) returns error? {
        io:println("FIRED::WorkflowDispatchService::onWorkflowDispatch");
    }
}

service SecurityAndAnalysisService on githubHarnessListener {
    remote function onSecurityAndAnalysis(SecurityAndAnalysisPayload event) returns error? {
        io:println("FIRED::SecurityAndAnalysisService::onSecurityAndAnalysis");
    }
}

service DeployKeyService on githubHarnessListener {
    remote function onDeployKeyCreated(DeployKeyPayload event) returns error? {
        io:println("FIRED::DeployKeyService::onDeployKeyCreated");
    }
    remote function onDeployKeyDeleted(DeployKeyPayload event) returns error? {
        io:println("FIRED::DeployKeyService::onDeployKeyDeleted");
    }
}

service ProjectColumnService on githubHarnessListener {
    remote function onProjectColumnMoved(ProjectColumnPayload event) returns error? {
        io:println("FIRED::ProjectColumnService::onProjectColumnMoved");
    }
    remote function onProjectColumnEdited(ProjectColumnPayload event) returns error? {
        io:println("FIRED::ProjectColumnService::onProjectColumnEdited");
    }
    remote function onProjectColumnDeleted(ProjectColumnPayload event) returns error? {
        io:println("FIRED::ProjectColumnService::onProjectColumnDeleted");
    }
    remote function onProjectColumnCreated(ProjectColumnPayload event) returns error? {
        io:println("FIRED::ProjectColumnService::onProjectColumnCreated");
    }
}

service MarketplacePurchaseService on githubHarnessListener {
    remote function onMarketplacePurchasePurchased(MarketplacePurchasePayload event) returns error? {
        io:println("FIRED::MarketplacePurchaseService::onMarketplacePurchasePurchased");
    }
    remote function onMarketplacePurchaseCancelled(MarketplacePurchasePayload event) returns error? {
        io:println("FIRED::MarketplacePurchaseService::onMarketplacePurchaseCancelled");
    }
    remote function onMarketplacePurchasePendingChangeCancelled(MarketplacePurchasePayload event) returns error? {
        io:println("FIRED::MarketplacePurchaseService::onMarketplacePurchasePendingChangeCancelled");
    }
    remote function onMarketplacePurchasePendingChange(MarketplacePurchasePayload event) returns error? {
        io:println("FIRED::MarketplacePurchaseService::onMarketplacePurchasePendingChange");
    }
    remote function onMarketplacePurchaseChanged(MarketplacePurchasePayload event) returns error? {
        io:println("FIRED::MarketplacePurchaseService::onMarketplacePurchaseChanged");
    }
}

service BranchProtectionConfigurationService on githubHarnessListener {
    remote function onBranchProtectionConfigurationEnabled(BranchProtectionConfigurationPayload event) returns error? {
        io:println("FIRED::BranchProtectionConfigurationService::onBranchProtectionConfigurationEnabled");
    }
    remote function onBranchProtectionConfigurationDisabled(BranchProtectionConfigurationPayload event) returns error? {
        io:println("FIRED::BranchProtectionConfigurationService::onBranchProtectionConfigurationDisabled");
    }
}

service PullRequestService on githubHarnessListener {
    remote function onPullRequestEnqueued(PullRequestPayload event) returns error? {
        io:println("FIRED::PullRequestService::onPullRequestEnqueued");
    }
    remote function onPullRequestReviewRequestRemoved(PullRequestPayload event) returns error? {
        io:println("FIRED::PullRequestService::onPullRequestReviewRequestRemoved");
    }
    remote function onPullRequestOpened(PullRequestPayload event) returns error? {
        io:println("FIRED::PullRequestService::onPullRequestOpened");
    }
    remote function onPullRequestReadyForReview(PullRequestPayload event) returns error? {
        io:println("FIRED::PullRequestService::onPullRequestReadyForReview");
    }
    remote function onPullRequestLabeled(PullRequestPayload event) returns error? {
        io:println("FIRED::PullRequestService::onPullRequestLabeled");
    }
    remote function onPullRequestUnassigned(PullRequestPayload event) returns error? {
        io:println("FIRED::PullRequestService::onPullRequestUnassigned");
    }
    remote function onPullRequestEdited(PullRequestPayload event) returns error? {
        io:println("FIRED::PullRequestService::onPullRequestEdited");
    }
    remote function onPullRequestSynchronize(PullRequestPayload event) returns error? {
        io:println("FIRED::PullRequestService::onPullRequestSynchronize");
    }
    remote function onPullRequestReviewRequested(PullRequestPayload event) returns error? {
        io:println("FIRED::PullRequestService::onPullRequestReviewRequested");
    }
    remote function onPullRequestReopened(PullRequestPayload event) returns error? {
        io:println("FIRED::PullRequestService::onPullRequestReopened");
    }
    remote function onPullRequestAutoMergeDisabled(PullRequestPayload event) returns error? {
        io:println("FIRED::PullRequestService::onPullRequestAutoMergeDisabled");
    }
    remote function onPullRequestLocked(PullRequestPayload event) returns error? {
        io:println("FIRED::PullRequestService::onPullRequestLocked");
    }
    remote function onPullRequestAutoMergeEnabled(PullRequestPayload event) returns error? {
        io:println("FIRED::PullRequestService::onPullRequestAutoMergeEnabled");
    }
    remote function onPullRequestMilestoned(PullRequestPayload event) returns error? {
        io:println("FIRED::PullRequestService::onPullRequestMilestoned");
    }
    remote function onPullRequestDequeued(PullRequestPayload event) returns error? {
        io:println("FIRED::PullRequestService::onPullRequestDequeued");
    }
    remote function onPullRequestUnlabeled(PullRequestPayload event) returns error? {
        io:println("FIRED::PullRequestService::onPullRequestUnlabeled");
    }
    remote function onPullRequestClosed(PullRequestPayload event) returns error? {
        io:println("FIRED::PullRequestService::onPullRequestClosed");
    }
    remote function onPullRequestUnlocked(PullRequestPayload event) returns error? {
        io:println("FIRED::PullRequestService::onPullRequestUnlocked");
    }
    remote function onPullRequestAssigned(PullRequestPayload event) returns error? {
        io:println("FIRED::PullRequestService::onPullRequestAssigned");
    }
    remote function onPullRequestConvertedToDraft(PullRequestPayload event) returns error? {
        io:println("FIRED::PullRequestService::onPullRequestConvertedToDraft");
    }
    remote function onPullRequestDemilestoned(PullRequestPayload event) returns error? {
        io:println("FIRED::PullRequestService::onPullRequestDemilestoned");
    }
}

service LabelService on githubHarnessListener {
    remote function onLabelEdited(LabelPayload event) returns error? {
        io:println("FIRED::LabelService::onLabelEdited");
    }
    remote function onLabelCreated(LabelPayload event) returns error? {
        io:println("FIRED::LabelService::onLabelCreated");
    }
    remote function onLabelDeleted(LabelPayload event) returns error? {
        io:println("FIRED::LabelService::onLabelDeleted");
    }
}

service DeploymentService on githubHarnessListener {
    remote function onDeployment(DeploymentPayload event) returns error? {
        io:println("FIRED::DeploymentService::onDeployment");
    }
}

service TeamAddService on githubHarnessListener {
    remote function onTeamAdd(TeamAddPayload event) returns error? {
        io:println("FIRED::TeamAddService::onTeamAdd");
    }
}

service CodeScanningAlertService on githubHarnessListener {
    remote function onCodeScanningAlertAppearedInBranch(CodeScanningAlertPayload event) returns error? {
        io:println("FIRED::CodeScanningAlertService::onCodeScanningAlertAppearedInBranch");
    }
    remote function onCodeScanningAlertClosedByUser(CodeScanningAlertPayload event) returns error? {
        io:println("FIRED::CodeScanningAlertService::onCodeScanningAlertClosedByUser");
    }
    remote function onCodeScanningAlertCreated(CodeScanningAlertPayload event) returns error? {
        io:println("FIRED::CodeScanningAlertService::onCodeScanningAlertCreated");
    }
    remote function onCodeScanningAlertFixed(CodeScanningAlertPayload event) returns error? {
        io:println("FIRED::CodeScanningAlertService::onCodeScanningAlertFixed");
    }
    remote function onCodeScanningAlertReopened(CodeScanningAlertPayload event) returns error? {
        io:println("FIRED::CodeScanningAlertService::onCodeScanningAlertReopened");
    }
    remote function onCodeScanningAlertReopenedByUser(CodeScanningAlertPayload event) returns error? {
        io:println("FIRED::CodeScanningAlertService::onCodeScanningAlertReopenedByUser");
    }
    remote function onCodeScanningAlertUpdatedAssignment(CodeScanningAlertPayload event) returns error? {
        io:println("FIRED::CodeScanningAlertService::onCodeScanningAlertUpdatedAssignment");
    }
}

service MembershipService on githubHarnessListener {
    remote function onMembershipAdded(MembershipPayload event) returns error? {
        io:println("FIRED::MembershipService::onMembershipAdded");
    }
    remote function onMembershipRemoved(MembershipPayload event) returns error? {
        io:println("FIRED::MembershipService::onMembershipRemoved");
    }
}

service SecretScanningAlertService on githubHarnessListener {
    remote function onSecretScanningAlertAssigned(SecretScanningAlertPayload event) returns error? {
        io:println("FIRED::SecretScanningAlertService::onSecretScanningAlertAssigned");
    }
    remote function onSecretScanningAlertReopened(SecretScanningAlertPayload event) returns error? {
        io:println("FIRED::SecretScanningAlertService::onSecretScanningAlertReopened");
    }
    remote function onSecretScanningAlertUnassigned(SecretScanningAlertPayload event) returns error? {
        io:println("FIRED::SecretScanningAlertService::onSecretScanningAlertUnassigned");
    }
    remote function onSecretScanningAlertCreated(SecretScanningAlertPayload event) returns error? {
        io:println("FIRED::SecretScanningAlertService::onSecretScanningAlertCreated");
    }
    remote function onSecretScanningAlertPubliclyLeaked(SecretScanningAlertPayload event) returns error? {
        io:println("FIRED::SecretScanningAlertService::onSecretScanningAlertPubliclyLeaked");
    }
    remote function onSecretScanningAlertValidated(SecretScanningAlertPayload event) returns error? {
        io:println("FIRED::SecretScanningAlertService::onSecretScanningAlertValidated");
    }
    remote function onSecretScanningAlertResolved(SecretScanningAlertPayload event) returns error? {
        io:println("FIRED::SecretScanningAlertService::onSecretScanningAlertResolved");
    }
}

service PushService on githubHarnessListener {
    remote function onPush(PushPayload event) returns error? {
        io:println("FIRED::PushService::onPush");
    }
}

service MemberService on githubHarnessListener {
    remote function onMemberEdited(MemberPayload event) returns error? {
        io:println("FIRED::MemberService::onMemberEdited");
    }
    remote function onMemberAdded(MemberPayload event) returns error? {
        io:println("FIRED::MemberService::onMemberAdded");
    }
    remote function onMemberRemoved(MemberPayload event) returns error? {
        io:println("FIRED::MemberService::onMemberRemoved");
    }
}

service RepositoryDispatchService on githubHarnessListener {
    remote function onRepositoryDispatch(RepositoryDispatchPayload event) returns error? {
        io:println("FIRED::RepositoryDispatchService::onRepositoryDispatch");
    }
}

service StatusService on githubHarnessListener {
    remote function onStatus(StatusPayload event) returns error? {
        io:println("FIRED::StatusService::onStatus");
    }
}

service RepositoryImportService on githubHarnessListener {
    remote function onRepositoryImport(RepositoryImportPayload event) returns error? {
        io:println("FIRED::RepositoryImportService::onRepositoryImport");
    }
}

service PersonalAccessTokenRequestService on githubHarnessListener {
    remote function onPersonalAccessTokenRequestCreated(PersonalAccessTokenRequestPayload event) returns error? {
        io:println("FIRED::PersonalAccessTokenRequestService::onPersonalAccessTokenRequestCreated");
    }
    remote function onPersonalAccessTokenRequestApproved(PersonalAccessTokenRequestPayload event) returns error? {
        io:println("FIRED::PersonalAccessTokenRequestService::onPersonalAccessTokenRequestApproved");
    }
    remote function onPersonalAccessTokenRequestDenied(PersonalAccessTokenRequestPayload event) returns error? {
        io:println("FIRED::PersonalAccessTokenRequestService::onPersonalAccessTokenRequestDenied");
    }
    remote function onPersonalAccessTokenRequestCancelled(PersonalAccessTokenRequestPayload event) returns error? {
        io:println("FIRED::PersonalAccessTokenRequestService::onPersonalAccessTokenRequestCancelled");
    }
}

service SubIssuesService on githubHarnessListener {
    remote function onSubIssuesSubIssueAdded(SubIssuesPayload event) returns error? {
        io:println("FIRED::SubIssuesService::onSubIssuesSubIssueAdded");
    }
    remote function onSubIssuesParentIssueAdded(SubIssuesPayload event) returns error? {
        io:println("FIRED::SubIssuesService::onSubIssuesParentIssueAdded");
    }
    remote function onSubIssuesSubIssueRemoved(SubIssuesPayload event) returns error? {
        io:println("FIRED::SubIssuesService::onSubIssuesSubIssueRemoved");
    }
    remote function onSubIssuesParentIssueRemoved(SubIssuesPayload event) returns error? {
        io:println("FIRED::SubIssuesService::onSubIssuesParentIssueRemoved");
    }
}

service RepositoryRulesetService on githubHarnessListener {
    remote function onRepositoryRulesetCreated(RepositoryRulesetPayload event) returns error? {
        io:println("FIRED::RepositoryRulesetService::onRepositoryRulesetCreated");
    }
    remote function onRepositoryRulesetEdited(RepositoryRulesetPayload event) returns error? {
        io:println("FIRED::RepositoryRulesetService::onRepositoryRulesetEdited");
    }
    remote function onRepositoryRulesetDeleted(RepositoryRulesetPayload event) returns error? {
        io:println("FIRED::RepositoryRulesetService::onRepositoryRulesetDeleted");
    }
}

service MilestoneService on githubHarnessListener {
    remote function onMilestoneCreated(MilestonePayload event) returns error? {
        io:println("FIRED::MilestoneService::onMilestoneCreated");
    }
    remote function onMilestoneEdited(MilestonePayload event) returns error? {
        io:println("FIRED::MilestoneService::onMilestoneEdited");
    }
    remote function onMilestoneOpened(MilestonePayload event) returns error? {
        io:println("FIRED::MilestoneService::onMilestoneOpened");
    }
    remote function onMilestoneDeleted(MilestonePayload event) returns error? {
        io:println("FIRED::MilestoneService::onMilestoneDeleted");
    }
    remote function onMilestoneClosed(MilestonePayload event) returns error? {
        io:println("FIRED::MilestoneService::onMilestoneClosed");
    }
}

service PublicService on githubHarnessListener {
    remote function onPublic(PublicPayload event) returns error? {
        io:println("FIRED::PublicService::onPublic");
    }
}

service WorkflowRunService on githubHarnessListener {
    remote function onWorkflowRunInProgress(WorkflowRunPayload event) returns error? {
        io:println("FIRED::WorkflowRunService::onWorkflowRunInProgress");
    }
    remote function onWorkflowRunCompleted(WorkflowRunPayload event) returns error? {
        io:println("FIRED::WorkflowRunService::onWorkflowRunCompleted");
    }
    remote function onWorkflowRunRequested(WorkflowRunPayload event) returns error? {
        io:println("FIRED::WorkflowRunService::onWorkflowRunRequested");
    }
}

service ProjectsV2statusUpdateService on githubHarnessListener {
    remote function onProjectsV2StatusUpdateEdited('ProjectsV2StatusUpdatePayload event) returns error? {
        io:println("FIRED::ProjectsV2statusUpdateService::onProjectsV2StatusUpdateEdited");
    }
    remote function onProjectsV2StatusUpdateDeleted('ProjectsV2StatusUpdatePayload event) returns error? {
        io:println("FIRED::ProjectsV2statusUpdateService::onProjectsV2StatusUpdateDeleted");
    }
    remote function onProjectsV2StatusUpdateCreated('ProjectsV2StatusUpdatePayload event) returns error? {
        io:println("FIRED::ProjectsV2statusUpdateService::onProjectsV2StatusUpdateCreated");
    }
}

service ProjectsV2itemService on githubHarnessListener {
    remote function onProjectsV2ItemEdited('ProjectsV2ItemPayload event) returns error? {
        io:println("FIRED::ProjectsV2itemService::onProjectsV2ItemEdited");
    }
    remote function onProjectsV2ItemCreated('ProjectsV2ItemPayload event) returns error? {
        io:println("FIRED::ProjectsV2itemService::onProjectsV2ItemCreated");
    }
    remote function onProjectsV2ItemArchived('ProjectsV2ItemPayload event) returns error? {
        io:println("FIRED::ProjectsV2itemService::onProjectsV2ItemArchived");
    }
    remote function onProjectsV2ItemDeleted('ProjectsV2ItemPayload event) returns error? {
        io:println("FIRED::ProjectsV2itemService::onProjectsV2ItemDeleted");
    }
    remote function onProjectsV2ItemRestored('ProjectsV2ItemPayload event) returns error? {
        io:println("FIRED::ProjectsV2itemService::onProjectsV2ItemRestored");
    }
    remote function onProjectsV2ItemReordered('ProjectsV2ItemPayload event) returns error? {
        io:println("FIRED::ProjectsV2itemService::onProjectsV2ItemReordered");
    }
    remote function onProjectsV2ItemConverted('ProjectsV2ItemPayload event) returns error? {
        io:println("FIRED::ProjectsV2itemService::onProjectsV2ItemConverted");
    }
}

service SponsorshipService on githubHarnessListener {
    remote function onSponsorshipCancelled(SponsorshipPayload event) returns error? {
        io:println("FIRED::SponsorshipService::onSponsorshipCancelled");
    }
    remote function onSponsorshipEdited(SponsorshipPayload event) returns error? {
        io:println("FIRED::SponsorshipService::onSponsorshipEdited");
    }
    remote function onSponsorshipTierChanged(SponsorshipPayload event) returns error? {
        io:println("FIRED::SponsorshipService::onSponsorshipTierChanged");
    }
    remote function onSponsorshipPendingCancellation(SponsorshipPayload event) returns error? {
        io:println("FIRED::SponsorshipService::onSponsorshipPendingCancellation");
    }
    remote function onSponsorshipCreated(SponsorshipPayload event) returns error? {
        io:println("FIRED::SponsorshipService::onSponsorshipCreated");
    }
    remote function onSponsorshipPendingTierChange(SponsorshipPayload event) returns error? {
        io:println("FIRED::SponsorshipService::onSponsorshipPendingTierChange");
    }
}

service MergeGroupService on githubHarnessListener {
    remote function onMergeGroupDestroyed(MergeGroupPayload event) returns error? {
        io:println("FIRED::MergeGroupService::onMergeGroupDestroyed");
    }
    remote function onMergeGroupChecksRequested(MergeGroupPayload event) returns error? {
        io:println("FIRED::MergeGroupService::onMergeGroupChecksRequested");
    }
}

service ProjectService on githubHarnessListener {
    remote function onProjectDeleted(ProjectPayload event) returns error? {
        io:println("FIRED::ProjectService::onProjectDeleted");
    }
    remote function onProjectCreated(ProjectPayload event) returns error? {
        io:println("FIRED::ProjectService::onProjectCreated");
    }
    remote function onProjectClosed(ProjectPayload event) returns error? {
        io:println("FIRED::ProjectService::onProjectClosed");
    }
    remote function onProjectReopened(ProjectPayload event) returns error? {
        io:println("FIRED::ProjectService::onProjectReopened");
    }
    remote function onProjectEdited(ProjectPayload event) returns error? {
        io:println("FIRED::ProjectService::onProjectEdited");
    }
}

service OrgBlockService on githubHarnessListener {
    remote function onOrgBlockBlocked(OrgBlockPayload event) returns error? {
        io:println("FIRED::OrgBlockService::onOrgBlockBlocked");
    }
    remote function onOrgBlockUnblocked(OrgBlockPayload event) returns error? {
        io:println("FIRED::OrgBlockService::onOrgBlockUnblocked");
    }
}

service SecretScanningAlertLocationService on githubHarnessListener {
    remote function onSecretScanningAlertLocation(SecretScanningAlertLocationPayload event) returns error? {
        io:println("FIRED::SecretScanningAlertLocationService::onSecretScanningAlertLocation");
    }
}

service InstallationTargetService on githubHarnessListener {
    remote function onInstallationTarget(InstallationTargetPayload event) returns error? {
        io:println("FIRED::InstallationTargetService::onInstallationTarget");
    }
}

service CheckSuiteService on githubHarnessListener {
    remote function onCheckSuiteCompleted(CheckSuitePayload event) returns error? {
        io:println("FIRED::CheckSuiteService::onCheckSuiteCompleted");
    }
    remote function onCheckSuiteRequested(CheckSuitePayload event) returns error? {
        io:println("FIRED::CheckSuiteService::onCheckSuiteRequested");
    }
    remote function onCheckSuiteRerequested(CheckSuitePayload event) returns error? {
        io:println("FIRED::CheckSuiteService::onCheckSuiteRerequested");
    }
}

service PingService on githubHarnessListener {
    remote function onPing(PingPayload event) returns error? {
        io:println("FIRED::PingService::onPing");
    }
}

service IssueCommentService on githubHarnessListener {
    remote function onIssueCommentEdited(IssueCommentPayload event) returns error? {
        io:println("FIRED::IssueCommentService::onIssueCommentEdited");
    }
    remote function onIssueCommentPinned(IssueCommentPayload event) returns error? {
        io:println("FIRED::IssueCommentService::onIssueCommentPinned");
    }
    remote function onIssueCommentDeleted(IssueCommentPayload event) returns error? {
        io:println("FIRED::IssueCommentService::onIssueCommentDeleted");
    }
    remote function onIssueCommentCreated(IssueCommentPayload event) returns error? {
        io:println("FIRED::IssueCommentService::onIssueCommentCreated");
    }
    remote function onIssueCommentUnpinned(IssueCommentPayload event) returns error? {
        io:println("FIRED::IssueCommentService::onIssueCommentUnpinned");
    }
}

service SecurityAdvisoryService on githubHarnessListener {
    remote function onSecurityAdvisoryWithdrawn(SecurityAdvisoryPayload event) returns error? {
        io:println("FIRED::SecurityAdvisoryService::onSecurityAdvisoryWithdrawn");
    }
    remote function onSecurityAdvisoryPublished(SecurityAdvisoryPayload event) returns error? {
        io:println("FIRED::SecurityAdvisoryService::onSecurityAdvisoryPublished");
    }
    remote function onSecurityAdvisoryUpdated(SecurityAdvisoryPayload event) returns error? {
        io:println("FIRED::SecurityAdvisoryService::onSecurityAdvisoryUpdated");
    }
}

service PackageService on githubHarnessListener {
    remote function onPackagePublished(PackagePayload event) returns error? {
        io:println("FIRED::PackageService::onPackagePublished");
    }
    remote function onPackageUpdated(PackagePayload event) returns error? {
        io:println("FIRED::PackageService::onPackageUpdated");
    }
}

service DiscussionService on githubHarnessListener {
    remote function onDiscussionUnanswered(DiscussionPayload event) returns error? {
        io:println("FIRED::DiscussionService::onDiscussionUnanswered");
    }
    remote function onDiscussionCreated(DiscussionPayload event) returns error? {
        io:println("FIRED::DiscussionService::onDiscussionCreated");
    }
    remote function onDiscussionTransferred(DiscussionPayload event) returns error? {
        io:println("FIRED::DiscussionService::onDiscussionTransferred");
    }
    remote function onDiscussionCategoryChanged(DiscussionPayload event) returns error? {
        io:println("FIRED::DiscussionService::onDiscussionCategoryChanged");
    }
    remote function onDiscussionDeleted(DiscussionPayload event) returns error? {
        io:println("FIRED::DiscussionService::onDiscussionDeleted");
    }
    remote function onDiscussionUnlocked(DiscussionPayload event) returns error? {
        io:println("FIRED::DiscussionService::onDiscussionUnlocked");
    }
    remote function onDiscussionPinned(DiscussionPayload event) returns error? {
        io:println("FIRED::DiscussionService::onDiscussionPinned");
    }
    remote function onDiscussionEdited(DiscussionPayload event) returns error? {
        io:println("FIRED::DiscussionService::onDiscussionEdited");
    }
    remote function onDiscussionReopened(DiscussionPayload event) returns error? {
        io:println("FIRED::DiscussionService::onDiscussionReopened");
    }
    remote function onDiscussionAnswered(DiscussionPayload event) returns error? {
        io:println("FIRED::DiscussionService::onDiscussionAnswered");
    }
    remote function onDiscussionClosed(DiscussionPayload event) returns error? {
        io:println("FIRED::DiscussionService::onDiscussionClosed");
    }
    remote function onDiscussionUnlabeled(DiscussionPayload event) returns error? {
        io:println("FIRED::DiscussionService::onDiscussionUnlabeled");
    }
    remote function onDiscussionLabeled(DiscussionPayload event) returns error? {
        io:println("FIRED::DiscussionService::onDiscussionLabeled");
    }
    remote function onDiscussionUnpinned(DiscussionPayload event) returns error? {
        io:println("FIRED::DiscussionService::onDiscussionUnpinned");
    }
    remote function onDiscussionLocked(DiscussionPayload event) returns error? {
        io:println("FIRED::DiscussionService::onDiscussionLocked");
    }
}

service ForkService on githubHarnessListener {
    remote function onFork(ForkPayload event) returns error? {
        io:println("FIRED::ForkService::onFork");
    }
}

service PullRequestReviewService on githubHarnessListener {
    remote function onPullRequestReviewSubmitted(PullRequestReviewPayload event) returns error? {
        io:println("FIRED::PullRequestReviewService::onPullRequestReviewSubmitted");
    }
    remote function onPullRequestReviewEdited(PullRequestReviewPayload event) returns error? {
        io:println("FIRED::PullRequestReviewService::onPullRequestReviewEdited");
    }
    remote function onPullRequestReviewDismissed(PullRequestReviewPayload event) returns error? {
        io:println("FIRED::PullRequestReviewService::onPullRequestReviewDismissed");
    }
}

service OrganizationService on githubHarnessListener {
    remote function onOrganizationMemberAdded(OrganizationPayload event) returns error? {
        io:println("FIRED::OrganizationService::onOrganizationMemberAdded");
    }
    remote function onOrganizationMemberRemoved(OrganizationPayload event) returns error? {
        io:println("FIRED::OrganizationService::onOrganizationMemberRemoved");
    }
    remote function onOrganizationDeleted(OrganizationPayload event) returns error? {
        io:println("FIRED::OrganizationService::onOrganizationDeleted");
    }
    remote function onOrganizationRenamed(OrganizationPayload event) returns error? {
        io:println("FIRED::OrganizationService::onOrganizationRenamed");
    }
    remote function onOrganizationMemberInvited(OrganizationPayload event) returns error? {
        io:println("FIRED::OrganizationService::onOrganizationMemberInvited");
    }
}

service IssuesService on githubHarnessListener {
    remote function onIssuesReopened(IssuesPayload event) returns error? {
        io:println("FIRED::IssuesService::onIssuesReopened");
    }
    remote function onIssuesTransferred(IssuesPayload event) returns error? {
        io:println("FIRED::IssuesService::onIssuesTransferred");
    }
    remote function onIssuesUnpinned(IssuesPayload event) returns error? {
        io:println("FIRED::IssuesService::onIssuesUnpinned");
    }
    remote function onIssuesAssigned(IssuesPayload event) returns error? {
        io:println("FIRED::IssuesService::onIssuesAssigned");
    }
    remote function onIssuesMilestoned(IssuesPayload event) returns error? {
        io:println("FIRED::IssuesService::onIssuesMilestoned");
    }
    remote function onIssuesLabeled(IssuesPayload event) returns error? {
        io:println("FIRED::IssuesService::onIssuesLabeled");
    }
    remote function onIssuesOpened(IssuesPayload event) returns error? {
        io:println("FIRED::IssuesService::onIssuesOpened");
    }
    remote function onIssuesPinned(IssuesPayload event) returns error? {
        io:println("FIRED::IssuesService::onIssuesPinned");
    }
    remote function onIssuesTyped(IssuesPayload event) returns error? {
        io:println("FIRED::IssuesService::onIssuesTyped");
    }
    remote function onIssuesEdited(IssuesPayload event) returns error? {
        io:println("FIRED::IssuesService::onIssuesEdited");
    }
    remote function onIssuesUntyped(IssuesPayload event) returns error? {
        io:println("FIRED::IssuesService::onIssuesUntyped");
    }
    remote function onIssuesDemilestoned(IssuesPayload event) returns error? {
        io:println("FIRED::IssuesService::onIssuesDemilestoned");
    }
    remote function onIssuesLocked(IssuesPayload event) returns error? {
        io:println("FIRED::IssuesService::onIssuesLocked");
    }
    remote function onIssuesUnassigned(IssuesPayload event) returns error? {
        io:println("FIRED::IssuesService::onIssuesUnassigned");
    }
    remote function onIssuesUnlocked(IssuesPayload event) returns error? {
        io:println("FIRED::IssuesService::onIssuesUnlocked");
    }
    remote function onIssuesUnlabeled(IssuesPayload event) returns error? {
        io:println("FIRED::IssuesService::onIssuesUnlabeled");
    }
    remote function onIssuesClosed(IssuesPayload event) returns error? {
        io:println("FIRED::IssuesService::onIssuesClosed");
    }
    remote function onIssuesDeleted(IssuesPayload event) returns error? {
        io:println("FIRED::IssuesService::onIssuesDeleted");
    }
}

service RegistryPackageService on githubHarnessListener {
    remote function onRegistryPackageUpdated(RegistryPackagePayload event) returns error? {
        io:println("FIRED::RegistryPackageService::onRegistryPackageUpdated");
    }
    remote function onRegistryPackagePublished(RegistryPackagePayload event) returns error? {
        io:println("FIRED::RegistryPackageService::onRegistryPackagePublished");
    }
}

service ProjectsV2Service on githubHarnessListener {
    remote function onProjectsV2Created('ProjectsV2Payload event) returns error? {
        io:println("FIRED::ProjectsV2Service::onProjectsV2Created");
    }
    remote function onProjectsV2Edited('ProjectsV2Payload event) returns error? {
        io:println("FIRED::ProjectsV2Service::onProjectsV2Edited");
    }
    remote function onProjectsV2Closed('ProjectsV2Payload event) returns error? {
        io:println("FIRED::ProjectsV2Service::onProjectsV2Closed");
    }
    remote function onProjectsV2Reopened('ProjectsV2Payload event) returns error? {
        io:println("FIRED::ProjectsV2Service::onProjectsV2Reopened");
    }
    remote function onProjectsV2Deleted('ProjectsV2Payload event) returns error? {
        io:println("FIRED::ProjectsV2Service::onProjectsV2Deleted");
    }
}

service RepositoryVulnerabilityAlertService on githubHarnessListener {
    remote function onRepositoryVulnerabilityAlertResolve(RepositoryVulnerabilityAlertPayload event) returns error? {
        io:println("FIRED::RepositoryVulnerabilityAlertService::onRepositoryVulnerabilityAlertResolve");
    }
    remote function onRepositoryVulnerabilityAlertReopen(RepositoryVulnerabilityAlertPayload event) returns error? {
        io:println("FIRED::RepositoryVulnerabilityAlertService::onRepositoryVulnerabilityAlertReopen");
    }
    remote function onRepositoryVulnerabilityAlertDismiss(RepositoryVulnerabilityAlertPayload event) returns error? {
        io:println("FIRED::RepositoryVulnerabilityAlertService::onRepositoryVulnerabilityAlertDismiss");
    }
    remote function onRepositoryVulnerabilityAlertCreate(RepositoryVulnerabilityAlertPayload event) returns error? {
        io:println("FIRED::RepositoryVulnerabilityAlertService::onRepositoryVulnerabilityAlertCreate");
    }
}

service StarService on githubHarnessListener {
    remote function onStarCreated(StarPayload event) returns error? {
        io:println("FIRED::StarService::onStarCreated");
    }
    remote function onStarDeleted(StarPayload event) returns error? {
        io:println("FIRED::StarService::onStarDeleted");
    }
}

service CreateService on githubHarnessListener {
    remote function onCreate(CreatePayload event) returns error? {
        io:println("FIRED::CreateService::onCreate");
    }
}

service DeploymentReviewService on githubHarnessListener {
    remote function onDeploymentReviewRequested(DeploymentReviewPayload event) returns error? {
        io:println("FIRED::DeploymentReviewService::onDeploymentReviewRequested");
    }
    remote function onDeploymentReviewRejected(DeploymentReviewPayload event) returns error? {
        io:println("FIRED::DeploymentReviewService::onDeploymentReviewRejected");
    }
    remote function onDeploymentReviewApproved(DeploymentReviewPayload event) returns error? {
        io:println("FIRED::DeploymentReviewService::onDeploymentReviewApproved");
    }
}

service GollumService on githubHarnessListener {
    remote function onGollum(GollumPayload event) returns error? {
        io:println("FIRED::GollumService::onGollum");
    }
}

service GithubAppAuthorizationService on githubHarnessListener {
    remote function onGithubAppAuthorization(GithubAppAuthorizationPayload event) returns error? {
        io:println("FIRED::GithubAppAuthorizationService::onGithubAppAuthorization");
    }
}

service WatchService on githubHarnessListener {
    remote function onWatch(WatchPayload event) returns error? {
        io:println("FIRED::WatchService::onWatch");
    }
}

service TeamService on githubHarnessListener {
    remote function onTeamCreated(TeamPayload event) returns error? {
        io:println("FIRED::TeamService::onTeamCreated");
    }
    remote function onTeamDeleted(TeamPayload event) returns error? {
        io:println("FIRED::TeamService::onTeamDeleted");
    }
    remote function onTeamEdited(TeamPayload event) returns error? {
        io:println("FIRED::TeamService::onTeamEdited");
    }
    remote function onTeamAddedToRepository(TeamPayload event) returns error? {
        io:println("FIRED::TeamService::onTeamAddedToRepository");
    }
    remote function onTeamRemovedFromRepository(TeamPayload event) returns error? {
        io:println("FIRED::TeamService::onTeamRemovedFromRepository");
    }
}

service WorkflowJobService on githubHarnessListener {
    remote function onWorkflowJobQueued(WorkflowJobPayload event) returns error? {
        io:println("FIRED::WorkflowJobService::onWorkflowJobQueued");
    }
    remote function onWorkflowJobWaiting(WorkflowJobPayload event) returns error? {
        io:println("FIRED::WorkflowJobService::onWorkflowJobWaiting");
    }
    remote function onWorkflowJobCompleted(WorkflowJobPayload event) returns error? {
        io:println("FIRED::WorkflowJobService::onWorkflowJobCompleted");
    }
    remote function onWorkflowJobInProgress(WorkflowJobPayload event) returns error? {
        io:println("FIRED::WorkflowJobService::onWorkflowJobInProgress");
    }
}

service ReleaseService on githubHarnessListener {
    remote function onReleaseCreated(ReleasePayload event) returns error? {
        io:println("FIRED::ReleaseService::onReleaseCreated");
    }
    remote function onReleasePublished(ReleasePayload event) returns error? {
        io:println("FIRED::ReleaseService::onReleasePublished");
    }
    remote function onReleaseReleased(ReleasePayload event) returns error? {
        io:println("FIRED::ReleaseService::onReleaseReleased");
    }
    remote function onReleasePrereleased(ReleasePayload event) returns error? {
        io:println("FIRED::ReleaseService::onReleasePrereleased");
    }
    remote function onReleaseUnpublished(ReleasePayload event) returns error? {
        io:println("FIRED::ReleaseService::onReleaseUnpublished");
    }
    remote function onReleaseDeleted(ReleasePayload event) returns error? {
        io:println("FIRED::ReleaseService::onReleaseDeleted");
    }
    remote function onReleaseEdited(ReleasePayload event) returns error? {
        io:println("FIRED::ReleaseService::onReleaseEdited");
    }
}

service InstallationService on githubHarnessListener {
    remote function onInstallationNewPermissionsAccepted(InstallationPayload event) returns error? {
        io:println("FIRED::InstallationService::onInstallationNewPermissionsAccepted");
    }
    remote function onInstallationSuspend(InstallationPayload event) returns error? {
        io:println("FIRED::InstallationService::onInstallationSuspend");
    }
    remote function onInstallationCreated(InstallationPayload event) returns error? {
        io:println("FIRED::InstallationService::onInstallationCreated");
    }
    remote function onInstallationDeleted(InstallationPayload event) returns error? {
        io:println("FIRED::InstallationService::onInstallationDeleted");
    }
    remote function onInstallationUnsuspend(InstallationPayload event) returns error? {
        io:println("FIRED::InstallationService::onInstallationUnsuspend");
    }
}

service CommitCommentService on githubHarnessListener {
    remote function onCommitComment(CommitCommentPayload event) returns error? {
        io:println("FIRED::CommitCommentService::onCommitComment");
    }
}

service DiscussionCommentService on githubHarnessListener {
    remote function onDiscussionCommentDeleted(DiscussionCommentPayload event) returns error? {
        io:println("FIRED::DiscussionCommentService::onDiscussionCommentDeleted");
    }
    remote function onDiscussionCommentCreated(DiscussionCommentPayload event) returns error? {
        io:println("FIRED::DiscussionCommentService::onDiscussionCommentCreated");
    }
    remote function onDiscussionCommentEdited(DiscussionCommentPayload event) returns error? {
        io:println("FIRED::DiscussionCommentService::onDiscussionCommentEdited");
    }
}

service BranchProtectionRuleService on githubHarnessListener {
    remote function onBranchProtectionRuleDeleted(BranchProtectionRulePayload event) returns error? {
        io:println("FIRED::BranchProtectionRuleService::onBranchProtectionRuleDeleted");
    }
    remote function onBranchProtectionRuleEdited(BranchProtectionRulePayload event) returns error? {
        io:println("FIRED::BranchProtectionRuleService::onBranchProtectionRuleEdited");
    }
    remote function onBranchProtectionRuleCreated(BranchProtectionRulePayload event) returns error? {
        io:println("FIRED::BranchProtectionRuleService::onBranchProtectionRuleCreated");
    }
}

service IssueDependenciesService on githubHarnessListener {
    remote function onIssueDependenciesBlockingRemoved(IssueDependenciesPayload event) returns error? {
        io:println("FIRED::IssueDependenciesService::onIssueDependenciesBlockingRemoved");
    }
    remote function onIssueDependenciesBlockedByRemoved(IssueDependenciesPayload event) returns error? {
        io:println("FIRED::IssueDependenciesService::onIssueDependenciesBlockedByRemoved");
    }
    remote function onIssueDependenciesBlockingAdded(IssueDependenciesPayload event) returns error? {
        io:println("FIRED::IssueDependenciesService::onIssueDependenciesBlockingAdded");
    }
    remote function onIssueDependenciesBlockedByAdded(IssueDependenciesPayload event) returns error? {
        io:println("FIRED::IssueDependenciesService::onIssueDependenciesBlockedByAdded");
    }
}

service RepositoryService on githubHarnessListener {
    remote function onRepositoryPrivatized(RepositoryPayload event) returns error? {
        io:println("FIRED::RepositoryService::onRepositoryPrivatized");
    }
    remote function onRepositoryCreated(RepositoryPayload event) returns error? {
        io:println("FIRED::RepositoryService::onRepositoryCreated");
    }
    remote function onRepositoryRenamed(RepositoryPayload event) returns error? {
        io:println("FIRED::RepositoryService::onRepositoryRenamed");
    }
    remote function onRepositoryTransferred(RepositoryPayload event) returns error? {
        io:println("FIRED::RepositoryService::onRepositoryTransferred");
    }
    remote function onRepositoryEdited(RepositoryPayload event) returns error? {
        io:println("FIRED::RepositoryService::onRepositoryEdited");
    }
    remote function onRepositoryDeleted(RepositoryPayload event) returns error? {
        io:println("FIRED::RepositoryService::onRepositoryDeleted");
    }
    remote function onRepositoryArchived(RepositoryPayload event) returns error? {
        io:println("FIRED::RepositoryService::onRepositoryArchived");
    }
    remote function onRepositoryPublicized(RepositoryPayload event) returns error? {
        io:println("FIRED::RepositoryService::onRepositoryPublicized");
    }
    remote function onRepositoryUnarchived(RepositoryPayload event) returns error? {
        io:println("FIRED::RepositoryService::onRepositoryUnarchived");
    }
}

service PullRequestReviewCommentService on githubHarnessListener {
    remote function onPullRequestReviewCommentCreated(PullRequestReviewCommentPayload event) returns error? {
        io:println("FIRED::PullRequestReviewCommentService::onPullRequestReviewCommentCreated");
    }
    remote function onPullRequestReviewCommentDeleted(PullRequestReviewCommentPayload event) returns error? {
        io:println("FIRED::PullRequestReviewCommentService::onPullRequestReviewCommentDeleted");
    }
    remote function onPullRequestReviewCommentEdited(PullRequestReviewCommentPayload event) returns error? {
        io:println("FIRED::PullRequestReviewCommentService::onPullRequestReviewCommentEdited");
    }
}

service DeploymentProtectionRuleService on githubHarnessListener {
    remote function onDeploymentProtectionRule(DeploymentProtectionRulePayload event) returns error? {
        io:println("FIRED::DeploymentProtectionRuleService::onDeploymentProtectionRule");
    }
}

service CustomPropertyValuesService on githubHarnessListener {
    remote function onCustomPropertyValues(CustomPropertyValuesPayload event) returns error? {
        io:println("FIRED::CustomPropertyValuesService::onCustomPropertyValues");
    }
}

service InstallationRepositoriesService on githubHarnessListener {
    remote function onInstallationRepositoriesRemoved(InstallationRepositoriesPayload event) returns error? {
        io:println("FIRED::InstallationRepositoriesService::onInstallationRepositoriesRemoved");
    }
    remote function onInstallationRepositoriesAdded(InstallationRepositoriesPayload event) returns error? {
        io:println("FIRED::InstallationRepositoriesService::onInstallationRepositoriesAdded");
    }
}

service SecretScanningScanService on githubHarnessListener {
    remote function onSecretScanningScan(SecretScanningScanPayload event) returns error? {
        io:println("FIRED::SecretScanningScanService::onSecretScanningScan");
    }
}

service ProjectCardService on githubHarnessListener {
    remote function onProjectCardEdited(ProjectCardPayload event) returns error? {
        io:println("FIRED::ProjectCardService::onProjectCardEdited");
    }
    remote function onProjectCardDeleted(ProjectCardPayload event) returns error? {
        io:println("FIRED::ProjectCardService::onProjectCardDeleted");
    }
    remote function onProjectCardMoved(ProjectCardPayload event) returns error? {
        io:println("FIRED::ProjectCardService::onProjectCardMoved");
    }
    remote function onProjectCardConverted(ProjectCardPayload event) returns error? {
        io:println("FIRED::ProjectCardService::onProjectCardConverted");
    }
    remote function onProjectCardCreated(ProjectCardPayload event) returns error? {
        io:println("FIRED::ProjectCardService::onProjectCardCreated");
    }
}

service CheckRunService on githubHarnessListener {
    remote function onCheckRunCreated(CheckRunPayload event) returns error? {
        io:println("FIRED::CheckRunService::onCheckRunCreated");
    }
    remote function onCheckRunCompleted(CheckRunPayload event) returns error? {
        io:println("FIRED::CheckRunService::onCheckRunCompleted");
    }
    remote function onCheckRunRequestedAction(CheckRunPayload event) returns error? {
        io:println("FIRED::CheckRunService::onCheckRunRequestedAction");
    }
    remote function onCheckRunRerequested(CheckRunPayload event) returns error? {
        io:println("FIRED::CheckRunService::onCheckRunRerequested");
    }
}

service PageBuildService on githubHarnessListener {
    remote function onPageBuild(PageBuildPayload event) returns error? {
        io:println("FIRED::PageBuildService::onPageBuild");
    }
}

service CustomPropertyService on githubHarnessListener {
    remote function onCustomPropertyUpdated(CustomPropertyPayload event) returns error? {
        io:println("FIRED::CustomPropertyService::onCustomPropertyUpdated");
    }
    remote function onCustomPropertyDeleted(CustomPropertyPayload event) returns error? {
        io:println("FIRED::CustomPropertyService::onCustomPropertyDeleted");
    }
    remote function onCustomPropertyPromoteToEnterprise(CustomPropertyPayload event) returns error? {
        io:println("FIRED::CustomPropertyService::onCustomPropertyPromoteToEnterprise");
    }
    remote function onCustomPropertyCreated(CustomPropertyPayload event) returns error? {
        io:println("FIRED::CustomPropertyService::onCustomPropertyCreated");
    }
}

service DependabotAlertService on githubHarnessListener {
    remote function onDependabotAlertAutoDismissed(DependabotAlertPayload event) returns error? {
        io:println("FIRED::DependabotAlertService::onDependabotAlertAutoDismissed");
    }
    remote function onDependabotAlertAutoReopened(DependabotAlertPayload event) returns error? {
        io:println("FIRED::DependabotAlertService::onDependabotAlertAutoReopened");
    }
    remote function onDependabotAlertCreated(DependabotAlertPayload event) returns error? {
        io:println("FIRED::DependabotAlertService::onDependabotAlertCreated");
    }
    remote function onDependabotAlertDismissed(DependabotAlertPayload event) returns error? {
        io:println("FIRED::DependabotAlertService::onDependabotAlertDismissed");
    }
    remote function onDependabotAlertReopened(DependabotAlertPayload event) returns error? {
        io:println("FIRED::DependabotAlertService::onDependabotAlertReopened");
    }
    remote function onDependabotAlertReintroduced(DependabotAlertPayload event) returns error? {
        io:println("FIRED::DependabotAlertService::onDependabotAlertReintroduced");
    }
    remote function onDependabotAlertAssigneesChanged(DependabotAlertPayload event) returns error? {
        io:println("FIRED::DependabotAlertService::onDependabotAlertAssigneesChanged");
    }
    remote function onDependabotAlertFixed(DependabotAlertPayload event) returns error? {
        io:println("FIRED::DependabotAlertService::onDependabotAlertFixed");
    }
}

service DeploymentStatusService on githubHarnessListener {
    remote function onDeploymentStatus(DeploymentStatusPayload event) returns error? {
        io:println("FIRED::DeploymentStatusService::onDeploymentStatus");
    }
}

service RepositoryAdvisoryService on githubHarnessListener {
    remote function onRepositoryAdvisoryReported(RepositoryAdvisoryPayload event) returns error? {
        io:println("FIRED::RepositoryAdvisoryService::onRepositoryAdvisoryReported");
    }
    remote function onRepositoryAdvisoryPublished(RepositoryAdvisoryPayload event) returns error? {
        io:println("FIRED::RepositoryAdvisoryService::onRepositoryAdvisoryPublished");
    }
}

service PullRequestReviewThreadService on githubHarnessListener {
    remote function onPullRequestReviewThreadUnresolved(PullRequestReviewThreadPayload event) returns error? {
        io:println("FIRED::PullRequestReviewThreadService::onPullRequestReviewThreadUnresolved");
    }
    remote function onPullRequestReviewThreadResolved(PullRequestReviewThreadPayload event) returns error? {
        io:println("FIRED::PullRequestReviewThreadService::onPullRequestReviewThreadResolved");
    }
}


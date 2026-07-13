import ballerina/http;

@display {label: ""}
public class Listener {
    private http:Listener httpListener;
    private DispatcherService dispatcherService;

    public function init(int|http:Listener listenTo = 8090, *ListenerConfiguration configuration) returns error? {
        if listenTo is http:Listener {
            self.httpListener = listenTo;
        } else {
            json configJson = configuration.toJson();
            map<json> configMap = check configJson.cloneWithType();
            _ = configMap.remove("webhookSecret");
            http:ListenerConfiguration httpConfig = check configMap.cloneWithType();
            self.httpListener = check new (listenTo, httpConfig);
        }
        self.dispatcherService = new DispatcherService(configuration.webhookSecret);
    }

    public isolated function attach(GenericServiceType serviceRef, () attachPoint) returns @tainted error? {
        string serviceTypeStr = self.getServiceTypeStr(serviceRef);
        check self.dispatcherService.addServiceRef(serviceTypeStr, serviceRef);
    }

    public isolated function detach(GenericServiceType serviceRef) returns error? {
        string serviceTypeStr = self.getServiceTypeStr(serviceRef);
        check self.dispatcherService.removeServiceRef(serviceTypeStr);
    }

    public isolated function 'start() returns error? {
        check self.httpListener.attach(self.dispatcherService, ());
        return self.httpListener.'start();
    }

    public isolated function gracefulStop() returns @tainted error? {
        return self.httpListener.gracefulStop();
    }

    public isolated function immediateStop() returns error? {
        return self.httpListener.immediateStop();
    }

    private isolated function getServiceTypeStr(GenericServiceType serviceRef) returns string {
        if serviceRef is DeleteService {
            return "DeleteService";
        } else if serviceRef is MetaService {
            return "MetaService";
        } else if serviceRef is WorkflowDispatchService {
            return "WorkflowDispatchService";
        } else if serviceRef is SecurityAndAnalysisService {
            return "SecurityAndAnalysisService";
        } else if serviceRef is DeployKeyService {
            return "DeployKeyService";
        } else if serviceRef is ProjectColumnService {
            return "ProjectColumnService";
        } else if serviceRef is MarketplacePurchaseService {
            return "MarketplacePurchaseService";
        } else if serviceRef is BranchProtectionConfigurationService {
            return "BranchProtectionConfigurationService";
        } else if serviceRef is PullRequestService {
            return "PullRequestService";
        } else if serviceRef is LabelService {
            return "LabelService";
        } else if serviceRef is DeploymentService {
            return "DeploymentService";
        } else if serviceRef is TeamAddService {
            return "TeamAddService";
        } else if serviceRef is CodeScanningAlertService {
            return "CodeScanningAlertService";
        } else if serviceRef is MembershipService {
            return "MembershipService";
        } else if serviceRef is SecretScanningAlertService {
            return "SecretScanningAlertService";
        } else if serviceRef is PushService {
            return "PushService";
        } else if serviceRef is MemberService {
            return "MemberService";
        } else if serviceRef is RepositoryDispatchService {
            return "RepositoryDispatchService";
        } else if serviceRef is StatusService {
            return "StatusService";
        } else if serviceRef is RepositoryImportService {
            return "RepositoryImportService";
        } else if serviceRef is PersonalAccessTokenRequestService {
            return "PersonalAccessTokenRequestService";
        } else if serviceRef is SubIssuesService {
            return "SubIssuesService";
        } else if serviceRef is RepositoryRulesetService {
            return "RepositoryRulesetService";
        } else if serviceRef is MilestoneService {
            return "MilestoneService";
        } else if serviceRef is PublicService {
            return "PublicService";
        } else if serviceRef is WorkflowRunService {
            return "WorkflowRunService";
        } else if serviceRef is ProjectsV2statusUpdateService {
            return "ProjectsV2statusUpdateService";
        } else if serviceRef is ProjectsV2itemService {
            return "ProjectsV2itemService";
        } else if serviceRef is SponsorshipService {
            return "SponsorshipService";
        } else if serviceRef is MergeGroupService {
            return "MergeGroupService";
        } else if serviceRef is ProjectService {
            return "ProjectService";
        } else if serviceRef is OrgBlockService {
            return "OrgBlockService";
        } else if serviceRef is SecretScanningAlertLocationService {
            return "SecretScanningAlertLocationService";
        } else if serviceRef is InstallationTargetService {
            return "InstallationTargetService";
        } else if serviceRef is CheckSuiteService {
            return "CheckSuiteService";
        } else if serviceRef is PingService {
            return "PingService";
        } else if serviceRef is IssueCommentService {
            return "IssueCommentService";
        } else if serviceRef is SecurityAdvisoryService {
            return "SecurityAdvisoryService";
        } else if serviceRef is PackageService {
            return "PackageService";
        } else if serviceRef is DiscussionService {
            return "DiscussionService";
        } else if serviceRef is ForkService {
            return "ForkService";
        } else if serviceRef is PullRequestReviewService {
            return "PullRequestReviewService";
        } else if serviceRef is OrganizationService {
            return "OrganizationService";
        } else if serviceRef is IssuesService {
            return "IssuesService";
        } else if serviceRef is RegistryPackageService {
            return "RegistryPackageService";
        } else if serviceRef is ProjectsV2Service {
            return "ProjectsV2Service";
        } else if serviceRef is RepositoryVulnerabilityAlertService {
            return "RepositoryVulnerabilityAlertService";
        } else if serviceRef is StarService {
            return "StarService";
        } else if serviceRef is CreateService {
            return "CreateService";
        } else if serviceRef is DeploymentReviewService {
            return "DeploymentReviewService";
        } else if serviceRef is GollumService {
            return "GollumService";
        } else if serviceRef is GithubAppAuthorizationService {
            return "GithubAppAuthorizationService";
        } else if serviceRef is WatchService {
            return "WatchService";
        } else if serviceRef is TeamService {
            return "TeamService";
        } else if serviceRef is WorkflowJobService {
            return "WorkflowJobService";
        } else if serviceRef is ReleaseService {
            return "ReleaseService";
        } else if serviceRef is InstallationService {
            return "InstallationService";
        } else if serviceRef is CommitCommentService {
            return "CommitCommentService";
        } else if serviceRef is DiscussionCommentService {
            return "DiscussionCommentService";
        } else if serviceRef is BranchProtectionRuleService {
            return "BranchProtectionRuleService";
        } else if serviceRef is IssueDependenciesService {
            return "IssueDependenciesService";
        } else if serviceRef is RepositoryService {
            return "RepositoryService";
        } else if serviceRef is PullRequestReviewCommentService {
            return "PullRequestReviewCommentService";
        } else if serviceRef is DeploymentProtectionRuleService {
            return "DeploymentProtectionRuleService";
        } else if serviceRef is CustomPropertyValuesService {
            return "CustomPropertyValuesService";
        } else if serviceRef is InstallationRepositoriesService {
            return "InstallationRepositoriesService";
        } else if serviceRef is SecretScanningScanService {
            return "SecretScanningScanService";
        } else if serviceRef is ProjectCardService {
            return "ProjectCardService";
        } else if serviceRef is CheckRunService {
            return "CheckRunService";
        } else if serviceRef is PageBuildService {
            return "PageBuildService";
        } else if serviceRef is CustomPropertyService {
            return "CustomPropertyService";
        } else if serviceRef is DependabotAlertService {
            return "DependabotAlertService";
        } else if serviceRef is DeploymentStatusService {
            return "DeploymentStatusService";
        } else if serviceRef is RepositoryAdvisoryService {
            return "RepositoryAdvisoryService";
        } else {
            return "PullRequestReviewThreadService";
        }
    }
}

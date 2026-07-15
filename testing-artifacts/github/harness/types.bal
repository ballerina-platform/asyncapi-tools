import ballerina/http;

public type ListenerConfiguration record {|
    *http:ListenerConfiguration;
    string webhookSecret = "";
|};

public type ForkPayload record {
    # The created (forked) repository
    Repository forkee;
    User sender;
    Repository repository;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type WorkflowRunPayload record {
    string action;
    WorkflowRun workflow_run;
    # The workflow that is being run
    record { int id?; string node_id?; string name?; string path?; string state?; string created_at?; string updated_at?; string url?; string html_url?; string badge_url?;}  workflow;
    User sender?;
    Repository repository?;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type GollumPayload record {
    # The pages that were updated
    record { # The name of the page
        string page_name; # The current page title
        string title; # A summary of the changes
        string summary?; string action; # The latest commit SHA of the page
        string sha; string html_url;} [] pages;
    User sender;
    Repository repository;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type ReleasePayload record {
    # The action that was performed
    string action;
    Release release;
    # For edited events, the changes to the release
    record {} changes?;
    User sender;
    Repository repository;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type SecretScanningAlertLocationPayload record {
    # The existing secret scanning alert the location was added to
    record { int number; string secret_type;}  alert;
    # The location where the secret was found
    record { string 'type; # Location details; shape varies by type
        record { string path?; int start_line?; int end_line?; int start_column?; int end_column?; string blob_sha?; string blob_url?; string commit_sha?; string commit_url?;}  details?;}  location;
    User sender?;
    Repository repository?;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type DeploymentReviewPayload record {
    string action;
    # The name of the environment that was approved or rejected
    string environment;
    # The reviewer's comment (for approved/rejected)
    string comment?;
    # ISO 8601 date of when the review was requested
    string since;
    # The reviewers who were requested or who reviewed
    record { string 'type?; # A User or Team object depending on type
        anydata reviewer?;} [] reviewers?;
    # The workflow run associated with the deployment
    record { int id; string name; string head_sha?; string head_branch?; int run_number?; string status?; string conclusion?; string html_url?; record {}[] pull_requests?;}  workflow_run;
    User sender?;
    Repository repository?;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type PullRequest record {
    int id;
    string node_id?;
    string url?;
    string html_url?;
    string diff_url?;
    string patch_url?;
    int number;
    string state;
    boolean locked?;
    string title;
    string body?;
    User user?;
    Label[] labels?;
    User assignee?;
    User[] assignees?;
    Milestone milestone?;
    PullRequestRef head?;
    PullRequestRef base?;
    boolean draft?;
    boolean merged?;
    boolean mergeable?;
    boolean rebaseable?;
    string mergeable_state?;
    string merge_commit_sha?;
    int comments?;
    int review_comments?;
    int commits?;
    int additions?;
    int deletions?;
    int changed_files?;
    string created_at?;
    string updated_at?;
    string closed_at?;
    string merged_at?;
    User merged_by?;
    string author_association?;
    record {} auto_merge?;
};

public type SecretScanningScanPayload record {
    # What type of scan was completed
    string 'type;
    # What type of content was scanned
    string 'source;
    # ISO 8601 timestamp when the scan started
    string started_at;
    # ISO 8601 timestamp when the scan completed
    string completed_at;
    # Patterns updated. Empty for normal backfill or custom pattern scans.
    string[] secret_types?;
    # If triggered by a custom pattern update, the name of that pattern
    string custom_pattern_name?;
    # If triggered by a custom pattern update, the scope of that pattern
    string custom_pattern_scope?;
    User sender?;
    Repository repository?;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type IssueCommentPayload record {
    string action;
    Issue issue;
    IssueComment comment;
    # For edited events, the changes to the comment
    record {} changes?;
    User sender;
    Repository repository;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type DeploymentStatusPayload record {
    string action;
    Deployment deployment;
    DeploymentStatus deployment_status;
    CheckRun check_run?;
    record {} workflow?;
    record {} workflow_run?;
    User sender;
    Repository repository;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type OrganizationPayload record {
    string action;
    # The membership between the user and the organization.
    # Not present when the action is member_invited.
    record { string url?; string state?; string role?; string organization_url?; User user?;}  membership?;
    # Present when action is member_invited
    record {} invitation?;
    # For renamed events, the old and new organization name
    record {} changes?;
    User sender?;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type WebhookHeaders record {
    # The name of the event that triggered the delivery
    @http:Header {name: "X-GitHub-Event"}
    string xGitHubEvent;
    # A globally unique identifier (GUID) for this delivery
    @http:Header {name: "X-GitHub-Delivery"}
    string xGitHubDelivery;
    # The unique identifier of the webhook
    @http:Header {name: "X-GitHub-Hook-ID"}
    int xGitHubHookID;
    # The unique identifier of the resource where the webhook was created
    @http:Header {name: "X-GitHub-Hook-Installation-Target-ID"}
    int xGitHubHookInstallationTargetID?;
    # The type of resource where the webhook was created
    @http:Header {name: "X-GitHub-Hook-Installation-Target-Type"}
    string xGitHubHookInstallationTargetType?;
    # HMAC hex digest of the request body using SHA-1. Sent only when a
    # webhook secret is configured. Use X-Hub-Signature-256 instead.
    @http:Header {name: "X-Hub-Signature"}
    string xHubSignature?;
    # HMAC hex digest of the request body using SHA-256. Sent only when
    # a webhook secret is configured. Preferred over X-Hub-Signature.
    @http:Header {name: "X-Hub-Signature-256"}
    string xHubSignature256?;
    # Always has the prefix GitHub-Hookshot/
    @http:Header {name: "User-Agent"}
    string userAgent?;
};

public type RepositoryDispatchPayload record {
    # The event_type specified in the dispatch request body
    string action;
    # The branch from which the dispatch was triggered
    string branch;
    # The client_payload from the dispatch request body
    record {} client_payload;
    Installation installation?;
    User sender?;
    Repository repository?;
    Organization organization?;
    Enterprise enterprise?;
};

public type MergeGroupPayload record {
    string action;
    # A group of pull requests grouped together by the merge queue
    record { # The SHA of the merge group's head commit
        string head_sha; # The full ref of the merge group targeting branch
        string head_ref; # The SHA of the merge group's base branch
        string base_sha; # The full ref of the branch being merged into
        string base_ref; Commit head_commit?;}  merge_group;
    # For destroyed action, the reason the merge group was destroyed
    string reason?;
    User sender?;
    Repository repository?;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type WorkflowJobPayload record {
    string action;
    WorkflowJob workflow_job;
    # The deployment associated with the workflow job (if applicable)
    Deployment deployment?;
    User sender?;
    Repository repository?;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type OrgBlockPayload record {
    string action;
    User blocked_user;
    User sender?;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type DependabotAlertPayload record {
    string action;
    # A Dependabot alert
    record { int number; string state; record { record { string ecosystem?; string name?;}  package?; string manifest_path?; string scope?;}  dependency?; record { string ghsa_id?; string cve_id?; string summary?; string description?; string severity?; record {}[] vulnerabilities?;}  security_advisory?; record { record {} package?; string severity?; string vulnerable_version_range?; record { string identifier?;}  first_patched_version?;}  security_vulnerability?; string url?; string html_url?; string created_at?; string updated_at?; string dismissed_at?; User dismissed_by?; string dismissed_reason?; string dismissed_comment?; string fixed_at?; string auto_dismissed_at?; User[] assignees?;}  alert;
    User sender?;
    Repository repository?;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type CustomPropertyValuesPayload record {
    string action;
    # The new custom property values for the repository
    record { string property_name; # String or array of strings
        anydata value?;} [] new_property_values;
    # The old custom property values for the repository
    record { string property_name; # String or array of strings
        anydata value?;} [] old_property_values;
    User sender?;
    Repository repository?;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type SecretScanningAlertPayload record {
    string action;
    # The secret scanning alert
    record { int number; string created_at?; string updated_at?; string url?; string html_url?; string locations_url?; string state; string resolution?; string resolved_at?; User resolved_by?; string resolution_comment?; # The type of secret that was detected
        string secret_type?; string secret_type_display_name?; string validity?; boolean publicly_leaked?; boolean multi_repo?; boolean push_protection_bypassed?; User push_protection_bypassed_by?; string push_protection_bypassed_at?;}  alert;
    # Present on assigned/unassigned actions
    User assignee?;
    User sender?;
    Repository repository?;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type PullRequestReviewThreadPayload record {
    string action;
    PullRequest pull_request;
    # The review thread that was resolved or unresolved
    record { string node_id?; PullRequestReviewComment[] comments?;}  thread;
    string updated_at?;
    User sender?;
    Repository repository?;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type IssueComment record {
    int id;
    string node_id?;
    string url?;
    string html_url?;
    string body;
    User user?;
    string created_at?;
    string updated_at?;
    string author_association?;
};

public type RegistryPackagePayload record {
    string action;
    # The registry package object
    record { int id; string name; string namespace?; string description?; string ecosystem?; string package_type; string html_url?; string created_at?; string updated_at?; User owner?; record { int id?; string 'version?; string summary?; string html_url?; string target_commitish?; string target_oid?; boolean draft?; boolean prerelease?; string created_at?; string updated_at?; record { string download_url?; int id?; string name?; string 'sha256?; string content_type?; int size?; string created_at?; string updated_at?;} [] package_files?; User author?; string installation_command?;}  package_version?; record { string about_url?; string name?; string 'type?; string url?; string vendor?;}  registry?;}  registry_package;
    User sender?;
    Repository repository?;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type CheckSuitePayload record {
    string action;
    CheckSuite check_suite;
    User sender?;
    Repository repository?;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type DiscussionCommentPayload record {
    string action;
    # The discussion comment
    record { int id?; string node_id?; string html_url?; string body?; User user?; string created_at?; string updated_at?; string author_association?;}  comment;
    Discussion discussion;
    # For edited events, the changes to the comment
    record {} changes?;
    User sender?;
    Repository repository?;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type Organization record {
    string login?;
    int id?;
    string node_id?;
    string url?;
    string html_url?;
    string repos_url?;
    string avatar_url?;
    string description?;
};

public type RepositoryImportPayload record {
    # The final status of the import
    string status;
    User sender?;
    Repository repository?;
    Organization organization?;
    Installation installation?;
};

public type RepositoryPayload record {
    string action;
    # For edited/renamed/transferred events, the changes that occurred
    record {} changes?;
    Repository repository?;
    User sender?;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type StarPayload record {
    string action;
    # The time the star was created (ISO 8601). Null for the deleted action.
    string starred_at;
    User sender?;
    Repository repository?;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type WatchPayload record {
    string action;
    User sender;
    Repository repository;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type PackagePayload record {
    string action;
    # Information about the package
    record { int id; string name; string namespace?; string description?; string ecosystem?; string package_type?; string html_url?; string created_at?; string updated_at?; User owner?; record { int id?; string 'version?; string summary?; string name?; string description?; string body?; string body_html?; record {} release?; string manifest?; string html_url?; string tag_name?; string target_commitish?; string target_oid?; boolean draft?; boolean prerelease?; string created_at?; string updated_at?; record {}[] metadata?; record {} container_metadata?; record {} npm_metadata?; record {}[] nuget_metadata?; record {}[] rubygems_metadata?; record {}[] package_files?; string package_url?; User author?; string source_url?; string installation_command?;}  package_version?; record { string about_url?; string name?; string 'type?; string url?; string vendor?;}  registry?;}  package;
    User sender?;
    Repository repository?;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type WorkflowDispatchPayload record {
    # The inputs provided when manually triggering the workflow
    record {} inputs?;
    # The branch or tag ref from which the workflow was triggered
    string ref;
    # The path to the workflow file (e.g. .github/workflows/main.yml)
    string workflow;
    User sender?;
    Repository repository?;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type SponsorshipPayload record {
    string action;
    # The sponsorship object
    record { string node_id; string created_at; string privacy_level; # The tier the sponsor has chosen
        record { string node_id; string created_at?; string description?; int monthly_price_in_cents; int monthly_price_in_dollars; string name; boolean is_one_time?; boolean is_custom_amount?;}  tier; User sponsor; User sponsorable;}  sponsorship;
    # For edited, tier_changed, and pending_tier_change events
    record { record { # The previous tier object (same shape as sponsorship.tier)
            record {} 'from?;}  tier?; record { string 'from?;}  privacy_level?;}  changes?;
    # For pending_cancellation and pending_tier_change, the date the
    # change takes effect (ISO 8601 date).
    string effective_date?;
    User sender?;
    Organization organization?;
    Installation installation?;
};

public type SubIssuesPayload record {
    string action;
    # The ID of the parent issue.
    int parent_issue_id;
    # The parent issue.
    Issue parent_issue;
    # The repository of the parent issue.
    Repository parent_issue_repo;
    # The ID of the sub-issue.
    int sub_issue_id;
    # The sub-issue.
    Issue sub_issue;
    User sender?;
    Repository repository?;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type ProjectColumnPayload record {
    string action;
    # A column in a classic project board
    record { int id; string node_id; string url?; string project_url?; string cards_url?; string name; # The ID of the column this column was moved after
        int after_id?; string created_at?; string updated_at?;}  project_column;
    # For edited events, the changes made to the column
    record { record { string 'from?;}  name?;}  changes?;
    User sender?;
    Repository repository?;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type Team record {
    int id;
    string node_id?;
    string name;
    string slug;
    string description?;
    string privacy?;
    string notification_setting?;
    string permission?;
    string url?;
    string html_url?;
    string members_url?;
    string repositories_url?;
    record {} parent?;
};

public type MarketplacePurchasePayload record {
    string action;
    # The GitHub Marketplace purchase
    record { record { string 'type; int id; string node_id?; string login; string organization_billing_email?;}  account; string billing_cycle; int unit_count; boolean on_free_trial?; string free_trial_ends_on?; string next_billing_date?; record { int id; string name; string description; int monthly_price_in_cents; int yearly_price_in_cents; string price_model; boolean has_free_trial?; string unit_name?; string[] bullets?;}  plan;}  marketplace_purchase;
    # The previous purchase state (for changed/pending_change events)
    record {} previous_marketplace_purchase?;
    # ISO 8601 date when the change takes effect
    string effective_date;
    User sender;
    Installation installation?;
};

public type PushPayload record {
    # The full git ref that was pushed (e.g. refs/heads/main or refs/tags/v3.14.1)
    string ref;
    # The SHA of the most recent commit on ref before the push
    string before;
    # The SHA of the most recent commit on ref after the push
    string after;
    # The base ref for the push (if applicable)
    string base_ref?;
    # Whether this push created the ref
    boolean created;
    # Whether this push deleted the ref
    boolean deleted;
    # Whether this push was a force push of the ref
    boolean forced;
    # URL showing the changes in this ref update
    string compare;
    # Array of commit objects (maximum 2048)
    Commit[] commits;
    Commit head_commit?;
    # Metaproperties for the Git author/committer
    CommitAuthor pusher;
    User sender;
    Repository repository;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type Installation record {
    int id?;
    string node_id?;
};

public type BranchProtectionRulePayload record {
    string action;
    # The branch protection rule. Includes name and all branch protection
    # settings applied to matching branches. Binary settings are boolean;
    # multi-level configs are off, non_admins, or everyone; actor and
    # build lists are arrays of strings.
    record { int id?; int repository_id?; string name?; string created_at?; string updated_at?; string pull_request_reviews_enforcement_level?; int required_approving_review_count?; boolean dismiss_stale_reviews_on_push?; boolean require_code_owner_review?; boolean authorized_dismissal_actors_only?; boolean ignore_approvals_from_contributors?; boolean require_last_push_approval?; string[] required_status_checks?; string required_status_checks_enforcement_level?; boolean strict_required_status_checks_policy?; string signature_requirement_enforcement_level?; string linear_history_requirement_enforcement_level?; boolean admin_enforced?; string allow_force_pushes_enforcement_level?; string allow_deletions_enforcement_level?; string merge_queue_enforcement_level?; string required_deployments_enforcement_level?; string required_conversation_resolution_level?; boolean authorized_actors_only?; string[] authorized_actor_names?;}  rule;
    # For edited events, the changes to the rule
    record {} changes?;
    User sender?;
    Repository repository?;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type PullRequestReviewCommentPayload record {
    string action;
    PullRequestReviewComment comment;
    PullRequest pull_request;
    # For edited events, the changes to the comment
    record {} changes?;
    User sender;
    Repository repository;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type PullRequestRef record {
    string label?;
    string ref?;
    string sha?;
    User user?;
    Repository repo?;
};

public type 'ProjectsV2ItemPayload record {
    string action;
    # An item belonging to a Projects v2 project
    record { int id; string node_id; string project_node_id; string content_node_id; string content_type; string created_at?; string updated_at?; string archived_at?; User creator?;}  'projects_v2_item;
    # The changes made to the item (for edited events)
    record { record { string field_node_id?; string field_type?;}  field_value?;}  changes;
    User sender?;
    Organization organization?;
    Installation installation?;
};

public type PingPayload record {
    # Random string of GitHub zen
    string zen?;
    # The ID of the webhook that triggered the ping
    int hook_id?;
    # The webhook that is being pinged
    record { string 'type?; int id?; string name?; boolean active?; string[] events?; record { string content_type?; string insecure_ssl?; string url?;}  config?; string updated_at?; string created_at?; string url?;}  hook?;
    User sender?;
    Repository repository?;
    Organization organization?;
};

public type CreatePayload record {
    # The git ref resource (branch or tag name)
    string ref;
    # The type of Git ref object created
    string ref_type;
    # The name of the repository's default branch (usually main)
    string master_branch;
    # The repository's current description
    string description?;
    # The pusher type; either user or a deploy key
    string pusher_type;
    User sender;
    Repository repository;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type Repository record {
    int id;
    string node_id?;
    # The repository name
    string name;
    # The full repository name including owner (e.g. octocat/Hello-World)
    string full_name;
    User owner;
    # Whether the repository is private
    boolean 'private;
    string html_url?;
    string description?;
    boolean 'fork?;
    string url?;
    string homepage?;
    string language?;
    int forks_count?;
    int stargazers_count?;
    int watchers_count?;
    int size?;
    string default_branch?;
    int open_issues_count?;
    string[] topics?;
    boolean has_issues?;
    boolean has_projects?;
    boolean has_wiki?;
    boolean has_pages?;
    boolean has_downloads?;
    boolean archived?;
    boolean disabled?;
    string visibility?;
    string pushed_at?;
    string created_at?;
    string updated_at?;
    record { string 'key?; string name?; string spdx_id?; string url?;}  license?;
};

public type PullRequestReviewComment record {
    int id;
    string node_id?;
    int pull_request_review_id?;
    string url?;
    string html_url?;
    string body;
    string diff_hunk?;
    string path?;
    int position?;
    int original_position?;
    string commit_id?;
    string original_commit_id?;
    User user?;
    string created_at?;
    string updated_at?;
    string author_association?;
    string side?;
    string start_side?;
};

public type TeamPayload record {
    string action;
    Team team;
    # For edited events, the changes to the team
    record { record { string 'from?;}  description?; record { string 'from?;}  name?; record { string 'from?;}  privacy?; record { string 'from?;}  notification_setting?; # For added_to_repository/removed_from_repository events
        record {} repository?;}  changes?;
    # Present for added_to_repository and removed_from_repository actions
    Repository repository?;
    User sender?;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type Enterprise record {
    int id?;
    string slug?;
    string name?;
    string node_id?;
    string avatar_url?;
    string description?;
    string website_url?;
    string html_url?;
    string created_at?;
    string updated_at?;
};

public type ProjectPayload record {
    string action;
    # A classic project board
    record { int id; string node_id; string url?; string html_url?; string columns_url?; string name; string body?; int number; string state; User creator?; string created_at?; string updated_at?;}  project;
    # For edited events, the changes made to the project
    record { record { string 'from?;}  name?; record { string 'from?;}  body?;}  changes?;
    User sender?;
    Repository repository?;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type InstallationTargetPayload record {
    string action;
    # The account (user or organization) where the app is installed
    record { int id; string node_id?; string login; string 'type; string avatar_url?; string html_url?; boolean site_admin?;}  account;
    string target_type;
    # The changes made to the account
    record { record { string 'from?;}  login?; record { string 'from?;}  slug?;}  changes;
    Installation installation?;
};

public type DeploymentStatus record {
    int id;
    string node_id?;
    string state;
    User creator?;
    string description?;
    string environment?;
    string environment_url?;
    string log_url?;
    string target_url?;
    string deployment_url?;
    string repository_url?;
    string created_at?;
    string updated_at?;
    record {} performed_via_github_app?;
};

public type InstallationRepositoriesPayload record {
    string action;
    # Repositories added to the installation
    record { int id?; string node_id?; string name?; string full_name?; boolean 'private?;} [] repositories_added;
    # Repositories removed from the installation
    record { int id?; string node_id?; string name?; string full_name?; boolean 'private?;} [] repositories_removed;
    # Whether all repositories or a selection are accessible
    string repository_selection;
    User requester;
    Installation installation?;
    User sender?;
    Organization organization?;
    Enterprise enterprise?;
};

public type Issue record {
    int id;
    string node_id?;
    string url?;
    string html_url?;
    int number;
    string title;
    string body?;
    string state;
    boolean locked?;
    User user?;
    Label[] labels?;
    User assignee?;
    User[] assignees?;
    Milestone milestone?;
    int comments?;
    string created_at?;
    string updated_at?;
    string closed_at?;
    string author_association?;
    string active_lock_reason?;
};

public type Label record {
    int id;
    string node_id?;
    string url?;
    string name;
    # 6-character hex color code
    string color;
    boolean 'default?;
    string description?;
};

public type Deployment record {
    int id;
    string node_id?;
    string sha;
    string ref;
    string task;
    record {} payload?;
    string original_environment?;
    string environment;
    string description?;
    User creator?;
    string created_at?;
    string updated_at?;
    string statuses_url?;
    string repository_url?;
    boolean transient_environment?;
    boolean production_environment?;
    record {} performed_via_github_app?;
};

public type BranchProtectionConfigurationPayload record {
    # disabled — all branch protections were disabled. enabled — all were enabled.
    string action;
    User sender?;
    Repository repository?;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type RepositoryRulesetPayload record {
    string action;
    # A set of rules to apply when specified conditions are met
    record { int id; string name; string target?; string source_type?; string 'source?; string enforcement; record {} conditions?; record { string 'type?; record {} parameters?;} [] rules?; record { int actor_id?; string actor_type?; string bypass_mode?;} [] bypass_actors?; string created_at?; string updated_at?;}  repository_ruleset;
    # For edited events, the changes made to the ruleset
    record {} changes?;
    User sender?;
    Repository repository?;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type SecurityAndAnalysisPayload record {
    # The security and analysis settings that changed
    record { # Change to GitHub Advanced Security enablement
        record { string 'from?; string to?;}  advanced_security?; # Change to Dependabot alerts enablement
        record { string 'from?; string to?;}  dependabot_alerts?; # Change to Dependabot security updates enablement
        record { string 'from?; string to?;}  dependabot_security_updates?; # Change to secret scanning enablement
        record { string 'from?; string to?;}  secret_scanning?; # Change to secret scanning push protection enablement
        record { string 'from?; string to?;}  secret_scanning_push_protection?; # Change to non-provider pattern scanning enablement
        record { string 'from?; string to?;}  secret_scanning_non_provider_patterns?;}  changes;
    User sender?;
    Repository repository?;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type CommitAuthor record {
    string name?;
    string email?;
    string username?;
};

public type DeployKeyPayload record {
    string action;
    # The deploy key resource
    record { int id; # The public key
        string 'key; string url?; string title?; boolean verified?; string created_at?; boolean read_only?; string added_by?; string last_used?;}  'key;
    User sender?;
    Repository repository?;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type IssueDependenciesPayload record {
    string action;
    # The ID of the blocked issue.
    int blocked_issue_id?;
    Issue blocked_issue?;
    # The ID of the blocking issue.
    int blocking_issue_id?;
    Issue blocking_issue?;
    Repository blocking_issue_repo?;
    User sender?;
    Repository repository?;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type RepositoryAdvisoryPayload record {
    string action;
    # A repository security advisory
    record { # The GitHub Security Advisory identifier
        string ghsa_id; string cve_id; string url?; string html_url?; string summary; string description?; string severity; User author?; User publisher?; record { string 'type?; string value?;} [] identifiers?; string state; string created_at?; string updated_at?; string published_at?; string withdrawn_at?; record {} submission?; record { record { string ecosystem?; string name?;}  package?; string vulnerable_version_range?; string patched_versions?; string[] vulnerable_functions?;} [] vulnerabilities?; record { string vector_string?; decimal score?;}  cvss?; record { string cwe_id?; string name?;} [] cwes?; record { User user?; string 'type?;} [] credits?;}  repository_advisory;
    User sender?;
    Repository repository?;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type RepositoryVulnerabilityAlertPayload record {
    string action;
    # The security alert of the vulnerable dependency
    record { int id; string affected_package_name; string affected_range; string fixed_in?; string severity; string ghsa_id?; string external_identifier?; string external_reference?; string created_at?; string auto_dismissed_at?; string dismiss_reason?; string dismissed_at?; User dismissed_by?; int number?;}  alert;
    User sender?;
    Repository repository?;
    Organization organization?;
    Installation installation?;
};

public type Milestone record {
    int id?;
    string node_id?;
    int number?;
    string title?;
    string description?;
    string state?;
    int open_issues?;
    int closed_issues?;
    string created_at?;
    string updated_at?;
    string due_on?;
    string closed_at?;
    User creator?;
};

public type IssuesPayload record {
    # The action that was performed
    string action;
    Issue issue;
    User assignee?;
    Label label?;
    # For edited events, the changes to the issue
    record {} changes?;
    Milestone milestone?;
    User sender;
    Repository repository;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type CodeScanningAlertPayload record {
    string action;
    # The code scanning alert involved in the event
    record { int number; string created_at?; string updated_at?; string url?; string html_url?; string state; string fixed_at?; User dismissed_by?; string dismissed_at?; string dismissed_reason?; string dismissed_comment?; record { string id?; string severity?; string security_severity_level?; string description?; string name?; string full_description?; string[] tags?; string help?;}  rule?; record { string name?; string guid?; string 'version?;}  tool?; record { string ref?; string analysis_key?; string environment?; string state?; string commit_sha?; record {} location?;}  most_recent_instance?;}  alert;
    # The commit SHA of the alert. Empty when action is reopened_by_user
    # or closed_by_user.
    string commit_oid;
    # The git ref of the alert. Empty when action is reopened_by_user
    # or closed_by_user.
    string ref;
    User sender?;
    Repository repository?;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type PullRequestReviewPayload record {
    string action;
    PullRequestReview review;
    PullRequest pull_request;
    # For edited events, the changes to the review
    record {} changes?;
    User sender;
    Repository repository;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type 'ProjectsV2Payload record {
    string action;
    # A Projects v2 project
    record { int id; string node_id; User owner; User creator?; string title; string description?; boolean 'public?; string closed_at?; string created_at?; string updated_at?; string deleted_at?; User deleted_by?; int number?; string short_description?; string status?;}  'projects_v2;
    User sender?;
    Organization organization?;
    Installation installation?;
};

public type PersonalAccessTokenRequestPayload record {
    string action;
    # A fine-grained personal access token request
    record { int id; User owner; # Permissions added by the request
        record { record {} organization?; record {} repository?; record {} other?;}  permissions_added?; # Permissions upgraded from existing token
        record { record {} organization?; record {} repository?; record {} other?;}  permissions_upgraded?; # The resulting full set of permissions if approved
        record { record {} organization?; record {} repository?; record {} other?;}  permissions_result?; string repository_selection?; string repositories_url?; Repository[] repositories?; boolean token_expired?; string token_expires_at?; string token_last_used_at?; string created_at?;}  personal_access_token_request;
    User sender?;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type InstallationPayload record {
    string action;
    Installation installation;
    # An array of repositories the installation can access
    record { int id?; string node_id?; string name?; string full_name?; boolean 'private?;} [] repositories?;
    User requester?;
    User sender?;
    Organization organization?;
    Enterprise enterprise?;
};

public type WorkflowRun record {
    int id;
    string name;
    string node_id?;
    int check_suite_id?;
    string check_suite_node_id?;
    string head_branch?;
    string head_sha?;
    int run_number?;
    string event?;
    string status;
    string conclusion?;
    int workflow_id?;
    string url?;
    string html_url?;
    record {}[] pull_requests?;
    string created_at?;
    string updated_at?;
    int run_attempt?;
    string run_started_at?;
    User actor?;
    User triggering_actor?;
    string jobs_url?;
    string logs_url?;
    string check_suite_url?;
    string artifacts_url?;
    string cancel_url?;
    string rerun_url?;
    string workflow_url?;
    Commit head_commit?;
    Repository repository?;
};

public type DiscussionPayload record {
    string action;
    Discussion discussion;
    # Present on answered action — the comment marked as answer
    record {} answer?;
    Label label?;
    # For edited/category_changed events, the changes made
    record {} changes?;
    User sender?;
    Repository repository?;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type CheckSuite record {
    int id;
    string node_id?;
    string head_branch?;
    string head_sha?;
    string status?;
    string conclusion?;
    string url?;
    string before?;
    string after?;
    record {}[] pull_requests?;
    record {} app?;
    string created_at?;
    string updated_at?;
};

public type StatusPayload record {
    # The unique identifier of the status
    int id;
    # The commit SHA
    string sha;
    # The repository name
    string name;
    # The new state of the commit status
    string state;
    # The status context identifier
    string context;
    # The optional human-readable description
    string description?;
    # The optional link added to the status
    string target_url?;
    string avatar_url?;
    # The commit the status is associated with
    record { string sha?; record {} 'commit?; string url?; string html_url?; User author?; User committer?;}  'commit;
    # Array of branches containing the status SHA (max 10)
    record { string name?; record { string sha?; string url?;}  'commit?; boolean protected?;} [] branches;
    string created_at;
    string updated_at;
    User sender;
    Repository repository;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type 'ProjectsV2StatusUpdatePayload record {
    string action;
    # A status update belonging to a Projects v2 project
    record { int id; string node_id; string project_node_id; string status?; string body?; string created_at?; string updated_at?; string start_date?; string target_date?; User creator?;}  'projects_v2_status_update;
    User sender?;
    Organization organization?;
    Installation installation?;
};

public type CommonPayload record {
    User sender?;
    Repository repository?;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type Discussion record {
    int id;
    string node_id?;
    int number;
    string title;
    string body?;
    string state;
    record { int id?; string node_id?; int repository_id?; string emoji?; string name?; string description?; string created_at?; string updated_at?; string slug?; boolean is_answerable?;}  category?;
    User user?;
    string html_url?;
    int comments?;
    Label[] labels?;
    boolean locked?;
    string active_lock_reason?;
    string answer_html_url?;
    string answer_chosen_at?;
    User answer_chosen_by?;
    string created_at?;
    string updated_at?;
};

public type User record {
    # The user's GitHub username
    string login;
    # The user's unique numeric identifier
    int id;
    string node_id?;
    string avatar_url?;
    string gravatar_id?;
    string url?;
    string html_url?;
    string 'type?;
    boolean site_admin?;
};

public type PullRequestReview record {
    int id;
    string node_id?;
    User user?;
    string body?;
    string state;
    string html_url?;
    string pull_request_url?;
    string submitted_at?;
    string commit_id?;
    string author_association?;
};

public type DeletePayload record {
    # The git ref resource (branch or tag name)
    string ref;
    # The type of Git ref object deleted
    string ref_type;
    # The pusher type; either user or a deploy key
    string pusher_type;
    User sender;
    Repository repository;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type MetaPayload record {
    # Always deleted — the webhook that triggered this event was deleted
    string action;
    # The id of the modified webhook
    int hook_id;
    # The deleted webhook. Fields vary by webhook type (repository,
    # organization, business, app, or GitHub Marketplace).
    record { string 'type; int id; string name; boolean active; string[] events?; record { string content_type?; string insecure_ssl?; string url?; # Omitted from payloads for security
            string secret?;}  config?; string updated_at?; string created_at?;}  hook;
    User sender?;
    Repository repository?;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type DeploymentPayload record {
    string action;
    Deployment deployment;
    # The workflow that triggered the deployment (if applicable)
    record {} workflow;
    # The workflow run that triggered the deployment (if applicable)
    record {} workflow_run;
    User sender;
    Repository repository;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type DeploymentProtectionRulePayload record {
    # The name of the environment that has the deployment protection rule
    string environment?;
    # The event that triggered the deployment protection rule
    string event?;
    # The commit SHA that triggered the workflow
    string sha?;
    # The branch or tag ref that triggered the workflow
    string ref?;
    # The URL to call to approve or reject the deployment
    string deployment_callback_url?;
    # A request for a specific ref to be deployed
    Deployment deployment?;
    # The pull requests associated with the deployment
    record { int number?; string url?; PullRequestRef head?; PullRequestRef base?;} [] pull_requests?;
    User sender?;
    Installation installation?;
    Repository repository?;
    Organization organization?;
};

public type LabelPayload record {
    string action;
    Label label;
    # For edited events, the changes to the label
    record { record { string 'from?;}  color?; record { string 'from?;}  name?; record { string 'from?;}  description?;}  changes?;
    User sender;
    Repository repository;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type GithubAppAuthorizationPayload record {
    # Always revoked — a user revoked their GitHub App authorization
    string action;
    User sender?;
    Installation installation?;
};

public type PageBuildPayload record {
    # The unique identifier of the page build
    int id;
    # The GitHub Pages build object
    record { string url; # Current build status
        string status; # Error information if the build failed
        record { string message;}  'error; User pusher; # The SHA of the commit that triggered the build
        string 'commit; # Duration of the build in milliseconds
        int duration; string created_at; string updated_at;}  build;
    User sender?;
    Repository repository?;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type ProjectCardPayload record {
    string action;
    # A card on a classic project board
    record { int id; string node_id; string url?; int column_id; string column_url?; string project_url?; string note?; # Link to the issue or PR if the card is content-based
        string content_url?; # The ID of the card this card was moved after
        int after_id?; User creator?; string created_at?; string updated_at?;}  project_card;
    # For edited/moved events, the changes made
    record { record { string 'from?;}  note?; record { int 'from?;}  column_id?;}  changes?;
    User sender?;
    Repository repository?;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type PullRequestPayload record {
    # The action that was performed
    string action;
    # The pull request number
    int number;
    PullRequest pull_request;
    User assignee;
    # For edited events, the changes to the pull request
    record {} changes?;
    User requested_reviewer?;
    Label label?;
    Milestone milestone?;
    User sender;
    Repository repository;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type TeamAddPayload record {
    # The team that was granted access to the repository
    record { int id; string node_id; string url?; string html_url?; string name; string slug; string description?; string privacy?; string notification_setting?; string permission?; string members_url?; string repositories_url?; # The parent team, if this is a child team
        record { int id?; string node_id?; string name?; string slug?; string description?; string privacy?; string permission?; string members_url?; string repositories_url?; string html_url?;}  parent?;}  team;
    User sender?;
    Repository repository?;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type WorkflowJob record {
    int id;
    int run_id?;
    string run_url?;
    int run_attempt?;
    string node_id?;
    string head_sha?;
    string url?;
    string html_url?;
    string status;
    string conclusion?;
    string created_at?;
    string started_at?;
    string completed_at?;
    string name;
    record { string name?; string status?; string conclusion?; int number?; string started_at?; string completed_at?;} [] steps?;
    string check_run_url?;
    string[] labels?;
    int runner_id?;
    string runner_name?;
    int runner_group_id?;
    string runner_group_name?;
    string workflow_name?;
    string head_branch?;
};

public type Release record {
    int id;
    string node_id?;
    string url?;
    string html_url?;
    string assets_url?;
    string upload_url?;
    string tag_name;
    string name;
    string body?;
    boolean draft?;
    boolean prerelease?;
    string target_commitish?;
    User author?;
    record {}[] assets?;
    string created_at?;
    string published_at?;
};

public type CustomPropertyPayload record {
    string action;
    # Custom property defined on an organization
    record { string property_name; string value_type; string required?; # Default value (string or array of strings)
        anydata default_value?; string description?; string[] allowed_values?;}  definition;
    User sender?;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type PublicPayload record {
    User sender;
    Repository repository;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type MemberPayload record {
    string action;
    User member;
    # For edited events, the changes to the member's permissions
    record { record { string 'from?;}  old_permission?; record { string 'from?; string to?;}  permission?;}  changes?;
    User sender;
    Repository repository;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type MilestonePayload record {
    string action;
    Milestone milestone;
    # For edited events, the changes to the milestone
    record { record { string 'from?;}  description?; record { string 'from?;}  due_on?; record { string 'from?;}  title?;}  changes?;
    User sender;
    Repository repository;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type SecurityAdvisoryPayload record {
    string action;
    # The details of the global security advisory, including summary,
    # description, severity, and affected packages.
    record { string schema_version?; string ghsa_id; string cve_id?; string url?; string html_url?; string summary; string description?; string severity; record { string value?; string 'type?;} [] identifiers?; record { string url?;} [] references?; string published_at?; string updated_at?; string withdrawn_at?; record { record { string ecosystem?; string name?;}  package?; string severity?; string vulnerable_version_range?; record { string identifier?;}  first_patched_version?;} [] vulnerabilities?; record { string vector_string?; decimal score?;}  cvss?; record { string cwe_id?; string name?;} [] cwes?;}  security_advisory;
    Installation installation?;
};

public type CheckRunPayload record {
    string action;
    CheckRun check_run;
    # Present for requested_action events
    record { string identifier?;}  requested_action?;
    User sender?;
    Repository repository?;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type CommitCommentPayload record {
    string action;
    # The commit comment resource
    record { int id; string node_id?; string url?; string html_url?; string body; # The relative path of the file being commented on
        string path?; # The line index in the diff
        int position?; # The line of the blob the comment refers to
        int line?; string commit_id?; User user?; string created_at?; string updated_at?; string author_association?;}  comment;
    User sender?;
    Repository repository?;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type Commit record {
    # The commit SHA
    string id;
    string tree_id?;
    # Whether this commit is distinct from any that have been pushed before
    boolean 'distinct?;
    string message;
    string timestamp?;
    string url?;
    CommitAuthor author?;
    CommitAuthor committer?;
    # Files added in this commit
    string[] added?;
    # Files removed in this commit
    string[] removed?;
    # Files modified in this commit
    string[] modified?;
};

public type CheckRun record {
    int id;
    string name;
    string node_id?;
    string head_sha?;
    string external_id?;
    string url?;
    string html_url?;
    string details_url?;
    string status?;
    string conclusion?;
    string started_at?;
    string completed_at?;
    record { string title?; string summary?; string text?; int annotations_count?; string annotations_url?;}  output?;
    record { int id?;}  check_suite?;
    record {} app?;
    record {}[] pull_requests?;
};

public type MembershipPayload record {
    string action;
    User member;
    # The scope of the membership (currently always "team")
    string scope;
    Team team;
    User sender?;
    Organization organization?;
    Installation installation?;
    Enterprise enterprise?;
};

public type GenericDataType ForkPayload|WorkflowRunPayload|GollumPayload|ReleasePayload|SecretScanningAlertLocationPayload|DeploymentReviewPayload|PullRequest|SecretScanningScanPayload|IssueCommentPayload|DeploymentStatusPayload|OrganizationPayload|WebhookHeaders|RepositoryDispatchPayload|MergeGroupPayload|WorkflowJobPayload|OrgBlockPayload|DependabotAlertPayload|CustomPropertyValuesPayload|SecretScanningAlertPayload|PullRequestReviewThreadPayload|IssueComment|RegistryPackagePayload|CheckSuitePayload|DiscussionCommentPayload|Organization|RepositoryImportPayload|RepositoryPayload|StarPayload|WatchPayload|PackagePayload|WorkflowDispatchPayload|SponsorshipPayload|SubIssuesPayload|ProjectColumnPayload|Team|MarketplacePurchasePayload|PushPayload|Installation|BranchProtectionRulePayload|PullRequestReviewCommentPayload|PullRequestRef|'ProjectsV2ItemPayload|PingPayload|CreatePayload|Repository|PullRequestReviewComment|TeamPayload|Enterprise|ProjectPayload|InstallationTargetPayload|DeploymentStatus|InstallationRepositoriesPayload|Issue|Label|Deployment|BranchProtectionConfigurationPayload|RepositoryRulesetPayload|SecurityAndAnalysisPayload|CommitAuthor|DeployKeyPayload|IssueDependenciesPayload|RepositoryAdvisoryPayload|RepositoryVulnerabilityAlertPayload|Milestone|IssuesPayload|CodeScanningAlertPayload|PullRequestReviewPayload|'ProjectsV2Payload|PersonalAccessTokenRequestPayload|InstallationPayload|WorkflowRun|DiscussionPayload|CheckSuite|StatusPayload|'ProjectsV2StatusUpdatePayload|CommonPayload|Discussion|User|PullRequestReview|DeletePayload|MetaPayload|DeploymentPayload|DeploymentProtectionRulePayload|LabelPayload|GithubAppAuthorizationPayload|PageBuildPayload|ProjectCardPayload|PullRequestPayload|TeamAddPayload|WorkflowJob|Release|CustomPropertyPayload|PublicPayload|MemberPayload|MilestonePayload|SecurityAdvisoryPayload|CheckRunPayload|CommitCommentPayload|Commit|CheckRun|MembershipPayload;

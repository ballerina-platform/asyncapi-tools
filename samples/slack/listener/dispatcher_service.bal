import ballerina/http;
import ballerinax/asyncapi.native.handler;

service class DispatcherService {
    private map<GenericServiceType> services = {};
    private handler:NativeHandler nativeHandler = new ();

    isolated function addServiceRef(string serviceType, GenericServiceType genericService) returns error? {
        if (self.services.hasKey(serviceType)) {
            return error("Service of type " + serviceType + " has already been attached");
        }
        self.services[serviceType] = genericService;
    }

    isolated function removeServiceRef(string serviceType) returns error? {
        if (!self.services.hasKey(serviceType)) {
            return error("Cannot detach the service of type " + serviceType + ". Service has not been attached to the listener before");
        }
        _ = self.services.remove(serviceType);
    }

    // We are not using the (@http:payload GenericEventWrapperEvent g) notation because of a bug in Ballerina.
    // Issue: https://github.com/ballerina-platform/ballerina-lang/issues/32859
    resource function post events(http:Caller caller, http:Request request) returns error? {
        json payload = check request.getJsonPayload();
        GenericDataType genericDataType = check payload.cloneWithType(GenericDataType);
        check self.matchRemoteFunc(genericDataType);
    }

    private function matchRemoteFunc(GenericDataType genericDataType) returns error? {
        match genericDataType.event.'type {
            "app_mention" => {
                check self.executeRemoteFunc(genericDataType, "app_mention", "AppService", "onAppMention");
            }
            "app_rate_limited" => {
                check self.executeRemoteFunc(genericDataType, "app_rate_limited", "AppService", "onAppRateLimited");
            }
            "app_uninstalled" => {
                check self.executeRemoteFunc(genericDataType, "app_uninstalled", "AppService", "onAppUninstalled");
            }
            "channel_archive" => {
                check self.executeRemoteFunc(genericDataType, "channel_archive", "ChannelService", "onChannelArchive");
            }
            "channel_created" => {
                check self.executeRemoteFunc(genericDataType, "channel_created", "ChannelService", "onChannelCreated");
            }
            "channel_deleted" => {
                check self.executeRemoteFunc(genericDataType, "channel_deleted", "ChannelService", "onChannelDeleted");
            }
            "channel_history_changed" => {
                check self.executeRemoteFunc(genericDataType, "channel_history_changed", "ChannelService", "onChannelHistoryChanged");
            }
            "channel_left" => {
                check self.executeRemoteFunc(genericDataType, "channel_left", "ChannelService", "onChannelLeft");
            }
            "channel_rename" => {
                check self.executeRemoteFunc(genericDataType, "channel_rename", "ChannelService", "onChannelRename");
            }
            "channel_unarchive" => {
                check self.executeRemoteFunc(genericDataType, "channel_unarchive", "ChannelService", "onChannelUnarchive");
            }
            "dnd_updated" => {
                check self.executeRemoteFunc(genericDataType, "dnd_updated", "DndService", "onDndUpdated");
            }
            "dnd_updated_user" => {
                check self.executeRemoteFunc(genericDataType, "dnd_updated_user", "DndService", "onDndUpdatedUser");
            }
            "email_domain_changed" => {
                check self.executeRemoteFunc(genericDataType, "email_domain_changed", "EmailDomainChangedService", "onEmailDomainChanged");
            }
            "emoji_changed" => {
                check self.executeRemoteFunc(genericDataType, "emoji_changed", "EmojiChangedService", "onEmojiChanged");
            }
            "file_change" => {
                check self.executeRemoteFunc(genericDataType, "file_change", "FileService", "onFileChange");
            }
            "file_comment_added" => {
                check self.executeRemoteFunc(genericDataType, "file_comment_added", "FileService", "onFileCommentAdded");
            }
            "file_comment_deleted" => {
                check self.executeRemoteFunc(genericDataType, "file_comment_deleted", "FileService", "onFileCommentDeleted");
            }
            "file_comment_edited" => {
                check self.executeRemoteFunc(genericDataType, "file_comment_edited", "FileService", "onFileCommentEdited");
            }
            "file_created" => {
                check self.executeRemoteFunc(genericDataType, "file_created", "FileService", "onFileCreated");
            }
            "file_deleted" => {
                check self.executeRemoteFunc(genericDataType, "file_deleted", "FileService", "onFileDeleted");
            }
            "file_public" => {
                check self.executeRemoteFunc(genericDataType, "file_public", "FileService", "onFilePublic");
            }
            "file_shared" => {
                check self.executeRemoteFunc(genericDataType, "file_shared", "FileService", "onFileShared");
            }
            "file_unshared" => {
                check self.executeRemoteFunc(genericDataType, "file_unshared", "FileService", "onFileUnshared");
            }
            "grid_migration_finished" => {
                check self.executeRemoteFunc(genericDataType, "grid_migration_finished", "GridMigrationService", "onGridMigrationFinished");
            }
            "grid_migration_started" => {
                check self.executeRemoteFunc(genericDataType, "grid_migration_started", "GridMigrationService", "onGridMigrationStarted");
            }
            "group_archive" => {
                check self.executeRemoteFunc(genericDataType, "group_archive", "GroupService", "onGroupArchive");
            }
            "group_close" => {
                check self.executeRemoteFunc(genericDataType, "group_close", "GroupService", "onGroupClose");
            }
            "group_history_changed" => {
                check self.executeRemoteFunc(genericDataType, "group_history_changed", "GroupService", "onGroupHistoryChanged");
            }
            "group_left" => {
                check self.executeRemoteFunc(genericDataType, "group_left", "GroupService", "onGroupLeft");
            }
            "group_open" => {
                check self.executeRemoteFunc(genericDataType, "group_open", "GroupService", "onGroupOpen");
            }
            "group_rename" => {
                check self.executeRemoteFunc(genericDataType, "group_rename", "GroupService", "onGroupRename");
            }
            "group_unarchive" => {
                check self.executeRemoteFunc(genericDataType, "group_unarchive", "GroupService", "onGroupUnarchive");
            }
            "im_close" => {
                check self.executeRemoteFunc(genericDataType, "im_close", "ImService", "onImClose");
            }
            "im_created" => {
                check self.executeRemoteFunc(genericDataType, "im_created", "ImService", "onImCreated");
            }
            "im_history_changed" => {
                check self.executeRemoteFunc(genericDataType, "im_history_changed", "ImService", "onImHistoryChanged");
            }
            "im_open" => {
                check self.executeRemoteFunc(genericDataType, "im_open", "ImService", "onImOpen");
            }
            "link_shared" => {
                check self.executeRemoteFunc(genericDataType, "link_shared", "LinkSharedService", "onLinkShared");
            }
            "member_joined_channel" => {
                check self.executeRemoteFunc(genericDataType, "member_joined_channel", "MemberService", "onMemberJoinedChannel");
            }
            "member_left_channel" => {
                check self.executeRemoteFunc(genericDataType, "member_left_channel", "MemberService", "onMemberLeftChannel");
            }
            "message" => {
                check self.executeRemoteFunc(genericDataType, "message", "MessageService", "onMessage");
            }
            "message.app_home" => {
                check self.executeRemoteFunc(genericDataType, "message.app_home", "MessageService", "onMessageAppHome");
            }
            "message.channels" => {
                check self.executeRemoteFunc(genericDataType, "message.channels", "MessageService", "onMessageChannels");
            }
            "message.groups" => {
                check self.executeRemoteFunc(genericDataType, "message.groups", "MessageService", "onMessageGroups");
            }
            "message.im" => {
                check self.executeRemoteFunc(genericDataType, "message.im", "MessageService", "onMessageIm");
            }
            "message.mpim" => {
                check self.executeRemoteFunc(genericDataType, "message.mpim", "MessageService", "onMessageMpim");
            }
            "pin_added" => {
                check self.executeRemoteFunc(genericDataType, "pin_added", "PinService", "onPinAdded");
            }
            "pin_removed" => {
                check self.executeRemoteFunc(genericDataType, "pin_removed", "PinService", "onPinRemoved");
            }
            "reaction_added" => {
                check self.executeRemoteFunc(genericDataType, "reaction_added", "ReactionService", "onReactionAdded");
            }
            "reaction_removed" => {
                check self.executeRemoteFunc(genericDataType, "reaction_removed", "ReactionService", "onReactionRemoved");
            }
            "resources_added" => {
                check self.executeRemoteFunc(genericDataType, "resources_added", "ResourcesService", "onResourcesAdded");
            }
            "resources_removed" => {
                check self.executeRemoteFunc(genericDataType, "resources_removed", "ResourcesService", "onResourcesRemoved");
            }
            "scope_denied" => {
                check self.executeRemoteFunc(genericDataType, "scope_denied", "ScopeService", "onScopeDenied");
            }
            "scope_granted" => {
                check self.executeRemoteFunc(genericDataType, "scope_granted", "ScopeService", "onScopeGranted");
            }
            "star_added" => {
                check self.executeRemoteFunc(genericDataType, "star_added", "StarService", "onStarAdded");
            }
            "star_removed" => {
                check self.executeRemoteFunc(genericDataType, "star_removed", "StarService", "onStarRemoved");
            }
            "subteam_created" => {
                check self.executeRemoteFunc(genericDataType, "subteam_created", "SubteamService", "onSubteamCreated");
            }
            "subteam_members_changed" => {
                check self.executeRemoteFunc(genericDataType, "subteam_members_changed", "SubteamService", "onSubteamMembersChanged");
            }
            "subteam_self_added" => {
                check self.executeRemoteFunc(genericDataType, "subteam_self_added", "SubteamService", "onSubteamSelfAdded");
            }
            "subteam_self_removed" => {
                check self.executeRemoteFunc(genericDataType, "subteam_self_removed", "SubteamService", "onSubteamSelfRemoved");
            }
            "subteam_updated" => {
                check self.executeRemoteFunc(genericDataType, "subteam_updated", "SubteamService", "onSubteamUpdated");
            }
            "team_domain_change" => {
                check self.executeRemoteFunc(genericDataType, "team_domain_change", "TeamService", "onTeamDomainChange");
            }
            "team_join" => {
                check self.executeRemoteFunc(genericDataType, "team_join", "TeamService", "onTeamJoin");
            }
            "team_rename" => {
                check self.executeRemoteFunc(genericDataType, "team_rename", "TeamService", "onTeamRename");
            }
            "tokens_revoked" => {
                check self.executeRemoteFunc(genericDataType, "tokens_revoked", "TokensRevokedService", "onTokensRevoked");
            }
            "url_verification" => {
                check self.executeRemoteFunc(genericDataType, "url_verification", "UrlVerificationService", "onUrlVerification");
            }
            "user_change" => {
                check self.executeRemoteFunc(genericDataType, "user_change", "UserChangeService", "onUserChange");
            }
        }
    }

    private function executeRemoteFunc(GenericDataType genericEvent, string eventName, string serviceTypeStr, string eventFunction) returns error? {
        GenericServiceType? genericService = self.services[serviceTypeStr];
        if genericService is GenericServiceType {
            check self.nativeHandler.invokeRemoteFunction(genericEvent, eventName, eventFunction, genericService);
        }
    }
}

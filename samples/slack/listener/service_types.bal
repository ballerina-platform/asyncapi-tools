public type AppService service object {
    remote function onAppMention(GenericEventWrapper event) returns error?;
    remote function onAppRateLimited(GenericEventWrapper event) returns error?;
    remote function onAppUninstalled(GenericEventWrapper event) returns error?;
};

public type ChannelService service object {
    remote function onChannelArchive(GenericEventWrapper event) returns error?;
    remote function onChannelCreated(GenericEventWrapper event) returns error?;
    remote function onChannelDeleted(GenericEventWrapper event) returns error?;
    remote function onChannelHistoryChanged(GenericEventWrapper event) returns error?;
    remote function onChannelLeft(GenericEventWrapper event) returns error?;
    remote function onChannelRename(GenericEventWrapper event) returns error?;
    remote function onChannelUnarchive(GenericEventWrapper event) returns error?;
};

public type DndService service object {
    remote function onDndUpdated(GenericEventWrapper event) returns error?;
    remote function onDndUpdatedUser(GenericEventWrapper event) returns error?;
};

public type EmailDomainChangedService service object {
    remote function onEmailDomainChanged(GenericEventWrapper event) returns error?;
};

public type EmojiChangedService service object {
    remote function onEmojiChanged(GenericEventWrapper event) returns error?;
};

public type FileService service object {
    remote function onFileChange(GenericEventWrapper event) returns error?;
    remote function onFileCommentAdded(GenericEventWrapper event) returns error?;
    remote function onFileCommentDeleted(GenericEventWrapper event) returns error?;
    remote function onFileCommentEdited(GenericEventWrapper event) returns error?;
    remote function onFileCreated(GenericEventWrapper event) returns error?;
    remote function onFileDeleted(GenericEventWrapper event) returns error?;
    remote function onFilePublic(GenericEventWrapper event) returns error?;
    remote function onFileShared(GenericEventWrapper event) returns error?;
    remote function onFileUnshared(GenericEventWrapper event) returns error?;
};

public type GridMigrationService service object {
    remote function onGridMigrationFinished(GenericEventWrapper event) returns error?;
    remote function onGridMigrationStarted(GenericEventWrapper event) returns error?;
};

public type GroupService service object {
    remote function onGroupArchive(GenericEventWrapper event) returns error?;
    remote function onGroupClose(GenericEventWrapper event) returns error?;
    remote function onGroupHistoryChanged(GenericEventWrapper event) returns error?;
    remote function onGroupLeft(GenericEventWrapper event) returns error?;
    remote function onGroupOpen(GenericEventWrapper event) returns error?;
    remote function onGroupRename(GenericEventWrapper event) returns error?;
    remote function onGroupUnarchive(GenericEventWrapper event) returns error?;
};

public type ImService service object {
    remote function onImClose(GenericEventWrapper event) returns error?;
    remote function onImCreated(GenericEventWrapper event) returns error?;
    remote function onImHistoryChanged(GenericEventWrapper event) returns error?;
    remote function onImOpen(GenericEventWrapper event) returns error?;
};

public type LinkSharedService service object {
    remote function onLinkShared(GenericEventWrapper event) returns error?;
};

public type MemberService service object {
    remote function onMemberJoinedChannel(GenericEventWrapper event) returns error?;
    remote function onMemberLeftChannel(GenericEventWrapper event) returns error?;
};

public type MessageService service object {
    remote function onMessage(Message event) returns error?;
    remote function onMessageAppHome(GenericEventWrapper event) returns error?;
    remote function onMessageChannels(GenericEventWrapper event) returns error?;
    remote function onMessageGroups(GenericEventWrapper event) returns error?;
    remote function onMessageIm(GenericEventWrapper event) returns error?;
    remote function onMessageMpim(GenericEventWrapper event) returns error?;
};

public type PinService service object {
    remote function onPinAdded(GenericEventWrapper event) returns error?;
    remote function onPinRemoved(GenericEventWrapper event) returns error?;
};

public type ReactionService service object {
    remote function onReactionAdded(GenericEventWrapper event) returns error?;
    remote function onReactionRemoved(GenericEventWrapper event) returns error?;
};

public type ResourcesService service object {
    remote function onResourcesAdded(GenericEventWrapper event) returns error?;
    remote function onResourcesRemoved(GenericEventWrapper event) returns error?;
};

public type ScopeService service object {
    remote function onScopeDenied(GenericEventWrapper event) returns error?;
    remote function onScopeGranted(GenericEventWrapper event) returns error?;
};

public type StarService service object {
    remote function onStarAdded(GenericEventWrapper event) returns error?;
    remote function onStarRemoved(GenericEventWrapper event) returns error?;
};

public type SubteamService service object {
    remote function onSubteamCreated(GenericEventWrapper event) returns error?;
    remote function onSubteamMembersChanged(GenericEventWrapper event) returns error?;
    remote function onSubteamSelfAdded(GenericEventWrapper event) returns error?;
    remote function onSubteamSelfRemoved(GenericEventWrapper event) returns error?;
    remote function onSubteamUpdated(GenericEventWrapper event) returns error?;
};

public type TeamService service object {
    remote function onTeamDomainChange(GenericEventWrapper event) returns error?;
    remote function onTeamJoin(GenericEventWrapper event) returns error?;
    remote function onTeamRename(GenericEventWrapper event) returns error?;
};

public type TokensRevokedService service object {
    remote function onTokensRevoked(GenericEventWrapper event) returns error?;
};

public type UrlVerificationService service object {
    remote function onUrlVerification(GenericEventWrapper event) returns error?;
};

public type UserChangeService service object {
    remote function onUserChange(GenericEventWrapper event) returns error?;
};

public type GenericServiceType AppService|ChannelService|DndService|EmailDomainChangedService|EmojiChangedService|FileService|GridMigrationService|GroupService|ImService|LinkSharedService|MemberService|MessageService|PinService|ReactionService|ResourcesService|ScopeService|StarService|SubteamService|TeamService|TokensRevokedService|UrlVerificationService|UserChangeService;

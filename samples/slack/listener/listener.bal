import ballerina/http;

@display {
    label: "",
    'type: "trigger",
    description: ""
}
public class Listener {
    private http:Listener httpListener;
    private DispatcherService dispatcherService;

    public function init(int|http:Listener listenOn = 8090) returns error? {
        if listenOn is http:Listener {
            self.httpListener = listenOn;
        } else {
            self.httpListener = check new (listenOn);
        }
        self.dispatcherService = new DispatcherService();
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
        if serviceRef is AppService {
            return "AppService";
        } else if serviceRef is ChannelService {
            return "ChannelService";
        } else if serviceRef is DndService {
            return "DndService";
        } else if serviceRef is EmailDomainChangedService {
            return "EmailDomainChangedService";
        } else if serviceRef is EmojiChangedService {
            return "EmojiChangedService";
        } else if serviceRef is FileService {
            return "FileService";
        } else if serviceRef is GridMigrationService {
            return "GridMigrationService";
        } else if serviceRef is GroupService {
            return "GroupService";
        } else if serviceRef is ImService {
            return "ImService";
        } else if serviceRef is LinkSharedService {
            return "LinkSharedService";
        } else if serviceRef is MemberService {
            return "MemberService";
        } else if serviceRef is MessageService {
            return "MessageService";
        } else if serviceRef is PinService {
            return "PinService";
        } else if serviceRef is ReactionService {
            return "ReactionService";
        } else if serviceRef is ResourcesService {
            return "ResourcesService";
        } else if serviceRef is ScopeService {
            return "ScopeService";
        } else if serviceRef is StarService {
            return "StarService";
        } else if serviceRef is SubteamService {
            return "SubteamService";
        } else if serviceRef is TeamService {
            return "TeamService";
        } else if serviceRef is TokensRevokedService {
            return "TokensRevokedService";
        } else if serviceRef is UrlVerificationService {
            return "UrlVerificationService";
        } else {
            return "UserChangeService";
        }
    }
}

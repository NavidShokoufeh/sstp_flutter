//
//  PacketTunnelProvider.m
//  sstp_extension
//
//  Created by Navid Shokoufeh on 1403-08-01.
//

#import "PacketTunnelProvider.h"
#import <openconnect/openconnect.h>
#include <net/if.h>
#import <ExtParser/ExtParser.h>

@interface PacketTunnelProvider ()<OpenAdapterDelegate>

@end

@implementation PacketTunnelProvider{
    OpenAdapter *vpnAdapter;
    NWTCPConnection *_TCPConnection;
    NWUDPSession *_UDPSession;
}

+ (void)LOGRedirect {
    NSString *logFilePath = [NSString stringWithFormat:@"%@/Documents/%@", NSHomeDirectory(), @"xray.log"];
    [[NSFileManager defaultManager] removeItemAtURL:[NSURL fileURLWithPath:logFilePath] error:nil];
    [[NSFileManager defaultManager] createFileAtPath:logFilePath contents:nil attributes:nil];
    freopen([logFilePath cStringUsingEncoding:NSASCIIStringEncoding], "w+", stdout);
    freopen([logFilePath cStringUsingEncoding:NSASCIIStringEncoding], "w+", stderr);
}


-(int32_t)tunnelFd {
    char buf[IFNAMSIZ] = {0};
    for (int fd = 0; fd <= 1024; fd++) {
        socklen_t len = (socklen_t)(IFNAMSIZ);
        int rc = getsockopt(fd, 2 /* IGMP */, 2, buf, &len);
        if (rc == 0) {
            NSString *cc = [NSString stringWithUTF8String:buf];
            if ([cc hasPrefix:@"utun"]) {
                return fd;
            }
        }
    }
    int fd = (int)[[self.packetFlow valueForKey:@"socket.fileDescriptor"] integerValue];
    return fd;
}



-(void)startSSTPWithOptions:(NSDictionary *)v completionHandler:(void (^)(NSError *))completionHandler {
    

    SSSstp *p = [[SSSstp alloc] init];
    p.username = v[@"username"];
    p.password = v[@"password"];
    p.tls = YES;
    p.host = v[@"address"];
    p.port = [v[@"port"] intValue];
    p.allowInsecure = [v[@"allowInsecure"] boolValue];
    p.PAP_ENABLE = v[@"pap_enable"] ? [v[@"pap_enable"] boolValue] : NO;
    p.CHAP_ENABLE = v[@"chap_enable"] ? [v[@"chap_enable"] boolValue] : NO;
    p.MS_CHAP_ENABLE = v[@"mschap_enable"] ? [v[@"mschap_enable"] boolValue] : NO;
    p.MS2_CHAP_ENABLE = v[@"mschap2_enable"] ? [v[@"mschap2_enable"] boolValue] : YES;

    
    [[lwIP shared] setup:p];
    
    __weak PacketTunnelProvider *weakSelf = self;
    [[lwIP shared] SetlwSetupNetworkSettingsCallback:^(NEPacketTunnelNetworkSettings *setting) {
        [self setTunnelNetworkSettings:setting completionHandler:^(NSError * _Nullable e) {
            if (e) {
                return completionHandler(e);
            }
            __strong PacketTunnelProvider *strongSelf = weakSelf;
            [strongSelf readPackets];
            completionHandler(nil);
        }];
    }];
}


-(void)readPackets {
    [[lwIP shared] SetlwIPOutputCallback:^(NSData *ipPacket, int family) {
        [self.packetFlow writePackets:@[ipPacket] withProtocols:@[@(family)]];
    }];
    [self.packetFlow readPacketsWithCompletionHandler:^(NSArray<NSData *> * _Nonnull packets, NSArray<NSNumber *> * _Nonnull protocols) {
        for (NSInteger i = 0; i < packets.count; i ++) {
            if (protocols[i].intValue == AF_INET6) return;
            [[lwIP shared] WriteIPPacket:packets[i] family:protocols[i].intValue];
        }
        [self readPackets];
    }];
}


-(nullable NSDictionary *)parse:(NSString *)uri {
    NSURL *url = [NSURL URLWithString:uri];
    NSArray *nodes = [uri componentsSeparatedByString:@"#"];
    NSString *remark = nodes.count >= 2 ? nodes.lastObject : @"sstp";
    NSArray <NSString *>*parameters = [url.query componentsSeparatedByString:@"&"];
    NSString *username = url.user;
    NSString *password = url.password;
    if (password == nil) {
        NSArray *up = [[PDBase64 decode:username] componentsSeparatedByString:@":"];
        if (up.count != 2) {
            return nil;
        }
        username = up[0];
        password = up[1];
        
        username = [username stringByTrimmingCharactersInSet:NSCharacterSet.whitespaceAndNewlineCharacterSet];
        password = [password stringByTrimmingCharactersInSet:NSCharacterSet.whitespaceAndNewlineCharacterSet];
    }
    NSMutableDictionary *body = @{@"username":username, @"password":password, @"port":url.port, @"xx":url.scheme, @"address":url.host, @"remark":remark}.mutableCopy;
    [parameters enumerateObjectsUsingBlock:^(NSString * _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
        NSArray *t = [obj componentsSeparatedByString:@"="];
        if (t.count == 2) {
            body[t[0]] = t[1];
        }
    }];
    return body;
}

- (void)startTunnelWithOptions:(NSDictionary *)options completionHandler:(void (^)(NSError *))completionHandler {
    
    if (!options) {
        NETunnelProviderProtocol *protocolConfiguration = (NETunnelProviderProtocol *)self.protocolConfiguration;
        NSMutableDictionary *copy = protocolConfiguration.providerConfiguration.mutableCopy;
        options = copy[@"configuration"];
    }
    NSDictionary *ss;
    NSString *uri = options[@"uri"];
    BOOL global = [options[@"global"] boolValue];
    if (!global) {
        [NWRunLoop setup:PDVPNManager.mmdb];
    }
    else {
        [NWRunLoop setup:nil];
    }
    
    if ([uri hasPrefix:@"vless"] || [uri hasPrefix:@"vmess"]) {
        ss = [PDParser parseURI:uri];
    }
    else {
        ss = [self parse:uri];
    }
    NSString *scheme = ss[@"xx"];
    if ([scheme isEqualToString:@"sstp"]) {
        return [self startSSTPWithOptions:ss completionHandler:completionHandler];
    }
    
    completionHandler([NSError errorWithDomain:@"Not Found" code:404 userInfo:nil]);
    
}

- (void)stopTunnelWithReason:(NEProviderStopReason)reason completionHandler:(void (^)(void))completionHandler {
    NSLog(@"[Y] stop tunnel in orb:%@", @(reason));
    BOOL v = [[lwIP shared] close];
    if (v) {
        sleep(1);
        completionHandler();
    }
    else {
        completionHandler();
    }
}

- (void)handleAppMessage:(NSData *)messageData completionHandler:(void (^)(NSData *))completionHandler {
    NSDictionary *app = [NSJSONSerialization JSONObjectWithData:messageData options:NSJSONReadingMutableContainers error:nil];
    NSInteger type = [app[@"type"] integerValue];
    if (type == 2) {
        BOOL isGlobalMode = [app[@"global"] boolValue];
        if (isGlobalMode) {
            [NWRunLoop setup:nil];
        }
        else {
            [NWRunLoop setup:PDVPNManager.mmdb];
        }
    }
    else if (type == 5) {
        BOOL global = [app[@"global"] boolValue];
        if (global) {
            [NWRunLoop setup:nil];
        }
        else {
            [NWRunLoop setup:PDVPNManager.mmdb];
        }
    }
    
    completionHandler(messageData);
}

- (void)sleepWithCompletionHandler:(void (^)(void))completionHandler {
    completionHandler();
}

- (void)wake {
}

- (void)openVPNAdapter:(NSError *)error {
    NSLog(@"[Y] openVPNAdapter:%@", error);
}

- (void) setupUDPSession: (NEPacketTunnelNetworkSettings *) setting{
    self.reasserting = false;
    if(_TCPConnection != nil){
        self.reasserting = true;
        _TCPConnection = nil;
    }
    if(_UDPSession != nil){
        self.reasserting = true;
        _UDPSession = nil;
    }
    __weak PacketTunnelProvider *weakSelf  = self;
    [self setTunnelNetworkSettings:nil completionHandler:^(NSError * _Nullable error){
        if(error != nil){
            NSLog(@"[Y] Error set TunnelNetwork %@", error);
        }
        NSLog(@"[Y] setTunnelNetworkSettings %@", setting);
        __strong PacketTunnelProvider *strongSelf = weakSelf;
        [strongSelf setTunnelNetworkSettings:setting completionHandler:^(NSError * _Nullable error){
            if(error != nil){
                NSLog(@"[Y] %@", error);
            }
        }];
    }];
}
- (void)openVPNAdapter:(OpenAdapter * _Nullable)openVPNAdapter configureTunnelWithNetworkSettings:(NEPacketTunnelNetworkSettings * _Nullable)networkSettings completionHandler:(void (^ _Nullable)(NEPacketTunnelFlow *_Nullable))completionHandler {
    [self setupUDPSession:networkSettings];
    completionHandler(self.packetFlow);
}

- (void)openVPNAdapter:(OpenAdapter * _Nullable)openVPNAdapter handleError:(NSError * _Nullable)error {
    NSLog(@"[Y] ERROR IN HERE");
}

@end

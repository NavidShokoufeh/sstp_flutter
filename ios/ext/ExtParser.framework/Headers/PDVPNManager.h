//
//  YDVPNManager.h
//  VPNExtension
//
//  Created by NavidShokoufeh on 1403-08-01.
//

#import <Foundation/Foundation.h>
#import <NetworkExtension/NetworkExtension.h>
#import <ExtParser/ExtParser.h>

NS_ASSUME_NONNULL_BEGIN

typedef void(^YDProviderManagerCompletion)(NETunnelProviderManager *_Nullable manager);
typedef void(^YDPingResponse)(NSString *url, long long rtt);
typedef void(^YDGetStatisticsResponse)(int64_t downloadlink, int64_t uploadlink, int64_t mdownloadlink, int64_t muploadlink);
typedef void(^YDDownloadProgress)(int64_t downloadSpeed, BOOL isDone);
typedef void(^YDUploadProgress)(int64_t uploadSpeed, BOOL isDone);

@class YDItemInfo;

typedef enum : NSUInteger {
    YDVPNStatusIDLE = 0,
    YDVPNStatusConnecting,
    YDVPNStatusConnected,
    YDVPNStatusDisconnecting,
    YDVPNStatusDisconnected
} YDVPNStatus;


@protocol YDStorage <NSObject>

- (BOOL)setObject:(nullable NSObject<NSCoding> *)object forKey:(NSString *)key;

- (BOOL)setString:(NSString *)value forKey:(NSString *)key;

- (nullable id)getObjectOfClass:(Class)cls forKey:(NSString *)key;

- (nullable NSString *)getStringForKey:(NSString *)key;

- (void)removeValueForKey:(NSString *)key;

@end



@interface PDVPNManager : NSObject

+(void)setVPNServerAddress:(NSString *)address;

+(void)setVPNLocalizedDescription:(NSString *)description;

+(void)setGroupID:(NSString *)groupId;

+(instancetype)sharedManager;

-(void)setupVPNManager;


@property (nonatomic, strong, readonly, class, nullable)NSString *mmdb;

@property (nonatomic)BOOL udpSocks;

@property (nonatomic)BOOL shareable;

@property (nonatomic)BOOL pingUseTcp;

@property (nonatomic, strong)id<YDStorage> storage;

@property (nonatomic, readonly)YDVPNStatus status;

@property (nonatomic, strong, readonly)NSString *connectedURL;

@property (nonatomic, strong, readonly)NSDate *connectedDate;

@property (nonatomic)int engine;

@property (nonatomic)BOOL isGlobalMode;

-(void)connect:(NSString *)url;

-(BOOL)connectUsing:(NSDictionary *)server;

-(void)disconnect;

-(void)changeURL:(NSString *)url;

-(NSArray <NSString *> *)GetDNS;

-(void)deleteDnsServer:(NSString *)dns;

-(void)addDnsServer:(NSString *)dns;

-(void)ping:(NSArray <NSString *> *)ips response:(YDPingResponse)response;

-(void)tcpping:(NSArray <NSString *> *)ips response:(YDPingResponse)response;

-(void)addProtocol:(NSString *)protocol;

-(void)addProtocol:(NSString *)protocol name:(NSString *)name;

-(void)deleteProtocol:(NSString *)protoccol name:(NSString *)name;

-(void)deleteName:(NSString *)name;

-(NSArray <NSString *> *)allProtocols;

-(NSArray <NSString *> *)allProtocols:(NSString *)name;

-(NSArray <NSString *> *)allSubscriptions;

-(void)echo;


-(void)getStatistics:(YDGetStatisticsResponse) response;

-(void)setRouterConfiguration:(nullable NSArray <NSDictionary *> *)router;

-(void)download:(YDDownloadProgress)progress;

-(void)upload:(YDUploadProgress)progress;

@end

@interface PDVPNManager (Extension)

-(void)ping:(NSArray *)ips type:(int)type;

-(void)setupExtenstionApplication;

-(NSArray <NSDictionary *> *)getRouterConfiguration;
@end


NS_ASSUME_NONNULL_END

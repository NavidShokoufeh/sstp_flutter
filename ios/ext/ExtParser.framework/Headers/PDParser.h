//
//  xVPNProtocolParser.h
//  xVPN
//
//  Created by NavidShokoufeh on 1403-08-01.
//

#import <Foundation/Foundation.h>

typedef enum : NSUInteger {
    xVPNProtocolUnknown = 0,
    xVPNProtocolVmess,
    xVPNProtocolVless,
    xVPNProtocolTrojan,
    xVPNProtocolSS,
    xVPNProtocolSocks,
    xVPNProtocolHttp,
    xVPNProtocolSSTP,
} xVPNProtocol;


NS_ASSUME_NONNULL_BEGIN

@interface PDParser : NSObject

+ (void)setHttpProxyPort:(uint16_t)port;

+ (uint16_t)HttpProxyPort;

+ (uint16_t)SocksProxyPort;

+ (void)setLogLevel:(NSString *)level;

+ (void)setGlobalProxyEnable:(BOOL)enable;

+ (void)setDirectDomainList:(NSArray *)list;

+ (void)setProxyDomainList:(NSArray *)list;

+ (void)setBlockDomainList:(NSArray *)list;

+ (void)setDirectIPList:(NSArray *)list;

+ (void)setProxyIPList:(NSArray *)list;

+ (void)setBlockIPList:(NSArray *)list;

+ (NSDictionary *)parseURI:(NSString *)uri;

+ (NSArray *)GetRules;

+ (NSDictionary *)GetHttpInbound;

+ (NSDictionary *)GetStatsPolicy;

+ (NSDictionary *)GetShareableProxy;

@end

NS_ASSUME_NONNULL_END

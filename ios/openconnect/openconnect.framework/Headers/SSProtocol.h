//
//  SSProtocol.h
//  xsocks
//
//  Created by NavidShokoufeh on 1403-08-01.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

typedef enum : NSUInteger {
    SSClientType_DIRECT = 0,
    SSClientType_HTTP,
    SSClientType_SOCKS,
    SSClientType_VMESS,
    SSClientType_VLESS,
    SSClientType_TROJAN,
    SSClientType_SHADOWSOCKS,
    SSClientType_SSTP,
    SSClientType_SHADOWSOCKSR,
} SSClientType;

typedef enum : NSUInteger {
    SSTransportTypeTCP = 0,
    SSTransportTypeWebSocket,
    SSTransportTypeGRPC,
    SSTransportTypeQUIC,
    SSTransportTypeKCP
} SSTransportType;


typedef enum : UInt8 {
    SSSecurityType_UNKNOWN              = 0,
    SSSecurityType_LEGACY               = 1,
    SSSecurityType_AUTO                 = 2,
    SSSecurityType_AES_128_GCM           = 3,
    SSSecurityType_CHACHA20_POLY1305    = 4,
    SSSecurityType_NONE                 = 5,
    SSSecurityType_ZERO                 = 6,
    
    SSSecurityType_SS_NONE                 = 100,
    SSSecurityType_SS_AES_128_GCM          = 101,
    SSSecurityType_SS_AES_256_GCM          = 102,
    SSSecurityType_SS_CHACHA20_POLY1305    = 103,
    SSSecurityType_SS_XCHACHA20_POLY1305   = 104,
} SSSecurityType;


@interface SSProtocol : NSObject

@property (nonatomic)int port;

@property (nonatomic, strong)NSString *host;

@property (nonatomic, strong)NSString *query;

@property (nonatomic, strong, nullable)NSString *domain;

@property (nonatomic)BOOL tls;

@property (nonatomic)BOOL allowInsecure;

@property (nonatomic)SSTransportType transport;

@property (nonatomic, readonly)SSClientType protocol;

@property (nonatomic)SSSecurityType security;

@end

@interface SSVMess : SSProtocol

@property (nonatomic, strong)NSString *uuid;

@property (nonatomic, readonly)uint8_t *userId;

@property (nonatomic)NSInteger alterId;

@property (nonatomic, strong, readonly)NSArray <NSData *> *alterIDs;

@end



@interface SSSstp : SSProtocol

@property (nonatomic, strong, nullable)NSString *username;

@property (nonatomic, strong, nullable)NSString *password;

@property (nonatomic)BOOL PAP_ENABLE;

@property (nonatomic)BOOL CHAP_ENABLE;

@property (nonatomic)BOOL MS_CHAP_ENABLE;

@property (nonatomic)BOOL MS2_CHAP_ENABLE;
@end

@interface SSVLess : SSProtocol

@property (nonatomic, strong)NSString *uuid;

@property (nonatomic, strong)NSString *encryption;

@property (nonatomic, strong)NSString *flow;
@end

NS_ASSUME_NONNULL_END

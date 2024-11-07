//
//  SSClient.h
//  xsocks
//
//  Created by NavidShokoufeh on 1403-08-01.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

typedef enum : NSUInteger {
    SSClientStateIDLE = 0,
    SSClientStateConnecting,
    SSClientStateConnected,
    SSClientStateDisconnected,
} SSClientState;

typedef enum : NSUInteger {
    SSProtocolTypeTCP,
    SSProtocolTypeUDP,
    SSProtocolTypeICMP,
} SSProtocolType;

@class SSTCPClient;
@class SSProtocol;

@protocol SSClientDelegate <NSObject>

-(void)tcpClient:(SSTCPClient *)client didReceiveMessage:(uint8_t *)message length:(int)length;

-(void)tcpClient:(SSTCPClient *)client didChangeState:(SSClientState)state;
@end


@interface SSTCPClient : NSObject

@property (nonatomic, class, readonly)long long refCount;

@property (nonatomic, strong, readonly)NSString *target;

@property (nonatomic, readonly)int port;

@property (nonatomic, readonly)SSProtocolType proto;

@property (nonatomic, strong, readonly, nullable)SSProtocol *protocol;

@property (nonatomic, strong)NSString *tag;

@property (nonatomic)SSClientState sstate;

@property (nonatomic, weak)id<SSClientDelegate> delegate;

-(instancetype)initWithTarget:(NSString *)target port:(int)port proto:(SSProtocolType)proto;

-(instancetype)initWithTarget:(NSString *)target port:(int)port proto:(SSProtocolType)proto family:(int)family;

-(BOOL)sendMessage:(uint8_t *)message length:(int)length;

-(void)getCertificate:(uint8_t *)certificate length:(int *)length evp:(int)evp;

-(void)close;

-(void)connect;

-(void)checkTimeout;
@end

NS_ASSUME_NONNULL_END

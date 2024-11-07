//
//  SSSSTPClient.h
//  xsocks
//
//  Created by badwin on 2023/12/29.NavidShokoufeh on 1403-08-01.
//

#import <Foundation/Foundation.h>
#import <openconnect/openconnect.h>
#import <openconnect/SSTCPClient.h>

NS_ASSUME_NONNULL_BEGIN


@class SSSSTPClient;

typedef enum : NSUInteger {
    SSTPPPPState_OPEN,
    SSTPPPPState_ESTABLISHED,
    SSTPPPPState_CLOSE,
} SSTPPPPState;

@protocol SSSSTPClientDelegate <SSClientDelegate>

-(void)tcpClient:(SSTCPClient *)client didChangeSSTPState:(SSTPPPPState)state;

-(void)tcpClient:(SSTCPClient *)client didReceiveIPPacket:(uint8_t *)ipPacket length:(int)length;

@end


@interface SSSSTPClient : SSTCPClient

-(void)echo;

@end

NS_ASSUME_NONNULL_END

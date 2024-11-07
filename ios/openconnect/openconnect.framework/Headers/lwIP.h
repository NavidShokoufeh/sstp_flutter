//
//  lwIP.h
//  lwIP
//
//  Created by NavidShokoufeh on 1403-08-01.
//

#import <Foundation/Foundation.h>


//#include <mach/mach_time.h>
//#include <stdio.h>

@class SSProtocol;
@class NEPacketTunnelNetworkSettings;

typedef void(^lwSetupNetworkSettingsCallback)(NEPacketTunnelNetworkSettings *setting);

typedef void(^lwIPOutputCallback)(NSData *ipPackets, int family);

@interface lwIP : NSObject

+(instancetype)shared;

@property (nonatomic, readonly, strong)dispatch_queue_t worker;

+(void)setup:(NSString *)geoip;

-(void)setup:(SSProtocol *)protcol;

-(void)WriteIPPacket:(NSData *)packet family:(int)family;

-(void)SetlwIPOutputCallback:(lwIPOutputCallback)callback;

-(void)SetlwSetupNetworkSettingsCallback:(lwSetupNetworkSettingsCallback)callback;

-(BOOL)InQueue;

+(BOOL)InQueue;

-(BOOL)close;

@end

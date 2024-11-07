//
//  NWRunLoop.h
//  xsocks
//
//  Created by NavidShokoufeh on 1403-08-01.
//

#import <Foundation/Foundation.h>
#import <openconnect/SSProtocol.h>

NS_ASSUME_NONNULL_BEGIN

@interface NWRunLoop : NSObject
+(instancetype)currentRunLoop;

@property (nonatomic, strong)dispatch_queue_t worker;

@property (nonatomic, strong, readonly)SSProtocol *protocol;

+(void)setup:(nullable NSString *)geoip;

-(void)setup:(SSProtocol *)protocol;

-(BOOL)lookup:(char *)ip;

@end

NS_ASSUME_NONNULL_END

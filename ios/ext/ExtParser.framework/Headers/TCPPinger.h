//
//  MMTCPPinger.h
//  TCPPing
//
//  Created by NavidShokoufeh on 1403-08-01.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN


@interface TCPPinger : NSObject


- (int)ping:(NSString *)node port:(int)port count:(int)count;


+ (BOOL)isValidIP:(NSString *)ipStr;


+ (NSString *)LookupIP:(NSString *)domain;

@end

NS_ASSUME_NONNULL_END

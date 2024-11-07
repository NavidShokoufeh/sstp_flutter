//
//  PDBase64.h
//  ExtParser
//
//  Created by NavidShokoufeh on 1403-08-01.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface PDBase64 : NSObject
+(NSString *)decode:(NSString *)base64EncodedString;
+(NSString *)encode:(NSString *)raw;
@end

NS_ASSUME_NONNULL_END

//
//  PDParserSSTP.h
//  ExtParser
//
//  Created by NavidShokoufeh on 1403-08-01.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface PDParserGeneral : NSObject

+(nullable NSDictionary *)parse:(NSString *)uri protocol:(NSString *)protocol;

+(nullable NSDictionary *)parse:(NSString *)url;

+(void)setLogLevel:(NSString *)level;
@end

NS_ASSUME_NONNULL_END

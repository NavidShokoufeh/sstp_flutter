//
//  CheckConnectData.m
//
//  Created by NavidShokoufeh on 1403-08-01.
//

#import "CheckConnectData.h"
#import "NodeModel.h"

@implementation CheckConnectData

+(NSDictionary*)checkConnectParmer:(NodeModel*)modal {
    if([modal.nodeName isEqualToString:@"SSTP"]){
        return @{@"type":@"sstp",
                 @"username":modal.username,
                 @"password":modal.password,
                 @"address":modal.server,
                 @"port":modal.port,
                 @"chap_enable":@(modal.CHAP),
                 @"mschap2_enable":@(modal.MSCHAP2),
                 @"pap_enable":@(modal.PAP),
                 @"allowInsecure":@(modal.TLS),
                };
    }
    return @{};
}

+(NodeModel*)getModalForDic:(NSDictionary*)selectDic {
    if([selectDic[@"type"] isEqualToString:@"sstp"]) {
        NodeModel * modal = [NodeModel new];
        modal.nodeName = @"SSTP";
        modal.username =selectDic[@"username"];
        modal.password =selectDic[@"password"];
        modal.server =selectDic[@"address"];
        modal.PAP =[selectDic[@"pap_enable"] boolValue];
        modal.CHAP =[selectDic[@"chap_enable"] boolValue];
        modal.MSCHAP2 =[selectDic[@"mschap2_enable"] boolValue];
        modal.port =selectDic[@"port"];
        modal.TLS =[selectDic[@"allowInsecure"] boolValue];
        return modal;

    }
    return NodeModel.new;
}

@end

//
//  NodeModel.m
//
//  Created by NavidShokoufeh on 1403-08-01.
//

#import "NodeModel.h"
#import "CheckConnectData.h"

@implementation NodeModel
static NodeModel * modal = nil;

+(instancetype)shareInstance {
    @synchronized (self) {
        if(!modal) {
            modal = [[self alloc] init];
            modal.selectNode = [NodeModel new];
        }
        return modal;
    }
}

-(NSString*)nodeName {
    if(_nodeName.length==0) {
        return @"SSTP";
    }
    return _nodeName;
}


-(NSString*)port {
    if(_port.length==0) {
        return @"443";
    }
    return _port;
}

-(NSString*)udpgw_port {
    if(_udpgw_port.length==0) {
        return @"7300";
    }
    return _udpgw_port;
}

//-(void)setSelectNode:(NodeModel *)selectNode {
//    [[NSUserDefaults standardUserDefaults] setObject:[CheckConnectData checkConnectParmer:selectNode] forKey:@"selectDic"];
//    [[NSUserDefaults standardUserDefaults] synchronize];
//}

//-(NodeModel*)selectNode {
//
////    if([[NSUserDefaults standardUserDefaults] objectForKey:@"selectDic"]) {
////        return [CheckConnectData getModalForDic:[[NSUserDefaults standardUserDefaults] objectForKey:@"selectDic"]];
////    }
//    return _selectNode;
//
//}

@end

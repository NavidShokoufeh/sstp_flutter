//
//  NodeModel.h
//
//  Created by 丽丽 on 2024/1/13.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface NodeModel : NSObject

@property (nonatomic,copy) NSString * nodeName;
@property (nonatomic,copy) NSString * server;
@property (nonatomic,copy) NSString * port;
@property (nonatomic,copy) NSString * password;
@property (nonatomic,copy) NSString * desc;
@property (nonatomic,copy) NSString * username;
@property (nonatomic,copy) NSString * udpgw;
@property (nonatomic,copy) NSString * udpgw_port;
@property (nonatomic,assign) BOOL TLS;
@property (nonatomic,assign) BOOL PAP;
@property (nonatomic,assign) BOOL CHAP;
@property (nonatomic,assign) BOOL MSCHAP2;

@property (nonatomic,assign) BOOL isSelected;
@property (nonatomic,strong) NodeModel * selectNode;

+(instancetype)shareInstance;

@end

NS_ASSUME_NONNULL_END

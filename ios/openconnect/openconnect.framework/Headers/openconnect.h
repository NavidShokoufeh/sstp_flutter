//
//  openconnect.h
//  openconnect
//
//  Created by NavidShokoufeh on 1403-08-01.
//

#import <Foundation/Foundation.h>

//! Project version number for openconnect.
FOUNDATION_EXPORT double openconnectVersionNumber;

//! Project version string for openconnect.
FOUNDATION_EXPORT const unsigned char openconnectVersionString[];

// In this header, you should import all the public headers of your framework using statements like #import <openconnect/PublicHeader.h>

#import <openconnect/OpenAdapter.h>
#import <openconnect/SSTCPClient.h>
#import <openconnect/SSSSTPClient.h>
#import <openconnect/NWRunLoop.h>
#import <openconnect/lwIP.h>

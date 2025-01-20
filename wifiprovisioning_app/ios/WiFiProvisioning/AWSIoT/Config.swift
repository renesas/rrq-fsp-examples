//
//  Config.swift
//  WiFiProvisioning
//
//  Created by livekim on 2021/06/07.
//
// ******************************************************************************
//*
//* Copyright (C) 2023 Renesas Electronics.
//* This computer program includes Confidential, Proprietary Information
//* of Renesas Electronics. All Rights Reserved.
//*
//*******************************************************************************//

import Foundation

class Config {
    
    class func getIdentityPoolId() -> String? {
        var identityPoolId: String?
        
        //[[for single Cognito pool id
        identityPoolId = "ap-northeast-2:b414964c-595d-4db5-aa3a-babe9dc24f96"
        //]]

        return identityPoolId
        
    }
    
}

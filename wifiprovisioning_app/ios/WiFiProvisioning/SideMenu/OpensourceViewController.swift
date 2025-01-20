//
//  OpensourceViewController.swift
//  WiFiProvisioning
//
//  Created by livekim on 2020/12/23.
//
// ******************************************************************************
//*
//* Copyright (C) 2023 Renesas Electronics.
//* This computer program includes Confidential, Proprietary Information
//* of Renesas Electronics. All Rights Reserved.
//*
//*******************************************************************************

import UIKit

class OpensourceViewController: UIViewController {
    
    @IBOutlet weak var labelOpenSource: UILabel!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        labelOpenSource.text = opensourceString
    }
    
    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        labelOpenSource.sizeToFit()
    }
    
    @IBAction func onBtnBack(_ sender: UIButton) {
        
        let mainVC = UIStoryboard(name: "First", bundle: nil).instantiateViewController(withIdentifier: "MainViewController")
        mainVC.modalPresentationStyle = .fullScreen
        present(mainVC, animated: true, completion: nil)
        
    }
    
    let opensourceString = """
    This application is Copyright(c) 2023 Renesas Electronics. All rights reserved.

    The following sets forth attribution notices for third party software that may be contained in this software.

    #CocoaAsyncSocket
    https://github.com/robbiehanson/CocoaAsyncSocket
    Copyright (c) 2017, Deusty, LLC. All rights reserved.
    Public Domain License

    #SwiftyJSON
    https://github.com/SwiftyJSON/SwiftyJSON
    Copyright (c) 2017 Ruoyu Fu
    MIT License

    #Alamofire
    https://github.com/Alamofire/Alamofire
    Copyright (c) 2014-2020 Alamofire Software Foundation (http://alamofire.org/)
    MIT License

    #MBProgressHUD
    https://github.com/jdg/MBProgressHUD
    Copyright © 2009-2020 Matej Bukovinski
    MIT License

    #SideMenu
    https://github.com/jonkykong/SideMenu
    Copyright (c) 2015 Jonathan Kent
    MIT License
    
    -----------------------------------------------------------------------------------------------------

    
    
    """
    
}

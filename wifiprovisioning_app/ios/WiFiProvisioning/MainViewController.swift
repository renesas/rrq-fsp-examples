//
//  MainViewController.swift
//  WiFiProvisioning
//
//  Created by livekim on 2021/01/15.
//
// ******************************************************************************
//*
//* Copyright (C) 2023 Renesas Electronics.
//* This computer program includes Confidential, Proprietary Information
//* of Renesas Electronics. All Rights Reserved.
//*
//*******************************************************************************

import UIKit
import Foundation

class MainViewController: UIViewController {
    let appDelegate = UIApplication.shared.delegate as! AppDelegate
    @IBOutlet weak var btnInfo: UIButton!
    @IBOutlet weak var btnDA16200: UIButton!
    @IBOutlet weak var btnDA16600: UIButton!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        appDelegate.mainVC = self
        self.btnDA16200.layer.cornerRadius = 10
        self.btnDA16200.layer.shadowColor = UIColor(red: 30/255, green: 24/255, blue: 148/255, alpha: 1).cgColor
        self.btnDA16200.layer.shadowOpacity = 1;
        self.btnDA16200.layer.shadowRadius = 1;
        self.btnDA16200.layer.shadowOffset = CGSize(width: 1, height: 4)
        //[[add in v2.4.12
        self.btnDA16200.titleLabel!.textAlignment = NSTextAlignment.center;
        //]]
        
        self.btnDA16600.layer.cornerRadius = 10
        self.btnDA16600.layer.shadowColor = UIColor(red: 30/255, green: 24/255, blue: 148/255, alpha: 1).cgColor
        self.btnDA16600.layer.shadowOpacity = 1;
        self.btnDA16600.layer.shadowRadius = 1;
        self.btnDA16600.layer.shadowOffset = CGSize(width: 1, height: 4)
        //[[add in v2.4.12
        self.btnDA16600.titleLabel!.textAlignment = NSTextAlignment.center;
        //]]
        
    }
    
    @IBAction func onBtnInfo(_ sender: UIButton) {
        
    }
    
    func gotoOpensourceView() {
        let opensourceVC = UIStoryboard(name: "First", bundle: nil).instantiateViewController(withIdentifier: "OpensourceViewController")
        opensourceVC.modalPresentationStyle = .fullScreen
        present(opensourceVC, animated: true, completion: nil)
        
    }
    
    func gotoAwsIotDoorView() {
        let awsIotDoorVC = UIStoryboard(name: "First", bundle: nil).instantiateViewController(withIdentifier: "AWSIoTDoorViewController")
        awsIotDoorVC.modalPresentationStyle = .fullScreen
        present(awsIotDoorVC, animated: true, completion: nil)
        
    }
    
    func gotoAwsIotSensorView() {
        let awsIotSensorVC = UIStoryboard(name: "First", bundle: nil).instantiateViewController(withIdentifier: "AWSIoTSensorViewController")
        awsIotSensorVC.modalPresentationStyle = .fullScreen
        present(awsIotSensorVC, animated: true, completion: nil)
        
    }
    
    func gotoAwsIotDeviceView() {
        let awsIotDeviceVC = UIStoryboard(name: "First", bundle: nil).instantiateViewController(withIdentifier: "AWSIoTDeviceViewController")
        awsIotDeviceVC.modalPresentationStyle = .fullScreen
        present(awsIotDeviceVC, animated: true, completion: nil)
        
    }
    
    func gotoAzureIotDoorView() {
        let azureIotDoorVC = UIStoryboard(name: "First", bundle: nil).instantiateViewController(withIdentifier: "AzureIoTDoorViewController")
        azureIotDoorVC.modalPresentationStyle = .fullScreen
        present(azureIotDoorVC, animated: true, completion: nil)
    }
    
    @IBAction func onBtnDA16200(_ sender: UIButton) {
        appDelegate.deviceName = "DA16200"
        let DA16200mainVC = UIStoryboard(name: "First", bundle: nil).instantiateViewController(withIdentifier: "DA16200MainViewController")
        DA16200mainVC.modalPresentationStyle = .fullScreen
        present(DA16200mainVC, animated: true, completion: nil)
    }
    
    
    @IBAction func onBtnDA16600(_ sender: UIButton) {
        appDelegate.deviceName = "DA16600"
        let DA16600mainVC = UIStoryboard(name: "First", bundle: nil).instantiateViewController(withIdentifier: "DA16600MainViewController")
        DA16600mainVC.modalPresentationStyle = .fullScreen
        present(DA16600mainVC, animated: true, completion: nil)
    }
}


// MARK: - SideMenu

import SideMenu

extension ViewController: SideMenuNavigationControllerDelegate {
    func sideMenuWillAppear(menu: SideMenuNavigationController, animated: Bool) {
        NSLog("\(#fileID):\(#line) >> sideMenuWillAppear")
    }
    func sideMenuDidAppear(menu: SideMenuNavigationController, animated: Bool) {
        NSLog("\(#fileID):\(#line) >> sideMenuDidAppear")
    }
    func sideMenuWillDisappear(menu: SideMenuNavigationController, animated: Bool) {
        NSLog("\(#fileID):\(#line) >> sideMenuWillDisappear")
    }
    func sideMenuDidDisappear(menu: SideMenuNavigationController, animated: Bool) {
        NSLog("\(#fileID):\(#line) >> sideMenuDidDisappear")
    }
}

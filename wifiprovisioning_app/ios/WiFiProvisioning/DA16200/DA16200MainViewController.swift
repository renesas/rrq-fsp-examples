//
//  CheckViewController.swift
//  WiFiProvisioning
//
//  Created by livekim on 2021/01/08.
//
// ******************************************************************************
//*
//* Copyright (C) 2023 Renesas Electronics.
//* This computer program includes Confidential, Proprietary Information
//* of Renesas Electronics. All Rights Reserved.
//*
//*******************************************************************************

import UIKit
import CoreLocation

class DA16200MainViewController: UIViewController, CLLocationManagerDelegate {
    
    let appDelegate = UIApplication.shared.delegate as! AppDelegate
    
    var locationManager: CLLocationManager!
        
    
     @IBOutlet weak var btnNext: UIButton!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        appDelegate.DA16200mainVC = self
        
        self.locationManager = CLLocationManager()
        self.locationManager.requestWhenInUseAuthorization()
        self.locationManager.delegate = self
        
        self.btnNext.layer.cornerRadius = 10
        self.btnNext.layer.cornerRadius = 10
        self.btnNext.layer.shadowColor = UIColor(red: 30/255, green: 24/255, blue: 148/255, alpha: 1).cgColor
        self.btnNext.layer.shadowOpacity = 1;
        self.btnNext.layer.shadowRadius = 1;
        self.btnNext.layer.shadowOffset = CGSize(width: 1, height: 4)
        self.btnNext.isHidden = true
        
        appDelegate.apInfo.ssid = ""
        appDelegate.apInfo.pw = ""
        appDelegate.apInfo.securityModeString = ""
        appDelegate.apInfo.securityModeInt = -1
    }
    
    @IBAction func onBtnBack(_ sender: UIButton) {
        let mainVC = UIStoryboard(name: "First", bundle: nil).instantiateViewController(withIdentifier: "MainViewController")
        mainVC.modalPresentationStyle = .fullScreen
        present(mainVC, animated: true, completion: nil)
    }
    
    
    @IBAction func onBtnNext(_ sender: UIButton) {
        
        let connectDeviceVC = UIStoryboard(name: "First", bundle: nil).instantiateViewController(withIdentifier: "ConnectDeviceViewController")
        connectDeviceVC.modalPresentationStyle = .fullScreen
        present(connectDeviceVC, animated: true, completion: nil)
        
    }
    
    func getLocationUsagePermission() {
        self.locationManager.requestWhenInUseAuthorization()
    }
    
    func locationManager(_ manager: CLLocationManager, didChangeAuthorization status: CLAuthorizationStatus) {

        //location
        switch status {
        case .authorizedAlways, .authorizedWhenInUse:
            print("GPS permission authorizedAlways!")
            self.btnNext.isHidden = false
            
        case .restricted, .notDetermined:
            print("GPS permission restricted!")
            self.getLocationUsagePermission()
        case .denied:
            print("GPS permission denied!")
            self.getLocationUsagePermission()
        default:
            print("GPS: Default")
        }
    }
    
}

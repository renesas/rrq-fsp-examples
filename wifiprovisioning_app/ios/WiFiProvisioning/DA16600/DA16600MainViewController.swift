//
//  DA16600MainViewController.swift
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

class DA16600MainViewController: UIViewController {

    @IBOutlet weak var btnBack: UIButton!
    @IBOutlet weak var btnStart: UIButton!
    
    override func viewDidLoad() {
        super.viewDidLoad()

        self.btnStart.layer.cornerRadius = 10
        self.btnStart.layer.shadowColor = UIColor(red: 30/255, green: 24/255, blue: 148/255, alpha: 1).cgColor
        self.btnStart.layer.shadowOpacity = 1;
        self.btnStart.layer.shadowRadius = 1;
        self.btnStart.layer.shadowOffset = CGSize(width: 1, height: 4)
    }
    
    @IBAction func onBtnBack(_ sender: UIButton) {
        let mainVC = UIStoryboard(name: "First", bundle: nil).instantiateViewController(withIdentifier: "MainViewController")
        mainVC.modalPresentationStyle = .fullScreen
        present(mainVC, animated: true, completion: nil)
    }
    
    @IBAction func onBtnStart(_ sender: UIButton) {
        let perVC = UIStoryboard(name: "First", bundle: nil).instantiateViewController(withIdentifier: "PeripheralViewController")
        perVC.modalPresentationStyle = .fullScreen
        present(perVC, animated: true, completion: nil)
    }
    
}

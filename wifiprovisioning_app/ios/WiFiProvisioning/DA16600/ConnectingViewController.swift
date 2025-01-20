//
//  ConnectingViewController.swift
//  BLEScanner
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

protocol ConnectingViewControllerDelegate: AnyObject {
    func didTapCancel(_ vc: ConnectingViewController)
}

class ConnectingViewController: UIViewController {
    @IBOutlet private weak var titleLabel: UILabel!
    @IBOutlet private weak var loadingOverlayView: UIView!
    @IBOutlet private weak var cancelButton: UIButton!
    
    weak var delegate: ConnectingViewControllerDelegate?
    var peripheralName = ""
    
    override func viewDidLoad() {
        super.viewDidLoad()
        titleLabel.text = "Connecting to \(peripheralName)..."
        loadingOverlayView.layer.cornerRadius = 3
        self.cancelButton.layer.cornerRadius = 3
        self.cancelButton.layer.shadowColor = UIColor(red: 30/255, green: 24/255, blue: 148/255, alpha: 1).cgColor
        self.cancelButton.layer.shadowOpacity = 1;
        self.cancelButton.layer.shadowRadius = 1;
        self.cancelButton.layer.shadowOffset = CGSize(width: 1, height: 4)
        
    }
    
    @IBAction private func didTapCancelButton(_ sender: Any) {
        delegate?.didTapCancel(self)
    }
}

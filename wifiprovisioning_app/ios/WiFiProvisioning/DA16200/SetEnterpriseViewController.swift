//  SetEnterpriseViewController.swift
//  WiFiProvisioning
//
//  Created by livekim on 2024/10/25.
//
// ******************************************************************************
//*
//* Copyright (C) 2023 Renesas Electronics.
//* This computer program includes Confidential, Proprietary Information
//* of Renesas Electronics. All Rights Reserved.
//*
//*******************************************************************************

import Foundation
import UIKit
import DLRadioButton

protocol SetEnterpriseViewControllerDelegate: AnyObject {
    func didTapCancel(_ vc: SetEnterpriseViewController)
    func didTapOk(_ vc: SetEnterpriseViewController)
}

class SetEnterpriseViewController: UIViewController, UITextFieldDelegate {
    let appDelegate = UIApplication.shared.delegate as! AppDelegate
    weak var delegate: SetEnterpriseViewControllerDelegate?
    
    @IBOutlet weak var enterpriseview: UIView!
    @IBOutlet weak var tf_name: UITextField!
    @IBOutlet weak var tf_password: UITextField!
    @IBOutlet weak var btn_cancel: UIButton!
    @IBOutlet weak var btn_ok: UIButton!
    
    @IBOutlet weak var eapBtn0: DLRadioButton!
    @IBOutlet weak var eapBtn1: DLRadioButton!
    @IBOutlet weak var eapBtn2: DLRadioButton!
    @IBOutlet weak var eapBtn3: DLRadioButton!
    
    @IBOutlet weak var authBtn0: DLRadioButton!
    @IBOutlet weak var authBtn1: DLRadioButton!
    @IBOutlet weak var authBtn2: DLRadioButton!
    
    
    @objc func tapDone(sender: Any) {
        self.view.endEditing(true)
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        enterpriseview.layer.cornerRadius = 3
        enterpriseview.center = self.view.center
        
        self.eapBtn0.isSelected = true
        self.appDelegate.apInfo.enterpriseAuthType = 0
        
        self.eapBtn0.addTarget(self, action: #selector(self.eapBtnTouch(_:)), for: .touchUpInside)
        self.eapBtn1.addTarget(self, action: #selector(self.eapBtnTouch(_:)), for: .touchUpInside)
        self.eapBtn2.addTarget(self, action: #selector(self.eapBtnTouch(_:)), for: .touchUpInside)
        self.eapBtn3.addTarget(self, action: #selector(self.eapBtnTouch(_:)), for: .touchUpInside)
        
        self.authBtn0.isSelected = true
        self.appDelegate.apInfo.enterpriseAuthProtocol = 0
        
        self.authBtn0.addTarget(self, action: #selector(self.authBtnTouch(_:)), for: .touchUpInside)
        self.authBtn1.addTarget(self, action: #selector(self.authBtnTouch(_:)), for: .touchUpInside)
        self.authBtn2.addTarget(self, action: #selector(self.authBtnTouch(_:)), for: .touchUpInside)
        
        
        self.btn_cancel.layer.cornerRadius = 3
        self.btn_cancel.layer.shadowColor = UIColor(red: 30/255, green: 24/255, blue: 148/255, alpha: 1).cgColor
        self.btn_cancel.layer.shadowOpacity = 1;
        self.btn_cancel.layer.shadowRadius = 1;
        self.btn_cancel.layer.shadowOffset = CGSize(width: 1, height: 4)
        
        self.btn_ok.layer.cornerRadius = 3
        self.btn_ok.layer.shadowColor = UIColor(red: 30/255, green: 24/255, blue: 148/255, alpha: 1).cgColor
        self.btn_ok.layer.shadowOpacity = 1;
        self.btn_ok.layer.shadowRadius = 1;
        self.btn_ok.layer.shadowOffset = CGSize(width: 1, height: 4)
        
    }
    
    override func viewWillAppear(_ animated: Bool) {
        self.registerForKeyboardNotifications()
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        self.unregisterForKeyboardNotifications()
        appDelegate.networkVC?.isShowSetEnterprise = false
    }

    
    @objc func eapBtnTouch(_ sender:DLRadioButton) {
        let selectedButtonTitle = String(format:  sender.currentTitle!)
        NSLog("\(#fileID):\(#line) >> \(selectedButtonTitle)")
        if (selectedButtonTitle == "PEAP or TTLS or FAST (Recommend)") {
            appDelegate.apInfo.enterpriseAuthType = 0
        } else if (selectedButtonTitle == "PEAP") {
            appDelegate.apInfo.enterpriseAuthType = 1
        } else if (selectedButtonTitle == "TTLS") {
            appDelegate.apInfo.enterpriseAuthType = 2
        } else if (selectedButtonTitle == "FAST") {
            appDelegate.apInfo.enterpriseAuthType = 3
        }
    }
    
    @objc func authBtnTouch(_ sender:DLRadioButton) {
        let selectedButtonTitle = String(format:  sender.currentTitle!)
        NSLog("\(#fileID):\(#line) >> \(selectedButtonTitle)")
        if (selectedButtonTitle == "MSCHAPv2 or GTC (Recommend)") {
            appDelegate.apInfo.enterpriseAuthProtocol = 0
        } else if (selectedButtonTitle == "MSCHAPv2") {
            appDelegate.apInfo.enterpriseAuthProtocol = 1
        } else if (selectedButtonTitle == "GTC") {
            appDelegate.apInfo.enterpriseAuthProtocol = 2
        }
    }
    

    @IBAction func didTapCancelButton(_ sender: UIButton) {
        delegate?.didTapCancel(self)
        self.dismiss(animated: true, completion: nil)
        self.appDelegate.apInfo.enterpriseAuthType = -1
        self.appDelegate.apInfo.enterpriseAuthProtocol = -1
        self.appDelegate.apInfo.enterpriseID = ""
        self.appDelegate.apInfo.enterprisePassword = ""
    }

    @IBAction private func didTapOkButton(_ sender: Any) {
        delegate?.didTapOk(self)

        self.appDelegate.apInfo.enterpriseID = tf_name.text!
        self.appDelegate.apInfo.enterprisePassword = tf_password.text!
        NSLog("\(#fileID):\(#line) >> \(self.appDelegate.apInfo.enterprisePassword)")
        
        self.appDelegate.apInfo.isHiddenWiFi = 0
        
        appDelegate.networkVC?.tcpSendDPMSet()
        usleep(100000)
        appDelegate.networkVC?.sendEnterpriseConfig()
        self.dismiss(animated: true, completion: nil)
    }
    
    //MARK: - Keyboard Event
    
    func registerForKeyboardNotifications() {
        NotificationCenter.default.addObserver(self, selector:#selector(keyboardWillShow), name: UIResponder.keyboardWillShowNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector:#selector(keyboardWillHide), name: UIResponder.keyboardWillHideNotification, object: nil)
    }
    
    func unregisterForKeyboardNotifications() {
        NotificationCenter.default.removeObserver(self, name:UIResponder.keyboardWillShowNotification, object: nil)
        NotificationCenter.default.removeObserver(self, name:UIResponder.keyboardWillHideNotification, object: nil)
    }
    
    @objc func keyboardWillShow(notification: NSNotification) {
        if let keyboardSize = (notification.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? NSValue)?.cgRectValue {
            if self.view.frame.origin.y == 0 {
                //self.view.frame.origin.y -= keyboardSize.height
                self.view.frame.origin.y -= 150
            }
        }
    }
    
    @objc func keyboardWillHide(notification: NSNotification) {
        if self.view.frame.origin.y != 0 {
            self.view.frame.origin.y = 0
        }
    }
}

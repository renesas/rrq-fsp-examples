//
//  DA16200AddHiddenViewController.swift
//  WiFiProvisioning
//
//  Created by livekim on 2021/07/08.
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
import DLRadioButton

protocol DA16200AddHiddenViewControllerDelegate: AnyObject {
    func didTapCancel(_ vc: DA16200AddHiddenViewController)
    func didTapOk(_ vc: DA16200AddHiddenViewController)
}

class DA16200AddHiddenViewController: UIViewController, UITextFieldDelegate {
    let appDelegate = UIApplication.shared.delegate as! AppDelegate
    weak var delegate: DA16200AddHiddenViewControllerDelegate?
    
    @IBOutlet weak var hiddenView: UIView!
    @IBOutlet weak var passwordView: UIView!
    @IBOutlet weak var tfSSID: UITextField!
    @IBOutlet weak var tfPassword: UITextField!
    
    @IBOutlet weak var btnRequireAuth: UIButton!
    
    @IBOutlet weak var btnCancel: UIButton!
    @IBOutlet weak var btnOk: UIButton!
    
    @objc func tapDone(sender: Any) {
        self.view.endEditing(true)
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        hiddenView.layer.cornerRadius = 3
        
        self.btnRequireAuth.isSelected = false
        self.passwordView.isHidden = true
        
        self.btnCancel.layer.cornerRadius = 3
        self.btnCancel.layer.shadowColor = UIColor(red: 30/255, green: 24/255, blue: 148/255, alpha: 1).cgColor
        self.btnCancel.layer.shadowOpacity = 1;
        self.btnCancel.layer.shadowRadius = 1;
        self.btnCancel.layer.shadowOffset = CGSize(width: 1, height: 4)
        
        self.btnOk.layer.cornerRadius = 3
        self.btnOk.layer.shadowColor = UIColor(red: 30/255, green: 24/255, blue: 148/255, alpha: 1).cgColor
        self.btnOk.layer.shadowOpacity = 1;
        self.btnOk.layer.shadowRadius = 1;
        self.btnOk.layer.shadowOffset = CGSize(width: 1, height: 4)
    }
    
    override func viewWillAppear(_ animated: Bool) {
        self.registerForKeyboardNotifications()
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        self.unregisterForKeyboardNotifications()
    }
    
    @IBAction func checkBoxTapped(_ sender: UIButton) {
        if sender.isSelected {
            sender.isSelected = false
            self.btnRequireAuth.setTitleColor(.black, for: .normal)
            self.passwordView.isHidden = true
            self.appDelegate.apInfo.securityModeInt = 0
        } else {
            sender.isSelected = true
            self.btnRequireAuth.setTitleColor(.systemBlue, for: .selected)
            self.passwordView.isHidden = false
            self.appDelegate.apInfo.securityModeInt = 3
        }
    }
    
    @IBAction private func didTapCancelButton(_ sender: Any) {
        delegate?.didTapCancel(self)
        self.appDelegate.apInfo.ssid = ""
        self.appDelegate.apInfo.pw = ""
        self.appDelegate.apInfo.securityModeInt = -1
        self.dismiss(animated: true, completion: nil)
    }
    
    @IBAction private func didTapOkButton(_ sender: Any) {
        delegate?.didTapOk(self)
        
        self.appDelegate.apInfo.ssid = tfSSID.text!
        self.appDelegate.apInfo.pw = tfPassword.text!
        NSLog("\(#fileID):\(#line) >> \(self.appDelegate.apInfo.pw)")
        
        self.appDelegate.apInfo.isHiddenWiFi = 1
        appDelegate.networkVC?.tcpSendDPMSet()
        usleep(100000)
        NSLog("\(#fileID):\(#line) >> \(self.appDelegate.apInfo.securityModeInt)")
        appDelegate.networkVC?.tcpSendSSIDPW_1()
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

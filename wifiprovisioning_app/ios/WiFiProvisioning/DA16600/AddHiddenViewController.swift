//
//  AddHiddenViewController.swift
//  WiFiProvisioning
//
//  Created by livekim on 2021/07/07.
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

protocol AddHiddenViewControllerDelegate: AnyObject {
    func didTapCancel(_ vc: AddHiddenViewController)
    func didTapOk(_ vc: AddHiddenViewController)
}

class AddHiddenViewController: UIViewController, UITextFieldDelegate {
    
    let appDelegate = UIApplication.shared.delegate as! AppDelegate
    weak var delegate: AddHiddenViewControllerDelegate?
    @IBOutlet weak var hiddenView: UIView!
    @IBOutlet weak var passwordView: UIView!
    @IBOutlet weak var tfSSID: UITextField!
    @IBOutlet weak var tfPassword: UITextField!
    
    @IBOutlet weak var btnOPEN: DLRadioButton!
    @IBOutlet weak var btnWEP: DLRadioButton!
    @IBOutlet weak var btnWPA: DLRadioButton!
    @IBOutlet weak var btnWPA2: DLRadioButton!
    
    @IBOutlet weak var btnCancel: UIButton!
    @IBOutlet weak var btnOk: UIButton!
    
    @objc func tapDone(sender: Any) {
        self.view.endEditing(true)
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        hiddenView.layer.cornerRadius = 3
        hiddenView.center = self.view.center
        
        btnOPEN.iconSize = 24
        btnWEP.iconSize = 24
        btnWPA.iconSize = 24
        btnWPA2.iconSize = 24
        
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
    
    @objc @IBAction private func logSelectedButton(_ radioButton: DLRadioButton) {
        if (radioButton.isMultipleSelectionEnabled) {
            for button in radioButton.selectedButtons() {
                //for check box
                NSLog("\(#fileID):\(#line) >> \(String(format: "%@ is selected.\n", button.titleLabel!.text!))")
            }
        } else {
            //for radio
            let selectedButtonTitle = String(format:  radioButton.titleLabel!.text!)
            NSLog("\(#fileID):\(#line) >> \(selectedButtonTitle)")
            if (selectedButtonTitle == "OPEN") {
                appDelegate.apInfo_DA16600.security = 0
                self.passwordView.isHidden = true
            } else if (selectedButtonTitle == "WEP") {
                appDelegate.apInfo_DA16600.security = 1
                self.passwordView.isHidden = false
            } else if (selectedButtonTitle == "WPA") {
                appDelegate.apInfo_DA16600.security = 2
                self.passwordView.isHidden = false
            } else if (selectedButtonTitle == "WPA2") {
                appDelegate.apInfo_DA16600.security = 3
                self.passwordView.isHidden = false
            }
        }
    }
    
    @IBAction private func didTapCancelButton(_ sender: Any) {
        delegate?.didTapCancel(self)
        self.dismiss(animated: true, completion: nil)
    }
    
    @IBAction private func didTapOkButton(_ sender: Any) {
        delegate?.didTapOk(self)
        
        self.appDelegate.apInfo_DA16600.ssid = tfSSID.text!
        self.appDelegate.apInfo_DA16600.pw = tfPassword.text!
        NSLog("\(#fileID):\(#line) >> \(self.appDelegate.apInfo_DA16600.pw)")
        
        appDelegate.periConnectedVC?.networkTable.isHidden = true
        self.appDelegate.apInfo_DA16600.isHiddenWiFi = 1
        appDelegate.periConnectedVC?.setRawText()
        appDelegate.periConnectedVC?.labelRawCommand.isHidden = false
        appDelegate.periConnectedVC?.tvData1.isHidden = false
        appDelegate.periConnectedVC?.tvData2.isHidden = false
        
        appDelegate.periConnectedVC?.btnConnect.setTitle("Connect to \(self.appDelegate.apInfo_DA16600.ssid)", for: .normal)
        appDelegate.periConnectedVC?.btnConnect.isHidden = false
        
        usleep(100000)
        
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

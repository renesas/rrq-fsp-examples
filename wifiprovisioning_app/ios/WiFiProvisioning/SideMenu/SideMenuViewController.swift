//
//  SideMenuViewController.swift
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

class SideMenuViewController: UIViewController {
    
    let appDelegate = UIApplication.shared.delegate as! AppDelegate
    var mode: Int?
    var thingName: String?
    var azureConString: String?
    
    @IBOutlet weak var labelVersion: UILabel!
    override func viewDidLoad() {
        super.viewDidLoad()
        
        labelVersion.text = appDelegate.appVersion
        labelVersion.font = UIFont.italicSystemFont(ofSize: UIFont.labelFontSize)
        
        self.mode = UserDefaults.standard.integer(forKey: "modeKey")
        if (self.mode != nil) {
            NSLog("\(#fileID):\(#line) >> mode : \(self.mode!)")
        }
        
        self.thingName = UserDefaults.standard.string(forKey: "thingNameKey")
        if (self.thingName != nil) {
            NSLog("\(#fileID):\(#line) >> thingName : \(self.thingName!)")
        }
        
        self.azureConString = UserDefaults.standard.string(forKey: "azureConStringKey")
        if (self.azureConString != nil) {
            NSLog("\(#fileID):\(#line) >> azureConString : \(self.azureConString!)")
        }
        
        // Do any additional setup after loading the view.
    }
    
    
    @IBAction func onBtnOpensource(_ sender: UIButton) {
        
        appDelegate.mySidemenu?.dismiss(animated: true, completion: nil)
        
        appDelegate.mainVC?.gotoOpensourceView()
        
    }
    
    @IBAction func onBtnAws(_ sender: UIButton) {
        
        if self.mode != nil  {
            
            NSLog("\(#fileID):\(#line) >> mode : \(self.mode!)")
            
            if self.mode! < 10 {  //Generic SDK
                self.showAwsNotSupportDialog()
            } else if ((self.mode! as Int == 10) || (self.mode! as Int == 12)) {  //General AWS IoT
                
                if self.thingName != nil {
                    NSLog("\(#fileID):\(#line) >> thingName : \(thingName!)")
                    if ((self.thingName!.contains("DOORLOCK")) || (self.thingName!.contains("DoorLock")) || (self.thingName!.contains("doorlock") || (self.thingName!.contains("IOT")))) {
                        appDelegate.mySidemenu?.dismiss(animated: true, completion: nil)
                        appDelegate.mainVC?.gotoAwsIotDoorView()
                        NSLog("\(#fileID):\(#line) >> go AWSIoTDoorViewController")
                    } else if self.thingName!.contains("SENSOR") {
                        appDelegate.mySidemenu?.dismiss(animated: true, completion: nil)
                        appDelegate.mainVC?.gotoAwsIotSensorView()
                        NSLog("\(#fileID):\(#line) >> go AWSIoTSensorViewController")
                    }
                    else {
                        appDelegate.mySidemenu?.dismiss(animated: true, completion: nil)
                        appDelegate.mainVC?.gotoAwsIotDoorView()
                    }
                } else {
                    NSLog("\(#fileID):\(#line) >> thingName is nil~")
                    self.showAwsNotSupportDialog()
                }
                
            } else if ((self.mode! == 11) || (self.mode! as Int == 13)) {  //AT-CMD AWS IoT
                if self.thingName != nil {
                    appDelegate.mySidemenu?.dismiss(animated: true, completion: nil)
                    appDelegate.mainVC?.gotoAwsIotDeviceView()
                    NSLog("\(#fileID):\(#line) >> go AWSIoTDeviceViewController")
                } else {
                    NSLog("\(#fileID):\(#line) >> thingName is nil~")
                    self.showAwsNotSupportDialog()
                }
            } else {
                self.showAwsNotSupportDialog()
            }
            
        } else {
            NSLog("\(#fileID):\(#line) >> mode is nil~")
            self.showAwsNotSupportDialog()
        }
        
    }
    
    @IBAction func onBtnAzure(_ sender: UIButton) {
        if self.mode != nil  {
            
            NSLog("\(#fileID):\(#line) >> mode : \(self.mode!)")
            
            if (self.mode! as Int == 20 || self.mode! as Int == 21) {  //General Azure IoT or AT-CMD Azure IoT
                
                if (self.thingName != nil && self.azureConString != nil) {
                    if (!self.thingName!.isEmpty && !self.azureConString!.isEmpty) {
                        NSLog("\(#fileID):\(#line) >> thingName : \(thingName!)")
                        appDelegate.mySidemenu?.dismiss(animated: true, completion: nil)
                        appDelegate.mainVC?.gotoAzureIotDoorView()
                        NSLog("\(#fileID):\(#line) >> go AzureIoTDoorViewController")
                    } else {
                        NSLog("\(#fileID):\(#line) >> thingName or azureConString is nil~")
                        self.showAzureNotSupportDialog()
                    }
                    
                } else {
                    NSLog("\(#fileID):\(#line) >> thingName or azureConString is nil~")
                    self.showAzureNotSupportDialog()
                }
                
            } else {
                self.showAzureNotSupportDialog()
            }
            
        } else {
            NSLog("\(#fileID):\(#line) >> mode is nil~")
            self.showAzureNotSupportDialog()
        }
    }
    
    
    // MARK: - Dialog
    
    func showAwsNotSupportDialog() {
        let alertTitle = "Not supported"
        
        let titleParagraphStyle = NSMutableParagraphStyle()
        titleParagraphStyle.alignment = NSTextAlignment.center
        
        let attributedTitleText = NSMutableAttributedString(
            string: alertTitle,
            attributes: [
                NSAttributedString.Key.paragraphStyle: titleParagraphStyle,
                NSAttributedString.Key.font: UIFont.systemFont(ofSize: 20.0)
            ]
        )
        
        let alertMessage = """
            
            Perform provisioning using an SDK that supports AWS IoT.
            """
        
        let messageParagraphStyle = NSMutableParagraphStyle()
        messageParagraphStyle.alignment = NSTextAlignment.left
        
        let attributedMessageText = NSMutableAttributedString(
            string: alertMessage,
            attributes: [
                NSAttributedString.Key.paragraphStyle: messageParagraphStyle,
                NSAttributedString.Key.font: UIFont.systemFont(ofSize: 15.0)
            ]
        )
        
        let alert = UIAlertController.init(title: nil, message: nil, preferredStyle: UIAlertController.Style.alert)
        alert.setValue(attributedTitleText, forKey: "attributedTitle")
        alert.setValue(attributedMessageText, forKey: "attributedMessage")
        let okAction = UIAlertAction(title: "OK", style: .default, handler: { (action) -> Void in
            self.appDelegate.mySidemenu?.dismiss(animated: true, completion: nil)
        })
        alert.addAction(okAction)
        present(alert, animated: true, completion: nil)
    }
    
    func showAzureNotSupportDialog() {
        let alertTitle = "Not supported"
        
        let titleParagraphStyle = NSMutableParagraphStyle()
        titleParagraphStyle.alignment = NSTextAlignment.center
        
        let attributedTitleText = NSMutableAttributedString(
            string: alertTitle,
            attributes: [
                NSAttributedString.Key.paragraphStyle: titleParagraphStyle,
                NSAttributedString.Key.font: UIFont.systemFont(ofSize: 20.0)
            ]
        )
        
        let alertMessage = """
            
            Perform provisioning using an SDK that supports Azure IoT.
            """
        
        let messageParagraphStyle = NSMutableParagraphStyle()
        messageParagraphStyle.alignment = NSTextAlignment.left
        
        let attributedMessageText = NSMutableAttributedString(
            string: alertMessage,
            attributes: [
                NSAttributedString.Key.paragraphStyle: messageParagraphStyle,
                NSAttributedString.Key.font: UIFont.systemFont(ofSize: 15.0)
            ]
        )
        
        let alert = UIAlertController.init(title: nil, message: nil, preferredStyle: UIAlertController.Style.alert)
        alert.setValue(attributedTitleText, forKey: "attributedTitle")
        alert.setValue(attributedMessageText, forKey: "attributedMessage")
        let okAction = UIAlertAction(title: "OK", style: .default, handler: { (action) -> Void in
            self.appDelegate.mySidemenu?.dismiss(animated: true, completion: nil)
        })
        alert.addAction(okAction)
        present(alert, animated: true, completion: nil)
    }
    
    func showAzureDeviceFailDialog() {
        let alertTitle = "Error"
        
        let titleParagraphStyle = NSMutableParagraphStyle()
        titleParagraphStyle.alignment = NSTextAlignment.center
        
        let attributedTitleText = NSMutableAttributedString(
            string: alertTitle,
            attributes: [
                NSAttributedString.Key.paragraphStyle: titleParagraphStyle,
                NSAttributedString.Key.font: UIFont.systemFont(ofSize: 20.0)
            ]
        )
        
        let alertMessage = """
            
            The device information is corrupted.
            Please check the Azure-related information stored on the device
            and run provisioning again.
            """
        
        let messageParagraphStyle = NSMutableParagraphStyle()
        messageParagraphStyle.alignment = NSTextAlignment.left
        
        let attributedMessageText = NSMutableAttributedString(
            string: alertMessage,
            attributes: [
                NSAttributedString.Key.paragraphStyle: messageParagraphStyle,
                NSAttributedString.Key.font: UIFont.systemFont(ofSize: 15.0)
            ]
        )
        
        let alert = UIAlertController.init(title: nil, message: nil, preferredStyle: UIAlertController.Style.alert)
        alert.setValue(attributedTitleText, forKey: "attributedTitle")
        alert.setValue(attributedMessageText, forKey: "attributedMessage")
        let okAction = UIAlertAction(title: "OK", style: .default, handler: { (action) -> Void in
            self.appDelegate.mySidemenu?.dismiss(animated: true, completion: nil)
        })
        alert.addAction(okAction)
        present(alert, animated: true, completion: nil)
    }
    
}

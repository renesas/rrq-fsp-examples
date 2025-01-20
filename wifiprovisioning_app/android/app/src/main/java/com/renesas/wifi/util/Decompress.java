package com.renesas.wifi.util;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.io.File;

public class Decompress {



   private String _zipFile;
   private String _location;

   public Decompress(String zipFile, String location) {
      _zipFile = zipFile;
      _location = location;

      _dirChecker("");
   }

   public void unzip() {
      try  {
         FileInputStream fin = new FileInputStream(_zipFile);
         ZipInputStream zin = new ZipInputStream(fin);
         ZipEntry ze = null;
         while ((ze = zin.getNextEntry()) != null) {
            MyLog.i("Unzipping " + ze.getName());

            if(ze.isDirectory()) {
               _dirChecker(ze.getName());
            } else {
               FileOutputStream fout = new FileOutputStream(_location + ze.getName());
               BufferedOutputStream bufout = new BufferedOutputStream(fout);
               byte[] buffer = new byte[1024];
               int read = 0;
               while ((read = zin.read(buffer)) != -1) {
                  bufout.write(buffer, 0, read);
               }

               bufout.close();

               zin.closeEntry();
               fout.close();
            }

         }
         zin.close();

         MyLog.i("Unzipping complete. path :  " +_location );
      } catch(Exception e) {
         MyLog.e("unzip error ==> "+e);
         MyLog.i("Unzipping failed");
      }

   }

   private void _dirChecker(String dir) {
      File f = new File(_location + dir);

      if(!f.isDirectory()) {
         f.mkdirs();
      }
   }


}

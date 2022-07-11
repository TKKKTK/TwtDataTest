package com.wg.twtdatatest;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * android:requestLegacyExternalStorage="false" false代表采用分区存储
 * 为true 代表的是采用之前的存储方式
 */
public class FileDownload {

    private String file_name;
    private FileOutputStream out = null;
    private BufferedWriter writer = null;
    private Context context;


    public FileDownload(String dir,Context context){
        file_name = dir;
        this.context = context;
    }

    public void save(Object obj){
        try{
            out = context.openFileOutput(file_name,Context.MODE_PRIVATE);
            writer = new BufferedWriter(new OutputStreamWriter(out));
            writer.write(obj.toString());
            Log.d(TAG, "save: "+context.getFilesDir());
        }catch (IOException e){
            e.printStackTrace();
        }finally {
            try{
                if (writer != null){
                    writer.close();
                }
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }
}

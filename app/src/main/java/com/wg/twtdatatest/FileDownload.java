package com.wg.twtdatatest;

import android.content.Context;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

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

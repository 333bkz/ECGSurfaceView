package com.bkz.ecg;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private Queue<Integer> dataQueue = new LinkedList<>();
    private ECGSurfaceView surfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        surfaceView = findViewById(R.id.surfaceView);
        loadDatas();
        simulator();
    }

    private void simulator() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (dataQueue.size() > 0) {
                    surfaceView.setData(dataQueue.poll());
                }
            }
        }, 0, 10);
    }

    private void loadDatas() {
        InputStream is = null;
        try {
            is = getResources().openRawResource(R.raw.ecgdata);
            int length = is.available();
            byte[] buffer = new byte[length];
            is.read(buffer);
            String data = new String(buffer);
            String[] datas = data.split(",");
            List<Integer> list = new ArrayList<>();
            for (String str : datas) {
                list.add(Integer.parseInt(str));
            }
            dataQueue.addAll(list);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (is != null)
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }
}

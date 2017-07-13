package com.lyf.pluginapk;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    private Button mButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        mButton = (Button) findViewById(R.id.btn_click);
        if (mButton != null) {
            mButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    Intent intent = new Intent(MainActivity.this,ThreeActivity.class);
                    startActivity(intent);
                }
            });
        }




    }

}

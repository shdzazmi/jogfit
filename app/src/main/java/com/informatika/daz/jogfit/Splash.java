package com.informatika.daz.jogfit;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageButton;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class Splash extends AppCompatActivity {

    @BindView(R.id.imageButtonNext) ImageButton imageButtonNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        ButterKnife.bind(this);
    }

    @OnClick(R.id.imageButtonNext)
    public void next(){
        Intent intent = new Intent(Splash.this, Home.class);
        startActivity(intent);
    }
}

package app.informatika.daz.jogfit.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import app.informatika.daz.jogfit.R;

public class SplashActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        Thread thread = new Thread() {
            public void run() {
                try {
                    sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    startActivity(new Intent(SplashActivity.this, MapsActivity.class));
                    finish();
                }
            }
        };
        thread.start();
    }
}

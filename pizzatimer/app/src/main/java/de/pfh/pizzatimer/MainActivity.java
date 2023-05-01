package de.pfh.pizzatimer;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private int count;
    private TextView showCount;
    private CountDownTimer timer;

    // TODO: Toast entfernen (auch in strings.xml)
    // TODO: Eingabe der Minuten
    // TODO: Auch Sekunden
    // TODO: Anzeige von Minuten:Sekunden
    // TODO: Nur ein Timer

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        showCount = findViewById(R.id.show_count);
        count = 0;
    }
    public void countUp(View view) {
        if (timer != null) {
            timer.cancel();
        }
        EditText editText = findViewById(R.id.show_count);
            int totalSeconds = Integer.parseInt(showCount.getText().toString()) * 60;
            timer = new CountDownTimer(totalSeconds * 1000, 1000) {
                public void onTick(long millisUntilFinished) {
                    int secondsLeft = (int) (millisUntilFinished / 1000);
                    int minutes = secondsLeft / 60;
                    int seconds = secondsLeft % 60;
                    showCount.setText(String.format("%02d%02d", minutes, seconds));
                }

                public void onFinish() {
                    showCount.setText("done!");
                }
            };
            timer.start();

    }

}
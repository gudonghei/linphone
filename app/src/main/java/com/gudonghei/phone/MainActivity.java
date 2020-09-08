package com.gudonghei.phone;


import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.gudonghei.phone.phone.LinphoneMiniManager;
import com.gudonghei.phone.phone.PhoneUtils;


import static com.gudonghei.phone.phone.PhoneUtils.phoneLogin;


public class MainActivity extends Activity {

    Button btn;
    Button btn2;
    Button btn3;
    Button btn4;
    Button btn5;
    Button btn6;
    Button btn7;

    EditText username;
    EditText password;
    EditText domain;
    EditText port;
    EditText number;

    TextView status;
    TextView other;
    TextView phone_status;


    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn = findViewById(R.id.btn);
        btn2 = findViewById(R.id.btn2);
        btn3 = findViewById(R.id.btn3);
        btn4 = findViewById(R.id.btn4);
        btn5 = findViewById(R.id.btn5);
        btn6 = findViewById(R.id.btn6);
        btn7 = findViewById(R.id.btn7);
        username = findViewById(R.id.username);
        password = findViewById(R.id.password);
        domain = findViewById(R.id.domain);
        port = findViewById(R.id.port);
        status = findViewById(R.id.status);
        phone_status = findViewById(R.id.phone_status);
        other = findViewById(R.id.other);
        number = findViewById(R.id.number);
        LinphoneMiniManager miniManager = LinphoneMiniManager.getInstance();

        if (miniManager == null) {
            miniManager = new LinphoneMiniManager(MainActivity.this);
        }

        miniManager.setPhoneListener(new LinphoneMiniManager.PhoneListener() {
            @Override
            public void phoneReturn(int x, String code) {
                Log.i("TAG", "\nphoneReturn: " + x +"     "+code);

                switch (x) {
                    case 0:
                        status.setText(code);
                        break;
                    case 1:
                        other.setText(code);
                        break;
                    case 2:
                        phone_status.setText(code);
                        break;
                }

            }
        });

        final LinphoneMiniManager FminiManager = miniManager;
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                phoneLogin(FminiManager,
                        username.getText().toString(),
                        password.getText().toString(),
                        domain.getText().toString(),
                        port.getText().toString());


            }
        });

        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    FminiManager.getmLinphoneCore()
                            .removeProxyConfig(FminiManager.getmLinphoneCore().getDefaultProxyConfig());

                } catch (Exception ignored) {
                }

            }
        });

        btn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PhoneUtils.Answer(FminiManager);
            }
        });

        btn4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PhoneUtils.HangUp(FminiManager);
            }
        });

        btn5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PhoneUtils.CallSomebody(FminiManager, number.getText().toString(), domain.getText().toString());
            }
        });
        btn6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PhoneUtils.useSpeaker(MainActivity.this);
            }
        });

        btn7.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PhoneUtils.useReceiver(MainActivity.this);
            }
        });
    }


}

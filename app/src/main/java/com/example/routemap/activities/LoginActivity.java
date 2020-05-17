package com.example.routemap.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.routemap.R;
import com.example.routemap.domain.User;

import java.util.ArrayList;
import java.util.List;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    private List<User> registredUsers;

    Button loginButton;
    TextView registerButton;
    EditText user;
    EditText password;

    private static final int PERMISSION_REQUEST_CODE = 200;
    private boolean locationPermission = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //3ª Entrega: Obtener lista de usuarios registrados de la Base de Datos
        registredUsers = new ArrayList<>();
        registredUsers.add(new User("admin@admin.com", "admin", "admin"));

        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);
        user = findViewById(R.id.user);
        password = findViewById(R.id.password);

        loginButton.setOnClickListener(this);
        registerButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.loginButton:
                if (user.getText().toString().isEmpty() || password.getText().toString().isEmpty()) {
                    user.setText("");
                    user.requestFocus();
                    password.setText("");
                    Toast.makeText(this, "Ambos campos son obligatorios", Toast.LENGTH_SHORT).show();
                    break;
                }

                if(check_login(user.getText().toString(), password.getText().toString())) {
                    checkLocationPermissions();
                    if(locationPermission) {
                        Intent in = new Intent(this, MapActivity.class);
                        startActivity(in);
                    }
                }else {
                    user.setText("");
                    user.requestFocus();
                    password.setText("");
                    user.setBackground(getDrawable(R.drawable.edit_text_design_error));
                    password.setBackground(getDrawable(R.drawable.edit_text_design_error));
                }
                break;

            case R.id.registerButton:
                Intent in = new Intent(this, RegisterActivity.class);
                startActivity(in);
                break;
        }
    }

    public Boolean check_login(String user, String password) {

        for (User registredUser : registredUsers) {
            if (registredUser.getUser().equals(user) && registredUser.getPassword().equals(password)) {
                return true;
            }
        }
        return false;
    }

    private void checkLocationPermissions() {

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
        }
        else {
            locationPermission = true;
        }
    }

    @Override
    protected void onResume() {
        user.setBackground(getDrawable(R.drawable.edit_text_design));
        password.setBackground(getDrawable(R.drawable.edit_text_design));
        super.onResume();
    }
}

package com.example.routemap.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.routemap.R;
import com.example.routemap.domain.User;

import java.util.List;

public class RegisterActivity extends AppCompatActivity implements View.OnClickListener {

    private Button registerButton;
    EditText email;
    EditText user;
    EditText password;
    EditText password2;

    private static final int PERMISSION_REQUEST_CODE = 200;
    private boolean locationPermission = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        registerButton = findViewById(R.id.sendRegisterButton);
        email = findViewById(R.id.registerEmail);
        user = findViewById(R.id.registerUser);
        password = findViewById(R.id.registerPassword);
        password2 = findViewById(R.id.registerPassword2);

        registerButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {

        EditText email = findViewById(R.id.registerEmail);
        EditText user = findViewById(R.id.registerUser);
        EditText password = findViewById(R.id.registerPassword);
        EditText password2 = findViewById(R.id.registerPassword2);

        if (email.getText().toString().equals("")
                || user.getText().toString().equals("")
                || password.getText().toString().equals("")
                || password2.getText().toString().equals("")) {
            Toast.makeText(this, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show();
        } else {
            if (password.getText().toString().equals(password2.getText().toString())) {

                //3ª Entrega: Comprobar si el email o el user ya estan registrados en la app
                User registredUser = new User(email.getText().toString(), user.getText().toString(), password.getText().toString());
                //3ª Entrega: Añadir user a base de datos.

                checkLocationPermissions();
                if(locationPermission) {
                    Toast.makeText(this, "Registro completado", Toast.LENGTH_SHORT).show();
                    Intent in = new Intent(this, MapActivity.class);
                    startActivity(in);
                }
            } else {
                password.setText("");
                password2.setText("");
                password.requestFocus();
                password.setBackground(getDrawable(R.drawable.edit_text_design_error));
                password2.setBackground(getDrawable(R.drawable.edit_text_design_error));
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            }
        }
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
        password.setBackground(getDrawable(R.drawable.edit_text_design));
        password2.setBackground(getDrawable(R.drawable.edit_text_design));
        super.onResume();
    }
}

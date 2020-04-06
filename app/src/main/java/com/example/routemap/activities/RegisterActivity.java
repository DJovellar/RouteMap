package com.example.routemap.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.routemap.R;
import com.example.routemap.domain.User;

import java.util.List;

public class RegisterActivity extends AppCompatActivity implements View.OnClickListener {

    private Button registerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        registerButton = findViewById(R.id.sendRegisterButton);
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

                Toast.makeText(this, "Registro completado", Toast.LENGTH_SHORT).show();

                //Intent in = new Intent();
                //startActivity(in);
            } else {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

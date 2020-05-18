package com.example.routemap.activities;

import androidx.annotation.NonNull;
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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    private Button loginButton;
    private TextView registerButton;
    private EditText user;
    private EditText password;

    private static final int PERMISSION_REQUEST_CODE = 200;
    private boolean locationPermission = false;

    FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        firebaseAuth = FirebaseAuth.getInstance();

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
                    Toast.makeText(this, "Todos los campos son obligatorios", Toast.LENGTH_LONG).show();
                    break;
                }

                checkLocationPermissions();
                if(locationPermission) {
                    firebaseAuth.signInWithEmailAndPassword(user.getText().toString(), password.getText().toString())
                            .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()) {
                                        Intent in = new Intent(getApplicationContext(), MapActivity.class);
                                        startActivity(in);
                                    }
                                    else {
                                        FirebaseAuthException exception = (FirebaseAuthException) task.getException();
                                        String errorCode = exception.getErrorCode();

                                        switch (errorCode) {
                                            case "ERROR_INVALID_EMAIL":
                                                Toast.makeText(LoginActivity.this, "El formato del email no es valido", Toast.LENGTH_SHORT).show();
                                                showBorderErrors(user, null);
                                                user.requestFocus();
                                                break;
                                            case "ERROR_USER_NOT_FOUND":
                                                Toast.makeText(LoginActivity.this, "El email no esta registrado", Toast.LENGTH_SHORT).show();
                                                showBorderErrors(user, null);
                                                user.requestFocus();
                                                break;
                                            case "ERROR_WRONG_PASSWORD":
                                                Toast.makeText(LoginActivity.this, "Contraseña incorrecta", Toast.LENGTH_SHORT).show();
                                                showBorderErrors(null, password);
                                                password.requestFocus();
                                                break;
                                            case "ERROR_USER_DISABLED":
                                                Toast.makeText(LoginActivity.this, "Usuario deshabilitado, contacte con Soporte", Toast.LENGTH_SHORT).show();
                                                showBorderErrors(user, password);
                                                user.requestFocus();
                                            default:
                                                Toast.makeText(LoginActivity.this, "Error por causa desconocida, escriba a Soporte para mas información", Toast.LENGTH_SHORT).show();
                                                showBorderErrors(user, password);
                                                user.requestFocus();
                                        }
                                    }
                                }
                            });
                } else {
                    Toast.makeText(this, "Acepte los permisos para acceder a la aplicación", Toast.LENGTH_LONG).show();
                }
                break;

            case R.id.registerButton:
                Intent in = new Intent(this, RegisterActivity.class);
                startActivity(in);
                break;
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

    public void showBorderErrors(EditText user_aux, EditText password_aux) {
        if(user_aux == null) {
            user.setBackground(getDrawable(R.drawable.edit_text_design));
        } else {
            user.setText("");
            user.setBackground(getDrawable(R.drawable.edit_text_design_error));
        }

        if(password_aux == null) {
            password.setBackground(getDrawable(R.drawable.edit_text_design));
        } else {
            password.setText("");
            password.setBackground(getDrawable(R.drawable.edit_text_design_error));
        }
    }

    @Override
    protected void onResume() {
        user.setBackground(getDrawable(R.drawable.edit_text_design));
        password.setBackground(getDrawable(R.drawable.edit_text_design));
        super.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if(currentUser != null) {
            checkLocationPermissions();
            if(locationPermission) {
                Intent in = new Intent(this, MapActivity.class);
                startActivity(in);
            }
        }
    }
}

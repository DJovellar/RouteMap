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
import android.widget.Toast;

import com.example.routemap.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;


public class RegisterActivity extends AppCompatActivity implements View.OnClickListener {

    private Button registerButton;
    private EditText email;
    private EditText alias;
    private EditText password;
    private EditText password2;

    private FirebaseAuth firebaseAuth;

    private static final int PERMISSION_REQUEST_CODE = 200;
    private boolean locationPermission = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        registerButton = findViewById(R.id.sendRegisterButton);
        email = findViewById(R.id.registerEmail);
        password = findViewById(R.id.registerPassword);
        password2 = findViewById(R.id.registerPassword2);
        alias = findViewById(R.id.registerAlias);

        registerButton.setOnClickListener(this);

        firebaseAuth = FirebaseAuth.getInstance();
    }

    @Override
    public void onClick(View v) {
        if (email.getText().toString().equals("")
                || alias.getText().toString().equals("")
                || password.getText().toString().equals("")
                || password2.getText().toString().equals("")) {
            Toast.makeText(this, "Todos los campos son obligatorios", Toast.LENGTH_LONG).show();
        } else {
            if (password.getText().toString().equals(password2.getText().toString())) {
                checkLocationPermissions();
                if(locationPermission) {
                    firebaseAuth.createUserWithEmailAndPassword(email.getText().toString(), password.getText().toString())
                            .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()) {

                                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder().setDisplayName(alias.getText().toString()).build();
                                        firebaseUser.updateProfile(profileUpdates);

                                        Intent in = new Intent(getApplicationContext(), MapActivity.class);
                                        startActivity(in);
                                    }
                                    else {
                                        FirebaseAuthException exception = (FirebaseAuthException) task.getException();
                                        String errorCode = exception.getErrorCode();

                                        switch (errorCode) {
                                            case "ERROR_INVALID_EMAIL":
                                                Toast.makeText(RegisterActivity.this, "El formato del email no es valido", Toast.LENGTH_SHORT).show();
                                                showBorderErrors(email, null,null, null);
                                                email.requestFocus();
                                                break;
                                            case "ERROR_EMAIL_ALREADY_IN_USE":
                                                Toast.makeText(RegisterActivity.this, "El email ya esta registrado", Toast.LENGTH_SHORT).show();
                                                showBorderErrors(email, null,null, null);
                                                email.requestFocus();
                                                break;
                                            case "ERROR_WEAK_PASSWORD":
                                                Toast.makeText(RegisterActivity.this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
                                                showBorderErrors(null, null, password, password2);
                                                password.requestFocus();
                                                break;
                                            default:
                                                Toast.makeText(RegisterActivity.this, "Error por causa desconocida, escriba a Soporte para mas información", Toast.LENGTH_SHORT).show();
                                                email.requestFocus();
                                        }
                                    }
                                }
                            });
                } else {
                    Toast.makeText(this, "Acepte los permisos para registrarse en la aplicación", Toast.LENGTH_LONG).show();
                }
            } else {
                showBorderErrors(null, null, password, password2);
                password.requestFocus();
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_LONG).show();
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

    public void showBorderErrors(EditText email_aux, EditText user_aux, EditText password_aux, EditText password2_aux) {

        if (email_aux == null) {
            email.setBackground(getDrawable(R.drawable.edit_text_design));
        } else {
            email.setText("");
            email.setBackground(getDrawable(R.drawable.edit_text_design_error));
        }

        if (user_aux == null) {
            alias.setBackground(getDrawable(R.drawable.edit_text_design));
        } else {
            alias.setText("");
            alias.setBackground(getDrawable(R.drawable.edit_text_design_error));
        }

        if(password_aux == null) {
            password.setBackground(getDrawable(R.drawable.edit_text_design));
        } else {
            password.setText("");
            password.setBackground(getDrawable(R.drawable.edit_text_design_error));
        }

        if(password2_aux == null) {
            password2.setBackground(getDrawable(R.drawable.edit_text_design));
        } else {
            password2.setText("");
            password2.setBackground(getDrawable(R.drawable.edit_text_design_error));
        }
    }

    @Override
    protected void onResume() {
        password.setBackground(getDrawable(R.drawable.edit_text_design));
        password2.setBackground(getDrawable(R.drawable.edit_text_design));
        super.onResume();
    }
}

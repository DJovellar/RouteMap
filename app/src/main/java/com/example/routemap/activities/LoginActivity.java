package com.example.routemap.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.routemap.R;
import com.example.routemap.domain.User;

import java.util.ArrayList;
import java.util.List;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    private Toolbar toolbar;

    private List<User> registredUsers;

    Button loginButton;
    Button registerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //3ª Entrega: Obtener lista de usuarios registrados de la Base de Datos
        registredUsers = new ArrayList<>();
        registredUsers.add(new User("admin@admin.com", "admin", "admin"));

        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);

        loginButton.setOnClickListener(this);
        registerButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.loginButton:

                EditText user = findViewById(R.id.user);
                EditText password = findViewById(R.id.password);

                if(check_login(user.getText().toString(), password.getText().toString())) {
                    Toast.makeText(this, "Sesion iniciada correctamente", Toast.LENGTH_SHORT).show();
                    Intent in = new Intent(this, MapActivity.class);
                    startActivity(in);
                }else {
                    user.setText("");
                    password.setText("");
                    Toast.makeText(this, "Error en los datos introducidos", Toast.LENGTH_SHORT).show();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu_login_register, menu);
        return true;
    }
}

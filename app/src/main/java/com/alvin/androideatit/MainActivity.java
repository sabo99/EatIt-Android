package com.alvin.androideatit;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.alvin.androideatit.Callback.ILoadCreateUIDListener;
import com.alvin.androideatit.Common.Common;
import com.alvin.androideatit.Model.UserModel;
import com.alvin.androideatit.Retrofit.ICloudFunctions;
import com.alvin.androideatit.Retrofit.RetrofitICloudClient;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.util.Arrays;
import java.util.List;

import dmax.dialog.SpotsDialog;
import io.reactivex.disposables.CompositeDisposable;

public class MainActivity extends AppCompatActivity implements ILoadCreateUIDListener {

    private static int APP_REQUEST_CODE = 7171;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener listener;

    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private ICloudFunctions cloudFunctions;
    private DatabaseReference userRef;
    private List<AuthUI.IdpConfig> providers;
    private ILoadCreateUIDListener listenerUID;


    private AlertDialog.Builder builder;
    private AlertDialog dialog;

    @Override
    protected void onStart() {
        super.onStart();
        firebaseAuth.addAuthStateListener(listener);
    }

    @Override
    protected void onStop() {
        if(listener != null)
            firebaseAuth.removeAuthStateListener(listener);
        compositeDisposable.clear();
        super.onStop();
    }

    @Override
    protected void onResume() {
        isBackButtonClicked = false;
        super.onResume();
    }

    // Exit this app
    boolean isBackButtonClicked = false;

    @Override
    public void onBackPressed() {
        if (isBackButtonClicked){
            super.onBackPressed();
            return;
        }
        this.isBackButtonClicked = true;
        Toast.makeText(this, "Please click BACK again to Exit", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
        listenerUID = this;

    }


    private void init() {
        dialog = new SpotsDialog.Builder().setCancelable(false).setContext(this).build();

        providers = Arrays.asList(new AuthUI.IdpConfig.PhoneBuilder().build());

        userRef = FirebaseDatabase.getInstance().getReference(Common.USER_REFERENCES);
        firebaseAuth = FirebaseAuth.getInstance();
        cloudFunctions = RetrofitICloudClient.getInstance().create(ICloudFunctions.class);
        listener = firebaseAuth -> {


            Dexter.withActivity(this)
                    .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    .withListener(new PermissionListener() {
                        @Override
                        public void onPermissionGranted(PermissionGrantedResponse response) {

                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            if (user != null){
                                checkUserFromFirebase(user);

                            }else{
                                phoneLogin();

                            }
                            
                        }

                        @Override
                        public void onPermissionDenied(PermissionDeniedResponse response) {
                            Toast.makeText(MainActivity.this, "Please enable permission to use app!", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                        }
                    }).check();

        };
    }

    private void checkUserFromFirebase(FirebaseUser user) {
        dialog.show();
        userRef.child(user.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists())
                        {
                            Toast.makeText(MainActivity.this, "Authentication success.", Toast.LENGTH_SHORT).show();

                            UserModel userModel = dataSnapshot.getValue(UserModel.class);
                            goToHomeActivity(userModel);
                        }
                        else
                        {
                            showRegisterDialog(user);
                        }
                        dialog.dismiss();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        dialog.dismiss();
                        Toast.makeText(MainActivity.this, ""+databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showRegisterDialog(FirebaseUser user) {
        builder = new AlertDialog.Builder(this);
        builder.setTitle("Register").setMessage("Please fill information");

        View itemView = LayoutInflater.from(this).inflate(R.layout.layout_register, null);
        EditText edt_name = itemView.findViewById(R.id.edt_name);
        EditText edt_address = itemView.findViewById(R.id.edt_address);
        EditText edt_phone = itemView.findViewById(R.id.edt_phone);

        // Set
        edt_phone.setText(user.getPhoneNumber());
        builder.setView(itemView)
                .setNegativeButton("CANCEL", (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                    finish();
                })
                .setPositiveButton("REGISTER", (dialogInterface, i) -> {
                    if (TextUtils.isEmpty(edt_name.getText().toString())){
                        Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    else if (TextUtils.isEmpty(edt_address.getText().toString())){
                        Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    UserModel userModel = new UserModel();
                    userModel.setUid(user.getUid());
                    userModel.setName(edt_name.getText().toString());
                    userModel.setAddress(edt_address.getText().toString());
                    userModel.setPhone(edt_phone.getText().toString());

                    createUID(userModel, user);
                    /// disini letak Create User sebelumnya

                })
                .setView(itemView);

        dialog = builder.create();
        dialog.show();
        Button bg1, bg2;
        bg1 = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        bg2 = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        bg1.setBackgroundColor(Color.TRANSPARENT);
        bg1.setTextColor(getResources().getColor(R.color.alert_button_color));
        bg2.setBackgroundColor(Color.TRANSPARENT);
        bg2.setTextColor(getResources().getColor(R.color.alert_button_color));
    }

    private void createUID(UserModel userModel, FirebaseUser user) {
        final DatabaseReference offsetRef = FirebaseDatabase.getInstance().getReference(".info/serverTimeOffset");
        offsetRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                long offset = dataSnapshot.getValue(Long.class);
                long estimatedServerTimeMs = System.currentTimeMillis() + offset;

                // CreateDate to CreateUID (Long) to Firebase
                listenerUID.onLoadCreateUIDSuccess(userModel, user, estimatedServerTimeMs);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                listenerUID.onLoadCreateUIDFailed(databaseError.getMessage());
            }
        });
    }

    private void writeToFirebase(UserModel userModel, FirebaseUser user) {
        userRef.child(user.getUid())
                .setValue(userModel)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()){
                        dialog.dismiss();
                        Toast.makeText(MainActivity.this, "Congratulation ! Register success", Toast.LENGTH_SHORT).show();
                        goToHomeActivity(userModel);

                    }
                });
    }

    private void goToHomeActivity(UserModel userModel) {

//        FirebaseInstanceId.getInstance()
//                .getInstanceId()
//                .addOnFailureListener(e -> {
//                    Toast.makeText(this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
//
//                    Common.currentUser = userModel; // Important, you need alwayw assign value for it before use
//                    startActivity(new Intent(MainActivity.this, HomeActivity.class));
//                    finish();
//
//                })
//                .addOnCompleteListener(task -> {
//
//                    Common.updateToken(MainActivity.this, task.getResult().getToken());
//                    Common.currentUser = userModel; // Important, you need always assign value for it before use
//                    startActivity(new Intent(MainActivity.this, HomeActivity.class));
//                    finish();
//
//                });

        Common.currentUser = userModel; // Important, you need always assign value for it before use
        startActivity(new Intent(MainActivity.this, HomeActivity.class));
        finish();
    }

    private void phoneLogin() {

        startActivityForResult(AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers).build()
                , APP_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (requestCode == APP_REQUEST_CODE)
        {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (requestCode == RESULT_OK)
            {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

            }
//            else
//            {
//                Toast.makeText(this, "Failed to sign in!", Toast.LENGTH_SHORT).show();
//            }
        }


    }

    @Override
    public void onLoadCreateUIDSuccess(UserModel userModel, FirebaseUser user, long createUID) {
        userModel.setCreateUID(createUID);
        writeToFirebase(userModel, user);
    }


    @Override
    public void onLoadCreateUIDFailed(String message) {
        Toast.makeText(this, ""+message, Toast.LENGTH_SHORT).show();
    }
}

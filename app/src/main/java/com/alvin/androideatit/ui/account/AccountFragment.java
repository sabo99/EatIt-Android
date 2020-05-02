package com.alvin.androideatit.ui.account;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.alvin.androideatit.Common.Common;
import com.alvin.androideatit.EventBus.HideFABCart;
import com.alvin.androideatit.HomeActivity;
import com.alvin.androideatit.MainActivity;
import com.alvin.androideatit.R;
import com.andremion.counterfab.CounterFab;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.greenrobot.eventbus.EventBus;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;

public class AccountFragment extends Fragment {

    private AccountViewModel accountViewModel;

    private DatabaseReference accountRef;

    private AlertDialog.Builder dialog;
    private AlertDialog alert;
    private Button btn_edit_account, btn_save_account;

    TextView name;
    TextView phone;
    TextView address;
    TextView uid;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        accountViewModel =
                ViewModelProviders.of(this).get(AccountViewModel.class);
        View root = inflater.inflate(R.layout.fragment_account, container, false);

        initViews();
        uid = root.findViewById(R.id.edt_account_uid);
        name = root.findViewById(R.id.edt_account_name);
        phone = root.findViewById(R.id.edt_account_phone);
        address = root.findViewById(R.id.edt_account_address);

        accountViewModel.getUid().observe(this, aLong -> uid.setText(aLong.toString()));
        accountViewModel.getName().observe(this, s -> name.setText(s));
        accountViewModel.getPhone().observe(this, s -> phone.setText(s));
        accountViewModel.getAddress().observe(this, s -> address.setText(s));

        btn_edit_account = root.findViewById(R.id.btn_edit_account);
        btn_save_account = root.findViewById(R.id.btn_save_account);



        btn_edit_account.setOnClickListener(v -> {

            name.setEnabled(true);
            phone.setEnabled(true);
            address.setEnabled(true);

            btn_edit_account.setVisibility(View.GONE);
            btn_save_account.setVisibility(View.VISIBLE);

            name.requestFocus();

        });

        btn_save_account.setOnClickListener(v -> {

            name.setEnabled(false);
            phone.setEnabled(false);
            address.setEnabled(false);

            showInformationBeforeUpdate();

        });

        return root;
    }

    private void setVisibleButton(){
        btn_edit_account.setVisibility(View.VISIBLE);
        btn_save_account.setVisibility(View.GONE);
    }

    private void displayAlertDialog_QuestionUpdate(){
        alert = dialog.create();
        alert.show();

        Button bg1, bg2;
        bg1 = alert.getButton(DialogInterface.BUTTON_POSITIVE);
        bg2 = alert.getButton(DialogInterface.BUTTON_NEGATIVE);
        bg1.setBackgroundColor(Color.TRANSPARENT);
        bg1.setTextColor(getResources().getColor(R.color.alert_button_color));
        bg2.setBackgroundColor(Color.TRANSPARENT);
        bg2.setTextColor(getResources().getColor(R.color.alert_button_color));


        TextView message = alert.findViewById(android.R.id.message);
        message.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        message.setPadding(20, 0, 20, 0);
    }

    private void displayAlertDialog_Button_Message_Center(){
        alert = dialog.create();
        alert.show();

        Button bg1;
        bg1 = alert.getButton(DialogInterface.BUTTON_POSITIVE);
        bg1.setBackgroundColor(Color.TRANSPARENT);
        bg1.setTextColor(getResources().getColor(R.color.alert_button_color));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.weight = 1.0f;
        params.gravity = Gravity.CENTER;
        bg1.setLayoutParams(params);

        TextView message = alert.findViewById(android.R.id.message);
        message.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        message.setPadding(30, 0, 30, 0);
    }


    private void showInformationBeforeUpdate(){
        dialog = new AlertDialog.Builder(getContext());
        dialog.setTitle("Information !")
                .setCancelable(false)
                .setMessage("UPDATE your Account Information. \nYou'll be restart from this app.")
                .setPositiveButton("CONTINUE", (dialog1, which) -> {
                    dialog1.dismiss();
                    showQuestionUpdate();
                });

        displayAlertDialog_Button_Message_Center();
    }

    private void showQuestionUpdate() {
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("name", name.getText().toString());
        updateData.put("phone", phone.getText().toString());
        updateData.put("address", address.getText().toString());

        dialog = new AlertDialog.Builder(getContext());
        dialog.setTitle("Update User Account")
                .setCancelable(false)
                .setMessage("Are you sure change this information?")
                .setPositiveButton("OK", (dialog1, which) -> {

                    updateUserAccount(updateData);
                    setVisibleButton();
                })
                .setNegativeButton("CANCEL", (dialog1, which) -> {
                    dialog1.dismiss();
                    setVisibleButton();
                });

        displayAlertDialog_QuestionUpdate();
    }

    private void updateUserAccount(Map<String, Object> updateData) {
        accountRef.child(Common.currentUser.getUid())
                .updateChildren(updateData)
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "[UPDATE ERROR] " + e.getMessage(), Toast.LENGTH_SHORT).show();
                })
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Update Successfully", Toast.LENGTH_SHORT).show();

//                    getActivity().finish();
                    Intent i = new Intent(getActivity(), MainActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    getActivity().finish();
                });
    }


    private void initViews() {
        accountRef = FirebaseDatabase.getInstance().getReference(Common.USER_REFERENCES);
        EventBus.getDefault().postSticky(new HideFABCart(true));
        setHasOptionsMenu(true);
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        menu.findItem(R.id.action_refresh).setVisible(false); // Hide Home menu already inflate
        menu.findItem(R.id.action_search).setVisible(false);
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.info_account_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_info_account)
        {
            showAccountUpdateInformation();
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAccountUpdateInformation() {

        dialog = new AlertDialog.Builder(getContext());
        dialog.setTitle("Information !")
                .setCancelable(false)
                .setMessage("If you UPDATE your Account Information. You'll be restart from this application " +
                        "and if successful, your account information is updated.")
                .setPositiveButton("OK", (dialog1, which) -> {
                    dialog1.dismiss();

                });

        displayAlertDialog_Button_Message_Center();
    }




    @Override
    public void onStop() {
        EventBus.getDefault().postSticky(new HideFABCart(false));

        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);

        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().postSticky(new HideFABCart(true));

    }



}

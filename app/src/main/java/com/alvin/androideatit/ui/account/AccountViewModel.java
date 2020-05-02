package com.alvin.androideatit.ui.account;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.alvin.androideatit.Common.Common;

public class AccountViewModel extends ViewModel {

    private MutableLiveData<Long> uid;
    private MutableLiveData<String> name;
    private MutableLiveData<String> phone;
    private MutableLiveData<String> address;

    public AccountViewModel() {
        name = new MutableLiveData<>();
        phone = new MutableLiveData<>();
        address = new MutableLiveData<>();
        uid = new MutableLiveData<>();

        name.setValue(Common.currentUser.getName());
        phone.setValue(Common.currentUser.getPhone());
        address.setValue(Common.currentUser.getAddress());
        uid.setValue(Common.currentUser.getCreateUID());
    }

    public MutableLiveData<Long> getUid() {
        return uid;
    }

    public MutableLiveData<String> getName() {
        return name;
    }

    public MutableLiveData<String> getPhone() {
        return phone;
    }

    public MutableLiveData<String> getAddress() {
        return address;
    }

}

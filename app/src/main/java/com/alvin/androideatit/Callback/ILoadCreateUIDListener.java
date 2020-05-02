package com.alvin.androideatit.Callback;

import com.alvin.androideatit.Model.UserModel;
import com.google.firebase.auth.FirebaseUser;

public interface ILoadCreateUIDListener {
    void onLoadCreateUIDSuccess(UserModel userModel, FirebaseUser user, long createUID);
    void onLoadCreateUIDFailed(String message);
}

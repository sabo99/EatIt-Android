package com.alvin.androideatit.ui.comments;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.alvin.androideatit.Model.CommentModel;

import java.util.List;

public class CommentViewModel extends ViewModel {

    private MutableLiveData<List<CommentModel>> listMutableLiveDataFoodList;

    public CommentViewModel() {
        listMutableLiveDataFoodList = new MutableLiveData<>();
    }

    public MutableLiveData<List<CommentModel>> getListMutableLiveDataFoodList() {
        return listMutableLiveDataFoodList;
    }

    public void setCommentList(List<CommentModel> commentList)
    {
        listMutableLiveDataFoodList.setValue(commentList);
    }
}

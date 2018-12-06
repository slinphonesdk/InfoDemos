package com.udp.master2;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface PostRequestInterface
{

    @POST("device/register")
    Call<PostRequestCallModel> register(@Body RegisterModel model);

    @POST("device/save")
    Call<PostRequestCallModel> updateCallRecord(@Body UpdateRecordModel model);


    @POST("device/status")
    Call<PostRequestCallModel> updateStatus(@Body UpdateStatusModel model);

}

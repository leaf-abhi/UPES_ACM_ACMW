package org.upesacm.acmacmw.retrofit;

import org.upesacm.acmacmw.model.EmailMsg;
import org.upesacm.acmacmw.model.Member;
import org.upesacm.acmacmw.model.NewMember;

import java.util.HashMap;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface MembershipClient  {

    @PUT("unconfirmed_members/{id}.json")
    Call<NewMember> saveNewMemberData(@Path("id") String id, @Body NewMember newMember);

    @GET("unconfirmed_members/{id}.json")
    Call<NewMember> getNewMemberData(@Path("id")String id);

    @GET("acm_acmw_members/{id}.json")
    Call<Member> getMember(@Path("id")String id);

    @PUT("acm_acmw_members/{id}.json")
    Call<Member> createMember(@Path("id")String id,@Body Member member);

    @GET("otp_recepients.json")
    Call<HashMap<String,String>> getOTPRecipients();
    @Multipart
    @POST("/upesacmacmwapp/upload.php")
    Call<ResponseModel> uploadFile(@Part ("name") RequestBody name,@Part MultipartBody.Part filepart );

    @GET("email_msg.json")
    Call<EmailMsg> getEmailMsg();

}

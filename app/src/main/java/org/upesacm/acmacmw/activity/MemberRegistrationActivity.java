package org.upesacm.acmacmw.activity;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import org.upesacm.acmacmw.R;
import org.upesacm.acmacmw.fragment.member.registration.MemberRegistrationFragment;
import org.upesacm.acmacmw.fragment.member.registration.SapIdFragment;
import org.upesacm.acmacmw.fragment.payment.OtpConfirmationFragment;
import org.upesacm.acmacmw.fragment.payment.PaymentDetailsFragment;
import org.upesacm.acmacmw.fragment.payment.RecipientSelectFragment;
import org.upesacm.acmacmw.model.EmailMsg;
import org.upesacm.acmacmw.model.Member;
import org.upesacm.acmacmw.model.NewMember;
import org.upesacm.acmacmw.util.FirebaseConfig;
import org.upesacm.acmacmw.util.OTPSender;
import org.upesacm.acmacmw.util.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MemberRegistrationActivity extends AppCompatActivity implements
        SapIdFragment.FragmentInteractionListener,
        MemberRegistrationFragment.RegistrationResultListener,
        OtpConfirmationFragment.OnFragmentInteractionListener,
        RecipientSelectFragment.FragmentInteractionListener,
        PaymentDetailsFragment.OnFragmentInteractionListener {
    public static final String TAG = "MemberRegActivity";
    private static final String NEW_MEMBER_SAP_KEY = "sap key";
    private static final String NEW_MEMBER_KEY = "new Member key";
    private FrameLayout frameLayout;
    private Bundle tempStorage = new Bundle();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member_registration);
        frameLayout = findViewById(R.id.frame_layout_member_registration_activity);
        setCurrentFragment(SapIdFragment.newInstance(),false);
    }

    void setCurrentFragment(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(frameLayout.getId(),fragment);
        if(addToBackStack)
            ft.addToBackStack(null);
        ft.commit();
    }

    private void sendIDCard(String recipientEmail,String subject,String mailBody) {
        OTPSender sender=new OTPSender();
        sender.execute(mailBody,recipientEmail,subject);
    }

    @Override
    public void onSAPIDAvailable(final String sapId) {
        //Check if the SAP ID is alread registered in the ACM database
        FirebaseDatabase.getInstance().getReference()
                .child(FirebaseConfig.ACM_ACMW_MEMBERS)
                .child(sapId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        Member acmMember = dataSnapshot.getValue(Member.class);
                        if(acmMember==null) {
                            //Check if the details related to the sap id are in unconfirmed member list
                            FirebaseDatabase.getInstance().getReference()
                                    .child(FirebaseConfig.UNCONFIRMED_MEMBERS)
                                    .child(sapId)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            NewMember newMember = dataSnapshot.getValue(NewMember.class);
                                            if(newMember==null) {
                                                setCurrentFragment(new MemberRegistrationFragment(),false);
                                            } else {
                                                //Retrieve the fee recipient corresponding to the new member sap
                                                FirebaseDatabase.getInstance().getReference()
                                                        .child(FirebaseConfig.ACM_ACMW_MEMBERS)
                                                        .child(newMember.getRecipientSap())
                                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                                Member recipient = dataSnapshot.getValue(Member.class);
                                                                if(recipient!=null) {
                                                                    //TODO:  implement the logic to calculate amount to be paid here
                                                                    setCurrentFragment(PaymentDetailsFragment.newInstance(recipient,45),false);
                                                                } else {
                                                                    Toast.makeText(MemberRegistrationActivity.this,"failed to load recipient details",Toast.LENGTH_SHORT).show();
                                                                }
                                                            }

                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                                                databaseError.toException().printStackTrace();
                                                            }
                                                        });
                                            }
                                            tempStorage.putString(NEW_MEMBER_SAP_KEY,sapId);
                                        }
                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {
                                            databaseError.toException().printStackTrace();
                                        }
                                    });
                        } else {
                            Toast.makeText(MemberRegistrationActivity.this,"Alread a Member",Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        databaseError.toException().printStackTrace();
                    }
                });
    }

    @Override
    public void onRegistrationDataAvailable(int resultCode, NewMember newMember) {
        tempStorage.putParcelable(NEW_MEMBER_KEY,newMember);
        FirebaseDatabase.getInstance().getReference()
                .child(FirebaseConfig.REGISTRATION_OTP_RECIPIENT)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        Map<String,String> recipientMap = dataSnapshot.getValue(new GenericTypeIndicator<Map<String, String>>() {});
                        List<String> recipientSaps = new ArrayList<>(recipientMap.values());
                        setCurrentFragment(RecipientSelectFragment.newInstance(recipientSaps),false);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        databaseError.toException().printStackTrace();
                    }
                });
    }

    @Override
    public void onRecipientSelect(final Member recipient) {
        NewMember newMember = new NewMember.Builder((NewMember)tempStorage.getParcelable(NEW_MEMBER_KEY))
                .setRecipientSap(recipient.getSap())
                .build();
        FirebaseDatabase.getInstance().getReference()
                .child(FirebaseConfig.UNCONFIRMED_MEMBERS)
                .child(newMember.getSapId())
                .setValue(newMember)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()) {
                            //TODO: obtain the correct amount to be paid by the new member
                            setCurrentFragment(PaymentDetailsFragment.newInstance(recipient,26),false);
                        }
                    }
                });
    }

    @Override
    public void onClickNext(Member recipient) {
        String sapId = tempStorage.getString(NEW_MEMBER_SAP_KEY);
        String otpUrl = FirebaseConfig.UNCONFIRMED_MEMBERS+"/"+
                sapId+"/"+
                FirebaseConfig.MEMBER_OTP;
        Log.i(TAG,"otp url : "+otpUrl);
        setCurrentFragment(OtpConfirmationFragment.newInstance(otpUrl),false);
    }

    @Override
    public void onOtpConfirmationResult(boolean confirmed) {
        if(confirmed) {
            final String sap = tempStorage.getString(NEW_MEMBER_SAP_KEY);
            FirebaseDatabase.getInstance().getReference()
                    .child(FirebaseConfig.UNCONFIRMED_MEMBERS)
                    .child(sap)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            NewMember newMember = dataSnapshot.getValue(NewMember.class);
                            final Member member = new Member.Builder(newMember).build();
                            //create a new entry in the acm members database
                            FirebaseDatabase.getInstance().getReference()
                                    .child(FirebaseConfig.ACM_ACMW_MEMBERS)
                                    .child(newMember.getSapId())
                                    .setValue(member)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(task.isSuccessful()) {
                                                SessionManager.getInstance().createMemberSession(member);
                                                //Obtain the mail to be sent from the database and send it
                                                FirebaseDatabase.getInstance().getReference()
                                                        .child(FirebaseConfig.EMAIL_MSG)
                                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                                EmailMsg message = dataSnapshot.getValue(EmailMsg.class);
                                                                String memberDetails =
                                                                        "<b>Name</b>      : " + member.getName() + "<br />"
                                                                                + "<b>SAP ID</b>    : " + member.getSap() + "<br />"
                                                                                + "<b>ACM ID</b>    : " + member.getMemberId() + "<br />"
                                                                                + "<b>Password</b>  : " + member.getPassword() + "<br />" +
                                                                                "(Please set your own password from the app)" + "<br />"
                                                                                + "<b>Branch</b>    : " + member.getBranch() + "<br />"
                                                                                + "<b>Year</b>      : " + member.getYear() + "<br />"
                                                                                + "<b>Contact</b>   : " + member.getContact() + "<br />"
                                                                                + "<b>WhatsApp</b>  : " + member.getWhatsappNo() + "<br />"
                                                                                + "<b>DOB</b>       : " + member.getDob() + "<br />"
                                                                                + "<b>Address</b>   : " + member.getCurrentAdd() + "<br />";
                                                                if(message!=null) {
                                                                    String mailBody = message.getBody()+"<br /><br />"+memberDetails+"<br />"+message.getSenderDetails();

                                                                    sendIDCard(member.getSap() + "@" + getString(R.string.upes_domain),message.getSubject(),
                                                                            mailBody);
                                                                }
                                                                else
                                                                    sendIDCard(member.getSap()+"@"+ getString(R.string.upes_domain),
                                                                            "ACM Member Details",memberDetails);
                                                            }

                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                                            }
                                                        });

                                                //Delete the entry from the unconfirmed members database
                                                FirebaseDatabase.getInstance().getReference()
                                                        .child(FirebaseConfig.UNCONFIRMED_MEMBERS)
                                                        .child(sap)
                                                        .setValue(null);
                                                MemberRegistrationActivity.this.finish();
                                            }
                                        }
                                    });
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            databaseError.toException().printStackTrace();
                        }
                    });
        } else {
            Toast.makeText(this,"Max tries exceeded",Toast.LENGTH_SHORT).show();
        }
    }
}
package org.upesacm.acmacmw.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.upesacm.acmacmw.asynctask.OTPSender;
import org.upesacm.acmacmw.fragment.ImageUploadFragment;
import org.upesacm.acmacmw.fragment.LoginDialogFragment;
import org.upesacm.acmacmw.R;
import org.upesacm.acmacmw.fragment.HomePageFragment;
import org.upesacm.acmacmw.fragment.MemberRegistrationFragment;
import org.upesacm.acmacmw.fragment.OTPVerificationFragment;
import org.upesacm.acmacmw.fragment.homepage.PostsFragment;
import org.upesacm.acmacmw.model.Member;
import org.upesacm.acmacmw.model.NewMember;
import org.upesacm.acmacmw.retrofit.HomePageClient;
import org.upesacm.acmacmw.retrofit.MembershipClient;
import org.upesacm.acmacmw.util.MemberIDGenerator;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

public class HomeActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener,
        LoginDialogFragment.InteractionListener,
        MemberRegistrationFragment.RegistrationResultListener,
        OTPVerificationFragment.OTPVerificationResultListener,
        PostsFragment.HomeFragmentInteractionListener,
        ImageUploadFragment.UploadResultListener,
        View.OnClickListener{
    private static final String BASE_URL="https://acm-acmw-app-6aa17.firebaseio.com/";

    Toolbar toolbar;
    DrawerLayout drawerLayout;
    ActionBarDrawerToggle toggle;
    FragmentManager fragmentManager;
    NavigationView navigationView;
    Retrofit retrofit;
    HomePageClient homePageClient;
    MembershipClient membershipClient;
    Member signedInMember;
    View headerLayout;
    String newMemberSap;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_main);
        toolbar = findViewById(R.id.my_toolbar);
        drawerLayout=findViewById(R.id.drawer_layout);
        fragmentManager=getSupportFragmentManager();

        retrofit=new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(JacksonConverterFactory.create())
                .build();
        homePageClient =retrofit.create(HomePageClient.class);
        membershipClient=retrofit.create(MembershipClient.class);


        /* *****************Setting up home page fragment ***********************/
        FragmentTransaction fragmentTransaction=fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout,HomePageFragment.newInstance(homePageClient,
                this),"homepage");
        fragmentTransaction.commit();
        /* *********************************************************************************/

        navigationView=findViewById(R.id.nav_view);

        /* *************************Setting the the action bar *****************************/
        setSupportActionBar(toolbar);
        toggle=new ActionBarDrawerToggle(this,drawerLayout,toolbar,
                R.string.drawer_opened, R.string.drawer_closed) ;
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        /* **********************************************************************************/


        navigationView.setNavigationItemSelectedListener(this);
        headerLayout=navigationView.getHeaderView(0);
        Button signin=headerLayout.findViewById(R.id.button_sign_in);
        signin.setOnClickListener(this);

        SharedPreferences preferences=getPreferences(Context.MODE_PRIVATE);
        String signedInMemberSap=preferences.getString(getString(R.string.logged_in_member_key),null);
        if(signedInMemberSap!=null) {
            membershipClient.getMember(signedInMemberSap)
                    .enqueue(new Callback<Member>() {
                        @Override
                        public void onResponse(Call<Member> call, Response<Member> response) {
                            signedInMember=response.body();
                            if(signedInMember!=null) {
                                setUpMemberProfile(signedInMember);
                            }
                        }

                        @Override
                        public void onFailure(Call<Member> call, Throwable t) {
                            System.out.println("failed to fetch signed in member details");
                        }
                    });
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        System.out.println("onNaviagationItemSelected");
        return true;
    }

    @Override
    public void onLoginPressed(LoginDialogFragment loginDialogFragment) {
        System.out.println("login button pressed");
        final String username=loginDialogFragment.getUsername();
        final String password=loginDialogFragment.getPassword();
        System.out.println("login user name : "+username);
        System.out.println("login password : "+password);

        Call<Member> memberCall=membershipClient.getMember(username);
        memberCall.enqueue(new Callback<Member>() {
            @Override
            public void onResponse(Call<Member> call, Response<Member> response) {
                Member member=response.body();
                String msg="";
                if(member!=null) {
                    if(member.getPassword().equals(password)) {
                        setUpMemberProfile(member);
                        msg="Successfully signed in";
                    }
                    else {
                        msg="Incorrect Username or password";
                    }
                }
                else {
                    msg="Incorrect Username or password";
                }
                Toast.makeText(HomeActivity.this,msg,Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(Call<Member> call, Throwable t) {
                Toast.makeText(HomeActivity.this,"Unable to verify",Toast.LENGTH_SHORT).show();
            }
        });
        loginDialogFragment.dismiss();
    }

    @Override
    public void onSignUpPressed(LoginDialogFragment loginDialogFragment) {
        System.out.println("sign up button pressed");
        loginDialogFragment.dismiss();

        /* **************** obtaining stored sap(if any)************************************* */
        SharedPreferences preferences=getPreferences(Context.MODE_PRIVATE);
        newMemberSap=preferences.getString(getString(R.string.new_member_sap_key),null);
        /* **************************************************************************************/
        System.out.println("stored sap id : "+newMemberSap);
        if(newMemberSap==null) {

            /* *****************Open the new member registration fragment here *************** */
            FragmentTransaction ft = fragmentManager.beginTransaction();
            ft.addToBackStack("homepage");
            ft.replace(R.id.frame_layout, MemberRegistrationFragment.newInstance(membershipClient, toolbar),
                    "member_registration_fragment");
            ft.commit();

            /* ******************************************************************************/
        }
        else {
            startOTPVerificationPage(null);
        }
        setDrawerEnabled(false);
    }

    @Override
    public void onCancelPressed(LoginDialogFragment loginDialogFragment) {
        System.out.println("Cancel button pressed");
        loginDialogFragment.dismiss();
    }

    public void setDrawerEnabled(boolean enable) {
        int lockMode=enable?DrawerLayout.LOCK_MODE_UNLOCKED:DrawerLayout.
                LOCK_MODE_LOCKED_CLOSED;
        drawerLayout.setDrawerLockMode(lockMode);
        toggle.setDrawerIndicatorEnabled(enable);

    }

    public void setActionBarTitle(String title) {
        toolbar.setTitle(title);
    }

    @Override
    public void onRegistrationDataSave(int resultCode, NewMember newMember) {
        System.out.println("result code is : "+resultCode);
        String msg="";
        if(resultCode==MemberRegistrationFragment.DATA_SAVE_SUCCESSFUL) {
            msg="Data Saved Successfully";
            String mailBody="name : "+newMember.getFullName()+"\n"
                            +"Email : "+newMember.getEmail()+"\n"
                            +"OTP : "+newMember.getOtp();
            OTPSender sender=new OTPSender();
            sender.execute(mailBody,"arkk.abhi1@gmail.com");
            startOTPVerificationPage(newMember);
        }
        else if(resultCode==MemberRegistrationFragment.NEW_MEMBER_ALREADY_PRESENT) {
            msg="New member data already present";
        }
        else if(resultCode==MemberRegistrationFragment.ALREADY_PART_OF_ACM) {
            msg="Alread a part of ACM";
        }
        else
            msg="Data save Failed. Please check your connection";

        Toast.makeText(this,msg,Toast.LENGTH_SHORT).show();
    }

    public void startOTPVerificationPage(NewMember newMember) {
        OTPVerificationFragment fragment;
        if(newMember!=null) {
            fragment=OTPVerificationFragment.newInstance(membershipClient, newMember.getSapId());
            Bundle bundle = new Bundle();
            bundle.putParcelable(getString(R.string.new_member_key), newMember);
            fragment.setArguments(bundle);
        }
        else {
            fragment=OTPVerificationFragment.newInstance(membershipClient,newMemberSap);
        }
        FragmentTransaction ft=fragmentManager.beginTransaction();
        ft.replace(R.id.frame_layout,fragment,"otp_verifiction");
        ft.commit();
    }

    @Override
    public void onSuccessfulVerification(final OTPVerificationFragment otpVerificationFragment) {
        System.out.println("successfully verified");
        NewMember verifiedNewMember=otpVerificationFragment.getVerifiedNewMember();
        fragmentManager.beginTransaction()
                .detach(otpVerificationFragment)
                .commit();
        final Member member=new Member.Builder()
                .setmemberId(MemberIDGenerator.generate(verifiedNewMember.getSapId()))
                .setName(verifiedNewMember.getFullName())
                .setPassword("somepassword")
                .setSAPId(verifiedNewMember.getSapId())
                .setBranch(verifiedNewMember.getBranch())
                .setEmail(verifiedNewMember.getEmail())
                .setContact(verifiedNewMember.getPhoneNo())
                .setYear(verifiedNewMember.getYear())
                .build();
        Call<Member> memberCall=membershipClient.createMember(verifiedNewMember.getSapId(),member);
        memberCall.enqueue(new Callback<Member>() {
            @Override
            public void onResponse(Call<Member> call, Response<Member> response) {
                System.out.println("new acm acm w member added");
                /* ********************Adding log in info locally ************************/
                SharedPreferences.Editor editor=getPreferences(Context.MODE_PRIVATE).edit();
                editor.putString(getString(R.string.logged_in_member_key),member.getSap());
                editor.commit();
                /* ************************************************************************* */
                Toast.makeText(HomeActivity.this,"Welocme to ACM/ACM-W",Toast.LENGTH_LONG).show();
                setUpMemberProfile(member);
                fragmentManager.beginTransaction()
                        .detach(otpVerificationFragment)
                        .commit();
                fragmentManager.popBackStackImmediate();
            }

            @Override
            public void onFailure(Call<Member> call, Throwable t) {
                System.out.println("failed to add new acm acmw member");
                fragmentManager.beginTransaction()
                        .detach(otpVerificationFragment)
                        .commit();
                fragmentManager.popBackStackImmediate();
            }
        });
    }

    @Override
    public void onMaxTriesExceed(OTPVerificationFragment otpVerificationFragment) {
        System.out.println("Max tries exceed");
        fragmentManager.beginTransaction()
                .detach(otpVerificationFragment)
                .commit();
    }

    @Override
    public void onNewPostDataAvailable(Bundle args) {
        System.out.println("on new post data available called");
        ImageUploadFragment imageUploadFragment=ImageUploadFragment.newInstance(homePageClient);
        imageUploadFragment.setArguments(args);

        FragmentTransaction ft=fragmentManager.beginTransaction();
        ft.addToBackStack("posts_fragment");
        ft.add(R.id.frame_layout,imageUploadFragment,"image_upload_fragment");
        ft.commit();
    }

    @Override
    public void onUpload(ImageUploadFragment imageUploadFragment,int resultCode) {
            fragmentManager.beginTransaction().detach(imageUploadFragment).commit();
    }

    void setUpMemberProfile(Member member){
        System.out.println("setting up member profile");
        this.signedInMember=member;

        /* ******* Change the header layout ********* */
        navigationView.removeHeaderView(headerLayout);
        navigationView.inflateHeaderView(R.layout.signed_in_header);
        /* ******************************************* */

        /* Setting the listener on the sign out button */
        headerLayout=navigationView.getHeaderView(0);
        Button signout=headerLayout.findViewById(R.id.button_sign_out);
        signout.setOnClickListener(this);

        TextView textViewUsername = headerLayout.findViewById(R.id.text_view_username);
        textViewUsername.setText(member.getName());
        /* ***********************************************************/

        /* *************** Adding personal corner for signed in members ***************************/
        Menu navdrawerMenu = navigationView.getMenu();
        Menu submenu = navdrawerMenu.addSubMenu(Menu.NONE,Menu.NONE,Menu.FIRST,"Personalized Corner");
        submenu.add("Edit Profile");
        submenu.add("item 2");
        navigationView.invalidate();
        /* ******************************************************************************************************/
    }

    void onSignOutClicked() {
        System.out.println("onSignOutclicked called");

        /* ******************* Clear the member data from the app ***********************/
        signedInMember=null;
        SharedPreferences.Editor editor=getPreferences(Context.MODE_PRIVATE).edit();
        editor.clear();
        editor.commit();
        /* **************************************************************************/

        /* ***Change the header layout and add again add the listener to sign in button  ******/
        navigationView.removeHeaderView(headerLayout);
        headerLayout = navigationView.inflateHeaderView(R.layout.nav_drawer_header);
        Button signin=headerLayout.findViewById(R.id.button_sign_in);
        signin.setOnClickListener(this);
        /* ************************************************************************************/

        /* *************** Adding the logged header and menu **************************/
        Menu navdrawerMenu = navigationView.getMenu();
        navdrawerMenu.clear();
        getMenuInflater().inflate(R.menu.navigationdrawer,navdrawerMenu);
        navigationView.invalidate();
        /* ******************************************************************************/
    }

    @Override
    public void onClick(View view) {
        if(view.getId()==R.id.button_sign_in) {
            LoginDialogFragment loginDialogFragment =new LoginDialogFragment();
            loginDialogFragment.show(fragmentManager,"fragment_login");
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        else if(view.getId()==R.id.button_sign_out) {
            onSignOutClicked();
        }
    }
}

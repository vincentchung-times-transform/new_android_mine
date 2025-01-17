package com.HKSHOPU.hk.ui.onboard.registeration.activity

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast

import androidx.lifecycle.Observer
import com.facebook.*
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.HKSHOPU.hk.Base.BaseActivity
import com.HKSHOPU.hk.Base.response.Status
import com.HKSHOPU.hk.R
import com.HKSHOPU.hk.databinding.ActivityBuildacntBinding
import com.HKSHOPU.hk.net.ApiConstants
import com.HKSHOPU.hk.net.Web
import com.HKSHOPU.hk.net.WebListener
import com.HKSHOPU.hk.ui.main.seller.shop.activity.ShopmenuActivity
import com.HKSHOPU.hk.ui.onboard.vm.AuthVModel
import com.HKSHOPU.hk.widget.view.KeyboardUtil
import com.tencent.mmkv.MMKV
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.*


class BuildAccountActivity : BaseActivity(), TextWatcher {
    private lateinit var binding: ActivityBuildacntBinding
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    val RC_SIGN_IN = 900;

    lateinit var callbackManager: CallbackManager
    private val VM = AuthVModel()
    private lateinit var settings: SharedPreferences
    var email: String = ""
    var password: String = ""
    var passwordconf: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBuildacntBinding.inflate(layoutInflater)
        setContentView(binding.root)

        settings = getSharedPreferences("DATA",0)

        callbackManager = CallbackManager.Factory.create()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestId()
            .requestEmail()
            .build()
        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        initView()
        initVM()
        initClick()
    }

    override fun afterTextChanged(s: Editable?) {
        email = binding.editEmailReg.text.toString()
        password = binding.passwordReg.text.toString()
        passwordconf = binding.passwordConf.text.toString()

        if (email.isEmpty() || password.isEmpty() || passwordconf.isEmpty()) {
            binding.imgViewNextStep.isEnabled = false
            binding.imgViewNextStep.setImageResource(R.mipmap.next_step_inable)
        } else {
            binding.imgViewNextStep.isEnabled = true
            binding.imgViewNextStep.setImageResource(R.mipmap.next_step)

        }
    }
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

    private fun initVM() {
        VM.emailcheckLiveData.observe(this, Observer {
            when (it?.status) {
                Status.Success -> {
                    if(android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                        if (it.ret_val!!.equals("該電子郵件沒有重複使用!")) {

                            settings.edit()
                                .putString("email", email)
                                .putString("password", password)
                                .putString("passwordconf", passwordconf)
                                .apply()
                            MMKV.mmkvWithID("http").putString("Email",email)

                            binding.progressBarBuildAccount.visibility = View.GONE
                            binding.ivLoadingBackgroundBuildAccount.visibility = View.GONE

                            val intent = Intent(this, UserIofoActivity::class.java)
                            startActivity(intent)

                        }else{
                            Toast.makeText(this, it.ret_val.toString(), Toast.LENGTH_SHORT).show()
                            binding.progressBarBuildAccount.visibility = View.GONE
                            binding.ivLoadingBackgroundBuildAccount.visibility = View.GONE
                        }
                    }else {
                        Toast.makeText(this, "電郵格式錯誤", Toast.LENGTH_SHORT).show()
                        binding.progressBarBuildAccount.visibility = View.GONE
                        binding.ivLoadingBackgroundBuildAccount.visibility = View.GONE
                    }

                }
//                Status.Start -> showLoading()
//                Status.Complete -> disLoading()
            }
        })
//        VM.socialloginLiveData.observe(this, Observer {
//            when (it?.status) {
//                Status.Success -> {
//                    Log.d("OnBoardActivity", "Sign-In Result" + it.ret_val)
//                    if (it.ret_val.toString().isNotEmpty()) {
//
//                        val intent = Intent(this, ShopmenuActivity::class.java)
//                        startActivity(intent)
//                        finish()
//
//                    } else {
//                        val intent = Intent(this, BuildAccountActivity::class.java)
//                        startActivity(intent)
//                        finish()
//                    }
//
//                }
////                Status.Start -> showLoading()
////                Status.Complete -> disLoading()
//            }
//        })
    }

    private fun initView() {
        binding.progressBarBuildAccount.visibility = View.GONE
        binding.ivLoadingBackgroundBuildAccount.visibility = View.GONE

        //imgViewNextStep預設不能按
        binding.imgViewNextStep.isEnabled = false

        initEditText()
        initClick()

        KeyboardUtil.showKeyboard(binding.editEmailReg)
    }
    private fun initClick() {
        binding.titleBack.setOnClickListener {
            finish()
        }
        binding.btnGoogleLogin.setOnClickListener {
            GoogleAccountBuild()
        }
        binding.btnFacebookLogin.setOnClickListener {
            LoginManager.getInstance().logInWithReadPermissions(
                this, Arrays.asList("public_profile", "email")
            )
            LoginManager.getInstance().registerCallback(callbackManager,
                object : FacebookCallback<LoginResult> {
                    override fun onSuccess(loginResult: LoginResult) {
                        val request =
                            GraphRequest.newMeRequest(loginResult.accessToken) { `object`, response ->
                                Log.d("BuildAccountActivity", response.toString())
                                try {

                                    // method_1.判斷用戶是否登入過
                                    if (Profile.getCurrentProfile() != null) {
                                        val profile: Profile = Profile.getCurrentProfile()
                                        // 取得用戶大頭照
                                        val userPhoto: Uri = profile.getProfilePictureUri(300, 300)
                                        val id: String = profile.getId()
                                        val name: String = profile.getName()
                                        Log.d("OnBoardActivity", "Facebook userPhoto: $userPhoto")
                                        Log.d("OnBoardActivity", "Facebook id: $id")
                                        Log.d("OnBoardActivity", "Facebook name: $name")
                                    }

                                    // method_2.判斷用戶是否登入過
                                    /*if (AccessToken.getCurrentAccessToken() != null) {
                                        Log.d(TAG, "Facebook getApplicationId: " + AccessToken.getCurrentAccessToken().getApplicationId());
                                        Log.d(TAG, "Facebook getUserId: " + AccessToken.getCurrentAccessToken().getUserId());
                                        Log.d(TAG, "Facebook getExpires: " + AccessToken.getCurrentAccessToken().getExpires());
                                        Log.d(TAG, "Facebook getLastRefresh: " + AccessToken.getCurrentAccessToken().getLastRefresh());
                                        Log.d(TAG, "Facebook getToken: " + AccessToken.getCurrentAccessToken().getToken());
                                        Log.d(TAG, "Facebook getSource: " + AccessToken.getCurrentAccessToken().getSource());
                                    }*/
                                    runOnUiThread {
                                        binding.progressBarBuildAccount.visibility = View.VISIBLE
                                        binding.ivLoadingBackgroundBuildAccount.visibility = View.VISIBLE
                                    }

                                    // Application code
                                    val id = response.jsonObject.getString("id")
                                    val email = response.jsonObject.getString("email")
                                    doSocialLogin(email,id,"","")

                                } catch (e: Exception) {
                                        e.printStackTrace()
                                    Log.d("BuildAccountActivity", "Exception: ${e.toString()}")
                                }
                            }
                        val parameters = Bundle()
                        parameters.putString("fields", "id,name,email,gender,birthday")
                        request.parameters = parameters
                        request.executeAsync()
                    }

                    override fun onCancel() {
                        Log.d("BuildAccountActivity", "Facebook onCancel.")
                    }

                    override fun onError(error: FacebookException) {
                        Log.d("BuildAccountActivity", "Facebook onError.")
                    }
                })
        }
        binding.showPassBtn.setOnClickListener {
            ShowHidePass(it)
        }
        binding.showPassconfBtn.setOnClickListener {
            ShowHidePass(it)
        }
        binding.imgViewNextStep.setOnClickListener {
            binding.progressBarBuildAccount.visibility = View.VISIBLE
            binding.ivLoadingBackgroundBuildAccount.visibility = View.VISIBLE

            val regex  = """^(?=.*?[A-Z])(?=(.*[a-z]){1,})(?=(.*[\d]){1,})(?=(.*[\W]){1,})(?!.*\s).{8,}${'$'}""".toRegex()
            if(regex.matches(password)||regex.matches(passwordconf)){
                if(password != passwordconf){
                    Toast.makeText(this, "密碼不一致", Toast.LENGTH_SHORT).show()
                    binding.progressBarBuildAccount.visibility = View.GONE
                    binding.ivLoadingBackgroundBuildAccount.visibility = View.GONE
                }else{
                    VM.emailCheck(this,email)
                }
            }else{
                Toast.makeText(this, "密碼格式錯誤", Toast.LENGTH_SHORT).show()
                binding.progressBarBuildAccount.visibility = View.GONE
                binding.ivLoadingBackgroundBuildAccount.visibility = View.GONE
            }
        }
        binding.tvAgreeterm.setOnClickListener {
            val intent = Intent(this, TermsOfServiceActivity::class.java)
            startActivity(intent)

        }
    }
    private fun initEditText() {
        binding.editEmailReg.addTextChangedListener(this)
        binding.passwordReg.addTextChangedListener(this)
        binding.passwordReg.setFilters(arrayOf<InputFilter>(LengthFilter(16)))
        binding.passwordReg.setTransformationMethod(PasswordTransformationMethod.getInstance())
        binding.passwordConf.addTextChangedListener(this)
        binding.passwordConf.setFilters(arrayOf<InputFilter>(LengthFilter(16)))
        binding.passwordConf.setTransformationMethod(PasswordTransformationMethod.getInstance())
    }

    private fun GoogleAccountBuild() {
        val signInIntent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager?.onActivityResult(requestCode, resultCode, data)
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                runOnUiThread {
                    binding.progressBarBuildAccount.visibility = View.VISIBLE
                    binding.ivLoadingBackgroundBuildAccount.visibility = View.VISIBLE
                }

                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)!!
                val email = account.email.toString()
                val id = account.id.toString()
                doSocialLogin(email,"",id,"")

            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Log.d("BuildAccountActivity", "Google sign in failed", e)

                runOnUiThread {
                    binding.progressBarBuildAccount.visibility = View.GONE
                    binding.ivLoadingBackgroundBuildAccount.visibility = View.GONE
                }
            }
        }
    }
    private fun doSocialLogin(email: String, facebook_account: String, google_account: String, apple_account: String) {
        var url = ApiConstants.API_PATH+"user/socialLoginProcess/"
        var user_id: String = "0"
        val web = Web(object : WebListener {
            override fun onResponse(response: Response) {
                var resStr: String? = ""
                var ret_val: Any = ""
                var status: Any = 999
                try {
                    resStr = response.body()!!.string()
                    val json = JSONObject(resStr)

                    Log.d("doSocialLogin", "返回資料 resStr：" + resStr)
                    Log.d("doSocialLogin", "返回資料 ret_val：" + json.get("ret_val"))

                    ret_val = json.get("ret_val")
                    status = json.get("status")

                    if (status != 0) {
                        Log.d("doSocialLogin", "ret_val: ${ret_val.toString()}")

                        user_id= json.getString("user_id")
                        doBackendUserIDValidation(user_id, email)

                        doInsertAuditLog(user_id,
                            "第三方登入/doSocialLogin()",
                            "email: ${email.toString()} ; " +
                                    "facebook_account: ${facebook_account} ; " +
                                    "google_account : ${google_account} ; " +
                                    "apple_account : ${apple_account} ; ",
                            json.get("ret_val").toString()
                        )

                    } else {

                        user_id = json.getString("user_id")
                        doInsertAuditLog(user_id,
                            "第三方登入/doSocialLogin()",
                            "email: ${email.toString()} ; " +
                                    "facebook_account: ${facebook_account} ; " +
                                    "google_account : ${google_account} ; " +
                                    "apple_account : ${apple_account} ; ",
                            json.get("ret_val").toString()
                        )

                        Log.d("doSocialLogin", "ret_val: ${ret_val.toString()}")
                        runOnUiThread {
                            Toast.makeText(this@BuildAccountActivity, ret_val.toString(), Toast.LENGTH_SHORT).show()
                            binding.progressBarBuildAccount.visibility = View.GONE
                            binding.ivLoadingBackgroundBuildAccount.visibility = View.GONE
                        }

                        val intent = Intent(this@BuildAccountActivity, BuildAccountActivity::class.java)
                        startActivity(intent)
                        finish()
                    }

                } catch (e: JSONException) {
                    doInsertAuditLog(user_id,
                        "第三方登入/doSocialLogin()",
                        "email: ${email.toString()} ; " +
                                "facebook_account: ${facebook_account} ; " +
                                "google_account : ${google_account} ; " +
                                "apple_account : ${apple_account} ; ",
                        e.toString()
                    )

                    Log.d("doSocialLogin", "JSONException: ${e.toString()}")
                    runOnUiThread {
                        Toast.makeText(this@BuildAccountActivity, "網路異常請重新登入", Toast.LENGTH_SHORT).show()
                        binding.progressBarBuildAccount.visibility = View.GONE
                        binding.ivLoadingBackgroundBuildAccount.visibility = View.GONE
                    }

                } catch (e: IOException) {
                    e.printStackTrace()
                    doInsertAuditLog(user_id,
                        "第三方登入/doSocialLogin()",
                        "email: ${email.toString()} ; " +
                                "facebook_account: ${facebook_account} ; " +
                                "google_account : ${google_account} ; " +
                                "apple_account : ${apple_account} ; ",
                        e.toString()
                    )

                    Log.d("doSocialLogin", "IOException: ${e.toString()}")
                    runOnUiThread {
                        Toast.makeText(this@BuildAccountActivity, "網路異常請重新登入", Toast.LENGTH_SHORT).show()
                        binding.progressBarBuildAccount.visibility = View.GONE
                        binding.ivLoadingBackgroundBuildAccount.visibility = View.GONE
                    }
                }
            }

            override fun onErrorResponse(ErrorResponse: IOException?) {
                doInsertAuditLog(user_id,
                    "第三方登入/doSocialLogin()",
                    "email: ${email.toString()} ; " +
                            "facebook_account: ${facebook_account} ; " +
                            "google_account : ${google_account} ; " +
                            "apple_account : ${apple_account} ; ",
                    ErrorResponse.toString()
                )
                runOnUiThread {
                    Toast.makeText(this@BuildAccountActivity, "網路異常請重新登入", Toast.LENGTH_SHORT).show()
                    Log.d("doSocialLogin", "ErrorResponse: ${ErrorResponse.toString()}")
                    binding.progressBarBuildAccount.visibility = View.GONE
                    binding.ivLoadingBackgroundBuildAccount.visibility = View.GONE
                }
            }
        })
        web.Do_SocialLogin(url, email,facebook_account,google_account, apple_account)
    }
    private fun doBackendUserIDValidation(user_id: String, email: String) {

        var url = ApiConstants.API_PATH+"user/user_id_validation/"

        val web = Web(object : WebListener {
            override fun onResponse(response: Response) {
                var resStr: String? = ""
                try {

                    resStr = response.body()!!.string()
                    val json = JSONObject(resStr)

                    Log.d("doBackendUserIDValidation", "返回資料 resStr：" + resStr)
//                    Log.d("doInsertAuditLog", "返回資料 ret_val：" + json.get("ret_val"))

                    val ret_val = json.get("ret_val")
                    val status = json.get("status")

                    if (status == 0) {
                        if (ret_val.equals("該使用者存在!")){
                            Log.d("doBackendUserIDValidation", "該使用者存在!")

                            MMKV.mmkvWithID("http").putString("UserId", user_id)
                                .putString("Email",email)

                            runOnUiThread {
                                binding.progressBarBuildAccount.visibility = View.GONE
                                binding.ivLoadingBackgroundBuildAccount.visibility = View.GONE
                            }

                            val intent = Intent(this@BuildAccountActivity, ShopmenuActivity::class.java)
                            startActivity(intent)
                            finish()

                        }else{
                            Log.d("doBackendUserIDValidation", "該使用者不存在!")
                            runOnUiThread {
                                Toast.makeText(this@BuildAccountActivity, "該使用者不存在!", Toast.LENGTH_SHORT).show()
                                binding.progressBarBuildAccount.visibility = View.GONE
                                binding.ivLoadingBackgroundBuildAccount.visibility = View.GONE
                            }
                        }
                    }

                } catch (e: JSONException) {
                    Log.d("doBackendUserIDValidation", "JSONException: ${e.toString()}")
                    runOnUiThread {
                        Toast.makeText(this@BuildAccountActivity, "網路異常請重新登入", Toast.LENGTH_SHORT).show()
                        binding.progressBarBuildAccount.visibility = View.GONE
                        binding.ivLoadingBackgroundBuildAccount.visibility = View.GONE
                    }

                } catch (e: IOException) {
                    e.printStackTrace()
                    Log.d("doBackendUserIDValidation", "IOException: ${e.toString()}")
                    runOnUiThread {
                        Toast.makeText(this@BuildAccountActivity, "網路異常請重新登入", Toast.LENGTH_SHORT).show()
                        binding.progressBarBuildAccount.visibility = View.GONE
                        binding.ivLoadingBackgroundBuildAccount.visibility = View.GONE
                    }
                }
            }

            override fun onErrorResponse(ErrorResponse: IOException?) {
                Log.d("doBackendUserIDValidation", "ErrorResponse: ${ErrorResponse.toString()}")
                runOnUiThread {
                    Toast.makeText(this@BuildAccountActivity, "網路異常請重新登入", Toast.LENGTH_SHORT).show()
                    binding.progressBarBuildAccount.visibility = View.GONE
                    binding.ivLoadingBackgroundBuildAccount.visibility = View.GONE
                }
            }
        })
        web.doBackendUserIDValidation(url, user_id)
    }
    private fun doInsertAuditLog(user_id: String ,action: String, parameter_in: String, parameter_out: String) {

        var url = ApiConstants.API_PATH+"user/${user_id}/auditLog/"

        val web = Web(object : WebListener {
            override fun onResponse(response: Response) {
                var resStr: String? = ""

                try {
                    resStr = response.body()!!.string()
                    val json = JSONObject(resStr)
                    val ret_val = json.get("ret_val")
                    val status = json.get("status")

                    Log.d("doInsertAuditLog", "返回資料 resStr：" + resStr)
                    Log.d("doInsertAuditLog", "返回資料 ret_val：" + json.get("ret_val"))

                    if (status == 0) {
                        if (ret_val.equals("新增成功")){
                            Log.d("doInsertAuditLog", "訊息狀態：訊息已送出!!")
                        }else{
                            Log.d("doInsertAuditLog", "訊息狀態：訊息尚未送出~")
                        }
                    }

                } catch (e: JSONException) {
                    Log.d("doInsertAuditLog", "JSONException： ${e.toString()}")
                } catch (e: IOException) {
                    e.printStackTrace()
                    Log.d("doInsertAuditLog", "IOException： ${e.toString()}")
                }
            }
            override fun onErrorResponse(ErrorResponse: IOException?) {
                Log.d("doInsertAuditLog", "ErrorResponse： ${ErrorResponse.toString()}")
            }
        })
        web.InsertAuditLog(url, action,parameter_in,parameter_out)
    }

    fun ShowHidePass(view: View) {
        if (view.getId() == R.id.show_pass_btn) {
            if (binding.passwordReg.getTransformationMethod()
                    .equals(PasswordTransformationMethod.getInstance())
            ) {
                (view as ImageView).setImageResource(R.mipmap.ic_eyeon)
                //Show Password
                binding.passwordReg.setTransformationMethod(HideReturnsTransformationMethod.getInstance())
            } else {
                (view as ImageView).setImageResource(R.mipmap.ic_eyeoff)
                //Hide Password
                binding.passwordReg.setTransformationMethod(PasswordTransformationMethod.getInstance())
            }
        }
        if (view.getId() == R.id.show_passconf_btn) {
            if (binding.passwordConf.getTransformationMethod()
                    .equals(PasswordTransformationMethod.getInstance())
            ) {
                (view as ImageView).setImageResource(R.mipmap.ic_eyeon)
                //Show Password
                binding.passwordConf.setTransformationMethod(HideReturnsTransformationMethod.getInstance())
            } else {
                (view as ImageView).setImageResource(R.mipmap.ic_eyeoff)
                //Hide Password
                binding.passwordConf.setTransformationMethod(PasswordTransformationMethod.getInstance())
            }
        }
    }


}
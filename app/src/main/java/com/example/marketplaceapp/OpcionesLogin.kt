package com.example.marketplaceapp

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.example.marketplaceapp.Opciones_login.Login_email
import com.example.marketplaceapp.databinding.ActivityOpcionesLoginBinding

class OpcionesLogin : AppCompatActivity() {

    private lateinit var binding : ActivityOpcionesLoginBinding

    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var mGoogleSignInClient : GoogleSignInClient
    private lateinit var progressDialog : ProgressDialog

    private companion object {
        private const val TAG = "OpcionesLogin"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOpcionesLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Espere por favor")
        progressDialog.setCanceledOnTouchOutside(false)

        firebaseAuth = FirebaseAuth.getInstance()
        comprobarSesion()

        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

            mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
        } catch (e: Exception) {
            Log.e(TAG, "Error configurando Google Sign In: ${e.message}", e)
            Toast.makeText(this, 
                "Error al configurar Google Sign In. Verifica tu configuración.", 
                Toast.LENGTH_LONG).show()
        }

        binding.IngresarEmail.setOnClickListener {
            startActivity(Intent(this@OpcionesLogin, Login_email::class.java))
        }

        binding.IngresarGoogle.setOnClickListener {
            googleLogin()
        }
    }

    private fun googleLogin() {
        try {
            val googleSignInIntent = mGoogleSignInClient.signInIntent
            googleSignInARL.launch(googleSignInIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error al iniciar Google Sign In: ${e.message}", e)
            Toast.makeText(this, 
                "Error al iniciar sesión con Google: ${e.message}", 
                Toast.LENGTH_SHORT).show()
        }
    }

    private val googleSignInARL = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        val data = resultado.data
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val cuenta = task.getResult(ApiException::class.java)
            if (cuenta?.idToken != null) {
                autenticacionGoogle(cuenta.idToken)
            } else {
                Log.e(TAG, "idToken es null")
                Toast.makeText(this, "Error: No se pudo obtener el token de Google", Toast.LENGTH_SHORT).show()
            }
        } catch (apiEx: ApiException) {
            // <-- línea importante para diagnóstico
            Log.e(TAG, "Google Sign In failed. statusCode=${apiEx.statusCode}, message=${apiEx.message}", apiEx)
            Toast.makeText(this, "Inicio de sesión falló (code=${apiEx.statusCode})", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error desconocido en Google Sign In: ${e.message}", e)
            Toast.makeText(this, "Error al iniciar sesión: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun autenticacionGoogle(idToken: String?) {
        if (idToken == null) {
            Toast.makeText(this, "Error: Token inválido", Toast.LENGTH_SHORT).show()
            return
        }
        
        progressDialog.setMessage("Autenticando con Google...")
        progressDialog.show()
        
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnSuccessListener {resultadoAuth->
                if (resultadoAuth.additionalUserInfo!!.isNewUser){
                    llenarInfoBD()
                }else{
                    progressDialog.dismiss()
                    startActivity(Intent(this, MainActivity::class.java))
                    finishAffinity()
                }
            }
            .addOnFailureListener { e->
                progressDialog.dismiss()
                Log.e(TAG, "Error en autenticación Firebase: ${e.message}", e)
                Toast.makeText(this, 
                    "Error de autenticación: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            }
    }

    private fun llenarInfoBD() {
        progressDialog.setMessage("Guardando información")

        val tiempo = Constantes.obtenerTiempoDis()
        val emailUsuario = firebaseAuth.currentUser!!.email
        val uidUsuario = firebaseAuth.uid
        val nombreUsuario = firebaseAuth.currentUser?.displayName

        val hashMap = HashMap<String, Any>()
        hashMap["nombres"] = "${nombreUsuario}"
        hashMap["codigoTelefono"] = ""
        hashMap["telefono"] = ""
        hashMap["urlImagenPerfil"] = ""
        hashMap["proveedor"] = "Google"
        hashMap["escribiendo"] = ""
        hashMap["tiempo"] = tiempo
        hashMap["estado"] = "online"
        hashMap["email"] = "${emailUsuario}"
        hashMap["uid"] = "${uidUsuario}"
        hashMap["fecha_nac"] = ""

        val ref = FirebaseDatabase.getInstance().getReference("Usuarios")
        ref.child(uidUsuario!!)
            .setValue(hashMap)
            .addOnSuccessListener {
                progressDialog.dismiss()
                startActivity(Intent(this, MainActivity::class.java))
                finishAffinity()
            }
            .addOnFailureListener { e->
                progressDialog.dismiss()
                Toast.makeText(
                    this,
                    "No se registró debido a ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun comprobarSesion(){
        if (firebaseAuth.currentUser != null){
            startActivity(Intent(this, MainActivity::class.java))
            finishAffinity()
        }
    }
}

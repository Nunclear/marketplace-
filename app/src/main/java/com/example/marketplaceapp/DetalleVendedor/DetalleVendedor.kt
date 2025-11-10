package com.example.marketplaceapp.DetalleVendedor

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.example.marketplaceapp.Adaptadores.AdaptadorAnuncio
import com.example.marketplaceapp.Comentarios
import com.example.marketplaceapp.Constantes
import com.example.marketplaceapp.Modelo.ModeloAnuncio
import com.example.marketplaceapp.R
import com.example.marketplaceapp.databinding.ActivityDetalleVendedorBinding

class DetalleVendedor : AppCompatActivity() {

    private lateinit var binding : ActivityDetalleVendedorBinding
    private var uidVendedor = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetalleVendedorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        uidVendedor = intent.getStringExtra("uidVendedor").toString()

        cargarInfoVendedor()
        cargarAnunciosVendedor()


        binding.IbRegresar.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.IvComentarios.setOnClickListener {
            val intent = Intent(this, Comentarios::class.java)
            intent.putExtra("uidVendedor", uidVendedor)
            startActivity(intent)
        }

    }

    private fun cargarAnunciosVendedor(){
        val anuncioArrayList : ArrayList<ModeloAnuncio> = ArrayList()

        val ref = FirebaseDatabase.getInstance().getReference("Anuncios")
        ref.orderByChild("uid").equalTo(uidVendedor)
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    anuncioArrayList.clear()
                    for (ds in snapshot.children){
                        try {
                            val modeloAnuncio = ds.getValue(ModeloAnuncio::class.java)
                            anuncioArrayList.add(modeloAnuncio!!)
                        }catch (e:Exception){

                        }
                    }

                    val adaptador = AdaptadorAnuncio(this@DetalleVendedor, anuncioArrayList)
                    binding.anunciosRv.adapter = adaptador

                    val contadorAnuncios = "${anuncioArrayList.size}"
                    binding.TvNumAnuncios.text = contadorAnuncios
                }

                override fun onCancelled(error: DatabaseError) {
                    // Error loading seller ads
                }
            })
    }

    private fun cargarInfoVendedor(){
        val ref = FirebaseDatabase.getInstance().getReference("Usuarios")
        ref.child(uidVendedor)
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    val nombres = "${snapshot.child("nombres").value}"
                    val imagen = "${snapshot.child("urlImagenPerfil").value}"
                    val tiempo_r = snapshot.child("tiempo").value as Long

                    val f_fecha = Constantes.obtenerFecha(tiempo_r)

                    binding.TvNombres.text = nombres
                    binding.TvMiembro.text = f_fecha

                    try {
                        Glide.with(this@DetalleVendedor)
                            .load(imagen)
                            .placeholder(R.drawable.img_perfil)
                            .into(binding.IvVendedor)
                    }catch (e:Exception){

                    }

                }

                override fun onCancelled(error: DatabaseError) {
                    // Error loading seller info
                }
            })
    }
}

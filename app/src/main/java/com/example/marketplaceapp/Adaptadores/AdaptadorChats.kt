package com.example.marketplaceapp.Adaptadores

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.example.marketplaceapp.Chat.BuscarChat
import com.example.marketplaceapp.Chat.ChatActivity
import com.example.marketplaceapp.Constantes
import com.example.marketplaceapp.Modelo.ModeloChats
import com.example.marketplaceapp.R
import com.example.marketplaceapp.databinding.ItemChatsBinding
import android.util.Log

class AdaptadorChats : RecyclerView.Adapter<AdaptadorChats.HolderChats>, Filterable {

    private var context : Context
    var chatsArrayList : ArrayList<ModeloChats>
    private lateinit var firebaseAuth: FirebaseAuth
    private var miUid = ""
    private var filtroLista : ArrayList<ModeloChats>
    private var filtro : BuscarChat?=null

    constructor(context: Context, chatsArrayList: ArrayList<ModeloChats>) {
        this.context = context
        this.chatsArrayList = chatsArrayList
        this.filtroLista = chatsArrayList
        firebaseAuth = FirebaseAuth.getInstance()
        miUid = firebaseAuth.uid ?: ""
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderChats {
        val binding = ItemChatsBinding.inflate(LayoutInflater.from(context), parent, false)
        return HolderChats(binding)
    }

    override fun getItemCount(): Int {
        return chatsArrayList.size
    }

    override fun onBindViewHolder(holder: HolderChats, position: Int) {
        if (position < 0 || position >= chatsArrayList.size) {
            Log.e("AdaptadorChats", "Posición inválida: $position")
            return
        }
        
        val modeloChats = chatsArrayList[position]
        
        if (modeloChats.keyChat.isEmpty()) {
            Log.e("AdaptadorChats", "keyChat vacío en posición: $position")
            return
        }

        cargarUltimoMensaje(modeloChats, holder)

        holder.itemView.setOnClickListener {
            val uidRecibimos = modeloChats.uidRecibimos
            if (!uidRecibimos.isNullOrEmpty() && uidRecibimos != "null"){
                val intent = Intent(context, ChatActivity::class.java)
                intent.putExtra("uidVendedor", uidRecibimos)
                context.startActivity(intent)
            } else {
                Log.e("AdaptadorChats", "UID receptor inválido al hacer click")
            }
        }
    }

    private fun cargarUltimoMensaje(modeloChats: ModeloChats, holder: HolderChats) {
        val chatKey = modeloChats.keyChat
        
        Log.d("AdaptadorChats", "Cargando último mensaje para chat: $chatKey")

        val ref = FirebaseDatabase.getInstance().getReference("Chats")
        ref.child(chatKey).limitToLast(1)
            .addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        Log.e("AdaptadorChats", "No existen mensajes en el chat: $chatKey")
                        return
                    }
                    
                    for (ds in snapshot.children){
                        try {
                            val emisorUid = ds.child("emisorUid").value?.toString() ?: ""
                            val idMensaje = ds.child("idMensaje").value?.toString() ?: ""
                            val mensaje = ds.child("mensaje").value?.toString() ?: ""
                            val receptorUid = ds.child("receptorUid").value?.toString() ?: ""
                            val tiempo = try {
                                ds.child("tiempo").value as? Long ?: 0L
                            } catch (e: Exception) {
                                Log.e("AdaptadorChats", "Error al convertir tiempo: ${e.message}")
                                0L
                            }
                            val tipo_mensaje = ds.child("tipoMensaje").value?.toString() ?: ""

                            if (emisorUid.isEmpty() || receptorUid.isEmpty()) {
                                Log.e("AdaptadorChats", "UIDs vacíos en mensaje")
                                return
                            }

                            val formato_fecha_hora = Constantes.obtenerFechaHora(tiempo)

                            modeloChats.emisorUid = emisorUid
                            modeloChats.idMensaje = idMensaje
                            modeloChats.mensaje = mensaje
                            modeloChats.receptorUid = receptorUid
                            modeloChats.tiempo = tiempo
                            modeloChats.tipoMensaje = tipo_mensaje

                            holder.binding.TvFecha.text = formato_fecha_hora

                            if (tipo_mensaje == Constantes.MENSAJE_TIPO_TEXTO){
                                holder.binding.TvUltimoMensaje.text = mensaje
                            } else {
                                holder.binding.TvUltimoMensaje.text = "Se ha enviado una imagen"
                            }

                            cargarInfoUsuarioRecibimos(modeloChats, holder)
                            
                            Log.d("AdaptadorChats", "Último mensaje cargado exitosamente para: $chatKey")

                        } catch (e: Exception) {
                            Log.e("AdaptadorChats", "Error al procesar mensaje: ${e.message}", e)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("AdaptadorChats", "Error al cargar último mensaje: ${error.message}")
                }
            })
    }

    private fun cargarInfoUsuarioRecibimos(modeloChats: ModeloChats, holder: HolderChats) {
        val emisorUid = modeloChats.emisorUid
        val receptorUid = modeloChats.receptorUid

        val uidRecibimos = if (emisorUid == miUid) {
            receptorUid
        } else {
            emisorUid
        }

        if (uidRecibimos.isEmpty() || uidRecibimos == "null" || uidRecibimos == miUid) {
            Log.e("AdaptadorChats", "UID inválido o es el mismo usuario: $uidRecibimos")
            return
        }

        modeloChats.uidRecibimos = uidRecibimos
        
        Log.d("AdaptadorChats", "Cargando información del usuario: $uidRecibimos")

        val ref = FirebaseDatabase.getInstance().getReference("Usuarios")
        ref.child(uidRecibimos)
            .addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        Log.e("AdaptadorChats", "Usuario no existe en Firebase: $uidRecibimos")
                        return
                    }
                    
                    try {
                        val nombres = snapshot.child("nombres").value?.toString() ?: "Usuario"
                        val imagen = snapshot.child("urlImagenPerfil").value?.toString() ?: ""

                        modeloChats.nombres = nombres
                        modeloChats.urlImagenPerfil = imagen

                        holder.binding.TvNombres.text = nombres

                        try {
                            Glide.with(context)
                                .load(imagen)
                                .placeholder(R.drawable.img_perfil)
                                .error(R.drawable.img_perfil)
                                .into(holder.binding.IvPerfil)
                        } catch (e: Exception) {
                            Log.e("AdaptadorChats", "Error al cargar imagen con Glide: ${e.message}")
                            holder.binding.IvPerfil.setImageResource(R.drawable.img_perfil)
                        }
                        
                        Log.d("AdaptadorChats", "Información del usuario cargada: $nombres")

                    } catch (e: Exception) {
                        Log.e("AdaptadorChats", "Error al procesar datos del usuario: ${e.message}", e)
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    Log.e("AdaptadorChats", "Error al cargar información del usuario: ${error.message}")
                }
            })
    }

    inner class HolderChats(val binding: ItemChatsBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getFilter(): Filter {
        if (filtro == null){
            filtro = BuscarChat(this, filtroLista)
        }
        return filtro as BuscarChat
    }
}

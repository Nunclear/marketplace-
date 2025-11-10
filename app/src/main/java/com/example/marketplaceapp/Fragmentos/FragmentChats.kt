package com.example.marketplaceapp.Fragmentos

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.example.marketplaceapp.Adaptadores.AdaptadorChats
import com.example.marketplaceapp.Modelo.ModeloChats
import com.example.marketplaceapp.databinding.FragmentChatsBinding
import android.util.Log

class FragmentChats : Fragment() {

    private lateinit var binding : FragmentChatsBinding

    private lateinit var firebaseAuth: FirebaseAuth
    private var miUid = ""
    private lateinit var chatsArrayList : ArrayList<ModeloChats>
    private lateinit var adaptadorChats : AdaptadorChats
    private lateinit var mContext : Context

    override fun onAttach(context: Context) {
        mContext = context
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentChatsBinding.inflate(inflater,container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()
        miUid = firebaseAuth.uid ?: ""
        
        if (miUid.isEmpty()) {
            Log.e("FragmentChats", "Usuario no autenticado")
            return
        }
        
        Log.d("FragmentChats", "Iniciando FragmentChats con UID: $miUid")
        
        chatsArrayList = ArrayList()
        adaptadorChats = AdaptadorChats(mContext, chatsArrayList)
        binding.chatsRv.adapter = adaptadorChats
        
        cargarChats()

        binding.EtBuscar.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(filtro: CharSequence?, p1: Int, p2: Int, p3: Int) {
                try {
                    val consulta = filtro.toString()
                    adaptadorChats.filter.filter(consulta)
                }catch (e:Exception){
                    Log.e("FragmentChats", "Error al filtrar: ${e.message}")
                }
            }

            override fun afterTextChanged(p0: Editable?) {

            }
        })

    }

    private fun cargarChats() {
        Log.d("FragmentChats", "Iniciando carga de chats para UID: $miUid")
        
        val ref = FirebaseDatabase.getInstance().getReference("Chats")
        ref.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                chatsArrayList.clear()
                
                Log.d("FragmentChats", "Total de chats en Firebase: ${snapshot.childrenCount}")
                
                if (!snapshot.exists()) {
                    Log.d("FragmentChats", "No hay chats disponibles")
                    adaptadorChats.notifyDataSetChanged()
                    return
                }
                
                for (ds in snapshot.children){
                    try {
                        val chatKey = ds.key ?: ""
                        
                        if (chatKey.isEmpty()) {
                            continue
                        }
                        
                        if (chatKey.contains(miUid)){
                            if (ds.hasChildren()) {
                                val modeloChats = ModeloChats()
                                modeloChats.keyChat = chatKey
                                chatsArrayList.add(modeloChats)
                                Log.d("FragmentChats", "Chat agregado: $chatKey")
                            } else {
                                Log.d("FragmentChats", "Chat sin mensajes: $chatKey")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("FragmentChats", "Error al procesar chat: ${e.message}", e)
                    }
                }
                
                adaptadorChats.notifyDataSetChanged()
                Log.d("FragmentChats", "Total de chats cargados: ${chatsArrayList.size}")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FragmentChats", "Error al cargar chats: ${error.message}")
            }
        })
    }
}

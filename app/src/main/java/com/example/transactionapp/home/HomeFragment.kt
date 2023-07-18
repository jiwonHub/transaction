package com.example.transactionapp.home

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.transactionapp.DBKey.Companion.CHILD_CHAT
import com.example.transactionapp.DBKey.Companion.DB_ARTICLES
import com.example.transactionapp.DBKey.Companion.DB_USERS
import com.example.transactionapp.R
import com.example.transactionapp.chatList.ChatListItem
import com.example.transactionapp.databinding.FragmentHomeBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var articleDB: DatabaseReference
    private lateinit var userDB: DatabaseReference
    private lateinit var articleAdapter: ArticleAdapter


    private val articleList = mutableListOf<ArticleModel>() // ArticleModel 형식의 리스트
    private val listener = object : ChildEventListener{ // 데이터베이스의 특정한 노드에 대한 변경을 수신 대기
        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) { // 리스트의 아이템을 검색하거나 추가가 있을 때 수신합니다.
            val articleModel = snapshot.getValue(ArticleModel::class.java)
            articleModel ?: return

            articleList.add(articleModel)
            articleAdapter.submitList(articleList) // 아이템 업데이트

        }

        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {} // 리스트의 아이템의 변화가 있을때 수신합니다.

        override fun onChildRemoved(snapshot: DataSnapshot) {} //  리스트의 아이템이 삭제되었을때 수신합니다.

        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {} // 리스트의 순서가 변경되었을때 수신합니다.

        override fun onCancelled(error: DatabaseError) {}

    }

    private var binding: FragmentHomeBinding? = null
    private val auth: FirebaseAuth by lazy{
        Firebase.auth
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fragmentHomeBinding = FragmentHomeBinding.bind(view)
        binding = fragmentHomeBinding

        articleList.clear()
        articleDB = Firebase.database.reference.child(DB_ARTICLES) // key = articles 부분에 추가할 DB
        userDB = Firebase.database.reference.child(DB_USERS) // key = users 부분에 추가할 DB
        articleAdapter = ArticleAdapter(onItemClicked = { articleModel ->
            if(auth.currentUser != null){ // 로그인이 되어있는가?
                if(auth.currentUser!!.uid != articleModel.sellerId) { // 내 아이디와 아이템 판매자 아이디가 다른가?
                    val chatRoom = ChatListItem(
                        buyId = auth.currentUser!!.uid,
                        sellerId = articleModel.sellerId,
                        itemTitle = articleModel.title,
                        key = System.currentTimeMillis()
                    )

                    userDB.child(auth.currentUser!!.uid) // chat 밑으로 userId 만들고
                        .child(CHILD_CHAT)
                        .push()
                        .setValue(chatRoom)

                    userDB.child(articleModel.sellerId) // 그 userId 밑으로 sellerId 등등을 추가
                        .child(CHILD_CHAT)
                        .push()
                        .setValue(chatRoom)

                    Snackbar.make(view, "채팅방이 생성되었습니다. 채팅탭에서 확인해주세요.", Snackbar.LENGTH_LONG).show()

                } else { // 아이디가 같으면
                    Snackbar.make(view, "내가 올린 아이템입니다.", Snackbar.LENGTH_LONG).show()
                }
            } else { // 로그인이 안되어있으면
                Snackbar.make(view, "로그인 후 사용해주세요.", Snackbar.LENGTH_LONG).show()
            }
        })

        fragmentHomeBinding.articleRecyclerView.layoutManager = LinearLayoutManager(context)
        fragmentHomeBinding.articleRecyclerView.adapter = articleAdapter
        fragmentHomeBinding.addFloatingButton.setOnClickListener {
            if(auth.currentUser != null){
                val intent = Intent(requireContext(), AddArticleActivity::class.java)
                startActivity(intent)
            } else{
               Snackbar.make(view, "로그인 후 사용해주세요.", Snackbar.LENGTH_LONG).show()
            }
        }

        articleDB.addChildEventListener(listener)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()

        articleAdapter.notifyDataSetChanged() // Adapter 에게 RecyclerView 의 리스트 데이터가 바뀌었으니 모든 항목을 통째로 업데이트를 하라는 신호가 간다.
    }

    override fun onDestroyView() {
        super.onDestroyView()

        articleDB.removeEventListener(listener)
    }
}
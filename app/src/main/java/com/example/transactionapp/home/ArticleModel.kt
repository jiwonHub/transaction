package com.example.transactionapp.home

data class ArticleModel(
    val sellerId : String,
    val title : String,
    val createdAt : Long,
    val price : String,
    val imageUrl: String
)
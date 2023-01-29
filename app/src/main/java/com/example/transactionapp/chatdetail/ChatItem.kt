package com.example.transactionapp.chatdetail

data class ChatItem (
    val senderId: String,
    val message: String
) {
    constructor() : this("", "")
}

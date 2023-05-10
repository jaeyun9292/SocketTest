package com.example.sockettestapp

interface OnServerInteractionListener {
    fun onServerInteraction(header : String, body : String)
    fun onTestInteraction(testStr : String)
}
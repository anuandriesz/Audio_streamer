package com.aeturnum.test.audiostreamer.utils
import java.text.SimpleDateFormat
import java.util.*


object Helper {
    private lateinit var lastRecordedFilePath:String

    fun setLastRecFilePath(path:String){
        this.lastRecordedFilePath = path
    }

    fun getLastRecFilePath():String{
        return lastRecordedFilePath
    }

    fun getCurrentDate():String{
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

}
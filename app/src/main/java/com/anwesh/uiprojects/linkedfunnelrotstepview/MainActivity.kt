package com.anwesh.uiprojects.linkedfunnelrotstepview

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.anwesh.uiprojects.funnelrotstepview.FunnelRotStepView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FunnelRotStepView.create(this)
    }
}

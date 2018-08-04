package com.mobelite.tourismguide.data.roomservice.model

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey
import com.google.android.gms.common.server.BaseApi.append
import org.jetbrains.annotations.NotNull


@Entity(tableName = "restaurant")
class Restaurant {

    @NotNull
    @PrimaryKey (autoGenerate = true)
    @ColumnInfo (name = "id")
    var ID:Int =0

    @ColumnInfo(name = "name")
    var Name:String?=null

    @ColumnInfo(name = "phone")
    var Phone :String?=null

    @ColumnInfo(name = "description")
    var Desc :String?=null

    @ColumnInfo(name = "lat")
    var Lat :String?=null

    @ColumnInfo(name = "lng")
    var Lng :String?=null

    @ColumnInfo(name = "image")
    var Image :String?=null

    @ColumnInfo(name = "userid")
    var UserID :String?=null


    constructor()
    @Ignore
    constructor(ID: Int, Name: String?, Phone: String?, Desc: String?, Lat: String?, Lng: String?, Image: String?, UserID: String?) {
        this.ID = ID
        this.Name = Name
        this.Phone = Phone
        this.Desc = Desc
        this.Lat = Lat
        this.Lng = Lng
        this.Image = Image
        this.UserID = UserID
    }

    override fun toString():String {
        return StringBuilder ("resto :").append("\n").append(ID)
                .append("\n")
                .append(Name)
                .append("\n")
                .append(Phone)
                .append("\n")
                .append(Desc)
                .append("\n")
                .append(Lat)
                .append("\n")
                .append(Lng)
                .append("\n")
                .append(UserID)
                .toString()
    }
}
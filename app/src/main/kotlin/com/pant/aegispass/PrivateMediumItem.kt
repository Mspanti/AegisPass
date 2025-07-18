package com.pant.aegispass

import android.os.Parcel
import android.os.Parcelable

// FIX: Manually implement Parcelable to avoid ClassNotFoundException issues
data class PrivateMediaItem(
    val fileName: String,
    val filePath: String,
    val isVideo: Boolean
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(fileName)
        parcel.writeString(filePath)
        parcel.writeByte(if (isVideo) 1.toByte() else 0.toByte())
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<PrivateMediaItem> {
        override fun createFromParcel(parcel: Parcel): PrivateMediaItem {
            return PrivateMediaItem(parcel)
        }

        override fun newArray(size: Int): Array<PrivateMediaItem?> {
            return arrayOfNulls(size)
        }
    }
}
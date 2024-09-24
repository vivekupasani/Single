package com.vivekupasani.single.notification

import com.vivekupasani.single.notification.models.Notification
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface NotificationInterface {

    @POST("/v1/projects/single-5664b/messages:send")
    @Headers("Content-Type: application/json", "Accept: application/json")
    suspend fun sendNotification(
        @Body message: Notification,
        @Header("Authorization") accessToken: String
    ): Notification
}

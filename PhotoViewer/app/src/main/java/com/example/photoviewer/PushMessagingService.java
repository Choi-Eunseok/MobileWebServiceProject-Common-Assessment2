package com.example.photoviewer;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

// 게시물 등록 푸시 알림 수신 (FCM)
//  - 서버는 새 게시물이 생기면 토픽(TOPIC_NEW_POST)으로 알림을 보낸다.
//  - 앱이 백그라운드일 때는 시스템이 알림을 직접 표시하고,
//    앱이 화면에 떠 있을 때는 onMessageReceived 에서 직접 알림을 띄운다.
public class PushMessagingService extends FirebaseMessagingService {
    private static final String TAG = "PushMessaging";

    public static final String TOPIC_NEW_POST = "new_post";      // 서버 settings.FCM_TOPIC 과 동일
    public static final String CHANNEL_NEW_POST = "new_post";    // 서버 AndroidNotification.channel_id 와 동일
    public static final String EXTRA_POST_ID = "post_id";
    public static final String EXTRA_POST_TITLE = "title";      // 서버 data 의 title 키와 동일

    // 알림 채널 생성 (앱 시작 시 호출, 이미 있으면 무시됨)
    public static void createChannel(Context context) {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_NEW_POST, "새 게시물 알림", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("새 게시물이 등록되면 알려줍니다.");
        context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        Map<String, String> data = message.getData();
        String title = "새 게시물이 등록되었습니다";
        String body = data.getOrDefault("title", "");
        if (message.getNotification() != null) {
            if (message.getNotification().getTitle() != null) title = message.getNotification().getTitle();
            if (message.getNotification().getBody() != null) body = message.getNotification().getBody();
        }
        Log.d(TAG, "수신: " + title + " / " + body + " / " + data);
        showNotification(title, body, data.get(EXTRA_POST_ID), data.get(EXTRA_POST_TITLE));
    }

    @Override
    public void onNewToken(@NonNull String token) {
        // 토픽 방식이라 서버에 토큰을 등록할 필요는 없음 (디버깅용 로그)
        Log.d(TAG, "FCM token: " + token);
    }

    private void showNotification(String title, String body, String postId, String postTitle) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;   // 알림 권한 거부 시 표시하지 않음
        }
        createChannel(this);

        // 알림을 누르면 MainActivity 를 열고 해당 게시물을 보여줌
        Intent intent = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(EXTRA_POST_ID, postId)
                .putExtra(EXTRA_POST_TITLE, postTitle);
        int requestCode = postId != null ? postId.hashCode() : 0;
        PendingIntent pendingIntent = PendingIntent.getActivity(this, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_NEW_POST)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat.from(this).notify((int) System.currentTimeMillis(), builder.build());
    }
}

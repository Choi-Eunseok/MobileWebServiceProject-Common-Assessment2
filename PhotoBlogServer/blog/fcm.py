"""FCM 푸시 알림 전송 (Firebase Admin SDK)

서비스 계정 키(JSON)는 settings.FCM_CREDENTIALS 경로에서 읽는다.
키는 비밀 정보이므로 git 에 올리지 않는다 (.gitignore: *sdk*.json).
"""
import logging
import threading

from django.conf import settings

logger = logging.getLogger(__name__)

_app = None
_lock = threading.Lock()


def _get_app():
    """Firebase 앱을 한 번만 초기화해서 재사용한다. 키가 없으면 None."""
    global _app
    if _app is not None:
        return _app
    with _lock:
        if _app is None:
            cred_path = getattr(settings, 'FCM_CREDENTIALS', None)
            if not cred_path:
                logger.warning('FCM 서비스 계정 키를 찾을 수 없어 푸시 알림을 보내지 않습니다.')
                return None
            import firebase_admin
            from firebase_admin import credentials
            _app = firebase_admin.initialize_app(credentials.Certificate(str(cred_path)))
    return _app


def send_new_post_notification(post):
    """새 게시물 알림을 FCM 토픽(settings.FCM_TOPIC)으로 전송한다.

    네트워크 호출이 응답을 늦추지 않도록 백그라운드 스레드에서 보낸다.
    """
    if not getattr(settings, 'FCM_ENABLED', True):
        return

    title = '새 게시물이 등록되었습니다'
    body = post.title or '(제목 없음)'
    data = {
        'type': 'new_post',
        'post_id': str(post.pk),
        'title': post.title or '',
        'author': getattr(post.author, 'username', ''),
    }

    def _send():
        try:
            app = _get_app()
            if app is None:
                return
            from firebase_admin import messaging
            message = messaging.Message(
                topic=settings.FCM_TOPIC,
                notification=messaging.Notification(title=title, body=body),
                data=data,
                android=messaging.AndroidConfig(
                    priority='high',
                    notification=messaging.AndroidNotification(channel_id='new_post'),
                ),
            )
            message_id = messaging.send(message, app=app)
            logger.info('FCM 전송 성공 post_id=%s message_id=%s', post.pk, message_id)
        except Exception:
            logger.exception('FCM 전송 실패 post_id=%s', post.pk)

    threading.Thread(target=_send, daemon=True).start()

from django.db import transaction
from django.db.models.signals import post_save
from django.dispatch import receiver

from .fcm import send_new_post_notification
from .models import Post


@receiver(post_save, sender=Post)
def notify_new_post(sender, instance, created, **kwargs):
    """게시물이 새로 생성되면 (웹 폼 / REST API / YOLOv5 자동 업로드 모두) 푸시 알림 전송.
    수정(update)일 때는 보내지 않는다."""
    if created:
        # DB 저장이 확정된 뒤에 전송
        transaction.on_commit(lambda: send_new_post_notification(instance))

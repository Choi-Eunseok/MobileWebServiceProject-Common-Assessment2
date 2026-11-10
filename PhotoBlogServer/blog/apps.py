from django.apps import AppConfig


class BlogConfig(AppConfig):
    name = 'blog'

    def ready(self):
        from . import signals  # noqa: F401  (post_save 시그널 등록)

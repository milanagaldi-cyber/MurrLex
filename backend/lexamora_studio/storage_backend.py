from django.conf import settings
from django.core.files.storage import FileSystemStorage
from django.utils.deconstruct import deconstructible


@deconstructible
class PrivateStudioStorage(FileSystemStorage):
    def __init__(self):
        super().__init__(location=settings.STUDIO_PRIVATE_MEDIA_ROOT)

from django.contrib import admin
from .models import Card, ImportLog, Lesson


class CardInline(admin.TabularInline):
    model = Card
    extra = 0
    fields = ("external_card_id", "native_value", "correct_value", "card_kind", "stars")
    readonly_fields = ()


@admin.register(Lesson)
class LessonAdmin(admin.ModelAdmin):
    list_display = (
        "title",
        "external_id",
        "card_kind",
        "source_language",
        "target_language",
        "cards_total",
        "updated_at",
    )
    search_fields = ("title", "external_id", "source_language", "target_language")
    list_filter = ("card_kind", "source_language", "target_language")
    readonly_fields = ("created_at", "updated_at")
    inlines = [CardInline]

    @admin.display(description="Cards")
    def cards_total(self, lesson: Lesson) -> int:
        return lesson.cards.count()


@admin.register(Card)
class CardAdmin(admin.ModelAdmin):
    list_display = (
        "external_card_id",
        "lesson",
        "card_kind",
        "source_language",
        "target_language",
        "stars",
        "updated_at",
    )
    search_fields = ("external_card_id", "native_value", "correct_value", "mistake", "lesson__title")
    list_filter = ("card_kind", "source_language", "target_language", "stars")
    readonly_fields = ("created_at", "updated_at")


@admin.register(ImportLog)
class ImportLogAdmin(admin.ModelAdmin):
    list_display = ("created_at", "status", "source", "external_lesson_id", "cards_count")
    search_fields = ("source", "external_lesson_id", "error_message")
    list_filter = ("status", "source")
    readonly_fields = ("created_at",)

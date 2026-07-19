from django.contrib import admin

from .models import AccessEvent, AdditionalGeneration, AiModelProfile, AiSuggestion, AiUsageLog, Asset, AuditEvent, Character, DialogueLine, DocxImport, Episode, EpisodeCover, ExportJob, GenerationOutput, MovieRenderJob, MovieTimeline, MovieTimelineRevision, Project, ProjectMembership, Prompt, PromptBlock, PromptTemplate, RecommendedTrack, Revision, Scene, StudioTextModel, SubtitleLine, SubtitleTrack, TranslationUnit, Workspace, WorkspaceMembership


class MembershipInline(admin.TabularInline):
    model = WorkspaceMembership
    extra = 0


class ProjectMembershipInline(admin.TabularInline):
    model = ProjectMembership
    extra = 0


@admin.register(Workspace)
class WorkspaceAdmin(admin.ModelAdmin):
    list_display = ("name", "slug", "owner", "updated_at")
    search_fields = ("name", "slug", "owner__username")
    inlines = [MembershipInline]


@admin.register(Project)
class ProjectAdmin(admin.ModelAdmin):
    list_display = ("title", "workspace", "project_type", "status", "updated_at")
    list_filter = ("project_type", "status", "workspace")
    search_fields = ("title", "concept")
    inlines = [ProjectMembershipInline]


admin.site.register(Character)
admin.site.register(Episode)
admin.site.register(Scene)
admin.site.register(AiModelProfile)
admin.site.register(StudioTextModel)
admin.site.register(PromptTemplate)
admin.site.register(DialogueLine)
admin.site.register(Prompt)
admin.site.register(PromptBlock)
@admin.register(Asset)
class AssetAdmin(admin.ModelAdmin):
    list_display = (
        "original_filename", "workspace", "content_type", "processing_status",
        "duration_ms", "size_bytes", "created_at",
    )
    list_filter = ("processing_status", "content_type", "workspace")
    search_fields = ("original_filename", "checksum_sha256")
    readonly_fields = (
        "checksum_sha256", "media_metadata", "processing_error", "processing_attempts",
        "processing_started_at", "processing_finished_at",
    )
admin.site.register(AdditionalGeneration)
admin.site.register(GenerationOutput)
admin.site.register(RecommendedTrack)
admin.site.register(EpisodeCover)


class AppendOnlyAdmin(admin.ModelAdmin):
    def has_add_permission(self, request):
        return False

    def has_change_permission(self, request, obj=None):
        return False

    def has_delete_permission(self, request, obj=None):
        return False


admin.site.register(Revision, AppendOnlyAdmin)
admin.site.register(AuditEvent, AppendOnlyAdmin)
admin.site.register(AccessEvent, AppendOnlyAdmin)
admin.site.register(AiSuggestion, AppendOnlyAdmin)
admin.site.register(AiUsageLog, AppendOnlyAdmin)
admin.site.register(SubtitleTrack)
admin.site.register(SubtitleLine)
admin.site.register(TranslationUnit)
admin.site.register(ExportJob, AppendOnlyAdmin)
admin.site.register(DocxImport)
admin.site.register(MovieTimeline)
admin.site.register(MovieTimelineRevision, AppendOnlyAdmin)
admin.site.register(MovieRenderJob, AppendOnlyAdmin)

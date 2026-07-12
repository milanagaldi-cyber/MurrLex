from django.contrib import admin

from .models import AdditionalGeneration, AiModelProfile, Asset, Character, DialogueLine, Episode, GenerationOutput, Project, Prompt, PromptBlock, Scene, Workspace, WorkspaceMembership


class MembershipInline(admin.TabularInline):
    model = WorkspaceMembership
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


admin.site.register(Character)
admin.site.register(Episode)
admin.site.register(Scene)
admin.site.register(AiModelProfile)
admin.site.register(DialogueLine)
admin.site.register(Prompt)
admin.site.register(PromptBlock)
admin.site.register(Asset)
admin.site.register(AdditionalGeneration)
admin.site.register(GenerationOutput)

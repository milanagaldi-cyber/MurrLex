import json
from datetime import timedelta
from unittest.mock import patch

from cryptography.fernet import Fernet
from django.contrib.auth import get_user_model
from django.db import connection
from django.test import TestCase, override_settings
from django.test.utils import CaptureQueriesContext

from .models import Project, ProjectAccessExclusion, ProjectMembership, Prompt, WorkspaceMembership
from .permissions import accessible_projects, accessible_workspaces, has_capability, has_project_capability
from .services import create_workspace


class StudioPermissionsTests(TestCase):
    def setUp(self):
        users = get_user_model()
        self.owner = users.objects.create_user("owner", password="strong-pass")
        self.viewer = users.objects.create_user("viewer", password="strong-pass")
        self.outsider = users.objects.create_user("outsider", password="strong-pass")
        self.workspace = create_workspace(user=self.owner, name="Dom Studio", slug="dom-studio")
        WorkspaceMembership.objects.create(
            workspace=self.workspace,
            user=self.viewer,
            role=WorkspaceMembership.Role.VIEWER,
        )

    def test_user_without_membership_cannot_see_workspace(self):
        self.assertFalse(accessible_workspaces(self.outsider).filter(id=self.workspace.id).exists())

    def test_viewer_cannot_edit(self):
        self.assertTrue(has_capability(self.viewer, self.workspace, "view"))
        self.assertFalse(has_capability(self.viewer, self.workspace, "edit"))

    def test_owner_can_edit_and_manage_members(self):
        self.assertTrue(has_capability(self.owner, self.workspace, "edit"))
        self.assertTrue(has_capability(self.owner, self.workspace, "manage_members"))

    def test_dashboard_requires_login(self):
        response = self.client.get("/studio/")
        self.assertEqual(response.status_code, 302)
        self.assertIn("/login/", response.url)

    def test_cross_workspace_project_is_not_exposed(self):
        project = Project.objects.create(
            workspace=self.workspace,
            project_type=Project.Type.SERIES,
            title="Dom",
            created_by=self.owner,
            updated_by=self.owner,
        )
        self.client.force_login(self.outsider)
        self.assertEqual(self.client.get(f"/studio/projects/{project.id}/").status_code, 404)

    def test_workspace_creation_makes_owner_membership(self):
        created = create_workspace(user=self.outsider, name="Second", slug="second")
        membership = WorkspaceMembership.objects.get(workspace=created, user=self.outsider)
        self.assertEqual(membership.role, WorkspaceMembership.Role.OWNER)
        self.assertTrue(membership.can_use_ai)


class ProjectSharingTests(TestCase):
    def setUp(self):
        users = get_user_model()
        self.owner = users.objects.create_user("share-owner", email="owner@example.com", password="pass")
        self.viewer = users.objects.create_user("share-viewer", email="viewer@example.com", password="pass")
        self.editor = users.objects.create_user("share-editor", email="editor@example.com", password="pass")
        self.controller = users.objects.create_user("share-controller", email="controller@example.com", password="pass")
        self.outsider = users.objects.create_user("share-outsider", email="outsider@example.com", password="pass")
        self.workspace = create_workspace(user=self.owner, name="Private Studio", slug="private-studio")
        self.project = Project.objects.create(
            workspace=self.workspace, project_type=Project.Type.SERIES, title="Shared project",
            created_by=self.owner, updated_by=self.owner,
        )
        self.other_project = Project.objects.create(
            workspace=self.workspace, project_type=Project.Type.SERIES, title="Private project",
            created_by=self.owner, updated_by=self.owner,
        )
        ProjectMembership.objects.create(project=self.project, user=self.viewer, role=ProjectMembership.Role.VIEWER, invited_by=self.owner)
        ProjectMembership.objects.create(project=self.project, user=self.editor, role=ProjectMembership.Role.EDITOR, invited_by=self.owner)
        ProjectMembership.objects.create(project=self.project, user=self.controller, role=ProjectMembership.Role.CONTROLLER, invited_by=self.owner)

    def test_workspace_owner_can_open_project_sharing(self):
        self.assertTrue(has_project_capability(self.owner, self.project, "manage_project"))
        self.client.force_login(self.owner)
        detail = self.client.get(f"/studio/projects/{self.project.id}/")
        self.assertContains(detail, f"/studio/projects/{self.project.id}/access/")
        self.assertEqual(self.client.get(f"/studio/projects/{self.project.id}/access/").status_code, 200)

    def test_project_share_does_not_expose_workspace_or_sibling_project(self):
        self.assertTrue(accessible_projects(self.viewer).filter(id=self.project.id).exists())
        self.assertFalse(accessible_projects(self.viewer).filter(id=self.other_project.id).exists())
        self.assertFalse(accessible_workspaces(self.viewer).filter(id=self.workspace.id).exists())
        self.client.force_login(self.viewer)
        self.assertEqual(self.client.get(f"/studio/projects/{self.project.id}/").status_code, 200)
        self.assertEqual(self.client.get(f"/studio/projects/{self.other_project.id}/").status_code, 404)

    def test_viewer_cannot_edit_and_editor_can_edit(self):
        self.assertFalse(has_project_capability(self.viewer, self.project, "edit"))
        self.assertTrue(has_project_capability(self.editor, self.project, "edit"))
        self.client.force_login(self.viewer)
        self.assertEqual(self.client.get(f"/studio/projects/{self.project.id}/edit/").status_code, 403)
        self.client.force_login(self.editor)
        self.assertEqual(self.client.get(f"/studio/projects/{self.project.id}/edit/").status_code, 200)

    def test_full_control_can_manage_project_members(self):
        self.client.force_login(self.controller)
        response = self.client.post(
            f"/studio/projects/{self.project.id}/access/",
            {"email": self.outsider.email, "role": ProjectMembership.Role.VIEWER},
        )
        self.assertEqual(response.status_code, 302)
        self.assertTrue(ProjectMembership.objects.filter(project=self.project, user=self.outsider).exists())
        self.outsider.api_access.refresh_from_db()
        self.assertTrue(self.outsider.api_access.ai_api_enabled)

    def test_editor_cannot_manage_project_members(self):
        self.client.force_login(self.editor)
        self.assertEqual(self.client.get(f"/studio/projects/{self.project.id}/access/").status_code, 403)

    def test_full_control_can_manage_members_through_api(self):
        self.client.force_login(self.controller)
        response = self.client.post(
            f"/api/v1/studio/projects/{self.project.id}/members",
            data=json.dumps({"email": self.outsider.email, "role": ProjectMembership.Role.EDITOR}),
            content_type="application/json",
        )
        self.assertEqual(response.status_code, 201)
        self.outsider.api_access.refresh_from_db()
        self.assertTrue(self.outsider.api_access.ai_api_enabled)
        membership_id = response.json()["id"]
        self.assertEqual(self.client.get(f"/api/v1/studio/projects/{self.project.id}/members").status_code, 200)
        self.assertEqual(self.client.delete(f"/api/v1/studio/projects/{self.project.id}/members/{membership_id}").status_code, 200)
        self.outsider.api_access.refresh_from_db()
        self.assertTrue(self.outsider.api_access.ai_api_enabled)

    def test_viewer_cannot_manage_members_through_api(self):
        self.client.force_login(self.viewer)
        self.assertEqual(self.client.get(f"/api/v1/studio/projects/{self.project.id}/members").status_code, 403)

    def test_workspace_share_is_inherited_and_can_be_revoked_per_project(self):
        self.client.force_login(self.owner)
        shared = self.client.post(
            f"/studio/workspaces/{self.workspace.id}/access/",
            {"email": self.outsider.email, "role": WorkspaceMembership.Role.EDITOR, "can_use_ai": "on"},
        )
        self.assertEqual(shared.status_code, 302)
        self.assertTrue(accessible_projects(self.outsider).filter(id=self.project.id).exists())
        self.assertTrue(accessible_projects(self.outsider).filter(id=self.other_project.id).exists())

        revoked = self.client.post(
            f"/studio/projects/{self.other_project.id}/access/",
            {"action": "exclude", "user_id": str(self.outsider.id)},
        )
        self.assertEqual(revoked.status_code, 302)
        self.assertTrue(ProjectAccessExclusion.objects.filter(project=self.other_project, user=self.outsider).exists())
        self.assertFalse(accessible_projects(self.outsider).filter(id=self.other_project.id).exists())
        self.assertTrue(accessible_projects(self.outsider).filter(id=self.project.id).exists())

        direct = self.client.post(
            f"/studio/projects/{self.other_project.id}/access/",
            {"email": self.outsider.email, "role": ProjectMembership.Role.VIEWER},
        )
        self.assertEqual(direct.status_code, 302)
        self.assertFalse(ProjectAccessExclusion.objects.filter(project=self.other_project, user=self.outsider).exists())
        self.assertTrue(accessible_projects(self.outsider).filter(id=self.other_project.id).exists())

class StudioApiTests(TestCase):
    def setUp(self):
        users = get_user_model()
        self.owner = users.objects.create_user("api-owner", password="strong-pass")
        self.viewer = users.objects.create_user("api-viewer", password="strong-pass")
        self.workspace = create_workspace(user=self.owner, name="API Studio", slug="api-studio")
        WorkspaceMembership.objects.create(workspace=self.workspace, user=self.viewer, role=WorkspaceMembership.Role.VIEWER)

    def test_workspace_api_requires_login(self):
        self.assertEqual(self.client.get("/api/v1/studio/workspaces").status_code, 401)

    def test_owner_can_create_project(self):
        self.client.force_login(self.owner)
        response = self.client.post(
            "/api/v1/studio/projects",
            data=json.dumps({
                "workspaceId": str(self.workspace.id), "type": "SERIES", "title": "Dom",
                "promptTemplate": "No music. Keep character references.",
            }),
            content_type="application/json",
        )
        self.assertEqual(response.status_code, 201)
        self.assertEqual(response.json()["title"], "Dom")
        self.assertEqual(response.json()["promptTemplate"], "No music. Keep character references.")

    def test_viewer_cannot_create_project(self):
        self.client.force_login(self.viewer)
        response = self.client.post(
            "/api/v1/studio/projects",
            data=json.dumps({"workspaceId": str(self.workspace.id), "type": "SERIES", "title": "Denied"}),
            content_type="application/json",
        )
        self.assertEqual(response.status_code, 403)

class StudioSceneTests(TestCase):
    def setUp(self):
        from .models import AiModelProfile, DialogueLine, Episode, Prompt, PromptBlock, Scene
        users = get_user_model()
        self.editor = users.objects.create_user("scene-editor", password="strong-pass")
        self.translator = users.objects.create_user("scene-translator", password="strong-pass")
        self.workspace = create_workspace(user=self.editor, name="Scene Studio", slug="scene-studio")
        WorkspaceMembership.objects.create(workspace=self.workspace, user=self.translator, role=WorkspaceMembership.Role.TRANSLATOR)
        self.project = Project.objects.create(
            workspace=self.workspace, project_type=Project.Type.SERIES, title="Scene Project",
            created_by=self.editor, updated_by=self.editor,
        )
        self.episode = Episode.objects.create(
            project=self.project, number=1, title="Pilot", created_by=self.editor, updated_by=self.editor,
        )
        self.scene = Scene.objects.create(
            episode=self.episode, number=1, title="Opening", created_by=self.editor, updated_by=self.editor,
        )
        self.line = DialogueLine.objects.create(
            scene=self.scene, speaker="Hero", text="Original", status=DialogueLine.Status.APPROVED,
            created_by=self.editor, updated_by=self.editor,
        )
        self.model = AiModelProfile.objects.create(name="Test Video", provider="Test", model_id="test-video", media_type="VIDEO")
        self.prompt = Prompt.objects.create(
            scene=self.scene, ai_model=self.model, prompt_type=Prompt.Type.VIDEO,
            created_by=self.editor, updated_by=self.editor,
        )
        PromptBlock.objects.create(
            prompt=self.prompt, block_type=PromptBlock.Type.DIALOGUE_REFERENCE,
            content="Original", source_dialogue=self.line, created_by=self.editor, updated_by=self.editor,
        )

    def test_translator_cannot_change_approved_source_dialogue(self):
        self.client.force_login(self.translator)
        response = self.client.patch(
            f"/api/v1/studio/dialogue/{self.line.id}",
            data=json.dumps({"text": "Changed"}),
            content_type="application/json",
        )
        self.assertEqual(response.status_code, 403)
        self.line.refresh_from_db()
        self.assertEqual(self.line.text, "Original")

    def test_editor_changes_dialogue_and_marks_prompt_for_review(self):
        self.client.force_login(self.editor)
        response = self.client.patch(
            f"/api/v1/studio/dialogue/{self.line.id}",
            data=json.dumps({"text": "Changed"}),
            content_type="application/json",
        )
        self.assertEqual(response.status_code, 200)
        self.prompt.refresh_from_db()
        self.assertTrue(self.prompt.needs_review)

    def test_editor_can_create_structured_prompt(self):
        self.client.force_login(self.editor)
        response = self.client.post(
            f"/api/v1/studio/scenes/{self.scene.id}/prompts",
            data=json.dumps({
                "aiModelId": str(self.model.id),
                "type": "IMAGE",
                "blocks": [{"type": "NARRATIVE", "content": "Cinematic office"}, {"type": "NEGATIVE", "content": "No text"}],
            }),
            content_type="application/json",
        )
        self.assertEqual(response.status_code, 201)

class StudioAssetTests(TestCase):
    def setUp(self):
        import tempfile
        from lexamora_studio.models import Episode, Scene, private_storage

        self.temp_media = tempfile.TemporaryDirectory()
        self.private_storage = private_storage
        self.previous_location = private_storage._location
        private_storage._location = self.temp_media.name
        private_storage.__dict__.pop("base_location", None)
        private_storage.__dict__.pop("location", None)

        users = get_user_model()
        self.owner = users.objects.create_user("asset-owner", password="strong-pass")
        self.outsider = users.objects.create_user("asset-outsider", password="strong-pass")
        self.workspace = create_workspace(user=self.owner, name="Asset Studio", slug="asset-studio")
        self.other_workspace = create_workspace(user=self.outsider, name="Other Studio", slug="other-studio")
        self.project = Project.objects.create(
            workspace=self.workspace, project_type=Project.Type.SERIES, title="Asset Project",
            created_by=self.owner, updated_by=self.owner,
        )
        self.episode = Episode.objects.create(
            project=self.project, number=1, title="Pilot", created_by=self.owner, updated_by=self.owner,
        )
        self.scene = Scene.objects.create(
            episode=self.episode, number=1, title="Asset Scene", created_by=self.owner, updated_by=self.owner,
        )

    def tearDown(self):
        self.private_storage._location = self.previous_location
        self.private_storage.__dict__.pop("base_location", None)
        self.private_storage.__dict__.pop("location", None)
        self.temp_media.cleanup()

    def image_upload(self, name="scene.png"):
        import io
        from PIL import Image
        from django.core.files.uploadedfile import SimpleUploadedFile

        buffer = io.BytesIO()
        Image.new("RGB", (32, 24), "#f08a43").save(buffer, "PNG")
        return SimpleUploadedFile(name, buffer.getvalue(), content_type="image/png")

    def upload_asset(self):
        self.client.force_login(self.owner)
        return self.client.post(
            "/api/v1/studio/assets",
            data={
                "workspaceId": str(self.workspace.id),
                "projectId": str(self.project.id),
                "sceneId": str(self.scene.id),
                "kind": "SCENE_IMAGE",
                "file": self.image_upload(),
            },
        )

    def test_image_upload_generates_private_thumbnail(self):
        from .models import Asset

        response = self.upload_asset()
        self.assertEqual(response.status_code, 201, response.content)
        asset = Asset.objects.get(id=response.json()["id"])
        self.assertEqual((asset.width, asset.height), (32, 24))
        self.assertTrue(asset.thumbnail.name)
        self.assertTrue(asset.file.storage.exists(asset.file.name))
        self.assertTrue(asset.thumbnail.storage.exists(asset.thumbnail.name))

    def test_owner_can_view_original_inline_and_download_attachment(self):
        from .models import AccessEvent, AuditEvent

        response = self.upload_asset()
        asset_id = response.json()["id"]
        viewed = self.client.get(f"/api/v1/studio/assets/{asset_id}/view")
        self.assertEqual(viewed.status_code, 200)
        self.assertTrue(viewed["Content-Disposition"].startswith("inline;"))
        viewed.close()
        downloaded = self.client.get(f"/api/v1/studio/assets/{asset_id}/download")
        self.assertEqual(downloaded.status_code, 200)
        self.assertTrue(downloaded["Content-Disposition"].startswith("attachment;"))
        downloaded.close()
        self.assertTrue(AuditEvent.objects.filter(action="ASSET_VIEW", entity_id=asset_id).exists())
        self.assertTrue(AccessEvent.objects.filter(asset_id=asset_id, action="VIEW").exists())
    def test_user_from_another_workspace_cannot_download_asset(self):
        response = self.upload_asset()
        self.client.force_login(self.outsider)
        denied = self.client.get(f"/api/v1/studio/assets/{response.json()['id']}/download")
        self.assertEqual(denied.status_code, 404)

    def test_invalid_image_is_rejected(self):
        from django.core.files.uploadedfile import SimpleUploadedFile

        self.client.force_login(self.owner)
        response = self.client.post(
            "/api/v1/studio/assets",
            data={
                "workspaceId": str(self.workspace.id),
                "kind": "SCENE_IMAGE",
                "file": SimpleUploadedFile("bad.png", b"not an image", content_type="image/png"),
            },
        )
        self.assertEqual(response.status_code, 400)

    def test_generation_can_select_one_final_output(self):
        from .models import Asset, GenerationOutput

        first_response = self.upload_asset()
        second_response = self.upload_asset()
        self.client.force_login(self.owner)
        generation = self.client.post(
            f"/api/v1/studio/scenes/{self.scene.id}/generations",
            data=json.dumps({"reason": "Closer shot", "prompt": "Close-up of the glass"}),
            content_type="application/json",
        )
        self.assertEqual(generation.status_code, 201)
        output_ids = []
        for asset_id in (first_response.json()["id"], second_response.json()["id"]):
            output = self.client.post(
                f"/api/v1/studio/generations/{generation.json()['id']}/outputs",
                data=json.dumps({"assetId": asset_id, "modelMetadata": {"model": "Nano Banana 2"}}),
                content_type="application/json",
            )
            self.assertEqual(output.status_code, 201)
            output_ids.append(output.json()["id"])
        self.client.post(f"/api/v1/studio/generation-outputs/{output_ids[0]}/final")
        self.client.post(f"/api/v1/studio/generation-outputs/{output_ids[1]}/final")
        self.assertEqual(GenerationOutput.objects.filter(generation_id=generation.json()["id"], is_final=True).count(), 1)

    def test_upload_and_download_are_audited(self):
        from .models import AccessEvent, AuditEvent

        response = self.upload_asset()
        asset_id = response.json()["id"]
        self.assertTrue(AuditEvent.objects.filter(action="ASSET_UPLOAD", entity_id=asset_id).exists())
        download = self.client.get(f"/api/v1/studio/assets/{asset_id}/download")
        self.assertEqual(download.status_code, 200)
        download.close()
        self.assertTrue(AuditEvent.objects.filter(action="ASSET_DOWNLOAD", entity_id=asset_id).exists())
        self.assertTrue(AccessEvent.objects.filter(asset_id=asset_id, action="DOWNLOAD").exists())

class StudioRevisionTests(TestCase):
    def setUp(self):
        from .revisions import record_revision

        users = get_user_model()
        self.editor = users.objects.create_user("revision-editor", password="strong-pass")
        self.viewer = users.objects.create_user("revision-viewer", password="strong-pass")
        self.workspace = create_workspace(user=self.editor, name="Revision Studio", slug="revision-studio")
        WorkspaceMembership.objects.create(workspace=self.workspace, user=self.viewer, role=WorkspaceMembership.Role.VIEWER)
        self.project = Project.objects.create(
            workspace=self.workspace, project_type=Project.Type.SERIES, title="Original title",
            concept="First", created_by=self.editor, updated_by=self.editor,
        )
        self.initial = record_revision(instance=self.project, user=self.editor, operation="CREATE")

    def test_history_shows_author_and_timestamp(self):
        self.client.force_login(self.editor)
        self.client.patch(
            f"/api/v1/studio/projects/{self.project.id}",
            data=json.dumps({"title": "Changed title"}),
            content_type="application/json",
        )
        response = self.client.get(f"/api/v1/studio/entities/project/{self.project.id}/revisions")
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.json()["results"]), 2)
        self.assertEqual(response.json()["results"][0]["author"], self.editor.username)
        self.assertTrue(response.json()["results"][0]["createdAt"])

    def test_restore_creates_new_revision(self):
        from .models import Revision

        self.client.force_login(self.editor)
        self.client.patch(
            f"/api/v1/studio/projects/{self.project.id}",
            data=json.dumps({"title": "Changed title"}),
            content_type="application/json",
        )
        response = self.client.post(
            f"/api/v1/studio/entities/project/{self.project.id}/restore/{self.initial.id}"
        )
        self.assertEqual(response.status_code, 200, response.content)
        self.project.refresh_from_db()
        self.assertEqual(self.project.title, "Original title")
        self.assertEqual(Revision.objects.filter(entity_id=self.project.id).count(), 3)
        self.assertEqual(Revision.objects.filter(entity_id=self.project.id).order_by("-sequence").first().operation, "RESTORE")

    def test_viewer_cannot_restore_revision(self):
        self.client.force_login(self.viewer)
        response = self.client.post(
            f"/api/v1/studio/entities/project/{self.project.id}/restore/{self.initial.id}"
        )
        self.assertEqual(response.status_code, 403)

    def test_revision_is_append_only(self):
        self.initial.operation = "TAMPERED"
        with self.assertRaises(ValueError):
            self.initial.save()


@override_settings(
    CREDENTIAL_ENCRYPTION_KEY=Fernet.generate_key().decode("ascii"),
    STUDIO_AI_RATE_PER_MINUTE=10,
)
class StudioAiSuggestionTests(TestCase):
    def setUp(self):
        from lessons.models import ProviderCredential, UserApiAccess
        from .models import AiModelProfile, DialogueLine, Episode, Prompt, PromptBlock, Scene

        users = get_user_model()
        self.editor = users.objects.create_user("ai-editor", password="strong-pass")
        self.viewer = users.objects.create_user("ai-viewer", password="strong-pass")
        access = UserApiAccess.objects.get(user=self.editor)
        access.ai_api_enabled = True
        access.save()
        credential = ProviderCredential(provider=ProviderCredential.Provider.OPENAI)
        credential.set_api_key("sk-test")
        credential.save()

        self.workspace = create_workspace(user=self.editor, name="AI Studio", slug="ai-studio")
        WorkspaceMembership.objects.create(workspace=self.workspace, user=self.viewer, role=WorkspaceMembership.Role.VIEWER)
        project = Project.objects.create(
            workspace=self.workspace, project_type=Project.Type.SERIES, title="AI Project",
            created_by=self.editor, updated_by=self.editor,
        )
        self.project = project
        episode = Episode.objects.create(project=project, number=1, title="Pilot", created_by=self.editor, updated_by=self.editor)
        scene = Scene.objects.create(episode=episode, number=1, title="Opening", created_by=self.editor, updated_by=self.editor)
        model = AiModelProfile.objects.create(name="AI Test Video", provider="Test", model_id="video-test", media_type="VIDEO")
        self.prompt = Prompt.objects.create(
            scene=scene, ai_model=model, prompt_type=Prompt.Type.VIDEO,
            created_by=self.editor, updated_by=self.editor,
        )
        self.narrative = PromptBlock.objects.create(
            prompt=self.prompt, block_type=PromptBlock.Type.NARRATIVE, content="Plain room",
            position=0, created_by=self.editor, updated_by=self.editor,
        )
        self.source_dialogue = DialogueLine.objects.create(
            scene=scene, speaker="Hero", text="Secret dialogue", language="en",
            position=0, created_by=self.editor, updated_by=self.editor,
        )
        self.dialogue = PromptBlock.objects.create(
            prompt=self.prompt, block_type=PromptBlock.Type.DIALOGUE_REFERENCE, content="Secret dialogue",
            source_dialogue=self.source_dialogue, position=1, created_by=self.editor, updated_by=self.editor,
        )

    def provider_response(self):
        return json.dumps({"blocks": [
            {"id": str(self.narrative.id), "content": "Cinematic room with precise lighting"},
            {"id": str(self.dialogue.id), "content": "Rewritten dialogue"},
        ]})

    @patch("lexamora_studio.views.run_text")
    def test_localized_field_translation_returns_both_dependent_languages(self, mocked_run_text):
        mocked_run_text.return_value = (
            json.dumps({"prompt": "A quiet office", "dialogue": "Una oficina tranquila"}),
            "gpt-5.4-mini",
        )
        self.client.force_login(self.editor)
        response = self.client.post(
            f"/studio/projects/{self.project.id}/localized-translate/",
            data=json.dumps({
                "source": "Spokojne biuro", "sourceLanguage": "PL",
                "targets": {"prompt": "EN", "dialogue": "ES"},
            }),
            content_type="application/json",
        )
        self.assertEqual(response.status_code, 200, response.content)
        self.assertEqual(response.json()["translations"]["prompt"], "A quiet office")
        self.assertEqual(response.json()["translations"]["dialogue"], "Una oficina tranquila")

    def test_inline_prompt_ui_is_compact_and_exposes_clear_ai_actions(self):
        self.client.force_login(self.editor)
        response = self.client.get(f"/studio/scenes/{self.prompt.scene_id}/")
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, "Added automatically")
        self.assertContains(response, "Copy")
        self.assertContains(response, "Paste")
        self.assertContains(response, "Clear")
        self.assertContains(response, "Suggest translation")
        self.assertContains(response, "Entire prompt")
        self.assertContains(response, "Dialogue only")
        self.assertContains(response, "Selected text")
        self.assertContains(response, "Apply Translation")
        self.assertContains(response, "Improve Translation")
        self.assertContains(response, "data-prompt-toggle")
        self.assertContains(response, '<option value="BL">BL</option>', html=True)
        self.assertContains(response, "gpt-5.4-mini")
        self.assertNotContains(response, "Mandatory template")
        self.assertNotContains(response, "Improve action")

    @patch("lexamora_studio.ai.run_text")
    def test_prompt_translation_preview_and_apply_create_language_version(self, mocked_run_text):
        mocked_run_text.return_value = (json.dumps({"content": "Filmowy pokoj"}), "gpt-5.4-mini")
        self.client.force_login(self.editor)
        preview = self.client.post(
            f"/studio/prompts/{self.prompt.id}/ai-preview/",
            data=json.dumps({
                "action": "translate", "content": "Cinematic room", "targetLanguage": "PL",
                "scope": "FULL", "textModel": "gpt-5.4-mini",
            }),
            content_type="application/json",
        )
        self.assertEqual(preview.status_code, 200, preview.content)
        self.assertEqual(preview.json()["content"], "Filmowy pokoj")

        applied = self.client.post(
            f"/studio/prompts/{self.prompt.id}/apply-translation/",
            data=json.dumps({"content": preview.json()["content"], "targetLanguage": "PL", "scope": "FULL"}),
            content_type="application/json",
        )
        self.assertEqual(applied.status_code, 200, applied.content)
        translated = Prompt.objects.get(source_prompt=self.prompt, language="PL")
        self.assertEqual(translated.content, "Filmowy pokoj")
        self.assertEqual(translated.blocks.get().content, "Filmowy pokoj")
        self.assertEqual({row["language"] for row in applied.json()["versions"]}, {"EN", "PL"})

    @patch("lexamora_studio.ai.run_text")
    def test_selected_text_preview_preserves_unselected_prompt(self, mocked_run_text):
        mocked_run_text.return_value = (json.dumps({"content": "pokoj"}), "gpt-5.4-mini")
        self.client.force_login(self.editor)
        source = "A room at night"
        response = self.client.post(
            f"/studio/prompts/{self.prompt.id}/ai-preview/",
            data=json.dumps({
                "action": "translate", "content": source, "targetLanguage": "PL",
                "scope": "SELECTED", "selectionStart": 2, "selectionEnd": 6,
                "textModel": "gpt-5.4-mini",
            }),
            content_type="application/json",
        )
        self.assertEqual(response.status_code, 200, response.content)
        self.assertEqual(response.json()["content"], "A pokoj at night")
        provider_payload = json.loads(mocked_run_text.call_args.args[1])
        self.assertEqual(provider_payload["content"], "room")

    @patch("lexamora_studio.ai.run_text")
    def test_dialogue_preview_updates_spoken_language_label(self, mocked_run_text):
        mocked_run_text.return_value = (
            json.dumps({"content": 'Hero says in Polish: "Dzien dobry". Camera stays wide.'}),
            "gpt-5.4-mini",
        )
        self.client.force_login(self.editor)
        response = self.client.post(
            f"/studio/prompts/{self.prompt.id}/ai-preview/",
            data=json.dumps({
                "action": "translate", "content": 'Hero says in Polish: "Dzien dobry". Camera stays wide.',
                "targetLanguage": "RU", "scope": "DIALOGUE", "textModel": "gpt-5.4-mini",
            }),
            content_type="application/json",
        )
        self.assertEqual(response.status_code, 200, response.content)
        self.assertEqual(response.json()["content"], 'Hero says in Russian: "Dzien dobry". Camera stays wide.')
        provider_payload = json.loads(mocked_run_text.call_args.args[1])
        self.assertIn("in Russian", " ".join(provider_payload["rules"]))

    @patch("lexamora_studio.ai.run_text")
    def test_improve_creates_suggestion_without_overwriting_or_sending_dialogue(self, mocked_run_text):
        from .models import AiSuggestion, AiUsageLog

        mocked_run_text.return_value = (self.provider_response(), "gpt-5.4-mini")
        self.client.force_login(self.editor)
        response = self.client.post(
            f"/api/v1/studio/prompts/{self.prompt.id}/improve",
            data=json.dumps({"mode": "cinematic", "textModel": "gpt-5.4-mini"}),
            content_type="application/json",
        )
        self.assertEqual(response.status_code, 201, response.content)
        self.narrative.refresh_from_db()
        self.assertEqual(self.narrative.content, "Plain room")
        self.assertEqual(AiSuggestion.objects.get().status, AiSuggestion.Status.PENDING)
        self.assertEqual(AiUsageLog.objects.get().status, "SUCCESS")
        provider_prompt = mocked_run_text.call_args.args[1]
        self.assertNotIn("Secret dialogue", provider_prompt)
        self.assertIn(self.prompt.template.content, provider_prompt)

    @patch("lexamora_studio.ai.run_text")
    def test_russian_prompt_can_be_improved_and_translated_to_english(self, mocked_run_text):
        self.narrative.content = "Простая комната и герой входит"
        self.narrative.save(update_fields=["content", "updated_at"])
        mocked_run_text.return_value = (
            json.dumps({"blocks": [{"id": str(self.narrative.id), "content": "A cinematic room as the hero enters."}]}),
            "gpt-5.4",
        )
        self.client.force_login(self.editor)
        response = self.client.post(
            f"/api/v1/studio/prompts/{self.prompt.id}/improve",
            data=json.dumps({"mode": "improve_translate_en", "textModel": "gpt-5.4"}),
            content_type="application/json",
        )
        self.assertEqual(response.status_code, 201, response.content)
        self.assertEqual(response.json()["model"], "gpt-5.4")
        provider_prompt = mocked_run_text.call_args.args[1]
        self.assertIn("Return every editable block in English", provider_prompt)

    @patch("lexamora_studio.ai.run_text")
    def test_dialogue_translation_creates_new_prompt_and_preserves_source(self, mocked_run_text):
        from .models import AiUsageLog, Prompt, TranslationUnit

        mocked_run_text.return_value = (
            json.dumps({"blocks": [{"id": str(self.dialogue.id), "content": "Tajny dialog"}]}),
            "gpt-5.4-mini",
        )
        self.client.force_login(self.editor)
        response = self.client.post(
            f"/api/v1/studio/prompts/{self.prompt.id}/translate-dialogue",
            data=json.dumps({"targetLanguage": "PL", "textModel": "gpt-5.4-mini"}),
            content_type="application/json",
        )
        self.assertEqual(response.status_code, 201, response.content)
        self.narrative.refresh_from_db()
        self.dialogue.refresh_from_db()
        self.assertEqual(self.narrative.content, "Plain room")
        self.assertEqual(self.dialogue.content, "Secret dialogue")
        translated_prompt = Prompt.objects.get(id=response.json()["promptId"])
        translated_blocks = list(translated_prompt.blocks.order_by("position"))
        self.assertEqual(translated_prompt.source_prompt, self.prompt)
        self.assertEqual(translated_prompt.language, "PL")
        self.assertEqual(translated_prompt.translation_scope, Prompt.TranslationScope.DIALOGUE)
        self.assertEqual(translated_blocks[0].content, "Plain room")
        self.assertEqual(translated_blocks[1].content, "Tajny dialog")
        self.assertTrue(TranslationUnit.objects.filter(
            dialogue_line=self.source_dialogue, target_language="PL", translated_text="Tajny dialog"
        ).exists())
        self.assertTrue(AiUsageLog.objects.filter(action="TRANSLATE_DIALOGUE", status="SUCCESS").exists())
        provider_prompt = mocked_run_text.call_args.args[1]
        self.assertNotIn("Plain room", provider_prompt)

    @patch("lexamora_studio.ai.run_text")
    def test_full_translation_creates_new_prompt_with_every_block_translated(self, mocked_run_text):
        from .models import Prompt

        mocked_run_text.return_value = (
            json.dumps({"blocks": [
                {"id": str(self.narrative.id), "content": "Pokoj"},
                {"id": str(self.dialogue.id), "content": "Tajny dialog"},
            ]}),
            "gpt-5.4-mini",
        )
        self.client.force_login(self.editor)
        response = self.client.post(
            f"/api/v1/studio/prompts/{self.prompt.id}/translate",
            data=json.dumps({"targetLanguage": "PL", "scope": "FULL", "textModel": "gpt-5.4-mini"}),
            content_type="application/json",
        )
        self.assertEqual(response.status_code, 201, response.content)
        translated_prompt = Prompt.objects.get(id=response.json()["promptId"])
        self.assertEqual(translated_prompt.translation_scope, Prompt.TranslationScope.FULL)
        self.assertEqual(list(translated_prompt.blocks.values_list("content", flat=True)), ["Pokoj", "Tajny dialog"])

    @patch("lexamora_studio.ai.run_text")
    def test_accept_updates_non_dialogue_and_preserves_dialogue(self, mocked_run_text):
        from .models import AiSuggestion, Revision

        mocked_run_text.return_value = (self.provider_response(), "gpt-5.4-mini")
        self.client.force_login(self.editor)
        created = self.client.post(
            f"/api/v1/studio/prompts/{self.prompt.id}/improve",
            data=json.dumps({"mode": "non_dialogue"}),
            content_type="application/json",
        )
        accepted = self.client.post(f"/api/v1/studio/suggestions/{created.json()['id']}/accept")
        self.assertEqual(accepted.status_code, 200, accepted.content)
        self.narrative.refresh_from_db()
        self.dialogue.refresh_from_db()
        self.assertEqual(self.narrative.content, "Cinematic room with precise lighting")
        self.assertEqual(self.dialogue.content, "Secret dialogue")
        self.assertEqual(AiSuggestion.objects.get().status, AiSuggestion.Status.ACCEPTED)
        self.assertTrue(Revision.objects.filter(entity_id=self.prompt.id, operation="AI_ACCEPT").exists())

        undone = self.client.post(f"/api/v1/studio/suggestions/{created.json()['id']}/undo")
        self.assertEqual(undone.status_code, 200, undone.content)
        self.narrative.refresh_from_db()
        self.assertEqual(self.narrative.content, "Plain room")
        self.assertEqual(AiSuggestion.objects.get().status, AiSuggestion.Status.UNDONE)
        self.assertTrue(Revision.objects.filter(entity_id=self.prompt.id, operation="AI_UNDO").exists())

    @patch("lexamora_studio.ai.run_text")
    def test_reject_keeps_prompt_unchanged(self, mocked_run_text):
        mocked_run_text.return_value = (self.provider_response(), "gpt-5.4-mini")
        self.client.force_login(self.editor)
        created = self.client.post(
            f"/api/v1/studio/prompts/{self.prompt.id}/improve",
            data=json.dumps({"mode": "shorter"}),
            content_type="application/json",
        )
        rejected = self.client.post(f"/api/v1/studio/suggestions/{created.json()['id']}/reject")
        self.assertEqual(rejected.status_code, 200)
        self.narrative.refresh_from_db()
        self.assertEqual(self.narrative.content, "Plain room")

    def test_user_without_ai_capability_cannot_improve(self):
        self.client.force_login(self.viewer)
        response = self.client.post(
            f"/api/v1/studio/prompts/{self.prompt.id}/improve",
            data=json.dumps({"mode": "non_dialogue"}),
            content_type="application/json",
        )
        self.assertEqual(response.status_code, 403)
    @override_settings(STUDIO_AI_RATE_PER_MINUTE=1)
    @patch("lexamora_studio.ai.run_text")
    def test_ai_rate_limit_returns_429(self, mocked_run_text):
        mocked_run_text.return_value = (self.provider_response(), "gpt-5.4-mini")
        self.client.force_login(self.editor)
        first = self.client.post(
            f"/api/v1/studio/prompts/{self.prompt.id}/improve",
            data=json.dumps({"mode": "non_dialogue"}),
            content_type="application/json",
        )
        second = self.client.post(
            f"/api/v1/studio/prompts/{self.prompt.id}/improve",
            data=json.dumps({"mode": "non_dialogue"}),
            content_type="application/json",
        )
        self.assertEqual(first.status_code, 201)
        self.assertEqual(second.status_code, 429)

    @patch("lexamora_studio.ai.run_text")
    def test_stale_suggestion_cannot_be_accepted(self, mocked_run_text):
        from .revisions import record_revision

        mocked_run_text.return_value = (self.provider_response(), "gpt-5.4-mini")
        self.client.force_login(self.editor)
        created = self.client.post(
            f"/api/v1/studio/prompts/{self.prompt.id}/improve",
            data=json.dumps({"mode": "non_dialogue"}),
            content_type="application/json",
        )
        self.prompt.title = "Changed after suggestion"
        self.prompt.updated_by = self.editor
        self.prompt.save()
        record_revision(instance=self.prompt, user=self.editor, operation="UPDATE")
        accepted = self.client.post(f"/api/v1/studio/suggestions/{created.json()['id']}/accept")
        self.assertEqual(accepted.status_code, 409)
        self.narrative.refresh_from_db()
        self.assertEqual(self.narrative.content, "Plain room")


class StudioSubtitleTranslationTests(TestCase):
    def setUp(self):
        from .models import AiModelProfile, DialogueLine, Episode, Prompt, PromptBlock, Scene

        users = get_user_model()
        self.editor = users.objects.create_user("subtitle-editor", password="strong-pass")
        self.translator = users.objects.create_user("subtitle-translator", password="strong-pass")
        self.viewer = users.objects.create_user("subtitle-viewer", password="strong-pass")
        self.workspace = create_workspace(user=self.editor, name="Subtitle Studio", slug="subtitle-studio")
        WorkspaceMembership.objects.create(workspace=self.workspace, user=self.translator, role=WorkspaceMembership.Role.TRANSLATOR)
        WorkspaceMembership.objects.create(workspace=self.workspace, user=self.viewer, role=WorkspaceMembership.Role.VIEWER)
        self.project = Project.objects.create(
            workspace=self.workspace, project_type=Project.Type.SERIES, title="Subtitle Project",
            created_by=self.editor, updated_by=self.editor,
        )
        self.episode = Episode.objects.create(
            project=self.project, number=1, title="Pilot", created_by=self.editor, updated_by=self.editor,
        )
        self.scene = Scene.objects.create(
            episode=self.episode, number=1, title="Opening", created_by=self.editor, updated_by=self.editor,
        )
        self.line = DialogueLine.objects.create(
            scene=self.scene, speaker="Hero", text="Original source", status=DialogueLine.Status.APPROVED,
            created_by=self.editor, updated_by=self.editor,
        )
        model = AiModelProfile.objects.create(name="Subtitle Test", provider="Test", model_id="test", media_type="VIDEO")
        self.prompt = Prompt.objects.create(
            scene=self.scene, ai_model=model, prompt_type=Prompt.Type.VIDEO,
            created_by=self.editor, updated_by=self.editor,
        )
        PromptBlock.objects.create(
            prompt=self.prompt, block_type=PromptBlock.Type.DIALOGUE_REFERENCE,
            content="Original source", source_dialogue=self.line, created_by=self.editor, updated_by=self.editor,
        )

    def test_translator_can_translate_but_cannot_edit_source(self):
        from .models import TranslationUnit

        self.client.force_login(self.translator)
        translated = self.client.patch(
            f"/api/v1/studio/dialogue/{self.line.id}/translations/pl",
            data=json.dumps({"translation": "Tekst docelowy", "status": "APPROVED"}),
            content_type="application/json",
        )
        self.assertEqual(translated.status_code, 200, translated.content)
        self.assertEqual(TranslationUnit.objects.get().status, TranslationUnit.Status.APPROVED)
        source_edit = self.client.patch(
            f"/api/v1/studio/dialogue/{self.line.id}",
            data=json.dumps({"text": "Forbidden"}),
            content_type="application/json",
        )
        self.assertEqual(source_edit.status_code, 403)

    def test_source_change_marks_translation_stale_and_prompt_for_review(self):
        from .models import TranslationUnit
        from .services import save_translation

        save_translation(
            dialogue_line=self.line, user=self.translator, target_language="pl",
            translated_text="Tekst docelowy", status=TranslationUnit.Status.APPROVED,
        )
        self.client.force_login(self.editor)
        changed = self.client.patch(
            f"/api/v1/studio/dialogue/{self.line.id}",
            data=json.dumps({"text": "Updated source"}),
            content_type="application/json",
        )
        self.assertEqual(changed.status_code, 200)
        self.assertEqual(TranslationUnit.objects.get().status, TranslationUnit.Status.STALE)
        self.prompt.refresh_from_db()
        self.assertTrue(self.prompt.needs_review)

    def test_bulk_subtitles_and_reorder(self):
        from .models import SubtitleLine

        self.client.force_login(self.translator)
        created = self.client.post(
            f"/api/v1/studio/episodes/{self.episode.id}/subtitle-tracks",
            data=json.dumps({"language": "pl", "kind": "WORKING"}),
            content_type="application/json",
        )
        self.assertEqual(created.status_code, 201)
        track_id = created.json()["id"]
        pasted = self.client.post(
            f"/api/v1/studio/subtitle-tracks/{track_id}/lines",
            data=json.dumps({"lines": ["FIRST", "SECOND", "THIRD"]}),
            content_type="application/json",
        )
        self.assertEqual(pasted.status_code, 200, pasted.content)
        ids = [row["id"] for row in pasted.json()["results"]]
        reordered = self.client.post(
            f"/api/v1/studio/subtitle-tracks/{track_id}/lines/reorder",
            data=json.dumps({"lineIds": list(reversed(ids))}),
            content_type="application/json",
        )
        self.assertEqual(reordered.status_code, 200)
        values = list(SubtitleLine.objects.filter(track_id=track_id).values_list("text", flat=True))
        self.assertEqual(values, ["THIRD", "SECOND", "FIRST"])

    def test_viewer_cannot_replace_subtitles(self):
        from .models import SubtitleTrack

        track = SubtitleTrack.objects.create(
            episode=self.episode, language="pl", kind=SubtitleTrack.Kind.WORKING,
            created_by=self.editor, updated_by=self.editor,
        )
        self.client.force_login(self.viewer)
        response = self.client.post(
            f"/api/v1/studio/subtitle-tracks/{track.id}/lines",
            data=json.dumps({"lines": ["Denied"]}),
            content_type="application/json",
        )
        self.assertEqual(response.status_code, 403)


@override_settings(
    STUDIO_PDF_FONT_PATH="C:/Windows/Fonts/arial.ttf",
    STUDIO_EXPORT_RATE_PER_HOUR=10,
)
class StudioExportTests(TestCase):
    def setUp(self):
        import tempfile
        from .models import DialogueLine, Episode, Scene, SubtitleLine, SubtitleTrack, private_storage

        self.temp_media = tempfile.TemporaryDirectory()
        self.private_storage = private_storage
        self.previous_location = private_storage._location
        private_storage._location = self.temp_media.name
        private_storage.__dict__.pop("base_location", None)
        private_storage.__dict__.pop("location", None)

        users = get_user_model()
        self.owner = users.objects.create_user("export-owner", password="strong-pass")
        self.viewer = users.objects.create_user("export-viewer", password="strong-pass")
        self.workspace = create_workspace(user=self.owner, name="Export Studio", slug="export-studio")
        WorkspaceMembership.objects.create(workspace=self.workspace, user=self.viewer, role=WorkspaceMembership.Role.VIEWER)
        self.project = Project.objects.create(
            workspace=self.workspace, project_type=Project.Type.SERIES,
            title="Dom serial", concept="Historia domu", original_language="pl",
            translation_languages=["ru"], rights_holder="Leksa Programs",
            created_by=self.owner, updated_by=self.owner,
        )
        self.episode = Episode.objects.create(
            project=self.project, number=1, title="Ostatnie zadanie",
            summary="Pilot episode", created_by=self.owner, updated_by=self.owner,
        )
        scene = Scene.objects.create(
            episode=self.episode, number=1, title="Office",
            description="A cinematic office scene", created_by=self.owner, updated_by=self.owner,
        )
        DialogueLine.objects.create(
            scene=scene, speaker="Hero", text="Dzien dobry",
            created_by=self.owner, updated_by=self.owner,
        )
        track = SubtitleTrack.objects.create(
            episode=self.episode, language="pl", kind=SubtitleTrack.Kind.WORKING,
            created_by=self.owner, updated_by=self.owner,
        )
        SubtitleLine.objects.create(
            track=track, position=0, text="DZIEN DOBRY",
            created_by=self.owner, updated_by=self.owner,
        )

    def tearDown(self):
        self.private_storage._location = self.previous_location
        self.private_storage.__dict__.pop("base_location", None)
        self.private_storage.__dict__.pop("location", None)
        self.temp_media.cleanup()

    def test_pdf_export_creates_private_asset_and_audit_history(self):
        from .models import AccessEvent, AuditEvent, ExportJob

        self.client.force_login(self.owner)
        response = self.client.post(
            f"/api/v1/studio/projects/{self.project.id}/export/pdf",
            data=json.dumps({"sections": ["metadata", "episodes", "scenes", "dialogue", "subtitles"]}),
            content_type="application/json",
        )
        self.assertEqual(response.status_code, 201, response.content)
        job = ExportJob.objects.get(id=response.json()["id"])
        self.assertEqual(job.status, ExportJob.Status.SUCCESS)
        self.assertTrue(job.output_asset.file.storage.exists(job.output_asset.file.name))
        with job.output_asset.file.open("rb") as stream:
            self.assertEqual(stream.read(4), b"%PDF")
        self.assertTrue(AuditEvent.objects.filter(action="EXPORT_GENERATED").exists())

        download = self.client.get(f"/api/v1/studio/exports/{job.id}/download")
        self.assertEqual(download.status_code, 200)
        download.close()
        self.assertTrue(AuditEvent.objects.filter(action="EXPORT_DOWNLOAD").exists())
        self.assertTrue(AccessEvent.objects.filter(asset=job.output_asset, action="DOWNLOAD").exists())

    def test_viewer_cannot_generate_export(self):
        self.client.force_login(self.viewer)
        response = self.client.post(
            f"/api/v1/studio/projects/{self.project.id}/export/pdf",
            data=json.dumps({"sections": []}),
            content_type="application/json",
        )
        self.assertEqual(response.status_code, 403)

    @override_settings(STUDIO_EXPORT_RATE_PER_HOUR=1)
    def test_export_rate_limit(self):
        self.client.force_login(self.owner)
        first = self.client.post(
            f"/api/v1/studio/projects/{self.project.id}/export/pdf",
            data=json.dumps({"sections": ["metadata"]}),
            content_type="application/json",
        )
        second = self.client.post(
            f"/api/v1/studio/projects/{self.project.id}/export/pdf",
            data=json.dumps({"sections": ["metadata"]}),
            content_type="application/json",
        )
        self.assertEqual(first.status_code, 201)
        self.assertEqual(second.status_code, 429)

class StudioWebCreationTests(TestCase):
    def setUp(self):
        users = get_user_model()
        self.owner = users.objects.create_user("web-owner", password="strong-pass")
        self.viewer = users.objects.create_user("web-viewer", password="strong-pass")
        self.workspace = create_workspace(user=self.owner, name="Web Studio", slug="web-studio")
        WorkspaceMembership.objects.create(workspace=self.workspace, user=self.viewer, role=WorkspaceMembership.Role.VIEWER)

    def test_owner_can_create_project_episode_and_scene_in_web_ui(self):
        from .models import Episode, Scene

        self.client.force_login(self.owner)
        response = self.client.post(
            f"/studio/workspaces/{self.workspace.id}/projects/new/",
            {"project_type": "SERIES", "title": "Browser Project", "original_language": "ru", "translation_languages": "en, pl", "status": "DRAFT"},
        )
        project = Project.objects.get(title="Browser Project")
        self.assertRedirects(response, f"/studio/projects/{project.id}/")
        self.assertEqual(project.translation_languages, ["en", "pl"])

        response = self.client.post(
            f"/studio/projects/{project.id}/episodes/new/",
            {"number": 1, "title": "Pilot"},
        )
        episode = Episode.objects.get(project=project)
        self.assertRedirects(response, f"/studio/projects/{project.id}/")

        response = self.client.post(
            f"/studio/episodes/{episode.id}/scenes/new/",
            {"number": 1, "title": "Opening", "status": "DRAFT"},
        )
        scene = Scene.objects.get(episode=episode)
        self.assertRedirects(response, f"/studio/scenes/{scene.id}/")

    def test_viewer_cannot_open_project_creation_form(self):
        self.client.force_login(self.viewer)
        response = self.client.get(f"/studio/workspaces/{self.workspace.id}/projects/new/")
        self.assertEqual(response.status_code, 403)

class StudioWebEditingAndImagesTests(TestCase):
    def setUp(self):
        from .models import Episode, Scene

        users = get_user_model()
        self.owner = users.objects.create_user("web-edit-owner", password="strong-pass")
        self.viewer = users.objects.create_user("web-edit-viewer", password="strong-pass")
        self.outsider = users.objects.create_user("web-edit-outsider", password="strong-pass")
        self.workspace = create_workspace(user=self.owner, name="Editable Studio", slug="editable-studio")
        WorkspaceMembership.objects.create(workspace=self.workspace, user=self.viewer, role=WorkspaceMembership.Role.VIEWER)
        self.project = Project.objects.create(workspace=self.workspace, project_type=Project.Type.SERIES, title="Before", original_language="ru", translation_languages=["en"], created_by=self.owner, updated_by=self.owner)
        self.episode = Episode.objects.create(project=self.project, number=1, title="Pilot", created_by=self.owner, updated_by=self.owner)
        self.scene = Scene.objects.create(episode=self.episode, number=1, title="Opening", created_by=self.owner, updated_by=self.owner)

    @staticmethod
    def image_file(name="reference.png", color="#36a889"):
        import io
        from PIL import Image
        from django.core.files.uploadedfile import SimpleUploadedFile

        buffer = io.BytesIO()
        Image.new("RGB", (48, 36), color).save(buffer, "PNG")
        return SimpleUploadedFile(name, buffer.getvalue(), content_type="image/png")

    def test_owner_can_edit_project_and_revision_is_recorded(self):
        from .models import Revision

        self.client.force_login(self.owner)
        response = self.client.post(
            f"/studio/projects/{self.project.id}/edit/",
            {"project_type": "SERIES", "title": "After", "concept": "Updated", "original_language": "ru", "translation_languages": "en, pl", "status": "DRAFT"},
        )
        self.assertRedirects(response, f"/studio/projects/{self.project.id}/")
        self.project.refresh_from_db()
        self.assertEqual(self.project.title, "After")
        self.assertEqual(self.project.translation_languages, ["en", "pl"])
        self.assertTrue(Revision.objects.filter(entity_id=self.project.id, operation="UPDATE").exists())

    def test_project_language_change_requires_confirmation_and_propagates(self):
        from .models import AiModelProfile, DialogueLine, Prompt

        model = AiModelProfile.objects.create(
            name="Language image model", provider="test", model_id="language-image",
            media_type=AiModelProfile.MediaType.IMAGE,
        )
        original = Prompt.objects.create(
            scene=self.scene, ai_model=model, prompt_type=Prompt.Type.IMAGE,
            content="Original prompt", created_by=self.owner, updated_by=self.owner,
        )
        translated = Prompt.objects.create(
            scene=self.scene, ai_model=model, source_prompt=original, language="EN",
            prompt_type=Prompt.Type.IMAGE, position=1, content="Translated prompt",
            created_by=self.owner, updated_by=self.owner,
        )
        dialogue = DialogueLine.objects.create(
            scene=self.scene, speaker="Hero", text="Czesc", language="RU",
            created_by=self.owner, updated_by=self.owner,
        )
        payload = {
            "project_type": "SERIES", "title": "Before", "original_language": "PL",
            "translation_languages": "en", "status": "DRAFT",
        }
        self.client.force_login(self.owner)
        form_page = self.client.get(f"/studio/projects/{self.project.id}/edit/")
        self.assertContains(form_page, "data-language-propagation-dialog")
        self.assertNotContains(form_page, '<aside class="form-warning">')

        rejected = self.client.post(f"/studio/projects/{self.project.id}/edit/", payload)
        self.assertEqual(rejected.status_code, 200)
        self.assertContains(rejected, "Confirm that the new language")
        self.project.refresh_from_db()
        self.assertEqual(self.project.original_language, "ru")

        payload["confirm_language_propagation"] = "on"
        saved = self.client.post(f"/studio/projects/{self.project.id}/edit/", payload, follow=True)
        self.assertRedirects(saved, f"/studio/projects/{self.project.id}/")
        self.assertContains(saved, "Updated 1 original prompts, 1 translations, and 1 dialogue lines")
        self.project.refresh_from_db()
        original.refresh_from_db()
        translated.refresh_from_db()
        dialogue.refresh_from_db()
        self.assertEqual(self.project.original_language, "PL")
        self.assertEqual((original.original_language, original.language), ("PL", "PL"))
        self.assertEqual((translated.original_language, translated.language), ("PL", "EN"))
        self.assertEqual(dialogue.language, "PL")

    def test_viewer_cannot_edit_project(self):
        self.client.force_login(self.viewer)
        response = self.client.get(f"/studio/projects/{self.project.id}/edit/")
        self.assertEqual(response.status_code, 403)

    def test_api_language_change_also_requires_propagation_confirmation(self):
        self.client.force_login(self.owner)
        denied = self.client.patch(
            f"/api/v1/studio/projects/{self.project.id}",
            data=json.dumps({"originalLanguage": "PL"}),
            content_type="application/json",
        )
        self.assertEqual(denied.status_code, 409, denied.content)
        self.assertEqual(denied.json()["error"]["code"], "language_propagation_confirmation_required")
        self.project.refresh_from_db()
        self.assertEqual(self.project.original_language, "ru")

        saved = self.client.patch(
            f"/api/v1/studio/projects/{self.project.id}",
            data=json.dumps({"originalLanguage": "PL", "confirmLanguagePropagation": True}),
            content_type="application/json",
        )
        self.assertEqual(saved.status_code, 200, saved.content)
        self.assertEqual(saved.json()["originalLanguage"], "PL")

    def test_owner_can_upload_private_scene_image_and_open_thumbnail(self):
        import tempfile
        from pathlib import Path
        from .models import Asset

        self.client.force_login(self.owner)
        with tempfile.TemporaryDirectory() as directory:
            with self.settings(STUDIO_PRIVATE_MEDIA_ROOT=Path(directory)):
                response = self.client.post(f"/studio/scenes/{self.scene.id}/images/new/", {"file": self.image_file()})
                asset = Asset.objects.get(scene=self.scene)
                self.assertRedirects(response, f"/studio/scenes/{self.scene.id}/")
                self.assertEqual(asset.kind, Asset.Kind.SCENE_IMAGE)
                self.assertEqual((asset.width, asset.height), (48, 36))
                thumbnail = self.client.get(f"/api/v1/studio/assets/{asset.id}/thumbnail")
                self.assertEqual(thumbnail.status_code, 200)

    def test_scene_accepts_multiple_images_and_rejects_workspace_duplicate(self):
        import tempfile
        from pathlib import Path
        from .models import Asset

        self.client.force_login(self.owner)
        with tempfile.TemporaryDirectory() as directory:
            with self.settings(STUDIO_PRIVATE_MEDIA_ROOT=Path(directory)):
                response = self.client.post(
                    f"/studio/scenes/{self.scene.id}/images/new/",
                    {"file": [self.image_file("one.png", "#113355"), self.image_file("two.png", "#557799")]},
                )
                self.assertEqual(response.status_code, 302)
                self.assertEqual(Asset.objects.filter(scene=self.scene).count(), 2)
                duplicate = self.client.post(
                    f"/studio/scenes/{self.scene.id}/images/new/",
                    {"file": self.image_file("one.png", "#113355")},
                    follow=True,
                )
                self.assertContains(duplicate, "already uploaded in this workspace")
                self.assertEqual(Asset.objects.filter(scene=self.scene).count(), 2)

    def test_outsider_cannot_open_scene_image_form(self):
        self.client.force_login(self.outsider)
        self.assertEqual(self.client.get(f"/studio/scenes/{self.scene.id}/images/new/").status_code, 404)

class StudioDocxImportTests(TestCase):
    def setUp(self):
        import tempfile
        from .models import private_storage

        self.temp_media = tempfile.TemporaryDirectory()
        self.private_storage = private_storage
        self.previous_location = private_storage._location
        private_storage._location = self.temp_media.name
        private_storage.__dict__.pop("base_location", None)
        private_storage.__dict__.pop("location", None)
        users = get_user_model()
        self.owner = users.objects.create_user("docx-owner", password="strong-pass")
        self.viewer = users.objects.create_user("docx-viewer", password="strong-pass")
        self.workspace = create_workspace(user=self.owner, name="Import Studio", slug="import-studio")
        WorkspaceMembership.objects.create(workspace=self.workspace, user=self.viewer, role=WorkspaceMembership.Role.VIEWER)

    def tearDown(self):
        self.private_storage._location = self.previous_location
        self.private_storage.__dict__.pop("base_location", None)
        self.private_storage.__dict__.pop("location", None)
        self.temp_media.cleanup()

    @staticmethod
    def docx_file():
        import io
        import zipfile
        from PIL import Image
        from django.core.files.uploadedfile import SimpleUploadedFile

        namespace = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
        paragraphs = [
            ("", "DOM TEST"), ("Heading1", "СЕРИЯ 1: Pilot"), ("Heading3", "Краткое описание серии"), ("", "Summary"),
            ("Heading1", "Сцена 1. Opening"), ("Heading3", "Общее описание сцены"), ("", "A room"),
            ("Heading3", "Диалоги"), ("", 'Hero: “Hello!”'),
        ]
        body = []
        for style, text in paragraphs:
            ppr = f'<w:pPr><w:pStyle w:val="{style}"/></w:pPr>' if style else ""
            body.append(f'<w:p>{ppr}<w:r><w:t>{text}</w:t></w:r></w:p>')
        xml = f'<?xml version="1.0" encoding="UTF-8"?><w:document xmlns:w="{namespace}"><w:body>{"".join(body)}</w:body></w:document>'
        image = io.BytesIO()
        Image.new("RGB", (20, 12), "#227755").save(image, "PNG")
        output = io.BytesIO()
        with zipfile.ZipFile(output, "w") as archive:
            archive.writestr("[Content_Types].xml", "<Types/>")
            archive.writestr("word/document.xml", xml)
            archive.writestr("word/media/image1.png", image.getvalue())
        return SimpleUploadedFile("master.docx", output.getvalue(), content_type="application/vnd.openxmlformats-officedocument.wordprocessingml.document")

    def test_owner_can_preview_and_accept_docx_with_embedded_image(self):
        from .models import Asset, DialogueLine, DocxImport, Episode, Scene

        self.client.force_login(self.owner)
        response = self.client.post(f"/studio/workspaces/{self.workspace.id}/imports/docx/new/", {"file": self.docx_file()})
        draft = DocxImport.objects.get()
        self.assertRedirects(response, f"/studio/imports/{draft.id}/")
        self.assertEqual(draft.parsed_data["image_count"], 1)
        self.assertEqual(len(draft.parsed_data["episodes"]), 1)
        accepted = self.client.post(f"/studio/imports/{draft.id}/accept/")
        draft.refresh_from_db()
        self.assertRedirects(accepted, f"/studio/projects/{draft.project_id}/")
        self.assertEqual(Episode.objects.filter(project=draft.project).count(), 1)
        self.assertEqual(Scene.objects.filter(episode__project=draft.project).count(), 1)
        self.assertEqual(DialogueLine.objects.filter(scene__episode__project=draft.project).count(), 1)
        self.assertEqual(Asset.objects.filter(project=draft.project, content_type="image/png").count(), 1)

    def test_import_edit_export_docx_round_trip_uses_current_project_data(self):
        from docx import Document
        from .docx_exports import generate_docx_export
        from .models import DocxImport

        self.client.force_login(self.owner)
        self.client.post(f"/studio/workspaces/{self.workspace.id}/imports/docx/new/", {"file": self.docx_file()})
        draft = DocxImport.objects.get()
        self.client.post(f"/studio/imports/{draft.id}/accept/")
        draft.refresh_from_db()
        scene = draft.project.episodes.first().scenes.first()
        scene.title = "Edited opening"
        scene.updated_by = self.owner
        scene.save()
        job = generate_docx_export(project=draft.project, user=self.owner)
        with job.output_asset.file.open("rb") as stream:
            document = Document(stream)
        text = "\n".join(paragraph.text for paragraph in document.paragraphs)
        self.assertIn("Edited opening", text)
        self.assertGreaterEqual(len(document.inline_shapes), 1)
        self.assertEqual(job.sections, ["DOCX"])
    def test_owner_can_edit_preview_before_accepting(self):
        from .models import AuditEvent, DocxImport

        self.client.force_login(self.owner)
        self.client.post(f"/studio/workspaces/{self.workspace.id}/imports/docx/new/", {"file": self.docx_file()})
        draft = DocxImport.objects.get()
        saved = self.client.post(
            f"/studio/imports/{draft.id}/",
            {"title": "Edited project", "episode_0_include": "on", "episode_0_title": "Edited pilot", "episode_0_scene_0_include": "on", "episode_0_scene_0_title": "Edited scene"},
        )
        self.assertRedirects(saved, f"/studio/imports/{draft.id}/")
        draft.refresh_from_db()
        self.assertEqual(draft.parsed_data["title"], "Edited project")
        self.assertEqual(draft.parsed_data["episodes"][0]["scenes"][0]["title"], "Edited scene")
        self.assertTrue(AuditEvent.objects.filter(action="DOCX_IMPORT_PREVIEW_EDITED").exists())
        self.client.post(f"/studio/imports/{draft.id}/accept/")
        draft.refresh_from_db()
        self.assertEqual(draft.project.title, "Edited project")
        self.assertEqual(draft.project.episodes.first().scenes.first().title, "Edited scene")
    def test_docx_export_redirects_to_roundtrip_report_before_download(self):
        import io
        from PIL import Image
        from django.core.files.uploadedfile import SimpleUploadedFile
        from docx import Document
        from docx.shared import Inches
        from .models import AuditEvent, DocxImport, ExportJob

        image = io.BytesIO()
        Image.new("RGB", (40, 30), "#557799").save(image, "PNG")
        image.seek(0)
        document = Document()
        document.add_heading("Round trip source", 0)
        document.add_heading("EPISODE 1: Pilot", 1)
        document.add_heading("\u0421\u0446\u0435\u043d\u0430 1. Opening", 1)
        document.add_heading("\u041e\u0431\u0449\u0435\u0435 \u043e\u043f\u0438\u0441\u0430\u043d\u0438\u0435 \u0441\u0446\u0435\u043d\u044b", 3)
        document.add_paragraph("A room with a window.")
        table = document.add_table(rows=2, cols=2)
        table.cell(0, 0).text = "Field"
        table.cell(0, 1).text = "Value"
        table.cell(1, 0).text = "Mood"
        table.cell(1, 1).text = "Quiet"
        document.add_picture(image, width=Inches(1))
        payload = io.BytesIO()
        document.save(payload)

        self.client.force_login(self.owner)
        self.client.post(
            f"/studio/workspaces/{self.workspace.id}/imports/docx/new/",
            {"file": SimpleUploadedFile(
                "round-trip.docx",
                payload.getvalue(),
                content_type="application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            )},
        )
        draft = DocxImport.objects.get()
        self.client.post(f"/studio/imports/{draft.id}/accept/")
        draft.refresh_from_db()

        generated = self.client.post(
            f"/studio/projects/{draft.project_id}/exports/",
            {"format": "docx", "sections": ["metadata"]},
        )
        job = ExportJob.objects.get(project=draft.project, sections=["DOCX"])
        self.assertRedirects(generated, f"/studio/exports/{job.id}/round-trip/")
        report = self.client.get(f"/studio/exports/{job.id}/round-trip/")
        self.assertEqual(report.status_code, 200)
        self.assertContains(report, "DOCX round-trip report")
        self.assertContains(report, "Structure")
        self.assertContains(report, "Texts")
        self.assertContains(report, "Tables")
        self.assertContains(report, "Images")
        self.assertContains(report, "Section order")
        self.assertContains(report, f"/api/v1/studio/exports/{job.id}/download")
        self.assertTrue(AuditEvent.objects.filter(action="DOCX_ROUNDTRIP_REVIEWED").exists())

        history = self.client.get(f"/studio/projects/{draft.project_id}/exports/")
        self.assertContains(history, "Review &amp; download", html=True)
    def test_viewer_cannot_upload_docx(self):
        self.client.force_login(self.viewer)
        response = self.client.get(f"/studio/workspaces/{self.workspace.id}/imports/docx/new/")
        self.assertEqual(response.status_code, 403)

class StudioMasterDocumentTests(TestCase):
    def setUp(self):
        from .models import DialogueLine, Episode, Scene

        users = get_user_model()
        self.owner = users.objects.create_user("master-owner", password="strong-pass")
        self.viewer = users.objects.create_user("master-viewer", password="strong-pass")
        self.workspace = create_workspace(user=self.owner, name="Master Studio", slug="master-studio")
        WorkspaceMembership.objects.create(workspace=self.workspace, user=self.viewer, role=WorkspaceMembership.Role.VIEWER)
        self.project = Project.objects.create(
            workspace=self.workspace,
            project_type=Project.Type.SERIES,
            title="Master Project",
            concept="A complete document",
            original_language="ru",
            created_by=self.owner,
            updated_by=self.owner,
        )
        self.episode = Episode.objects.create(
            project=self.project, number=1, title="Pilot", position=0,
            created_by=self.owner, updated_by=self.owner,
        )
        self.first_scene = Scene.objects.create(
            episode=self.episode, number=1, title="First scene", position=0,
            created_by=self.owner, updated_by=self.owner,
        )
        self.second_scene = Scene.objects.create(
            episode=self.episode, number=2, title="Second scene", position=1,
            created_by=self.owner, updated_by=self.owner,
        )
        self.first_line = DialogueLine.objects.create(
            scene=self.first_scene, speaker="One", text="First line", position=0,
            created_by=self.owner, updated_by=self.owner,
        )
        self.second_line = DialogueLine.objects.create(
            scene=self.first_scene, speaker="Two", text="Second line", position=1,
            created_by=self.owner, updated_by=self.owner,
        )

    def test_member_can_open_master_document(self):
        self.client.force_login(self.viewer)
        response = self.client.get(f"/studio/projects/{self.project.id}/master/")
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, "Master Project")
        self.assertContains(response, "First scene")
        self.assertContains(response, "Second line")
        self.assertNotContains(response, f"/studio/move/scene/{self.first_scene.id}/up/")

    def test_owner_can_reorder_scenes_and_records_history(self):
        from .models import AuditEvent, Revision, Scene

        self.client.force_login(self.owner)
        response = self.client.post(f"/studio/move/scene/{self.second_scene.id}/up/")
        self.assertRedirects(
            response,
            f"/studio/projects/{self.project.id}/master/#item-{self.second_scene.id}",
        )
        self.assertEqual(
            list(Scene.objects.filter(episode=self.episode).order_by("position").values_list("id", flat=True)),
            [self.second_scene.id, self.first_scene.id],
        )
        self.assertTrue(Revision.objects.filter(entity_id=self.second_scene.id, operation="REORDER").exists())
        self.assertTrue(AuditEvent.objects.filter(entity_id=self.second_scene.id, action="ENTITY_REORDERED").exists())

    def test_dialogue_reorder_respects_unique_positions(self):
        from .models import DialogueLine

        self.client.force_login(self.owner)
        response = self.client.post(f"/studio/move/dialogue/{self.second_line.id}/up/")
        self.assertEqual(response.status_code, 302)
        self.assertEqual(
            list(DialogueLine.objects.filter(scene=self.first_scene).order_by("position").values_list("id", flat=True)),
            [self.second_line.id, self.first_line.id],
        )

    def test_viewer_cannot_reorder(self):
        self.client.force_login(self.viewer)
        response = self.client.post(f"/studio/move/scene/{self.second_scene.id}/up/")
        self.assertEqual(response.status_code, 403)
        self.first_scene.refresh_from_db()
        self.second_scene.refresh_from_db()
        self.assertEqual((self.first_scene.position, self.second_scene.position), (0, 1))

class StudioImageGenerationWorkflowTests(TestCase):
    def setUp(self):
        from .models import Episode, Scene

        users = get_user_model()
        self.owner = users.objects.create_user("generation-owner", password="strong-pass")
        self.viewer = users.objects.create_user("generation-viewer", password="strong-pass")
        self.workspace = create_workspace(user=self.owner, name="Generation Studio", slug="generation-studio")
        WorkspaceMembership.objects.create(
            workspace=self.workspace, user=self.viewer, role=WorkspaceMembership.Role.VIEWER
        )
        self.project = Project.objects.create(
            workspace=self.workspace,
            project_type=Project.Type.SERIES,
            title="Visual Project",
            created_by=self.owner,
            updated_by=self.owner,
        )
        self.episode = Episode.objects.create(
            project=self.project, number=1, title="Pilot",
            created_by=self.owner, updated_by=self.owner,
        )
        self.scene = Scene.objects.create(
            episode=self.episode, number=1, title="Visual scene",
            created_by=self.owner, updated_by=self.owner,
        )

    @staticmethod
    def image_file(name="result.png"):
        import io
        from PIL import Image
        from django.core.files.uploadedfile import SimpleUploadedFile

        buffer = io.BytesIO()
        Image.new("RGB", (64, 48), "#54a778").save(buffer, "PNG")
        return SimpleUploadedFile(name, buffer.getvalue(), content_type="image/png")

    def test_owner_can_create_generation_upload_result_and_select_final(self):
        import tempfile
        from pathlib import Path
        from .models import AdditionalGeneration, AuditEvent, GenerationOutput, Revision

        self.client.force_login(self.owner)
        created = self.client.post(
            f"/studio/scenes/{self.scene.id}/generations/new/",
            {"reason": "Closer composition", "prompt": "A close-up with softer light", "status": "DRAFT"},
        )
        generation = AdditionalGeneration.objects.exclude(scene=self.scene).get()
        generated_scene = generation.scene
        self.assertEqual(generated_scene.title, "Догенерация 1")
        self.assertGreater(generated_scene.position, self.scene.position)
        self.assertRedirects(created, f"/studio/scenes/{generated_scene.id}/media/")
        self.assertTrue(Revision.objects.filter(entity_id=generation.id, operation="CREATE").exists())

        with tempfile.TemporaryDirectory() as directory:
            with self.settings(STUDIO_PRIVATE_MEDIA_ROOT=Path(directory)):
                uploaded = self.client.post(
                    f"/studio/generations/{generation.id}/outputs/new/",
                    {"file": self.image_file(), "model_name": "Test image model"},
                )
                output = GenerationOutput.objects.get(generation=generation)
                self.assertRedirects(uploaded, f"/studio/scenes/{generated_scene.id}/media/")
                self.assertEqual(output.model_metadata["model"], "Test image model")
                edited = self.client.post(
                    f"/studio/assets/{output.asset_id}/edit/",
                    {"original_filename": "approved-result.png", "kind": "GENERATION_OUTPUT"},
                )
                self.assertRedirects(edited, f"/studio/scenes/{generated_scene.id}/media/")
                output.asset.refresh_from_db()
                self.assertEqual(output.asset.original_filename, "approved-result.png")
                selected = self.client.post(f"/studio/generation-outputs/{output.id}/final/")
                self.assertRedirects(
                    selected,
                    f"/studio/scenes/{generated_scene.id}/media/#generation-{generation.id}",
                )

        output.refresh_from_db()
        generation.refresh_from_db()
        self.assertTrue(output.is_final)
        self.assertEqual(generation.status, AdditionalGeneration.Status.FINAL)
        self.assertTrue(Revision.objects.filter(entity_id=generation.id, operation="FINAL_OUTPUT").exists())
        self.assertTrue(AuditEvent.objects.filter(action="GENERATION_OUTPUT_SELECTED").exists())

    def test_viewer_can_browse_but_cannot_create_generation(self):
        self.client.force_login(self.viewer)
        page = self.client.get(f"/studio/scenes/{self.scene.id}/media/")
        self.assertEqual(page.status_code, 200)
        self.assertContains(page, "Images &amp; additional generations", html=True)
        self.assertNotContains(page, "New generation")
        denied = self.client.post(
            f"/studio/scenes/{self.scene.id}/generations/new/",
            {"reason": "Change", "prompt": "Change it", "status": "DRAFT"},
        )
        self.assertEqual(denied.status_code, 403)

class StudioInlineEditingWorkflowTests(TestCase):
    def setUp(self):
        from .models import AiModelProfile, Episode, Scene

        users = get_user_model()
        self.owner = users.objects.create_user("inline-owner", password="strong-pass")
        self.viewer = users.objects.create_user("inline-viewer", password="strong-pass")
        self.workspace = create_workspace(user=self.owner, name="Inline Studio", slug="inline-studio")
        WorkspaceMembership.objects.create(
            workspace=self.workspace, user=self.viewer, role=WorkspaceMembership.Role.VIEWER
        )
        self.project = Project.objects.create(
            workspace=self.workspace, project_type=Project.Type.SERIES, title="Inline Project",
            created_by=self.owner, updated_by=self.owner,
        )
        self.episode = Episode.objects.create(
            project=self.project, number=1, title="Pilot",
            created_by=self.owner, updated_by=self.owner,
        )
        self.first_scene = Scene.objects.create(
            episode=self.episode, number=1, title="First", position=0,
            created_by=self.owner, updated_by=self.owner,
        )
        self.second_scene = Scene.objects.create(
            episode=self.episode, number=2, title="Second", position=1,
            created_by=self.owner, updated_by=self.owner,
        )
        self.ai_model = AiModelProfile.objects.create(
            name="Inline Image Model", provider="test", model_id="inline-image",
            media_type=AiModelProfile.MediaType.IMAGE,
        )

    @staticmethod
    def image_file(name="prompt.png"):
        import io
        from PIL import Image
        from django.core.files.uploadedfile import SimpleUploadedFile

        output = io.BytesIO()
        Image.new("RGB", (72, 54), "#286c91").save(output, "PNG")
        return SimpleUploadedFile(name, output.getvalue(), content_type="image/png")

    def test_inline_prompt_create_edit_and_image_upload(self):
        import tempfile
        from pathlib import Path
        from .models import Asset, AuditEvent, Prompt, PromptBlock

        self.client.force_login(self.owner)
        self.project.prompt_template = "Camera: locked. No music."
        self.project.save(update_fields=["prompt_template", "updated_at"])
        created = self.client.post(
            f"/studio/scenes/{self.first_scene.id}/prompts/quick-create/",
            {
                "title": "Opening frame",
                "ai_model": str(self.ai_model.id),
                "prompt_type": "IMAGE",
                "status": "DRAFT",
                "content": "Wide establishing shot",
            },
        )
        prompt = Prompt.objects.get(scene=self.first_scene)
        block = prompt.blocks.get()
        self.assertRedirects(created, f"/studio/scenes/{self.first_scene.id}/#prompt-{prompt.id}")
        self.assertEqual(prompt.content, "Wide establishing shot\n\nCamera: locked. No music.")
        self.assertEqual(block.content, prompt.content)
        prompt_page = self.client.get(f"/studio/scenes/{self.first_scene.id}/")
        self.assertContains(prompt_page, "Add Template")

        saved = self.client.post(
            f"/studio/prompts/{prompt.id}/quick-save/",
            {
                "title": "Opening frame revised",
                "ai_model": str(self.ai_model.id),
                "prompt_type": "IMAGE",
                "status": "IN_REVIEW",
                "content": "Closer establishing shot\n\nNo text overlays",
            },
        )
        self.assertRedirects(saved, f"/studio/scenes/{self.first_scene.id}/#prompt-{prompt.id}")
        prompt.refresh_from_db()
        block.refresh_from_db()
        self.assertEqual(prompt.title, "Opening frame revised")
        self.assertEqual(prompt.status, "IN_REVIEW")
        self.assertEqual(prompt.content, "Closer establishing shot\n\nNo text overlays")
        self.assertEqual(block.content, "Closer establishing shot\n\nNo text overlays")
        self.assertEqual(PromptBlock.objects.filter(prompt=prompt).count(), 1)

        ajax_saved = self.client.post(
            f"/studio/prompts/{prompt.id}/quick-save/",
            {
                "title": "Opening frame revised",
                "ai_model": str(self.ai_model.id),
                "prompt_type": "IMAGE",
                "status": "IN_REVIEW",
                "content": "Saved without reloading the scene",
            },
            HTTP_X_REQUESTED_WITH="XMLHttpRequest",
        )
        self.assertEqual(ajax_saved.status_code, 200, ajax_saved.content)
        self.assertEqual(ajax_saved.json()["content"], "Saved without reloading the scene")
        self.assertEqual(ajax_saved.json()["id"], str(prompt.id))
        self.assertIn("previewUrl", ajax_saved.json())

        with tempfile.TemporaryDirectory() as directory:
            with self.settings(STUDIO_PRIVATE_MEDIA_ROOT=Path(directory)):
                uploaded = self.client.post(
                    f"/studio/prompts/{prompt.id}/images/new/",
                    {"file": self.image_file()},
                )
                self.assertRedirects(uploaded, f"/studio/scenes/{self.first_scene.id}/#prompt-{prompt.id}")
                asset = Asset.objects.get(prompt=prompt)
                self.assertEqual(asset.scene, self.first_scene)
                page = self.client.get(f"/studio/scenes/{self.first_scene.id}/")
                self.assertContains(page, asset.original_filename)
                self.assertContains(page, "data-image-modal")
        self.assertTrue(AuditEvent.objects.filter(action="PROMPT_INLINE_UPDATED").exists())

    def test_original_prompt_language_is_editable_and_inherited_by_translations(self):
        from .models import Prompt

        self.client.force_login(self.owner)
        original = Prompt.objects.create(
            scene=self.first_scene, ai_model=self.ai_model, prompt_type=Prompt.Type.IMAGE,
            content="Source", created_by=self.owner, updated_by=self.owner,
        )
        translated = Prompt.objects.create(
            scene=self.first_scene, ai_model=self.ai_model, source_prompt=original,
            language="DE", prompt_type=Prompt.Type.IMAGE, position=1, content="Ubersetzung",
            created_by=self.owner, updated_by=self.owner,
        )
        page = self.client.get(f"/studio/scenes/{self.first_scene.id}/")
        self.assertContains(page, "Original &middot; EN")
        self.assertContains(page, "Translation &middot; DE")
        self.assertNotContains(page, "<label>Original language")
        response = self.client.post(
            f"/studio/prompts/{original.id}/quick-save/",
            {
                "title": "Source prompt", "ai_model": str(self.ai_model.id),
                "original_language": "PL", "prompt_type": "IMAGE", "status": "DRAFT",
                "content": "Source",
            },
            HTTP_X_REQUESTED_WITH="XMLHttpRequest",
        )
        self.assertEqual(response.status_code, 200, response.content)
        self.assertEqual(response.json()["originalLanguage"], "PL")
        self.assertEqual({row["label"] for row in response.json()["versions"]}, {"Original · PL", "Translation · DE"})
        original.refresh_from_db()
        translated.refresh_from_db()
        self.assertEqual((original.original_language, original.language), ("PL", "PL"))
        self.assertEqual((translated.original_language, translated.language), ("PL", "DE"))

    def test_scene_chain_quick_edit_and_drag_reorder_endpoint(self):
        from .models import AuditEvent, Scene

        self.client.force_login(self.owner)
        page = self.client.get(f"/studio/projects/{self.project.id}/scene-chain/")
        self.assertEqual(page.status_code, 200)
        self.assertContains(page, "Scene chain")
        self.assertContains(page, "data-scene-list")
        self.assertContains(page, "drag-handle")
        self.assertContains(page, "data-theme-toggle")
        self.assertContains(page, "timeline-track")
        self.assertContains(page, "scene-editor-deck")
        self.assertContains(page, "data-scroll-direction")
        self.assertContains(page, 'class="scene-editor-panel"')
        self.assertContains(page, "Open prompts and images")
        self.assertNotContains(page, 'class="prompt-editor"')
        self.assertNotContains(page, "asset-picker image-picker-dialog")

        saved = self.client.post(
            f"/studio/scenes/{self.first_scene.id}/quick-save/",
            {
                "number": 1,
                "title": "First revised",
                "hook": "Hook",
                "description": "Updated in chain",
                "location": "Room",
                "actions": "Walks",
                "performance_notes": "Quiet",
                "scene_type": "ORIGINAL",
                "status": "IN_REVIEW",
            },
        )
        self.assertRedirects(saved, f"/studio/projects/{self.project.id}/scene-chain/#scene-{self.first_scene.id}")
        self.first_scene.refresh_from_db()
        self.assertEqual(self.first_scene.title, "First revised")

        reordered = self.client.post(
            f"/studio/episodes/{self.episode.id}/scenes/reorder/",
            data=json.dumps({"sceneIds": [str(self.second_scene.id), str(self.first_scene.id)]}),
            content_type="application/json",
        )
        self.assertEqual(reordered.status_code, 200)
        self.assertEqual(
            list(Scene.objects.filter(episode=self.episode).order_by("position").values_list("id", flat=True)),
            [self.second_scene.id, self.first_scene.id],
        )
        self.assertTrue(AuditEvent.objects.filter(action="SCENES_DRAG_REORDERED").exists())

    def test_scene_editor_updates_type_and_production_status(self):
        self.client.force_login(self.owner)
        response = self.client.post(
            f"/studio/scenes/{self.first_scene.id}/",
            {
                "title": self.first_scene.title,
                "hook": "",
                "description": "Alternative production cut",
                "location": "",
                "actions": "",
                "performance_notes": "",
                "scene_type": "ALTERNATIVE",
                "status": "PRODUCTION",
            },
        )
        self.assertRedirects(response, f"/studio/scenes/{self.first_scene.id}/?focus=top#scene-navigation-top")
        self.first_scene.refresh_from_db()
        self.assertEqual(self.first_scene.scene_type, "ALTERNATIVE")
        self.assertEqual(self.first_scene.status, "PRODUCTION")

    def test_scene_editor_saves_three_language_fields_and_status_comment(self):
        self.client.force_login(self.owner)
        response = self.client.post(
            f"/studio/scenes/{self.first_scene.id}/",
            {
                "title": "Opis sceny", "title_prompt": "Scene description", "title_dialogue": "Opis dialogu",
                "hook": "Dokumentacja", "hook_prompt": "Prompt copy", "hook_dialogue": "Dialog copy",
                "description": "Główny opis", "description_prompt": "Main description",
                "description_dialogue": "Opis docelowy", "location": "Biuro", "location_prompt": "Office",
                "location_dialogue": "Biuro dialog", "actions": "Idzie", "actions_prompt": "Walks",
                "actions_dialogue": "Idzie dialog", "performance_notes": "Spokojnie",
                "performance_notes_prompt": "Calmly", "performance_notes_dialogue": "Spokojnie dialog",
                "scene_type": "ORIGINAL", "status": "PRODUCTION", "status_comment": "Ready for shooting",
            },
        )
        self.assertEqual(response.status_code, 302)
        self.first_scene.refresh_from_db()
        self.assertEqual(self.first_scene.title_prompt, "Scene description")
        self.assertEqual(self.first_scene.description_dialogue, "Opis docelowy")
        self.assertEqual(self.first_scene.status_comment, "Ready for shooting")

    def test_new_character_can_include_multilingual_fields_and_avatar(self):
        import tempfile
        from pathlib import Path
        from .models import Character

        self.client.force_login(self.owner)
        with tempfile.TemporaryDirectory() as directory:
            with self.settings(STUDIO_PRIVATE_MEDIA_ROOT=Path(directory)):
                response = self.client.post(
                    f"/studio/projects/{self.project.id}/characters/new/",
                    {
                        "name": "Kot", "name_prompt": "Cat", "name_dialogue": "Gato",
                        "description": "Opis", "description_prompt": "Description",
                        "description_dialogue": "Descripcion", "visual_description": "Rudy",
                        "visual_description_prompt": "Ginger", "visual_description_dialogue": "Naranja",
                        "avatar_file": self.image_file("cat-avatar.png"),
                    },
                )
                self.assertEqual(response.status_code, 302)
                character = Character.objects.get(project=self.project, name="Kot")
                self.assertEqual(character.name_prompt, "Cat")
                self.assertEqual(character.name_dialogue, "Gato")
                self.assertIsNotNone(character.avatar_asset_id)
                self.assertTrue(character.reference_assets.filter(id=character.avatar_asset_id).exists())

    def test_scene_copy_appends_deep_copy_and_delete_renumbers(self):
        from .models import DialogueLine, Prompt, Scene

        DialogueLine.objects.create(
            scene=self.first_scene, speaker="Cat", text="Hello", position=0,
            created_by=self.owner, updated_by=self.owner,
        )
        Prompt.objects.create(
            scene=self.first_scene, ai_model=self.ai_model, prompt_type="IMAGE",
            title="Frame", content="A ginger cat", position=0, needs_review=True,
            created_by=self.owner, updated_by=self.owner,
        )
        self.first_scene.scene_type = Scene.Type.ALTERNATIVE
        self.first_scene.save(update_fields=["scene_type", "updated_at"])
        self.client.force_login(self.owner)

        copied_response = self.client.post(f"/studio/scenes/{self.first_scene.id}/copy/")
        copied = Scene.objects.get(episode=self.episode, number=3)
        self.assertRedirects(copied_response, f"/studio/scenes/{copied.id}/")
        self.assertEqual((copied.position, copied.scene_type, copied.status), (2, "ALTERNATIVE", "DRAFT"))
        self.assertEqual(copied.dialogue_lines.get().text, "Hello")
        self.assertTrue(copied.prompts.get().needs_review)

        deleted_response = self.client.post(f"/studio/scenes/{self.second_scene.id}/delete/")
        self.assertRedirects(deleted_response, f"/studio/scenes/{copied.id}/")
        ordered = list(Scene.objects.filter(episode=self.episode).order_by("position"))
        self.assertEqual([(scene.id, scene.number, scene.position) for scene in ordered], [
            (self.first_scene.id, 1, 0), (copied.id, 2, 1),
        ])

    def test_scene_pages_show_full_path_actions_and_ordered_project_navigation(self):
        self.client.force_login(self.owner)
        page = self.client.get(f"/studio/scenes/{self.first_scene.id}/")
        self.assertContains(page, 'aria-label="Scene path"')
        self.assertContains(page, "Lexamora Studio")
        self.assertContains(page, f"Project {self.project.title}")
        self.assertContains(page, f'/studio/scenes/{self.first_scene.id}/copy/')
        self.assertContains(page, f'/studio/scenes/{self.first_scene.id}/delete/')
        content = page.content.decode()
        self.assertLess(content.index("General View"), content.index("Master Document"))
        self.assertLess(content.index("Master Document"), content.index("Scene Chain"))
        self.assertLess(content.index("Scene Chain"), content.index("Translations"))

        chain = self.client.get(f"/studio/projects/{self.project.id}/scene-chain/")
        self.assertContains(chain, 'name="scene_type"')
        self.assertContains(chain, "Production")
        self.assertContains(chain, f'/studio/scenes/{self.first_scene.id}/copy/')
        self.assertContains(chain, f'/studio/scenes/{self.first_scene.id}/delete/')

    def test_asset_api_can_attach_image_to_prompt(self):
        import tempfile
        from pathlib import Path
        from .models import Asset, Prompt

        prompt = Prompt.objects.create(
            scene=self.first_scene,
            ai_model=self.ai_model,
            prompt_type="IMAGE",
            title="API prompt",
            created_by=self.owner,
            updated_by=self.owner,
        )
        self.client.force_login(self.owner)
        with tempfile.TemporaryDirectory() as directory:
            with self.settings(STUDIO_PRIVATE_MEDIA_ROOT=Path(directory)):
                response = self.client.post(
                    "/api/v1/studio/assets",
                    {
                        "workspaceId": str(self.workspace.id),
                        "promptId": str(prompt.id),
                        "kind": "OTHER",
                        "file": self.image_file("api-prompt.png"),
                    },
                )
        self.assertEqual(response.status_code, 201)
        self.assertEqual(response.json()["promptId"], str(prompt.id))
        self.assertTrue(Asset.objects.filter(prompt=prompt, scene=self.first_scene).exists())

    def test_scene_detail_is_inline_editor_with_two_way_navigation(self):
        self.client.force_login(self.owner)
        page = self.client.get(f"/studio/scenes/{self.first_scene.id}/")
        self.assertEqual(page.status_code, 200)
        self.assertContains(page, 'id="scene-editor-form"')
        self.assertContains(page, "Create scene", count=1)
        self.assertContains(page, f'/studio/scenes/{self.second_scene.id}/')
        self.assertContains(page, "data-scene-cancel disabled", count=1)
        self.assertNotContains(page, 'name="number"')
        self.assertContains(page, 'id="scene-navigation-bottom"')
        self.assertContains(page, "+ Add model...")

    def test_workspace_avatar_and_manual_generation_model(self):
        import tempfile
        from pathlib import Path
        from .models import AiModelProfile

        self.client.force_login(self.owner)
        form_page = self.client.get(f"/studio/workspaces/{self.workspace.id}/avatar/")
        self.assertContains(form_page, 'enctype="multipart/form-data"')
        with tempfile.TemporaryDirectory() as directory:
            with self.settings(STUDIO_PRIVATE_MEDIA_ROOT=Path(directory)):
                uploaded = self.client.post(f"/studio/workspaces/{self.workspace.id}/avatar/", {"file": self.image_file("workspace.png")})
                self.assertRedirects(uploaded, f"/studio/workspaces/{self.workspace.id}/")
                self.workspace.refresh_from_db()
                self.assertIsNotNone(self.workspace.avatar_asset_id)
        created = self.client.post(
            f"/studio/workspaces/{self.workspace.id}/models/",
            {"name": "Manual Motion", "provider": "Custom", "model_id": "custom/motion-v1", "media_type": "VIDEO"},
        )
        self.assertRedirects(created, f"/studio/workspaces/{self.workspace.id}/models/")
        self.assertTrue(AiModelProfile.objects.filter(name="Manual Motion", model_id="custom/motion-v1").exists())

    def test_workspace_description_saves_revision_without_server_error(self):
        from .models import Revision

        self.client.force_login(self.owner)
        response = self.client.post(
            f"/studio/workspaces/{self.workspace.id}/edit/",
            {"name": self.workspace.name, "description": "A fuller production workspace description."},
        )
        self.assertRedirects(response, f"/studio/workspaces/{self.workspace.id}/")
        self.workspace.refresh_from_db()
        self.assertEqual(self.workspace.description, "A fuller production workspace description.")
        self.assertTrue(Revision.objects.filter(entity_type="lexamora_studio.workspace", entity_id=self.workspace.id).exists())

    def test_owner_is_visible_and_cannot_be_excluded_from_project(self):
        self.client.force_login(self.owner)
        page = self.client.get(f"/studio/projects/{self.project.id}/access/")
        self.assertContains(page, "Workspace owner / full access")
        owner_membership = WorkspaceMembership.objects.get(workspace=self.workspace, user=self.owner)
        denied = self.client.post(
            f"/studio/projects/{self.project.id}/access/",
            {"action": "exclude", "user_id": str(owner_membership.user_id)},
        )
        self.assertEqual(denied.status_code, 403)
        self.assertTrue(accessible_projects(self.owner).filter(id=self.project.id).exists())

    def test_only_workspace_owner_or_admin_can_copy_or_archive(self):
        users = get_user_model()
        editor = users.objects.create_user("archive-editor", email="archive-editor@example.com", password="strong-pass")
        WorkspaceMembership.objects.create(workspace=self.workspace, user=editor, role=WorkspaceMembership.Role.EDITOR)
        self.client.force_login(editor)
        self.assertEqual(self.client.post(f"/studio/workspaces/{self.workspace.id}/copy/").status_code, 403)
        self.assertEqual(self.client.post(f"/studio/workspaces/{self.workspace.id}/trash/").status_code, 403)
        self.assertEqual(self.client.post(f"/studio/projects/{self.project.id}/copy/").status_code, 403)
        self.assertEqual(self.client.post(f"/studio/projects/{self.project.id}/trash/").status_code, 403)

    def test_archive_has_no_auto_expiry_and_purge_requires_30_seconds(self):
        from django.core import signing
        from django.utils import timezone
        from .models import Project

        self.client.force_login(self.owner)
        archived = self.client.post(f"/studio/projects/{self.project.id}/trash/")
        self.assertRedirects(archived, f"/studio/workspaces/{self.workspace.id}/")
        Project.all_objects.filter(id=self.project.id).update(deleted_at=timezone.now() - timedelta(days=90))
        archive_page = self.client.get(f"/studio/workspaces/{self.workspace.id}/projects/trash/")
        self.assertContains(archive_page, "Project archive")
        self.assertContains(archive_page, "data-purge-delay=\"30\"")
        self.assertIsNone(Project.all_objects.get(id=self.project.id).purged_at)
        immediate_token = archive_page.context["projects"][0].purge_token
        self.assertEqual(self.client.post(f"/studio/projects/{self.project.id}/purge/", {"purge_token": immediate_token}).status_code, 403)
        ready_token = signing.dumps(
            {"kind": "project", "id": str(self.project.id), "user": str(self.owner.id), "issued": timezone.now().timestamp() - 31},
            salt="studio-archive-purge",
        )
        purged = self.client.post(f"/studio/projects/{self.project.id}/purge/", {"purge_token": ready_token})
        self.assertRedirects(purged, f"/studio/workspaces/{self.workspace.id}/projects/trash/")
        self.assertIsNotNone(Project.all_objects.get(id=self.project.id).purged_at)

    def test_workspace_archive_purge_uses_the_same_delay(self):
        from django.core import signing
        from django.utils import timezone
        from .models import Workspace

        self.client.force_login(self.owner)
        archived = self.client.post(f"/studio/workspaces/{self.workspace.id}/trash/")
        self.assertRedirects(archived, "/studio/")
        archive_page = self.client.get("/studio/")
        self.assertContains(archive_page, "Workspace archive")
        immediate_token = archive_page.context["archived_workspaces"][0].purge_token
        self.assertEqual(self.client.post(f"/studio/workspaces/{self.workspace.id}/purge/", {"purge_token": immediate_token}).status_code, 403)
        ready_token = signing.dumps(
            {"kind": "workspace", "id": str(self.workspace.id), "user": str(self.owner.id), "issued": timezone.now().timestamp() - 31},
            salt="studio-archive-purge",
        )
        purged = self.client.post(f"/studio/workspaces/{self.workspace.id}/purge/", {"purge_token": ready_token})
        self.assertRedirects(purged, "/studio/")
        self.assertIsNotNone(Workspace.all_objects.get(id=self.workspace.id).purged_at)

    def test_new_workspace_and_project_shares_send_email(self):
        from django.core import mail

        users = get_user_model()
        workspace_guest = users.objects.create_user("workspace-guest", email="workspace-guest@example.com", password="strong-pass")
        project_guest = users.objects.create_user("project-guest", email="project-guest@example.com", password="strong-pass")
        self.client.force_login(self.owner)
        with self.settings(EMAIL_BACKEND="django.core.mail.backends.locmem.EmailBackend"):
            with self.captureOnCommitCallbacks(execute=True):
                workspace_response = self.client.post(
                    f"/studio/workspaces/{self.workspace.id}/access/",
                    {"email": workspace_guest.email, "role": WorkspaceMembership.Role.VIEWER, "can_use_ai": "on"},
                )
            with self.captureOnCommitCallbacks(execute=True):
                project_response = self.client.post(
                    f"/studio/projects/{self.project.id}/access/",
                    {"email": project_guest.email, "role": "VIEWER"},
                )
        self.assertRedirects(workspace_response, f"/studio/workspaces/{self.workspace.id}/access/")
        self.assertRedirects(project_response, f"/studio/projects/{self.project.id}/access/")
        self.assertEqual([message.to for message in mail.outbox[-2:]], [[workspace_guest.email], [project_guest.email]])

    def test_translations_show_prompt_versions_as_separate_cards_and_filters(self):
        translated = Prompt.objects.create(
            scene=self.first_scene, ai_model=self.ai_model, source_prompt=None,
            prompt_type="IMAGE", title="Original prompt", language="EN", original_language="EN",
            content="Original", created_by=self.owner, updated_by=self.owner,
        )
        Prompt.objects.create(
            scene=self.first_scene, ai_model=self.ai_model, source_prompt=translated,
            prompt_type="IMAGE", title="Polish prompt", language="PL", original_language="EN",
            content="Polski tekst", position=1, created_by=self.owner, updated_by=self.owner,
        )
        self.client.force_login(self.owner)
        page = self.client.get(f"/studio/projects/{self.project.id}/translations/?scene={self.first_scene.id}&prompt_language=PL&sort=desc")
        self.assertEqual(page.status_code, 200)
        self.assertContains(page, "Original prompt")
        self.assertContains(page, "Polski tekst")
        self.assertContains(page, "Saved translation")

    def test_scene_reorder_recomputes_display_numbers(self):
        from .models import Scene

        self.client.force_login(self.owner)
        response = self.client.post(
            f"/studio/episodes/{self.episode.id}/scenes/reorder/",
            data=json.dumps({"sceneIds": [str(self.second_scene.id), str(self.first_scene.id)]}),
            content_type="application/json",
        )
        self.assertEqual(response.status_code, 200)
        ordered = list(Scene.objects.filter(episode=self.episode).order_by("position"))
        self.assertEqual([(item.id, item.number) for item in ordered], [(self.second_scene.id, 1), (self.first_scene.id, 2)])

    def test_crop_creates_second_project_image_and_detach_keeps_file(self):
        import tempfile
        from pathlib import Path
        from .models import Asset
        from .storage import create_asset

        self.client.force_login(self.owner)
        with tempfile.TemporaryDirectory() as directory:
            with self.settings(STUDIO_PRIVATE_MEDIA_ROOT=Path(directory)):
                source = create_asset(
                    user=self.owner, workspace=self.workspace, project=self.project,
                    scene=self.first_scene, uploaded=self.image_file("source.png"), kind=Asset.Kind.SCENE_IMAGE,
                )
                cropped = self.client.post(
                    f"/studio/assets/{source.id}/crop/",
                    data=json.dumps({"x": 4, "y": 3, "width": 40, "height": 30}),
                    content_type="application/json",
                )
                self.assertEqual(cropped.status_code, 201, cropped.content)
                crop = Asset.objects.get(id=cropped.json()["id"])
                self.assertEqual((crop.width, crop.height), (40, 30))
                self.assertEqual(crop.project, self.project)
                self.assertIsNone(crop.scene)
                self.assertTrue(Asset.objects.filter(id=source.id).exists())

                detached = self.client.post(
                    f"/studio/assets/{source.id}/detach/scene/{self.first_scene.id}/",
                    {"next": f"/studio/scenes/{self.first_scene.id}/#images"},
                )
                self.assertRedirects(detached, f"/studio/scenes/{self.first_scene.id}/#images")
                source.refresh_from_db()
                self.assertIsNone(source.scene)
                self.assertTrue(Asset.objects.filter(id=source.id, deleted_at__isnull=True).exists())

    def test_workspace_media_library_uploads_multiple_images(self):
        import tempfile
        from pathlib import Path
        from .models import Asset

        self.client.force_login(self.owner)
        with tempfile.TemporaryDirectory() as directory:
            with self.settings(STUDIO_PRIVATE_MEDIA_ROOT=Path(directory)):
                response = self.client.post(
                    f"/studio/workspaces/{self.workspace.id}/images/new/",
                    {
                        "file": [self.image_file("workspace-a.png"), self.image_file("workspace-b.png")],
                        "next": f"/studio/workspaces/{self.workspace.id}/#workspace-images",
                    },
                )
                self.assertRedirects(response, f"/studio/workspaces/{self.workspace.id}/#workspace-images")
                self.assertEqual(Asset.objects.filter(workspace=self.workspace, project__isnull=True).count(), 2)
                page = self.client.get(f"/studio/workspaces/{self.workspace.id}/")
                self.assertContains(page, "Workspace Images")
                self.assertContains(page, "workspace-a.png")
                self.assertContains(page, "data-media-library")

    def test_workspace_image_is_available_in_project_picker_and_can_be_cropped(self):
        import tempfile
        from pathlib import Path
        from .models import Asset
        from .storage import create_asset

        self.client.force_login(self.owner)
        with tempfile.TemporaryDirectory() as directory:
            with self.settings(STUDIO_PRIVATE_MEDIA_ROOT=Path(directory)):
                source = create_asset(
                    user=self.owner,
                    workspace=self.workspace,
                    uploaded=self.image_file("workspace-reference.png"),
                    kind=Asset.Kind.OTHER,
                )
                page = self.client.get(f"/studio/projects/{self.project.id}/")
                self.assertContains(page, "workspace-reference.png")
                self.assertContains(page, "Workspace / 72x54")
                cropped = self.client.post(
                    f"/studio/assets/{source.id}/crop/",
                    data=json.dumps({"x": 4, "y": 3, "width": 40, "height": 30}),
                    content_type="application/json",
                )
                self.assertEqual(cropped.status_code, 201, cropped.content)
                result = Asset.objects.get(id=cropped.json()["id"])
                self.assertIsNone(result.project)
                self.assertEqual((result.width, result.height), (40, 30))

    def test_workspace_gallery_deduplicates_identical_files_and_keeps_project_links(self):
        import tempfile
        from pathlib import Path
        from .models import Asset, Project
        from .storage import create_asset

        second_project = Project.objects.create(
            workspace=self.workspace,
            project_type=Project.Type.SERIES,
            title="Second image project",
            created_by=self.owner,
            updated_by=self.owner,
        )
        self.client.force_login(self.owner)
        with tempfile.TemporaryDirectory() as directory:
            with self.settings(STUDIO_PRIVATE_MEDIA_ROOT=Path(directory)):
                original = create_asset(
                    user=self.owner,
                    workspace=self.workspace,
                    project=self.project,
                    uploaded=self.image_file("original.png"),
                    kind=Asset.Kind.OTHER,
                )
                duplicate = create_asset(
                    user=self.owner,
                    workspace=self.workspace,
                    project=second_project,
                    uploaded=self.image_file("duplicate.png"),
                    kind=Asset.Kind.OTHER,
                )
                self.assertEqual(original.checksum_sha256, duplicate.checksum_sha256)

                response = self.client.get(f"/studio/workspaces/{self.workspace.id}/")
                gallery_assets = response.context["gallery_assets"]
                self.assertEqual(len(gallery_assets), 1)
                self.assertEqual(gallery_assets[0].id, original.id)
                self.assertEqual(
                    set(gallery_assets[0].gallery_project_ids.split(",")),
                    {str(self.project.id), str(second_project.id)},
                )

    def test_workspace_gallery_query_count_does_not_grow_per_image(self):
        import io
        import tempfile
        from pathlib import Path
        from PIL import Image
        from django.core.files.uploadedfile import SimpleUploadedFile
        from .models import Asset
        from .storage import create_asset

        self.client.force_login(self.owner)
        with tempfile.TemporaryDirectory() as directory:
            with self.settings(STUDIO_PRIVATE_MEDIA_ROOT=Path(directory)):
                first_output = io.BytesIO()
                Image.new("RGB", (72, 54), (0, 100, 180)).save(first_output, "PNG")
                create_asset(
                    user=self.owner,
                    workspace=self.workspace,
                    project=self.project,
                    uploaded=SimpleUploadedFile(
                        "performance-0.png",
                        first_output.getvalue(),
                        content_type="image/png",
                    ),
                    kind=Asset.Kind.OTHER,
                )
                with CaptureQueriesContext(connection) as baseline_queries:
                    baseline_response = self.client.get(f"/studio/workspaces/{self.workspace.id}/")
                self.assertEqual(baseline_response.status_code, 200)

                for index in range(1, 8):
                    output = io.BytesIO()
                    Image.new("RGB", (72, 54), (index * 20, 100, 180)).save(output, "PNG")
                    create_asset(
                        user=self.owner,
                        workspace=self.workspace,
                        project=self.project,
                        uploaded=SimpleUploadedFile(
                            f"performance-{index}.png",
                            output.getvalue(),
                            content_type="image/png",
                        ),
                        kind=Asset.Kind.OTHER,
                    )

                with CaptureQueriesContext(connection) as populated_queries:
                    populated_response = self.client.get(f"/studio/workspaces/{self.workspace.id}/")
                self.assertEqual(populated_response.status_code, 200)
                self.assertLessEqual(len(populated_queries), len(baseline_queries) + 1)

    def test_project_image_detaches_but_only_workspace_gallery_offers_trash(self):
        import tempfile
        from pathlib import Path
        from .models import Asset
        from .storage import create_asset

        self.client.force_login(self.owner)
        with tempfile.TemporaryDirectory() as directory:
            with self.settings(STUDIO_PRIVATE_MEDIA_ROOT=Path(directory)):
                asset = create_asset(
                    user=self.owner,
                    workspace=self.workspace,
                    project=self.project,
                    scene=self.first_scene,
                    uploaded=self.image_file("detachable.png"),
                    kind=Asset.Kind.SCENE_IMAGE,
                )
                project_page = self.client.get(f"/studio/projects/{self.project.id}/")
                self.assertContains(project_page, f"/studio/assets/{asset.id}/detach/project/{self.project.id}/")
                self.assertNotContains(project_page, f"/studio/assets/{asset.id}/trash/")
                workspace_page = self.client.get(f"/studio/workspaces/{self.workspace.id}/")
                self.assertContains(workspace_page, f"/studio/assets/{asset.id}/trash/")

                detached = self.client.post(
                    f"/studio/assets/{asset.id}/detach/project/{self.project.id}/",
                    {"next": f"/studio/projects/{self.project.id}/#images"},
                )
                self.assertRedirects(detached, f"/studio/projects/{self.project.id}/#images")
                asset.refresh_from_db()
                self.assertIsNone(asset.project)
                self.assertIsNone(asset.scene)
                self.assertIsNone(asset.deleted_at)

    def test_shared_workspace_image_is_not_duplicated_and_requires_complete_access_to_delete(self):
        import tempfile
        from pathlib import Path
        from .models import Asset
        from .storage import create_asset

        editor = get_user_model().objects.create_user("shared-image-editor", password="strong-pass")
        WorkspaceMembership.objects.create(
            workspace=self.workspace,
            user=editor,
            role=WorkspaceMembership.Role.EDITOR,
        )
        hidden_project = Project.objects.create(
            workspace=self.workspace,
            project_type=Project.Type.SERIES,
            title="Private linked project",
            created_by=self.owner,
            updated_by=self.owner,
        )
        ProjectAccessExclusion.objects.create(
            project=hidden_project,
            user=editor,
            revoked_by=self.owner,
        )

        with tempfile.TemporaryDirectory() as directory:
            with self.settings(STUDIO_PRIVATE_MEDIA_ROOT=Path(directory)):
                asset = create_asset(
                    user=self.owner,
                    workspace=self.workspace,
                    project=self.project,
                    uploaded=self.image_file("shared-once.png"),
                    kind=Asset.Kind.OTHER,
                )
                asset.projects.add(hidden_project)
                self.assertEqual(Asset.objects.count(), 1)

                self.client.force_login(editor)
                page = self.client.get(f"/studio/workspaces/{self.workspace.id}/")
                self.assertEqual(page.status_code, 200)
                self.assertNotContains(page, hidden_project.title)
                denied = self.client.post(
                    f"/studio/assets/{asset.id}/trash/",
                    {"workspace_delete": "1", "confirm_usage": "1"},
                )
                self.assertEqual(denied.status_code, 403)
                asset.refresh_from_db()
                self.assertIsNone(asset.deleted_at)

                self.client.force_login(self.owner)
                needs_confirmation = self.client.post(
                    f"/studio/assets/{asset.id}/trash/",
                    {"workspace_delete": "1"},
                )
                self.assertEqual(needs_confirmation.status_code, 302)
                asset.refresh_from_db()
                self.assertIsNone(asset.deleted_at)
                confirmed = self.client.post(
                    f"/studio/assets/{asset.id}/trash/",
                    {"workspace_delete": "1", "confirm_usage": "1"},
                )
                self.assertEqual(confirmed.status_code, 302)
                asset.refresh_from_db()
                self.assertIsNotNone(asset.deleted_at)

    def test_project_cover_can_be_selected_from_workspace(self):
        import tempfile
        from pathlib import Path
        from .models import Asset
        from .storage import create_asset

        self.client.force_login(self.owner)
        with tempfile.TemporaryDirectory() as directory:
            with self.settings(STUDIO_PRIVATE_MEDIA_ROOT=Path(directory)):
                asset = create_asset(
                    user=self.owner,
                    workspace=self.workspace,
                    project=self.project,
                    uploaded=self.image_file("project-cover.png"),
                    kind=Asset.Kind.OTHER,
                )
                page = self.client.get(f"/studio/workspaces/{self.workspace.id}/")
                self.assertContains(page, f'id="{self.project.id}-cover-picker"')
                self.assertContains(page, "Project Cover")
                self.assertContains(page, "data-preserve-position")

                selected = self.client.post(
                    f"/studio/assets/attach/project_cover/{self.project.id}/",
                    {
                        "asset_id": str(asset.id),
                        "next": f"/studio/workspaces/{self.workspace.id}/#project-{self.project.id}",
                    },
                )
                self.assertRedirects(
                    selected,
                    f"/studio/workspaces/{self.workspace.id}/#project-{self.project.id}",
                )
                self.project.refresh_from_db()
                self.assertEqual(self.project.cover_asset_id, asset.id)

                self.client.force_login(self.viewer)
                forbidden = self.client.post(
                    f"/studio/assets/attach/project_cover/{self.project.id}/",
                    {"asset_id": str(asset.id)},
                )
                self.assertEqual(forbidden.status_code, 403)

    def test_workspace_and_character_avatars_use_shared_image_picker(self):
        import tempfile
        from pathlib import Path
        from .models import Asset, Character
        from .storage import create_asset

        character = Character.objects.create(
            project=self.project,
            name="Murr",
            description="Studio character",
            created_by=self.owner,
            updated_by=self.owner,
        )
        self.client.force_login(self.owner)
        with tempfile.TemporaryDirectory() as directory:
            with self.settings(STUDIO_PRIVATE_MEDIA_ROOT=Path(directory)):
                workspace_asset = create_asset(
                    user=self.owner,
                    workspace=self.workspace,
                    project=None,
                    uploaded=self.image_file("workspace-avatar.png"),
                    kind=Asset.Kind.OTHER,
                )
                character_asset = create_asset(
                    user=self.owner,
                    workspace=self.workspace,
                    project=self.project,
                    uploaded=self.image_file("character-avatar.png"),
                    kind=Asset.Kind.CHARACTER_REFERENCE,
                )

                workspace_page = self.client.get(f"/studio/workspaces/{self.workspace.id}/")
                self.assertContains(workspace_page, 'id="workspace-avatar-picker"')
                self.assertContains(workspace_page, "Workspace Avatar")

                project_page = self.client.get(f"/studio/projects/{self.project.id}/")
                self.assertContains(project_page, f'id="{character.id}-avatar-picker"')
                self.assertContains(project_page, "Character Avatar")
                self.assertNotContains(project_page, "image-modal-footer")

                workspace_selected = self.client.post(
                    f"/studio/assets/attach/workspace_avatar/{self.workspace.id}/",
                    {
                        "asset_id": str(workspace_asset.id),
                        "next": f"/studio/workspaces/{self.workspace.id}/",
                    },
                )
                self.assertRedirects(workspace_selected, f"/studio/workspaces/{self.workspace.id}/")
                self.workspace.refresh_from_db()
                self.assertEqual(self.workspace.avatar_asset_id, workspace_asset.id)

                character_selected = self.client.post(
                    f"/studio/assets/attach/character_avatar/{character.id}/",
                    {
                        "asset_id": str(character_asset.id),
                        "next": f"/studio/projects/{self.project.id}/#characters",
                    },
                )
                self.assertRedirects(
                    character_selected,
                    f"/studio/projects/{self.project.id}/#characters",
                )
                character.refresh_from_db()
                character_asset.refresh_from_db()
                self.assertEqual(character.avatar_asset_id, character_asset.id)
                self.assertTrue(character.reference_assets.filter(id=character_asset.id).exists())
                self.assertTrue(character_asset.projects.filter(id=self.project.id).exists())

    def test_project_header_is_shared_by_all_four_views(self):
        self.client.force_login(self.owner)
        urls = (
            f"/studio/projects/{self.project.id}/",
            f"/studio/projects/{self.project.id}/scene-chain/",
            f"/studio/projects/{self.project.id}/master/",
            f"/studio/projects/{self.project.id}/translations/",
        )
        for url in urls:
            with self.subTest(url=url):
                page = self.client.get(url)
                self.assertEqual(page.status_code, 200)
                self.assertContains(page, f"Project {self.project.title}")
                self.assertContains(page, "General View")
                self.assertContains(page, "Scene Chain")
                self.assertContains(page, "Master Document")
                self.assertContains(page, "Translations")

    def test_workspace_tiles_copy_trash_and_restore(self):
        self.client.force_login(self.owner)
        dashboard = self.client.get("/studio/")
        self.assertContains(dashboard, f"/studio/workspaces/{self.workspace.id}/copy/")
        self.assertContains(dashboard, f"/studio/workspaces/{self.workspace.id}/trash/")
        copied_response = self.client.post(f"/studio/workspaces/{self.workspace.id}/copy/")
        copied = accessible_workspaces(self.owner).exclude(id=self.workspace.id).get()
        self.assertRedirects(copied_response, f"/studio/workspaces/{copied.id}/")
        trashed = self.client.post(f"/studio/workspaces/{copied.id}/trash/")
        self.assertRedirects(trashed, "/studio/")
        self.assertFalse(accessible_workspaces(self.owner).filter(id=copied.id).exists())
        restored = self.client.post(f"/studio/workspaces/{copied.id}/restore/")
        self.assertRedirects(restored, f"/studio/workspaces/{copied.id}/")
        self.assertTrue(accessible_workspaces(self.owner).filter(id=copied.id).exists())

    def test_prompt_can_attach_existing_project_image(self):
        import tempfile
        from pathlib import Path
        from .models import Asset, Prompt
        from .storage import create_asset

        prompt = Prompt.objects.create(scene=self.first_scene, ai_model=self.ai_model, prompt_type="IMAGE", title="References", created_by=self.owner, updated_by=self.owner)
        self.client.force_login(self.owner)
        with tempfile.TemporaryDirectory() as directory:
            with self.settings(STUDIO_PRIVATE_MEDIA_ROOT=Path(directory)):
                asset = create_asset(user=self.owner, workspace=self.workspace, project=self.project, uploaded=self.image_file("library.png"), kind=Asset.Kind.OTHER)
                response = self.client.post(f"/studio/prompts/{prompt.id}/images/attach/", {"asset_id": str(asset.id)})
                self.assertRedirects(response, f"/studio/scenes/{self.first_scene.id}/#prompt-{prompt.id}")
                self.assertTrue(prompt.reference_assets.filter(id=asset.id).exists())

    def test_project_copy_and_soft_delete_restore(self):
        import tempfile
        from pathlib import Path
        from .models import Asset
        from .storage import create_asset

        self.client.force_login(self.owner)
        with tempfile.TemporaryDirectory() as directory:
            with self.settings(STUDIO_PRIVATE_MEDIA_ROOT=Path(directory)):
                source_asset = create_asset(user=self.owner, workspace=self.workspace, project=self.project, uploaded=self.image_file("cover.png"), kind=Asset.Kind.OTHER)
                copied_response = self.client.post(f"/studio/projects/{self.project.id}/copy/")
                copied = Project.objects.exclude(id=self.project.id).get()
                self.assertRedirects(copied_response, f"/studio/projects/{copied.id}/")
                self.assertEqual(copied.episodes.count(), 1)
                self.assertEqual(copied.episodes.get().scenes.count(), 2)
                self.assertTrue(copied.media_assets.filter(id=source_asset.id).exists())
                self.assertEqual(Asset.objects.count(), 1)
                changed_cover = self.client.post(
                    f"/studio/assets/attach/project_cover/{copied.id}/",
                    {
                        "asset_id": str(source_asset.id),
                        "next": f"/studio/workspaces/{self.workspace.id}/#project-{copied.id}",
                    },
                )
                self.assertRedirects(
                    changed_cover,
                    f"/studio/workspaces/{self.workspace.id}/#project-{copied.id}",
                )
                copied.refresh_from_db()
                self.assertEqual(copied.cover_asset_id, source_asset.id)
                deleted = self.client.post(f"/studio/projects/{copied.id}/trash/")
                self.assertRedirects(deleted, f"/studio/workspaces/{self.workspace.id}/")
                self.assertFalse(Project.objects.filter(id=copied.id).exists())
                restored = self.client.post(f"/studio/projects/{copied.id}/restore/")
                self.assertRedirects(restored, f"/studio/projects/{copied.id}/")
                self.assertTrue(Project.objects.filter(id=copied.id).exists())

    def test_viewer_sees_inline_prompts_but_cannot_change_them(self):
        from .models import Prompt

        prompt = Prompt.objects.create(
            scene=self.first_scene, ai_model=self.ai_model, prompt_type="IMAGE",
            title="Read only", created_by=self.owner, updated_by=self.owner,
        )
        self.client.force_login(self.viewer)
        page = self.client.get(f"/studio/scenes/{self.first_scene.id}/")
        self.assertEqual(page.status_code, 200)
        self.assertContains(page, "prompt-fieldset")
        self.assertContains(page, "disabled")
        denied = self.client.post(
            f"/studio/prompts/{prompt.id}/quick-save/",
            {"title": "Changed", "ai_model": str(self.ai_model.id), "prompt_type": "IMAGE", "status": "DRAFT"},
        )
        self.assertEqual(denied.status_code, 403)
        reorder = self.client.post(
            f"/studio/episodes/{self.episode.id}/scenes/reorder/",
            data=json.dumps({"sceneIds": [str(self.second_scene.id), str(self.first_scene.id)]}),
            content_type="application/json",
        )
        self.assertEqual(reorder.status_code, 403)


class StudioAssetLifecycleTests(TestCase):
    def setUp(self):
        from .models import Project

        users = get_user_model()
        self.owner = users.objects.create_user("asset-owner", password="strong-pass")
        self.viewer = users.objects.create_user("asset-viewer", password="strong-pass")
        self.workspace = create_workspace(user=self.owner, name="Asset Studio", slug="asset-studio")
        WorkspaceMembership.objects.create(
            workspace=self.workspace, user=self.viewer, role=WorkspaceMembership.Role.VIEWER
        )
        self.project = Project.objects.create(
            workspace=self.workspace, project_type=Project.Type.SERIES, title="Asset Project",
            created_by=self.owner, updated_by=self.owner,
        )

    @staticmethod
    def image_file(name="lifecycle.png"):
        import io
        from PIL import Image
        from django.core.files.uploadedfile import SimpleUploadedFile

        output = io.BytesIO()
        Image.new("RGB", (80, 60), "#d87a31").save(output, "PNG")
        return SimpleUploadedFile(name, output.getvalue(), content_type="image/png")

    def test_image_can_be_trashed_restored_and_permanently_purged(self):
        import tempfile
        from pathlib import Path
        from .models import Asset, AuditEvent
        from .storage import create_asset

        self.client.force_login(self.owner)
        with tempfile.TemporaryDirectory() as directory:
            with self.settings(STUDIO_PRIVATE_MEDIA_ROOT=Path(directory)):
                asset = create_asset(
                    user=self.owner, workspace=self.workspace, project=self.project,
                    uploaded=self.image_file(), kind=Asset.Kind.OTHER,
                )
                original_path = Path(asset.file.path)
                thumbnail_path = Path(asset.thumbnail.path)
                trashed = self.client.post(
                    f"/studio/assets/{asset.id}/trash/",
                    {
                        "next": f"/studio/projects/{self.project.id}/images/trash/",
                        "workspace_delete": "1",
                        "confirm_usage": "1",
                    },
                )
                self.assertEqual(trashed.status_code, 302)
                self.assertFalse(Asset.objects.filter(id=asset.id).exists())
                self.assertEqual(self.client.get(f"/api/v1/studio/assets/{asset.id}/view").status_code, 404)
                trash_page = self.client.get(f"/studio/workspaces/{self.workspace.id}/recycle-bin/")
                self.assertContains(trash_page, asset.original_filename)
                trash_preview = self.client.get(f"/studio/assets/{asset.id}/trash-thumbnail/")
                self.assertEqual(trash_preview.status_code, 200)
                trash_preview.close()

                restored = self.client.post(f"/studio/assets/{asset.id}/restore/")
                self.assertEqual(restored.status_code, 302)
                self.assertTrue(Asset.objects.filter(id=asset.id).exists())

                with self.captureOnCommitCallbacks(execute=True):
                    purged = self.client.post(f"/studio/assets/{asset.id}/purge/")
                self.assertEqual(purged.status_code, 302)
                asset = Asset.all_objects.get(id=asset.id)
                self.assertIsNotNone(asset.purged_at)
                self.assertEqual(asset.file.name, "")
                self.assertFalse(original_path.exists())
                self.assertFalse(thumbnail_path.exists())
        self.assertTrue(AuditEvent.objects.filter(action="ASSET_TRASHED").exists())
        self.assertTrue(AuditEvent.objects.filter(action="ASSET_RESTORED").exists())
        self.assertTrue(AuditEvent.objects.filter(action="ASSET_PURGED").exists())

    def test_viewer_cannot_delete_image(self):
        import tempfile
        from pathlib import Path
        from .models import Asset
        from .storage import create_asset

        with tempfile.TemporaryDirectory() as directory:
            with self.settings(STUDIO_PRIVATE_MEDIA_ROOT=Path(directory)):
                asset = create_asset(
                    user=self.owner, workspace=self.workspace, project=self.project,
                    uploaded=self.image_file(), kind=Asset.Kind.OTHER,
                )
                self.client.force_login(self.viewer)
                self.assertEqual(self.client.post(f"/studio/assets/{asset.id}/trash/").status_code, 403)
                self.assertTrue(Asset.objects.filter(id=asset.id).exists())

    def test_project_detach_keeps_workspace_file_and_attach_restores_link(self):
        import tempfile
        from pathlib import Path
        from .models import Asset
        from .storage import create_asset

        self.client.force_login(self.owner)
        with tempfile.TemporaryDirectory() as directory:
            with self.settings(STUDIO_PRIVATE_MEDIA_ROOT=Path(directory)):
                asset = create_asset(
                    user=self.owner, workspace=self.workspace, project=self.project,
                    uploaded=self.image_file("shared.png"), kind=Asset.Kind.OTHER,
                )
                detached = self.client.post(
                    f"/studio/assets/{asset.id}/detach/project/{self.project.id}/",
                    {"next": f"/studio/projects/{self.project.id}/#images"},
                )
                self.assertEqual(detached.status_code, 302)
                asset.refresh_from_db()
                self.assertTrue(Asset.objects.filter(id=asset.id, workspace=self.workspace).exists())
                self.assertFalse(asset.projects.filter(id=self.project.id).exists())
                self.assertIsNone(asset.project_id)

                attached = self.client.post(
                    f"/studio/assets/attach/project/{self.project.id}/",
                    {"asset_id": str(asset.id), "next": f"/studio/projects/{self.project.id}/#images"},
                )
                self.assertEqual(attached.status_code, 302)
                self.assertTrue(asset.projects.filter(id=self.project.id).exists())


class StudioGeneralSettingsTests(TestCase):
    def setUp(self):
        users = get_user_model()
        self.admin = users.objects.create_superuser("studio-admin", "admin@example.com", "strong-pass")
        self.user = users.objects.create_user("studio-user", password="strong-pass")

    def test_settings_are_admin_only_and_can_change_default_model(self):
        from .models import StudioTextModel

        self.client.force_login(self.user)
        self.assertEqual(self.client.get("/studio/settings/").status_code, 403)
        self.client.force_login(self.admin)
        model = StudioTextModel.objects.get(model_id="gpt-5.4")
        response = self.client.post("/studio/settings/", {
            "action": "update_model",
            "id": str(model.id),
            "model-name": "GPT 5.4 production",
            "model-model_id": "gpt-5.4",
            "model-is_active": "on",
            "model-is_default": "on",
        })
        self.assertRedirects(response, "/studio/settings/")
        model.refresh_from_db()
        self.assertTrue(model.is_default)
        self.assertFalse(StudioTextModel.objects.exclude(id=model.id).filter(is_default=True).exists())

    def test_admin_can_update_default_prompt_addition(self):
        from .models import PromptTemplate

        self.client.force_login(self.admin)
        response = self.client.post("/studio/settings/", {
            "action": "update_prompt_addition",
            "prompt_addition": "Negative Prompt: No Music",
        })
        self.assertRedirects(response, "/studio/settings/")
        template = PromptTemplate.objects.get(is_default=True)
        self.assertEqual(template.content, "Negative Prompt: No Music")
        page = self.client.get("/studio/settings/")
        self.assertContains(page, "Default prompt addition")
        self.assertNotContains(page, "Create template")

    def test_admin_can_add_or_activate_model_from_prompt_dropdown(self):
        from .models import StudioTextModel

        self.client.force_login(self.admin)
        response = self.client.post(
            "/studio/settings/text-models/quick-create/",
            {"name": "Fast prompt model", "model_id": "gpt-5.4-nano"},
            HTTP_X_REQUESTED_WITH="XMLHttpRequest",
        )
        self.assertEqual(response.status_code, 200, response.content)
        self.assertEqual(response.json()["modelId"], "gpt-5.4-nano")
        model = StudioTextModel.objects.get(model_id="gpt-5.4-nano")
        self.assertEqual(model.name, "Fast prompt model")
        self.assertTrue(model.is_active)

        self.client.force_login(self.user)
        denied = self.client.post(
            "/studio/settings/text-models/quick-create/",
            {"name": "Forbidden", "model_id": "gpt-5.4-mini"},
        )
        self.assertEqual(denied.status_code, 403)

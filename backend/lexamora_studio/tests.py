import json
from unittest.mock import patch

from cryptography.fernet import Fernet
from django.contrib.auth import get_user_model
from django.test import TestCase, override_settings

from .models import Project, WorkspaceMembership
from .permissions import accessible_workspaces, has_capability
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
            data=json.dumps({"workspaceId": str(self.workspace.id), "type": "SERIES", "title": "Dom"}),
            content_type="application/json",
        )
        self.assertEqual(response.status_code, 201)
        self.assertEqual(response.json()["title"], "Dom")

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
        self.dialogue = PromptBlock.objects.create(
            prompt=self.prompt, block_type=PromptBlock.Type.DIALOGUE_REFERENCE, content="Secret dialogue",
            position=1, created_by=self.editor, updated_by=self.editor,
        )

    def provider_response(self):
        return json.dumps({"blocks": [
            {"id": str(self.narrative.id), "content": "Cinematic room with precise lighting"},
            {"id": str(self.dialogue.id), "content": "Rewritten dialogue"},
        ]})

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
    def image_file():
        import io
        from PIL import Image
        from django.core.files.uploadedfile import SimpleUploadedFile

        buffer = io.BytesIO()
        Image.new("RGB", (48, 36), "#36a889").save(buffer, "PNG")
        return SimpleUploadedFile("reference.png", buffer.getvalue(), content_type="image/png")

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

    def test_viewer_cannot_edit_project(self):
        self.client.force_login(self.viewer)
        response = self.client.get(f"/studio/projects/{self.project.id}/edit/")
        self.assertEqual(response.status_code, 403)

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
            {"title": "Edited project", "episode_0_title": "Edited pilot", "episode_0_scene_0_title": "Edited scene"},
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
    def test_viewer_cannot_upload_docx(self):
        self.client.force_login(self.viewer)
        response = self.client.get(f"/studio/workspaces/{self.workspace.id}/imports/docx/new/")
        self.assertEqual(response.status_code, 403)

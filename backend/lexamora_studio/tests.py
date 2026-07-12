import json
from django.contrib.auth import get_user_model
from django.test import TestCase

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

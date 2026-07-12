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

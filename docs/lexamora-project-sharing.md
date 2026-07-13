# Lexamora Studio project sharing

Project owners and users with **Full control** can open **Share** on a project and grant an existing registered user one of three roles:

- **View**: read-only access to this project.
- **Edit**: view, edit, and translate this project.
- **Full control**: edit, translate, use permitted AI features, export, and manage project sharing.

AI operations still require the user's separate server AI grant. A project membership never grants access to its workspace or sibling projects. Shared projects appear under **Shared with me** on the Studio dashboard.

Authenticated API clients with Full control can use:

```text
GET  /api/v1/studio/projects/{project_id}/members
POST /api/v1/studio/projects/{project_id}/members
DELETE /api/v1/studio/projects/{project_id}/members/{membership_id}
```

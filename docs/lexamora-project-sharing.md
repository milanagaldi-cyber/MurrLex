# Lexamora Studio project sharing

Project owners and users with **Full control** can open **Share** on a project and grant an existing registered user one of three roles:

- **View**: read-only access to this project.
- **Edit**: view, edit, and translate this project.
- **Full control**: edit, translate, use permitted AI features, export, and manage project sharing.

An active project invitation automatically enables the user's server AI grant, so Lexamora Studio and its AI operations are immediately available. Removing one project membership does not automatically revoke that global grant because the user may still need it for another shared project; an administrator can revoke it separately. A project membership never grants access to its workspace or sibling projects. Shared projects appear under **Shared with me** on the Studio dashboard.

Authenticated API clients with Full control can use:

```text
GET  /api/v1/studio/projects/{project_id}/members
POST /api/v1/studio/projects/{project_id}/members
DELETE /api/v1/studio/projects/{project_id}/members/{membership_id}
```

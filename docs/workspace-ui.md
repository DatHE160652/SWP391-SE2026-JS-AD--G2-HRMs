# Workspace interface

Employee Lifecycle Data Management is the visual reference for application screens:
the white collapsible sidebar, 76px topbar, indigo accent, neutral page background,
Segoe UI typography, bordered cards, and shared controls.

- Include `common/workspace-theme.jspf` at the end of the document head, after module CSS.
- Use `body.lc` and `main.lc-main`. Keep module classes for specialized grids and editors.
- Tenant screens use `common/tenant-shell.jspf`, which selects the existing company administrator,
  manager, or employee menu from the session role. Navigation does not grant API permissions.
- Platform screens retain their own routes and CSRF protected logout within the same frame.
- Add menu destinations to the shared role shells rather than rebuilding them in page scripts.
- `workspace.css` maps older module variables to `--lc-*` tokens. Surface, accent, density,
  corners and dark mode follow the existing appearance preference.
- Preserve status colors, table scroll containers, editor IDs, API parameters and print styles.
- The homepage and its dedicated header/footer remain unchanged. Standalone payslips retain
  their printable document layout; authentication uses the shared colors and controls.

Build Java and compile JSPs after changing shared includes. Check desktop/mobile layouts,
both languages, sidebar persistence, dark mode and the affected forms before publishing.

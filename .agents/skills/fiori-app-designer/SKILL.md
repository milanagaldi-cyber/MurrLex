---
name: fiori-app-designer
description: Design and review enterprise web interfaces with open-source UI5 Web Components and SAP Fiori Horizon principles while preserving the product's own brand. Use for admin panels, tables, forms, dashboards, list reports, object details, workflows, user or content management, analytics, responsive enterprise UI, and accessibility reviews.
---

# Fiori App Designer

Build calm, information-dense enterprise interfaces with UI5 Web Components and Horizon design principles. Preserve the product identity: this is Fiori-inspired product design, not a pixel copy of SAP software.

## Workflow

1. Inspect the existing framework, component library, package manager, theme architecture, routing, and local conventions before changing code.
2. State the user role, primary business object, highest-frequency task, and information hierarchy.
3. Choose one floorplan before implementation. Read `references/floorplans.md` when the choice is not obvious.
4. Reuse an existing local component or an open-source UI5 Web Component before creating a custom equivalent.
5. Map colors, spacing, typography, elevation, states, and density to centralized tokens. Keep the product logo, naming, and distinctive brand accents.
6. Implement every relevant state: loading, empty, no results, partial data, validation, error, permission denied, success, and destructive confirmation.
7. Verify keyboard navigation, visible focus, labels, landmarks, contrast, screen-reader names, and responsive behavior.
8. Run the repository's lint, typecheck, tests, build, and visual review. Review desktop, tablet, and mobile sizes.

## Floorplan Gate

Do not start arranging controls until a floorplan is selected:

- Use **List Report** for searchable and filterable sets of business objects.
- Use **Object Page** for one business object with sections, status, actions, and related data.
- Use **Worklist** for a role's actionable queue where status and next action dominate.
- Use **Overview Page** for a role-based dashboard made from concise, actionable summaries.
- Use **Flexible Column Layout** for master-detail navigation that benefits from preserving context.
- Use **Wizard** only when a complex process genuinely requires ordered, validated steps.

If no standard floorplan fits, document why before using a custom layout.

## UI Rules

- Prefer UI5 Web Components for buttons, inputs, selects, dialogs, tables, tabs, statuses, dates, shells, and business icons.
- Use Morning Horizon as the light baseline and Evening Horizon as the dark baseline. Map the repository's Business theme to Horizon-like tokens without claiming SAP branding.
- Make semantic status explicit with text plus icon or color; never rely on color alone.
- Keep page titles compact. Favor toolbars, tables, sections, object headers, and split layouts over decorative cards.
- Support cozy and compact density where repeated operational work benefits from it.
- Keep actions close to the object they affect and place destructive actions behind confirmation.
- Use progressive disclosure for secondary metadata and infrequent controls.
- Maintain stable geometry so loading, status, and validation changes do not shift nearby controls.

## Accessibility And Responsiveness

- All actions must be reachable and operable with a keyboard.
- Preserve logical tab order and provide a clearly visible focus indicator.
- Give icon-only controls an accessible name and a concise tooltip.
- Associate labels, descriptions, validation messages, and required state with their fields.
- Use semantic headings, landmarks, tables, lists, and live regions.
- Test at desktop, tablet, and mobile widths; tables need a deliberate small-screen strategy.
- Respect reduced motion and do not use motion as the only status signal.

## Prohibited Patterns

- Do not produce a generic Tailwind admin dashboard.
- Do not overuse rounded cards, glassmorphism, gradients, neon, or giant headings.
- Do not introduce arbitrary colors when semantic or theme tokens exist.
- Do not create a custom control when an appropriate UI5 component already exists.
- Do not copy SAP screens pixel-for-pixel, use SAP logos or proprietary assets, or imply SAP endorsement.
- Do not add a competing design system without a documented technical need.

## Completion Report

Report the selected floorplan, components and tokens used, responsive and accessibility checks, commands run, visual-review coverage, and any pre-existing blockers separately from new regressions.

## Authoritative References

- UI5 Web Components: https://ui5.github.io/webcomponents/
- UI5 Web Components packages: https://ui5.github.io/webcomponents/docs/getting-started/first-steps/
- UI5 configuration, Horizon themes, density, and accessibility: https://ui5.github.io/webcomponents/docs/advanced/configuration/
- SAP Fiori web design guidance: https://experience.sap.com/fiori-design-web/

# Enterprise Floorplan Reference

Use this reference after identifying the main business object and user task.

| Floorplan | Choose it when | Essential regions |
| --- | --- | --- |
| List Report | Users search, filter, compare, and act on many records | Page title, filter bar, result count, semantic table/list, bulk and row actions |
| Object Page | Users inspect or edit one object with related sections | Object header, key status and actions, anchored sections, related objects, audit data |
| Worklist | Users process a queue of work | Queue title, compact filters, priority/status, ownership, due data, direct next action |
| Overview Page | Users monitor a role or domain | Role-based summary, concise cards, alerts, drill-down links, freshness indicator |
| Flexible Column Layout | Users repeatedly move between a list and related detail | Master list, detail column, optional related-detail column, preserved selection |
| Wizard | Steps have real dependencies and validation gates | Progress, one focused step, validation, back/next, resumable state where needed |

## Selection Questions

1. Is the user acting on one object or comparing many?
2. Must selection context remain visible while details open?
3. Is the page primarily monitoring, searching, editing, or processing a queue?
4. Are steps technically dependent, or would sections and validation be simpler than a wizard?
5. What is the smallest responsive representation that preserves the primary task?

## Review Checklist

- The primary action is obvious but not oversized.
- Filters expose active values and have clear reset behavior.
- Tables preserve headers, alignment, sorting state, and empty/no-result distinctions.
- Object statuses are semantic and understandable without color.
- Navigation preserves context and has predictable Back behavior.
- Loading, permission, error, success, and destructive states are designed.
- Keyboard, focus, screen-reader names, contrast, zoom, and reduced motion are checked.

---
name: widgetkit
description: WidgetKit implementation and review guide for Course Deals iOS Home/Lock Screen widgets, Live Activities (`ActivityConfiguration`), and watchOS Apple Watch Complications (`.accessoryCircular`, `.accessoryRectangular`, `.accessoryInline`, `.accessoryCorner`).
source: https://github.com/dpearson2699/swift-ios-skills/blob/main/skills/widgetkit/SKILL.md (Aligned to Course Deals)
---

# Course Deals WidgetKit: iOS Widgets, Live Activities & watchOS Complications

Aligned to `course_deals_ComposeMultiplatform` (`iosApp/` & `watchApp/` Widget Extensions).

## 1. Shared Data via App Groups (`group.com.thanh0x.coursedeals`)

- Widgets and watchOS complications run in separate extension processes and cannot read the host app's standard `UserDefaults.standard` or private container.
- Share lightweight cached deal snapshots (top 100%-off courses, expiring coupon timers, bookmark counts) via App Group container (`UserDefaults(suiteName: "group.com.thanh0x.coursedeals")` or shared JSON file in `FileManager.default.containerURL(forSecurityApplicationGroupIdentifier:)`).
- Trigger reloads from `iosApp`/`watchApp` only on meaningful data changes via `WidgetCenter.shared.reloadTimelines(ofKind:)` (respecting system refresh budgets).

## 2. Supported Widget & Watch Complication Families

Adapt layout using `@Environment(\.widgetFamily)`:

| Family | Target Surface | Course Deals Use Case |
|---|---|---|
| `.systemSmall` / `.systemMedium` | iOS Home Screen & StandBy | Featured 100%-off course deal card(s) with deep link (`coursedeals://coupon/<id>`) |
| `.accessoryCircular` | iOS Lock Screen & **watchOS Complication** | Circular gauge/badge showing active free deals count |
| `.accessoryRectangular` | iOS Lock Screen & **watchOS Smart Stack / Modular** | Course title + coupon expiry countdown + category |
| `.accessoryInline` | iOS Lock Screen & **watchOS Top Slot** | Single-line summary (`"🔥 12 Free Udemy Deals Today"`) |
| `.accessoryCorner` | **watchOS Corner Complication** | Curved label + icon for Apple Watch faces |

## 3. `TimelineProvider` & `AppIntentTimelineProvider` Rules

- `placeholder(in:)` must return synchronously with static sample data (no network or async calls).
- `getSnapshot(in:completion:)` must return immediately when `context.isPreview` is `true`.
- `getTimeline(in:completion:)` reads cached App Group data or performs a bounded background fetch and specifies a sensible reload policy (`.after(nextRefreshDate)`).
- Always attach `.containerBackground(..., for: .widget)` on root widget views for iOS 17+ / watchOS 10+ compatibility.
- Use `.widgetURL(URL(string: "coursedeals://coupon/\(entry.id)"))` or `Link(destination:)` for deep-linking into `CourseDealsApp`.

## Checklist
- [ ] App Group (`group.com.thanh0x.coursedeals`) used for host ↔ widget/watch data sharing
- [ ] `placeholder(in:)` is synchronous and deterministic; `context.isPreview` handled fast
- [ ] `.containerBackground(..., for: .widget)` applied to all widget/complication views
- [ ] Deep links route cleanly into `iosApp` / `watchApp`

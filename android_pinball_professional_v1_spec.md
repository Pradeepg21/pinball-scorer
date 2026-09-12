# Android Pinball Game — Professional V1 Specification

## 1. Product Vision

Create a premium-looking, highly polished Android pinball game inspired by classic arcade pinball machines.

The game must feel like a real physical pinball table rather than a simple 2D ball-bouncing game.

The experience should include:

- realistic ball physics
- responsive flippers
- bumpers
- ramps
- targets
- lanes
- multipliers
- jackpots
- combos
- missions
- particle effects
- dynamic lighting
- screen shake
- sound effects
- background music
- high-score tracking
- local achievements
- smooth 60 FPS gameplay

The V1 application should work completely offline.

No backend, server, login, Firebase, AWS, or database is required.

All scores and settings will be stored locally.

---

## 2. Target Platform

Platform:

**Android**

Recommended:

- Minimum Android: Android 8 / API 26
- Target SDK: latest Play Store requirement
- Phones primarily
- Tablets supported
- Portrait orientation
- Full-screen immersive mode
- 60 FPS target
- 90/120 FPS support where possible

Recommended language:

**Kotlin**

For the game itself, use either:

### Preferred

**LibGDX + Box2D**

or

**Android Kotlin + custom OpenGL/Canvas + Box2D physics**

For a professional pinball game, I recommend:

> **Kotlin + LibGDX + Box2D**

rather than normal Jetpack Compose views for the actual game board.

Jetpack Compose can still be used for menus/settings if desired.

---

## 3. Core Design Principle

The reference image has several important qualities that should be reproduced conceptually:

- dark arcade cabinet
- illuminated playfield
- many interactive objects
- strong neon/LED lighting
- detailed table artwork
- glowing bumpers
- metallic rails
- visible scoring zones
- large central feature
- two lower flippers
- multiple side lanes
- upper playfield activity
- animated effects after scoring
- compact score HUD at the top

However:

**Do not duplicate the exact copyrighted pinball table shown in the reference image.**

Create an original visual identity.

---

## 4. Recommended Game Theme

For V1, I recommend:

## Theme: Neon Cosmic Reactor

The table represents a futuristic energy reactor.

Central feature:

**Quantum Reactor Core**

Other components:

- Plasma Bumpers
- Energy Rails
- Reactor Ramp
- Warp Tunnel
- Power Cells
- Orbital Lanes
- Reactor Targets
- Hyperdrive Gate
- Jackpot Chamber

This type of theme allows strong:

- blue
- purple
- cyan
- orange
- red

lighting effects similar in quality to the supplied reference without copying its artwork.

---

## 5. Game Screen Layout

Portrait orientation.

Approximate screen composition:

```text
┌───────────────────────────────┐
│         TOP HUD               │
│ Lives     Score     Highscore │
├───────────────────────────────┤
│                               │
│      Upper Playfield          │
│                               │
│    ○ Bumper   ○ Bumper        │
│          ○ Bumper             │
│                               │
│      Ramps / Targets          │
│                               │
│        ENERGY CORE            │
│                               │
│ Side Lane          Side Lane  │
│                               │
│     Slingshot   Slingshot     │
│                               │
│       \         /             │
│        \       /              │
│         FLIPPERS              │
│                               │
│           ↓                   │
│          DRAIN                │
└───────────────────────────────┘
```

---

## 6. Top HUD

HUD should be compact so maximum space remains available for the table.

Show:

```text
BALL 1/3       SCORE
               125,480

MULTI x3       HIGH
               980,000
```

Optional mission indicator:

```text
REACTOR CHARGE
██████░░░░ 60%
```

HUD requirements:

- neon border
- digital/arcade typography
- subtle glow
- score animation
- score numbers roll/count upward
- multiplier briefly enlarges when increased
- jackpot animation

---

## 7. Starting Game

Home screen:

```text
GAME LOGO

        PLAY

     HIGH SCORES

      HOW TO PLAY

       SETTINGS
```

Background should contain:

- animated pinball table
- subtle floating particles
- light pulses
- slowly moving camera/light effects

Buttons should have:

- glowing edge
- press animation
- tactile sound
- haptic feedback

---

## 8. Ball Launch

Player begins with:

**3 balls**

The ball initially sits in the launch lane.

Player can:

### Option A

Swipe downward and release.

Launch force depends on swipe strength.

OR

### Recommended for V1

Hold a large spring/plunger control.

The longer the user holds, the stronger the launch.

Example:

```text
POWER

████████░░

RELEASE TO LAUNCH
```

The plunger should animate physically.

---

## 9. Flipper Controls

Two main flippers.

Control:

Left half of lower screen:

**Left flipper**

Right half:

**Right flipper**

Do not require visible buttons during gameplay.

Invisible touch zones should be used.

Benefits:

- larger playing area
- natural pinball control
- easier rapid tapping

Support simultaneous touch.

Example:

```text
LEFT FINGER              RIGHT FINGER
     ↓                         ↓

   LEFT FLIPPER           RIGHT FLIPPER
```

Touch response must feel immediate.

Desired input latency:

**<30 ms where possible.**

---

## 10. Flipper Physics

Flippers must not simply rotate visually.

They must have:

- rotational velocity
- angular limits
- collision physics
- ball impulse
- return spring
- configurable strength

Suggested angles:

Rest:

~20–30°

Activated:

~60–70°

Flipper activation time:

approximately:

**70–100 ms**

Return:

**100–150 ms**

Tune after device testing.

---

## 11. Ball Physics

Physics quality is one of the most important requirements.

The ball must feel heavy and metallic.

Implement:

- gravity
- friction
- restitution
- angular velocity
- collision impulse
- rolling
- bouncing
- surface interaction

Ball properties:

```text
Shape: Circle
Body: Dynamic
Density: Tunable
Restitution: Medium
Friction: Low
Continuous Collision Detection: Enabled
```

CCD is important so that a fast-moving ball does not pass through walls.

---

## 12. Table Components

The V1 table should contain approximately:

### Flippers

2 main lower flippers.

Optional:

1 upper mini-flipper.

### Pop Bumpers

3.

Triangle arrangement.

Example:

```text
   O

O     O
```

Each hit gives:

**1,000 points**

Repeated hits increase temporary combo bonus.

---

## 13. Slingshots

Place immediately above the lower flippers.

```text
  /▲\              /▲\
 /   \            /   \
     \ FLIPPERS /
```

When ball touches:

- detect collision
- push ball away
- flash light
- play mechanical snap
- add points

Score:

**500 points**

---

## 14. Targets

Include approximately:

**8–12 stand-up targets**

Example:

```text
R E A C T O R
```

Hitting all letters activates:

**REACTOR MODE**

Each target:

500 points.

Complete group:

10,000 bonus.

---

## 15. Drop Targets

Optional but recommended.

Group:

```text
[1][2][3]
```

Each target physically lowers after hit.

When all three are down:

- reset them
- give bonus
- activate multiplier

Animation should show targets physically moving.

---

## 16. Ramps

At least:

**2 ramps**

### Left Ramp

Reactor Ramp

Rewards:

5,000 points.

Repeated ramp hit:

```text
1st     5,000
2nd     10,000
3rd     20,000
4th     40,000
```

### Right Ramp

Hyperdrive Ramp.

Three consecutive hits start:

**HYPERDRIVE MODE**

---

## 17. Orbit Lanes

Provide two long paths:

- left orbit
- right orbit

Ball travels around upper section and returns.

Rewards:

```text
Orbit Hit     3,000
Combo Orbit   7,500
Triple Combo  20,000
```

---

## 18. Central Reactor

The most visually impressive feature should sit near the center.

Example:

```text
      ╭─────────╮
     │  REACTOR │
     │    ◉     │
      ╰─────────╯
```

It should contain:

- circular animated energy
- rotating rings
- pulsing glow
- particle effects
- light intensity based on charge level

Reactor charge:

```text
0%
25%
50%
75%
100%
```

At 100%:

**REACTOR OVERLOAD MODE**

starts.

---

## 19. Reactor Overload

Duration:

**20 seconds**

During this mode:

- table lighting changes
- score multiplier increases
- music intensifies
- reactor pulses
- particle effects increase
- selected targets blink

Multiplier:

**x3**

At end:

large effect:

```text
REACTOR STABILIZED

BONUS +100,000
```

---

## 20. Multiball

Professional pinball should contain multiball.

Trigger:

Complete three Reactor Locks.

Example:

```text
LOCK 1
LOCK 2
LOCK 3
```

Then:

# MULTIBALL

Release:

**3 balls**

simultaneously.

During multiball:

- brighter table
- energetic music
- jackpot targets blink

---

## 21. Jackpot

During multiball:

selected ramp becomes:

```text
JACKPOT
```

Hit:

**100,000**

Then activate:

```text
SUPER JACKPOT
```

Worth:

**250,000**

---

## 22. Scoring System

Suggested scoring:

| Action | Points |
|---|---:|
| Normal target | 500 |
| Slingshot | 500 |
| Pop bumper | 1,000 |
| Lane | 1,500 |
| Orbit | 3,000 |
| Ramp | 5,000 |
| Target bank completion | 10,000 |
| Combo | 10K–50K |
| Mission completion | 50,000 |
| Jackpot | 100,000 |
| Super Jackpot | 250,000 |
| Reactor Overload | 100,000 |

Scores should feel large and satisfying.

---

## 23. Combo System

If the player hits special elements within a short time:

```text
RAMP
↓
ORBIT
↓
TARGET
```

trigger:

```text
3X COMBO
+25,000
```

Combo timeout:

approximately:

**3 seconds**

Examples:

```text
DOUBLE COMBO
TRIPLE COMBO
MEGA COMBO
ULTRA COMBO
```

---

## 24. Ball Save

Immediately after launch:

Ball Save:

**10 seconds**

HUD shows:

```text
BALL SAVE
```

If ball drains during this period:

automatically return another ball.

---

## 25. Extra Ball

Extra balls can be earned.

Examples:

Complete:

```text
E X T R A
```

or reach:

**1,000,000 score**

Reward:

```text
EXTRA BALL
```

---

## 26. Tilt Mechanic

Optional V1 feature.

Allow device movement using accelerometer.

Small shake:

nudges table.

Repeated strong shake:

```text
WARNING

WARNING

TILT
```

Tilt disables flippers until ball drains.

This adds realism.

Make it optional in Settings.

---

## 27. Visual Quality

The game should NOT look like a flat mobile UI.

Target appearance:

### Table

- highly detailed
- metallic rails
- painted table surface
- illuminated inserts
- glowing targets
- 3D-like depth
- shadows
- reflective ball
- lighting overlays

Use layered rendering.

Example:

```text
Background artwork
↓
Table base
↓
Static objects
↓
Dynamic objects
↓
Ball shadow
↓
Ball
↓
Lighting
↓
Particles
↓
Glow/Bloom
↓
HUD
```

---

## 28. Ball Graphics

Ball should appear metallic.

Use:

- radial highlight
- reflection
- subtle environment reflection
- shadow underneath
- optional motion trail at high velocity

When very fast:

small blue/white trail.

---

## 29. Lighting System

Important for premium appearance.

Interactive objects must react to gameplay.

Example:

When bumper hits:

```text
normal

   ↓

bright flash

   ↓

glow

   ↓

fade
```

Lights should animate rather than simply switching on/off.

---

## 30. Glow Effects

Use additive blending.

Suggested glow colors:

- cyan
- purple
- orange
- red
- electric blue

Avoid excessive glow.

Objects should remain readable.

---

## 31. Particle Effects

Particles should appear for:

- bumper hits
- jackpot
- combos
- reactor activation
- multiball
- high score
- ramp completion

Particle styles:

- sparks
- energy particles
- glowing dots
- small smoke
- electric arcs

---

## 32. Screen Shake

Very small camera shake for:

- large bumper collision
- jackpot
- multiball
- reactor explosion

Example:

Normal hit:

0–1 pixel.

Jackpot:

3–5 pixels briefly.

Never make gameplay hard to follow.

---

## 33. Camera

Keep most gameplay fixed.

Do not continuously follow the ball.

Possible subtle effects:

- 1–2% zoom during multiball
- micro shake
- jackpot pulse

The entire table should normally remain visible.

---

## 34. Art Resolution

Create game assets at high resolution.

Recommended master playfield:

approximately:

**2160 × 3840**

Then scale down dynamically.

Use:

- PNG/WebP
- transparent sprites
- texture atlases

Do not load dozens of individual high-resolution files unnecessarily.

---

## 35. Original Art Asset List

Create approximately:

```text
table_background.webp
table_overlay.webp

ball.webp

flipper_left.webp
flipper_right.webp

bumper_blue.webp
bumper_purple.webp
bumper_orange.webp

reactor_core.webp
reactor_ring_1.webp
reactor_ring_2.webp

ramp_left.webp
ramp_right.webp

rail_left.webp
rail_right.webp

target_normal.webp
target_active.webp

slingshot_left.webp
slingshot_right.webp

lane_light_off.webp
lane_light_on.webp

plunger.webp
plunger_spring.webp
```

Plus particle textures.

---

## 36. Animations

Animations required:

### Reactor

- slow rotation
- charge pulse
- energy movement

### Bumpers

- compression
- rebound
- glow flash

### Targets

- hit reaction
- shake
- down animation

### Flippers

Physics-driven rotation.

### Ramp lights

Sequential chasing lights.

Example:

```text
● ○ ○ ○
○ ● ○ ○
○ ○ ● ○
○ ○ ○ ●
```

---

## 37. Sound Design

Professional pinball depends heavily on audio.

Required sounds:

```text
ball_roll
ball_wall_hit
ball_metal_hit

flipper_up
flipper_down

bumper_hit_1
bumper_hit_2

target_hit

slingshot

ramp_enter
ramp_exit

combo

jackpot

super_jackpot

multiball

extra_ball

ball_lost

game_over

new_high_score
```

---

## 38. Music

Background music should adapt.

Normal:

slow futuristic arcade music.

Reactor Mode:

faster version.

Multiball:

high-energy music.

Game Over:

short ending cue.

Music must loop seamlessly.

---

## 39. Haptic Feedback

Use small vibration for:

Flipper:

very light.

Bumper:

light.

Jackpot:

medium.

Multiball:

short double pulse.

Settings:

```text
HAPTICS
ON / OFF
```

---

## 40. Game Flow

Game lifecycle:

```text
App Open
   ↓
Splash
   ↓
Main Menu
   ↓
Play
   ↓
Load Table
   ↓
Ball 1
   ↓
Drain
   ↓
Ball 2
   ↓
Drain
   ↓
Ball 3
   ↓
Drain
   ↓
Bonus Calculation
   ↓
Game Over
   ↓
High Score
   ↓
Main Menu / Play Again
```

---

## 41. End-of-Ball Bonus

After each ball:

calculate bonuses.

Example:

```text
END OF BALL

TARGETS        12,000
RAMPS          20,000
COMBOS         15,000
REACTOR        30,000

MULTIPLIER       x2

TOTAL          154,000
```

Animate numbers quickly.

---

## 42. Local High Scores

Store:

Top 10 scores.

Example:

```text
HIGH SCORES

1. AAA   2,840,500
2. PRD   1,925,300
3. ACE   1,540,200
...
```

For the simplest V1:

store player initials.

Maximum three characters.

---

## 43. Persistence

Use:

**DataStore Preferences**

for:

- high scores
- settings
- music preference
- SFX preference
- haptics
- last selected theme
- tutorial completed

No Room database is necessary initially.

---

## 44. Settings

Settings page:

```text
MUSIC
[──────●──]

SOUND EFFECTS
[───────●─]

HAPTICS
ON

LEFT-HANDED MODE
OFF

TABLE NUDGE
ON

GRAPHICS QUALITY
AUTO

SHOW FPS
OFF
```

---

## 45. Graphics Quality

Three modes:

```text
LOW
MEDIUM
HIGH
```

Auto recommended.

### Low

- fewer particles
- reduced glow
- reduced shadows

### Medium

normal.

### High

- maximum particles
- bloom
- better reflections
- enhanced lighting

---

## 46. Pause Screen

When app loses focus:

pause immediately.

Show:

```text
PAUSED

RESUME

RESTART

SETTINGS

QUIT
```

Do not allow ball physics to continue in background.

---

## 47. Tutorial

First launch only.

Step 1:

```text
TAP LEFT SIDE
TO CONTROL LEFT FLIPPER
```

Step 2:

```text
TAP RIGHT SIDE
TO CONTROL RIGHT FLIPPER
```

Step 3:

```text
HOLD & RELEASE
TO LAUNCH BALL
```

Step 4:

```text
HIT TARGETS
TO ACTIVATE MISSIONS
```

Then:

```text
LET'S PLAY
```

---

## 48. Game State Architecture

Recommended structure:

```text
GameApplication

screens/
    SplashScreen
    MainMenuScreen
    GameScreen
    SettingsScreen
    HighScoreScreen
    HowToPlayScreen

game/
    PinballWorld
    GameController
    ScoreManager
    MissionManager
    ComboManager
    MultiballManager

entities/
    Ball
    Flipper
    Bumper
    Target
    Ramp
    Slingshot
    Plunger

physics/
    PhysicsWorld
    CollisionManager
    CollisionCategories

rendering/
    TableRenderer
    LightingRenderer
    ParticleRenderer

audio/
    AudioManager

storage/
    SettingsRepository
    HighScoreRepository
```

---

## 49. Physics Collision Categories

Define:

```text
BALL
WALL
FLIPPER
BUMPER
TARGET
RAMP
SENSOR
DRAIN
PLUNGER
```

Use collision masks so irrelevant components do not collide unnecessarily.

---

## 50. Sensor Zones

Many scoring objects should use invisible Box2D sensors.

Example:

Ramp entry:

```text
physical ramp
+
invisible ramp sensor
```

Sensor detects:

```text
ball entered
ball exited
direction
```

This prevents duplicate scoring.

---

## 51. Game State Machine

Use states:

```text
MENU

READY

BALL_LAUNCH

PLAYING

BALL_DRAINING

BALL_BONUS

MULTIBALL

PAUSED

GAME_OVER
```

Avoid scattered boolean variables such as:

```text
isPlaying
isEnded
isLaunching
isBallDead
```

A state machine will make development much cleaner.

---

## 52. Score Event System

Use events.

Example:

```text
BumperHit
TargetHit
RampCompleted
OrbitCompleted
ComboCompleted
MissionCompleted
JackpotHit
BallLost
```

ScoreManager reacts to those events.

This keeps physics and scoring separated.

---

## 53. Performance Requirement

Target:

**60 FPS minimum**

Recommended frame budget:

```text
16.67 ms/frame
```

Avoid allocations every frame.

Use object pooling for:

- particles
- floating score labels
- temporary effects

---

## 54. Physics Update

Do not directly use variable frame time for Box2D.

Use fixed timestep.

Example conceptually:

```text
Physics: 1/60 second
Rendering: device refresh rate
```

This keeps physics consistent across phones.

---

## 55. Screen Sizes

Support:

- 720 × 1280
- 1080 × 1920
- 1080 × 2400
- 1440 × 3200
- tablets

Use aspect-aware scaling.

Never stretch the table.

Instead:

```text
scale uniformly
+
small crop/letterbox if required
```

---

## 56. Device Cutouts

Respect:

- camera punch holes
- notches
- navigation bars
- gesture areas

HUD must remain inside safe area.

---

## 57. Accessibility

Provide:

- vibration toggle
- music toggle
- SFX toggle
- reduced effects option
- high-contrast score
- readable text

Do not depend exclusively on red/green colors for important state information.

---

## 58. Offline Requirement

V1:

**100% offline**

Do not require:

- internet permission
- accounts
- login
- server
- ads
- analytics

This makes Play Store approval and privacy compliance much simpler.

---

## 59. Permissions

Ideally:

**No sensitive Android permissions.**

If nudge uses motion:

accelerometer does not normally require runtime permission.

The game should not request:

- contacts
- files
- camera
- microphone
- location
- notifications

for V1.

---

## 60. V1 Screen List

Keep initial release focused.

```text
1. Splash Screen
2. Main Menu
3. Game Screen
4. Pause Screen
5. Game Over Screen
6. High Scores
7. How To Play
8. Settings
9. Credits
```

---

## 61. Professional Splash Screen

Duration:

approximately 1.5–2 seconds.

Example:

```text
        ◇

   NEON FORGE
     GAMES

       presents
```

Then transition into the game logo.

Avoid a static plain logo.

Use:

- small glow
- fade
- particle animation

---

## 62. Recommended Original Game Names

Potential names:

- **Neon Pinball**
- **Cosmic Pinball**
- **Hyper Pinball**
- **Neon Reactor Pinball**
- **Pinball Nova**
- **Quantum Pinball**
- **Arcade Reactor**

My preference for this concept:

# Neon Reactor Pinball

It clearly communicates the game genre and matches the visual theme.

---

## 63. Icon Design

App icon concept:

```text
dark background

large chrome pinball

bright cyan/purple reactor ring

two small flippers underneath

neon glow
```

Avoid putting too much detail inside the icon.

The ball should remain recognizable at small sizes.

---

## 64. Game Board Quality Target

The final table should contain enough visual detail that a screenshot immediately feels like a commercial game.

Target approximately:

- 2 main flippers
- 2 slingshots
- 3 bumpers
- 2 ramps
- 2 orbit lanes
- 8–12 targets
- 1 central animated feature
- 1 launch lane
- 20–30 table lights
- 5+ animated effects
- several metallic rails
- detailed original background artwork

That will give approximately the same visual richness as the reference you supplied.

---

## 65. Important Physics Edge Cases

Test carefully:

### Ball stuck between objects

Detect very-low velocity for several seconds.

Allow automatic subtle nudge.

### Ball escapes table

Restore to safe point.

### Multiple collision scoring

Prevent a single collision from generating hundreds of points.

Use hit cooldowns.

### Ball passing through flipper

Use continuous collision detection.

### Multiball ball tracking

Never end the ball until every active ball is drained.

---

## 66. High-Quality Gameplay Feedback

Every important action should produce three types of feedback:

```text
PLAYER ACTION
     ↓

PHYSICAL FEEDBACK
ball/flipper movement

     +

VISUAL FEEDBACK
light/particles/animation

     +

AUDIO FEEDBACK
sound/music
```

For example:

```text
Ball hits bumper

Bumper compresses
+
Bright flash
+
Particles
+
Score floats upward
+
"BANG" sound
+
Small vibration
```

This is what creates a premium feel.

---

## 67. Floating Score

When something is hit:

```text
+1,000
```

should appear briefly near the object.

Animation:

```text
spawn
↓
scale 120%
↓
float upward
↓
fade
```

Duration:

approximately:

**600–900 ms**

---

## 68. Major Event Text

Large announcements should appear in center:

```text
MULTIBALL!
```

```text
JACKPOT!
```

```text
REACTOR OVERLOAD!
```

```text
SUPER JACKPOT!
```

Animation:

```text
small
↓
zoom
↓
flash
↓
hold
↓
fade
```

---

## 69. Professional Polish Requirements

Before release, game must contain:

- smooth scene transitions
- no default Android buttons
- no placeholder icons
- no rectangular developer-style controls
- no debug text
- no visible FPS unless enabled
- no image stretching
- no low-resolution textures
- no abrupt audio transitions
- no physics glitches
- no input lag
- no blank screens
- no clipped text

---

## 70. Recommended Development Phases

For AI-assisted development, do **not** ask the coding model to build the complete game in one huge generation.

Build in these phases:

```text
PHASE 1
Project + game loop

PHASE 2
Table + ball physics

PHASE 3
Flippers + launch

PHASE 4
Walls + bumpers + drain

PHASE 5
Targets + ramps

PHASE 6
Scoring

PHASE 7
Combos + missions

PHASE 8
Multiball

PHASE 9
Particles + lighting

PHASE 10
Audio

PHASE 11
Menus + settings

PHASE 12
High scores

PHASE 13
Optimization

PHASE 14
Play Store build
```

This will produce much better results than asking an AI coding model to generate the entire application at once.

---

## 71. Definition of Done for V1

V1 should not be considered complete until the following work reliably:

```text
✓ Ball launches properly
✓ Both flippers work simultaneously
✓ Ball physics feels realistic
✓ Ball cannot pass through walls
✓ Bumpers react correctly
✓ Slingshots work
✓ Targets score correctly
✓ Ramps work
✓ Combo detection works
✓ Reactor mission works
✓ Multiball works
✓ Jackpot works
✓ Ball save works
✓ Three-ball game works
✓ Game over works
✓ Local high score persists
✓ Audio works
✓ Haptics work
✓ Pause/resume works
✓ 60 FPS maintained
✓ Different Android resolutions work
✓ No internet is required
✓ No app crash after repeated games
```

---

## 72. AI Development Instruction

At the beginning of your coding specification, put this instruction:

> Build a production-quality Android pinball game rather than a prototype. Do not use placeholder rectangles or developer-style graphics in the final UI. The game must target smooth 60 FPS gameplay, realistic Box2D physics, responsive multi-touch flipper controls, high-quality animated lighting, particles, sound, haptic feedback, polished transitions and professional arcade-style visual design. Use an original table, artwork and branding. Do not reproduce copyrighted pinball artwork or characters from reference images. Structure the code cleanly so that additional tables can be added in future versions.

And add:

> Never simplify a requested visual effect merely because a basic implementation is easier. Where a final graphical asset is unavailable, implement the complete asset-loading architecture and use a clearly named temporary asset that can later be replaced without code changes.

---

## 73. Recommended V1 Scope

For the first Play Store version, build **one extremely polished table rather than 3–5 average tables**.

A single table with:

- realistic physics
- Reactor Core
- ramps
- bumpers
- missions
- three-ball multiball
- jackpots
- high scores
- excellent lighting
- strong sound design

can already achieve a professional visual and gameplay standard.

Future versions can add:

- Table 2 → Space Station
- Table 3 → Pirate Treasure
- Table 4 → Haunted Mansion
- Table 5 → Cyber City

The architecture should therefore treat `PinballTable` as configurable from day one so future tables do not require rebuilding the game engine.

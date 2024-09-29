# Preparations

## How to setup tools 
1. Install Calva (VS Code extension)
2. Run `npm install`
3. Enable binaryage's DevTools custom formatters in Chrome settings (see https://github.com/binaryage/cljs-devtools/blob/master/docs/faq.md#why-some-custom-formatters-were-not-rendered)

## How to run it from command prompt
1. watch:         `npm run watch`

2. release:       `npm run release`

4. start server:  `npm run server`

5. build report:  `npm run report`

6. REPL (node):   `npx shadow-cljs node-repl`

## How to run REPL in VS Code (Calva)
1. Calva jack-in `CTRL+ALT+C+J`
2. Project type select `shadow-cljs`
3. Builds to start `:astrox, :test`
4. Alias to launch `watch`
6. Select `:astrox` or `:test` as server to connect the REPL to.  
   **NOTE** that REPL will work only after the application was run (e.g. app server URL was hit).


# Development

## How to run the application
1. pick control server's URL of the shadow-cljs and select application server
2. OR select first URL displayed by shadow.cljs in the log.  
   It's a server that is defined via `:astrox` build target

## How to run unit tests
1. Got to the unit test server URL.  
   It's a server that is defined via `:test` build target

## How to create images atlas for UI resources
1. Use TexturePacker


# Improvements

1. Reactions: consider to use WeakRef for downstream references (see https://tonsky.me/blog/humble-signals/)
Only makes sense to do if reactions are not always disposed by the framework, e.g. when the the user is responsible to do it.

2. Reaction: consider to align with ideas in https://tonsky.me/blog/humble-signals/ and https://dev.to/modderme123/super-charging-fine-grained-reactive-performance-47ph

# Topics

- Culling       
  - look at culling lib: https://github.com/davidfig/pixi-cull
  - 40k objects render - https://github.com/pixijs/pixijs/issues/6350


# Issues

1. The @pixi/tilemap@3.2.1 package has outdated peer references to the older @pixi/core package:  
   _Install package manually with `npm i pixi-viewport@4.34.4 --legacy-peer-deps` command before starting ClojureScript jack-in or build._

2. The SPECS instrumentation will not always pick new specs definition on hot-reload.  
   _Save core_dev.cljs file in order to refresh SPECS instrumentation_

# TODO

- scene in game_screen.cljs has to be reset when exiting game_screen and and then starting game again.
  Currently it's crashing.

- resizing game on Level1 will "jump" object below the screen lower bound

+ make dedicated game stage container:
- make it and all children non-interactive -> (set! (.-interactiveChildren xxx) false)

- physics integration-step - round position and orientation to avoid ECS from updating rigid-body on every step
  -> add eq to RigidBody so it will not trigger re-evaluation 

- use drag instead of dumping?
  https://discussions.unity.com/t/how-drag-is-calculated-by-unity-engine/97622/2  

- disable mouse cursor for game screen
    (set! (.-cursor screen) "none")
    (set! (.-eventMode screen) "static")


- Touch is not working for buttons?

- Fix Pixi error in sprite tweening on hot reload -> still after recent "fix" with re-creation of shared ticker

- Pixi UI bug: [v.0.9.1] FancyButton: tweedle.js reports a WARNING on each playAnimations if both "animations" and any of "defaultView", "hoverView", "pressedView" or "icon" properties are set in constructor.
  see https://github.com/pixijs/ui/issues/115
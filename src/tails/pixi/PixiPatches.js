import UI from "@pixi/ui"

UI.FancyButton.prototype.destroy = function(options) {
    // The FancyButton class does not handle destruction gracefully and allows button animations to play 
    // even after the button itself was destroyed, which leads to runtime errors.
    // To prevent these errors from happening, we keep the button alive, allowing its animations to finish.
    
    // origFancyButtonDestroy.call(this, options)

    // Removing even listeners seems to only safe cleanup action we can do here
    this.removeAllListeners()
}
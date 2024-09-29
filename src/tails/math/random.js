
//
// Custom implementation of "seedable" random generator of better quality.
//
// Sources:
//  - Pseudorandom number generators: https://github.com/bryc/code/blob/master/jshash/PRNGs.md, https://stackoverflow.com/a/47593316
//  - dual LCG random generator: http://www.gameaipro.com/GameAIPro3/GameAIPro3_Chapter40_Vintage_Random_Number_Generators.pdf
//

// see: https://github.com/bryc/code/blob/master/jshash/PRNGs.md
function sfc32(a, b, c, d) {
    return function() {
      a |= 0; b |= 0; c |= 0; d |= 0; 
      var t = (a + b | 0) + d | 0;
      d = d + 1 | 0;
      a = b ^ b >>> 9;
      b = c + (c << 3) | 0;
      c = c << 21 | c >>> 11;
      c = c + t | 0;
      return (t >>> 0) / 4294967296;
    }
}

// see: https://github.com/bryc/code/blob/master/jshash/PRNGs.md
function xmur3(str) {
    for(var i = 0, h = 1779033703 ^ str.length; i < str.length; i++)
        h = Math.imul(h ^ str.charCodeAt(i), 3432918353),
        h = h << 13 | h >>> 19;
    return function() {
        h = Math.imul(h ^ h >>> 16, 2246822507),
        h = Math.imul(h ^ h >>> 13, 3266489909);
        return (h ^= h >>> 16) >>> 0;
    }
}

// Returns new random number generator function
export function createPrang(seedString) {
    var seedFn = xmur3(seedString)
    return sfc32(seedFn(), seedFn(), seedFn(), seedFn())
}

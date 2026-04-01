# PRISM-TTF
A TTF-to-SDF typeface glyph rendering utility designed for my web browser, Prism. Renders a given TrueType font file into a directory of IEEE754 floating-point SDFs (big endian).

## Running Directions
> gradle run --ttf <path_to_ttf_file> --font-size <font_size> --dpi <screen_dpi> --sdf <optional_sdf_directory>

## Rendering Criteria
 - Grayscale antialiasing
 - No support for hinting
 - No support for subpixel rendering
 - No support for non-glyph ligatures
 - No support for kerning
 - 10% by median glyph padding

## Who is this for?
Me. With fairly limited TTF feature implementation, I designed prism-ttf for my own purposes only in extracting efficient drawing performance without startup penalties in my simple web browser, Prism.

## Related Projects
- _prism_, my from-scratch web browser supporting a limited subset of HTML 5, CSS 3, and ECMAScript 16 via HTTP with a custom layout engine and THIS TTF renderer.
  - [https://github.com/connorjlink/prism](https://github.com/connorjlink/prism)

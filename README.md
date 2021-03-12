# The Lagrange keyboard

![The Lagrange keyboard](./doc/lagrange_keyboard_bridged.png?raw=true)

The Lagrange is a configurable, split, handwired keyboard, designed
for ergonomics.  This is meant in the sense that as many keys as
possible should be accessible with the minimum possible effort,
although some less reachable keys are also incorporated in the design
as they can still be useful.  Features that attempt to improve
ergonomics include concave main sections, convex thumb sections, a
tenting stand, palm keys and specially designed keycap shapes.  To get
a better idea of what all this looks like, see the [3D model of the
final assembly](./things/right-assembly.stl) and the photos of my
build in the [doc/](./doc/) directory.

The Lagrange is of course inspired by the designs that preceded it,
most notably the [Dactyl](https://github.com/adereth/dactyl-keyboard)
and the [Dactyl-ManuForm](https://github.com/tshort/dactyl-keyboard).
Differences with these designs and additional contributions to the art
include:

* A new thumb section layout, based on the spherical workspace of the
  thumb (and with the added bonus of a cross-shaped set of keys,
  useful for "directional" functions).
* Palm keys, meant to be pressed with the edge of the palm, without
  needing to leave the home row.
* An integrated, configurable tenting stand.
* Non-standard keycap designs for thumb and palm keys.
* Code features aiding exploration of different configurations, such
  as "interference test builds", allowing design validation prior to
  printing and "partial test builds", allowing quick prints of
  arbitrary subsections of the keyboard.  (These features can be
  instrumental to ergonomics optimization, as they allow both deeper
  exploration of possible configurations and minimizing the key
  distances within a given configuration, while still avoiding
  interference between switches, keycaps and the case.)

A further goal of the design was to comprise parts, both mechanical
and electrical, that fit together and that can be easily assembled to
result in something that can withstand the wear and tear of daily use.

## Building a Lagrange

If you want to build your own Lagrange, you can find detailed
instructions in [BUILD.md](./BUILD.md).

## License

Copyright © 2020 Dimitris Papavasiliou

This program (which includes everything except the
[things/](./things/), [kicad/](./kicad/) and [doc/](./doc/)
directories, is distributed under the [GNU Affero General Public
License Version 3](./LICENSE.AGPL).  The controller PCB design
residing in [kicad/](./kicad/), is distributed under the [GNU General
Public License Version 3](./LICENSE.GPL). Finally, the generated
models residing in [things/](./things/), as well as the material in
[doc/](./doc/), are licensed under a [Creative Commons
Attribution-ShareAlike 4.0 International License](./LICENSE.CC).

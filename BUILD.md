# Building instructions

Building a handwired keyboard can be a daunting task and the Lagrange
is no exception.  Many steps are involved, involving different
disciplines, with each one offering plenty of potential for error.  As
such, the typical ad-hoc build method, is perhaps not likely to fare
well. I've tried to address the problem by finding approaches that
make each step straightforward in execution, even if a little more
work may be required.  The guide below will hopefully present a
comprehensive, but not needlessly verbose set of instructions.

## Table of contents

- [Customization](#customization)
  * [Installation](#installation)
  * [The case](#the-case)
    + [Main section](#main-section)
    + [The thumb section](#the-thumb-section)
    + [Palm keys](#palm-keys)
    + [Switches](#switches)
    + [The case walls](#the-case-walls)
  * [The bottom cover](#the-bottom-cover)
  * [The stand](#the-stand)
  * [The boot](#the-boot)
  * [Final checks](#final-checks)
- [Printing](#printing)
  * [The chassis](#the-chassis)
  * [The bottom cover](#the-bottom-cover-1)
  * [The stand](#the-stand-1)
  * [The boot](#the-boot-1)
  * [Miscellanea](#miscellanea)
    + [Printing keycaps](#printing-keycaps)
  * [Some notes on finishing](#some-notes-on-finishing)
- [Assembly](#assembly)
  * [Installing the threaded inserts](#installing-the-threaded-inserts)
  * [Mounting the switches](#mounting-the-switches)
  * [Wiring the matrix](#wiring-the-matrix)
    + [Columns](#columns)
    + [Rows](#rows)
  * [Wiring the harness](#wiring-the-harness)
  * [Installing the boot](#installing-the-boot)
  * [Keycaps](#keycaps)
- [The controller](#the-controller)
  * [BOM](#bom)
  * [Design](#design)
- [Software](#software)

## Customization

The most straightforward way to build the Lagrange, is from the STL
files provided in the [things/](./things) directory.  These were
designed based on my hands, but I have no reason to doubt that they
would serve most people.  After all, our hands can adapt to wildly
differing configurations (including flat keyboards or concave
geometries, staggered or columnar arrangements, thumb sections at
various locations, etc.)  So you should be fine with the default
design, and if you decide to use it, you can skip this section.  On
the other hand, tailoring the geometry to one's own hands might still
have some benefits, so the Lagrange attempts to facilitate the
process.

This is achieved by making the design parametric and allowing
parameters to be changed wherever that might be useful in promoting
ergonomy.  Note that *no* guarantees are made that any arbitrary
parameter value will result in a buildable design.  Some might need
minor, or even major fix-ups, others might make the code crash.  Making
a truly parametric keyboard, even if the parameter space is
constrained, would be a tricky task.

One compromise might be to support several different, perhaps lightly
customizable but otherwise premade configurations.  The current code
does not support this, but it should be simple enough to do so.  If
you end up building a substantially different configuration and feel
that it works well, and that others might want to build it, consider
opening an issue or PR to incorporate the design.  If in the process
you change one or more parameters and end up with a problematic
design, consider trying to fix it and submitting a PR as well.  If you
can't fix it, feel free to open an issue and I will try to help out.

The first step to building your own design is to download the source
and get it to run.

### Installation

You'll need to install [the Clojure runtime](https://clojure.org),
[Leiningen](http://leiningen.org/) and
[OpenSCAD](http://www.openscad.org/), in whatever way is best for your
operating system and distribution.  The commands below assume you're
using Linux; the relevant procedures for other operating systems
should be similar.

Unfortunately, a needed fix in the library that generates SCAD code
has not been released yet, so until that happens you'll need to
install it yourself.  You can do that as follows:

```bash
$ cd /tmp
$ git clone https://github.com/dpapavas/scad-clj.git -b extrude-scale-fix
Cloning into 'scad-clj'...
remote: Enumerating objects: 1105, done.
remote: Total 1105 (delta 0), reused 0 (delta 0), pack-reused 1105
Receiving objects: 100% (1105/1105), 11.53 MiB | 5.09 MiB/s, done.
Resolving deltas: 100% (515/515), done.
$ cd scad-clj
$ lein install 
Created /tmp/scad-clj/target/scad-clj-0.5.4-SNAPSHOT.jar
Wrote /tmp/scad-clj/pom.xml
Installed jar and pom into local repo.
```

With this out of the way, check out the source for the Lagrange and
run it to produce your own design:

```bash
$ git clone https://github.com/dpapavas/lagrange-keyboard.git
Cloning into 'lagrange-keyboard'...
remote: Enumerating objects: 3, done.
remote: Counting objects: 100% (3/3), done.
remote: Compressing objects: 100% (3/3), done.
remote: Total 41 (delta 0), reused 3 (delta 0), pack-reused 38
Unpacking objects: 100% (41/41), done.
$ cd lagrange-keyboard
$ lein run right --draft
```

The above invocation is just an example.  In general anything after
`run` is interpreted in one of two ways: If it starts with `--` it is
interpreted as a request to change a parameter on the fly.  This is
quite useful for a certain set of parameters which don't really affect
the design, but which generally need to be changed often while
exploring possible configurations.  We'll get to those in a moment.
Anything else is a keyboard part to build.  These include the
following:

 * `right`, `right-cover`, `right-stand`, `right-boot`: The main
   chassis, bottom cover, tenting stand and anti-slip rubber boot for
   the right side.
 * `right-assembly`: All the above, assembled together.  Mostly useful
   to see how the end result will look and also to ensure that
   everything fits together.
 * `right-subassembly`: A partial assembly of `right` and
    `right-cover` only.  Mostly useful for interference tests between
    the keycaps, switches, case and PCB.
 * `left`, `left-cover`, `left-stand`, `left-assembly`,
   `left-subassembly`, `left-boot`: Same as above, but for the left
   side.  Most parts are simple mirror images, but the PCB is reversed
   on the left side, leading to different hole locations on the bottom
   cover.
 * `misc`: This includes miscellaneous small parts, such as a wiring
   bracket, certain printable keycaps, etc.

Some invocation examples:

The following builds the right side chassis in production mode:

```bash
lein run right --no-draft
```

In general, a switch such as `--foo` or `--no-foo` will set the global
variable `foo?` within the code to `true` or `false` respectively.
Examples of such variables include `draft?`, `mock-threads?`,
`place-keycaps?` and `place-switches?`.  Consult the source for the
meaning of these variables.

A switch like `--foo="value"` sets the global variable `foo` to the
result of evaluating `value`.  Make sure you quote the value properly.
The following builds a part of the main key section plus the thumb
section, for both sides:

```bash
lein run left right --thumb-test-build --key-test-range="[1 2, 4 4]"
```

  Such prints can be useful for partial test prints to try out the
ergonomics, before committing to a large print job (although you'd
typically print one side only).

### The case

This is the main part, that carries the switches and associated
wiring.  It consists of the main and thumb sections, of the keys meant
to be pressed with your fingers and thumb respectively, as well as a
lone palm key.  You can see a build of the default configuration
[here](./things/right.stl).

#### Main section

The nominal size of the main section is 6 columns by 5 rows, although
some columns are shorter than others.  You can change the size with
the `row-count` and `column-count` parameters, but be warned that this
is uncharted territory and extensive changes will be required to get a
configuration to work, if it can work at all.  Larger sizes are more
likely to fare well than smaller sizes.

Apart from size, the geometry of the main section depends on many
parameters and, to make matters worse, good values for some of them
may depend on the values of others.  It's therefore a good idea to
proceed methodically and test each parameter adjustment separately.
To facilitate testing, special modes are selectable that produce only
parts of the keyboard, which can be printed relatively quickly and
cheaply.  For the main section, this mode can be selected via the
`key-test-build?` and `key-test-range` variables, which you can
specify on the command line for convenience.  The former enables the
key (i.e. main section key) test build mode and the latter selects
what keys to print.  As an example the invocation

```bash
lein run right --key-test-build --key-test-range="[2 0, 2 4]"
```

builds the keys between zero-indexed column-row coordinates (2, 0) and
(2, 4), or in other words, the third column.  Such fragments take some
creative positioning to print without needing too much support, but
they can generally be printed in a few hours.  Below, where a set of
coordinates like `[2 0, 2 4]` are mentioned, they're meant as a value
for `--key-test-range` in an invocation like the above.

Begin by adjusting columns, as row geometry depends on column
curvature, but each column is relatively independent of the others.
The required column radius depends mostly on the length of the last
two segments of the finger, so you can measure the distance from the
middle of the second joint to the tip of each finger and add the
height of the keyswitch and keycap (about 18mm for SA keycaps and 14mm
for DSA, mounted on Cherry MX style switches) to arrive at values of
`column-radius` for each column.  Since the last segment tends to curl
up a bit, as the finger is rotated inwards you can reduce that value
by 5-10% if you want a tighter fit.

After adjusting the radii, adjust the column phases (which are the
relative rotations, not displacements, of the columns) so that the
faces of the home row keys are parallel.  Use test builds (without
actually printing) with only two consecutive home row keys (e.g. `[2
2, 3 2]`) to see how well the home row keys line up and adjust
manually.

Column offsets and heights account for the different lengths (of the
first segments and overall) of each finger, so that the corresponding
column needs to be shifted both front to back and upwards or
downwards, if the finger is to rest on the home row in its relaxed
position.  Some of the front to back shifts will result from the phase
shift introduced to align the home row in the previous step.  Further
offsetting should be applied via `column-offsets` and
`column-heights`.  Values for these can be estimated by measuring the
relative distances between the second joint knuckles and fingertips
respectively, with the fingers held in a relaxed curl.

Finally, adjust row and column spacings to the minimum amount needed
to clear any interferences between adjacent keycaps, or the keycaps
and case, with the keys in both depressed and released positions.  You
can place keycaps by specifying `--place-keycaps` and you can depress
them with `--keys-depressed`.  Interferences between keycaps can be
detected by visual inspection, but for interferences between keycap
and the case, a special test mode, selected with
`--interference-test-build` can be helpful.  This switch will produce
a special build that will subtract the case, bottom cover, PCB and
other parts from the the keycaps and switches (only selected parts of
the current assembly participate in the difference).

Here's an example:

```bash
lein run right --place-keycaps --keys-depressed --interference-test-build
```

When this build is rendered, you'll be left with just the caps, which
you can inspect for missing portions.  For instance, if a corner of
keycap appears sheared off, it means the missing portion interfered
with the case.  The same procedure can be followed to detect
interference between keyswitches and the case, or bottom cover.  Just
use `--place-keyswitches` and `right` or `right-cover` as needed.

Parameters for rows can't be estimated by measurement.  First decide
if you want to change `row-phase`, which essentially dictates built-in
tent.  Although you can set the exact tenting angle when configuring
the stand later on, this tent is important in several ways.  It sets
the minimum tent and thereby also the maximum height of the chassis.
Besides printing time and cost, the latter also determines the
available room for the thumb section and therefore the maximum slant
and minimum radius you can get for it.  Note though that changing the
row phase is likely to require some fix-ups, so double check the final
build by visual inspection and intereference tests.

It'd be best to leave the other parameter, `row-radius` at the default
value initially and make sure `row-spacing` is set appropriately as
described previously.  From there on print tests and adjust to taste.
Very rough and quick tests can be had by printing separate columns
(e.g. `[2 0, 2 4]` for the middle column) to try out the column radii
and by printing the center row (i.e. `[0 2, 5 2]`) for offsets and
heights and for the row radius.  Once settings are found to be in the
right ballpark, print the four center columns (i.e. `[1 0, 4 4]`) to
see how well everything fits together.  Mount switches and keycaps and
make sure that the keys are accessible with the minimum of strain, that
is, without needing to extend the fingers more than necessary, and
without needing to contract them to clear the keycaps.  The latter
would be a sign of insufficiently large column and/or row radii.  A
small row radius would also cause the sides of the middle and ring
fingers to drag against the sides of the keycaps of the neighboring
columns.

When testing out prints, keep the following points in mind.  Even if
the geometry is optimal, your fingers will still not be used to it, so
don't expect to be able to type straight away.  Ideally, your fingers
should be able to reach all keys of the main section by mere rotation,
without running into nearby keycaps as they do so.  Given that our
hands can adapt to infinite radius (i.e. a flat keyboard), it would
probably be wise to err on the side of larger radius.

#### The thumb section

The thumb section is a bit trickier to adjust, mostly because its
geometry is inherently more complex.  If you've changed the main
section geometry, run an interference test (use `--thumb-test-build`
to build only the thumb section) with switches and keycaps and make
sure that they don't interfere with the chassis (pay special attention
to the upper row).  Adjust the x and y values of `thumb-offset` so
that the thumb section is as close as possible to the main section,
without interference.  Adjust the z offset to the lowest possible
value that does not result in the key plate of the lowest key punching
through the XY plane.

Adjusting the rest of the parameters requires some understanding of
the thumb section geometry.  Like the main section (which,
disregarding offsets and phases, is essentially the inside of a
torus), the keys are placed on the thumb section along circular arcs
of radius `thumb-radius` for the top row, or derived from it for the
cross-shaped portion below it.  The `thumb-slant` parameter determines
the downwards slant of the thumb section.  If this were set to zero,
the axis of rotation for the thumb keys would be the Z axis and the
thumb section would be flat and horizontal.  A positive value
(actually in radians, although best though of as just a number)
rotates that axis downwards, towards the XY plane, bending the keys
downwards in the process.

Of the remaining parameters, `thumb-key-phase`, the rotation of each
key along its arc, should be set as the lowest possible values, which
still prevent interference between keycaps, switches and the chassis.
The `thumb-key-offset` can be used to finetune the position of each
key (it is responsible for the relative offset of the cross-shaped
keys relative to the top row among other things) and `thumb-key-slope`
determines the relative slope of each key (and again the cross-shaped
keys in particular).

The default setup is quite good.  The three first keys of the top row
are comfortably accessible without shifting the hand position, or
straining the thumb joint.  The last requires a bit of one or the
other, but is still usable for less frequently used keys, or special
modifiers.  In the cross-shaped section, two are again quite
accessible, while the other two can be readily reached with the palm,
or with a minor shift.  That said, I have no reason to believe that
the current setup is optimal and further exploration may prove
fruitful.

#### Palm keys

The palm keys, as suggested by the name, are keys at the bottom of the
outer columns, meant to be pressed by the edge of your palms.  They
work well with either convex or special saddle-top keycaps, of which
more later.  They share the key size of the outer column, adjustable
via `last-column-scale` and their exact location can be fine-tuned via
`palm-key-offset`, if required.

#### Switches

The switch mounts are designed for Cherry MX style switches.  Although
there are some parameters that can be adjusted, I do not think that
any other switches can be supported as is.  They only parameter of
interest, is `plate-thickness`, through which the thickness of the
"key plate", that is the chassis top can be adjusted.  The default
value is fine though and I do not think that much can be achieved by
changing it.

Optionally, small nubs can be placed at the edges of the switch holes,
meant to engage the tabs found in switches and retain the switch more
firmly.  I have not had much luck with these and have resigned myself
to gluing the switches in place, but should you wish to try, you can
configure them with the `place-nub?`, `nub-width` and `nub-height`
parameters.

#### The case walls

Not much can be customized here without changing the code itself.  The
`wall-thickness` parameter allows for making the walls thinner, if
printing time or cost is critical.  The default thickness is sturdy
enough, so that higher values are probably not very useful.

The bottom cover and stand, are attached to the chassis by screws.
You can change the location and number of the bosses for the screws
via the `screw-bosses` parameter, but keep in mind that this might
also necessitate changes to the stand.  You can also change the size
and threading of the screw bosses.  The default setup is meant for use
with self-tapping threaded inserts, with an M4 x 8mm internal thread
and a 6.5mm external thread with 0.7mm pitch.  The latter can be
printed relatively well, even at 0.2mm layer height, so that the
inserts can be simply screwed in, with little potential for error.  If
you intend to use different inserts, you'll probably need to adjust
`cover-fastener-thread` and possibly `screw-boss-height` and
`screw-boss-radius`.

If you do end up making changes to the side walls, or if you need to
print test pieces, for example to test insert installation, note that
there is a special case test build mode.  This places cuboid volumes
at the bosses specified by `case-test-locations`, of size and offset
relative to the boss given by `case-test-volume` and produces a volume
by taking their convex hull (a sort of minimal volume encompassing
them all).  It then builds only the part of the case that falls within
that volume.  For instance, you can use the following build invocation
to produce a test print of the outer column of the main section:

```bash
lein run right --case-test-build  --case-test-locations="[0 1]" --case-test-volume="[35 50 100]"
```

### The bottom cover

The bottom cover is made to fit the chassis and PCB, so it doesn't
offer (or require) much adjustability in itself.  For reference,
[here](./things/right-cover.stl) is the part in its default
configuration.  You can set the thickness via `cover-thickness` but
again, the default value should be fine.  The cover is fastened to the
chassis via M4 countersunk screws and you can adjust some parameters
of their geometry through the `cover-countersink-*` parameters, should
the need arise.

The controller PCB is mounted on the bottom cover, fastened onto screw
bosses designed for self-tapping threaded inserts with M3 x 6mm
internal diameter and 5mm outer diameter with a thread pitch of 0.5mm.
If you intend to use different inserts, the thread specification can
be adjusted through `pcb-fastener-thread`.  Changes to the chassis
might necessitate changing the PCB mount location.  This can be
adjusted with `pcb-position`, which will properly relocate the holes
for the connectors and reset button as well.  Changes to the PCB
itself, or use of a different controller PCB will require code
changes.

### The stand

The stand is essentially a rotational extrusion of the bottom cover,
that is, the solid produced when the bottom cover outline is swept
along an angle, with some parts removed.  The result, in the default
configuration, can be seen [here](./things/right-stand.stl).  The
angle of rotation, which is the tenting angle, or rather, the
additional tenting angle due to the stand, can be adjusted via
`stand-tenting-angle`.  To avoid bulk, the inside of the stand volume
is hollowed out, so that only a strip along its perimeter remains.
The width of that strip is `stand-width`.  To further lighten the
part, the upper portion of the stand (which attaches to the bottom
cover) is shortened as dictated by `stand-split-points`.  The meaning
of these numbers has to do with details of the stand's construction,
so it's best to just increase or decrease them until the desired
result is achieved.  Finally, a wedge-shaped part is cut out of what
remains of the stand.  This is formed so that the thickness of the
inner extremities of the stand are as specified in
`stand-minimum-thickness`, and the tip of the wedge is of size and
location determined by the `stand-cutout-*` parameters.  The stand
attaches to the chassis, via a set of shared screw bosses, given by
`stand-boss-indexes`.

The above probably sounds complex, but some experimentation with the
parameters will provide a feel for how the stand can be modified.  I
anticipate that many (if not most) will be put off by the relatively
large tenting angle, so, although I can personally attest that
ergonomically, it performs excellently, below are some parameters that
yield a serviceable-looking, albeit entirely untested 15° stand:

```clojure
(def stand-tenting-angle (degrees 15))
(def stand-cutout-radius [4 3])
(def stand-cutout-depth [-1 -1/2])
```

This is probably the smallest tenting angle, which still provides
enough clearance for the USB connector, but you should check via
`--place-pcb`.  Finally note that you can use `--case-test-build` to
produce test prints here too, much as before.

### The boot

You can simply stick anti-slip pads under the stand to keep it from
sliding around the table, but if you have a printer that can print
TPU, or some other elastic material, you can print a custom rubber
boot that will fit snugly under the stand.  In the default
configuration, it looks like [this](./things/right-boot.stl).  You can
set the height (or perhaps depth) of the boot via `boot-wall-height`.
The sidewall thickness and the thickness of the bottom of the boot can
be set via `boot-wall-thickness` and `boot-bottom-thickness`
respectively.


### Final checks

Before producing the final parts for printing, you should do an
interference test, to make sure that the switches and keycaps can be
mounted without issues.  You should do that even if you didn't change
the default design substantially; a lot of cleanup and polishing was
done to the code prior to publication, so that it might not produce
exactly the same parts I printed.  I tried my best to make sure the
produced parts are correct, but I may have missed something.

Keycaps that run into each other can be detected by straight visual
inspection, as they can be seen to merge into one part.  Interference
between parts (especially switches) and the chassis can be harder to
detect, as these are designed to fit quite snugly at places and
detecting whether a tip penetrates the chassis can be hard.  To aid
inspection, you can produce an interference test build with the
following invocation:

```bash
lein run right-subassembly --interference-test-build --place-pcb --place-keycaps \
                           --place-keyswitches
```

This will essentially subtract the case, bottom cover and PCB from the
keycaps and switches.  Any interference, that would not allow mounting
a cap or switch will show up as a missing piece from the part in
question.  To test that all keycaps can be fully depressed without
interference, rerun the above, adding `--keys-depressed`.

Load the produced build into OpenSCAD and render it, then carefully
inspect the result.  Look for clipped tips or edges in the switches
and keycaps.  Pay special attention to parts around the perimeter and
in the top thumb row, as these are the most likely to interfere with
the case.

Finally, once you're satisfied that the part is ready for printing,
build it with as follows:

```bash
lein run right --no-draft --no-mock-threads
```

The first switch disables draft mode, which results in a smoother
build, albeit of accordingly larger complexity.  The latter disables
thread mocking, which prevents rendering of threads, producing smooth
bores instead (threads are complex and OpenSCAD can't seem to handle
them without severe slow-downs).  Once the part is built, load it into
OpenSCAD, render it and prepare for a long wait.

## Printing

The proper print settings will naturally depend on the printer, so I
will limit the discussion here to a broad description of my experience
in attempting to print each part.  Note that this was essentially my
first printing project, so some of my conclusions or choices may well
be poor.  Print settings will be described in Cura terminology, as
that is the only slicing software I've used in practice.  As far as
I've seen, most mentioned features should be available in other tools
as well.  Keep in mind the possibility of building partial prints with
`--case-test-build` when experimenting.

### The chassis

My first intention, was to print the keyboard in ABS, as it is a
durable but not too flexible material, can be printed with relatively
good surface finish (i.e. no zits, blobs, stringing etc.) and is easy
to post-process.  Unfortunately printing in ABS proved impossible for
me due to warping.  After experimenting with various slicing
parameters (smaller or larger brims, rafts, various designs of the
perimeter to avoid sharp edges where warping stresses tend to be
maximal, various distributions of the screw bosses, in the hope that
they'll function as "mouse ears", various printing and bed
temperatures and even a makeshift cardboard enclosure, which
admittedly wasn't too airtight) without coming close to producing a
warp-free part, I decided that ABS was not an option and turned to
PETG.

PETG is functionally very suitable, it shares most of the desirable
qualities of ABS mentioned above, with the exception of suffering from
stringing and surface imperfections.  It also has a glossy appearance
which might be considered a pro or con, according to taste.

Functionally, a small layer height should not be too important.  If
you intend to post-process the part in order to get a smooth surface
though, a smaller layer height can make this much easier.  I printed
using 0.2mm layers at 50mm/s and managed to get consistently usable
parts (although in need of some postprocessing to remove blobs and
zits) with printing times of about half a day.

Due to the nature of the part (being a relatively thin shell), it
probably makes sense to print it as all-walls, i.e. "infinite" walls
with zero top and bottom layers and 100% infill.  (Using less infill
doesn't really reduce printing time and it reduces material
consumption only marginally). As the top surface is generally covered
by keycaps the top skin is not as important as the outer walls in
terms of quality, so you might want to experiment with printing the
outer walls at a somewhat lower speed.  My experimentation with
printing keycaps also indicates that setting "combing mode", to "not
in skin", or "within infill" and using "coasting" can also help get
cleaner surfaces.

The supports of this part (I used "zig-zag" supports at a density of
15%) are mostly fairly robust, but some areas along the perimeter can
require slim support structures, so using support brims and avoiding
supports when traveling is probably a good idea.  You might want to
experiment with setting a "support Z distance" of twice the layer
height for easier support removal.  I didn't do this and was still
able to remove supports safely, although it could be a nuisance at
times.  Using "fan speed override" to set the fan to 100% when
printing supported skin is recommended.

If using the part cooling fan (I did, but it depends on the material
used) it is probably best to keep it turned off for the first 5 or 10
layers.  Even with PETG this part can be large enough to warp
slightly, which can show up as seams between the part and bottom
cover, mostly at the corners.  (If you do get minor warping and want a
flush surface, you can put a sheet of sanding paper on a flat, hard
surface, such as a glass table-top, place the part over it and sand
with broad circular motions, while applying little pressure.  Test the
part for flatness often and keep in mind that you can only remove so
much material before the switch pins start hitting on the cover).

### The bottom cover

Settings for the bottom cover are mostly the same as above, with a
couple exceptions.  Infill should still be set at 100%, but as the
part is mainly flat, the default configuration of all bottom layers
should probably be used.  I also disabled the cooling fan for this
part, as surface quality is of virtually no importance, whereas part
strength does matter to some extent, especially at the mounting bosses
for the PCB, which, although designed with maximal surface for the
given space, for better adhesion to the bottom cover, can still be a
weak spot.  (I've had a boss detach - not break off - from the cover,
but after redesigning their cross-section shape, they now seem to be
more than sturdy enough.  Note that if one, or even two, bosses
detach, you can glue them on easily enough by using the PCB to
position them properly and keep them in place as the glue cures).

### The stand

The stand is much more bulky and should therefore be printed with less
dense infill.  I used "cubic" infill at a density of 25%, with a wall
thickness of 1.6mm and top and bottom thicknesses (which are less
critical in terms of part strength) of 1.2mm.  Some part cooling
should probably be used to keep the surface from getting too
unsightly, but it should be kept low, so as not to compromise strength
too much.

The part can be printed mostly without supports, with the exception of
certain areas of the upper protruding mounts and the conical bosses
that mate with the countersinks of the bottom cover.  Support
structures on the upper bosses can be particularly hard to remove, so
a "support Z distance" of at least two layers is recommended, as is
setting "fan speed override" to 100%.  Furthermore, as the supports
have a small base, particularly in the case of the lower countersink
supports, they may fail to stick to the build plate.  Apart from
lowering the initial layer print speed speed, setting the initial layer
acceleration lower than the normal value (half was a good value in my
case) is even more critical.  Using support brim can also help.

### The boot

The rubber boot should of course be printed in TPU.  Ideally you'd
want to use the softest filament you can get, as this will maximize
grip, but soft filaments can be hard to print, so use your best
judgment, based on prior experience and print some test pieces to
gauge the expected grip on the surfaces you expect to use the
keyboard on.

One trick worth trying, is to slice the bottom 0.5mm or so of the part
as support or infill.  This can be done by either building the part
with a 0.5mm lower `boot-bottom-thickness` and then raising it by the
same amount off the platform, or by using the "per-model settings"
feature.  You can then select a suitable infill or support pattern and
density to create a sort of "tread" on the bottom of the boot.  This
won't really increase the maximum grip to be had on a clean surface,
but it might help retain some grip as grime and dust starts building
up.  Sadly, you'll have to clean the table at some point.

### Miscellanea

There are a few more bits and pieces you can print.  To avoid ending
up with a multitude of STL files, these are all built into a file
named `things/misc.stl`.  You can build one at a time, with a command
of the general form:

```bash
lein run misc/subpart --no-draft
```

Here `subpart` can be any of the following:

* `bracket`: A bracket, mounted over the PCB, used to tie the wiring
  harness to.
* `dsa-1u-convex`, `dsa-1.25u-convex`, `dsa-1.5u-convex`: Convex 1u,
  1.25u and 1.5u DSA keycaps, useful for the thumb section.
* `dsa-1.5u-convex-homing`: As above, only with a homing bar in the
  center.
* `dsa-1u-concave`: A standard, concave, 1u DSA keycap, meant for the thumb section.
* `sa-1.5u-concave`: A standard, concave, 1.5u SA keycap, for the outer
  columns of the main section.
* `sa-1.5u-saddle`: A special variant of a 1.5u SA keycap, with a top
  that is concave along the vertical and convex along the horizontal.
  Meant for the palm keys, it provides a better fit for the side of
  your palm.
* `dsa-1u-fanged`: A special variant of a 1u DSA keycap, circularly
  extended on one side.  Meant for the inner key of the lower thumb
  section, to require less inward curling of the thumb compared to a
  standard 1u key.  It is also concave, to allow comfortable pressing
  of the key with the palm.

#### Printing keycaps

Printing keycaps doesn't present many challenges, but since there will
potentially be many of them to print, each taking relatively little
time and since surface finish might be of more importance compared to
other parts, it can pay off to finetune the printing parameters so as
to require as little postprocessing as possible, even at the expense
of added printing time.

Begin by printing at the lowest layer height your printer can manage
(I printed with PETG, at a layer height of 0.05mm).  Keep the printing
speed low and, perhaps more importantly, constant for each part of the
keycap (i.e. outer walls, bottom/top layers, etc.)  Differences in
speed seem to lead to color variation patterns in the keycap, probably
due to the varying dwell time of the plastic within the hotend.
Retractions at layer changes and before outer walls seem to help
minimize blobs and zits on the surface of the keycap, as does using
coasting and setting combing mode to "not in skin".

Finally you might want to experiment with "fuzzy skin", to make the
sides of the keycap rough (it is less effective on the top, which
you'll have to finish with high-grit sandpaper anyway).  The chief
benefit of this, is that it helps conceal the layer artifacts and thus
minimizes the necessary finishing.  I used fuzzy skin thickness and
point distance of 0.06mm.  You can use per-model settings to disable
fuzzy skin in the inside of the keycap.  While you're at it, you
should also set "Z seam alignment" to "random" for the stem, so as to
prevent the development of a Z seam there, which can interfere with
the switch and which can be a bother to file out.

### Some notes on finishing

Instructions on how to post-process 3D prints are outside the scope of
this guide and I wouldn't be the best person to offer them in any
case, but I'll note some caveats, in case you intend to sand the
printed parts to remove the layer lines.

The first caveat is that you shouldn't sand the parts, if you can stand
the unfinished appearance at all, as it will likely prove a
soul-crushing undertaking.  If you absolutely must, note that you'll
need to be careful if you intend to retain the features of the printed
part and avoid ending up with a sort of "molten icecream" look.  Use a
sanding block or back the sandpaper with a piece of foam and take care
to keep the paper's surface perpendicular to the surface of the part
at all times.  This is easy on the larger, flat areas, but can be
tricky for the chamfered edges.  I recommend printing a couple of case
test fragments and practicing before tackling the whole part.
Additionally, if you aim for seamless mating between the chassis,
bottom cover and/or stand, you should fasten them together before
sanding, so install the threaded inserts beforehand.

You can start with a grit of about 120 and double after each pass, as
usual, until you get to the desired finish.  I used grits of 120, 220,
320, 600 and 1000, wet-sanding with all but the first.  Just remember
to apply light pressure and use circular motions of varying radii, as
much as possible, and you should be fine.

For the keycaps, I used 600 and 1000 grit for the top and 1000 grit
only for the sides.  The cross mount will probably need some
finishing.  I used a needle file, which luckily happened to be exactly
the right size, to remove the debris from inside the cross, followed
by multiple applications of a stabilizer mount to prepare it, so that
the keycap will fully seat without too much force.  If you feel the
keycap dragging as it is depressed, you might need to smoothen the
keycap stem.  You can use a needle file with a round, curled tip.

## Assembly

Assembly consists of the following steps:

* Installing the threaded inserts.
* Mounting the switches
* Forming the matrix, by wiring the switches together using solid-core
  wire.
* Forming the wiring loom connecting the matrix to the PCB, using
  stranded wire.
* Mounting the PCB and wiring bracket, routing, tying and connecting
  the harness.
* Optionally, installing the boot.

With the exception of the last, which is straightforward, each of
these tasks presents challenges and must be approached methodically.
Each task also needs to be given some time.  Assume it will take
several hours and plan accordingly.  Keep an orderly, clean and
well-lit workspace, with all needed tools handy.  If you don't have a
lot of soldering experience, now is a good time to research proper
soldering technique and practice.  In contrast to the usual case of
PCB assembly, most of the required solder joints will need to be made
between the leads of unsupported components, often in recessed or
otherwise constrained spots.  Using proper technique, execution should
be straightforward, but you probably won't find it very forgiving to
bad practices.

Soldering is outside the scope of this guide, but an invaluable source
of information, are the NASA workmanship standards.  Although many are
now canceled, the older ones can still be [accessed
freely](https://workmanship.nasa.gov/lib/insp/2%20books/frameset.html)
and contain much information, both on specific techniques and on how
soldering works and what constitutes an acceptable solder joint, or
wiring connection.  Of particular interest for the work ahead, are the
sections on through-hole soldering (especially the part on service
lead splices), cables and harnesses and, for assembly of the PCB, on
surface-mount soldering.  The section on crimped terminations might
also be useful, if you intend to crimp your own connectors.

### Installing the threaded inserts

The screw bosses have been designed for self-tapping threaded inserts.
These are normally installed in a pre-bored hole, but to make
installation easier, the bosses are printed with internal threads
matching the external thread of the insert.  The inserts can be
installed without any special tools.  One way is the following: use a
short hex bolt with a nut threaded onto its end, leaving a few turns
of the bolt's thread exposed.  Thread the insert into the exposed end
and jam it against the nut.  You don't need to tighten it very much.
Next, carefully align the insert with the internal threads of the boss
and start threading it into the it.

You can use a hex socket to make installation more comfortable, but
using a wrench is not advisable, especially during the initial phase,
when cross-threading is possible.  Monitor the entry of the thread
constantly and make sure it remains perpendicular to the boss at all
times, as it is threaded into it.  Once it's all the way in and
finger-tight, use a spanner wrench to loosen the jam nut, while
holding on to the bolt to keep it from backing out and you should be
done.  The insert should be flush with the boss' surface, or barely
depressed into it, but make sure not to overtighten it.  No adhesive
should be required to keep the insert in place.

### Mounting the switches

Shown below, is a diagram of the switches with the pins shown as
copper-colored circles.  It is drawn, as are all the diagrams that
follow, looking down at the bottom of chassis, as if you had unscrewed
the cover and flipped it over.  The orientation of the switches, seen
from the location of their pins, has been chosen so as to make wiring
and soldering easier, especially for keys in hard to reach areas.
While working, refer to the diagram and double-check the orientation
before installing each switch.

![Wiring diagram (switches only)](./doc/wiring_diagram_switches_left.svg?raw=true)

  When working on the right side, you can use [this mirrored
diagram](./doc/wiring_diagram_switches_right.svg?raw=true) instead.

Begin by cutting off the plastic pins used for PCB mounted switches,
if present.  These can interfere with the chassis in certain tight
spots and will probably be a nuisance during wiring anyway.  Switches
are normally held onto the plate by a pair of tabs and also, and
probably chiefly, by the solder joints with the PCB.  Since this is a
(mostly) handwired build, the solder joints won't help keep the
switches in place.  The plate alone generally isn't sufficient to keep
the switches in, and although you can configure nubs on the edges of
the holes, to give the tabs on the switch something to grab on, I have
not found them to work reliably after trying several designs and
configurations.  This may be due to fundamental limitations of the FDM
technique, but perhaps nubs could be designed better, so you might want
to perform your own experiments.  Otherwise, if robustness is desired,
glue will be necessary.

Depending on the filament type used for the chassis and the material
of the switches, some glue chemistries work better than others.
Cyanoacrylate glues (super glues) work, although adhesion is not good
with nylon (some tests indicate a sufficiently strong bond will
develop, if allowed enough time to cure).  The bond is generally
permanent, making assembly errors and subsequently failed switches
harder to repair.  If you decide to use it anyway, a thick gel-type
formulation should be used and applied with care, to avoid allowing
the glue to flow inside the switch, through openings on the casing.
Generally, glue should only be applied at the four corners, below the
flange that rests against the top of the switch hole.

Another possibility, with excellent final results, albeit much harder
application is to use contact adhesives.  Glues based on
polychloroprene (neoprene), seem to bond well with most plastics
(notably the PETG and nylon combination I had to deal with) forming a
bond that is strong enough to keep the switches in place, but that can
be broken, if need be, to correct errors or perform repairs, with
sufficient force.  The major difficulty is in application, as contact
adhesives need to be applied to both surfaces and precise application
can be challenging.  Again, care must be taken to use a glue that is
not too fluid, as it will otherwise enter the switch.  Experimenting
on a partial test print of the chassis, with the switch to be used is
highly recommend, so as to practice application of the glue and
evaluate the resulting bond and the electrical and mechanical
condition of the switch after application.  A tool, such as a
toothpick, or perhaps a brush can aid precise application of the glue.
It will generally take longer to apply, compared to applying it
straight out of the tube, but much time and effort can be saved when
cleaning up afterwards.

Before starting, the side walls of the switches, the undersides of the
flanges which rest against the plate and the mating parts of the holes
walls and plate top, should be lightly sanded with a small, fine file,
both to remove loose plastic and to scuff the plastic, increasing the
surface area on which the glue can adhere.  Whole columns of switches
should be glued in one go, to avoid the possibility of glue falling on
nearby installed switches.  This presents a problem as contact
adhesives need to dry for some time (typically a quarter of an hour)
and, as it takes some time to apply glue to a switch and corresponding
hole, care needs to be exercised to keep approximate track of the time
the glue on each switch-hole pair has had to dry.  A lap timer can be
useful in this regard.  Glue should be applied to the corners of the
switch, trying to bridge over the centers, where there may be gaps in
the switch housing, allowing ingress of the glue into the switch.  On
the hole, it should be enough to apply glue so as to wet the side
walls, as this is the major site of contact between the chassis and
the switch.  Again, tests should be performed beforehand, to evaluate
the resulting bond strength.

There will be overflow, which should be cleaned before the glue has
dried completely, especially in the case of glue that has covered the
switch pins or back of the switch.  Once the glue has dried, it can be
very hard to remove, but some time should probably be allowed before
attempting to clean the overflow from the top of the chassis, to
minimize the risk of inadvertently removing glue from between the
switch and chassis along with the overflow.  A scalpel, or similar
tool, can be used to cut along the periphery of the switch, as close
to the switch edge as possible, to allow cleaner separation of the
overflow.

If the glue is applied carefully, with a toothpick or similar tool, so
as to coat the sidewalls only with a generous layer, overflow should
be minimal, making cleaning optional.  In that case you can probably
also glue the switches one at a time, which would allow for simpler
albeit much slower execution.

### Wiring the matrix

The keyboard matrix consists of diodes and solid core wiring forming a
set of columns, which connect one terminal of each switch with the
corresponding terminal of the one below it, and rows, connecting the
other terminal of vertically adjacent switches.  It can be formed in
various ways; the layout I used is shown in the diagram below:

![Wiring diagram (the matrix layers only)](./doc/wiring_diagram_matrix_left.svg?raw=true)

The basic considerations in the design of the layout are ease of
construction and reliability.  This layout was designed so that the
columns can be formed using the diode leads alone, with one exception,
in the second column, at the connection between main and thumb
sections.  You'll have to use a jumper wire there.  Where possible,
wires and diodes have been routed so as to keep out of each other's
way, to reduce there risk of short circuits during or after
construction.

Solder joints are shown as circles in the wiring diagram, in the color
of the wire to be soldered.  Black wires are column diode leads, red
wires are solid core row wires, while switch pins are shown as
copper-colored circles.  As you work on the matrix and harness keep a
copy of the wiring diagram in view and double-check the diode
direction and lead and wire routing before soldering.  For the right
side, you can use [this mirrored
version](./doc/wiring_diagram_matrix_right.svg?raw=true) instead.

Knowing how the matrix works electrically is not necessary, but it can
help, especially with checking your work and troubleshooting problems.
Once all wiring is in place, carrying out tests and repairs is going
to be more difficult and more prone to create additional problems, so
you should validate your work as you go along, trying, as much as
possible, to have everything working properly by the time you screw
everything together and start loading software.

#### Columns

Begin by forming the columns first, working top to bottom, referring to
the wiring diagram for proper installation.  Have a spare switch,
mounted on a keycap, so that it will stay upside-down, and use it to
pre-form the diode leads in the (approximate) shape they will need to
have when installed.  Keep the diode body in the proper location on
the switch (as shown in the diagram) and bend the anode, routing it
towards the switch pin.  Make a sharp bend to mark where it meets the
pin, then use a tool like needle-point tweezers to form the remaining
anode lead (after the bend) into a coil, at right angles to the lead
and large enough to hold the pin.  This can be accomplished by winding
the lead around the tweezers, choosing the starting point so as to get
a proper coil radius.

Continue by bending and routing the cathode lead.  In the majority of
the cases, the cathode lead attaches to the next diode (on the next
row, that is) near its body, since both diode leads exit in the same
direction.  In this case bend the last few millimeters of the lead 45°
upwards, to form the beginning of a hook.  If the cathode leads of two
rows exit facing each other, (as is the case of most diodes in the
first and second rows), one of the cathodes should be left straight,
while the other is again formed into a coil, only in this case, the
coil should be coaxial with the lead, like a corkscrew.

When installing the diode, start by either inserting the 45° hook
below the bent cathode lead of the next diode, or through the
corkscrew coil, depending on the case. Next, carefully place the diode
in the correct position (as shown in the diagram) and thread the anode
coil around the switch pin.  Finally, keeping the two neighboring
diodes steady, using light fingertip pressure, use tweezers to bend
the free cathode end around the neighboring cathode, to close the hook
around the coiled or bent lead and keep them in place.  Here's a fully
formed diode column, right before soldering (shown rotated to save
page space):

![Formed diode column](./doc/diode_column.png?raw=true)

The whole column can usually be formed in this way, before soldering
takes places, which allows for last minute adjustments.  In certain
tricky situations, it can be helpful to solder some of the anodes
along the way, to keep the diodes steady.  Make sure that they are
properly installed before doing so.

Besides keeping the parts together during rigging, the coils allow a
proper solder joint to be formed.  The hooks are not as satisfactory,
but get the job done.  Note that, except in a couple of cases, where
neighboring switches are close together, you should not need to clip
any leads and even in the aforementioned cases, only the cathodes need
to be clipped.  Try to position the coils so that the switch pin
barely sticks out of it and avoid pushing them down while soldering.
Also keep the hooks away from the diode bodies, as much as possible.

After rigging, the hooks and coils should keep the parts to be
soldered in place, so that you can use your hands to hold the iron and
solder. Apply heat to both parts before feeding enough solder to get a
concave fillet, ideally keeping the contours of the joined leads
visible.  Make sure you don't overheat the switch pin, as you can
easily damage the switch (the solder iron should not need to be
applied for more than one or two seconds).  Also be cautious when
soldering in recessed areas, to avoid inadvertently melting the
chassis.  Again, this should be straightforward enough, with the parts
held securely in place.

After soldering of each column is complete, use a DMM in diode mode,
placing the black probe at one end of the cathode string and probing
each anode joint, to confirm that each diode shows a reading.  Then,
with the DMM in continuity mode, probe the pins of each switch and
make sure none of them are shorted, unless the switch is pressed.
Troubleshoot and repair as necessary.

#### Rows

Use solid core wire for the rows.  Strip the edge and form it into a
coil (using needle-point tweezers, as before), aiming for at least 3
windings.  Place it on the pin of the last column and route the wire
to the next pin.  Mark the wire right at the point where it meets the
pin and use an automatic wire stripper to strip the wire a millimeter
or so before the mark.  This will create an incision in the
insulation, exposing bare wire which can be formed into a loop for the
next pin, as before.  The stripping action will tend to push the
insulation back onto the already formed coils, deforming them in the
process.  To avoid this, hold the wire with the tweezers inside the
last formed loop while stripping.  The exposed wire will generally be
too small to form a new coil, especially for the first ones, where the
remaining wire is long.  More wire can be exposed, by forming the
first half of the first turn of the coil and pulling the remaining
insulation outwards, while holding onto the wire with the tweezers.
Allowing your fingers to slide while pulling can help displace the
insulation for the first coils, when it is still long.  Care should
be taken not to expose too much wire, since it's generally not
possible to slide the insulation back.  If you've exposed just a bit
too much, you can always just form a coil with one or two more turns.

Working in this manner, a single piece of wire with loops at the
right places can be formed.  This can then be mounted on the
respective pins, where it should be stable enough to solder into
place.  An example of a half-finished row can be seen below:

![Wired row](./doc/wired_row.png?raw=true)

Note a couple of things: while soldering, the nearby insulation can be
heated to the point of softening or melting.  Any column wires
crossing below it can then dig into the insulation and cause a short.
To avoid this, never hold the coil in place around the pin by pushing
it down while heat is applied.  Instead, lightly bend the wires
appropriately to keep it in place.  Furthermore, the coils should be
formed in such a way, that the wire exits the pin at the high side
whenever it crosses an exposed cathode lead immediately next to it.
This way, the coil will function as a spacer, keeping the wire
elevated and away from the cathode lead.  Unfortunately, the above
photo is a good counter-example of this.  On all but the right-most
switch, the coils have been formed incorrectly and the row wire is in
danger of coming in contact with the diode leads below as a result.
If the coils had been formed inverted, the wire would be elevated on
the side where it crosses the diode column.

### Wiring the harness

The harness consists of wiring between the matrix and the controller.
One end is terminated in male connectors that mate with connectors on
the PCB while the other is routed to each row and column and
terminated via solder joints.  If indicator LEDs are used on some
keys, these form additional wiring in the harness.

Multi-strand wire must be used for the harness, as it will need to
bend on its way to the PCB.  I used 0.20mm²1 (24 AWG) wire, but
thinner wire (i.e. a higher gauge) might be more appropriate, as
bundles of multiple (up to 13 or more) wires can quickly become
inflexible.

The routing must be designed so as to allow for proper strain relief.
This means two things: First, each wire should need to bend as little
as possible and, second, it should not bend at all at the solder
joints.  Several layouts are possible and the one I chose is shown
below, with harness wires for rows and columns drawn in orange and
dark gray respectively and solder joints shown as circles of matching
color.

![Wiring diagram](./doc/wiring_diagram_left.svg?raw=true)

It is not the shortest layout, but it allows all wires to be gathered
together well before the connectors, in the space under the thumb
section.  This is desirable for several reasons: Besides
considerations of tidiness, the area below the thumb section affords
the largest possible space in which to make the bend towards the PCB.
Additionally, depending on the wire gauge used, you might need to
leave the harness untied at this point, to allow each wire to bend on
its own, as otherwise the necessary radius might be too large.  Having
a larger volume available, as afforded by the empty space under the
thumb section can be helpful in this respect.  Finally and more
importantly, bundling the wires together before the PCB allows for
strain relief at the connectors.

To achieve this, wires should not bend right at the exit (or entry)
point into the connector.  Instead, they should exit in a straight
line, and bend after a couple of centimeters or so, into the largest
radius possible in the given space.  Furthermore, the harness should
be tied to a fixed point near the connectors, to maintain proper
routing and prevent strain at the connectors themselves.  A special
bracket has been designed for the purpose.  This is illustrated in the
following photo, with row wiring in red, column wiring in black and
LED wiring (not shown in the diagram) in green:

![Wiring harness (side view)](./doc/harness_side.png?raw=true)

Also in the interest of strain relief, the wiring making up the
harness should be tied together near the connectors and matrix solder
joints.  Use zip ties (or lacing if you feel so inclined), at regular
intervals, every couple of centimeters or so where possible, and
immediately before and after branches, i.e. where two or more separate
bundles of wires merge into one.  (At the matrix end the harness wires
can be routed under the solid core matrix wires for additional
support, or, perhaps, instead of tying them.)

The following photo will hopefully illustrate these points:

![Wiring harness (top-down view)](./doc/harness_top.png?raw=true)

The construction process is relatively straightforward.  Use books or
boxes to prop up the chassis and bottom cover as pictured and install
the PCB and bracket.  Loosely tie a zip-tie or rubber band around the
bracket to keep the wires in place as you lay them out and cut them to
length.  Initially, cut each wire 5-10cm longer than required, and
label it with the row or column number, using some paper tape.  Crimp
the terminals onto one end, if you didn't use pre-crimped leads and
insert the terminals into the connector housing.  Mate the connectors
onto the PCB and form the initial strain relief bends, then start
tying the row, column and LED wires into separate bundles and tie
these to the bracket.  Do not tighten the zip ties fully initially, so
as to allow final adjustments.  Route each wire to its destination
keeping it from tangling with already routed wires as much as possible
and solder it into place.  It is best to start with the wires of the
thumb section and work your way outwards, since the former are the
hardest to execute, so it's best to have fewer wires in the way when
soldering them.

To solder the harness wires onto the matrix, you can use lap splices
for the columns and hook splices for the rows.  Begin by stripping
about 8mm of insulation from the end of the wire and tinning it.  If
it is a column wire, tin the part of the cathode lead it will be
soldered to and then align the tinned areas and reflow the solder.
For row wires, form the tinned wire end into a hook of large enough
diameter to encompass the solder joint between the row wire coil and
the switch pin, then place the hook on it and reflow the solder.  Take
care to keep the exposed part of the wire away from the nearby column
solder joint, or any other exposed leads.

The lap splice approach for the column wires is perhaps not entirely
satisfactory, as it can be hard to properly align the wire to the lead
and keep it aligned as the solder solidifies.  Perhaps a hook splice,
as suggested for the row wire joints would be preferable, but I
haven't tried it.

The preceding discussion and diagrams does not address indicator LEDs,
as they're optional, and can be placed on any keys, according to
taste.  I installed LEDs on 3 keys of the top thumb row, on the right
side only.  The wiring can be done using the techniques discussed
above: a solid core wire can be used as for rows, to tie the LED
cathodes together and hook splices can be used for the harness wire
joints.  Note that the driver assumes that you'll only have LEDs on
the right side, as it transfers the LED state (and layer state, in
case you want to use LEDs to indicate the active layer) to the right
side when the left is master.  If you want to install LEDs on both
sides, or on the left side only, it can be accommodated in the driver,
but it requires changes at the keyboard level (i.e. it can't be done
as user customization).  Open an issue to make the behavior
configurable, if you don't want to maintain your own forked version of
the driver.

One final consideration is what connector and pin to use on the PCB
for each harness wire.  You can use anything you like in principle,
but the board was designed around the assumption that you'd use the
8-pin connectors for rows and columns and the 6-pin connector for LEDs
and other bells and whistles.  To that end the 6-pin connector also
features dedicated +5V and GND lines, which you can use to power the
LEDs or other devices.

I tried to use a reasonable pinout, with the first column (which is
the outer 1.5u column on the left side and the inner 1u column on the
right side) or row (always the top row) going to the first connector
pin and the rest following in sequence.  Unfortunately, while writing
the driver, I discovered that I had managed to mix up two pairs of
wires.  I didn't want to reopen the keyboard and cut and retie the
harness in order to swap the pins on the connectors, so as a
consequence, the "official" pinout is, for "historical reasons" as
they say, the following:


| Row/column #  | 1 | 2 | 3 | 4 | 5 | 6 | 7 |
| ------------  | - | - | - | - | - | - | - |
| Left side rows | E6 | F1 | F0 | F4 | F5 | F6 | F7 |
| Left side columns | B4 | B5 | D7 | B6 | C6 | D6 |
| Right side rows | B5 | B4 | D7 | B6 | C6 | D6 | D4 |
| Right side columns | C7 | F7 | F6 | F5 | F4 | F1 |

| LED | Pin |
| --- | --- |
| LED 1 (used as a layer indicator) | D0 |
| LED 2 (CAPS LOCK indicator) | D1 |
| LED 3 (SCROLL LOCK indicator) | D2 |

If you decide to (or simply, like me, discover that you happened to)
adopt a different pinout, you can always accommodate it in the driver
later , but it would be simplest to use the "official" routing
described in the table above to avoid needing driver changes.

Once everything is in place, install additional zip ties where
applicable to keep wires from bending at the solder joints, adjust zip
ties to ensure even spacing, tighten them enough to prevent easy
slipping, but not so much as to deform the wire insulation and install
the bottom cover.  Guide the harness as you do so to keep it from
bending excessively, or from getting nicked by the cover.

### Installing the boot

If you've decided to use the supplied rubber boot, you might want to
glue it to the bottom of the stand. Depending on the sidewall
configuration (height and inset) you can probably get it to stay on
fairly well without glue, but gluing allows more hassle-free handling
and is straightforward to do, since using a simple all-purpose
adhesive allows plenty of time to properly install the boot and remove
any residue before the glue cures.

### Keycaps

The default configuration has been designed for and tested with DSA
and SA keycaps.  I would expect other keycap profiles to work as well,
but you should probably stick to uniform height keysets.  The nominal
set of required keycaps is the following:

| Keycap size | No. required | Notes |
| ----------- | ------------ | ----- |
| 1u          | 54           | 22 for the main section of each side, 1 for the outer key on the first thumb row, plus 4 for the "cross-like" lower thumb rows.|
| 1.25u       | 2            | 1 for the inner key of the first thumb row on each side. |
| 1.5u        | 14           | 4 for the outer column of each side, plus 1 for the palm key and two more for the middle keys in the first thumb row. |

A standard Ergodox keyset should cover all of these keys except the
palm keys and the 1.25u keys in the thumb section.  It is highly
recommended to print and use convex keycaps for the first thumb row,
but if you don't want to, and can't easily source 1.25u keycaps in
small quantities, 1u keycaps can be used instead.  Similarly, using
custom "saddle" keycaps, or at least convex keycaps, for the palm keys
is recommended, but standard 1.5u (or even 2u, if you don't mind the
aesthetic result) keys can be used instead.  Special "fanged" 1u keys
can be used for the inner keys in the "cross" portion of the thumb
section, to reduce the curl required for the thumb to reach them as
much as possible and also to allow more comfortable pressing with the
palm, but if you can't stand their looks, simple 1u keys should work
well enough.

Finally keep in mind that the effective curvature radius, i.e. that of
the key tops, is the sum of the chassis radius plus the keycap height.
As a result, you can use different keycap sets to fine-tune the
geometry of a finished build.  For instance, the default design might
be more comfortable with DSA keycaps for someone with longer fingers,
whereas SA keycaps might be preferable for someone with smaller hands.
This is certainly true for the main sections, but you're probably
better off sticking with DSA keycaps for the thumb sections, as this
reduces overall height and affords a more comfortable resting position
for the thumb.

## The controller

The controller PCB was designed with [KiCad 5.0.2](https://kicad.org/)
and all relevant files are contained in the [kicad/](./kicad)
directory.  The board is the same for each side, so you will require a
total of two identical PCBs, populated with the same components.

There are numerous fabrication services, where you can simply upload
the KiCad PCB file
([kicad/keyboard_mcu.kicad_pcb](./kicad/keyboard_mcu.kicad_pcb)) and
have a small number of PCBs fabricated and sent to you at a small
price.  One example, that is simple to use as it doesn't present you
with a bewildering array of choices, is [OSH
Park](https://oshpark.com/).  Other services might require so-called
Gerber files, an industry standard which [KiCad](https://kicad.org/)
can generate.

Note that some aspects of the board, in particular the USB data
traces, have been tuned with a certain board setup in mind, namely a
standard 2 layer FR-4 board, of 63mil/1.6mm width, with 1oz/ft²
copper (1.4mil), so you should probably stick to that (which is the
most common setup anyway).  Additionally, the fabrication service you
choose, must be able to support a minimum track spacing of 6mil.

### BOM

Apart from the PCBs, you'll need the following parts:

| Component(s) | Part description | Part # |
| ------------ | ---------------- | ------ |
| C1 (1 pcs, 10uF) | 10uF 10%, X7R, 0805 | EMK212BB7106KG-T |
| C3,C2 (2 pcs, 22p) |15pF 1%, C0G, 0603 | GRM1885C1H150FA01D |
| C4,C7,C8,C6 (4 pcs, 100nF) | 0.1uF 10%, X7R, 0603 | EMK107B7104KA-T |
| C5 (1 pcs, 1uF 10%) | 1uF 10%, X7R, 0603 | EMK107B7105KA-T |
| J1 (1 pcs, USB_B) | TE Connectivity, USB Connectors, B, 2.0 | 5787834-1 |
| J2,J3 (2 pcs, Conn_01x08) | TE Connectivity, AMP HPI 2.0mm headers | 440054-8, 440129-8, 1735801-1 |
| J2,J3 (alternative) | JST, PH series connectors | B8B-PH-K-S, PHR-8, SPH-002T-P0.5L |
| J4 (1 pcs, 6P6C) |  TE Connectivity, RJ25 Connectors | 5520425-3 |
| J5 (1 pcs, Conn_01x06) | TE Connectivity, AMP HPI 2.0mm headers | 440054-6, 440129-6, 1735801-1 |
| J5 (alternative) | JST, PH series connectors | B6B-PH-K-S, PHR-6, SPH-002T-P0.5L |
| R2,R1 (2 pcs, 22 5%) | 22Ω 1%, 0603 | CRCW060322R0FKEA |
| R3 (1 pcs, 10K) | 10kΩ, 0603 | CRCW060310K0FKEA |
| R4 (1 pcs, 1k) | 1kΩ, 0.1W, 0603 | RCA06031K00FKEA |
| SW1 (1 pcs, SW_Push) | Tactile Switch SPST-NO | TL3305AF160QG |
| U1 (1 pcs ATmega32U4-AU) | AVR microcontroller | ATMEGA32U4-AU |
| Y1 (1 pcs, Crystal_GND24) | 16MHz, 10 pF | ABM3B-16.000MHZ-10-1-U-T |

In addition to the above, you'll also need a 6P6C (RJ25) cable, to
connect the controllers on each side.

I've tried to select components with high availability, but you can
treat the part numbers given for the two-terminal chip resistor and
capacitors as suggestions.  Any part with the specifications listed in
the part description should do equally well.

For the wire-to-board connectors (J2, J3, J5) I've listed two
alternative sets of components, one from TE Connectivity and one from
JST.  One or the other may be easier to get, depending on where you
order from, but note that I've used the parts from TE Connectivity, so
double-check the datasheets to make sure the JST PH connectors are
indeed applicable before using them.

I would advise not crimping your own wires unless you have the proper
equipment, prior experience and know you can do a good job.  The
procedure can be rather tedious and getting a proper result can be
hard to impossible, depending on the tools used.  You can get
pre-crimped wires instead, but if you do decide to crimp your own, get
*plenty* of crimp terminals.

### Design

The controller, as a circuit, is similar to most of the controllers
boards typically used for handwired keyboards.  It is designed around
the ATmega32U4 microcontroller and comprises the essential circuitry
needed to drive it.  The main reason for designing a special board,
was to allow for proper mounting of the PCB on the case with a minimum
of required space (as most compact general-purpose controller boards
don't feature mounting holes).  Another point of difference with
general purpose designs, is the use of a standard USB Type-B
connector, instead of a Mini-B or Micro-B connector.  Although the
latter are rated for substantially more mating cycles, my experience
so far with Micro-USB connectors and cables has not been very good.
On the other hand, I've never had a standard USB connector or cable
fail (at least at the Stanard-USB end).  This design choice can easily
be revised if it proves unfortunate.

SPI is used for communication between the two sides, over a connection
made via 6P6C modular jacks (often called RJ25 or RJ12 connectors).
I've been using my boards with a 50cm coiled cable (which should
equate to several meters total length) and had no communication errors
at 4MHz (resulting in a scan rate of around 1800 scans per second).

Power for the slave side is also carried over the interconnecting
cable.  Note that in the current design, there is no provision for
keeping the +5VDC carried over the cable from reaching the slave USB
port.  As such, **you should never connect the USB ports of both sides
at the same time, either to the same, or to different hosts**.

After initial assembly, the board should boot into the bootloader and
allow programming via DFU.  After that, the device can be reset to the
bootloader for programming either via software, or through the reset
switch.  The latter should be accessible via a hole in the bottom
cover.

I note in passing, that it might be possible, in principle, to make do
with only 4 wires, by not carrying the "slave select" (SS) and "master
out slave in" (MOSI) signals over the cable.  Instead of having the
master side select the (one existing) slave, the slave could select
itself, by pulling the SS line to ground and transmit its matrix over
the single MISO line.  This would allow using 6P4C connectors and
cables, which might be preferable as the latter can be easier to
source, but the one-way communication and inability of the master to
reset the slave's SPI, might make a reliable connection impossible (by
precluding handshaking).  It would also make it impossible to transfer
the LED state to the slave, necessitating LEDs on both sides.

## Software

You can use the [QMK firmware](https://qmk.fm/) to drive the keyboard.
At the time of this writing, the driver hasn't yet been merged into
QMK, so you'll have to check out my forked version.

```bash
$ git clone https://github.com/dpapavas/qmk_firmware.git -b lagrange_keyboard
$ cd qmk_firmware/
$ make git-submodule
$ make handwired/lagrange:default
```

The last command will simply build the firmware.  Instead, you can
build and install in one step with:

```bash
$ make handwired/lagrange:default:dfu
```

In the previous commands `default` refers to the default keymap, which
is meant to be a basic keymap, most closely resembling that of a
"typical" keyboard.  Read the relevant QMK documentation on how to
create your own private keymap.

If you decided to use a different row/column to pin mapping, perhaps
it would be best to override the defaults in your own keymap, instead
of changing the global, keyboard-level `config.h` file.  Simply place
a `config.h` in your keymap's directory and add something like the
following inside:

```C
#undef MATRIX_ROW_PINS
#define MATRIX_ROW_PINS { E6, F0, F1, F4, F5, F6, F7 }
```

The above will unswap rows 2 and 3, which have been swapped in the
"official" pinout.  You can redefine `MATRIX_COL_PINS`,
`MATRIX_ROW_PINS_RIGHT`, `MATRIX_COL_PINS_RIGHT`, `LED_CAPS_LOCK_PIN`
and `LED_SCROLL_LOCK_PIN` in a similar way.  This will allow you to
pull future changes to QMK without having to merge, or reapply the
changed definitions in the global `config.h` file.
